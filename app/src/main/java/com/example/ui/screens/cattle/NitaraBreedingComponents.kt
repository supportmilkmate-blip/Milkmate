package com.example.ui.screens.cattle

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.BreedingRecordEntity
import com.example.data.local.entity.CattleEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 5-Node Breeding Lifecycle Progress Tracker matching Nitara app style.
 * Nodes: Heat -> Insemination -> PD -> Dry -> Calving
 */
@Composable
fun BreedingLifecycleProgressTracker(
    cattle: CattleEntity,
    breedingRecords: List<BreedingRecordEntity>,
    modifier: Modifier = Modifier
) {
    val latestRecord = breedingRecords.maxByOrNull { it.date }
    val status = cattle.breedingStatus

    // Determine state for each of the 5 nodes:
    // 0 = Pending, 1 = Current/Active, 2 = Completed
    val heatState = when {
        status == "INSEMINATED" || status == "CONFIRMED_PREGNANT" || latestRecord?.eventType in listOf("HEAT", "INSEMINATION", "PREGNANCY_DIAGNOSIS", "CALVING") -> 2
        else -> 1
    }

    val inseminationState = when {
        status == "CONFIRMED_PREGNANT" || latestRecord?.eventType in listOf("PREGNANCY_DIAGNOSIS", "CALVING") -> 2
        status == "INSEMINATED" || latestRecord?.eventType == "INSEMINATION" -> 1
        else -> 0
    }

    val pdState = when {
        status == "CONFIRMED_PREGNANT" || (latestRecord?.eventType == "PREGNANCY_DIAGNOSIS" && latestRecord.pdStatus.contains("POSITIVE")) -> 2
        status == "INSEMINATED" -> 1
        else -> 0
    }

    val dryState = when {
        cattle.lactationStage == "DRY" || cattle.lactationStage == "PREGNANT_DRY" || latestRecord?.eventType == "DRY_OFF" -> 1
        status == "CONFIRMED_PREGNANT" -> 1
        else -> 0
    }

    val calvingState = when {
        status == "CALVED" || latestRecord?.eventType == "CALVING" -> 2
        status == "CONFIRMED_PREGNANT" -> 1
        else -> 0
    }

    val steps = listOf(
        Triple("Heat", "📅", heatState),
        Triple("Insemination", "💉", inseminationState),
        Triple("PD", "🩺", pdState),
        Triple("Dry", "🥛", dryState),
        Triple("Calving", "🐮", calvingState)
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
                text = "Breeding Lifecycle Tracker",
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
                    val circleColor = when (state) {
                        2 -> GrassGreen
                        1 -> RoyalBluePrimary
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    }

                    val contentColor = when (state) {
                        2, 1 -> Color.White
                        else -> TextSecondary
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
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = icon,
                                    fontSize = 14.sp
                                )
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
 * Cattle Card for Nitara Breeding List View
 */
@Composable
fun NitaraBreedingCattleCard(
    cattle: CattleEntity,
    breedingRecords: List<BreedingRecordEntity>,
    onViewBreeding: () -> Unit,
    onAddBreeding: () -> Unit,
    onInbreedingClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("nitara_breeding_card_${cattle.tagNumber}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Avatar, Tag/Name, Breed Badge, Lactation Info
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
                        color = if (cattle.type == "BUFFALO") DeepOceanNavy else GrassGreen.copy(alpha = 0.15f),
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

                        val lactationText = when (cattle.lactationStage) {
                            "HEIFER" -> "Heifer Cow Initial Breeding"
                            "DRY" -> "Dry Cow Lactation ${cattle.lactationCount}"
                            "CALF" -> "Young Calf"
                            else -> "Milking Cow Lactation ${cattle.lactationCount}"
                        }

                        Text(
                            text = lactationText,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Breeding status pill badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (cattle.breedingStatus) {
                        "CONFIRMED_PREGNANT" -> GrassGreen.copy(alpha = 0.15f)
                        "INSEMINATED" -> RoyalBlueLight
                        else -> FreshGoldLight
                    }
                ) {
                    Text(
                        text = cattle.breedingStatus.replace("_", " "),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (cattle.breedingStatus) {
                            "CONFIRMED_PREGNANT" -> GrassGreen
                            "INSEMINATED" -> RoyalBluePrimary
                            else -> WarmHoney
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Inbreeding Alert Pill (Nitara video feature)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = WarmHoney.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarmHoney.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onInbreedingClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("⚠️", fontSize = 12.sp)
                        Text(
                            text = "No of potential inbreeding matches >",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = WarmHoney
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = WarmHoney,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 5-Node Progress Tracker
            BreedingLifecycleProgressTracker(
                cattle = cattle,
                breedingRecords = breedingRecords
            )

            // Action Buttons Bar (View Breeding vs + Add)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onViewBreeding,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_view_breeding_${cattle.tagNumber}"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RoyalBluePrimary),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        tint = RoyalBluePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View Breeding",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBluePrimary
                    )
                }

                Button(
                    onClick = onAddBreeding,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_add_breeding_${cattle.tagNumber}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GrassGreen),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * View Breeding Records Timeline Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewBreedingRecordsSheet(
    cattle: CattleEntity,
    breedingRecords: List<BreedingRecordEntity>,
    onDismiss: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val recordsSorted = remember(breedingRecords) {
        breedingRecords.sortedByDescending { it.date }
    }

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
                        text = "👁️ Breeding History for ${cattle.name.ifBlank { "Tag #${cattle.tagNumber}" }}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Breed: ${cattle.breed} • Status: ${cattle.breedingStatus}",
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No breeding records found for this animal.", color = TextSecondary, fontSize = 13.sp)
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
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when (record.eventType) {
                                            "INSEMINATION" -> RoyalBlueLight
                                            "PREGNANCY_DIAGNOSIS" -> GrassGreen.copy(alpha = 0.15f)
                                            "CALVING" -> FreshGoldLight
                                            "ABORTION" -> DangerRed.copy(alpha = 0.15f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Text(
                                            text = record.eventType.replace("_", " "),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (record.eventType) {
                                                "INSEMINATION" -> RoyalBluePrimary
                                                "PREGNANCY_DIAGNOSIS" -> GrassGreen
                                                "CALVING" -> WarmHoney
                                                "ABORTION" -> DangerRed
                                                else -> TextPrimary
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Text(
                                        text = sdf.format(Date(record.date)),
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }

                                if (record.bullIdOrName.isNotBlank()) {
                                    Text(
                                        text = "Bull / Semen: ${record.bullIdOrName} (${record.inseminationType})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }

                                if (record.semenStrawsUsed > 0) {
                                    Text(
                                        text = "Semen Straws Used: ${record.semenStrawsUsed} • Done by: ${record.doneBy}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }

                                if (record.pdStatus != "PENDING") {
                                    Text(
                                        text = "PD Result: ${record.pdStatus}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (record.pdStatus.contains("POSITIVE")) GrassGreen else DangerRed
                                    )
                                }

                                if (record.expectedCalvingDate > 0) {
                                    val calSdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                    Text(
                                        text = "Expected Calving: ${calSdf.format(Date(record.expectedCalvingDate))}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WarmHoney
                                    )
                                }

                                if (record.calfTag.isNotBlank()) {
                                    Text(
                                        text = "Calf ID: ${record.calfTag} (${record.calfGender})",
                                        fontSize = 11.sp,
                                        color = TextPrimary
                                    )
                                }

                                if (record.notes.isNotBlank()) {
                                    Text(
                                        text = "Notes: ${record.notes}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
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

/**
 * Add Breeding Modal Bottom Sheet (6 Tile Options Grid)
 */
data class BreedingActionOption(
    val title: String,
    val icon: String,
    val color: Color,
    val typeCode: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBreedingOptionsSheet(
    cattle: CattleEntity,
    onDismiss: () -> Unit,
    onOptionSelected: (String) -> Unit
) {
    val options = listOf(
        BreedingActionOption("Heat", "📅", Color(0xFFFF9800), "HEAT"),
        BreedingActionOption("Insemination", "💉", Color(0xFF2196F3), "INSEMINATION"),
        BreedingActionOption("PD", "🩺", Color(0xFF4CAF50), "PD"),
        BreedingActionOption("Abortion", "⚠️", Color(0xFFF44336), "ABORTION"),
        BreedingActionOption("Calving", "🐮", Color(0xFF9C27B0), "CALVING"),
        BreedingActionOption("Dry period", "🥛", Color(0xFF607D8B), "DRY")
    )

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add breeding",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Select action for Tag #${cattle.tagNumber} (${cattle.name.ifBlank { cattle.breed }})",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(options) { opt ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = opt.color.copy(alpha = 0.08f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, opt.color.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(opt.typeCode) }
                            .testTag("breeding_opt_${opt.typeCode}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = opt.color.copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(opt.icon, fontSize = 20.sp)
                                }
                            }

                            Text(
                                text = opt.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
