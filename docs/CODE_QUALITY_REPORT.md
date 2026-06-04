# Fuji Recipes Android Application — Code Quality Analysis Report

## Overall Architecture Assessment: **A (90/100)** *(was A- / 87)*

---

## Project Stats

| Metric | Value | Was |
|--------|-------|-----|
| **Total source files** | 76 `.kt` files (main) | 64+ |
| **Test files** | 9 `.kt` files (1,972 lines) | 6 |
| **Largest file** | `LibraryScreen.kt` (2,589 lines) | 2,483 |
| **Total main source lines** | 20,454 | ~19,000 |
| **Tabs** | 4 (Camera/Library/Discover/Profile) | same |
| **Capture/OCR** | 4 files (OcrCaptureService, OcrCaptureActivity, OcrTileService, CaptureDiag) | — |
| **OCR parser** | `OcrRecipeParser.kt` (715 lines) with ML Kit + geometric stitching | — |

---

## ✅ Strengths

### 1. Clean Layering
- Clear separation between data, domain, and UI layers
- Domain models properly abstracted (`RecipePreset`, `CameraSlot`, `FujiPropertyCode`)
- Repository pattern implemented (`CameraRepository`, `UsbCameraRepository`)
- PTP protocol logic encapsulated in dedicated package
- Value mapper (`FujiValueMapper`) canonicalizes PTP wire ↔ dial conversions — single source of truth

### 2. PTP Protocol Implementation
- Well-structured PTP handling (`PtpPacket`, `PtpTransaction`, `PtpConstants`)
- Proper USB connection management with resource cleanup (`AutoCloseable`)
- Robust error handling with custom exceptions (`PtpProtocolException`)
- Correct bulk transfer chunking with `MAX_SMALL_CONTAINER_BYTES` guard
- `PtpDeviceInfo` parser validates lengths before reads, throws `PtpProtocolException` on overrun
- `FujiPtpProbe` probes camera capabilities, battery, and serial before recipe access

### 3. Compose UI Quality
- Modern Material 3 with proper theming
- Consistent design system with reusable components (`Atoms.kt`, `FilmSimBadge`, `FujiIcons`)
- Good state hoisting and composable functions
- Proper animations and transitions
- `OverlayStack` pattern provides single-source back-handler priority across all 4 screens

### 4. Data Persistence
- `LocalStore` uses tmp+rename atomic write strategy with `Mutex` for thread safety
- Multi-layer caching in `FxwRepository` (memory `ConcurrentHashMap` + disk with ETag/Last-Modified)
- Backup/restore for camera slots with metadata
- Settings, camera labels, models, firmwares all persisted

### 5. Domain Models
- Well-defined domain entities with proper enum usage for type safety
- `FujiPropertyCode.signed` distinguishes signed/unsigned PTP properties
- `FujiFilmSimulation` covers all 20 film simulation modes

### 6. ViewModel Architecture ✅
- `MainViewModel` owns camera, navigation, editor, reference images, profile/persistence
- `MainActivity` at 264 lines (Activity concerns only)
- `viewModelScope` for all coroutines
- `StateFlow<FujiSyncUiState>` exposed to UI
- `MainViewModelEvent` sealed class for one-shot UI events (USB permission, pickers)

### 7. Decomposed UI State ✅
- `FujiSyncUiState` composes `CameraUiState` + `LibraryUiState`
- Camera connection, slots, reading progress isolated
- Library recipes, groups, sort, duplicate dialog isolated
- `LibraryScreenState` derives from `LibraryUiState` + `LibraryScreenUiState` via `combine()`

### 8. Hilt Dependency Injection ✅
- `@HiltAndroidApp` on `FujiRecipesApplication`
- `AppModule` provides `UsbManager`, `UsbCameraRepository`, `UsbPtpConnection`, `CameraHeartbeat`, `LocalStore`
- `MainViewModel` is `@HiltViewModel @Inject constructor`
- `LibraryStateHolder` is `@Singleton @Inject constructor`
- `LibraryViewModel` is `@HiltViewModel`
- `MainActivity` is `@AndroidEntryPoint`

### 9. Non-blocking I/O ✅
- All `Thread.sleep()` replaced with `delay()` in `FujiRecipeCamera`
- `readPreset`, `writePresetName`, `writeFilmSimulation` are `suspend fun`
- `writePreset` accepts configurable `propertyWriteDelayMs` for timing-sensitive cameras

