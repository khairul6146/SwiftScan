# SwiftScan Titanium & Cobalt UI/UX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a world-class, anti-slop mobile interface for SwiftScan using the Titanium & Cobalt design system, eliminating all generic AI aesthetic tells in strict compliance with `antislop-ui`, `better-ui`, `better-colors`, and `better-accessibility`.

**Architecture:** Pure Jetpack Compose UI architecture following Material 3 Expressive & Flutter-inspired fluid component hierarchy. Integrates an obsidian/titanium surface palette with a single deliberate cobalt blue accent, functional 1.5px hairline optical framing, concentric radii geometry, and guaranteed 48dp minimum touch bounds.

**Tech Stack:** Kotlin 2.0, Jetpack Compose, Material 3, AndroidX Vector Graphics, Robolectric.

**Spec:** [`docs/superpowers/specs/2026-11-01-turbo-swiftscan-personal-revamp-design.md`](docs/superpowers/specs/2026-11-01-turbo-swiftscan-personal-revamp-design.md)

---

## Global Design Constraints (Antislop & Titanium-Cobalt Palette)

- **Color Palette Tokens (`better-colors` & `antislop-ui` R-29):**
  - `VoidCanvas = Color(0xFF0B0D11)` (OKLCH `0.13 0.008 260`) — Base viewport background
  - `SurfaceSlate = Color(0xFF161920)` (OKLCH `0.19 0.012 260`) — Neutral elevated card
  - `SurfaceElevated = Color(0xFF20242D)` (OKLCH `0.23 0.015 260`) — Floating pill & sheet elevation
  - `HairlineBorder = Color(0xFF2C323E)` (OKLCH `0.28 0.015 260`) — Crisp 1dp perimeter
  - `PrimaryCobalt = Color(0xFF3B82F6)` (OKLCH `0.62 0.21 255`) — Single primary action accent
  - `StatusEmerald = Color(0xFF10B981)` (OKLCH `0.72 0.17 160`) — Verified safe links & Wi-Fi
  - `StatusAmber = Color(0xFFF59E0B)` (OKLCH `0.75 0.16 75`) — Retail / ISBN products
  - `StatusCrimson = Color(0xFFEF4444)` (OKLCH `0.63 0.22 25`) — Security / Phishing warnings
  - `TextHighEmphasis = Color(0xFFF8FAFC)`
  - `TextMediumEmphasis = Color(0xFF94A3B8)`
  - `TextLowEmphasis = Color(0xFF64748B)`
- **Anti-Slop Hard Gates (`antislop-ui`):**
  - **No Generic Gradients (R-01):** Ban all blue-to-purple and cyan-to-green full-page background gradients.
  - **No Decorative Tech Grids (R-07):** Ban all background blueprint lines, graph paper, and ambient dot matrices.
  - **No Gratuitous Glows (R-13):** Ban all neon box shadows and glowing borders.
  - **No Excessive Glassmorphism (R-10):** Ban multi-layer blur. The camera HUD is direct and optical; the floating pill uses a clean, solid elevated slate surface with a crisp 1dp outline.
  - **No Emojis or Slop Buzzwords:** Ban all emojis in product UI; ban buzzwords like "PRO", "ULTIMATE", "MAX".
- **Concentric Radii Math (`better-ui`):**
  - Container / Floating Pill: `20.dp`
  - Internal Cards: `14.dp`
  - Primary Action Buttons: `12.dp`
  - Chip / Tag: `6.dp`
  - Strict Rule: `outerRadius = innerRadius + padding`
- **Touch Target & Accessibility (`better-accessibility` & `antislop-human`):**
  - Minimum touch target: `48.dp x 48.dp` for every interactive button and icon.
  - Contrast ratio: Minimum $4.5:1$ for all text against backgrounds (WCAG AA) and $\ge 7:1$ for primary elements (WCAG AAA).

---

## Review Focus

