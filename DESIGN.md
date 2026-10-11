# DESIGN.md — SwiftScan Visual & Interaction System

> **Product Thesis:** SwiftScan is a lightweight, privacy-first, tactile QR and Barcode power scanner engineered for instant performance (<300ms camera-to-action), quishing (phishing) security protection, high-throughput batch operations, and a clean Material 3 Expressive design.

---

## 1. Design Engineering Philosophy

SwiftScan balances **extreme lightness** (zero bloat, zero 3rd-party UI dependencies, pure hardware-accelerated Jetpack Compose Canvas) with **tactile industrial precision** (inspired by `better-ui`, `better-layout`, and `better-colors`).

### Core Tenets
1. **Performance Over Decoration:** Zero interstitial lag. Every frame is budgeted. Viewfinder overlays use a single composite hardware Canvas call rather than layered Compose boxes.
2. **Concentric Radii Mathematics:** Mismatched nested corners make interfaces feel cheap. All nested surfaces follow the strict formula: `outerRadius = innerRadius + padding`.
3. **Space-First Visual Grouping:** Use generous negative space (8dp intra-group, 16dp+ inter-group) rather than heavy borders or divider lines.
4. **Optical Center Alignment:** Viewfinder reticle, scanner icons, and badge counters are optically balanced rather than purely geometrically centered.
5. **Tactile Haptic Restraint:** Feedback is restricted to crisp 45ms physical clicks (`VibrationEffect.EFFECT_CLICK`) and subtle, low-latency audio pings (no multi-frequency buzzing).

---

## 2. Color Palette & Token System (OKLCH-Calibrated Hex)

The palette balances dark viewfinder contrast with bright day-mode clarity, avoiding muddy intermediate grays.

| Token Name | Hex Value | Purpose / Semantics |
| :--- | :--- | :--- |
| `LaserCyan` | `#00F0FF` | Primary active targeting reticle, pulse scan-line, precision accents |
| `SafeEmerald` | `#10B981` | Verified safe URLs, Wi-Fi connected badge, successful scan flashes |
| `CautionAmber` | `#F59E0B` | Unverified redirects, unknown domains, duplicate warnings |
| `AlertCrimson` | `#EF4444` | Malicious URL flag, suspicious executable downloads, destructive actions |
| `ObsidianBase` | `#0A0E17` | Viewfinder HUD background, deep dark canvas |
| `SurfaceElevated` | `#161F30` | Cards, bottom sheet surface, floating HUD pill containers |
| `SurfaceGlass` | `rgba(16, 24, 39, 0.75)` | Translucent frosted floating control pills (with 1dp subtle white stroke) |
| `TextPrimary` | `#FFFFFF` | High-contrast typography on dark HUD |
| `TextMuted` | `#94A3B8` | Secondary labels, timestamps, metadata |

---

## 3. Spatial Geometry & Concentric Radii

All UI components adhere to a 4dp incremental grid and strict radius geometry:

* **Floating Control Pills (Top & Zoom Bars):**
  - Height: `44dp`
  - Outer Radius: `22dp` (fully pillular)
  - Inner Icon Button: `36dp` with `18dp` radius and `4dp` internal padding
* **Scan Result Bottom Sheet:**
  - Top Corners Radius: `24dp`
  - Container Background: `SurfaceElevated` (`#161F30`)
  - Internal Action Cards: Radius `16dp` with `12dp` internal padding (satisfying concentric ratio: `24 ≈ 16 + 8`)
* **Viewfinder Reticle:**
  - Corner Radius: `18dp`
  - Corner Arm Length: `32dp`
  - Stroke Width: `3.5dp` (clean, visible, high-contrast)

---

## 4. Layout Architecture (`better-layout` Standard)

```
+-------------------------------------------------------------+
| [Flash]  [Sound]        [ SINGLE | BATCH ]      [Lens] [Gallery] |  <-- Top HUD Controls (floating 16dp from edges)
|                                                             |
|                                                             |
|                    +-------------------+                    |
|                    | ┏               ┓ |                    |
|                    |   [ Laser Bar ]   | |                    |  <-- Optics Viewfinder Reticle
|                    | ┗               ┛ |                    |      (Hardware Canvas, Cyan glow)
|                    +-------------------+                    |
|                      Targeting Active                       |
|                                                             |
|                         [ 1x | 2x | 3x ]                    |  <-- Zoom Quick-Pill (floating 20dp above nav)
|                                                             |
|               [ History (12) ]    [ Create QR ]             |  <-- Bottom Docked Actions (inset 16dp)
+-------------------------------------------------------------+
```

### Layout Rules
1. **Content Bleeds, Controls Float:** The camera preview bleeds 100% edge-to-edge. All controls float inside system window insets (`statusBarsPadding()` and `navigationBarsPadding()`).
2. **Thumb Zone Docking:** Primary interaction targets (History, Create, Zoom, View Batch) are docked in the lower third of the screen for natural one-handed reachability.
3. **Separators Eliminated:** Sections in the bottom sheet and history lists are demarcated by 12dp–16dp negative space, not horizontal hairline rules.

---

## 5. Motion & Micro-Interactions (`better-ui` Standard)

* **Viewfinder Sweep:** Linear continuous vertical sweep (1800ms period) with fade-out at viewport edges.
* **Lock-On Snap:**
  - Viewfinder corners flash `SafeEmerald` (`#10B981`) for 120ms on decode.
  - Scale on Press: All interactive pill buttons use subtle `scale(0.96)` tactile compression on press.
* **Bottom Sheet Entrance:** Spring animation with `dampingRatio = 0.82f` and `stiffness = Spring.StiffnessMediumLow`. No jarring rigid slide.
* **Batch Counter Increment:** Number transition using `slideInVertically` + `fadeIn` (duration: 150ms). Duplicate detections trigger a quick 3-pixel horizontal nudge shake.

---

## 6. Quishing (Anti-Phishing) Security UX

When a URL QR code is scanned:
1. **Domain Highlight Header:** The root domain (e.g. `github.com`) is displayed in 20sp Bold Display, while subdomains and path queries are rendered in muted, de-emphasized mono typography.
2. **Security Shield Indicator:**
   - 🟢 **Verified HTTPS:** Valid standard web protocol.
   - 🟡 **Caution:** IP addresses (`http://192.168...`), unusual ports (`:8080`), or URL shorteners with a 1-tap "Unshorten URL" preview button.
   - 🔴 **Danger Flag:** Cleartext HTTP with password inputs, direct executable links (`.apk`, `.bat`), or malicious schemes.
3. **1-Tap Tracker Strip:** Automatic toggle to strip `utm_source`, `fbclid`, `gclid`, and affiliate tags before launching the browser.

---

## 7. QR Generator Studio UI

* **Live Real-time Preview:** 220dp centered QR canvas that re-renders dynamically on text change using coroutine debouncing.
* **Palette Switcher:** 5 curated high-contrast palettes (Industrial Black, Laser Cyan, Cyber Indigo, Forest Green, Crimson Alert).
* **High-Res Export:** Direct 1-tap export options: **Save High-Res PNG (1024x1024)** or **Share QR**.
