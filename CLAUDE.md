# Fuji Recipes

Native Android app (Jetpack Compose + Material 3, package `com.paeki.fujirecipes`) for managing
Fujifilm X-series film-simulation recipes over USB-C OTG (PTP). Reverse-engineered protocol lives
in `PROTOCOL.md` (a living document — keep it accurate as implementation reveals new details).

The current Compose UI is a throwaway Codex test bench and will be scrapped; it is **not** a design
constraint.

## Design Context

Full design context is in `.impeccable.md`. Summary:

### Users
Fujifilm X-series shooters (enthusiast/pro) managing the camera's 7 custom slots (C1–C7) plus a
personal phone library. Connected via USB-C OTG in USB RAW CONV. / PTP mode. v1 jobs: read C1–C7,
edit & write recipes back to the camera, manage a phone recipe library.

### Brand Personality
Premium, editorial, focused. A precision tool — darkroom / light-meter calm. Terse, technical,
human. No clutter or gimmicks.

### Aesthetic Direction
- Dark only. Warm near-black background (`~#0D0D0D`); panels lifted by tone, not shadow.
- **Single** warm gold/brass accent (`~#C99A4E`) for all accents — never a second accent color.
  (Intentionally "Claude Code"–like; this is the house style.)
- Large bold UPPERCASE recipe names; letter-spaced uppercase section labels; mixed-weight wordmark.
- Dark pill summary chips; label-left / value-right property rows with small gold line-icons;
  sticky bottom primary CTA.
- Reference (liked, do NOT copy): `com.bechir.fujifilmrecipes` (screenshots in `references/`).
  Anti-references: default Material light/purple, busy multi-color dashboards, skeuomorphic kitsch.

### Differentiation — structure, not color
Identity comes from **page structure**, since the palette mirrors the reference on purpose.
Structure = **Slot-board first**: home (`MY CAMERA`) shows C1–C7 as a board of what's loaded now.
Nav = 3 tabs: `CAMERA · LIBRARY · PROFILE`. Screens: Slot Board → Slot Detail → Recipe Editor
(shared) → Library → Recipe Detail → Profile. Flows: read, write/edit, manage library.

### Design Principles
1. The camera is the spine — orient around C1–C7 and read → edit → write.
2. One accent, used sparingly — hierarchy via weight/size/space, not color.
3. Content over chrome — big type, dark space, the recipe data is the hero.
4. State honesty — always show connection state and whether content reflects camera, library, or
   unsaved edits.
5. Diverge by structure, not decoration — when tempted to copy the reference, change layout/flow,
   keep the aesthetic.
