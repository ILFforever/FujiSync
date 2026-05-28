# Fuji Recipes

Android app scaffold for managing Fujifilm custom shooting recipes over USB OTG.

The protocol details live in `PROTOCOL.md`. The app code is intentionally small at this stage:

- `app/src/main/java/com/paeki/fujirecipes/data/ptp` contains PTP constants and packet helpers.
- `app/src/main/java/com/paeki/fujirecipes/data/usb` contains USB scanning and PTP endpoint discovery.
- `app/src/main/java/com/paeki/fujirecipes/domain/model` contains C-slot and recipe property models.
- `app/src/main/java/com/paeki/fujirecipes/MainActivity.kt` is a Compose shell for camera detection and C1-C7 slots.

Open this folder in Android Studio and sync Gradle. The first hardware milestone is `FujiPtpProbe`: it claims the PTP interface, opens session `1`, reads `GetDeviceInfo`, and verifies that property `0xD18C` is advertised before any recipe read/write work starts.
