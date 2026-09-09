# Camera capability and gating

How the app works out what a connected Fujifilm body will accept, and what it does with that.

**TL;DR** — Three sources answer "will this setting work on this camera", ranked by how much they
can be trusted. Only the camera's own word blocks a write. Fuji's shipped compatibility data warns
but never blocks, because it describes what X RAW STUDIO offers rather than what camera firmware
accepts, and it has never been verified against a non-X-Trans-V body.

## The three sources

| Tier | Source | Answers | Trust |
|---|---|---|---|
| 1 | `GetDeviceInfo` supported-property list | Does this property code exist on this body | The camera's own statement |
| 2 | `GetDevicePropDesc` (`0x1014`) | The exact legal values | Authoritative when the body answers |
| 3 | Fuji's XRFC capability table | Value *limits* per camera generation | Evidence, not proof |

Tier 2 is optional, and on the one body tested it is **unavailable**: an X-H2 advertises `0x1014`
in its DeviceInfo operations list and then answers `GeneralError` to every call (confirmed
2026-09-03). It is probed once against a known-good property and skipped entirely if it fails, so
the cost is one round-trip and nothing depends on it.

Plan for it being absent rather than present. Where the camera will not state its own legal values,
value limits come from Fuji's table alone — which is why slot observation below is load-bearing
rather than a nicety.

Tier 3 is the only source that knows value-level limits — the film-simulation ceiling, whether the
tone dials have half steps, the two extra white-balance modes, continuous versus listed Kelvin,
whether a grain *size* axis exists. It is also the least trustworthy. That asymmetry is the whole
design.

## The rule

**Film Simulation blocks. Everything else warns.**

A recipe whose film simulation the body cannot take is refused outright — nothing is sent, not even
the slot selector, and the slot is left byte-for-byte as it was. Every other setting can be dropped
or adjusted and still leave a recognisable recipe; the simulation cannot. Writing the other fifteen
properties and letting the camera refuse the simulation would leave the slot holding this recipe's
tone curve over the *previous* recipe's look, and report as a success.

For everything else:

- **Tier 1 says absent** → the setting is not written. It appears in the sheet as dropped.
- **Tier 3 says absent, Tier 1 has not objected** → written anyway, and flagged.
- **A value is outside Tier 3's range** → written anyway; the camera gets the final say, and its
  rejection is reported per property.
- **A value is outside Tier 1/2's range** → the same, except for the three properties where snapping
  to the nearest legal value preserves intent (see *Adjustment* below).

A body that lists *none* of the recipe block in its DeviceInfo is not believed at all — it is
clearly not enumerating vendor properties, and treating that as "this camera has no settings" would
block every write on a camera that works fine.

### A table flag is not a property code

The table's flags name **features**; the recipe block names **property codes**; the two are not one
to one, because a code outlives the feature it was introduced for.

`BlackImageTone` is the worked example. The flag is true on only four configurations and its field
name attaches to `0xD193`, which reads like "an X-H2 does not have `0xD193`". It does — the code
carries the Warm/Cool axis of monochrome toning there, confirmed by reading a camera set by hand.
BlackImageTone was the single-axis toning of the X-T3 era; the two-axis control succeeded it and
reused the code.

This is why the table is never allowed to veto a code the camera advertises, and why
`CapabilityResolverTest` guards that. The table is accurate — mapping a feature flag onto a property
code is the lossy step.

### Three latches on the block

Each is enough on its own; they cover different routes.

1. **Recipe detail CTA** — reads *Film Sim Not Supported* and disables, so the sync sheet never opens.
2. **Sync sheet CTA** — the same, for any path that reaches the sheet another way.
3. **`writePreset`** — refuses before sending anything. This is the one that catches
   restore-from-backup and slot rearrange, which touch neither screen. Those write the slots they
   can and name the ones they could not.

### Slot observation beats the table

Every film simulation sitting in C1–C7 is proof the body accepts it. After the connect read,
`CameraCapability.withObservedFilmSimulations()` widens the known ceiling to include them.

It can only ever raise the ceiling, never lower it — a body can support Reala Ace with no slot
using it, so absence proves nothing. This is what stops a camera on firmware newer than Fuji's table
being blocked from a simulation it plainly supports, and it is sound because the simulation
numbering is a strictly nested chain: if value *n* is legal, everything below it is too.

## Identity

`0xD186` and `0xD187` return a string of the form `<Model>_<FirmwareGeneration>` — an X-H2 on
firmware 2.00 answers `"X-H2_0200"`. That string is the key into Fuji's table. Older bodies fail the
read; Fuji's own client then synthesises `<model>_0100`, and so does this app
([`CameraDeviceKey`](../app/src/main/java/com/ilfforever/fujisync/data/capability/CameraDeviceKey.kt)).

The generation suffix is **not** the firmware version shown in the camera menus: an X-H2 displaying
"5.20" still reports capability generation `_0200`.

Where the exact key is not in the table — a firmware newer than Fuji's data — the lookup falls back
to the highest known generation of the same model rather than giving up.

## The capability asset