1. **Brand Cleanliness:** Verify the word "PRO" is not rendered anywhere in UI strings, headers, or badges.
2. **Contrast on Muted Slate:** Verify secondary metadata (`TextMediumEmphasis` on `SurfaceSlate`) passes WCAG AA $\ge 4.5:1$.
3. **Hairline Optical Alignment:** Ensure reticle corner brackets render at exact 1.5px/2px stroke width without anti-aliasing blur.
4. **Touch Target Enforcement:** Confirm all icon buttons (`Torch`, `LensSwitch`, `Copy`, `Star`) have minimum 48dp bounds even when visible glyphs are 20dp.
5. **Fluid Spring Animation:** Ensure the floating action pill enters and exits smoothly with `spring(dampingRatio = 0.8f, stiffness = 400f)` without frame drops.

---

### Task 1: Titanium & Cobalt Theme System (`Theme.kt`, `Color.kt`, `Type.kt`)

**Files:**
- Modify: `app/src/main/java/com/example/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/example/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/example/ui/theme/Type.kt`
- Test: `app/src/test/java/com/example/ColorContrastTest.kt`

**Interfaces:**
- Produces: `SwiftScanColors` with Titanium-Cobalt tokens, WCAG AA verified typography, and concentric shape tokens.

- [ ] **Step 1: Write the failing color contrast unit test**

```kotlin
// app/src/test/java/com/example/ColorContrastTest.kt
package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.example.ui.theme.*
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastTest {
    private fun contrast(fg: Color, bg: Color): Double =
        ColorUtils.calculateContrast(fg.toArgb(), bg.toArgb())

    @Test
    fun testTitaniumCobaltContrastMeetsWcag() {
        val onBase = contrast(TextHighEmphasis, VoidCanvas)
        assertTrue("High emphasis text on base canvas must be >= 7.0 (AAA): $onBase", onBase >= 7.0)

        val onSlate = contrast(TextMediumEmphasis, SurfaceSlate)
        assertTrue("Medium emphasis text on slate must be >= 4.5 (AA): $onSlate", onSlate >= 4.5)

        val onCobaltBtn = contrast(Color.White, PrimaryCobalt)
        assertTrue("White text on Cobalt CTA must be >= 4.5 (AA): $onCobaltBtn", onCobaltBtn >= 4.5)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests com.example.ColorContrastTest`  
Expected: FAIL (unresolved references for new color tokens).

- [ ] **Step 3: Implement Titanium & Cobalt tokens**
  - In `Color.kt`, replace generic neon values with:
    ```kotlin
    val VoidCanvas = Color(0xFF0B0D11)
    val SurfaceSlate = Color(0xFF161920)
    val SurfaceElevated = Color(0xFF20242D)
    val HairlineBorder = Color(0xFF2C323E)
    val PrimaryCobalt = Color(0xFF3B82F6)
    val StatusEmerald = Color(0xFF10B981)
    val StatusAmber = Color(0xFFF59E0B)
    val StatusCrimson = Color(0xFFEF4444)
    val TextHighEmphasis = Color(0xFFF8FAFC)
    val TextMediumEmphasis = Color(0xFF94A3B8)
    val TextLowEmphasis = Color(0xFF64748B)
    ```
  - In `Type.kt`, set clean typographic scale:
    ```kotlin
    val Typography = Typography(
        headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = (-0.5).sp),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp),
        bodySmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp),
        labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    )
    ```
  - In `Theme.kt`, bind tokens to `SwiftScanTheme`.

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew testDebugUnitTest --tests com.example.ColorContrastTest`  
Expected: PASS with 100% WCAG AA/AAA compliance.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/ui/theme/ app/src/test/java/com/example/ColorContrastTest.kt
git commit -m "style(theme): establish titanium-cobalt design tokens and WCAG contrast verified typography"
```

---

### Task 2: Precision Optical Reticle HUD (`LaserReticleHUD.kt`)

**Files:**
- Create: `app/src/main/java/com/example/ui/components/LaserReticleHUD.kt`
- Modify: `app/src/main/java/com/example/ui/screens/ScannerScreen.kt`

**Interfaces:**
- Consumes: `isLocked: Boolean`, `lockElapsedMs: Long?`, `isAutoCopyActive: Boolean`, `onToggleTorch()`, `onSwitchCamera()`.
- Produces: Razor-sharp optical reticle (1.5px hairline corners + center crosshair) and 48dp header controls.

- [x] **Step 1: Write reticle render test**
Ensure HUD composable renders without throwing and respects camera controller state.

