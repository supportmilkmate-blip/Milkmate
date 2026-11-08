package com.example.ui.screens.cattle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Bottom-sheet dialog shown when user taps "Herd Milking"
 * allows choosing between:
 * 1. All Cattle Table Entry (Cow Name | Morning | Evening | Total)
 * 2. Single Animal Milking Log (FAT, SNF & Yield)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilkingChoiceDialog(
    totalCattleCount: Int,
    lactatingCount: Int,
    onDismiss: () -> Unit,
    onSelectTableEntry: () -> Unit,
    onSelectSingleEntry: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🐄 Herd Milking Entry",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "$totalCattleCount Total Herd • $lactatingCount In-Milk (Lactating)",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Text(
                text = "Choose how you want to record milk yield today:",
                fontSize = 13.sp,
                color = TextSecondary
            )

            // Option 1: All Cattle Table Entry (Primary)
            MilkingOptionCard(
                title = "Table Entry (All Cattle at Once)",
                subtitle = "Column A: Cow Name • Column B: Morning • Column C: Evening • Column D: Auto Total",
                badge = "RECOMMENDED",
                badgeColor = RoyalBluePrimary,
                icon = Icons.Default.TableChart,
                iconTint = RoyalBluePrimary,
                iconBg = RoyalBlueLight,
                testTag = "choice_table_entry",
                onClick = {
                    onDismiss()
                    onSelectTableEntry()
                }
            )

            // Option 2: Single Animal Entry
            MilkingOptionCard(
                title = "Single Animal Milking Log",
                subtitle = "Log individual cow yield with specific FAT %, SNF % and milker notes",
                badge = "DETAILED",
                badgeColor = GrassGreen,
                icon = Icons.Default.WaterDrop,
                iconTint = GrassGreen,
                iconBg = DairyGreenLight,
                testTag = "choice_single_entry",
                onClick = {
                    onDismiss()
                    onSelectSingleEntry()
                }
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun MilkingOptionCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, badgeColor.copy(alpha = 0.35f)),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconBg,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor.copy(alpha = 0.12f),
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