### 10. Unit Test Coverage ✅ *Expanded*
- 9 test files, 1,972 test lines
- `OcrRecipeParserTest` — 956 lines; comprehensive regex, column stitching, edge cases
- `LibraryStateHolderTest` — 368 lines; CRUD, dupes, groups, WB normalization, Turbine flow assertions
- `RecipePresetMapperTest` — 189 lines; `RecipePreset.toUiModel()` round-trip mapping
- `PtpPacketTest` — 118 lines; container parse/build round-trips
- `CameraPresetNameTest` — 101 lines; sanitization edge cases
- `PtpEncodingTest` — 98 lines; string encoding, hex dump
- `LocalStoreTest` — 60 lines; atomic write, read-back, thread-safety
- `FxwRecipeTest` — 47 lines; FXW recipe model parsing
- `PtpStringRoundTripTest` — 36 lines; PTP string encode/decode
- Test deps: `mockk:1.13.12`, `kotlinx-coroutines-test:1.8.1`, `turbine:1.2.0`

### 11. CameraScreen Decomposition ✅
- `CameraScreen.kt` at 526 lines
- Extracted into `SlotSheetContent.kt` (243), `SlotBackupSheets.kt` (1,156), `ConnectGuide.kt` (382), `CameraImageTunerScreen.kt` (308)

### 12. LocalStore Thread Safety & Atomicity ✅
- `Mutex()` added — all public methods wrapped in `mutex.withLock {}`
- `saveLibrary()` writes 3 files inside single lock — no torn reads from concurrent `loadLibrary()`
- `write()` fallback uses `copyTo(overwrite=true)` + `delete()` — no raw `writeText` bypass
- `copyReferenceImage`/`deleteReferenceImageFile` are `suspend` but intentionally unlocked — UUID filenames, no shared state

### 13. MainViewModel Decomposition ✅
- `MainViewModel.kt` at 1,058 lines
- Library data/mutations extracted to `LibraryStateHolder` (546 lines) — owns CRUD, duplicate detection, group management, favorites, sort, persistence, discover download
- Library UI state extracted to `LibraryViewModel` (268 lines) — owns filter/sort/search/selection UI state, combined `LibraryScreenState`, delegates data mutations to holder
- `MainViewModel` retains camera (14 funs), navigation (8), editor (6), reference images (6), profile/persistence (11), and thin library delegation

### 14. LibraryScreen Decomposition ✅
- All business logic (filtering, sorting, CRUD, persistence, duplicate detection) in `LibraryViewModel`/`LibraryStateHolder`
- 1 remaining `mutableStateOf` for pull-to-dismiss gesture distance (pure UI state)
- Function signature: 5 params (showImages, scrollToTopSignal, onOpenItem, onCreateRecipe, onAddGroupImage)
- Structurally sound at 2,589 lines — all rendering code

### 15. Overlay Stack Extraction ✅
- 10+ stacked `if`-block overlays extracted to `BoxScope.AppOverlays(…)` in `FujiSyncApp.kt`
- `OverlayStack` (`ui/overlay/OverlayStack.kt`, 33 lines) adopted by all 4 BackHandler sites
- Single-source priority via `dismissTop()` — no hand-maintained `when` chains

### 16. Error Handling — PTP Decoding ✅
- `decodeInt16Le` / `decodeUInt16Le` return `Int?` instead of silent `0` default
- `FujiRecipeCamera` skips properties with null decode (`if (value != null)`)

### 17. FxwRepository Thread Safety ✅
- `memoryPages` changed from `mutableMapOf()` to `ConcurrentHashMap`

### 18. OCR/Screenshot Import ✅ *New*
- `OcrRecipeParser.kt` (715 lines) — full ML Kit OCR pipeline
- Geometric column stitching via bounding-box analysis (language-agnostic)
- Text-heuristic fallback (`stitchFujiRecipeOrder`, `stitchColumnLayout`)
- Regex-based parsing for all Fuji recipe parameters (film sim, DR, grain, WB, tone, clarity, etc.)
- Fuzzy film simulation matching via Levenshtein similarity (≥72% threshold)
- `OcrParseResult.unmatchedFields` tells users exactly which fields failed
- OCR tile quick-capture: `OcrTileService` → `OcrCaptureService` → `OcrCaptureActivity` → broadcast result

### 19. EXIF Import ✅
- `FujiExifReader.kt` (268 lines) — reads MakerNote from Fujifilm JPEGs via metadata-extractor
- Handles all 20 film simulations including `Unknown (2816)` → Reala Ace
- Maps raw EXIF tag values to PTP wire values through `FujiValueMapper`
- Separate `RecipeFromExif` preserves Float precision for Highlight/Shadow Tone (0.5-step)

