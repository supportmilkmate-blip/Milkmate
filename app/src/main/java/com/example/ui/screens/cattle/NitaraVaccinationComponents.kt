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
import com.example.data.local.entity.VaccinationRecordEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Vaccination Coverage Tracker (4 Key Disease Nodes: FMD, HS, BQ, Brucellosis/LSD)
 */
@Composable
fun VaccinationCoverageTracker(
    vaccineRecords: List<VaccinationRecordEntity>,
    modifier: Modifier = Modifier
) {
    val fmdDone = vaccineRecords.any { it.vaccineName.contains("FMD", ignoreCase = true) || it.vaccineName.contains("Foot", ignoreCase = true) }
    val hsDone = vaccineRecords.any { it.vaccineName.contains("HS", ignoreCase = true) || it.vaccineName.contains("Haemorrhagic", ignoreCase = true) }
    val bqDone = vaccineRecords.any { it.vaccineName.contains("BQ", ignoreCase = true) || it.vaccineName.contains("Black", ignoreCase = true) }
    val lsdDone = vaccineRecords.any { it.vaccineName.contains("LSD", ignoreCase = true) || it.vaccineName.contains("Lumpy", ignoreCase = true) || it.vaccineName.contains("Brucella", ignoreCase = true) }

    val diseases = listOf(
        Triple("FMD", "🦶", if (fmdDone) 2 else 1),
        Triple("HS", "💉", if (hsDone) 2 else 0),
        Triple("BQ", "🧬", if (bqDone) 2 else 0),
        Triple("LSD / Bruc", "🛡️", if (lsdDone) 2 else 0)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NADCP Vaccine Protection Tracker",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Text(
                    text = "National Disease Program",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBluePrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                diseases.forEachIndexed { idx, (label, icon, state) ->
                    val circleColor = when (state) {
                        2 -> GrassGreen
                        1 -> RoyalBluePrimary
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

                    if (idx < diseases.size - 1) {
                        val nextState = diseases[idx + 1].third
                        val lineColor = when {
                            state == 2 && nextState > 0 -> GrassGreen
                            state > 0 -> RoyalBluePrimary
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
 * Nitara-style Vaccination Cattle Card
 */
@Composable
fun NitaraVaccinationCattleCard(
    cattle: CattleEntity,
    vaccineRecords: List<VaccinationRecordEntity>,
    onViewHistory: () -> Unit,
    onLogVaccine: () -> Unit
) {
    val lastRecord = vaccineRecords.maxByOrNull { it.date }
    val now = System.currentTimeMillis()
    val dayMillis = 86400000L
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val daysUntilBooster = if (lastRecord != null && lastRecord.nextDueDate > 0) {
        ((lastRecord.nextDueDate - now) / dayMillis)
    } else -1L

    val isBoosterOverdue = lastRecord != null && lastRecord.nextDueDate > 0 && daysUntilBooster < 0
    val isBoosterSoon = lastRecord != null && daysUntilBooster in 0..30

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("nitara_vaccine_card_${cattle.tagNumber}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Avatar, Tag/Name, Breed Badge, Count
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
                        color = RoyalBlueSecondary.copy(alpha = 0.15f),
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
                            text = if (lastRecord != null) "Latest: ${lastRecord.vaccineName}" else "No vaccine logged yet",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Vaccine count badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RoyalBlueLight
                ) {
                    Text(
                        text = "${vaccineRecords.size} Vaccine(s)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBluePrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Vaccination Status Banner
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    isBoosterOverdue -> DangerRed.copy(alpha = 0.12f)
                    isBoosterSoon -> WarmHoney.copy(alpha = 0.12f)
                    lastRecord != null -> GrassGreen.copy(alpha = 0.12f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when {
                        isBoosterOverdue -> DangerRed.copy(alpha = 0.3f)
                        isBoosterSoon -> WarmHoney.copy(alpha = 0.3f)
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
                        isBoosterOverdue -> "⚠️ Booster Overdue by ${Math.abs(daysUntilBooster)} day(s)! Log vaccine."
                        isBoosterSoon -> "⏰ Booster due in $daysUntilBooster day(s) (${sdf.format(Date(lastRecord!!.nextDueDate))})"
                        lastRecord != null -> "🛡️ Vaccinated on ${sdf.format(Date(lastRecord.date))} • Next booster due: ${sdf.format(Date(lastRecord.nextDueDate))}"
                        else -> "⚡ FMD / HS / BQ vaccination essential for NADCP herd protection"
                    }

                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isBoosterOverdue -> DangerRed
                            isBoosterSoon -> WarmHoney
                            lastRecord != null -> GrassGreen
                            else -> TextSecondary
                        }
                    )
                }
            }

            // 4-Disease Coverage Tracker
            VaccinationCoverageTracker(vaccineRecords = vaccineRecords)

            // Action Buttons Bar (View History vs + Log Vaccine)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onViewHistory,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_view_vaccine_${cattle.tagNumber}"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RoyalBluePrimary),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View History", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                }

                Button(
                    onClick = onLogVaccine,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_log_vaccine_${cattle.tagNumber}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueSecondary),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Vaccine", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * View Vaccination History Timeline Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewVaccinationHistorySheet(
    cattle: CattleEntity,
    vaccineRecords: List<VaccinationRecordEntity>,
    onDismiss: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val recordsSorted = remember(vaccineRecords) { vaccineRecords.sortedByDescending { it.date } }

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
                        text = "🛡️ Vaccination Records for ${cattle.name.ifBlank { "Tag #${cattle.tagNumber}" }}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Breed: ${cattle.breed} • Total Doses: ${vaccineRecords.size}",
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
                    Text("No vaccination records found for this animal.", color = TextSecondary, fontSize = 13.sp)
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
                                    Text("💉 ${record.vaccineName}", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = RoyalBluePrimary)
                                    Text(sdf.format(Date(record.date)), fontSize = 11.sp, color = TextSecondary)
                                }

                                Text("Manufacturer: ${record.manufacturer} • Batch #${record.batchNo.ifBlank { "N/A" }}", fontSize = 11.sp, color = TextSecondary)
                                Text("Vaccinated By: ${record.vaccinatedBy} • Cost: ₹${record.cost.toInt()}", fontSize = 11.sp, color = TextSecondary)

                                if (record.nextDueDate > 0) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = GrassGreen.copy(alpha = 0.15f)) {
                                        Text(
                                            text = "🛡️ Next Booster Due: ${sdf.format(Date(record.nextDueDate))}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GrassGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
