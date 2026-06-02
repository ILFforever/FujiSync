# Fuji Recipes Android Application — Code Quality Analysis Report

## Overall Architecture Assessment: **A- (87/100)** *(was B+ / 84)*

---

## Project Stats

| Metric | Value | Was |
|--------|-------|-----|
| **Total source files** | 64+ `.kt` files | 62 |
| **Largest file** | `LibraryScreen.kt` (2,351 lines) | 2,539 |
| **Tabs** | 4 (Camera/Library/Discover/Profile) | same |

---

## ✅ Strengths

### 1. Clean Layering
- Clear separation between data, domain, and UI layers
- Domain models properly abstracted (`RecipePreset`, `CameraSlot`, `FujiPropertyCode`)
- Repository pattern implemented (`CameraRepository`, `UsbCameraRepository`)
- PTP protocol logic encapsulated in dedicated package

### 2. PTP Protocol Implementation
- Well-structured PTP handling (`PtpPacket`, `PtpTransaction`, `PtpConstants`)
- Proper USB connection management with resource cleanup (`AutoCloseable`)
- Robust error handling with custom exceptions (`PtpProtocolException`)
- Correct bulk transfer chunking

### 3. Compose UI Quality
- Modern Material 3 with proper theming
- Consistent design system with reusable components (`Atoms.kt`, `FilmSimBadge`)
- Good state hoisting and composable functions
- Proper animations and transitions

### 4. Data Persistence
- `LocalStore` uses tmp+rename atomic write strategy
- Multi-layer caching in `FxwRepository` (memory + disk with ETag support)
- Backup/restore for camera slots

### 5. Domain Models
- Well-defined domain entities with proper enum usage for type safety

### 6. ViewModel Architecture ✅ *Was Fixed*
- `MainViewModel` owns all state and business logic
- `MainActivity` at 205 lines (Activity concerns only)
- `viewModelScope` for all coroutines
- `StateFlow<FujiSyncUiState>` exposed to UI

### 7. Decomposed UI State ✅ *Was Fixed*
- `FujiSyncUiState` composes `CameraUiState` + `LibraryUiState`
- Camera connection, slots, reading progress isolated
- Library recipes, groups, sort, duplicate dialog isolated

### 8. Hilt Dependency Injection ✅ *Was Fixed*
- `@HiltAndroidApp` on `FujiRecipesApplication`
- `AppModule` provides `UsbManager`, `UsbCameraRepository`, `UsbPtpConnection`, `LocalStore`
- `MainViewModel` is `@HiltViewModel @Inject constructor`
- `MainActivity` is `@AndroidEntryPoint`

### 9. Non-blocking I/O ✅ *Was Fixed*
- All `Thread.sleep()` replaced with `delay()` in `FujiRecipeCamera`
- `readPreset`, `writePresetName`, `writeFilmSimulation` are `suspend fun`

### 10. Unit Test Coverage ✅ *Expanded*
- 6 test files, 102+ test methods, 140+ assertions
- PTP encoding/decoding round-trips
- `CameraPresetName` sanitization edge cases
- `RecipePreset.toUiModel()` mapping (21 test methods)
- `LibraryStateHolderTest` — 32 tests across 7 areas (CRUD, dupes, groups, WB normalization, Turbine flow assertions)
- Test deps: `mockk:1.13.12`, `kotlinx-coroutines-test:1.8.1`, `turbine:1.2.0`

### 11. CameraScreen Decomposition ✅ *Was Fixed*
- `CameraScreen.kt` shrank from ~1,336 to 423 lines
- Extracted into `SlotSheetContent.kt` (243), `SlotBackupSheets.kt` (680), `ConnectGuide.kt` (382), `CameraImageTunerScreen.kt` (308)

### 12. LocalStore Thread Safety & Atomicity ✅ *New*
- `Mutex()` added — all public methods wrapped in `mutex.withLock {}`
- `saveLibrary()` writes 3 files inside single lock — no more torn reads from concurrent `loadLibrary()`
- `write()` fallback uses `copyTo(overwrite=true)` + `delete()` — no more `writeText` bypass of temp file
- `copyReferenceImage`/`deleteReferenceImageFile` are `suspend` but intentionally unlocked — UUID filenames, no shared state

### 13. MainViewModel Decomposition ✅ *New*
- `MainViewModel.kt` shrank from 1,277 to 913 lines
- Library data/mutations extracted to `LibraryStateHolder` (503 lines) — owns CRUD, duplicate detection, group management, favorites, sort, persistence, discover download
- Library UI state extracted to `LibraryViewModel` (273 lines) — owns filter/sort/search/selection UI state, combined `LibraryScreenState`, delegates data mutations to holder
- `MainViewModel` retains only camera (14 funs), navigation (8), editor (6), reference images (6), profile/persistence (11), and thin library delegation (10 one-liner pass-throughs)
- No library business logic remains in MainViewModel — all residual references are cross-cutting orchestration (syncing library→detail state, editor→library save)

