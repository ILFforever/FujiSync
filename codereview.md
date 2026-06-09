FujiSync Code Quality Report
  
  🔴 Critical — Would Immediately Fail Code Review
  
  1. MainViewModel.kt (1725 lines, 65 public functions) — God ViewModel
  
  Your entire app's state and logic lives in one ViewModel: USB connection, PTP I/O, EXIF parsing, OCR, QR import, library management, app updates, APK
  download/install, backup export, navigation, settings. It injects 8 dependencies and exposes a single flat FujiSyncUiState data class that triggers full-tree
  recomposition on any change.
  
  2. LibraryScreen.kt (2698 lines) — Extreme Monolith
  
  32 composable functions in one file. Contains BitmapFactory.decodeStream() in remember blocks (blocks composition thread), 24 hardcoded colors, 250 inline dp
  values, coroutine dispatchers used directly in composable scope.
  
  3. FujiSyncApp.kt (2020 lines, 60+ callback parameters) — God Composable
  
  Manual screen routing, 15+ remember state holders, nested logic functions, and an AppOverlays helper with 50+ parameters. This is doing the job of Navigation
  Compose by hand.
  
  4. OcrRecipeParser.kt (560 lines, 40+ functions, 30+ regex) — Monolith Object
  
  Tightly couples ML Kit integration with text parsing. Many regex patterns recompiled on each function call instead of being hoisted to object-level vals.
  
  ────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
  
  🟠 High — Significant Architecture Issues
  
  5. LocalStore.kt — God Class (SRP Violation)
  
  Handles library persistence, slot backups, reference images (bitmap decode/resize), settings, camera labels, and export/import serialization. Should be 4+ focused
  classes. Contains code duplication (3 identical load/save methods differing only by filename).
  
  6. Thread Safety in UsbPtpConnection
  
  nextTransactionId is atomic but executeCommand/executeCommandWithData are not synchronized. Concurrent coroutine access would corrupt the PTP session with
  interleaved send/receive calls.
  
  7. Silent Error Swallowing
  
  LocalStore.loadLibrary() uses runCatching { }.getOrNull() — if deserialization fails, the user silently loses their entire library. Same pattern in FxwRepository
   cache loading.
  
  8. FujiRecipeCamera.readPreset Ignores selectSlot Failure
  
  selectSlot() returns Boolean but the caller never checks it. If slot selection fails, reads return data from the wrong slot → silent data corruption pushed back to
  the user.
  
  9. DiscoverScreen Directly Injects MainViewModel
  
  Cross-screen coupling: mainViewModel: MainViewModel = hiltViewModel() in DiscoverScreen calls mainViewModel.handleSaveFromDiscover(). Makes isolated testing
  impossible.
  
  10. RecipeEditorScreen — State Explosion Without ViewModel
  
  20+ individual remember { mutableStateOf(...) } variables with no ViewModel. Cannot survive process death. 25-key remember block causes high recomposition
  frequency.
  ────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
  
  🟡 Medium — Would Get Comments in Review
  
  ┌─────────────────────────────────────────────────────────────────────┬─────────────────────────────────────────────────────────────────┐
  │ Issue                                                               │ Scope                                                           │
  ├─────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────┤
  │ Zero string resources — all text hardcoded, localization impossible │ Project-wide                                                    │
  ├─────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────┤
  │ 71 inline Color(0x...) outside theme                                │ LibraryScreen (24), FujiSyncApp (9), SlotBackupSheets (4), etc. │
  ├─────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────┤
  │ 33 contentDescription = null on images/icons                        │ Accessibility failure                                           │
  ├─────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────┤
  │ 135 .clickable() without role or semantics                          │ Accessibility                                                   │
  ├─────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────┤
  │ No interface for LocalStore or OcrRecipeParser                      │ Untestable                                                      │
  ├─────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────┤
  │ Hardcoded magic numbers in FujiExifReader (192, 224, 1024…)         │ Undocumented EXIF values                                        │
  ├─────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────┤
  │ FxwRepository/FxwApi are singleton objects                          │ Untestable without DI                                           │
  ├─────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────┤
  │ User-Agent hardcoded to "1.0"                                       │ FxwApi.kt                                                       │
  ├─────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────┤
  │ Incomplete HTML entity decoder (10 entities only)                   │ FxwApi.kt                                                       │
  ├─────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────┤
  │ FujiPropertyCode.fromCode() is O(n) linear scan                     │ Called in loops (19×19 iterations)                              │
  ├─────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────┤
  │ upload-keystore.jks committed to git                                │ Security risk                                                   │
  └─────────────────────────────────────────────────────────────────────┴─────────────────────────────────────────────────────────────────┘
  
  ────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
  
  🟡 Structural Smells
  
  - Single module — no separation between :data, :domain, :ui modules. Everything compiles together, no compile-time dependency boundaries.
  - LibraryStateHolder shared between two ViewModels — dual ownership creates potential race conditions.
  - No use-case layer — ViewModels directly call data classes, bypassing any domain logic abstraction.
  - No navigation framework — manual AppTab + overlay state enum managed in ViewModel instead of Navigation Compose.