### 20. Discover/FXW Integration ✅
- `FxwApi` fetches Fuji X Weekly recipes via WordPress REST API
- `FxwRepository` provides 3-tier caching: memory → disk → network (with ETag/304 support)
- `DiscoverScreen` (840 lines) + `DiscoverViewModel` (88 lines, `@HiltViewModel`)
- Stale-disk fallback serves cached data when offline

### 21. Dev/Bench Tools ✅
- `NameBench.kt` — validates preset name encoding across all 7 slots
- `WriteDelayBench.kt` — benchmarks write reliability at different inter-property delays
- `ExifBenchScreen.kt` — raw EXIF tag dump for reverse-engineering
- `DevToolsScreen.kt` (765 lines) — mock cameras, capture log, bench tools

---

## ❌ Issues

### 1. **No Navigation Component** 🟠 **MEDIUM**
**File:** `FujiSyncApp.kt` (1,616 lines)

- Manual state-based navigation, no back stack
- State classes (`FujiSyncUiState`, `CameraUiState`, `LibraryUiState`, `WriteToastState`, `AppTab`, `Screen`) defined in `FujiSyncApp.kt` instead of separate files
- No deep linking support
- `FujiSyncApp.kt` remains the largest UI file at 1,616 lines — complex overlay management

**Recommendation:** Jetpack Navigation Component or typed `Screen` sealed class with `NavController`.

---

### 2. **LocalStore Layer Violation** 🟡 **LOW**
**File:** `LocalStore.kt:7–11`

Data layer imports `ui.model.*` for serialization. Should use data-layer DTOs with mapping functions.

---

### 3. **Error Handling Partially Improved** 🟡 **LOW-MEDIUM**
**Multiple files**

| Pattern | File | Detail |
|---------|------|--------|
| Silent failure | `FxwRepository.kt:119` | `runCatching` swallows disk write errors |
| Hardcoded strings | `MainViewModel.kt` | `"USB permission denied."`, `"Camera not found…"`, etc. (~30 strings) |
| String-as-error | `MainViewModel.kt:283` | Returns `"READ FAILED"` instead of typed error |
| String-as-error | `readAllSlots` | `RecipeUiModel(name = "READ FAILED", sim = "—")` — uses UI model for error state |

---

### 4. **Large Files** 🟡 **LOW-MEDIUM**

| File | Lines | Concern |
|------|-------|---------|
| `LibraryScreen.kt` | 2,589 | Rendering only but unwieldy; could extract recipe card, group card, filter sheet |
| `FujiSyncApp.kt` | 1,616 | Overlay management + state definitions; could extract state classes to separate files |
| `SlotBackupSheets.kt` | 1,156 | Backup/restore UI; could split backup sheet from restore sheet |
| `OcrRecipeParser.kt` | 715 | Complex but cohesive; regex count is inherent to the domain |

---

### 5. **Test Coverage Gaps** 🟠 **MEDIUM**

| Untested area | Lines | Risk |
|---------------|-------|------|
| `MainViewModel.kt` | 1,058 | **HIGH** — zero tests |
| `FujiRecipeCamera.kt` | 137 | **HIGH** — core recipe read/write PTP |
| `UsbPtpConnection.kt` | 159 | HIGH — USB bulk transfer |
| `FujiExifReader.kt` | 268 | MEDIUM — pure logic, easily testable |
| `FxwApi.kt` / `FxwRepository.kt` | 136+152 | MEDIUM — network layer |
| `RecipePresetMapper.kt` | 209 | MEDIUM — round-trip partially tested via `RecipePresetMapperTest` |
| `DiscoverViewModel.kt` | 88 | LOW — thin wrapper |

Test deps available: `mockk`, `kotlinx-coroutines-test`, `turbine`. OCR parser, LibraryStateHolder, and LocalStore fully tested.

---

### 6. **Other Notable Issues** 🟡 **LOW**

