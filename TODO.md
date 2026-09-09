# TODO

Dev backlog — not user-facing. Ordered by priority within each section.

## Capability gating — needs hardware

The read path is confirmed on hardware; the write path is not. Panels for bodies nobody owns can be
inspected without a camera via **Profile → Dev Tools → Capability bench**. See
`docs/CAPABILITY_GATING.md` for the design.

**Confirmed on X-H2 (2026-09-03).** The identity card reads `X-H2_0200` · *reported by the camera*,
so `0xD186`/`0xD187` answer on this body and the synthesised `_0100` fallback never engaged. The
asset shipped, the lookup resolved to `X-H2Config2`, and every variant came out correct: 20 film
simulations up to Reala Ace (Std6), half-step tone dials (Std2), 14 WB modes including the
auto-priority pair (Std2), continuous 2500–10000K in 10K steps rather than the 31 fixed steps
(Std2), and a grain size axis (Std2). A wrong asset or lookup would have produced subtly wrong
values rather than none, so this exercises the whole read chain.

**`GetDevicePropDesc` does not work on the X-H2 — confirmed 2026-09-03.** The camera advertises
`0x1014` in its DeviceInfo operations list and then answers `GeneralError` to every call. **Tier 2
is unavailable on this body**, so nothing in the app ever learns a legal value set from the camera
directly; value limits come from Fuji's table alone, backstopped by slot observation.

Consequences, all by design but now load-bearing rather than theoretical:

- The film-simulation block rests on the XRFC table plus values observed in C1–C7. That is the
  weaker footing flagged in `docs/CAPABILITY_GATING.md`, and slot observation is the only thing
  standing between a stale table and a wrongly blocked write. Worth a hard look before anyone
  relies on the block for a body they cannot test.
- The connect-time probe costs exactly one wasted round-trip: `readDescriptors` probes once against
  `0xD18C` and returns empty on failure rather than attempting all seven. Worth confirming in a log
  that it is not retrying.
- Already documented upstream — `current-shooting-state.md` records the X-H2 advertising `0x1014`
  and answering `GeneralError`, and recommends reading C1–C7 as the substitute. This run is an
  independent reproduction of it, not a new finding.

**DeviceInfo lists all 19 recipe codes** — inferred rather than read directly: the capability card
showed no gap sections, and the table marks `BlackImageTone` false for `X-H2Config2`, so that
property would have appeared as doubtful had the camera's own list not overridden it. The X-H2
reports 61 properties in total. Worth confirming directly rather than by inference.

**Still open on the X-H2:**

1. **Write a recipe that fits.** Should behave exactly as before — the gating is invisible when
   nothing is wrong.
2. **Write with D Range Priority on.** Dynamic Range, Highlight and Shadow should be reported as
   skipped rather than failed. This is the one interlock whose scope changed: it used to skip only
   Dynamic Range, and the tone dials failed silently.

**Cannot be checked on an X-H2 at all** — it supports everything, so the block, the dropped rows
and the adjustment rows never trigger. Those are only inspectable through the bench, or on a
borrowed older body.

**If an X-Pro3 ever turns up:** it now records the identity string and the advertised property list
at connect. That is the missing evidence for why the body fails the protocol entirely, and it is
worth capturing for the public repo.

## `0xD193` / `0xD194` — confirmed on hardware 2026-09-03

Set on an X-H2 by hand (warm/cool `+5`, magenta/green `−3`) and read back through the mono tone
bench:

```
0xD193  BlackImageTone       50   (dial +5)
0xD194  MonochromaticColor  -30   (dial -3)
```

**Both protocol rows are correct.** `0xD193` is Mono WC, `0xD194` is Mono MG, and both are the
documented dial × 10 — so `FujiValueMapper.SCALED_X10` is right to contain them, and the app's
labels are right.

**Capability flags name features; the block names property codes; the two are not one to one.**
`BlackImageTone` is the single-axis monochrome toning of the X-T3 era, and the flag is true on
exactly the four configurations that had that feature — so `X-H2Config2` marking it false is
*correct*. Warm/cool and magenta/green succeeded it, and `0xD193` carries the warm/cool axis of the
successor on later bodies while keeping the legacy struct name `lBlackImageTone`.

