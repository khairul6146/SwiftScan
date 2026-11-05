# SwiftScan Project Agent Router (`.agents/AGENTS.md`)

## 1. Operational Rules
1. **Target Directory:** All code and assets reside inside the project workspace repository.
2. **Architecture Standard:** Android Jetpack Compose + CameraX + Google ML Kit Barcode Scanning + Room SQLite.
3. **Design Contract:** Follow [DESIGN.md](../DESIGN.md) for all colors, radii, motion timings, and UI layouts.
4. **Lightweight Execution:** Zero unnecessary third-party dependencies. Rely on Android native APIs and Google ML Kit.
5. **Security First:** Always parse and inspect URLs through `SecurityUtils` before allowing intent dispatches.
6. **Build Deliverable:** Must produce a clean, installable `.apk` file (`dist/SwiftScan.apk`).

---

## 2. Skill Routing Matrix

### Core UI & Compose Components
* **Triggers:** `Compose`, `ScannerScreen`, `ScanResultSheet`, `GenerateScreen`, `Theme`, `HUD`, `Reticle`
* **Governing Skills:** `better-ui`, `better-layout`, `better-colors`, `better-accessibility`
* **Behavior:** Enforce concentric radii (`outer = inner + padding`), optical center alignment, and hardware-accelerated Canvas drawing.

### Barcode & Security Engine
* **Triggers:** `ML Kit`, `Barcode`, `Quishing`, `Phishing`, `URL Unpack`, `CameraX`, `ZXing`
* **Behavior:** Low-latency on-device processing with zero telemetry, tracker parameter stripping, and domain safety scoring.

### Database & Batch Operations
* **Triggers:** `Room`, `ScanResult`, `ScanDao`, `History`, `CSV Export`, `JSON Export`, `Batch Mode`
* **Behavior:** Real-time state flows, Room SQLite transactions, and rapid batch concurrency.

### Build & APK Packaging
* **Triggers:** `Gradle`, `APK`, `assembleDebug`, `release`, `Android SDK`
* **Behavior:** Gradle build automation and delivering compiled APKs into `dist/`.