### 14. LibraryScreen Decomposition ✅ *New*
- 19 `var … by remember { mutableStateOf(…) }` reduced to 1 (pull-to-dismiss gesture distance)
- Function signature reduced from 23 params to 5 (showImages, scrollToTopSignal, onOpenItem, onCreateRecipe, onAddGroupImage)
- All business logic (filtering, sorting, CRUD, persistence, duplicate detection) moved to `LibraryViewModel`/`LibraryStateHolder`
- 7 remaining `mutableStateOf` instances across the file are all pure UI state (animations, gesture tracking, text field buffers)
- Structurally sound at 2,351 lines — all rendering code

### 15. Overlay Stack Extraction ✅ *New*
- 10+ stacked `if`-block overlays extracted to `BoxScope.AppOverlays(…)` — a named private composable at `FujiSyncApp.kt:438`
- 11 overlay blocks now live in `AppOverlays` (lines 473–580): RecipeDetail, WriteToast, ImageTuner, ExifBench, ImportGuide, ExifImport, Editor, DiscardDialog, ReadingOverlay, CameraDetailModal, DuplicateDialog
- `FujiSyncApp` main body (293–434) is now a readable layout skeleton: AppHeader → tab router → AppOverlays call → ChargingBanner → AppTabBar
- Zero inline overlay `if`-blocks remain in the main body

### 16. OverlayStack BackHandler Dispatch ✅ *New*
- `OverlayStack` (`ui/overlay/OverlayStack.kt`, 33 lines) adopted by all 4 BackHandler sites
- FujiSyncApp, CameraScreen, LibraryScreen, ProfileScreen all use `overlayStackOf(…).BackHandler()`
- Single-source priority via `dismissTop()` — no more hand-maintained `when` chains

### 17. Error Handling — PTP Decoding ✅ *New*
- `decodeInt16Le` / `decodeUInt16Le` return `Int?` instead of silent `0` default
- `FujiRecipeCamera` skips properties with null decode (`if (value != null)`)

### 18. FxwRepository Thread Safety ✅ *New*
- `memoryPages` changed from `mutableMapOf()` to `ConcurrentHashMap`

---

## ❌ Issues

### 1. **No Navigation Component** 🟠 **MEDIUM**
**File:** `FujiSyncApp.kt` (1,215 lines)

- Manual state-based navigation, no back stack
- State classes (`FujiSyncUiState`, `CameraUiState`, `LibraryUiState`, `WriteToastState`, `AppTab`, `Screen`) defined in `FujiSyncApp.kt` instead of separate files
- Full screens still inlined (`CameraDetailModal`, `ChargingWarningBanner`, `ExifImportLoadingScreen`)
- No deep linking support

**Recommendation:** Jetpack Navigation Component or typed `Screen` sealed class with `NavController`.

---

### 2. **LocalStore Layer Violation** 🟡 **LOW**
**File:** `LocalStore.kt:7–11`

Data layer imports `ui.model.*` for serialization. Should use data-layer DTOs with mapping functions.

---

### 3. **Error Handling Partially Improved** 🟡 **LOW-MEDIUM**
**Multiple files**

PTP silent `0` defaults fixed — `decodeInt16Le`/`decodeUInt16Le` now return `Int?`, `FujiRecipeCamera` skips null decodes. Remaining:

| Pattern | File | Detail |
|---------|------|--------|
| Silent failure | `FxwRepository.kt` | `runCatching` swallows disk write errors |
| Hardcoded strings | `MainViewModel.kt` | `"USB permission denied."` etc. |
| String-as-error | `readAllSlots` | Returns `"READ FAILED"` instead of typed error |

---

### 5. ~~Code Duplication~~ ✅ *Resolved*

All duplications eliminated:
- `decodeSampledBitmap` — moved to `Atoms.kt:380` as `internal`; consumers import from there
- `cameraImageTuning` + `CameraImageTuning` — `internal` in `CameraScreen.kt:261`; ProfileScreen imports
- `MONO_SIM_CODES` — single `internal val` in `PtpConstants.kt:32`; all 3 consumers import from there
- Sheet color constants — `SheetBg`/`SheetBorder` in `Color.kt:17-18`; 5 files import from theme
- `Wordmark` naming — SplashScreen's renamed to `SplashWordmark`; `Wordmark` in Atoms is unambiguous

---

