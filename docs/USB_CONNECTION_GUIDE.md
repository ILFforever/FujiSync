# USB Connection Guide

Connection architecture for code that talks to the camera over USB.

---

## Architecture overview

```
UsbCameraScanner          finds Fuji USB devices + detects PTP vs CardReader mode
UsbCameraRepository       wraps scanner, provides scanUsb() → List<FujiUsbDevice>
UsbPtpConnection          picks the PTP interface and claims it
OpenPtpConnection         live handle: framing, transactions, session state
PtpSessionStartup         opening a session on a waking camera, and recovering one
PtpContainerReader        reassembles PTP containers from the bulk-IN byte stream
FujiRecipeCamera          high-level camera ops: readPreset(), writePreset(), etc.
CameraSessionManager      owns usbMutex AND the held connection — the entry point
CameraHeartbeat           liveness + slot refresh; drives connected/disconnected state
CameraViewModel           owns the connection lifecycle; calls all of the above
```

`CameraSessionManager` is the entry point for everything that touches the camera. It owns
`usbMutex` and holds the open connection. Feature code does not call `UsbPtpConnection.open()`
directly.

---

## Session model

The connection is opened once and held, not reopened per operation. Claiming the interface,
opening a session and waiting for the body to settle costs far more than the commands that follow,
and older bodies are the least tolerant of having it done repeatedly.

```kotlin
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

The connection must be released when the camera goes away, or an unplugged-and-returned camera
finds its own interface still claimed by a connection that can no longer reach it.

---

## Writing a ViewModel that uses the camera

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

`delay()` calls (e.g. optional inter-write pauses inside `writePreset`) are coroutine suspensions,
not thread sleeps.

### Updating state from inside the block

`MutableStateFlow.value = ...` is thread-safe. UI state can be updated directly from inside the
session block without `withContext(Dispatchers.Main)`:

```kotlin
sessionManager.withRawSession(device) { conn ->
    _state.value = MyState.Running("Reading…")   // fine
    FujiRecipeCamera(conn).readPreset(slot)
}
```

### Opening a handle directly

Dev benches do this, and so does `FujiPtpProbe`. Such code wraps the handle in
`withExclusiveUsb`, which takes the mutex and closes the held connection first:

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

A second `claimInterface(force = true)` does not fail; it silently moves the claim, and the older
handle is then talking to nothing.

---

## Transport

Behavior implemented by the wire layer (`OpenPtpConnection`, `PtpContainerReader`,
`PtpSessionStartup`):

### Framing

Bulk reads have nothing to do with PTP container boundaries. A container can arrive split across
several reads, two can arrive in one, and a read can end mid-header. `PtpContainerReader` always
asks for a full 16 KiB buffer, appends to a persistent buffer, and lets the length header decide
where containers end, keeping whatever a read delivered past the end of one.

Two constraints follow from this:

- Reads request a full buffer rather than only the bytes still outstanding. A device sending a
  full max-size packet into a smaller request overflows, and Android reports that as a failed
  transfer.
- Bytes delivered past the end of a container are retained. The response container often follows
  the data container closely, and discarding it starts the next exchange mid-stream.

Framing is implemented in `PtpContainerReader`, independently of the USB I/O code, and covered by
`PtpContainerReaderTest`.

### Transaction validation

Every container is matched against the transaction ID that asked for it, and a command reads
containers until the response arrives rather than assuming how many will come. A container whose
transaction ID does not match the one waiting for it fails the transaction rather than being
treated as its reply.

### Connection states

`OpenPtpConnection` is `Usable`, `Poisoned` or `Closed`. A transport failure poisons it, and every
later command fails fast instead of talking into a desynced pipe. The way back is
`resetDevice()` — the Still Image class device reset (`bmRequestType 0x21`, `bRequest 0x66`, on the
interface), defined by the USB Still Image Capture Device class. A reset clears the camera's
transaction state but does not restore a session; callers must reopen one.

### Session startup

`openSessionWhenReady()` polls rather than taking the first answer — a body that has just
enumerated will refuse for a moment. `SessionAlreadyOpen` (`0x201E`) is treated as stale state
to clear, not as success: the camera is holding a session from a run that never closed cleanly,
and asking again will not help. `readDeviceInfoPayload()` retries with backoff for the same
reason — DeviceInfo is the first real payload requested and a failure there says nothing reliable
about the camera.

### Interface selection

Only an interface carrying both bulk directions can be the PTP one. The Still Image class (`0x06`)
wins outright; otherwise a lone candidate is accepted. Interface 0 is not used as a fallback. In
card-reader mode it is the mass-storage interface, which also exposes a bulk pair. Interfaces on a
non-default alternate setting need `setInterface()`, or the claim succeeds and nothing transfers.

---

## Wiring a dev screen into the app

Dev screens follow the ExifBench / WriteDelayBench pattern. Four touch-points:

### 1. ProfileScreen — parameter and navigation row (ui/profile/ProfileScreen.kt)

```kotlin
// Add to function signature (with default so previews don't break):
onOpenMyBench: () -> Unit = {},

// Add inside the "Dev" card, after the last existing ProfileDivider():
ProfileDivider()
ProfileNavRow(label = "My bench", onClick = onOpenMyBench, inCard = true)
```

### 2. FujiSyncApp — local state and overlay stack (ui/FujiSyncApp.kt)

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

### 3. AppOverlays — parameters and rendering (still in FujiSyncApp.kt)

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

## Key constants and classes

| Symbol | Location | Notes |
|---|---|---|
| `CameraSessionManager.usbMutex` | `data/usb/CameraSessionManager.kt` | Held by `withSession` / `withRawSession` / `withExclusiveUsb` |
| `CameraUsbMode.Ptp` | `data/usb/CameraUsbMode.kt` | The mode the app operates in |
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

## Restrictions

- Feature code does not call `UsbPtpConnection.open()` directly; it goes through
  `CameraSessionManager`. Code that needs its own handle uses `withExclusiveUsb`.
- Taking `usbMutex` directly and then opening a handle bypasses `withExclusiveUsb`, which also
  drops the held connection — raw locking does not.
- The connection handed to a `withSession` block is the held connection. Closing it pulls the
  interface out from under the next caller; release happens through the session manager instead.
- 0 ms is confirmed safe on X-H2 fw 5.20. `WriteDelayBench` finds the floor on another body; no
  single inter-write delay is assumed to be universal.