`app/src/main/assets/xrfc_capabilities.json` is generated by
[`tools/generate_capabilities.py`](../tools/generate_capabilities.py) from the decoded `XRFC.DAT`
XML in the [fujifilm-ptp-recipes](https://github.com/ILFforever/fujifilm-ptp-recipes) repo. **Never
edit it by hand.** Regenerate with:

```sh
python tools/generate_capabilities.py            # defaults to ../fujifilm-ptp-recipes
python tools/generate_capabilities.py --xml PATH
```

The per-variant value lists live inside the generator, transcribed from
`docs/reverse-engineering/xrfc-value-tables.md` — the XML names a variant (`Std6`) but the values it
selects are inside `XRFC.dll`, not in the XML.

`XrfcCapabilityAssetTest` guards the result: dangling config references, unknown property codes and
variants without a value table all fail the build.

## Where it plugs in

| File | Role |
|---|---|
| `data/capability/CameraDeviceKey.kt` | Builds and validates the `<Model>_<Generation>` key |
| `data/capability/XrfcCapabilityTable.kt` | Loads the asset; resolves a key to a capability record |
| `data/capability/CapabilityResolver.kt` | Merges the three tiers into one `CameraCapability` |
| `data/capability/RecipeWritePlan.kt` | Decides what is sent, adjusted, skipped, or warned about |
| `data/usb/CapabilityProbe.kt` | Reads identity and descriptors inside the existing PTP session |
| `data/usb/PropertyWriteOutcome.kt` | Per-property result of a write |

The probe runs inside `FujiPtpProbe.probe()`, which already holds an open session at connect time,
so it costs two property reads plus — only where the camera supports the operation — a handful of
descriptor reads.

## Interlocks

Separate from capability, and confirmed on hardware: the camera refuses some writes because of the
state it is in, not because it lacks the feature.

| Situation | Refused | Note |
|---|---|---|
| Monochrome film simulation | Colour Chrome, FX Blue, Colour, WB Shift R/B | Colour-only settings |
| Colour film simulation | Mono WC, Mono MG | Answers `0x201C` |
| White Balance not in Kelvin mode | Colour Temperature | Only writable in that mode |
| D Range Priority is Weak/Strong/Auto | Dynamic Range, **Highlight Tone, Shadow Tone** | All three answer `0x201C` |

The last one is easy to get wrong: while priority is active the camera owns all three properties,
not just Dynamic Range, and it rejects them with the same response code an out-of-range value
produces. A locked property therefore looks exactly like an unsupported one. `0xD191` is checked
before concluding a tone dial is missing.

## Adjustment

Where snapping a value to the nearest one a body accepts preserves the photographer's intent, the
planner does it and reports the change:

- **Highlight Tone / Shadow Tone** — a half step becomes the nearest whole step on a body without
  half steps.
- **Colour Temperature** — clamped into the body's Kelvin range.

Never for enumerations such as Film Simulation, where the "nearest" value is a different look
entirely. Those are sent as asked and the camera refuses them.

## Where compatibility does and does not belong

**The recipe editor is camera-independent, on purpose.** A recipe is authored on its own terms —
it outlives any particular body, gets shared by QR, and is edited with nothing plugged in. So
`RecipeEditorScreen` takes no camera parameter at all: every control is shown, every value is
offered, tone dials always take half steps. Compatibility is a property of the *push*, not of the
recipe.

This also removed the only camera coupling the editor ever had: two model-name string checks
(`!cameraModel.contains("X-Pro3")` for Smooth Skin, `!cameraModel.contains("X-T30")` for Clarity)
that were both wrong — an X-T30 II has Clarity, and many bodies besides the X-Pro3 lack Smooth
Skin — and that forced the value to a default on save, losing it from the recipe.

Compatibility surfaces at push time only:

- **Camera detail sheet** — a "what this body takes" card: the identity key and whether it was
  reported or assumed, the film-simulation count and ceiling, tone step size, white-balance modes,
  Kelvin range, grain axis, and any settings the body lacks.
- **Recipe detail and sync sheet** — one line above the write button. It is a control, not a
  banner: tapping opens the detail as a sheet rather than expanding in place, so the write button
  never moves under the user's thumb. Shown once per path — library recipes get it on the sync
  sheet where the slot is chosen, camera-slot recipes on the detail screen where they write
  directly.
- **After a write** — settings the camera actually refused are named. A partial write is never
  reported as a success.

The detail sheet lists **only changed settings**, grouped into the recipe's own sections. What
survives is covered by one sentence — "Everything not listed below transfers unchanged" — which
does the same reassuring work as listing a dozen untouched rows, at one line instead of a
screenful. Dropped values are struck through, adjusted ones show the pair (`+1.5 → +1`), and each
carries its reason underneath rather than squeezed into a right-hand column.

The blocked case gets a panel instead of a list, because there is no partial outcome to describe:
it names the simulation, says the slot is untouched, and says what to change.

## Limits

Everything Tier 3 knows comes from Fuji's desktop application. The upstream protocol notes are
explicit that it is a risk ranking rather than a compatibility guarantee, and that no part of it has
been checked against a non-X-Trans-V body. The gating here is built so that being wrong about a body
costs a warning, not a blocked feature.