| Issue | File | Lines |
|-------|------|-------|
| `DiscoverViewModel` uses `AndroidViewModel` | `DiscoverViewModel.kt` | 88 — **RESOLVED** — now `@HiltViewModel` with `@ApplicationContext` |
| Film sim identification via fragile strings | `FujiExifReader.kt` | 153–181 — metadata-extractor label changes break mapping |
| PTP session not closed on error | `FujiPtpProbe.kt` | 58, 63, 67 — early returns skip `closeSession()` |
| User-Agent mismatch | `FxwApi.kt` | 37 — `"FujiSync/1.0 Android"` vs app name "Fuji Recipes" |
| `CameraRepository` not bound in Hilt | `AppModule.kt` | Callers inject `UsbCameraRepository` directly |
| Domain imports data types | `CameraRepository.kt` | 3–4 — violates clean architecture |
| No cache eviction | `FxwRepository.kt` | 15 — `memoryPages` grows unbounded |
| `showSplash` lost on rotation | `MainActivity.kt` | 83 — `mutableStateOf(true)` resets |
| Hardcoded strings | Multiple | ~200+ strings not in `strings.xml` |
| No crash reporting | — | Firebase Crashlytics or Sentry |
| No certificate pinning | `FxwApi.kt` | Raw `HttpURLConnection`, no OkHttp |
| `CameraHeartbeat.consecutiveFailures` not `@Volatile` | `CameraHeartbeat.kt` | 33 — read/written from different coroutines |
| `UsbPtpConnection.nextTransactionId++` not atomic | `UsbPtpConnection.kt` | 69 — safe only due to external mutex |
| `LibraryStateHolder.scope` never cancelled | `LibraryStateHolder.kt` | 70 — `SupervisorJob()` leak; should tie to ViewModel lifecycle |
| `FxwRecipe.pillLabels()` uses different pill format | `FxwRecipe.kt` | 33–46 — diverges from `RecipePresetMapper.kt` pill format |
| Discover image download uses raw `URL.openStream()` | `LibraryStateHolder.kt` | 366 — no timeout, no size limit, no HTTPS enforcement |
| `SlotBackupSheets.kt` at 1,156 lines | — | Largest sheet file; could split backup/restore |

---

## 🔧 Remaining Work (Prioritized)

### 🔴 **HIGH Priority**
1. Expand test coverage — `MainViewModel`, `FujiRecipeCamera`, `FujiExifReader`
2. Fix `LibraryStateHolder.scope` lifecycle — tie to ViewModel or use `ProcessLifecycleOwner`

### 🟠 **MEDIUM Priority**
3. Adopt Jetpack Navigation — or typed `Screen` sealed class with `NavController`
4. Extract `FujiSyncApp.kt` state classes to separate files
5. Break up `LibraryScreen.kt` — extract recipe card, group card, filter sheet composables
6. Add crash reporting (Crashlytics/Sentry)

### 🟡 **LOW Priority**
8. Move strings to `strings.xml` for i18n
9. Certificate pinning for HTTPS (migrate to OkHttp)
10. Bind `CameraRepository` in Hilt, fix domain→data layer violation
11. Add `@Volatile`/`AtomicInteger` to `CameraHeartbeat`, `UsbPtpConnection`
12. Fix PTP session leak in `FujiPtpProbe` early returns
13. Add cache eviction to `FxwRepository.memoryPages`
14. Normalize pill labels between `FxwRecipe.pillLabels()` and `RecipePresetMapper`
15. Add timeout/size-limit to discover image downloads

---

## 📊 Code Quality Metrics

| Metric | Score | Was | Notes |
|--------|-------|-----|-------|
| **Architecture** | 9/10 | 9/10 | OCR + EXIF + Discover integrated cleanly; `LibraryStateHolder` scope lifecycle concern |
| **Code Organization** | 8/10 | 7/10 | OCR/EXIF/capture packages well-structured; state classes still in FujiSyncApp |
| **State Management** | 8/10 | 8/10 | Library properly layered; `showSplash` lost on rotation |
| **Dependency Injection** | 9/10 | 8/10 | All ViewModels + StateHolder use Hilt; `DiscoverViewModel` migrated to `@HiltViewModel` |
| **Error Handling** | 7/10 | 6/10 | PTP decoding nullable; OCR has unmatched field tracking; hardcoded error strings remain |
| **Testability** | 8/10 | 8/10 | OCR parser (956 lines) + LibraryStateHolder (368) + LocalStore (60) + mapper (189) tested; ViewModel/USB still untested |
| **UI Implementation** | 9/10 | 9/10 | OverlayStack clean; OCR/EXIF import flows polished |
| **Data Layer** | 7/10 | 7/10 | LocalStore thread-safe; FxwRepository concurrent; layer violation remains; no eviction |
| **Performance** | 8/10 | 8/10 | Non-blocking I/O, caching good, reference image downscaling |
| **Security** | 6/10 | 6/10 | R8 minification; no certificate pinning; raw HTTP for discover images |
| **Maintainability** | 8/10 | 8/10 | All duplications eliminated; clean single-source utilities; some large files remain |
| **Documentation** | 4/10 | 4/10 | No change |

**Overall: 7.6/10 (76%)** *(was 7.5/10)*

---

## 📝 Resolved Issues (Since First Report)

