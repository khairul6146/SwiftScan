# SwiftScan Turbo Personal Revamp Design Document

**Date:** 2026-11-01  
**Author:** Pair Programming Session with User  
**Status:** Ready for Review & Planning  
**Target Architecture:** Android Native (Jetpack Compose, CameraX, Google ML Kit, Room SQLite)  
**Governing UI/UX Standards:** `better-ui`, `better-layout`, `better-colors`, `better-accessibility`, `DESIGN.md`  

---

## 1. Intent & The "Why"
The user requires a **superfast, privacy-first barcode and QR scanner tailored for personal daily use**. The existing implementation suffered from an artificial 180ms frame analysis delay, coupled heavy OCR text models into the barcode analyzer pipeline, and blocked user interaction with full-screen modal sheets for every scan.

This revamp transforms SwiftScan into an instant, zero-friction scanning engine with sub-30ms decode speed, context-aware smart action pills, hands-free auto-copy capabilities, and expanded code generation/history features, while preserving the app's zero-telemetry and ad-free privacy contract.

---

## 2. Goals, Non-Goals, and Anti-Goals

### Goals
* **Sub-30ms Optical Decode:** Decouple ML Kit Barcode Analyzer from OCR Text Recognizer; lock CameraX analysis resolution to 720p with `STRATEGY_KEEP_ONLY_LATEST` buffer management.
* **Non-Blocking Turbo Floating Pill:** Replace blocking bottom sheets with an interactive floating pill positioned above the bottom navigation bar, leaving the live camera active.
* **Context-Aware Smart Type Detection:**
  * **Wi-Fi:** Parse `WIFI:S:...;P:...;` $\to$ 1-tap Android Wi-Fi connect suggestion + Copy Password.
  * **URLs:** Strip marketing tracking parameters (`utm_*`, `fbclid`, `gclid`), verify domain spoofing/punycode, and offer 1-tap browser launch.
  * **Retail Barcodes (EAN-13, UPC-A, ISBN, Code 128):** Display formatted product number $\to$ 1-tap Web/Shopping search + Copy.
  * **Contacts (vCard):** Parse name, phone, email $\to$ 1-tap Save Contact + Direct Dial.
  * **Plain Text:** 1-tap Copy to Clipboard + Search.
* **Hands-Free Auto-Copy Mode:** Fast toggle in the top HUD to automatically copy scanned contents to clipboard with a 40ms tactile haptic chirp and emerald reticle flash.
* **UI/UX Polish (`better-ui`, `better-layout`, `better-colors`, `better-accessibility`):**
  * Concentric corner radii: `outerRadius = innerRadius + padding` (e.g., card 24dp, inner item 16dp, padding 8dp).
  * Minimum 48dp touch targets for all interactive icons and buttons.
  * High-contrast dark theme HUD with Laser Cyan (`#00F0FF`) and Safe Emerald (`#10B981`) accent indicators.
  * Tactile haptic feedback: 40ms crisp click on decode (`VibrationEffect.EFFECT_CLICK`).
* **Expanded Code Studio Generator:** Generate 1D (Code 128, EAN-13) and 2D (QR Code, Data Matrix) with live preview and PNG/SVG export.
* **Chronological History Feed:** Group history items by "Today", "Yesterday", and "Earlier", with 1-tap copy, favorite toggle, and search.

### Non-Goals
* Paid cloud backups or remote telemetry (all database and vision processing remain 100% on-device).
* Commercial enterprise barcode wedge / MQTT server integrations (this is explicitly tuned for personal daily convenience).

### Anti-Goals
* Never display advertisements, tracking SDKs, or third-party web tracking cookies.
* Never auto-open malicious unverified URLs without anti-quishing inspection.
* Never freeze or tear the CameraX frame buffer when a scan is detected.

---

## 3. Approaches Considered & Tradeoffs

### Approach 1: Turbo Swift Pipeline (Selected)
* **Architecture:** Decouple barcode analyzer from OCR; lock CameraX to 720p with `STRATEGY_KEEP_ONLY_LATEST`. Replace modal dialogs with a floating, non-blocking contextual action pill at the bottom of the camera viewfinder.
* **Tradeoffs:** Gives sub-30ms decode and zero-friction personal speed while preserving full screen visibility and continuous scanning. Full security inspection remains 1 tap away.
* **Selection Reason:** User selected Option A for its seamless balance of speed, context awareness, and non-blocking viewfinder experience.

