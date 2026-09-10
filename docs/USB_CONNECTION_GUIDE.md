# USB Connection Guide — How to Build Camera-Aware Components

This documents the connection architecture so you can write new ViewModels or features
that talk to the camera without re-researching the codebase.

---

## Architecture Overview

```
UsbCameraScanner          finds Fuji USB devices + detects PTP vs CardReader mode
UsbCameraRepository       wraps scanner, provides scanUsb() → List<FujiUsbDevice>
UsbPtpConnection          picks the PTP interface and claims it
OpenPtpConnection         live handle: framing, transactions, session state
PtpSessionStartup         opening a session on a waking camera, and recovering one
PtpContainerReader        reassembles PTP containers from the bulk-IN byte stream
FujiRecipeCamera          high-level camera ops: readPreset(), writePreset(), etc.
CameraSessionManager      owns usbMutex AND the held connection — the way in
CameraHeartbeat           liveness + slot refresh; drives connected/disconnected state
CameraViewModel           owns the connection lifecycle; calls all of the above
```

**`CameraSessionManager` is the entry point for everything that touches the camera.** It owns
`usbMutex` and holds the open connection. Feature code should not call `UsbPtpConnection.open()`
directly.

---

## The Session Model

The connection is **opened once and held**, not reopened per operation. Claiming the interface,
opening a session and waiting for the body to settle costs far more than the commands that follow,
and older bodies are the least tolerant of having it done repeatedly.

```
sessionManager.withSession(device) { camera, connection -> ... }   // reuses the held session
sessionManager.withRawSession(device) { connection -> ... }        // same, no FujiRecipeCamera
sessionManager.withExclusiveUsb { ... }                            // releases it first
```

Both `withSession` and `withRawSession` take the mutex, reuse the held connection when it is for
the same device and still usable, open a fresh one otherwise, and return `Result<T>`. Neither
closes the connection on the way out — it stays open for the next caller. A connection that a
transport failure has poisoned is dropped automatically, so the next caller pays for a reconnect
rather than inheriting a pipe that cannot answer.

### Releasing

| Call | When |
|---|---|
| `release()` | suspending; the camera is gone or the user disconnected |
| `releaseImmediately()` | teardown that cannot suspend — `onCleared()`, `stopHeartbeat()` |

Something must release, or an unplugged-and-returned camera finds its own interface still claimed
by a connection that can no longer reach it.

---

## Writing a ViewModel That Uses the Camera

### Injected dependencies

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: UsbCameraRepository,        // to find the device
    private val sessionManager: CameraSessionManager,   // the way in
) : ViewModel()
```

Both are `@Singleton` provided by `AppModule`. No additional DI wiring needed.

### Canonical pattern

```kotlin
fun doSomething() {
    viewModelScope.launch {
        // 1. Find the device (IO, doesn't need the mutex)
        val device = withContext(Dispatchers.IO) {
            repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }?.device
        } ?: return@launch   // no camera in PTP mode — tell the user to check USB Setting

        // 2. Everything else. Mutex, session and dispatcher are handled for you.
        val result = sessionManager.withSession(device) { camera, _ ->
            camera.readPreset(CameraSlot.C1)
        }

        result
            .onSuccess { preset -> _state.value = MyState.Done(preset) }
            .onFailure { _state.value = MyState.Failed(it.message) }
    }
}
```

`delay()` calls (e.g. optional inter-write pauses inside `writePreset`) work fine — they are
coroutine suspensions, not thread sleeps.

### Updating state from inside the block

`MutableStateFlow.value = ...` is thread-safe. You can update UI state directly from inside the
session block without `withContext(Dispatchers.Main)`:

```kotlin
sessionManager.withRawSession(device) { conn ->
    _state.value = MyState.Running("Reading…")   // fine
    FujiRecipeCamera(conn).readPreset(slot)
}
```

### If you must open your own handle

Dev benches do this, and so does `FujiPtpProbe`. Wrap it in `withExclusiveUsb`, which takes the
mutex **and closes the held connection first**:

```kotlin
sessionManager.withExclusiveUsb {
    withContext(Dispatchers.IO) {
        val conn = connectionFactory.open(device) ?: return@withContext null
        conn.use {
            if (!conn.openSessionWhenReady().isOpen) return@use null
            // ...
        }
    }
}
```

This matters more than it looks: a second `claimInterface(force = true)` does **not** fail. It
silently moves the claim, and the older handle is then talking to nothing.

---

## Transport Notes

Things the wire layer handles that are easy to get wrong if you reimplement any of it.

### Framing is stream-based, not read-based

Bulk reads have nothing to do with PTP container boundaries. A container can arrive split across
several reads, two can arrive in one, and a read can end mid-header. `PtpContainerReader` always
asks for a full 16 KiB buffer, appends to a persistent buffer, and lets the length header decide
where containers end — keeping whatever a read delivered past the end of one.

Two specific traps it exists to avoid:

- Requesting only the bytes still outstanding. A device sending a full max-size packet into a
  smaller request overflows, and Android reports that as a failed transfer.
- Discarding the tail of a read. The response container often follows the data container closely;
  drop it and the *next* exchange starts mid-stream.

It is a separate class from the USB code so this logic can be tested a byte at a time without a
camera — see `PtpContainerReaderTest`.

### Transactions are checked

Every container is matched against the transaction ID that asked for it, and a command reads
containers until the response arrives rather than assuming how many will come. Answering with
another transaction's reply is worse than failing.

### Connections can be poisoned

`OpenPtpConnection` is `Usable`, `Poisoned` or `Closed`. A transport failure poisons it, and every
later command fails fast instead of talking into a desynced pipe. The way back is
`resetDevice()` — the Still Image class device reset (`bmRequestType 0x21`, `bRequest 0x66`, on the
interface), defined by the USB Still Image Capture Device class. A reset clears the camera's
transaction state but does not restore a session, so callers must reopen one.

### Session startup is patient

`openSessionWhenReady()` polls rather than taking the first answer — a body that has just
enumerated will refuse for a moment. `SessionAlreadyOpen` (`0x201E`) is treated as **stale state
to clear**, not as success: the camera is holding a session from a run that never closed cleanly,
and asking again will not help. `readDeviceInfoPayload()` retries with backoff for the same
reason — DeviceInfo is the first real payload requested and a failure there says nothing reliable
about the camera.

### Interface selection

Only an interface carrying both bulk directions can be the PTP one. The Still Image class (`0x06`)
wins outright; otherwise a lone candidate is accepted. There is deliberately **no** fall back to
interface 0 — on a camera in card-reader mode that is mass storage, which has a bulk pair too and
would swallow PTP traffic without complaining. Interfaces on a non-default alternate setting need
`setInterface()`, or the claim succeeds and nothing transfers.

---

## Wiring a Dev Screen Into the App

Dev screens follow the ExifBench / WriteDelayBench pattern. Four touch-points:

### 1. ProfileScreen — add param + nav row (ui/profile/ProfileScreen.kt)

```kotlin
// Add to function signature (with default so previews don't break):
onOpenMyBench: () -> Unit = {},

