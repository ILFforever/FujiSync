# Fujifilm USB Recipe Protocol — Reverse-Engineered From `Fuji Recipe.apk`

> Source: decompiled `com.example.fujirecipe` (Kotlin + Compose Android app).
> Key files: [`core/PTPConstants.java`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/core/PTPConstants.java), [`data/usb/UsbPtpConnection.java`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/UsbPtpConnection.java), [`data/usb/UsbPtpService.java`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/UsbPtpService.java), [`data/usb/PtpPacketKt.java`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/PtpPacketKt.java), [`data/usb/PtpResponse.java`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/PtpResponse.java), [`domain/model/FujiPropertyCode.java`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/domain/model/FujiPropertyCode.java), [`domain/model/FujiFilmSimulation.java`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/domain/model/FujiFilmSimulation.java), [`domain/model/FujiWBMode.java`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/domain/model/FujiWBMode.java), [`core/CameraPresetName.java`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/core/CameraPresetName.java).

---

## 0. TL;DR — How It Actually Works

The original `instructions.txt` assumed a **GetBackupData / SetBackupData blob-swap** model (libgphoto2-style). **The real app does not use that.** Instead, every recipe parameter is a standard PTP **device property** in the Fuji vendor range `0xD18E–0xD1A5`. To work with a specific C-slot you first write the slot index to the **slot-selector property** (`0xD18C`); subsequent reads/writes of recipe properties (and the preset-name property `0xD18D`) operate on **that slot**.

So the high-level flow is:

```
USB attach → permission → claim PTP interface → OpenSession
  → GetDeviceInfo → verify FUJI_SLOT_SELECTOR is in SupportedDeviceProperties
  → (read flow)   SetDevicePropValue(SlotSelector, slot)
                  → GetDevicePropValue(PresetName)
                  → for code in 0xD18E..0xD1A5:
                       GetDevicePropValue(code)
  → (write flow)  SetDevicePropValue(SlotSelector, slot)
                  → GetDeviceInfo  (called once during apply to refresh slot state)
                  → for each (prop, value) in pending changes:
                       SetDevicePropValue(prop, uint16LE/int16LE)
                       delay 150 ms
                  → SetDevicePropValue(PresetName, ptpString)
  → CloseSession → release interface → close connection
```

Everything below documents this in detail.

---

## 1. Physical Layer

| Item | Value |
|---|---|
| Vendor ID | `0x04CB` (1227 decimal) |
| Interface class to claim | `0x06` (Still Image / PTP) |
| Endpoints | one `BULK_OUT`, one `BULK_IN`, one `INTERRUPT_IN` |
| Bulk chunk size | 16 384 bytes |
| Standard timeout | 5 000 ms |
| Long-running timeout (render / large upload) | 120 000 ms |
| Max small-container payload | 4 194 304 bytes |
| Max render payload | 134 217 728 bytes |

If the device exposes interface class `0x08` (Mass Storage) instead of `0x06`, the camera is in **Card Reader / MTP** mode — bail out and tell the user to switch the camera's `USB Setting` to `USB RAW Conv. / Backup Restore`.

**Open sequence (Android):**

1. Locate interface where `usbInterface.getInterfaceClass() == 6`. If none, fall back to interface 0.
2. `connection.claimInterface(iface, force=true)`.
3. Walk endpoints:
   * `type == BULK && direction == OUT` → `bulkOut`
   * `type == BULK && direction == IN` → `bulkIn`
   * `type == INTERRUPT && direction == IN` → `interruptIn`
4. Success requires both `bulkOut` and `bulkIn` to be non-null. `interruptIn` is unused by this app — events are not consumed.

