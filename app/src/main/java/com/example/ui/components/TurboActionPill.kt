package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.ParsedPayload
import com.example.util.PayloadCategory

/**
 * Concentric, non-blocking floating action pill for SwiftScan.
 * Outer container 20dp radius, inner buttons 12dp radius, 8dp padding.
 * 48dp minimum touch bounds for accessibility.
 * Anti-slop: solid dark elevated surface without gratuitous blur or neon glows.
 */
@Composable
fun TurboActionPill(
    payload: ParsedPayload?,
    visible: Boolean,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    onOpenDetails: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && payload != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
        ),
        modifier = modifier
    ) {
        if (payload == null) return@AnimatedVisibility

        val icon: ImageVector = when (payload.category) {
            PayloadCategory.URL -> Icons.Default.Language
            PayloadCategory.WIFI -> Icons.Default.Wifi
            PayloadCategory.PRODUCT -> Icons.Default.QrCode
            PayloadCategory.CONTACT -> Icons.Default.Person
            PayloadCategory.TEXT -> Icons.Default.TextFields
        }

        val badgeBg = when (payload.category) {
            PayloadCategory.URL -> Color(0x1F10B981)
            PayloadCategory.WIFI -> Color(0x263B82F6)
            PayloadCategory.PRODUCT -> Color(0x1FF59E0B)
            PayloadCategory.CONTACT -> Color(0x26A855F7)
            PayloadCategory.TEXT -> Color(0x1AFFFFFF)
        }

        val badgeText = when (payload.category) {
            PayloadCategory.URL -> StatusEmerald
            PayloadCategory.WIFI -> PrimaryCobalt
            PayloadCategory.PRODUCT -> StatusAmber
            PayloadCategory.CONTACT -> Color(0xFFA855F7)
            PayloadCategory.TEXT -> TextMediumEmphasis
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(SurfaceElevated, RoundedCornerShape(20.dp))
                .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
                .clickable(onClick = onOpenDetails)
                .padding(14.dp)
                .testTag("turbo_action_pill")
        ) {
            // Drag affordance handle + dismiss button (48dp touch target)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(36.dp))

                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 3.dp)
                        .background(HairlineBorder, RoundedCornerShape(2.dp))
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClick = onDismiss)
                        .testTag("turbo_pill_dismiss"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Scan",
                        tint = TextMediumEmphasis,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Metadata Row: Icon + Title + Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = PrimaryCobalt,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = payload.title,
                            color = TextHighEmphasis,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = payload.subtitle,
                            color = TextMediumEmphasis,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Status Tag
                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(6.dp))
                        .border(1.dp, badgeText.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = payload.badge,
                        color = badgeText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contextual Actions Row (Concentric 12dp button radius, 48dp minimum touch bounds)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary Action Button (48dp height)
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("turbo_pill_primary_action"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryCobalt,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(
                        text = payload.primaryLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Secondary Action Button (48dp height)
                Button(
                    onClick = onSecondaryAction,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("turbo_pill_secondary_action"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceSlate,
                        contentColor = TextHighEmphasis
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(
                        text = payload.secondaryLabel,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