// Add inside the "Dev" card, after the last existing ProfileDivider():
ProfileDivider()
ProfileNavRow(label = "My bench", onClick = onOpenMyBench, inCard = true)
```

### 2. FujiSyncApp — local state + overlay stack (ui/FujiSyncApp.kt)

```kotlin
// Inside FujiSyncApp composable body:
var showMyBench by remember { mutableStateOf(false) }

// Inside overlayStackOf(...):
OverlayLayer(showMyBench) { showMyBench = false },

// Inside the AppTab.Profile branch, on the ProfileScreen call:
onOpenMyBench = { showMyBench = true },

// On the AppOverlays call:
showMyBench = showMyBench,
onMyBenchClose = { showMyBench = false },
```

### 3. AppOverlays — params + rendering (still in FujiSyncApp.kt)

```kotlin
// Add to AppOverlays signature:
showMyBench: Boolean,
onMyBenchClose: () -> Unit,

// Before the function body's closing brace, create the ViewModel once
// (outside any if-block so it survives open/close cycles):
val myBenchVm: MyBenchViewModel = hiltViewModel()

// Render the screen:
if (showMyBench) {
    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        MyBenchScreen(viewModel = myBenchVm, onClose = onMyBenchClose)
    }
}
```

### 4. Screen composable — minimal shell (ui/dev/MyBenchScreen.kt)

```kotlin
@Composable
fun MyBenchScreen(viewModel: MyBenchViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsState()
    // header row with CLOSE button
    // action button
    // results
}
```

---

## Key Constants and Classes

| Symbol | Location | Notes |
|---|---|---|
| `CameraSessionManager.usbMutex` | `data/usb/CameraSessionManager.kt` | Held for you by `withSession` / `withRawSession` / `withExclusiveUsb` |
| `CameraUsbMode.Ptp` | `data/usb/CameraUsbMode.kt` | The mode you want |
| `FujiRecipeCamera` | `data/usb/FujiRecipeCamera.kt` | readPreset / writePreset / writeFilmSimulation |
| `FujiRecipeCamera.SLOT_SWITCH_DELAY_MS` | `data/usb/FujiRecipeCamera.kt` | 25 ms after SetDevicePropValue(SlotSelector) |
| `CameraHeartbeat.PULSE_INTERVAL_MS` | `data/usb/CameraHeartbeat.kt` | 3 s; one round trip per pulse |
| `CameraHeartbeat.SLOT_REFRESH_EVERY_PULSES` | `data/usb/CameraHeartbeat.kt` | Full board re-read every 10th pulse (~30 s) |
| `PtpConstants.OPEN_SESSION_ATTEMPTS` | `data/ptp/PtpConstants.kt` | Readiness polling budget |
| `PtpConstants.DEVICE_INFO_ATTEMPTS` | `data/ptp/PtpConstants.kt` | GetDeviceInfo retry budget |
| `AppSettings.propertyWriteDelayMs` | `ui/model/AppSettings.kt` | Optional inter-property write delay; default 0 ms |
| `BENCH_DELAY_CANDIDATES` | `data/usb/WriteDelayBench.kt` | Low-ms sweep for finding the floor on a specific body |
| `MONO_SIM_CODES` | `data/ptp/PtpConstants.kt` | Film sim codes that suppress color-only props |

---

## What NOT to Do

- **Don't call `UsbPtpConnection.open()` from feature code** — go through
  `CameraSessionManager`. If you genuinely need your own handle, use `withExclusiveUsb`.
- **Don't take `usbMutex` yourself and then open a handle** — that is what `withExclusiveUsb`
  is for, and it also drops the held connection, which raw locking does not.
- **Don't close the connection you were handed.** `withSession` hands you the *held* connection;
  closing it pulls the interface out from under the next caller. Release through the session
  manager instead.
- **Don't assume one universal inter-write delay** — 0 ms is confirmed safe on X-H2 fw 5.20,
  but keep `WriteDelayBench` available to find the floor on another body.
