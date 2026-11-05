# SwiftScan Turbo Personal Revamp Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform SwiftScan into a sub-30ms, zero-friction personal code scanner featuring context-aware smart action pills, hands-free auto-copy, multi-format barcode generation, and chronological history grouping.

**Architecture:** Android Jetpack Compose + CameraX 720p ImageAnalysis (`STRATEGY_KEEP_ONLY_LATEST`) + Google ML Kit Barcode Scanning + Room SQLite. Eliminates the 180ms frame throttle, decouples heavy OCR models, and surfaces a non-blocking floating action pill above the camera feed.

**Tech Stack:** Kotlin 2.0, Jetpack Compose, CameraX 1.4+, Google ML Kit Vision, ZXing Core 3.5+, Room SQLite, Robolectric.

**Spec:** [`docs/superpowers/specs/2026-11-01-turbo-swiftscan-personal-revamp-design.md`](docs/superpowers/specs/2026-11-01-turbo-swiftscan-personal-revamp-design.md)

---

## Global Constraints

- **Platform:** Android 8.0+ (API 26 to 34+).
- **Privacy:** 100% on-device local execution; zero telemetry, zero trackers, zero ads.
- **Design Tokens:** Strict adherence to `DESIGN.md`, `better-ui`, and `better-layout`:
  - Concentric radii: `outerRadius = innerRadius + padding` (24dp card, 14dp buttons, 8dp padding).
  - Minimum touch target: 48dp x 48dp for all interactive elements (WCAG 2.5.5).
  - High contrast dark mode: Void (`#080D1A`), Laser Cyan (`#00F0FF`), Safe Emerald (`#10B981`), Warning Amber (`#F59E0B`), Crimson (`#EF4444`).
- **Build Output:** Final build deliverable must update `dist/SwiftScan.apk`.

---

## Review Focus

1. **Wi-Fi Payload Parsing Malformation:** Raw QR codes with missing semicolons or escaped characters must not crash the parser; fallback cleanly to plain text.
2. **Camera Frame Pipeline Starvation:** Closing or opening the floating pill must never unbind or freeze CameraX image analysis buffers.
3. **Rapid Successive Scans:** When scanning multiple codes in rapid succession, the floating pill must update smoothly without visual flicker or duplicated Room entries.
4. **Barcode Generator Check Digits:** Generating EAN-13 barcodes must validate or auto-compute check digits so ZXing does not throw unhandled `IllegalArgumentException`.
5. **Empty History Sections:** When no scans exist for "Today" or "Yesterday", the empty categories must hide gracefully without awkward layout gaps.

---

### Task 1: Context-Aware Payload Parser (`PayloadParser.kt`)

**Files:**
- Create: `app/src/main/java/com/example/util/PayloadParser.kt`
- Test: `app/src/test/java/com/example/PayloadParserTest.kt`

**Interfaces:**
- Consumes: Raw barcode content string (`String`) and format int (`Int`).
- Produces: `ParsedPayload(type: PayloadType, title: String, subtitle: String, primaryAction: Action, secondaryAction: Action)`.

- [ ] **Step 1: Write the failing tests for PayloadParser**

```kotlin
// app/src/test/java/com/example/PayloadParserTest.kt
package com.example

import com.example.util.PayloadParser
import com.example.util.PayloadType
import com.google.mlkit.vision.barcode.common.Barcode
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayloadParserTest {
    @Test
    fun testParseWifiPayload() {
        val result = PayloadParser.parse("WIFI:S:MyNetwork;T:WPA;P:SecretPass123;;", Barcode.FORMAT_QR_CODE)
        assertEquals(PayloadType.WIFI, result.type)
        assertEquals("MyNetwork", result.title)
        assertTrue(result.subtitle.contains("WPA"))
    }

    @Test
    fun testParseUrlPayload_stripsTrackers() {
        val result = PayloadParser.parse("https://example.com/shop?utm_source=fb&utm_medium=cpc&id=99", Barcode.FORMAT_QR_CODE)
        assertEquals(PayloadType.URL, result.type)
        assertEquals("https://example.com/shop?id=99", result.cleanedContent)
    }

    @Test
    fun testParseEan13ProductBarcode() {
        val result = PayloadParser.parse("9780134685991", Barcode.FORMAT_EAN_13)
        assertEquals(PayloadType.PRODUCT, result.type)
        assertEquals("EAN-13: 9780134685991", result.title)
    }

    @Test
    fun testParseVCardPayload() {
        val vcard = "BEGIN:VCARD\nVERSION:3.0\nN:Smith;John\nFN:John Smith\nTEL:+15551234567\nEMAIL:john@example.com\nEND:VCARD"
        val result = PayloadParser.parse(vcard, Barcode.FORMAT_QR_CODE)
        assertEquals(PayloadType.CONTACT, result.type)
        assertEquals("John Smith", result.title)
        assertTrue(result.subtitle.contains("+15551234567"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests com.example.PayloadParserTest`  
