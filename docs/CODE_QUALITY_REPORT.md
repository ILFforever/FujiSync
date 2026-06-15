# FujiSync — Code Quality Report

**Last updated:** 2026-06-15

---

## Project Stats

| Metric | Value |
|--------|-------|
| Source files | 103 `.kt` (main) |
| Test files | 13 `.kt` (~2,973 lines) |
| Main source lines | ~29,525 |
| Largest file | `DiscoverScreen.kt` (~1,179 lines) |
| ViewModels | 9 total (5 feature + 4 dev/bench) |
| Min SDK | 26 (Android 8.0) |
| Architecture | MVVM, Hilt DI, single-module, Jetpack Compose + Material 3 |

---

## Scores

| Metric | Score |
|--------|-------|
| Architecture | 9/10 |
| Code Organization | 9/10 |
| State Management | 8/10 |
| Dependency Injection | 9/10 |
| Error Handling | 8/10 |
| Testability | 9/10 |
| UI Implementation | 8/10 |
| Data Layer | 8/10 |
| Performance | 8/10 |
| Security | 9/10 |
| Maintainability | 8/10 |
| Documentation | 5/10 |
| **Overall** | **8.1/10** |

---


## Open Issues

### 🔴 High Priority

| Issue | File | Notes |
|-------|------|-------|
| `FujiRecipeCamera` zero tests | `FujiRecipeCamera.kt` (160 lines) | Core read/write PTP — pure logic, easily testable |
| `FujiExifReader` zero tests | `FujiExifReader.kt` (276 lines) | Pure logic EXIF tag parsing |
| `ImportViewModel` zero tests | `ImportViewModel.kt` (314 lines) | Import orchestration — EXIF/OCR/QR/SmartRef flows |

### 🟠 Medium Priority

| Issue | File | Notes |
|-------|------|-------|
| No navigation component | `FujiSyncApp.kt` | Manual routing; use Jetpack Navigation or typed sealed routes |
| 35 `contentDescription = null` | Multiple | Accessibility: screen reader support missing |
| 178 `.clickable()` without semantics | Multiple | Accessibility: role missing for AT |

### 🟡 Low Priority

| Issue | File | Notes |
|-------|------|-------|
| No crash reporting | — | Crashlytics or Sentry |
| ~200+ hardcoded strings | Multiple | Not in `strings.xml`; no i18n |
| Color constants scattered | Multiple | Consolidate to theme package |

---


## Strengths

### Architecture & DI
- MVVM with 5 feature ViewModels (`MainViewModel`, `CameraViewModel`, `ImportViewModel`, `LibraryViewModel`, `DiscoverViewModel`)
- Hilt DI: `@HiltAndroidApp`, `@HiltViewModel` on all VMs, `@Singleton` on data layer
- `CameraSessionManager` (67 lines) — single-point USB session lifecycle with mutex
- `MainViewModel` reduced from 1,825 → 809 lines (thin coordinator)
- Constructor: 4 params (`appContext`, `localStore`, `libraryHolder`, `releaseUpdater`)

### PTP Protocol
- Well-structured: `PtpPacket`, `PtpTransaction`, `PtpConstants`, `PtpProtocolException`
- USB mutex prevents concurrent access (`CameraHeartbeat.usbMutex` + `CameraSessionManager`)
- `AutoCloseable` + try/finally ensures cleanup
- Bulk transfer chunking, container length validation
- `FujiPtpProbe` probes battery, serial, capabilities before recipe access

### Data Layer
- `LocalStore`: `Mutex` + tmp+rename atomic write — no torn reads
- `FxwRepository`: 3-tier cache (ConcurrentHashMap → disk w/ ETag → network) + LRU eviction
- `FujiValueMapper`: single source of truth for PTP wire ↔ dial conversions
- All `Thread.sleep()` replaced with `suspend` + `delay()`

### UI
- Jetpack Compose + Material 3, dark-only
- `OverlayStack` (27 lines) — single-source back-handler priority, no hand-maintained `when` chains
- `AppOverlays.kt` (471 lines) extracted from FujiSyncApp
- State decomposed: `CameraUiState` + `LibraryUiState` in separate `FujiSyncState.kt`
- Reusable design system: `Atoms.kt`, `FilmSimBadge`, `FujiIcons`

### Import Features
- **JPEG→Recipe**: `FujiExifReader` (276 lines) reads MakerNote EXIF, all 20 film sims
- **Screenshot→Recipe**: `OcrRecipeParser` (926 lines) — ML Kit OCR + geometric stitching + fuzzy matching
- **QR codes**: offline encode/decode, instant import
- **Discover**: FXW community feed with stale-disk fallback