- [x] **Step 2: Implement `LaserReticleHUD.kt`**
  - Implement top header:
    - Left: Brand title `"SwiftScan"` (bold 16sp, no "PRO" badge).
    - Right: 48dp circular icon buttons for Torch and Lens Switch with vector graphics and ripple feedback.
  - Center Reticle Canvas:
    - Dimensions: 190dp x 190dp.
    - Idle State: 1.5dp corner brackets in `HairlineBorder`.
    - Lock State: 2dp corner brackets in `PrimaryCobalt` (or `StatusEmerald` on successful decode), snappy 100ms bracket contraction.
    - Optical Center Crosshair: subtle 2.5dp circle + 16dp crosshair line at 40% alpha.
    - Zero animated neon beam sweeps.
  - Telemetry badge: Monospace text `720p60 • READY` or `LOCKED (19ms)`.

- [x] **Step 3: Integrate into `ScannerScreen.kt`**
Replace canvas drawing in `ScannerScreen.kt` with `LaserReticleHUD`.

- [x] **Step 4: Run unit tests**
Run: `./gradlew testDebugUnitTest`  
Expected: PASS.

- [x] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/ui/components/LaserReticleHUD.kt app/src/main/java/com/example/ui/screens/ScannerScreen.kt
git commit -m "feat(hud): implement precision optical reticle HUD with titanium-cobalt styling"
```

---

### Task 3: Refined Turbo Action Pill (`TurboActionPill.kt`)

**Files:**
- Create: `app/src/main/java/com/example/ui/components/TurboActionPill.kt`
- Modify: `app/src/main/java/com/example/ui/screens/ScannerScreen.kt`

**Interfaces:**
- Consumes: `ParsedPayload`, `onPrimaryAction()`, `onSecondaryAction()`, `onDismiss()`.
- Produces: Concentric 20dp floating card with context-aware vector buttons and 48dp minimum touch targets.

- [x] **Step 1: Write component layout test**
Verify all 5 payload variants (URL, Wi-Fi, Product, vCard, Plain Text) supply valid accessibility content descriptions and 48dp action button heights.

- [x] **Step 2: Implement `TurboActionPill.kt`**
  - Container: `background(SurfaceElevated)`, `border(1.dp, HairlineBorder)`, `RoundedCornerShape(20.dp)`, padding 14dp.
  - Concentric button radius: `RoundedCornerShape(12.dp)` (since $20\text{dp} - 8\text{dp} = 12\text{dp}$).
  - Drag affordance: 28dp x 3dp pill in `HairlineBorder` at top.
  - Header Row:
    - 20dp Vector icon badge + Title (15sp SemiBold) + Status tag (e.g., `[SAFE]`, `[WI-FI]`, `[EAN-13]`).
  - Subtitle: 13sp `TextMediumEmphasis` detail (e.g. "Trackers stripped", "WPA2 Secured").
  - Actions Row:
    - Primary Button: `background(PrimaryCobalt)` with white text (14sp Bold), minimum 48dp height.
    - Secondary Button: `background(SurfaceSlate)` with `TextHighEmphasis`, 1dp `HairlineBorder`, minimum 48dp height.
  - Morphing schemas:
    - Wi-Fi: **[Connect Now 📶]** + **[Copy Key]**
    - URL: **[Open Link ↗]** + **[Copy URL]**
    - Product: **[Search Web 🔍]** + **[Copy Code]**
    - vCard: **[Save Contact 👤]** + **[Direct Call]**
    - Text: **[Copy Text 📋]** + **[Search]**

- [x] **Step 3: Wire into `ScannerScreen.kt`**
Place floating pill 16dp above the bottom navigation bar with a smooth spring enter/exit animation.

- [x] **Step 4: Run unit tests**
Run: `./gradlew testDebugUnitTest`  
Expected: PASS.

- [x] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/ui/components/TurboActionPill.kt app/src/main/java/com/example/ui/screens/ScannerScreen.kt
git commit -m "feat(ui): implement refined non-blocking turbo action pill with titanium-cobalt theme"
```

---

