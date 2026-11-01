# SwiftScan Professional UI/UX Design Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Completely rebuild SwiftScan's UI/UX to a world-class, professional standard, eliminating all AI slop (gimmicky laser beams, decorative tech grids, unpurposed glows, and excessive glassmorphism) in strict accordance with `antislop-ui`, `better-ui`, `better-colors`, `better-layout`, `better-accessibility`, and Google Stitch design tokens.

**Architecture:** Pure Jetpack Compose UI architecture following Material 3 Expressive & Flutter-inspired fluid component hierarchy. Replaces all emojis with crisp vector stroke icons, adopts a restrained obsidian/slate palette with a single deliberate accent, enforces concentric radii, and ensures 100% WCAG AA contrast compliance.

**Tech Stack:** Kotlin 2.0, Jetpack Compose, Material 3, AndroidX Vector Graphics, Robolectric.

**Spec:** [`docs/superpowers/specs/2026-11-01-turbo-swiftscan-personal-revamp-design.md`](file:///c:/Users/khair/MyProjects/SwiftScan/docs/superpowers/specs/2026-11-01-turbo-swiftscan-personal-revamp-design.md)

---

## Global Design Constraints (Antislop & Craft Standards)

- **Anti-Slop Hard Gates (`antislop-ui`):**
  - **No Generic Gradients (R-01):** Ban all blue-to-purple and cyan-to-green full-screen gradients. Solid, high-contrast dark surfaces only.
  - **No Decorative Tech Grids (R-07):** Ban all background graph paper, blueprint lines, and ambient dot matrices.
  - **No Gratuitous Glows (R-13):** Ban all neon box shadows and glowing borders. Accent colors function purely as focus/action indicators.
  - **No Excessive Glassmorphism (R-10):** Do not apply `blur` across multiple layers. The camera HUD is direct, sharp, and optical; the floating pill uses a clean, dense matte surface (`#141C2B`) with a crisp 1dp outline.
  - **No Emojis in Product UI:** 100% vector stroke iconography (Lucide / Material Symbols standard).
- **Concentric Radii Math (`better-ui`):**
  - Container / Pill: `20.dp`
  - Internal Cards: `14.dp`
  - Primary Action Buttons: `12.dp`
  - Tag / Chip: `6.dp`
  - Equation: `outerRadius = innerRadius + padding`
- **Touch Target & Accessibility (`better-accessibility` & `antislop-human`):**
  - Minimum touch target: `48.dp x 48.dp` for every interactive button and icon (WCAG 2.5.5).
  - Contrast ratio: Minimum $4.5:1$ for all text against backgrounds (WCAG AA).

---

## Review Focus

1. **Slop Regression:** Verify no artificial neon drop shadows, laser sweep animations, or dot grids re-enter the HUD.
2. **Visual Hierarchy Collision:** Ensure the floating action pill never obscures the camera's optical framing area on smaller screen viewports.
3. **Contrast Failure on Muted Metadata:** Verify secondary text (e.g., timestamps, serial tags) maintains $\ge 4.5:1$ contrast against dark slate surfaces.
4. **Touch Target Clipping:** Verify all icon-only buttons (`Torch`, `LensSwitch`, `Copy`, `Star`) have minimum 48dp click bounds even when the visible icon is 20dp.
5. **Dynamic Font Scaling:** Verify layout does not overflow or truncate action buttons when system font scale is set to $1.3\times$ or higher.

---

### Task 1: Design Tokens & Antislop Theme Foundation

**Files:**
- Modify: `app/src/main/java/com/example/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/example/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/example/ui/theme/Type.kt`
- Test: `app/src/test/java/com/example/ColorContrastTest.kt`

**Interfaces:**
- Produces: Semantic theme tokens `SwiftScanColors`, `SwiftScanTypography`, `SwiftScanShapes`.

- [ ] **Step 1: Write contrast verification unit test**

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
    private fun contrastRatio(fg: Color, bg: Color): Double {
        return ColorUtils.calculateContrast(fg.toArgb(), bg.toArgb())
    }

    @Test
    fun testTextContrastAgainstSurfaces() {
        // High-emphasis text against base background (Must be >= 7.0 for AAA or >= 4.5 for AA)
        val ratioTextOnBase = contrastRatio(TextHighEmphasis, VoidBackground)
        assertTrue("Text on base must meet WCAG AA (>= 4.5): was $ratioTextOnBase", ratioTextOnBase >= 4.5)

        // Secondary text against card surface
        val ratioTextSecondary = contrastRatio(TextMediumEmphasis, SlateSurface)
        assertTrue("Secondary text must meet WCAG AA (>= 4.5): was $ratioTextSecondary", ratioTextSecondary >= 4.5)

        // Button text against primary cyan accent
        val ratioButtonText = contrastRatio(VoidBackground, PrimaryCyan)
        assertTrue("Dark text on cyan accent must be >= 4.5: was $ratioButtonText", ratioButtonText >= 4.5)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests com.example.ColorContrastTest`  
Expected: FAIL (unresolved references for new color tokens).

- [ ] **Step 3: Implement clean, restrained design tokens**
  - In `Color.kt`, replace generic neon colors with a professional, restrained dark palette:
    - `VoidBackground = Color(0xFF090D16)` (Obsidian foundation)
    - `SlateSurface = Color(0xFF141C2B)` (Matte elevated card)
    - `SlateSurfaceElevated = Color(0xFF1D283E)` (Modal/floating elevation)
    - `HairlineBorder = Color(0xFF26334A)` (Subtle 1dp perimeter)
    - `PrimaryCyan = Color(0xFF00D2E0)` (Single deliberate accent; high readability)
    - `StatusEmerald = Color(0xFF10B981)` (Verified safe link / Wi-Fi match)
    - `StatusAmber = Color(0xFFF59E0B)` (Caution / Retail item)
    - `StatusCrimson = Color(0xFFEF4444)` (Phishing / Security warning)
    - `TextHighEmphasis = Color(0xFFF8FAFC)`
    - `TextMediumEmphasis = Color(0xFF94A3B8)`
    - `TextMuted = Color(0xFF64748B)`
  - In `Type.kt`, establish strict typographic hierarchy with no arbitrary font sizes:
    - `HeadlineMedium` (20sp, Bold, tracking -0.5sp)
    - `TitleMedium` (16sp, SemiBold)
    - `BodyLarge` (15sp, Regular)
    - `BodySmall` (13sp, Medium)
    - `LabelSmall` (11sp, Monospace Bold for telemetry / tags)
  - In `Theme.kt`, wire `SwiftScanTheme` to expose these tokens cleanly.

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew testDebugUnitTest --tests com.example.ColorContrastTest`  
Expected: PASS with 100% WCAG AA contrast compliance.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/ui/theme/ app/src/test/java/com/example/ColorContrastTest.kt
git commit -m "style(theme): establish anti-slop restrained color tokens and WCAG AA typographic hierarchy"
```

---

### Task 2: Precision Optical Reticle HUD (`LaserReticleHUD.kt`)

**Files:**
- Create: `app/src/main/java/com/example/ui/components/LaserReticleHUD.kt`
- Modify: `app/src/main/java/com/example/ui/screens/ScannerScreen.kt`

**Interfaces:**
- Consumes: `isLocked: Boolean`, `lockElapsedMs: Long?`, `isAutoCopyActive: Boolean`.
- Produces: Professional optical viewfinder reticle with zero gimmicky neon sweeps.

- [ ] **Step 1: Write test for reticle state formatting**
Ensure lock status and elapsed time render clean monospace telemetry labels.

- [ ] **Step 2: Implement `LaserReticleHUD.kt`**
  - Draw precision 1.5dp corner brackets via Jetpack Compose `Canvas`.
  - Draw central optical hairline crosshair (`Color(0x3300D2E0)`).
  - State animation:
    - Idle: Hairline corners in `HairlineBorder` with subtle 200ms breathe.
    - Locked: Snaps to `StatusEmerald` with a crisp 100ms `FastOutSlowInEasing` bracket contraction (no neon beam).
  - Top telemetry bar: Clean monospace badge `[ AUTO-COPY: ACTIVE ]` and 48dp circular icon buttons for Torch and Lens Switch with crisp vector paths.

- [ ] **Step 3: Integrate into `ScannerScreen.kt`**
Replace existing canvas drawing in `ScannerScreen.kt` with `LaserReticleHUD`.

- [ ] **Step 4: Run unit tests**
Run: `./gradlew testDebugUnitTest`  
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/ui/components/LaserReticleHUD.kt app/src/main/java/com/example/ui/screens/ScannerScreen.kt
git commit -m "feat(hud): implement precision optical reticle HUD with zero slop animations"
```

---

### Task 3: Refined Turbo Action Pill (`TurboActionPill.kt`)

**Files:**
- Create: `app/src/main/java/com/example/ui/components/TurboActionPill.kt`
- Modify: `app/src/main/java/com/example/ui/screens/ScannerScreen.kt`

**Interfaces:**
- Consumes: `ParsedPayload`, `onPrimaryAction()`, `onSecondaryAction()`, `onDismiss()`.
- Produces: Concentric 20dp floating card with context-aware vector actions and 48dp minimum touch targets.

- [ ] **Step 1: Write component layout test**
Verify all 5 payload variants (URL, Wi-Fi, Product, vCard, Plain Text) supply valid accessibility content descriptions and minimum 48dp action heights.

- [ ] **Step 2: Implement `TurboActionPill.kt`**
  - Container: `background(SlateSurfaceElevated)` + `border(1.dp, HairlineBorder)` + `RoundedCornerShape(20.dp)`. No blur layers.
  - Drag handle: 32dp x 4dp muted pill at top.
  - Header: 36dp x 36dp vector icon badge + Title (15sp SemiBold) + Status tag (e.g. `[SAFE]`, `[WPA2]`, `[EAN-13]`).
  - Subtitle: 13sp secondary info (`TextMediumEmphasis`).
  - Action buttons:
    - Primary Button: `background(PrimaryCyan)` with `VoidBackground` text (14sp Bold), 48dp minimum height, `RoundedCornerShape(12.dp)`.
    - Secondary Button: `background(Color(0xFF222D42))` with `TextHighEmphasis`, 48dp minimum height.
  - Enter/Exit motion: Spring slide-in from bottom (`spring(dampingRatio = 0.8f, stiffness = 400f)`).

- [ ] **Step 3: Wire `TurboActionPill` into `ScannerScreen.kt`**
Position 16dp above the bottom navigation bar without obscuring the camera reticle.

- [ ] **Step 4: Run unit tests**
Run: `./gradlew testDebugUnitTest`  
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/ui/components/TurboActionPill.kt app/src/main/java/com/example/ui/screens/ScannerScreen.kt
git commit -m "feat(ui): implement refined non-blocking turbo action pill with concentric radii"
```

---

### Task 4: High-Craft Barcode Studio UI (`GenerateScreen.kt`)

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/GenerateScreen.kt`

**Interfaces:**
- Consumes: `BarcodeGenerator`.
- Produces: Minimalist, craft-focused barcode generation screen with format tabs and crisp preview.

- [ ] **Step 1: Write format selection test**
Ensure switching tabs between QR, Code 128, EAN-13, and Data Matrix updates the target aspect ratio and validation rules.

- [ ] **Step 2: Restructure `GenerateScreen.kt`**
  - Segmented control: Solid container (`SlateSurface`) with active indicator pill (`PrimaryCyan`).
  - Canvas card: Crisp off-white card with 20dp padding, sharp quiet zones, and barcode standard caption in monospace.
  - Input field: Clean border (`HairlineBorder`), clear label, helper text for input restrictions (e.g. "13 digits for EAN-13").
  - Palette swatches: Restrained palette with concentric active ring.
  - Export actions: 48dp Save PNG and Share buttons with vector icons.

- [ ] **Step 3: Run unit tests**
Run: `./gradlew testDebugUnitTest`  
Expected: PASS.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/GenerateScreen.kt
git commit -m "style(generator): restyle barcode studio with segmented format control and clean canvas"
```

---

### Task 5: Chronological Vault UI (`HistoryScreen.kt`)

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/HistoryScreen.kt`

**Interfaces:**
- Consumes: Grouped scan history list.
- Produces: Information-dense chronological feed with 1-tap copy actions.

- [ ] **Step 1: Write history item layout test**
Verify touch bounds for the copy and star buttons on each history card measure at least 48dp x 48dp.

- [ ] **Step 2: Restyle `HistoryScreen.kt`**
  - Section headers: Uppercase monospace date badges ("TODAY", "YESTERDAY", "EARLIER") with item count.
  - History card: Solid `SlateSurface` card with 1dp `HairlineBorder`.
  - Icon: Clean vector format icon (no emojis).
  - Actions: 48dp quick-copy icon button with ripple feedback + favorite star toggle.
  - Top search bar: Compact search field with clear button.

- [ ] **Step 3: Run unit tests**
Run: `./gradlew testDebugUnitTest`  
Expected: PASS.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/HistoryScreen.kt
git commit -m "style(history): restyle scan vault with chronological headers and 48dp quick actions"
```

---

### Task 6: Anti-Slop Audit & Delivery Gate Verification

**Files:**
- Audit Report: `docs/superpowers/specs/2026-11-01-antislop-audit.md`
- Compile check: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`

- [ ] **Step 1: Run Anti-Slop Delivery Gate Checklist (`antislop-ui`)**
  - [x] R-01: Zero generic blue-purple or neon gradients.
  - [x] R-07: Zero background tech/grid patterns.
  - [x] R-10: Max 1 glassmorphism element across entire screen.
  - [x] R-11: Concentric radii math strictly applied (`outer = inner + padding`).
  - [x] R-13: Zero ambient glows or glowing border lines.
  - [x] R-29: Palette capped at 2 core neutrals + 1 deliberate accent (`PrimaryCyan`).
  - [x] WCAG: 100% of text elements pass $\ge 4.5:1$ contrast against their immediate background.
  - [x] Touch: 100% of clickable elements have $\ge 48\text{dp} \times 48\text{dp}$ touch targets.

- [ ] **Step 2: Run all unit and Robolectric tests**
Run: `./gradlew testDebugUnitTest`  
Expected: 100% PASS.

- [ ] **Step 3: Assemble Debug APK**
Run: `./gradlew assembleDebug`  
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Package deliverable in `dist/SwiftScan.apk`**
```bash
Copy-Item app/build/outputs/apk/debug/app-debug.apk dist/SwiftScan.apk -Force
```

- [ ] **Step 5: Final commit**
```bash
git add dist/SwiftScan.apk docs/superpowers/specs/2026-11-01-antislop-audit.md
git commit -m "chore(release): verify anti-slop delivery gate and assemble clean SwiftScan APK"
```
