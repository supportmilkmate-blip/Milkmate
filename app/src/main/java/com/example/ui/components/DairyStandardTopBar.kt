package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures

/**
 * Standardized Top Bar used across all MilkMate screens for consistent visual rhythm,
 * breadcrumbs, shift indicators, business mode badges, and universal quick actions.
 * Strictly bounded in height to maintain compact, edge-to-edge Material 3 layout.
 */
@Composable
fun DairyStandardTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    businessMode: String? = null,
    onModeClick: (() -> Unit)? = null,
    selectedShift: String? = null,
    onShiftToggle: (() -> Unit)? = null,
    onQuickActionClick: (() -> Unit)? = null,
    onAIClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            // Main App Header Row (strictly bounded compact height)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp, max = 56.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Navigation Action (Back or Menu)
                if (onBackClick != null) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("standard_topbar_back_btn")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Navigate Back",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else if (onMenuClick != null) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("standard_topbar_menu_btn")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RoyalBlueLight,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Drawer Menu",
                                    tint = RoyalBluePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Title and Subtitle Block (never wraps vertically)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Business Mode Pill (compact, single-line)
                        if (businessMode != null && onModeClick != null) {
                            val modeConfig = BusinessModeFeatures.getByType(businessMode)
                            val shortBadge = when (businessMode) {
                                "FARMER_WHOLESALE" -> "Farm"
                                "FARMER" -> "Farm"
                                "INTEGRATED" -> "Integrated"
                                "TRADER" -> "Trader"
                                "COLLECTION_CENTER" -> "BMC"
                                else -> "Custom"
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = modeConfig.themeColor.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .clickable { onModeClick() }
                                    .testTag("standard_topbar_mode_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = modeConfig.icon,
                                        contentDescription = null,
                                        tint = modeConfig.themeColor,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = shortBadge,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = modeConfig.themeColor,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Shift Pill Switcher (if provided)
                if (selectedShift != null && onShiftToggle != null) {
                    val isMorning = selectedShift.equals("MORNING", ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isMorning) FreshGoldLight else RoyalBlueLight,
                        modifier = Modifier
                            .clickable { onShiftToggle() }
                            .padding(end = 2.dp)
                            .testTag("standard_topbar_shift_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (isMorning) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                contentDescription = null,
                                tint = if (isMorning) WarmHoney else RoyalBluePrimary,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = if (isMorning) "AM" else "PM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isMorning) WarmHoney else RoyalBluePrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Universal Quick Action Trigger
                if (onQuickActionClick != null) {
                    IconButton(
                        onClick = onQuickActionClick,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("standard_topbar_quick_action_btn")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = FreshGoldLight,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = "Universal Quick Actions",
                                    tint = WarmHoney,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // AI Advisor Trigger
                if (onAIClick != null) {
                    IconButton(
                        onClick = onAIClick,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("standard_topbar_ai_btn")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RoyalBlueLight,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Gemini AI Dairy Advisor",
                                    tint = RoyalBluePrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Custom trailing actions
                actions()
            }
            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f), thickness = 0.8.dp)
        }
    }
}
