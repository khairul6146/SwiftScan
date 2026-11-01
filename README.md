# SwiftScan

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Compose-BOM_2024.04.01-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![ML Kit](https://img.shields.io/badge/Google_ML_Kit-Vision_&_OCR-EA4335.svg?style=flat&logo=google)](https://developers.google.com/ml-kit)
[![Design](https://img.shields.io/badge/Design-Titanium_&_Cobalt-2563EB.svg?style=flat)](DESIGN.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

An ultra-fast, privacy-first, ad-free mobile vision and barcode scanning power tool for Android. Crafted under strict anti-slop design principles with the **Titanium & Cobalt** design system, sub-100ms hardware-accelerated decode, direct part marking (DPM / 3D relief) inversion, high-throughput batch logistics, damaged-code OCR fallback, non-blocking Turbo Action Pill, and built-in anti-quishing cybersecurity protection.

---

## ⚡ What Makes SwiftScan Different?

Most QR scanners on Google Play are bloated with full-screen ads, track user data, and choke on industrial labels or damaged barcodes. **SwiftScan** is built with zero advertising, pure local hardware acceleration, and advanced vision capabilities designed for both everyday users and industrial warehouse/manufacturing environments.

| Capability | Standard Mobile Scanners | SwiftScan |
| :--- | :--- | :--- |
| **Monetization & Privacy** | Ads, trackers, data harvesting | **100% Ad-Free, Local-Only, Zero Telemetry** |
| **Latency & Architecture** | 400ms – 1200ms latency, heavy web wrappers | **< 100ms camera-to-action**, native CameraX + Jetpack Compose |
| **UX & Viewfinder** | Intrusive full-screen modal sheets | **Non-blocking Turbo Action Pill** anchored above navigation |
| **Anti-Slop Design** | Cheesy neon glows, laser animations, generic gradients | **Titanium & Cobalt**, 1.5px optical reticle, concentric radii |
| **3D / DPM Relief & Dot Peen** | Fails completely on low-contrast metal codes | **Luminance Inversion engine (\(255 - Y\)) & contrast stretching** |
| **Damaged / Scratched Codes** | Inability to decode causes workflow stoppage | **Instant OCR Fallback** extracts serial & part numbers automatically |
| **Security (Anti-Quishing)** | Blindly auto-opens malicious URLs | **Proactive Threat Analysis** (punycode, unmasked IPs, tracking stripping) |
| **Batch Scanning** | Paid premium paywall | **Native continuous batching** with deduplication and CSV/JSON export |

---

## 🛠️ Supported Formats & Vision Matrix

### 1. 2D Matrix Codes
* **QR Code** (Model 1 & 2, Micro QR)
* **Data Matrix** (Automotive & Aerospace ECC 200)
* **Aztec Code** (Transit & ticketing)
* **PDF417** (ID cards, boarding passes, shipping logistics)

### 2. 1D Linear Barcodes
* **Logistics & Retail:** EAN-13, EAN-8, UPC-A, UPC-E
* **Industrial & Shipping:** Code 128, Code 39, Code 93, Codabar, ITF (Interleaved 2 of 5)

### 3. Direct Part Marking (DPM) & 3D Relief Codes
* **Luminance Inversion:** Real-time inverted code detection for laser-etched, chemically etched, or dot-peened codes on brushed aluminum, cast iron, and dark PCBs.
* **Dynamic Range Stretching:** Expands low-contrast metallic gradients to full \(0 \to 255\) brightness.

### 4. Severe Damage OCR Serial Fallback
* When print is ripped, smudged, or bleached, switch to **OCR Mode** to extract printed human-readable text.
* Automatic heuristic parsing for:
  * Labeled serials: `SN:`, `S/N`, `SERIAL`, `LOT:`, `VIN:`, `PART:`, `BATCH:`
  * Structured codes: e.g. `PART-84921-X`, `2026-X8491`
  * Raw tracking identifiers and tokens

---

## 📱 User Interface & Design Contract

SwiftScan is crafted under strict design engineering principles (documented in [`DESIGN.md`](DESIGN.md)):
* **Titanium & Cobalt Palette:** Pure obsidian canvas (`#0B0D11`), elevated slate surfaces (`#161920`, `#20242D`), hairline borders (`#2C323E`), and a deliberate Cobalt Blue accent (`#2563EB`).
* **Precision Optical Reticle HUD:** Crisp 1.5px/2px corner brackets, central optical crosshair, and real-time decode telemetry badge.
* **Non-Blocking Turbo Action Pill:** Concentric 20dp floating card with context-aware quick actions (Wi-Fi, URL, Products, vCard, Plain Text).
* **Guaranteed 48dp Touch Targets:** 100% of interactive controls pass minimum $48\text{dp} \times 48\text{dp}$ accessibility bounds.
* **Modern Adaptive Icon:** Precision Optical Reticle design with Android 13/14/15 Material You themed icon support.

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Ladybug / Meerkat or newer
* Android SDK 34+
* JDK 17 (Microsoft Build of OpenJDK or Temurin)
* Device running Android 8.0 (API level 26) or higher

### Build & Run from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/khairul6146/SwiftScan.git
   cd SwiftScan
   ```

2. Run unit & integration tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```

3. Assemble debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
   The compiled APK will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## 📦 Direct APK Download

For quick installation, pre-built APK binaries are packaged inside the repository:
* **Release Artifact:** [`dist/SwiftScan.apk`](dist/SwiftScan.apk) (~86.3 MB with full ML Kit models bundled)

---

## 🛡️ Security & Anti-Phishing Engine

SwiftScan inspects every scanned URL before allowing interaction:
* **Tracking Stripper:** Strips `utm_*`, `fbclid`, `gclid`, and affiliate parameters.
* **Malicious TLD & Extension Detection:** Flags suspicious `.exe`, `.apk`, `.zip`, `.scr` downloads.
* **Punycode / Homograph Warning:** Detects deceptive domain spoofing.
* **Raw IP Detection:** Warns against non-hostname destination addresses.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
