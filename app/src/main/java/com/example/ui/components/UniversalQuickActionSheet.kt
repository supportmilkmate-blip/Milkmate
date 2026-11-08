package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures

data class QuickActionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconTint: Color,
    val bgTint: Color,
    val tag: String,
    val onClick: () -> Unit
)

/**
 * Universal Quick Action Sheet: Accessible from any screen via top-bar lightning icon or bottom FAB.
 * Provides instant 1-tap entry to all 7 primary daily workflows without tedious multi-step navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalQuickActionSheet(
    businessMode: String?,
    onDismiss: () -> Unit,
    onOpenOperationsHub: () -> Unit,
    onRecordMilking: () -> Unit,
    onFarmerCollection: () -> Unit,
    onCustomerDelivery: () -> Unit,
    onRecordPayment: () -> Unit,
    onCattleHealth: () -> Unit,
    onRecordExpense: () -> Unit,
    onStaffAttendance: () -> Unit,
    onOpenBusinessModeSelector: () -> Unit
) {
    val isFarmMode = businessMode in listOf("FARMER", "FARMER_WHOLESALE", "INTEGRATED", "CUSTOM", null)
    val isCollectionMode = businessMode in listOf("COLLECTION_CENTER", "TRADER", "INTEGRATED", "CUSTOM")

    val primaryTint = MaterialTheme.colorScheme.primary
    val primaryBg = MaterialTheme.colorScheme.primaryContainer
    val secondaryTint = MaterialTheme.colorScheme.secondary
    val secondaryBg = MaterialTheme.colorScheme.secondaryContainer
    val tertiaryTint = MaterialTheme.colorScheme.tertiary
    val tertiaryBg = MaterialTheme.colorScheme.tertiaryContainer
    val errorTint = MaterialTheme.colorScheme.error
    val errorBg = MaterialTheme.colorScheme.errorContainer

    val actions = buildList {
        // 0. Operations Control Hub (Featured Top Action)
        add(
            QuickActionItem(
                id = "operations_hub",
                title = "Operations Control Hub",
                subtitle = "Shifts, Dispatch, Rates & Catalog",
                icon = Icons.Default.Tune,
                iconTint = DairyGreen,
                bgTint = DairyGreenLight,
                tag = "Control",
                onClick = {
                    onDismiss()
                    onOpenOperationsHub()
                }
            )
        )
        // 1. Milking (Highlighted for Farm/Integrated)
        if (isFarmMode) {
            add(
                QuickActionItem(
                    id = "milking",
                    title = "Record Milking",
                    subtitle = "AM/PM Cow & Buffalo yield",
                    icon = Icons.Default.WaterDrop,
                    iconTint = primaryTint,
                    bgTint = primaryBg,
                    tag = "Farm",
                    onClick = {
                        onDismiss()
                        onRecordMilking()
                    }
                )
            )
        }

        // 2. Farmer Collection Desk (Highlighted for Center/Trader/Integrated)
        if (isCollectionMode) {
            add(
                QuickActionItem(
                    id = "collection",
                    title = "Milk Intake Desk",
                    subtitle = "Farmer FAT, SNF & Slip",
                    icon = Icons.Default.Science,
                    iconTint = tertiaryTint,
                    bgTint = tertiaryBg,
                    tag = "Intake",
                    onClick = {
                        onDismiss()
                        onFarmerCollection()
                    }
                )
            )
        }

        // 3. Customer Delivery
        add(
            QuickActionItem(
                id = "delivery",
                title = "Log Delivery",
                subtitle = "Route drop & bottle check",
                icon = Icons.Default.LocalShipping,
                iconTint = secondaryTint,
                bgTint = secondaryBg,
                tag = "Delivery",
                onClick = {
                    onDismiss()
                    onCustomerDelivery()
                }
            )
        )

        // 4. Payment / Khata
        add(
            QuickActionItem(
                id = "payment",
                title = "Record Payment",
                subtitle = "Cash, UPI & Farmer payouts",
                icon = Icons.Default.Payments,
                iconTint = tertiaryTint,
                bgTint = tertiaryBg,
                tag = "Finance",
                onClick = {
                    onDismiss()
                    onRecordPayment()
                }
            )
        )

        // 5. Cattle Health & Insemination
        if (isFarmMode) {
            add(
                QuickActionItem(
                    id = "cattle_health",
                    title = "Cattle Health & AI",
                    subtitle = "Breeding, heat & vaccine",
                    icon = Icons.Default.Healing,
                    iconTint = errorTint,
                    bgTint = errorBg,
                    tag = "Veterinary",
                    onClick = {
                        onDismiss()
                        onCattleHealth()
                    }
                )
            )
        }

        // 6. Expense Outlay
        add(
            QuickActionItem(
                id = "expense",
                title = "Log Expense",
                subtitle = "Feed purchase, fuel & repair",
                icon = Icons.Default.Receipt,
                iconTint = errorTint,
                bgTint = errorBg,
                tag = "Outlay",
                onClick = {
                    onDismiss()
                    onRecordExpense()
                }
            )
        )

        // 7. Staff & Attendance
        add(
            QuickActionItem(
                id = "staff",
                title = "Staff & Attendance",
                subtitle = "Worker roster & daily log",
                icon = Icons.Default.Badge,
                iconTint = primaryTint,
                bgTint = primaryBg,
                tag = "Team",
                onClick = {
                    onDismiss()
                    onStaffAttendance()
                }
            )
        )

        // 8. Business Mode Switcher
        add(
            QuickActionItem(
                id = "switch_mode",
                title = "Business Mode",
                subtitle = "Farm, Center, Trader setup",
                icon = Icons.Default.Tune,
                iconTint = primaryTint,
                bgTint = primaryBg,
                tag = "Setup",
                onClick = {
                    onDismiss()
                    onOpenBusinessModeSelector()
                }
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("universal_quick_action_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Universal 1-Tap Quick Actions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Record daily transactions from any screen",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Grid (2 columns)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(actions) { action ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { action.onClick() }
                            .testTag("quick_action_${action.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = action.bgTint,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = action.icon,
                                            contentDescription = null,
                                            tint = action.iconTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = action.bgTint.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text(
                                        text = action.tag,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = action.iconTint,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = action.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )

                            Text(
                                text = action.subtitle,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
