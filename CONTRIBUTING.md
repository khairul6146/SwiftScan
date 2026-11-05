# Contributing to SwiftScan

Thank you for your interest in improving **SwiftScan**!

## Code of Conduct

Please be respectful, constructive, and collaborative in all discussions, issues, and pull requests.

## How Can I Contribute?

* **Bug Reports:** Open an issue using the Bug Report template with device model, Android version, and reproduction steps.
* **Feature Suggestions:** Share your ideas or enhancements via the Feature Request template.
* **Pull Requests:** Bug fixes, performance optimizations, and accessibility enhancements are welcome.

## Development Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/khairul6146/SwiftScan.git
   cd SwiftScan
   ```

2. **Open in Android Studio:**
   - Recommended: Android Studio Ladybug / Meerkat or newer.
   - JDK: Java 17+.
   - Target SDK: Android 14/15 (API 34+).

3. **Run Unit Tests:**
   Ensure all automated unit and Robolectric tests pass:
   ```bash
   ./gradlew testDebugUnitTest
   ```

4. **Assemble Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```

## Design & Engineering Standards

* **Anti-Slop Philosophy:** Follow the guidelines in [`DESIGN.md`](DESIGN.md). Avoid unnecessary dependencies, gratuitous animations, fake laser glows, or unneeded third-party libraries.
* **Concentric Radii:** All UI elements should respect concentric corner ratios (`outerRadius = innerRadius + padding`).
* **Accessibility:** All clickable elements must maintain a minimum $48\text{dp} \times 48\text{dp}$ touch target and pass WCAG AA contrast ($4.5:1$).
* **Privacy First:** SwiftScan does not collect telemetry, analytics, or crash reports. Never introduce tracking libraries or network egress without explicit, opt-in user control.

## Submitting Pull Requests

1. Create a feature branch: `git checkout -b feat/your-feature-name`.
2. Follow [Conventional Commits](https://www.conventionalcommits.org/):
   - `feat: ...` for new features
   - `fix: ...` for bug fixes
   - `style: ...` for visual/design changes
   - `docs: ...` for documentation
3. Ensure all tests pass before opening your PR.