### Approach 2: Zero-Click Autonomous Scanner (Rejected)
* **Architecture:** Automatically launch links in browser or copy text without any on-screen result UI.
* **Tradeoffs:** Extremely fast for repetitive tasks, but takes away user agency and risks launching malicious URLs unintentionally.

### Approach 3: Pure Optical Engine Optimization (Rejected)
* **Architecture:** Optimize only the background CameraX pipeline and keep the existing blocking bottom sheet.
* **Tradeoffs:** Faster camera detection, but user is still forced to dismiss modal sheets for every scan, slowing down personal daily use.

---

## 4. Architectural & System Design

### 4.1 Vision & Camera Engine Pipeline
```
CameraX LifecycleCameraController (Back Camera / 720p)
   │
   ├─► PreviewView (Continuous 60 FPS Viewfinder)
   │
   └─► ImageAnalysis (STRATEGY_KEEP_ONLY_LATEST)
         │
         ├── ScanMode.SINGLE / BATCH:
         │     └─► BarcodeScanning (Isolated - NO OCR in pipeline)
         │           │
         │           └─► On Barcode Lock (< 30ms):
         │                 ├─► Haptic Laser Chirp (40ms)
         │                 ├─► Visual Reticle Lock (Emerald #10B981)
         │                 ├─► If Auto-Copy ON: System Clipboard.setPrimaryClip()
         │                 ├─► Parse Content Type (Wi-Fi, URL, Product, Contact, Text)
         │                 ├─► Dispatch to Room Database (Non-blocking async)
         │                 └─► Morph Turbo Floating Action Pill
         │
         └── ScanMode.OCR (Explicit user toggle only):
               └─► TextRecognition (Isolated Latin Text Recognizer)
```

### 4.2 UI Design Specifications (`better-ui`, `better-colors`, `better-layout`, `better-accessibility`)
* **Color Tokens:**
  * Background Void: `#080D1A`
  * Surface Dark: `#0F172A`
  * Surface Elevated: `#1E293B`
  * Border Subdued: `rgba(255, 255, 255, 0.12)`
  * Accent Primary (Laser Cyan): `#00F0FF`
  * State Success (Safe Emerald): `#10B981`
  * State Caution (Amber): `#F59E0B`
  * State Danger (Crimson): `#EF4444`
* **Concentric Radii Hierarchy:**
  * Container Floating Pill: `24dp`
  * Inner Primary Action Buttons: `14dp`
  * Icon Circle Buttons: `CircleShape` (48dp x 48dp minimum touch target)
* **Motion & Animation:**
  * Reticle Lock-On: `FastOutSlowInEasing` 150ms snap animation.
  * Floating Action Pill: `slideInVertically` from bottom with 200ms `spring(stiffness = Spring.StiffnessMediumLow)`.
  * Auto-dismiss / Timeout: Floating pill stays visible until user taps an action, scans another code, or dismisses via swipe down.

---

## 5. What is Left to the Builder (Implementation Plan Scope)
1. **Engine:** Decouple `BarcodeScanning` and `TextRecognition` in `ScannerScreen.kt`, remove the 180ms sleep throttle, set `STRATEGY_KEEP_ONLY_LATEST` and optimal 720p analysis target.
2. **Component:** Build the new `TurboActionPill.kt` with context-aware buttons (Wi-Fi, Clean URL, Product Barcode, vCard, Plain Text).
3. **Utilities:** Enhance `ScanUtils.kt` and `SecurityUtils.kt` with direct Wi-Fi suggestions, product barcode categorization, and contact extraction.
4. **Generator:** Update `QrGenerator.kt` to `BarcodeGenerator.kt` supporting Code 128, EAN-13, and Data Matrix.
5. **History:** Refactor `HistoryScreen.kt` with grouped date headers ("Today", "Yesterday", "Earlier") and instant 1-tap clipboard copy.
