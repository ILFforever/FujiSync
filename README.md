# FujiSync

Android app for managing Fujifilm X-series film simulation recipes over USB-C OTG.  
Read C1–C7 from a connected camera, edit them, write them back, and keep a personal library on your phone.

---

## Features

**Camera (USB OTG)**
- Connect any X-series camera in USB RAW CONV. / Backup Restore mode
- Read all 7 custom slots (C1–C7) over PTP — film simulation, grain, tone, colour, sharpness, WB, and more
- Edit any slot in the recipe editor and write it back to the camera
- Slot backup and restore (save the full C1–C7 set to phone, restore in one tap)

**Library**
- Save recipes from the camera or create new ones manually
- Organise into named groups with cover images
- Duplicate detection when saving
- Reference photo attachment per recipe

**Import**
- **Camera JPEG → read EXIF** — drop any unedited JPEG shot on an X-series and the app extracts the embedded recipe from Fujifilm's MakerNote
- **Recipe screenshot → read text** — on-device OCR (Google ML Kit) reads parameter labels from any screenshot: recipe apps, camera menu shots, Instagram posts. Handles full labels (`Highlight Tone: -2`) and abbreviated formats (`H -2, SH +1, NR 0`)

**Discover**
- Browse and save recipes from FXW (Fujifilm X Weekly community feed)

---

## Tech

| Layer | Stack |
|---|---|
| UI | Jetpack Compose + Material 3, dark-only |
| Architecture | MVVM, Hilt DI, single-module |
| Camera comms | USB OTG, PTP/IP (reverse-engineered Fujifilm protocol) |
| EXIF parsing | `com.drewnoakes:metadata-extractor` |
| OCR | `com.google.android.gms:play-services-mlkit-text-recognition` (GMS, download-on-demand) |
| Storage | JSON flat files in `filesDir` |
| Min SDK | 26 (Android 8.0) |

Protocol details are in [`PROTOCOL.md`](PROTOCOL.md) — a living document updated as implementation reveals new information.

---

## Building

```
./gradlew assembleDebug
```

Open the project root in Android Studio and sync Gradle. No API keys or local config required for a debug build.

**Signing (release)**  
Add to `local.properties`:
```
releaseStoreFile=/path/to/keystore.jks
releaseStorePassword=...
releaseKeyAlias=...
releaseKeyPassword=...
```

---

## Project structure

```
app/src/main/java/com/paeki/fujirecipes/
├── data/
│   ├── exif/          JPEG MakerNote parsing → RecipePreset
│   ├── ocr/           ML Kit OCR + regex parser for screenshots
│   ├── local/         JSON persistence (library, slots, settings)
│   ├── mapper/        PTP wire value ↔ display dial conversion
│   ├── ptp/           PTP packet/transaction primitives
│   ├── remote/        FXW community recipe API
│   └── usb/           USB OTG scanning, PTP session, camera read/write
├── domain/model/      RecipePreset, FujiPropertyCode, FujiFilmSimulation
└── ui/
    ├── camera/        Slot board, slot detail, connect guide
    ├── detail/        Recipe detail overlay
    ├── editor/        Recipe editor (create / edit)
    ├── library/       Library screen, groups, sort/filter
    ├── discover/      FXW browse + save
    ├── profile/       Settings, camera labels, import tools
    ├── model/         UI models and mappers
    └── components/    Shared atoms and icons
```