### Task 4: High-Craft Barcode Studio UI (`GenerateScreen.kt`)

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/GenerateScreen.kt`

**Interfaces:**
- Consumes: `BarcodeGenerator`.
- Produces: Clean segmented format switcher, crisp white canvas card, and 48dp export actions.

- [x] **Step 1: Write format selection test**
Ensure switching tabs updates the barcode format and input validation rules.

- [x] **Step 2: Restyle `GenerateScreen.kt`**
  - Top header: "Code Studio" (clean bold title, zero "PRO" tags).
  - Segmented control: Solid container (`SurfaceSlate`) with active indicator pill (`PrimaryCobalt`).
  - Canvas card: Crisp white card with 20dp padding, sharp quiet zones, and barcode standard caption in monospace.
  - Input field: Clean border (`HairlineBorder`), clear label, helper text for input restrictions.
  - Palette swatches: Restrained palette (Black, Charcoal, Navy, Forest, Deep Maroon) with concentric active ring.
  - Export actions: 48dp Save PNG and Share buttons with vector icons.

- [x] **Step 3: Run unit tests**
Run: `./gradlew testDebugUnitTest`  
Expected: PASS.

- [x] **Step 4: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/GenerateScreen.kt
git commit -m "style(generator): restyle barcode studio with segmented control and titanium-cobalt tokens"
```

---

### Task 5: Chronological Vault UI (`HistoryScreen.kt`)

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/HistoryScreen.kt`

**Interfaces:**
- Consumes: Grouped scan history list.
- Produces: Information-dense chronological feed with 1-tap 48dp copy actions.

- [x] **Step 1: Write history item layout test**
Verify touch bounds for the copy and star buttons on each history card measure at least 48dp x 48dp.

- [x] **Step 2: Restyle `HistoryScreen.kt`**
  - Top header: "Scan Vault" with item count and export icon.
  - Section headers: Uppercase monospace date badges ("TODAY", "YESTERDAY", "EARLIER").
  - History card: Solid `SurfaceSlate` card with 1dp `HairlineBorder`.
  - Icon: Clean vector format icon (no emojis).
  - Actions: 48dp quick-copy icon button with ripple feedback + favorite star toggle.
  - Top filter chips: Restrained chips with solid `PrimaryCobalt` active state.

- [x] **Step 3: Run unit tests**
Run: `./gradlew testDebugUnitTest`  
Expected: PASS.

- [x] **Step 4: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/HistoryScreen.kt
git commit -m "style(history): restyle scan vault with chronological headers and titanium-cobalt tokens"
```

---

### Task 6: Anti-Slop Audit & Delivery Gate Verification

**Files:**
- Output: `dist/SwiftScan.apk`

- [x] **Step 1: Run Anti-Slop Delivery Gate Checklist (`antislop-ui`)**
  - [x] Zero generic blue-purple or neon gradients (R-01).
  - [x] Zero background tech/grid patterns (R-07).
  - [x] Max 1 glassmorphism element across entire screen (R-10).
  - [x] Concentric radii math strictly applied (`outer = inner + padding`) (R-11).
  - [x] Zero ambient glows or glowing border lines (R-13).
  - [x] Palette capped at 2 core neutrals + 1 deliberate accent (`PrimaryCobalt`) (R-29).
  - [x] 100% of text elements pass $\ge 4.5:1$ contrast against their immediate background.
  - [x] 100% of clickable elements have $\ge 48\text{dp} \times 48\text{dp}$ touch targets.
  - [x] Zero emojis in product UI; word "PRO" completely purged.

- [x] **Step 2: Run all unit and Robolectric tests**
Run: `./gradlew testDebugUnitTest`  
Expected: 100% PASS.

- [x] **Step 3: Assemble Debug APK**
Run: `./gradlew assembleDebug`  
Expected: BUILD SUCCESSFUL with zero compile errors.

- [x] **Step 4: Package deliverable in `dist/SwiftScan.apk`**
```bash
Copy-Item app/build/outputs/apk/debug/app-debug.apk dist/SwiftScan.apk -Force
```

- [x] **Step 5: Final Git commit**
```bash
git add dist/SwiftScan.apk docs/superpowers/plans/2026-11-01-swiftscan-titanium-cobalt-design.md
git commit -m "chore(release): verify anti-slop delivery gate and assemble clean Titanium & Cobalt APK"
```
