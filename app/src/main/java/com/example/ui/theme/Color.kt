package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// SwiftScan Titanium & Cobalt Design System Tokens
val VoidCanvas = Color(0xFF0B0D11)          // Base canvas background (OKLCH 0.13 0.008 260)
val SurfaceSlate = Color(0xFF161920)        // Neutral card surface (OKLCH 0.19 0.012 260)
val SurfaceElevated = Color(0xFF20242D)     // Floating pill / modal surface (OKLCH 0.23 0.015 260)
val HairlineBorder = Color(0xFF2C323E)      // Subtle 1dp structural border (OKLCH 0.28 0.015 260)
val PrimaryCobalt = Color(0xFF2563EB)       // Electric Cobalt (WCAG AA 5.17:1 with White text)
val StatusEmerald = Color(0xFF10B981)       // Verified safe / Wi-Fi (OKLCH 0.72 0.17 160)
val StatusAmber = Color(0xFFF59E0B)         // Retail / ISBN barcode (OKLCH 0.75 0.16 75)
val StatusCrimson = Color(0xFFEF4444)       // Threat / Phishing warning (OKLCH 0.63 0.22 25)
val TextHighEmphasis = Color(0xFFF8FAFC)    // 16.5:1 contrast against VoidCanvas
val TextMediumEmphasis = Color(0xFF94A3B8)  // 5.8:1 contrast against SurfaceSlate
val TextLowEmphasis = Color(0xFF64748B)

// Aliases for seamless migration
val LaserCyan = PrimaryCobalt
val SafeEmerald = StatusEmerald
val CautionAmber = StatusAmber
val AlertCrimson = StatusCrimson
val ObsidianBase = VoidCanvas
val SurfaceElevatedHigh = SurfaceElevated
val SurfaceGlass = SurfaceElevated
val TextPrimary = TextHighEmphasis
val TextMuted = TextMediumEmphasis
val DarkBorder = HairlineBorder
val ElectricIndigo = PrimaryCobalt
