# Fuji Recipes

Native Android app (Jetpack Compose + Material 3, package `com.ilfforever.fujisync`) for managing
Fujifilm X-series film-simulation recipes over USB-C OTG (PTP). Community protocol documentation
is at https://github.com/ILFforever/fujifilm-ptp-recipes.

## Design Context

### Users
Fujifilm X-series shooters (enthusiast/pro) managing the camera's 7 custom slots (C1–C7) plus a
personal phone library. Connected via USB-C OTG in `USB RAW CONV./BACKUP RESTORE` mode. v1 jobs:
read C1–C7, edit & write recipes back to the camera, manage a phone recipe library.

### Brand Personality
Premium, editorial, focused. A precision tool — darkroom / light-meter calm. Terse, technical,
human. No clutter or gimmicks.

### Aesthetic Direction
- Dark only. Warm near-black background (`~#0D0D0D`); panels lifted by tone, not shadow.
- **Single** warm gold/brass accent (`~#C99A4E`) for all accents — never a second accent color.
- Large bold UPPERCASE recipe names; letter-spaced uppercase section labels; mixed-weight wordmark.
- Dark pill summary chips; label-left / value-right property rows with small gold line-icons;
  sticky bottom primary CTA.
- Anti-references: default Material light/purple, busy multi-color dashboards, skeuomorphic kitsch.

### Differentiation — structure, not color
Identity comes from **page structure**.
Structure = **Slot-board first**: home (`MY CAMERA`) shows C1–C7 as a board of what's loaded now.
Nav = 3 tabs: `CAMERA · LIBRARY · PROFILE`. Screens: Slot Board → Slot Detail → Recipe Editor
(shared) → Library → Recipe Detail → Profile. Flows: read, write/edit, manage library.

### Design Principles
1. The camera is the spine — orient around C1–C7 and read → edit → write.
2. One accent, used sparingly — hierarchy via weight/size/space, not color.
3. Content over chrome — big type, dark space, the recipe data is the hero.
4. State honesty — always show connection state and whether content reflects camera, library, or
   unsaved edits.
5. Diverge by structure, not decoration — when tempted to copy a reference, change layout/flow,
   keep the aesthetic.

## Code Organisation Rules

These are hard rules, not suggestions. Follow them on every change.

### File size limits
- **Screen files** (`*Screen.kt`): 400 lines max. Extract composables into a `components/`
  sub-package the moment a screen exceeds this.
- **ViewModel / StateHolder**: 500 lines max. Split by concern, not by line count — each class
  should own one coherent slice of state.
- **Data / repository files**: 300 lines max.
- If a file you're editing already exceeds these limits, extract at least the section you're
  touching into its own file before adding more code.

### Component extraction
- Every `@Composable` that is more than ~40 lines or used in more than one place belongs in its
  own file under a `components/` sub-package, not inlined in the screen.
- Name the file after the composable: `RecipeRow.kt` for `RecipeRow`, not `Helpers.kt`.
- One primary public composable per file. Private helpers for that composable live in the same
  file; shared helpers go in `components/`.

### Package structure
```
ui/
  <feature>/
    <Feature>Screen.kt        # thin: wires vm → components, handles nav callbacks
    <Feature>ViewModel.kt
    components/               # all composables extracted from the screen
      FooCard.kt
      BarRow.kt
      ...
```

### State
- No loose `mutableStateOf` at screen scope for anything that outlives a single gesture.
  Use a StateHolder or ViewModel.
- Screens receive state and callbacks — they do not own business logic or persistence calls.

### What "professional output" means here
- A reviewer opening any file should understand its full responsibility in under 30 seconds.
- New features ship as new files, not as additions to existing large files.
- If you find yourself adding a 10th function to a 400-line file, stop and extract first.

## Docs

Internal developer docs live in `docs/`:

- `docs/USB_CONNECTION_GUIDE.md` — connection architecture, mutex pattern, ViewModel wiring,
  dev screen scaffolding.
- `docs/CODE_QUALITY_REPORT.md` — latest code quality snapshot.
