# DESIGN.md — SwiftScan Visual & Interaction System

> **Product Thesis:** SwiftScan is an ultra-fast, privacy-first, anti-slop mobile vision and barcode scanning power tool for Android. Engineered for instant performance (<100ms camera-to-action), quishing security protection, high-throughput batch operations, and a refined Material 3 Expressive Titanium & Cobalt design contract.

---

## 1. Design Engineering Philosophy

SwiftScan balances **extreme lightness** (zero bloat, zero 3rd-party UI dependencies, pure hardware-accelerated Jetpack Compose Canvas) with **tactile industrial precision** (in strict compliance with `antislop-ui`, `better-ui`, `better-layout`, `better-colors`, and `better-accessibility`).

### Core Tenets
1. **Performance Over Decoration:** Zero interstitial lag. Every frame is budgeted. Viewfinder overlays use a single composite hardware Canvas call rather than layered Compose boxes.
2. **Concentric Radii Mathematics:** Mismatched nested corners make interfaces feel cheap. All nested surfaces follow the strict formula: `outerRadius = innerRadius + padding` (`20dp` containers, `14dp` cards, `12dp` buttons, `8dp` chips).
3. **Space-First Visual Grouping:** Use generous negative space rather than heavy borders or divider lines.
4. **Optical Hairline Precision:** 1.5px/2px crisp reticle corners and subtle central optical crosshairs without generic neon glows or animated laser sweeps.
5. **Tactile Haptic Restraint:** Feedback is restricted to crisp 45ms physical clicks (`VibrationEffect.EFFECT_CLICK`) and subtle, low-latency audio pings.
6. **Guaranteed 48dp Touch Targets:** Every interactive icon, button, and toggle has a minimum $48\text{dp} \times 48\text{dp}$ touch target.

---

## 2. Color Palette & Token System (Titanium & Cobalt)

The palette balances dark viewfinder contrast with bright day-mode clarity, adhering to strict WCAG AA/AAA contrast guidelines.

| Token Name | Hex Value | Purpose / Semantics |
| :--- | :--- | :--- |
| `VoidCanvas` | `#0B0D11` | Viewfinder HUD background, base dark canvas (OKLCH `0.13 0.008 260`) |
| `SurfaceSlate` | `#161920` | Elevated cards, segmented switcher background, button surface |
| `SurfaceElevated` | `#20242D` | Floating pill containers, dialogs, sheet surfaces |
| `HairlineBorder` | `#2C323E` | Crisp 1dp perimeter stroke on elevated surfaces |
| `PrimaryCobalt` | `#2563EB` | Single primary action accent (WCAG AA $5.17:1$ against white) |
| `StatusEmerald` | `#10B981` | Verified safe URLs, Wi-Fi connected badge, successful decode reticle flash |
| `StatusAmber` | `#F59E0B` | Retail products, GTIN/ISBN barcodes, caution warnings |
| `StatusCrimson` | `#EF4444` | Security threats, phishing flags, destructive delete actions |
| `TextHighEmphasis` | `#F8FAFC` | Primary labels, headers, and codes (WCAG AAA $\ge 7:1$) |
| `TextMediumEmphasis` | `#94A3B8` | Subtitles, metadata, secondary timestamps |
| `TextLowEmphasis` | `#64748B` | Monospace tags, auxiliary captions |

---

## 3. Spatial Geometry & Concentric Radii

All UI components adhere to a 4dp incremental grid and strict radius geometry:

* **Turbo Action Pill (Floating above bottom navigation):**
  - Container Radius: `20dp`
  - Internal Action Buttons: `12dp` radius, `48dp` minimum height
  - Container Padding: `14dp`
  - Concentric relation: $20\text{dp} \approx 12\text{dp} + 8\text{dp}$
* **Code Studio & Vault Cards:**
  - Card Radius: `14dp`
  - Internal Padding: `12dp`
  - Stroke: `1dp` `HairlineBorder`
* **Precision Viewfinder Reticle:**
  - Corner Radius: `10dp`
  - Corner Arm Length: `28dp`
  - Corner Stroke: `2dp` (`HairlineBorder` when idle, snappy `StatusEmerald` contraction on lock)
  - Center Crosshair: `2.5dp` dot + `16dp` crosshair lines at 40% alpha

---

## 4. Layout Architecture (`better-layout` Standard)

```
+-------------------------------------------------------------+
| SwiftScan  [AUTO-COPY]   [Sound]  [Flash]  [Lens]  [Gallery] |  <-- Top Precision HUD (floating, statusBarsPadding)
|                                                             |
|                                                             |
|                    +-------------------+                    |
|                    | ┏               ┓ |                    |
|                    |         •         | |                    |  <-- Optical Reticle HUD
|                    | ┗               ┛ |                    |      (1.5px Hairline, Crosshair, Telemetry)
|                    +-------------------+                    |
|                      LOCKED (19ms)                          |
|                                                             |
|          +---------------------------------------+          |
|          | 🌐 example.com                [SAFE]  |          |  <-- Non-Blocking Turbo Action Pill
|          | [ Open Link ↗ ]    [ Copy URL ]       |          |      (Anchored above nav, spring motion)
|          +---------------------------------------+          |
|                                                             |
|            [ 1x | 2x | 3x ]      [ Single | Batch | OCR ]   |  <-- Zoom & Mode Switcher Pills
|                                                             |
|               [ History (12) ]    [ Create QR ]             |  <-- Bottom Docked Actions (48dp height)
+-------------------------------------------------------------+
```

### Layout Rules
1. **Content Bleeds, Controls Float:** The camera preview bleeds 100% edge-to-edge. All controls float inside system window insets (`statusBarsPadding()` and `navigationBarsPadding()`).
2. **Thumb Zone Docking:** Primary interaction targets (Turbo Action Pill, Zoom, Mode Switcher, History, Create QR) are docked in the lower third for effortless one-handed operation.
3. **Non-Blocking Architecture:** Scans do not freeze the viewfinder with full-screen modals. The Turbo Action Pill slides up seamlessly without interrupting the camera pipeline.

---

## 5. Motion & Micro-Interactions (`better-ui` Standard)

* **Reticle Lock-On:** Snappy 120ms corner contraction ($200\text{dp} \to 190\text{dp}$) with instant color transition to `StatusEmerald`.
* **Turbo Action Pill Entrance/Exit:** Fluid spring animation (`dampingRatio = 0.8f`, `stiffness = 400f`).
* **Scale on Press:** Tactile feedback on all interactive icon buttons.

---

## 6. App Icon Specification

* **Style:** Concept A — Precision Optical Reticle.
* **Canvas:** Deep Obsidian (`#0B0D11`) with subtle titanium radial depth.
* **Foreground:** 4 precision optical corner brackets (`#F8FAFC`, radius 5dp) framing 3 Cobalt Blue finder nodes (`#2563EB`) and a Sky Blue velocity laser beam (`#38BDF8`) with an Emerald status dot.
* **Themed Icons:** Includes dedicated monochrome layer (`ic_launcher_monochrome.xml`) supporting Android 13/14/15 Material You dynamic wallpaper theming.