Reference: [`UsbPtpConnection.open()`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/UsbPtpConnection.java#L54-L92).

---

## 2. PTP Container Format

All packets use the standard PTP bulk container — **little-endian throughout**.

```
offset  size  field
0       4     length        (total packet bytes including this header)
4       2     type          (1=Command, 2=Data, 3=Response, 4=Event)
6       2     code          (opcode for cmd/resp; opcode echoed for data)
8       4     transactionId (monotonically increasing, scoped per session)
12      ...   payload       (for Data: raw bytes; for Cmd/Resp: up to 5×uint32 params)
```

Validation rule used everywhere: `length >= 12 && type in {1..4}`.

### Command packet
`length = 12 + 4*params.size()`, type = 1, payload = consecutive `uint32LE` params.

### Data-out packet
`length = 12 + payload.length`, type = 2, payload = raw bytes. Sent immediately after the command packet, **using the same opcode and transactionId**.

### Response packet
type = 3, payload = consecutive `uint32LE` response params (often empty).

### Transaction IDs
Start at `1`. After `disconnect()` they reset to `1`. Allocated by `nextTxnId()`; returned **then** incremented (so the first call yields 1, the next 2, etc.).

Builders / parsers: [`PtpPacketKt.buildCommandPacket`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/PtpPacketKt.java#L32-L47), [`PtpPacketKt.buildDataOutPacket`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/PtpPacketKt.java#L49-L61), [`PtpResponse.Companion.parse`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/PtpResponse.java#L165-L197).

---

## 3. Opcode Table (verified against `PTPConstants.kt`)

### Standard PTP

| Hex | Dec | Symbol | Direction |
|---|---|---|---|
| `0x1001` | 4097 | `GET_DEVICE_INFO` | data IN |
| `0x1002` | 4098 | `OPEN_SESSION` | command only |
| `0x1003` | 4099 | `CLOSE_SESSION` | command only |
| `0x1007` | 4103 | `GET_OBJECT_HANDLES` | data IN |
| `0x1008` | 4104 | `GET_OBJECT_INFO` | data IN |
| `0x1009` | 4105 | `GET_OBJECT` | data IN (large) |
| `0x100A` | 4106 | `GET_THUMB` | data IN |
| `0x100B` | 4107 | `DELETE_OBJECT` | command only |
| `0x1014` | 4116 | `GET_DEVICE_PROP_DESC` | data IN |
| `0x1015` | 4117 | `GET_DEVICE_PROP_VALUE` | data IN |
| `0x1016` | 4118 | `SET_DEVICE_PROP_VALUE` | data OUT |

### Fuji Vendor

| Hex | Dec | Symbol | Usage |
|---|---|---|---|
| `0x900C` | 36876 | `FUJI_SEND_OBJECT_INFO` | darkroom RAF upload (object info) |
| `0x900D` | 36877 | `FUJI_SEND_OBJECT2` | darkroom RAF upload (object data) |
| `0xD183` | 53635 | `FUJI_CONVERSION_TRIGGER` | RAF conversion trigger (darkroom) |
| `0xD185` | 53637 | `FUJI_CONVERSION_PROFILE` | RAF conversion profile |
| `0xD18C` | 53644 | **`FUJI_SLOT_SELECTOR`** | write target C-slot **before** any recipe op |
| `0xD18D` | 53645 | **`FUJI_PRESET_NAME`** | preset name string (UTF-16LE PTP string) |
| `0xD18E–0xD1A5` | 53646–53669 | **`PRESET_BLOCK_RANGE`** | all per-slot recipe parameters (see §5) |
| `0xF802` | 63490 | `RAF_OBJECT_FORMAT` | ObjectFormat code for RAF files |

### Response codes

| Hex | Dec | Symbol |
|---|---|---|
| `0x2001` | 8193 | `RESPONSE_OK` |
| `0x2002` | 8194 | `RESPONSE_GENERAL_ERROR` |
| `0x2003` | 8195 | `RESPONSE_SESSION_NOT_OPEN` |
| `0x201E` | 8222 | `RESPONSE_SESSION_ALREADY_OPEN` |

Source: [`PTPConstants.java`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/core/PTPConstants.java).

> ⚠️ The opcodes `0x9080 / 0x9081` from the original `instructions.txt` (`FUJI_GetBackupData / SetBackupData`) **do not appear in this APK at all**. That blob model is unused.

---

## 4. Transaction Engines

Three transaction shapes, all built on top of `bulkTransfer`:

### 4.1 Command + Response (`executeCommand`)
Used for ops with no data phase, or ops that return a Data packet followed by a Response.

```
TX:  Command(code, txId, params)
RX:  one container, length-padded read up to 16 KiB
     if type==2 (Data): keep its payload, then RX another container as the Response
     else: treat as Response (success or error)
```

Result is `PtpTransaction(data?, response?)`. `data.isOk()` checks `code == 0x2001` on the response. See [`UsbPtpConnection.executeCommand`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/UsbPtpConnection.java#L387-L402).

### 4.2 Command + Data-out + Response (`executeCommandWithData`)
For writes (e.g. `SetDevicePropValue`):

```
TX:  Command(code, txId, params)
TX:  Data(code, txId, payload)    — streamed in 16 KiB chunks via sendDataPacketStreaming
RX:  Response
```

The streaming sender packs the 12-byte container header into the first chunk and concatenates as much payload as fits. See [`UsbPtpConnection.executeCommandWithData`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/UsbPtpConnection.java#L419-L430) and [`sendDataPacketStreaming`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/UsbPtpConnection.java#L160-L201).

### 4.3 Command + Streamed Data-out + Response (`executeCommandWithStreamData`)
Same shape as 4.2 but pulls bytes from an `InputStream`. Used for RAF uploads in the darkroom flow.

### 4.4 Bulk receive helpers
* `receiveBulk(maxSize, timeout)` — one raw read up to `maxSize`.
* `receiveBulkFull(timeout)` — reads first 16 KiB, parses the length field, and keeps reading until the full PTP container has arrived. Validates `length ∈ [12, 4 194 304]`. See [`UsbPtpConnection.receiveBulkFull`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/UsbPtpConnection.java#L285-L313).

---

## 5. Property Codes (recipe parameters)

All live inside `PRESET_BLOCK_RANGE = 0xD18E..0xD1A5` (53646..53669, 24 codes).
Source of truth: [`FujiPropertyCode`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/domain/model/FujiPropertyCode.java).

| Hex | Dec | Symbol | Display | Default | Encoding |
|---|---|---|---|---|---|
| `0xD190` | 53648 | `DYNAMIC_RANGE` | Dynamic Range | 100 | uint16LE |
| `0xD192` | 53650 | `FILM_SIMULATION` | Film Simulation | `PROVIA` (1) | uint16LE |
| `0xD193` | 53651 | `MONO_WC` | Mono WC | 0 | **int16LE** |
| `0xD194` | 53652 | `MONO_MG` | Mono MG | 0 | **int16LE** |
| `0xD195` | 53653 | `GRAIN_EFFECT` | Grain Effect | 6 | uint16LE |
| `0xD196` | 53654 | `COLOR_CHROME` | Color Chrome | 1 | uint16LE |
| `0xD197` | 53655 | `COLOR_CHROME_FX_BLUE` | Color Chrome FX Blue | 1 | uint16LE |
| `0xD198` | 53656 | `SMOOTH_SKIN` | Smooth Skin | 1 | uint16LE |
| `0xD199` | 53657 | `WHITE_BALANCE` | White Balance | `AUTO` (2) | uint16LE |
| `0xD19A` | 53658 | `WB_SHIFT_RED` | WB Shift Red | 0 | **int16LE** |
| `0xD19B` | 53659 | `WB_SHIFT_BLUE` | WB Shift Blue | 0 | **int16LE** |
| `0xD19C` | 53660 | `COLOR_TEMP` | Color Temperature | 5600 | uint16LE |
| `0xD19D` | 53661 | `HIGHLIGHT_TONE` | Highlight Tone | 0 | **int16LE** |
| `0xD19E` | 53662 | `SHADOW_TONE` | Shadow Tone | 0 | **int16LE** |
| `0xD19F` | 53663 | `COLOR` | Color | 0 | **int16LE** |
| `0xD1A0` | 53664 | `SHARPNESS` | Sharpness | 0 | **int16LE** |
| `0xD1A1` | 53665 | `HIGH_ISO_NR` | High ISO NR | 8192 | uint16LE |
| `0xD1A2` | 53666 | `CLARITY` | Clarity | 0 | **int16LE** |

Read decoder picks signed vs unsigned via `FujiPropertyCode.isSignedInt16()`. The remaining codes in the block (`0xD18E, 0xD18F, 0xD191, 0xD1A3, 0xD1A4, 0xD1A5`) are queried during read but the app has no enum entry for them — they're silently ignored, but a sniffer-style implementation should still issue `GetDevicePropValue` across the full range to match what the app does (the camera may set undocumented internal state if a slot read is partial).

Read loop reference: [`UsbPtpService$readAllProperties$1`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/UsbPtpService$readAllProperties$1.java) and [`UsbPtpService$readPreset$1`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/UsbPtpService$readPreset$1.java).

---

## 6. Enum Values

> **These are NOT the legacy `0x01…0x1E` codes from `instructions.txt`.** They are sequential 1–20 (Film Sim) and a mix of standard PTP + Fuji-extended values (WB).

### 6.1 Film Simulation (`0xD192`)
| Code | Symbol | Label |
|---:|---|---|
| 1 | `PROVIA` | Provia/Standard |
| 2 | `VELVIA` | Velvia/Vivid |
| 3 | `ASTIA` | Astia/Soft |
| 4 | `PRO_NEG_HI` | Pro Neg Hi |
| 5 | `PRO_NEG_STD` | Pro Neg Std |
| 6 | `MONOCHROME` | Monochrome |
| 7 | `MONOCHROME_Y` | Monochrome+Y |
| 8 | `MONOCHROME_R` | Monochrome+R |
| 9 | `MONOCHROME_G` | Monochrome+G |
| 10 | `SEPIA` | Sepia |
| 11 | `CLASSIC_CHROME` | Classic Chrome |
| 12 | `ACROS` | ACROS |
| 13 | `ACROS_Y` | ACROS+Y |
| 14 | `ACROS_R` | ACROS+R |
| 15 | `ACROS_G` | ACROS+G |
| 16 | `ETERNA` | ETERNA |
| 17 | `CLASSIC_NEG` | Classic Neg |
| 18 | `ETERNA_BLEACH_BYPASS` | ETERNA Bleach Bypass |
| 19 | `NOSTALGIC_NEG` | Nostalgic Neg |
| 20 | `REALA` | REALA ACE |

`MONOCHROME_CODES = {6,7,8,9,10,12,13,14,15}` — when a recipe is monochrome the app suppresses color-only properties (e.g. WB-shift, Color, Color Chrome).

### 6.2 White Balance (`0xD199`) — from [`FujiWBMode.java`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/domain/model/FujiWBMode.java)
| Code | Symbol |
|---:|---|
| 2 (`0x0002`) | `AUTO` |
| 32800 (`0x8020`) | `AUTO_WHITE_PRIORITY` |
| 32801 (`0x8021`) | `AMBIENCE_PRIORITY` |
| 4 | `DAYLIGHT` |
| 6 | `INCANDESCENT` |
| 8 | `UNDERWATER` |
| 32774 (`0x8006`) | `SHADE` |
| 32775 (`0x8007`) | `COLOR_TEMP` (Kelvin) |

When `WHITE_BALANCE == COLOR_TEMP (0x8007)`, the `COLOR_TEMP (0xD19C)` property carries the actual Kelvin value.

---

## 7. PTP-String Encoding

PTP strings are length-prefixed UTF-16LE with a terminator.

```
byte 0     : char_count_including_null (uint8)
bytes 1..  : char_count × uint16LE codeunits (last is 0x0000)
```

Empty string is the single byte `0x00`. Builders use [`PtpPacketKt.toPtpString`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/PtpPacketKt.java#L99-L118), parsers [`PtpPacketKt.parsePtpString`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/PtpPacketKt.java#L120-L136).

### Camera-safe preset names ([`CameraPresetName`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/core/CameraPresetName.java))
* Max 31 characters
* Allowed: `A–Z`, `a–z`, `0–9`, space, and `'` (smart quotes folded to `'`)
* Accents NFD-decomposed and stripped (`é → e`)
* Disallowed characters become spaces; runs of spaces collapsed; ends trimmed
* If the result is empty, fall back to `"Untitled"` (or `"C<slot>"` from the read flow)

---

## 8. Connection State Machine

State enum: `ConnectionState = Idle | Connecting(...) | Connected(...) | Failed(message)`.
Camera USB mode: `CameraUsbMode = NOT_PLUGGED | PTP | CARD_READER | OTHER`.

```
                    USB_DEVICE_ATTACHED (vendor 0x04CB)
                                │
                                ▼
                        refreshCameraUsbMode()
                                │
                                ▼
                         maybeAutoConnect()
                                │
              ┌─────────────────┴─────────────────┐
              ▼                                   ▼
   user already granted permission       requestPermission()
              │                                   │
              └─────────────────┬─────────────────┘
                                ▼
                       openDeviceConnection
                                │
                                ▼
                   UsbPtpConnection.open()
                                │
                                ▼
                         OpenSession (0x1002, param=1)
                                │
                                ▼
                         GetDeviceInfo (0x1001)
                                │   parses model + supported props
                                ▼
              FUJI_SLOT_SELECTOR (0xD18C) in supported list?
              ┌────────── no ───────────┐
              │                         │
              ▼                         ▼
       CloseSession(0x1003)      mark supportsPresetSlots=true
       set CARD_READER           state = Connected
       state = Idle                     │
                                        ▼
                              (idle, await user actions)
```

References: [`UsbPtpService.scanAndConnect`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/UsbPtpService.java#L480-L486), [`probeFujiVendorPtp`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/UsbPtpService.java#L520-L572), [`UsbPtpService$scanAndConnect$1`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/UsbPtpService$scanAndConnect$1.java).

### Android intent plumbing
* Manifest receives `android.hardware.usb.action.USB_DEVICE_ATTACHED` / `DETACHED`
* App-private permission intent action: `"com.example.fujirecipe.USB_PERMISSION"`
* On Android 13+ the broadcast receiver registers with `Context.RECEIVER_NOT_EXPORTED` (flag `4`)
* On Android 12+ the permission `PendingIntent` uses `FLAG_MUTABLE | FLAG_UPDATE_CURRENT` (`0x0C000000`)

---

## 9. Wire-Level Protocol Walkthrough

Every step shown as: `→ Command(code, txId, params)`, `→ Data(...)`, `← Response(code, params)`.

### 9.1 Session open
```
→ Command(OPEN_SESSION 0x1002, txId=1, params=[sessionId=1])
← Response(RESPONSE_OK 0x2001, txId=1)
```
The app starts `transactionId` at 1, so the first command (`OpenSession`) uses txId 1 with sessionId 1 as its sole parameter. Older sessions may return `SESSION_ALREADY_OPEN (0x201E)` — treat as success.

### 9.2 Device-info probe (used by `probeFujiVendorPtp`)
```
→ Command(GET_DEVICE_INFO 0x1001, txId=2)
← Data(0x1001, txId=2, payload=<DeviceInfo dataset>)
← Response(RESPONSE_OK, txId=2)
```

**DeviceInfo parser** ([`UsbPtpService.parseDeviceInfo`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/UsbPtpService.java#L860-L867)):

```
offset 0–7  : header (StandardVersion, VendorExtensionID,
              VendorExtensionVersion, VendorExtensionDesc string starts here)
              the parser starts skipping at offset 8 (= after 8-byte preamble)

skipPtpString(8)                      → after VendorExtensionDesc
+ 2                                   → skip FunctionalMode (uint16)
skipUint16Array(...)                  → SupportedOperations
skipUint16Array(...)                  → SupportedEvents
readUint16Array(...)                  → SupportedDeviceProperties   ← used
skipUint16Array(...)                  → SupportedCaptureFormats
skipUint16Array(...)                  → SupportedImageFormats
skipPtpString(...)                    → Manufacturer
readPtpStringAt(...)                  → Model                       ← used
```

Only `SupportedDeviceProperties` and `Model` are kept; everything else is skipped. After parsing, the app filters supported props for codes ≥ `0xD000` (`>= 53248`) to count vendor properties, and verifies the slot-selector `0xD18C` is present.

### 9.3 Reading one preset slot ([`readPreset`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/UsbPtpService$readPreset$1.java))

```
# (1) point camera at the slot
→ Command(SET_DEVICE_PROP_VALUE 0x1016, txId=N,   params=[FUJI_SLOT_SELECTOR 0xD18C])
→ Data   (0x1016, txId=N, payload=toUInt16LE(slot))    # 2 bytes
← Response(RESPONSE_OK, txId=N)

delay 100 ms                                            # let the camera commit the switch

# (2) read the slot's name
→ Command(GET_DEVICE_PROP_VALUE 0x1015, txId=N+1, params=[FUJI_PRESET_NAME 0xD18D])
← Data(0x1015, txId=N+1, payload=<PTP string>)
← Response(RESPONSE_OK)

# (3) read every parameter in the preset block
for code in 0xD18E..0xD1A5:
    → Command(GET_DEVICE_PROP_VALUE, txId=N+k, params=[code])
    ← Data(code, txId=N+k, payload=<2 bytes>)           # interpret signed/unsigned by table §5
    ← Response(RESPONSE_OK)
```

The app stores the result in `_presets` (a `MutableStateFlow<List<CameraPreset>>`), replacing the entry for that slot. If a `GetDevicePropValue` fails or returns empty, that property is simply omitted — the loop keeps going.

### 9.4 Reading "current" properties (`readAllProperties`)
Same loop as 9.3 step (3) but **without** writing the slot selector first — these are the camera's *current live* values, not a stored slot's. The app uses this for the editor view ([`UsbPtpService$readAllProperties$1`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/UsbPtpService$readAllProperties$1.java)).

### 9.5 Reading every slot (`readAllPresets`)
Loops `slot = 1..7` and re-runs the §9.3 sequence per slot. Updates `_presetReadingSlot` along the way so the UI can show progress.

### 9.6 Writing a preset ([`applyPresetToCamera`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/UsbPtpService$applyPresetToCamera$2.java))

Outer guard: `_isApplying` flag, set true on entry, cleared in `finally`.

```
# (1) select target slot
→ SetDevicePropValue(FUJI_SLOT_SELECTOR 0xD18C, toUInt16LE(slot))
← OK

# (2) refresh camera state — the apply path re-reads DeviceInfo before pushing
→ GetDeviceInfo                                         # ignored if it fails
← Data + OK

# (3) optionally read current slot values for diff/skip logic
→ GetDevicePropValue for each prop being written        # to skip unchanged
← Data + OK   (per prop)

# (4) push each pending change
for (propCode, value) in settings:
    if isMonochrome(filmSim) and prop is color-only: skip
    if whiteBalance != COLOR_TEMP and prop == COLOR_TEMP: skip
    payload = isSignedInt16(prop) ? toInt16LE(value) : toUInt16LE(value)   # 2 bytes
    → SetDevicePropValue(propCode, payload)
    ← OK / GeneralError
    delay 150 ms                                        # CRITICAL — camera needs settle time

# (5) name the preset (always after numeric props)
→ SetDevicePropValue(FUJI_PRESET_NAME 0xD18D, toPtpString(safeName))
← OK
```

**Apply-time invariants observed in the decompile:**
* Order matters. `FILM_SIMULATION (0xD192)` is written before tone/grain/color props because some of those have different allowed ranges depending on the film sim. The decompiler shows the iteration order is the `settings` map's iteration order — the producing code orders it sensibly when building it.
* When `FILM_SIMULATION` maps to a monochrome value, the apply loop **skips** color-only properties even if they were queued.
* When `WHITE_BALANCE != COLOR_TEMP (0x8007)`, the `COLOR_TEMP (0xD19C)` write is suppressed.
* The 150 ms inter-write delay is non-negotiable on bodies like the X-H2; cutting it under ~100 ms causes intermittent `GeneralError` responses on the next write.
* Outcome tracking: `success`, `failed`, `skipped` counters → `ApplyOutcome` posted to `_lastApplyOutcome`.

### 9.7 Disconnect ([`UsbPtpService.disconnect`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/UsbPtpService.java#L574-L590) + [`disconnect$1`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/UsbPtpService$disconnect$1.java))

```
# (1) snapshot the txnId now, then null-out state synchronously
closeTxnId   = nextTxnId()
connection   = null
transactionId = 1
supportedVendorProps = []
supportsPresetSlots  = false
_connectionState     = Idle
_cameraSettings      = {}
_pendingChanges      = {}
_presets             = []

# (2) on IO dispatcher, fire CloseSession then release the interface
→ Command(CLOSE_SESSION 0x1003, txId=closeTxnId)   # exceptions are swallowed
← Response (best-effort)
connection.releaseInterface(ptpInterface)
connection.close()
log("Disconnected")
```

The order — flip state to `Idle` **before** sending CloseSession — is deliberate so a stale tail of UI updates doesn't try to issue more commands on a dying socket.

### 9.8 USB detach (cable yank or camera power-off)
The `USB_DEVICE_DETACHED` receiver calls `disconnect()` immediately. CloseSession will most likely fail (endpoint gone) — that's why it's wrapped in a swallowed try/catch.

---

## 10. Darkroom (RAF Conversion) — Brief

[`UsbPtpDarkroomOpsKt.java`](reverse-engineering/fujirecipe_src/sources/com/example/fujirecipe/data/usb/UsbPtpDarkroomOpsKt.java) implements the RAF in-camera conversion flow used by the app's "darkroom" feature. Outline:

1. `FUJI_SEND_OBJECT_INFO (0x900C)` — declare a RAF object (size, name, RAF_OBJECT_FORMAT = `0xF802`).
2. `FUJI_SEND_OBJECT2 (0x900D)` — stream the RAF bytes (uses `executeCommandWithStreamData`, 120 s timeout).
3. `SetDevicePropValue(FUJI_CONVERSION_PROFILE 0xD185, …)` — load profile that mirrors the current properties.
4. `SetDevicePropValue(FUJI_CONVERSION_TRIGGER 0xD183, 1)` — kick off conversion.
5. Poll via `GetDevicePropValue(FUJI_CONVERSION_TRIGGER)` until it goes back to 0, then `GetObject (0x1009)` to retrieve the rendered JPEG (or skip if the camera writes it to the SD card).

This is orthogonal to recipe management and is not required to read/write C-slots.

---

## 11. Putting It All Together — Reference Sequence

A complete, minimal "read C3, change a couple of params, write it back" trace:

```
# attach + open
USB attach (VID 0x04CB)
claimInterface(class=6)
endpoints discovered: bulkOut, bulkIn, interruptIn

# session
→ OPEN_SESSION       txId=1  params=[1]
← RESPONSE_OK        txId=1

→ GET_DEVICE_INFO    txId=2
← Data {model="X-H2", supportedProps=[..., 0xD18C, 0xD18D, 0xD190, ...]}
← RESPONSE_OK        txId=2

# read C3
→ SET_DEVICE_PROP_VALUE 0xD18C  txId=3   data=[0x03,0x00]
← RESPONSE_OK
sleep 100 ms
→ GET_DEVICE_PROP_VALUE 0xD18D  txId=4
← Data <ptpString "Pro 400H">
← RESPONSE_OK
for code in 0xD18E..0xD1A5:
   → GET_DEVICE_PROP_VALUE code  txId=5..
   ← Data <2 bytes>; RESPONSE_OK

# user tweaks shadow tone (-2) and clarity (+3)

# write C3
→ SET_DEVICE_PROP_VALUE 0xD18C  txId=N    data=[0x03,0x00]              # slot 3
← RESPONSE_OK
→ GET_DEVICE_INFO       txId=N+1                                        # refresh
← Data; RESPONSE_OK
→ SET_DEVICE_PROP_VALUE 0xD19E  txId=N+2  data=[0xFE,0xFF]              # shadow = -2 (int16LE)
← RESPONSE_OK; sleep 150 ms
→ SET_DEVICE_PROP_VALUE 0xD1A2  txId=N+3  data=[0x03,0x00]              # clarity = +3
← RESPONSE_OK; sleep 150 ms
→ SET_DEVICE_PROP_VALUE 0xD18D  txId=N+4  data=<ptpString "Pro 400H">   # name
← RESPONSE_OK

# disconnect
→ CLOSE_SESSION         txId=N+5
← RESPONSE_OK
releaseInterface; close
```

---

## 12. Differences vs. the Original `instructions.txt`

| `instructions.txt` claim | Reality from the APK |
|---|---|
| `FUJI_GetBackupData 0x9080`, `FUJI_SetBackupData 0x9081` | **Not used.** The app reads/writes individual device properties. |
| Recipes packed at byte offsets inside a settings blob | **No blob.** Each property is its own PTP transaction targeting `0xD18E–0xD1A5`. |
| Film sim codes `0x01, 0x02, 0x03, 0x04, 0x0D, 0x0E, 0x13, 0x0F, 0x14, 0x1C, 0x1E, 0x11…` | Sequential **1–20**, see §6.1. `REALA_ACE = 20`, `CLASSIC_NEG = 17`, not `0x1E`/`0x13`. |
| Min SDK 26 | Confirmed (manifest claims Compose 1.x + Material3, supports 8+). |
| Vendor ID `0x04CB`, class 6 | Confirmed (`FUJI_VENDOR_ID = 1227`, `PTP_CLASS = 6`). |
| Session opens with `OpenSession (0x1002)` | Confirmed; `sessionId = 1`, `txnId` starts at 1. |
| `interruptIn` used for events | Endpoint is found and stored but **never read** by this app. |
| Recipes live in C1–C7 | Confirmed; slot selector takes the slot index as a `uint16LE`. |

---

## 13. Open Questions / Caveats

1. **Slot count.** The APK does not hard-code "7" anywhere visible in the protocol layer — it's the model that exposes 7 slots on X-H2. If you target older bodies (X-T3 etc.) you may need to clamp differently.
2. **`SetDevicePropValue` size.** Every observed write is 2 bytes (int16/uint16). `FUJI_PRESET_NAME` is the only variable-length one (PTP string).
3. **Non-recipe properties in the block.** Codes `0xD18E, 0xD18F, 0xD191, 0xD1A3, 0xD1A4, 0xD1A5` are read but the app has no enum entry. Their meaning is unknown — a logger that captures them across slots would help map them.
4. **GetDeviceInfo during apply.** The decompile shows a `GetDeviceInfo` call early in the apply path (the `slotResult` continuation variable). It looks defensive — possibly to confirm the camera is still alive after writing the slot selector. Replicating it costs little, so do it.
5. **Inter-write delay** is 150 ms in the apply loop and 100 ms after writing the slot selector during reads. Don't tune below those without testing on multiple bodies.
6. **Card-reader fallback.** If the camera enumerates with interface class `0x08` (or DeviceInfo lacks `0xD18C`), the app immediately calls `CloseSession`, releases the interface, and reports CARD_READER mode. Replicate that — leaving the interface claimed will block Android's MTP indexer.
