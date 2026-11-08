package com.example.ui.screens.cattle

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CattleEntity
import com.example.data.local.entity.DewormingRecordEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Deworming Progress Lifecycle Tracker (4 Nodes)
 * Nodes: Last Dose -> Active Coverage -> Next Due -> Annual Repeat
 */
@Composable
fun DewormingProgressTracker(
    lastRecord: DewormingRecordEntity?,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val dayMillis = 86400000L

    val daysSinceLast = if (lastRecord != null) ((now - lastRecord.date) / dayMillis).coerceAtLeast(0) else -1
    val daysUntilNext = if (lastRecord != null && lastRecord.nextDueDate > 0) ((lastRecord.nextDueDate - now) / dayMillis) else -1

    val isOverdue = daysUntilNext < 0 && lastRecord != null && lastRecord.nextDueDate > 0
    val isDueSoon = daysUntilNext in 0..15

    // State of nodes: 0 = Pending, 1 = Active/Current, 2 = Completed
    val node1State = if (lastRecord != null) 2 else 1
    val node2State = if (lastRecord != null && !isOverdue) 2 else if (lastRecord != null) 1 else 0
    val node3State = if (isOverdue) 1 else if (isDueSoon) 1 else if (lastRecord != null) 2 else 0
    val node4State = if (daysSinceLast > 180) 1 else 0

    val steps = listOf(
        Triple("Last Dose", "💊", node1State),
        Triple("Immunity", "🛡️", node2State),
        Triple("Next Due", "⏰", node3State),
        Triple("Cycle", "🔄", node4State)
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Deworming Schedule Tracker",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                steps.forEachIndexed { idx, (label, icon, state) ->
                    val circleColor = when {
                        state == 2 -> FreshGold
                        state == 1 && isOverdue -> DangerRed
                        state == 1 -> WarmHoney
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(circleColor)
                        ) {
                            if (state == 2) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            } else {
                                Text(icon, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = label,
                            fontSize = 9.sp,
                            fontWeight = if (state > 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (state > 0) TextPrimary else TextSecondary
                        )
                    }

                    if (idx < steps.size - 1) {
                        val nextState = steps[idx + 1].third
                        val lineColor = when {
                            state == 2 && nextState > 0 -> FreshGold
                            state > 0 -> WarmHoney
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        }

                        Box(
                            modifier = Modifier
                                .weight(0.5f)
                                .height(2.dp)
                                .background(lineColor)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Nitara-style Deworming Cattle Card
 */
@Composable
fun NitaraDewormingCattleCard(
    cattle: CattleEntity,
    dewormingRecords: List<DewormingRecordEntity>,
    onViewHistory: () -> Unit,
    onLogDose: () -> Unit
) {
    val lastRecord = dewormingRecords.maxByOrNull { it.date }
    val now = System.currentTimeMillis()
    val dayMillis = 86400000L
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val daysUntilNext = if (lastRecord != null && lastRecord.nextDueDate > 0) {
        ((lastRecord.nextDueDate - now) / dayMillis)
    } else -1L

    val isOverdue = lastRecord != null && lastRecord.nextDueDate > 0 && daysUntilNext < 0
    val isDueSoon = lastRecord != null && daysUntilNext in 0..15

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("nitara_deworming_card_${cattle.tagNumber}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Avatar, Tag/Name, Breed Badge, Stage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = FreshGold.copy(alpha = 0.15f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(if (cattle.type == "BUFFALO") "🐃" else "🐄", fontSize = 24.sp)
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (cattle.name.isNotBlank()) cattle.name else "Tag #${cattle.tagNumber}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = cattle.breed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = if (lastRecord != null) "Last Salt: ${lastRecord.dewormerSalt.take(22)}" else "No deworming recorded yet",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Deworming count badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = FreshGoldLight
                ) {
                    Text(
                        text = "${dewormingRecords.size} Dose(s)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarmHoney,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Deworming Status Banner
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    isOverdue -> DangerRed.copy(alpha = 0.12f)
                    isDueSoon -> WarmHoney.copy(alpha = 0.12f)
                    lastRecord != null -> GrassGreen.copy(alpha = 0.12f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when {
                        isOverdue -> DangerRed.copy(alpha = 0.3f)
                        isDueSoon -> WarmHoney.copy(alpha = 0.3f)
                        lastRecord != null -> GrassGreen.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val statusText = when {
                        isOverdue -> "⚠️ Overdue by ${Math.abs(daysUntilNext)} day(s)! Repeat dose now."
                        isDueSoon -> "⏰ Next dose due in $daysUntilNext day(s) (${sdf.format(Date(lastRecord!!.nextDueDate))})"
                        lastRecord != null -> "✅ Dewormed on ${sdf.format(Date(lastRecord.date))} • Next due: ${sdf.format(Date(lastRecord.nextDueDate))}"
                        else -> "⚡ Deworming recommended every 90 days for parasite protection"
                    }

                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isOverdue -> DangerRed
                            isDueSoon -> WarmHoney
                            lastRecord != null -> GrassGreen
                            else -> TextSecondary
                        }
                    )
                }
            }

            // 4-Node Deworming Tracker
            DewormingProgressTracker(lastRecord = lastRecord)

            // Action Buttons Bar (View History vs + Log Dose)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onViewHistory,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_view_deworming_${cattle.tagNumber}"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarmHoney),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = WarmHoney, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View History", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarmHoney)
                }

                Button(
                    onClick = onLogDose,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_log_deworming_${cattle.tagNumber}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FreshGold),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Dose", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * View Deworming History Timeline Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewDewormingHistorySheet(
    cattle: CattleEntity,
    dewormingRecords: List<DewormingRecordEntity>,
    onDismiss: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val recordsSorted = remember(dewormingRecords) { dewormingRecords.sortedByDescending { it.date } }

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
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "💊 Deworming History for ${cattle.name.ifBlank { "Tag #${cattle.tagNumber}" }}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Breed: ${cattle.breed} • Total Doses: ${dewormingRecords.size}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            if (recordsSorted.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No deworming records found for this animal.", color = TextSecondary, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recordsSorted) { record ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(record.dewormerSalt, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    Text(sdf.format(Date(record.date)), fontSize = 11.sp, color = TextSecondary)
                                }

                                Text("Dose: ${record.dose} • By: ${record.administeredBy} • Cost: ₹${record.cost.toInt()}", fontSize = 11.sp, color = TextSecondary)

                                if (record.nextDueDate > 0) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = FreshGoldLight) {
                                        Text(
                                            text = "⏰ Next Due: ${sdf.format(Date(record.nextDueDate))} (Repeat every ${record.repeatAfterDays} days)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WarmHoney,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (record.remarks.isNotBlank()) {
                                    Text("Remarks: ${record.remarks}", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