### Test Coverage
- 13 test files, 2,973 lines
- `OcrRecipeParserTest` (1,182 lines), `LibraryStateHolderTest` (373 lines, 35 tests)
- `CameraViewModelTest` (287 lines, 22 tests) — sync + async via `@IoDispatcher`
- `RecipePresetMapperTest` (258), `LocalStoreTest` (211), `FxwApiParseTest` (189)
- `PtpPacketTest`, `CameraPresetNameTest`, `PtpEncodingTest`, `FxwRecipeTest`, `RecipeQrTest`, `PtpStringRoundTripTest`, `GitHubReleaseUpdaterTest`
- Deps: `mockk:1.13.12`, `kotlinx-coroutines-test:1.8.1`, `turbine:1.2.0`

---


## Resolved Issues

| Issue | Resolution |
|-------|------------|
| God class `MainActivity` (680 lines) | Now 365 lines; logic in ViewModels |
| No ViewModel / No DI | Hilt + 9 `@HiltViewModel` classes |
| `MainViewModel` god ViewModel (1,825 lines) | Split → CameraViewModel (702) + ImportViewModel (314); now 809 lines |
| Blocking `Thread.sleep()` | `suspend` + `delay()` throughout |
| Monolithic UI state | Decomposed: `FujiSyncState.kt`, `CameraUiState`, `LibraryUiState` |
| `FujiSyncApp.kt` 1,616 lines | Extracted `AppOverlays.kt` (471), `FujiSyncState.kt` (123); now 515 lines |
| Zero test coverage | 13 test files, ~2,973 lines |
| `CameraScreen.kt` 1,336 lines | Split into ConnectGuide (403), SlotBackupSheets (1,484), CameraImageTunerScreen (351) |
| `LocalStore` no thread safety | `Mutex` + atomic write (tmp+rename) |
| LibraryScreen 19 mutable states | All in `LibraryViewModel`/`LibraryStateHolder`; 1 UI-only remains |
| Overlay BackHandler chaos | `OverlayStack` single-source priority |
| Code duplication | Consolidated: `decodeSampledBitmap`→Atoms, `MONO_SIM_CODES`→PtpConstants |
| No release build config | `signingConfigs.release`, R8 + shrink |
| PTP silent `0` defaults | `decodeInt16Le`/`decodeUInt16Le` return `Int?` |
| `FxwRepository` not thread-safe | `ConcurrentHashMap` + LRU eviction (`MAX_MEMORY_PAGES`) |
| `CameraHeartbeat.consecutiveFailures` not thread-safe | `AtomicInteger` |
| `UsbPtpConnection.nextTransactionId` not atomic | `AtomicInteger.getAndIncrement()` |
| PTP session leak in `FujiPtpProbe` | try/finally + `.use {}` |
| `CameraRepository` not bound in Hilt | `AppModule.provideCameraRepository()` returns interface |
| `CameraViewModel` async paths not testable | `@IoDispatcher` qualifier; all `Dispatchers.IO` replaced |
| `DiscoverViewModel` used `AndroidViewModel` | `@HiltViewModel` with `@ApplicationContext` |
| `upload-keystore.jks` in git | FALSE ALARM — `.jks` in `.gitignore`, not tracked |
| `UsbPtpConnection` concurrent corruption | NOT AN ISSUE — external `usbMutex` serializes all callers |
| CameraViewModel ~30 hardcoded error strings | Moved to `strings.xml`; `@ApplicationContext` injected; i18n ready |
| `LibraryScreen.kt` 2,728 lines monolith | Split into 641 lines + 5 component files under `library/components/` (Constants, Filter, Group, List, Drawer) — 76% reduction |
| `SlotBackupSheets.kt` 1,425 lines (4 sheets) | Split into BackupSheet (368), RestoreSheet (524), RenameSheet (219), SaveAllSheet (426), shared RestoreReadingContent (186) |
| `RecipeDetailScreen.kt` 1,500 lines | Split into RecipeDetailScreen (665), RecipeReferenceImage (483), RecipeQrSheet (271), SyncToCameraSheet (290) |
| `RecipeEditorScreen.kt` 1,184 lines | Split into RecipeEditorScreen (582), EditorControls (441), EditorRecipeBuilder (202) |
| `selectSlot()` return value ignored in `writePresetName` | `if (!selectSlot(slot)) return false` guard added — consistent with all other write methods |
| `LocalStore.loadLibrary()` silently fails on parse error | `getOrThrow()` propagates exception; `LibraryStateHolder` catches and sets `loadError` in `LibraryUiState`; library screen shows "LIBRARY UNAVAILABLE" instead of empty state |
| No cleartext traffic blocking + unbounded API response | `network_security_config.xml` added (`cleartextTrafficPermitted="false"`); `FxwApi` response body capped at 4 MB; image download already had protections (was a stale finding) |
| `rdbg()` logging in production | `BuildConfig.DEBUG` guard added — log accumulation now no-ops in release builds |

---


## Next Steps

1. **Write tests** for `FujiRecipeCamera`, `FujiExifReader`, `ImportViewModel`
2. **Create `EditorStateHolder`** for `RecipeEditorScreen` (25+ `mutableStateOf` → proper state holder)
3. **Add accessibility** — `contentDescription` on icons, semantic roles on clickables
