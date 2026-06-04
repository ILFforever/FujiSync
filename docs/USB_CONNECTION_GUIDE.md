# USB Connection Guide — How to Build Camera-Aware Components

This documents the connection architecture so you can write new ViewModels or features
that talk to the camera without re-researching the codebase.

---

## Architecture Overview

```
UsbCameraScanner          finds Fuji USB devices + detects PTP vs CardReader mode
UsbCameraRepository       wraps scanner, provides scanUsb() → List<FujiUsbDevice>
UsbPtpConnection          opens a USB handle and finds PTP endpoints
OpenPtpConnection         live handle: holds bulkOut/bulkIn, issues PTP commands
FujiRecipeCamera          high-level camera ops: readPreset(), writePreset(), etc.
CameraHeartbeat           pings the camera every 3s; owns usbMutex
MainViewModel             owns the connection lifecycle; calls all of the above
```

All USB operations go through `CameraHeartbeat.usbMutex`. **Do not open a USB connection
without holding this mutex** — the heartbeat runs on a 3s timer and will fight you for
the interface.

---

## The Connection Cycle

Every camera operation follows this pattern:

```
1. Find the device      repository.scanUsb().firstOrNull { it.mode == Ptp }?.device
2. Acquire mutex        heartbeat.usbMutex.withLock { ... }
3. Open connection      connectionFactory.open(device) → OpenPtpConnection?
4. Open session         conn.openSession()
5. Do work              FujiRecipeCamera(conn).readPreset(slot) / writePreset(preset)
6. Close session        conn.closeSession()   (or conn.use { } handles close() automatically)
7. Release mutex        exits withLock block
```

`OpenPtpConnection` implements `AutoCloseable`. Always use `conn.use { }` or call
`conn.close()` in a finally block — it releases the USB interface claim.

---

## Writing a ViewModel That Uses the Camera

### Injected dependencies

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: UsbCameraRepository,       // to find the device
    private val connectionFactory: UsbPtpConnection,   // to open the handle
    private val heartbeat: CameraHeartbeat,            // for usbMutex
) : ViewModel()
```

All three are `@Singleton` provided by `AppModule`. No additional DI wiring needed.

### Canonical pattern

```kotlin
fun doSomething() {
    viewModelScope.launch {
        // 1. Find device (IO, doesn't need mutex)
        val device = withContext(Dispatchers.IO) {
            repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }?.device
        } ?: run {
            // No camera in PTP mode — tell the user to check USB Setting
            return@launch
        }

        // 2–7. Everything inside the mutex
        heartbeat.usbMutex.withLock {
            withContext(Dispatchers.IO) {
                val conn = connectionFactory.open(device)
                    ?: return@withContext   // couldn't claim interface
                conn.use {
                    if (!conn.openSession()) return@use
                    val camera = FujiRecipeCamera(conn)
                    // do your work here
                    camera.readPreset(CameraSlot.C1)
                }
            }
        }
    }
}
```

`delay()` calls (e.g. optional inter-write pauses inside `writePreset`) work fine on
`Dispatchers.IO` — they're coroutine suspensions, not thread sleeps.

### Updating state from inside the mutex/IO block

`MutableStateFlow.value = ...` is thread-safe. You can update UI state directly from
the IO dispatcher without `withContext(Dispatchers.Main)`:

```kotlin
heartbeat.usbMutex.withLock {
    withContext(Dispatchers.IO) {
        _state.value = MyState.Running("Reading…")   // fine
        val preset = FujiRecipeCamera(conn).readPreset(slot)
        _state.value = MyState.Done(preset)
    }
}
```

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
| `CameraHeartbeat.usbMutex` | `data/usb/CameraHeartbeat.kt` | Acquire before any USB open |
| `CameraUsbMode.Ptp` | `data/usb/CameraUsbMode.kt` | The mode you want |
| `FujiRecipeCamera` | `data/usb/FujiRecipeCamera.kt` | readPreset / writePreset / writeFilmSimulation |
| `PtpConstants.SLOT_SWITCH_DELAY_MS` | — | 100ms after SetDevicePropValue(SlotSelector) |
| `AppSettings.propertyWriteDelayMs` | `ui/model/AppSettings.kt` | Optional inter-property write delay; default 0ms |
| `BENCH_DELAY_CANDIDATES` | `data/usb/WriteDelayBench.kt` | Low-ms sweep for finding the floor on a specific body |
| `MONO_SIM_CODES` | `data/ptp/PtpConstants.kt` | Film sim codes that suppress color-only props |

---

## What NOT to Do

- **Don't open a USB connection without `heartbeat.usbMutex.withLock`** — the heartbeat
  will force-claim the interface every 3s and corrupt your transaction.
- **Don't call `connectionFactory.open()` twice concurrently** — Android gives one
  `UsbDeviceConnection` per device; the second open will either fail or steal the interface.
- **Don't keep a connection open across user interactions** — open → do work → close in
  one mutex-locked block. Long-lived connections are handled by the heartbeat, not feature code.
- **Don't assume one universal inter-write delay** — 0ms is confirmed safe on X-H2 fw 5.20,
  but keep `WriteDelayBench` available to find the floor on another body.