### 6. **Test Coverage Gaps** 🟠 **MEDIUM**

| Untested area | Lines | Risk |
|---------------|-------|------|
| `MainViewModel.kt` | 913 | **HIGH** — zero tests |
| `FujiRecipeCamera.kt` | 143 | **HIGH** — core recipe read/write PTP |
| `UsbPtpConnection.kt` | 184 | HIGH — USB bulk transfer |
| `PtpDeviceInfo.parseDeviceInfo()` | 122 | HIGH — complex binary parser |
| `FujiExifReader.kt` | 265 | MEDIUM — pure logic, easily testable |
| `LocalStore.kt` | 374 | HIGH — persistence layer |
| `FxwApi.kt` / `FxwRepository.kt` | 288 | MEDIUM — network layer |

Test deps now available: `mockk`, `kotlinx-coroutines-test`, `turbine`. `LibraryStateHolder` fully tested (32 tests).

---

### 7. **No Release Build Configuration** ~~🟠 **MEDIUM**~~ ✅ *Resolved*
- `signingConfigs.release` reads from `local.properties` with env var CI fallback
- `buildTypes.debug` has `.debug` suffix for side-by-side install
- `buildTypes.release` has `isMinifyEnabled`, `isShrinkResources`, ProGuard, signing config
- `proguard-rules.pro` keeps PTP data classes, Hilt ViewModels, metadata-extractor

---

### 8. **Data Layer Thread Safety** 🟡 **LOW-MEDIUM**

| File | Lines | Issue |
|------|-------|-------|
| `CameraHeartbeat.kt` | 28 | `consecutiveFailures` not `@Volatile` — read/written from different threads |
| `UsbPtpConnection.kt` | 69 | `nextTransactionId++` not atomic — safe only due to external mutex |

`FxwRepository.memoryPages` fixed — now `ConcurrentHashMap`.

---

### 9. **Other Notable Issues** 🟡 **LOW**

| Issue | File | Lines |
|-------|------|-------|
| `DiscoverViewModel` uses `AndroidViewModel` | `DiscoverViewModel.kt` | 23 — should be Hilt `ViewModel` |
| Film sim identification via fragile strings | `FujiExifReader.kt` | 153–165 — metadata-extractor label changes break mapping |
| PTP session not closed on error | `FujiPtpProbe.kt` | 58, 63, 67 — early returns skip `closeSession()` |
| User-Agent mismatch | `FxwApi.kt` | 37 — "FujiSync/1.0" vs app name "Fuji Recipes" |
| `CameraRepository` not bound in Hilt | `AppModule.kt` | Callers inject `UsbCameraRepository` directly |
| Domain imports data types | `CameraRepository.kt` | 3–4 — violates clean architecture |
| No cache eviction | `FxwRepository.kt` | 15 — `memoryPages` grows unbounded |
| `showSplash` lost on rotation | `MainActivity.kt` | 65 — `mutableStateOf(true)` resets |
| Hardcoded strings | Multiple | ~200+ strings not in `strings.xml` |
| Magic delay constants | `MainViewModel.kt` | `UiTimings` object added but delays still inline |
| No crash reporting | — | Firebase Crashlytics or Sentry |
| No certificate pinning | `FxwApi.kt` | Raw `HttpURLConnection`, no OkHttp |

---

## 🔧 Remaining Work (Prioritized)

### 🔴 **HIGH Priority**
1. ~~Add test dependencies~~ ✅ *Done*

### 🟠 **MEDIUM Priority**
2. **Adopt Jetpack Navigation** — or typed `Screen` sealed class with `NavController`
3. **Expand test coverage** — `MainViewModel`, `FujiRecipeCamera`, `LocalStore`, `FujiExifReader`
4. **Add input validation** — length + sanitization for library names

### 🟡 **LOW Priority**
5. Move strings to `strings.xml` for i18n
6. Add crash reporting (Crashlytics/Sentry)
7. Certificate pinning for HTTPS (migrate to OkHttp)
8. Bind `CameraRepository` in Hilt, fix domain→data layer violation
9. Migrate `DiscoverViewModel` from `AndroidViewModel` to Hilt `ViewModel`
10. Add `@Volatile`/`AtomicInteger` to `CameraHeartbeat`, `UsbPtpConnection`

---

## 📊 Code Quality Metrics

