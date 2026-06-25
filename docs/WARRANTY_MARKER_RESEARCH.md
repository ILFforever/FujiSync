# Fujifilm Warranty Marker — Research & Spoofing Plan

## Background

Ritchie Roesch (Fuji X Weekly) confirmed directly from Fujifilm that a **permanent marker is
written into the camera** each time a non-licensed third-party app connects over USB. This is how
Fujifilm detects warranty-voiding usage during service.

Licensed apps (Lightroom tethering plugin, Capture One) presumably do not trigger this marker,
implying they present some form of authentication during the PTP session.

---

## Hypothesis

The marker is **not** triggered by a rolling/time-based token (that would require Fujifilm to run
an auth server — unlikely given their software track record). It is almost certainly a **static
vendor-specific PTP command or property value** hardcoded into licensed software, sent during
session initialisation.

This means it is potentially spoofable by capturing and replaying that handshake.

---

## What to capture

Set up USB traffic sniffing between a licensed tethering app and the camera, then compare the
session to a raw PTP session from FujiSync.

### Tools

| Platform | Tool |
|----------|------|
| Windows | Wireshark + USBPcap, or USBlyzer |
| Mac | usbsnoop, IORegistryExplorer, or Wireshark with USB capture |
| Linux | Wireshark (`usbmon`) |

### Licensed apps to sniff

- **Adobe Lightroom Classic** — tethered capture plugin (ships with Lightroom install)
- **Capture One** — tethered shooting with Fujifilm body
- Either will do — just need one licensed session to diff against

---

## What to look for

Standard PTP session looks like:

```
GetDeviceInfo      (0x1001)
OpenSession        (0x1002)
... vendor commands ...
```

Look for any commands in the **`0x9***` range** (vendor operations) or **`0xD***` property reads/writes**
that appear in the licensed session but NOT in FujiSync's session. Specifically anything that
occurs before or immediately after `OpenSession`.

A static auth token would likely look like:
- `SetDevicePropValue` on an unknown `0xD***` property with a fixed byte sequence
- Or a vendor operation `0x9***` with a fixed payload

---

## Steps

1. **Install Lightroom or Capture One** on a Mac or Windows machine
2. **Set up USB capture** (USBPcap on Windows or usbmon on Linux/Mac)
3. **Connect camera** in tethered mode and start a capture session
4. **Save the pcap** and open in Wireshark — filter by USB vendor class
5. **Do the same with FujiSync** (or a minimal raw PTP session) on the same camera
6. **Diff the two sessions** — isolate commands that appear in the licensed session only
7. **Identify the static token/command** and its payload
8. **Add it to `UsbPtpConnection.openSession()`** in FujiSync before or after the standard handshake
9. **Verify** — connect, check if the marker behaviour changes (may need Fujifilm service contact
   to confirm, or observe camera behaviour differences)

---

## Test hardware

Both developer X-H2 bodies are out of warranty — safe to use as test subjects without any
real risk.

---

## If the token is found

Add a `sendLicenseHandshake()` step to `OpenPtpConnection.openSession()` in
`app/src/main/java/com/ilfforever/fujisync/data/usb/UsbPtpConnection.kt`:

```kotlin
fun openSession(sessionId: Int = 1): Boolean {
    sendLicenseHandshake() // if token found
    val transaction = executeCommand(
        code = PtpConstants.OPEN_SESSION,
        params = listOf(sessionId),
    )
    ...
}
```

Document the discovered token in `data/ptp/PtpConstants.kt` with a clear comment explaining
its origin and purpose.

---

## If the marker is firmware-enforced (no token)

If there is no difference between the licensed and unlicensed PTP sessions, the camera's firmware
is detecting and marking connections independently — nothing sent from the app side can prevent it.
In that case, the marker cannot be spoofed and this research is closed.

---

## Related files

- `data/usb/UsbPtpConnection.kt` — session init, where the handshake would be added
- `data/ptp/PtpConstants.kt` — where any discovered token/op code would be documented
- `docs/USB_CONNECTION_GUIDE.md` — connection architecture reference
