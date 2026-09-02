# TODO

Dev backlog — not user-facing. Ordered by priority within each section.

## Low priority

- **`writePreset` has no mono-only suppression rule.** It suppresses colour-only properties
  under a monochrome film simulation (`COLOR_ONLY_PROPS` / `isMono` in `FujiRecipeCamera.kt`),
  but not the reverse: Mono WC (`0xD193`) and Mono MG (`0xD194`) under a *colour* simulation.
  Confirmed on an X-H2 — the camera rejects those writes with `InvalidDevicePropValue`
  (`0x201C`). Currently harmless because the editor UI never offers the mono controls unless a
  mono simulation is selected, so the values never reach `writePreset`. Worth adding as a guard
  anyway, since the suppression logic should not depend on the UI to be correct — a recipe
  imported or restored from backup could carry mono values alongside a colour simulation.
  Same shape as the existing `isColorTemp` check.

- **Tablet UI work introduced a perceptible camera-connect loading delay.** Since the
  `tablet-ui` branch changes (nav rail, `tabletContentWidth`/adaptive layout wiring in
  `FujiSyncApp.kt`), the Camera tab appears to take noticeably longer (on the order of a
  frame or so) to show connected-camera content after the camera connects, versus before
  those changes. Not yet root-caused — candidates worth checking first: the new
  `CompositionLocalProvider(LocalWindowWidthClass provides widthClass)` wrapper and
  `currentWindowWidthClass()` recomposition behavior, or the `Row { AppNavRail; Column }`
  restructuring around the tab content in `FujiSyncApp.kt`. Compare frame timing against
  `main` before assuming which change is responsible.