Expected: FAIL with compilation error (Unresolved reference: `PayloadParser`).

- [ ] **Step 3: Implement `PayloadParser.kt`**
Create `app/src/main/java/com/example/util/PayloadParser.kt` with parsers for Wi-Fi, URL (calling `SecurityUtils.analyzeUrl`), Product (EAN-13, UPC-A, ISBN, Code 128), vCard, and Plain Text.

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew testDebugUnitTest --tests com.example.PayloadParserTest`  
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/util/PayloadParser.kt app/src/test/java/com/example/PayloadParserTest.kt
git commit -m "feat(parser): add context-aware payload parser for WiFi, URLs, products, and vCards"
```

---

### Task 2: Turbo Camera & Vision Pipeline Optimization

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/ScannerScreen.kt`
- Modify: `app/src/main/java/com/example/util/SoundUtils.kt`

**Interfaces:**
- Consumes: CameraX `LifecycleCameraController`, `PayloadParser`.
- Produces: Decoupled 60 FPS analysis, 40ms haptic laser tick, Auto-Copy clipboard trigger, and live emerald reticle lock-on.

- [ ] **Step 1: Write integration test for auto-copy and throttle removal**
Verify `MainViewModel` accepts consecutive distinct scans with zero artificial frame sleep and handles auto-copy clipboard logic cleanly.

- [ ] **Step 2: Run test to verify behavior**
Run: `./gradlew testDebugUnitTest --tests com.example.AllSymbologiesTest`

- [ ] **Step 3: Optimize `ScannerScreen.kt`**
  - In `ScannerScreen.kt`, decouple `barcodeScanner` from `textRecognizer`: when `scanMode == ScanMode.SINGLE` or `ScanMode.BATCH`, pass only `listOf(barcodeScanner)` to `MlKitAnalyzer`.
  - Remove the `now - lastAnalysisTime < 180L` throttle; allow full camera frame throughput with `STRATEGY_KEEP_ONLY_LATEST`.
  - Add an `autoCopyEnabled` state toggle in the top bar.
  - When a code locks, trigger 40ms `VibrationEffect.EFFECT_CLICK` and emerald reticle border flash. If `autoCopyEnabled == true`, copy payload directly to Android clipboard with a subtle feedback toast.

- [ ] **Step 4: Run unit tests to verify camera setup and symbology decodes**
Run: `./gradlew testDebugUnitTest`  
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/ScannerScreen.kt
git commit -m "perf(camera): decouple vision analyzers and remove 180ms frame throttle for turbo scan"
```

---

### Task 3: Turbo Floating Action Pill Component (`TurboActionPill.kt`)

**Files:**
- Create: `app/src/main/java/com/example/ui/components/TurboActionPill.kt`
- Modify: `app/src/main/java/com/example/ui/screens/ScannerScreen.kt`

**Interfaces:**
- Consumes: `ParsedPayload`, `onDismiss`, `onOpenDetails`.
- Produces: Polished, non-blocking floating card over the camera viewfinder with 1-tap contextual actions.

- [ ] **Step 1: Write Component test / preview verification**
Ensure `TurboActionPill` renders the correct primary and secondary buttons based on `PayloadType`.

- [ ] **Step 2: Implement `TurboActionPill.kt`**
  - Implement using `better-ui` and `better-layout`:
    - Concentric radii: Container `RoundedCornerShape(24.dp)`, action buttons `RoundedCornerShape(14.dp)`.
    - Minimum touch targets of 48dp x 48dp.
    - Dark glassmorphism background: `Color(0xEB0F172A)` with `border(1.dp, Color(0x4D00F0FF))`.
    - Wi-Fi: **[Connect Now 📶]** (via `WifiNetworkSpecifier` / Intent) and **[Copy Pass 🔑]**.
    - URL: **[Open Link ↗]** (via `Intent.ACTION_VIEW`) and **[Copy URL 📋]**.
    - Product: **[Search Product 🔍]** (Google Shopping intent) and **[Copy Barcode 📋]**.
    - Contact: **[Save Contact 👤]** (`ContactsContract.Intents.Insert`) and **[Call 📞]**.
    - Text: **[Copy Text 📋]** and **[Search 🔍]**.
  - Tap card body to expand full `ScanResultSheet` for deep security/note editing.