So a code outlives the feature it was named for, and a false flag says nothing about whether the
code works. The table is not unreliable — mapping a feature flag onto a property code is. The
regression test in `CapabilityResolverTest` pins this.

Unblocks the mapper bug below: the codes and scaling are settled, so wiring Mono WC/MG through
`RecipePresetMapper` is now a mechanical change that can actually be tested.

### Range is ±18, so the editor is wrong

The camera dial runs **−18 … +18**, which is **±180 on the wire** at the confirmed ×10 scaling.
`RecipeEditorScreen` caps both steppers at ±9 — half the range, unreachable in the app.

The sweep probed only as far as ±90, i.e. dial ±9, so it never approached the ceiling and measured
nothing about it. Worth re-running with the range known.

### Legal values: multiples of ten only

Write sweep on an X-H2, identical for both codes:

```
accepted   0  10  20  50  90  100  -90
rejected   1   2   5   9  -9
```

**Every non-multiple of ten is refused with `0x201C`; every multiple is accepted.** The camera takes
exact dial positions and nothing between them, which confirms the ×10 scaling from the write side as
well as the read side. Dial ±18 puts the wire range at ±180.

`tools/generate_capabilities.py` carried a guess of ±100 for both codes — now corrected to ±180
step 10 and the asset regenerated. The step was right; the ceiling was not.

Per-slot storage is confirmed on the X-H2, so writing these per recipe is safe — no repeat of the
WB shift trap.

### Fixed 2026-09-03

- `RecipePresetMapper` now maps `0xD193`/`0xD194` in both directions, on the dial × 10 scale, and
  surfaces them only under a monochrome simulation.
- `RecipeEditorScreen` steppers widened to ±18.
- Four round-trip tests in `RecipePresetMapperTest`, including the range ends.

Not done, deliberately: collapsing to a single axis on `BlackImageTone`-era bodies (X-T3, X-T30,
GFX100 fw1-2). That would re-couple the editor to the attached camera, which was removed on purpose
— a recipe is authored independently of any body. The write path already skips `0xD194` where the
camera does not advertise it, and the compatibility sheet reports it as dropped.

**Still untested on hardware.** The mapper has never sent these values before, so the first write of
a monochrome recipe with toning is the real test.

### Written up upstream

`properties.md` now carries a Monochrome Toning section with the axes, scale, ±180 range, the
multiples-of-ten rule and the colour-simulation interlock; `tested-bodies.md` has the X-H2 row
updated; and `xrfc-capability-database.md` explains why a capability flag names a feature rather
than a property code.

## Superseded — the reasoning that turned out wrong

Kept because the *shape* of the mistake recurs. The 4-vs-24 split led to the guess that `0xD193`
was a dead property modern bodies ignore. Half right: it *was* introduced for the older single-axis
feature, but the code was carried forward to the successor rather than retired. The reading that
fits every observation is feature succession with a reused code, not deprecation.

The intermediate conclusion — "the table is describing X RAW STUDIO, not the camera, so it is
unreliable" — was also wrong, and worse, because it is the kind of wrong that spreads. The table is
accurate. The lossy step is assuming one flag corresponds to one property code.

Also wrong along the way: treating a blog post about the X-T30's ±9 menu dial as evidence about wire
scaling, and briefly concluding the app's labels were wrong on the strength of it. The protocol
notes were right the whole time; what they lacked was a marker distinguishing measured rows from
inferred ones.

## Found while building capability gating — not fixed (out of that scope)

- **Mono WC / Mono MG never reach the camera, in either direction.** The editor offers both
  (`EditorRecipeBuilder.kt:117-118` writes them into the UI model's `tone` map), but
  `RecipePresetMapper.toPreset` does not map them into `RecipePreset.properties`, and
  `toUiModel` does not read `0xD193`/`0xD194` back out. So a photographer can set monochrome
  toning, save the recipe, and the values are silently dropped on write and never restored on
  read. Verified by inspection of both mappers, not on hardware.
  The fix is two lines each way — the ×10 scaling is now hardware-confirmed, so `parseScaled` /
  `signedScaled` already do the work. The write planner skips both under a colour simulation, and
  the capability layer skips them on a body that does not advertise them. Left alone here because
  it is a write-path data bug rather than a gating one, and it deserves its own change.

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
