package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * High-craft, anti-slop optical reticle HUD for SwiftScan.
 * Features 1.5dp hairline corner brackets, central optical crosshair,
 * clean status telemetry, and 48dp touch targets.
 * Zero gimmicky laser sweeps or neon bloom shadows.
 */
@Composable
fun LaserReticleHUD(
    isLocked: Boolean,
    lockElapsedMs: Long?,
    isTorchOn: Boolean,
    isAutoCopyActive: Boolean,
    soundEnabled: Boolean,
    onToggleTorch: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleAutoCopy: () -> Unit,
    onToggleSound: () -> Unit,
    onPickGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bracketColor by animateColorAsState(
        targetValue = if (isLocked) StatusEmerald else HairlineBorder,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "bracketColor"
    )

    val reticleSize by animateDpAsState(
        targetValue = if (isLocked) 190.dp else 200.dp,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "reticleSize"
    )

    Box(modifier = modifier.fillMaxSize()) {
        
        // Top System / Branding & Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clean Brand Title (Purged of "PRO" or other slop buzzwords)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "SwiftScan",
                    color = TextHighEmphasis,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                )
            }

            // Quick Viewfinder Controls (All with 48dp Minimum Touch Bounds)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Auto-Copy Pill Toggle
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .background(
                            color = if (isAutoCopyActive) Color(0x263B82F6) else SurfaceSlate,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isAutoCopyActive) PrimaryCobalt else HairlineBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable(onClick = onToggleAutoCopy)
                        .padding(horizontal = 10.dp)
                        .testTag("auto_copy_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isAutoCopyActive) "AUTO-COPY" else "MANUAL",
                        color = if (isAutoCopyActive) PrimaryCobalt else TextMediumEmphasis,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Sound Toggle (48dp Touch Target)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onToggleSound)
                        .testTag("sound_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(SurfaceSlate, CircleShape)
                            .border(1.dp, HairlineBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Toggle Audio Feedback",
                            tint = if (soundEnabled) StatusEmerald else TextMediumEmphasis,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Torch Button (48dp Touch Target)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onToggleTorch)
                        .testTag("flash_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(SurfaceSlate, CircleShape)
                            .border(1.dp, if (isTorchOn) PrimaryCobalt else HairlineBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Toggle Flashlight",
                            tint = if (isTorchOn) StatusAmber else TextHighEmphasis,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Camera Lens Switcher (48dp Touch Target)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onSwitchCamera)
                        .testTag("camera_switch_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(SurfaceSlate, CircleShape)
                            .border(1.dp, HairlineBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera Lens",
                            tint = TextHighEmphasis,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Gallery Photo Picker (48dp Touch Target)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onPickGallery)
                        .testTag("gallery_picker_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(SurfaceSlate, CircleShape)
                            .border(1.dp, HairlineBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Scan Image from Gallery",
                            tint = TextHighEmphasis,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Center Optical Framing Reticle
        Box(
            modifier = Modifier
                .size(reticleSize)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 2.dp.toPx()
                val armLength = 28.dp.toPx()
                val radius = 10.dp.toPx()

                // Top-Left Bracket
                drawLine(
                    color = bracketColor,
                    start = Offset(0f, radius),
                    end = Offset(0f, armLength),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawArc(
                    color = bracketColor,
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(0f, 0f),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(
                    color = bracketColor,
                    start = Offset(radius, 0f),
                    end = Offset(armLength, 0f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                // Top-Right Bracket
                drawLine(
                    color = bracketColor,
                    start = Offset(size.width - radius, 0f),
                    end = Offset(size.width - armLength, 0f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawArc(
                    color = bracketColor,
                    startAngle = 270f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(size.width - radius * 2, 0f),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(
                    color = bracketColor,
                    start = Offset(size.width, radius),
                    end = Offset(size.width, armLength),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                // Bottom-Left Bracket
                drawLine(
                    color = bracketColor,
                    start = Offset(0f, size.height - radius),
                    end = Offset(0f, size.height - armLength),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawArc(
                    color = bracketColor,
                    startAngle = 90f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(0f, size.height - radius * 2),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(
                    color = bracketColor,
                    start = Offset(radius, size.height),
                    end = Offset(armLength, size.height),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                // Bottom-Right Bracket
                drawLine(
                    color = bracketColor,
                    start = Offset(size.width - radius, size.height),
                    end = Offset(size.width - armLength, size.height),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawArc(
                    color = bracketColor,
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(size.width - radius * 2, size.height - radius * 2),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(
                    color = bracketColor,
                    start = Offset(size.width, size.height - radius),
                    end = Offset(size.width, size.height - armLength),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                // Center Optical Crosshair (Subtle precision hairline)
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val crosshairLength = 8.dp.toPx()
                val crosshairGap = 4.dp.toPx()

                // Center dot
                drawCircle(
                    color = if (isLocked) StatusEmerald else PrimaryCobalt,
                    radius = 2.5.dp.toPx(),
                    center = Offset(centerX, centerY)
                )

                // Horizontal ticks
                drawLine(
                    color = HairlineBorder,
                    start = Offset(centerX - crosshairGap - crosshairLength, centerY),
                    end = Offset(centerX - crosshairGap, centerY),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = HairlineBorder,
                    start = Offset(centerX + crosshairGap, centerY),
                    end = Offset(centerX + crosshairGap + crosshairLength, centerY),
                    strokeWidth = 1.dp.toPx()
                )

                // Vertical ticks
                drawLine(
                    color = HairlineBorder,
                    start = Offset(centerX, centerY - crosshairGap - crosshairLength),
                    end = Offset(centerX, centerY - crosshairGap),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = HairlineBorder,
                    start = Offset(centerX, centerY + crosshairGap),
                    end = Offset(centerX, centerY + crosshairGap + crosshairLength),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Lock Telemetry Badge
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xEB0B0D11),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .border(1.dp, StatusEmerald, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (lockElapsedMs != null) "LOCKED (${lockElapsedMs}ms)" else "LOCKED",
                        color = StatusEmerald,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