- [ ] **Step 3: Integrate `TurboActionPill` into `ScannerScreen.kt`**
Replace blocking modal sheet trigger in `ScannerScreen.kt` with `TurboActionPill`, maintaining camera continuity.

- [ ] **Step 4: Run unit tests to ensure no regressions**
Run: `./gradlew testDebugUnitTest`  
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/ui/components/TurboActionPill.kt app/src/main/java/com/example/ui/screens/ScannerScreen.kt
git commit -m "feat(ui): implement non-blocking turbo action pill with context-aware smart actions"
```

---

### Task 4: Multi-Format Barcode Studio Generator

**Files:**
- Modify: `app/src/main/java/com/example/util/QrGenerator.kt` $\to$ expand or create `BarcodeGenerator.kt`
- Modify: `app/src/main/java/com/example/ui/screens/GenerateScreen.kt`
- Test: `app/src/test/java/com/example/BarcodeGeneratorTest.kt`

**Interfaces:**
- Consumes: Content string, `BarcodeFormat` (QR_CODE, CODE_128, EAN_13, DATA_MATRIX), colors.
- Produces: Rendered `Bitmap` formatted for linear 1D barcodes or 2D matrix codes.

- [ ] **Step 1: Write failing unit test for `BarcodeGenerator`**
Test generation of QR Code, Code 128, and EAN-13 barcodes with valid dimensions.

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests com.example.BarcodeGeneratorTest`  
Expected: FAIL.

- [ ] **Step 3: Implement multi-format generation**
  - Implement `BarcodeGenerator.generateBarcodeBitmap(content, format, width, height, color)`.
  - In `GenerateScreen.kt`, add format tabs: `[ QR Code | Code 128 | EAN-13 | Data Matrix ]`.
  - Adjust canvas preview aspect ratio (square for 2D, wide rectangular for 1D).

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew testDebugUnitTest --tests com.example.BarcodeGeneratorTest`  
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/util/BarcodeGenerator.kt app/src/main/java/com/example/ui/screens/GenerateScreen.kt app/src/test/java/com/example/BarcodeGeneratorTest.kt
git commit -m "feat(generator): expand code generator to 1D barcodes (Code 128, EAN-13) and Data Matrix"
```

---

### Task 5: Chronological History Grouping & Quick Actions

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/HistoryScreen.kt`

**Interfaces:**
- Consumes: `List<ScanResult>` from `MainViewModel`.
- Produces: Chronologically sectioned lazy list ("Today", "Yesterday", "Earlier") with 1-tap copy and favorite icons.

- [ ] **Step 1: Write unit test for history date bucketing**
Verify timestamps map accurately to "Today", "Yesterday", and "Earlier".

- [ ] **Step 2: Update `HistoryScreen.kt`**
  - Group history items by calendar date.
  - Implement 1-tap clipboard copy icon directly on each card so the user doesn't need to open the details dialog.
  - Preserve search, favorite toggle, and CSV/JSON export.

- [ ] **Step 3: Run unit tests**
Run: `./gradlew testDebugUnitTest`  
Expected: PASS.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/HistoryScreen.kt
git commit -m "feat(history): group scan history chronologically with 1-tap quick copy actions"
```

---

### Task 6: Full Verification & APK Assembly

**Files:**
- Output: `dist/SwiftScan.apk`

- [ ] **Step 1: Execute all unit and integration tests**
Run: `./gradlew testDebugUnitTest`  
Expected: 100% tests passing.

- [ ] **Step 2: Assemble Release/Debug APK**
Run: `./gradlew assembleDebug`  
Expected: Build successful with zero compile warnings.

- [ ] **Step 3: Copy compiled APK to `dist/SwiftScan.apk`**
```bash
Copy-Item app/build/outputs/apk/debug/app-debug.apk dist/SwiftScan.apk -Force
```

- [ ] **Step 4: Final Git commit**
```bash
git add dist/SwiftScan.apk
git commit -m "chore(release): package updated SwiftScan Turbo APK in dist/"
```