| Issue | File | Resolution |
|-------|------|------------|
| God class `MainActivity` (680 lines) | `MainActivity.kt` | Gutted to ~264 lines; logic in ViewModel |
| No ViewModel | `MainActivity.kt` | `MainViewModel` with `StateFlow` |
| No DI | `MainActivity.kt` | Hilt — `AppModule`, `@HiltViewModel` |
| Blocking `Thread.sleep()` | `FujiRecipeCamera.kt` | Replaced with `suspend` + `delay()` |
| Monolithic UI state | `FujiSyncApp.kt` | `CameraUiState` + `LibraryUiState` |
| Zero test coverage | — | 7 test files, ~1,881 lines of tests |
| `CameraScreen.kt` 1,336 lines | `CameraScreen.kt` | Split into 4 files; now 526 lines |
| `LocalStore` thread safety | `LocalStore.kt` | `Mutex` on all public methods; atomic write |
| God ViewModel 1,277 lines | `MainViewModel.kt` | Library extracted to `LibraryStateHolder` (546) + `LibraryViewModel` (268); now 1,058 lines |
| LibraryScreen 19 mutable states | `LibraryScreen.kt` | All moved to `LibraryViewModel`/`LibraryStateHolder`; 1 UI-only state remains |
| Overlay BackHandler (all 4 sites) | Multiple | `OverlayStack` + `overlayStackOf(…).BackHandler()` |
| Overlay stack extraction | `FujiSyncApp.kt` | Extracted to `BoxScope.AppOverlays(…)` |
| Code duplication | Multiple | All consolidated: `decodeSampledBitmap`→Atoms, `MONO_SIM_CODES`→PtpConstants, sheet colors→Color.kt |
| No release build configuration | `app/build.gradle.kts` | `signingConfigs.release`; debug `.debug` suffix; release R8 + shrink + ProGuard |
| PTP silent `0` defaults | `PtpEncoding.kt` | `decodeInt16Le`/`decodeUInt16Le` return `Int?` |
| `FxwRepository.memoryPages` not thread-safe | `FxwRepository.kt` | Changed to `ConcurrentHashMap` |
| Test dependency gap | `app/build.gradle.kts` | Added `mockk`, `kotlinx-coroutines-test`, `turbine` |
| `LibraryStateHolder` zero tests | — | 383 lines of tests (CRUD, dupes, groups, WB, Turbine) |
| No OCR/screenshot import | — | Full ML Kit OCR pipeline with geometric stitching + regex parsing (715 lines, 956 test lines) |
| No EXIF recipe import | — | `FujiExifReader` (268 lines) + `RecipeFromExif` mapper; all 20 film sims mapped |
| No Discover/FXW integration | — | `FxwApi` + `FxwRepository` (3-tier cache) + `DiscoverScreen` + `DiscoverViewModel` |
| No camera heartbeat/monitoring | — | `CameraHeartbeat` (75 lines) with USB mutex, auto-reconnect, slot refresh |
| No dev/bench tools | — | `NameBench`, `WriteDelayBench`, `ExifBenchScreen`, `DevToolsScreen` |
| No OCR tile capture | — | `OcrTileService` + `OcrCaptureService` + `OcrCaptureActivity` + `CaptureDiag` |
| `DiscoverViewModel` used `AndroidViewModel` | `DiscoverViewModel.kt` | Migrated to `@HiltViewModel` with `@ApplicationContext` |
| `LocalStore` zero tests | — | `LocalStoreTest` (60 lines); atomic write, read-back, thread-safety |
| No FXW recipe model tests | — | `FxwRecipeTest` (47 lines) |

---

## 📝 Open Issues

| Issue | File | Severity |
|-------|------|----------|
| No navigation component | `FujiSyncApp.kt` | 🟠 Medium |
| `MainViewModel` zero tests | `MainViewModel.kt` | 🔴 High |
| `FujiRecipeCamera` zero tests | `FujiRecipeCamera.kt` | 🔴 High |
| Error handling (hardcoded strings, string-as-error) | `MainViewModel.kt`, `readAllSlots` | 🟡 Low-Medium |
| `LibraryStateHolder.scope` lifecycle leak | `LibraryStateHolder.kt` | 🟠 Medium |
| Hardcoded strings (~200+) | Multiple | 🟡 Low |
| No crash reporting | — | 🟡 Low |
| No certificate pinning | `FxwApi.kt` | 🟡 Low-Medium |
| Discover image download — no timeout/size limit | `LibraryStateHolder.kt` | 🟡 Low-Medium |
| `FujiSyncApp.kt` 1,616 lines | `FujiSyncApp.kt` | 🟡 Low-Medium |
| `SlotBackupSheets.kt` 1,156 lines | `SlotBackupSheets.kt` | 🟡 Low |
| PTP session leak on probe error | `FujiPtpProbe.kt` | 🟡 Low |
