#!/usr/bin/env python3
"""Generate the app's camera-capability asset from Fuji's own XRFC capability database.

Input  : the decoded ``XRFC.DAT`` XML shipped in the fujifilm-ptp-recipes repo
         (``docs/reverse-engineering/reference/xrfc-capabilities.xml``).
Output : ``app/src/main/assets/xrfc_capabilities.json``.

The XML gives, per camera/firmware, whether each setting is supported and *which encoding
variant* it uses. The variant name alone is useless to a client -- the value lists it selects
live inside ``XRFC.dll``, not in the XML -- so the per-variant tables below are transcribed
from the reverse-engineering write-up that recovered them:

    fujifilm-ptp-recipes/docs/reverse-engineering/xrfc-value-tables.md

Nothing here is hardware-verified. This is Fuji's client-side view of what each body accepts,
which is strong evidence but not proof; the app treats it as a risk ranking and never as a hard
block on its own. See docs/CAPABILITY_GATING.md.

Usage:
    python tools/generate_capabilities.py [--xml PATH] [--out PATH]
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_XML = (
    REPO_ROOT.parent
    / "fujifilm-ptp-recipes"
    / "docs"
    / "reverse-engineering"
    / "reference"
    / "xrfc-capabilities.xml"
)
DEFAULT_OUT = REPO_ROOT / "app" / "src" / "main" / "assets" / "xrfc_capabilities.json"

# XRFC element name -> the PTP slot property code(s) it governs.
# Pairings are from xrfc-value-tables.md section 15 (the block builder FUN_1800c55f0), which reads
# the mapping directly out of Fuji's binary rather than inferring it from field order.
ELEMENT_TO_CODES: dict[str, list[int]] = {
    "DynamicRange": [0xD190],
    "WideDynamicRange": [0xD191],  # "D Range Priority" in the app's language
    "FilmSimulation": [0xD192],
    "BlackImageTone": [0xD193],  # Mono Warm/Cool
    "MonochromaticColor": [0xD194],  # Mono Magenta/Green
    "GrainEffect": [0xD195],
    "ChromeEffect": [0xD196],  # Color Chrome
    "ColorChromeBlue": [0xD197],
    "SmoothSkinEffect": [0xD198],
    "WhiteBalance": [0xD199],
    "WBShift_RB": [0xD19A, 0xD19B],  # one flag governs both shift axes
    "WBColorTemp": [0xD19C],
    "HighlightTone": [0xD19D],
    "ShadowTone": [0xD19E],
    "Color": [0xD19F],
    "Sharpness": [0xD1A0],
    "NoiseReduction": [0xD1A1],
    "Clarity": [0xD1A2],
    # 0xD1A3 (Lens Modulation Optimiser) and 0xD1A4 (Colour Space) are in Fuji's table but are not
    # recipe settings and are never written by this app, so they are left out: the asset is meant to
    # correspond exactly to the properties the app manages, and a test enforces that.
}

# ---------------------------------------------------------------------------
# Per-variant legal value sets, transcribed from xrfc-value-tables.md.
#
# Two shapes:
#   {"values": [...]}                     an enumerated set
#   {"min": n, "max": n, "step": n}       a continuous range
# A variant that carries no value restriction at all is simply absent here.
# ---------------------------------------------------------------------------

# Section 5. Strictly nested; value 1 is Provia on every body, so a newer body's value written to
# an older one is out of range rather than silently a different simulation.
FILM_SIMULATION = {name: {"values": list(range(1, top + 1))} for name, top in
                   (("Std1", 15), ("Std2", 16), ("Std3", 17), ("Std4", 18), ("Std5", 19), ("Std6", 20))}

# Section 7. Wire value is the display dial x 10. Std2 adds the half steps; it is a strict superset.
TONE_DIAL = {
    "Std1": {"values": [-20, -10, 0, 10, 20, 30, 40]},
    "Std2": {"values": [-20, -15, -10, -5, 0, 5, 10, 15, 20, 25, 30, 35, 40]},
}

# Section 9. Std2 adds the two Auto-priority modes. Both sets include Custom 1-3.
WHITE_BALANCE_STD1 = [0x0002, 0x0004, 0x0006, 0x0008, 0x8001, 0x8002,
                      0x8003, 0x8006, 0x8007, 0x8008, 0x8009, 0x800A]
WHITE_BALANCE = {
    "Std1": {"values": WHITE_BALANCE_STD1},
    "Std2": {"values": WHITE_BALANCE_STD1 + [0x8020, 0x8021]},
}

# Section 10. The generational difference runs the other way here: older bodies are limited to 31
# fixed steps, newer ones accept any Kelvin in 2500-10000 at 10 K granularity.
COLOR_TEMPERATURE = {
    "Std1": {"values": [
        10000, 9100, 8300, 7700, 7100, 6700, 6300, 5900, 5600, 5300,
        5000, 4800, 4500, 4300, 4200, 4000, 3800, 3700, 3600, 3400,
        3300, 3200, 3100, 3000, 2950, 2850, 2800, 2700, 2650, 2550,
        2500,
    ]},
    "Std2": {"min": 2500, "max": 10000, "step": 10},
}

# Section 11. The variant governs whether a grain *size* axis exists at all, not the strength list.
# Std1 bodies have no size axis, so the composites 4 and 5 are unreachable there.
GRAIN_EFFECT = {
    "Std1": {"values": [1, 2, 3], "grainSize": False},
    "Std2": {"values": [1, 2, 3, 4, 5], "grainSize": True},
}

# Section 8. Std1 and Std2 hold identical values -- the variant distinction has no effect on the
# legal set. Recorded anyway so the table stays a faithful transcription.
HIGH_ISO_NR_VALUES = [20480, 24576, 0, 4096, 8192, 12288, 16384, 28672, 32768]
HIGH_ISO_NR = {"Std1": {"values": HIGH_ISO_NR_VALUES}, "Std2": {"values": HIGH_ISO_NR_VALUES}}

VARIANT_TABLES: dict[int, dict[str, dict]] = {
    0xD192: FILM_SIMULATION,
    0xD195: GRAIN_EFFECT,
    0xD199: WHITE_BALANCE,
    0xD19C: COLOR_TEMPERATURE,
    0xD19D: TONE_DIAL,
    0xD19E: TONE_DIAL,
    0xD1A1: HIGH_ISO_NR,
}

# Legal sets that are the same on every body -- the fourteen properties the capability data records
# only as supported / not supported. From docs/properties.md. Emitted so the app has one source of
# truth for "is this value writable at all", independent of generation.
UNIVERSAL_VALUES: dict[int, dict] = {
    0xD190: {"values": [0, 100, 200, 400]},
    0xD191: {"values": [0, 1, 2, 32768]},
    # Monochrome toning. Dial ±18, wire ±180, and the camera refuses anything that is not an exact
    # dial position — confirmed on an X-H2 by write sweep: 0, 10, 20, 50, 90, 100 and -90 accepted,
    # 1, 2, 5, 9 and -9 all rejected with 0x201C. Both codes behave identically.
    0xD193: {"min": -180, "max": 180, "step": 10},
    0xD194: {"min": -180, "max": 180, "step": 10},
    0xD196: {"values": [1, 2, 3]},
    0xD197: {"values": [1, 2, 3]},
    0xD198: {"values": [1, 2, 3]},
    0xD19A: {"min": -9, "max": 9, "step": 1},
    0xD19B: {"min": -9, "max": 9, "step": 1},
}


def hexkey(code: int) -> str:
    return "0x%04X" % code


def parse_xml(path: Path) -> tuple[dict[str, str], dict[str, dict], str]:
    """Return (device -> config, config -> per-property record, source version)."""
    text = path.read_text(encoding="utf-8")
    root = ET.fromstring(text)
    version = root.get("Version", "unknown")

    devices: dict[str, str] = {}
    compat = root.find("Compatibilitys")
    if compat is None:
        sys.exit(f"{path}: no <Compatibilitys> section")
    for node in compat.findall("Compatibility"):
        device = node.get("Device")
        if device and node.text:
            devices[device] = node.text.strip()

    configs: dict[str, dict] = {}
    groups = root.find("PropertyGroups")
    if groups is None:
        sys.exit(f"{path}: no <PropertyGroups> section")
    for group in groups.findall("PropertyGroup"):
        name = group.get("Config")
        if not name:
            continue
        props: dict[str, dict] = {}
        for element, codes in ELEMENT_TO_CODES.items():
            node = group.find(element)
            # An absent element and an explicit "false" both mean the body does not get the
            # setting. The XML comment at the top of the file states true/false is lowercase only.
            supported = node is not None and (node.text or "").strip() == "true"
            record: dict = {"supported": supported}
            if supported and node is not None:
                variant = node.get("type")
                if variant:
                    record["variant"] = variant
            for code in codes:
                props[hexkey(code)] = dict(record)
        configs[name] = {"properties": props}

    return devices, configs, version


def build_variants() -> dict[str, dict]:
    out: dict[str, dict] = {}
    for code, table in VARIANT_TABLES.items():
        out[hexkey(code)] = table
    return out


def validate(devices: dict[str, str], configs: dict[str, dict]) -> None:
    """Fail loudly rather than shipping a table with dangling references."""
    missing = sorted({config for config in devices.values() if config not in configs})
    if missing:
        sys.exit(f"devices reference configs that do not exist: {missing}")

    for name, config in configs.items():
        for key, record in config["properties"].items():
            variant = record.get("variant")
            if variant is None:
                continue
            table = VARIANT_TABLES.get(int(key, 16))
            if table is not None and variant not in table:
                sys.exit(
                    f"{name}/{key}: variant {variant!r} has no value table. "
                    f"Known: {sorted(table)}"
                )

    # Every device key must look like Model_FirmwareGeneration; the app synthesises that shape for
    # bodies whose identity read fails, so a malformed key here would never be matched.
    bad = [d for d in devices if not re.fullmatch(r".+_\d{4}", d)]
    if bad:
        sys.exit(f"malformed device keys: {bad}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--xml", type=Path, default=DEFAULT_XML)
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    args = parser.parse_args()

    if not args.xml.exists():
        sys.exit(
            f"capability XML not found at {args.xml}\n"
            "Pass --xml explicitly, pointing at the fujifilm-ptp-recipes checkout."
        )

    devices, configs, version = parse_xml(args.xml)
    validate(devices, configs)

    payload = {
        "schema": 1,
        "source": f"XRFC.DAT ConversionCaps Version {version}",
        "note": (
            "Fuji's client-side tether-RAW capability table, decoded from XRFC.DAT. Strong "
            "evidence of what a body accepts, not a firmware guarantee. Generated by "
            "tools/generate_capabilities.py -- do not edit by hand."
        ),
        "devices": dict(sorted(devices.items())),
        "configs": dict(sorted(configs.items())),
        "variants": build_variants(),
        "universal": {hexkey(code): table for code, table in sorted(UNIVERSAL_VALUES.items())},
    }

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")

    print(f"wrote {args.out}")
    print(f"  {len(devices)} devices -> {len(configs)} configs, source version {version}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