| Metric | Score | Was | Notes |
|--------|-------|-----|-------|
| **Architecture** | 9/10 | 8/10 | Library extracted; MainViewModel focused on camera/nav/cross-cutting |
| **Code Organization** | 7/10 | 5/10 | Library ViewModel/StateHolder added; overlay extraction clean |
| **State Management** | 8/10 | 7/10 | Library state properly layered (Holder → ViewModel → Screen) |
| **Dependency Injection** | 7/10 | 7/10 | `LibraryViewModel` uses Hilt; `DiscoverViewModel` still `AndroidViewModel` |
| **Error Handling** | 6/10 | 4/10 | PTP decoding returns nullable; silent `0` defaults eliminated |
| **Testability** | 7/10 | 6/10 | Test deps added; LibraryStateHolder fully tested (32 tests) |
| **UI Implementation** | 9/10 | 8/10 | All BackHandlers use OverlayStack; clean single-source priority |
| **Data Layer** | 7/10 | 6/10 | `LocalStore` now thread-safe with Mutex; layer violation remains |
| **Performance** | 8/10 | 8/10 | Non-blocking I/O, caching good |
| **Security** | 6/10 | 5/10 | R8 minification + resource shrinking enabled for release |
| **Maintainability** | 8/10 | 7/10 | All duplications eliminated; clean single-source utilities |
| **Documentation** | 4/10 | 4/10 | No change |

**Overall: 7.5/10 (75%)** *(was 7.2/10)*

---

## 📝 Resolved Issues (Since First Report)

| Issue | File | Resolution |
|-------|------|------------|
| God class `MainActivity` (680 lines) | `MainActivity.kt` | Gutted to ~205 lines; logic in ViewModel |
| No ViewModel | `MainActivity.kt` | `MainViewModel` with `StateFlow` |
| No DI | `MainActivity.kt` | Hilt — `AppModule`, `@HiltViewModel` |
| Blocking `Thread.sleep()` | `FujiRecipeCamera.kt` | Replaced with `suspend` + `delay()` |
| Monolithic UI state | `FujiSyncApp.kt` | `CameraUiState` + `LibraryUiState` |
| Zero test coverage | — | 5 test files, 70 methods, 108 assertions |
| `CameraScreen.kt` 1,336 lines | `CameraScreen.kt` | Split into 4 files; now 423 lines |
| `LocalStore` thread safety | `LocalStore.kt` | `Mutex` on all public methods; `saveLibrary` under single lock; `copyTo`+`delete` fallback |
| God ViewModel 1,277 lines | `MainViewModel.kt` | Library extracted to `LibraryStateHolder` (503) + `LibraryViewModel` (273); now 913 lines, camera/nav/cross-cutting only |
| LibraryScreen 19 mutable states | `LibraryScreen.kt` | All moved to `LibraryViewModel`/`LibraryStateHolder`; 1 UI-only state remains; params 23→5 |
| Overlay BackHandler (all 4 sites) | Multiple | `OverlayStack` + `overlayStackOf(…).BackHandler()` — FujiSyncApp, CameraScreen, LibraryScreen, ProfileScreen |
| Overlay stack extraction | `FujiSyncApp.kt` | Extracted to `BoxScope.AppOverlays(…)` at line 438; main body is clean layout skeleton | All consolidated: `decodeSampledBitmap`→Atoms, `cameraImageTuning`→CameraScreen internal, `MONO_SIM_CODES`→PtpConstants, sheet colors→Color.kt, `SplashWordmark` renamed |
| No release build configuration | `app/build.gradle.kts` | `signingConfigs.release` from local.properties/CI; debug `.debug` suffix; release R8 + shrink + ProGuard rules |
| Overlay stack extraction + BackHandler dispatch | Multiple | `OverlayStack` adopted by all 4 sites (FujiSyncApp, CameraScreen, LibraryScreen, ProfileScreen) |
| PTP silent `0` defaults | `PtpEncoding.kt` | `decodeInt16Le`/`decodeUInt16Le` return `Int?`; `FujiRecipeCamera` skips null decodes |
| `FxwRepository.memoryPages` not thread-safe | `FxwRepository.kt` | Changed to `ConcurrentHashMap` |
| Test dependency gap | `app/build.gradle.kts` | Added `mockk`, `kotlinx-coroutines-test`, `turbine` |
| `LibraryStateHolder` zero tests | — | 32 tests across 7 areas (CRUD, dupes, groups, WB, Turbine) |

---

## 📝 Open Issues

| Issue | File | Severity |
|-------|------|----------|
| No navigation component | `FujiSyncApp.kt` | 🟠 Medium |
| Error handling (hardcoded strings, string-as-error) | `MainViewModel.kt`, `readAllSlots` | 🟡 Low-Medium |
| No input validation | `MainViewModel.kt` | 🟠 Medium |
| Hardcoded strings (~200+) | Multiple | 🟡 Low |
| No crash reporting | — | 🟡 Low |
| No certificate pinning | `FxwApi.kt` | 🟡 Low-Medium |
