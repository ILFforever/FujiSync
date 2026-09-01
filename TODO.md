# TODO

Dev backlog — not user-facing. Ordered by priority within each section.

## Low priority

- **Tablet UI work introduced a perceptible camera-connect loading delay.** Since the
  `tablet-ui` branch changes (nav rail, `tabletContentWidth`/adaptive layout wiring in
  `FujiSyncApp.kt`), the Camera tab appears to take noticeably longer (on the order of a
  frame or so) to show connected-camera content after the camera connects, versus before
  those changes. Not yet root-caused — candidates worth checking first: the new
  `CompositionLocalProvider(LocalWindowWidthClass provides widthClass)` wrapper and
  `currentWindowWidthClass()` recomposition behavior, or the `Row { AppNavRail; Column }`
  restructuring around the tab content in `FujiSyncApp.kt`. Compare frame timing against
  `main` before assuming which change is responsible.
