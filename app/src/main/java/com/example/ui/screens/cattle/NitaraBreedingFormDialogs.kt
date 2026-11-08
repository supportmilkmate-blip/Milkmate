package com.example.ui.screens.cattle

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * 1. Add Heat Dialog Form
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHeatDialog(
    cattle: CattleEntity,
    onDismiss: () -> Unit,
    onSave: (BreedingRecordEntity) -> Unit
) {
    var heatType by remember { mutableStateOf("NATURAL") } // NATURAL, ARTIFICIAL
    var notes by remember { mutableStateOf("") }
    val now = System.currentTimeMillis()
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "📅 Record Heat Observation",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Animal: Tag #${cattle.tagNumber} (${cattle.name.ifBlank { cattle.breed }})",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Text("Heat Sign Type", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = heatType == "NATURAL",
                        onClick = { heatType = "NATURAL" },
                        label = { Text("Natural Signs") }
                    )
                    FilterChip(
                        selected = heatType == "HORMONE",
                        onClick = { heatType = "HORMONE" },
                        label = { Text("Hormone Induced") }
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observation Notes (e.g., Bellowing, Mucus Discharge)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Text(
                    text = "Heat Date: ${sdf.format(Date(now))}",
                    fontSize = 12.sp,
                    color = WarmHoney,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val record = BreedingRecordEntity(
                        id = "BREED-${UUID.randomUUID().toString().take(8).uppercase()}",
                        businessId = cattle.businessId,
                        cattleId = cattle.id,
                        cattleTag = cattle.tagNumber,
                        eventType = "HEAT",
                        date = now,
                        notes = "Heat Type: $heatType. $notes".trim()
                    )
                    onSave(record)
                },
                colors = ButtonDefaults.buttonColors(containerColor = WarmHoney)
            ) {
                Text("Save Heat Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * 2. Insemination Choice Modal (Natural vs Artificial)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInseminationChoiceModal(
    cattle: CattleEntity,
    onDismiss: () -> Unit,
    onNaturalSelect: () -> Unit,
    onArtificialSelect: () -> Unit
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
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "💉 Select Insemination Type",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = RoyalBlueLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onArtificialSelect() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🧪", fontSize = 28.sp)
                    Column {
                        Text("Artificial Insemination (A.I.)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RoyalBluePrimary)
                        Text("Upload semen straw photo, brand, bull ID, and straw count", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GrassGreen.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNaturalSelect() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🐂", fontSize = 28.sp)
                    Column {
                        Text("Natural Mating", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GrassGreen)
                        Text("Record Bull ID / Name and mating remarks", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

/**
 * Artificial Insemination Form (Nitara app style)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddArtificialInseminationDialog(
    cattle: CattleEntity,
    onDismiss: () -> Unit,
    onSave: (BreedingRecordEntity) -> Unit
) {
    var semenBrand by remember { mutableStateOf("ABS India Prime") }
    var bullId by remember { mutableStateOf("ABS Bull #902") }
    var strawsUsed by remember { mutableStateOf("1") }
    var doneBy by remember { mutableStateOf("Myself") } // Myself / Veterinary Doctor
    var location by remember { mutableStateOf("At Farm") }
    var notes by remember { mutableStateOf("") }

    val now = System.currentTimeMillis()
    val dayMillis = 86400000L
    val isBuffalo = cattle.type == "BUFFALO"
    val gestationDays = if (isBuffalo) 310L else 283L
    val expectedCalving = now + (gestationDays * dayMillis)

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
                        text = "💉 Artificial Insemination (A.I.)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RoyalBluePrimary
                    )
                    Text(
                        text = "Tag #${cattle.tagNumber} (${cattle.name.ifBlank { cattle.breed }})",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Photo Upload Box Placeholder matching video
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(28.dp))
                            Column {
                                Text("Upload Semen Straw Image", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Take Picture / Upload from gallery (Optional)", fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = semenBrand,
                        onValueChange = { semenBrand = it },
                        label = { Text("Semen Brand / Source") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = bullId,
                        onValueChange = { bullId = it },
                        label = { Text("Bull ID / Code") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    Text("No. of Semen Straws Used", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        listOf("1", "2", "3").forEach { num ->
                            FilterChip(
                                selected = strawsUsed == num,
                                onClick = { strawsUsed = num },
                                label = { Text("$num Straw(s)") }
                            )
                        }
                    }
                }

                item {
                    Text("Insemination Done By", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        listOf("Myself", "Veterinary Doctor", "A.I. Technician").forEach { opt ->
                            FilterChip(
                                selected = doneBy == opt,
                                onClick = { doneBy = opt },
                                label = { Text(opt) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Insemination Location") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    Button(
                        onClick = {
                            val record = BreedingRecordEntity(
                                id = "BREED-${UUID.randomUUID().toString().take(8).uppercase()}",
                                businessId = cattle.businessId,
                                cattleId = cattle.id,
                                cattleTag = cattle.tagNumber,
                                eventType = "INSEMINATION",
                                date = now,
                                inseminationType = "ARTIFICIAL",
                                bullIdOrName = "$semenBrand - $bullId",
                                semenStrawsUsed = strawsUsed.toIntOrNull() ?: 1,
                                doneBy = doneBy,
                                pdStatus = "PENDING",
                                expectedCalvingDate = expectedCalving,
                                notes = "Location: $location. $notes".trim()
                            )
                            onSave(record)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Artificial Insemination", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Natural Insemination Form
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNaturalInseminationDialog(
    cattle: CattleEntity,
    onDismiss: () -> Unit,
    onSave: (BreedingRecordEntity) -> Unit
) {
    var bullName by remember { mutableStateOf("Farm Sire Bull") }
    var remarks by remember { mutableStateOf("") }
    val now = System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🐂 Natural Mating", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = bullName,
                    onValueChange = { bullName = it },
                    label = { Text("Bull ID / Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { if (it.length <= 400) remarks = it },
                    label = { Text("Remarks (max 400 chars)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val record = BreedingRecordEntity(
                        id = "BREED-${UUID.randomUUID().toString().take(8).uppercase()}",
                        businessId = cattle.businessId,
                        cattleId = cattle.id,
                        cattleTag = cattle.tagNumber,
                        eventType = "INSEMINATION",
                        date = now,
                        inseminationType = "NATURAL",
                        bullIdOrName = bullName,
                        notes = remarks
                    )
                    onSave(record)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GrassGreen)
            ) {
                Text("Save Natural Mating")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * 3. Pregnancy Detection (PD) Form
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPdDialog(
    cattle: CattleEntity,
    onDismiss: () -> Unit,
    onSave: (BreedingRecordEntity) -> Unit
) {
    var isPregnant by remember { mutableStateOf(true) }
    var pdMethod by remember { mutableStateOf("Physical Examination") }
    var doneBy by remember { mutableStateOf("Myself") }
    val now = System.currentTimeMillis()

    val methods = listOf("Physical Examination", "Milk Test", "Blood Based Test", "Ultrasound", "Urine Test")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🩺 Pregnancy Diagnosis (PD)", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Is cattle pregnant?", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { isPregnant = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPregnant) GrassGreen else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isPregnant) Color.White else TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Yes (Pregnant)")
                    }

                    Button(
                        onClick = { isPregnant = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isPregnant) DangerRed else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!isPregnant) Color.White else TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("No (Open)")
                    }
                }

                Text("PD Method Used", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    methods.forEach { m ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pdMethod = m }
                        ) {
                            RadioButton(selected = pdMethod == m, onClick = { pdMethod = m })
                            Text(m, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val status = if (isPregnant) "POSITIVE_PREGNANT" else "NEGATIVE"
                    val record = BreedingRecordEntity(
                        id = "BREED-${UUID.randomUUID().toString().take(8).uppercase()}",
                        businessId = cattle.businessId,
                        cattleId = cattle.id,
                        cattleTag = cattle.tagNumber,
                        eventType = "PREGNANCY_DIAGNOSIS",
                        date = now,
                        pdStatus = status,
                        doneBy = doneBy,
                        notes = "Method: $pdMethod"
                    )
                    onSave(record)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GrassGreen)
            ) {
                Text("Save PD Result")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * 4. Abortion Form
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAbortionDialog(
    cattle: CattleEntity,
    onDismiss: () -> Unit,
    onSave: (BreedingRecordEntity) -> Unit
) {
    var reason by remember { mutableStateOf("Brucellosis") }
    val reasons = listOf("Brucellosis", "Albendazole / Dewormers", "Antibiotics / Hormones", "Fight amongst cattle", "Bull mounting", "Infected Uterus", "Other")
    val now = System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚠️ Record Abortion", fontWeight = FontWeight.Bold, color = DangerRed) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Abortion Reason", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                reasons.forEach { r ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { reason = r }
                    ) {
                        RadioButton(selected = reason == r, onClick = { reason = r })
                        Text(r, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val record = BreedingRecordEntity(
                        id = "BREED-${UUID.randomUUID().toString().take(8).uppercase()}",
                        businessId = cattle.businessId,
                        cattleId = cattle.id,
                        cattleTag = cattle.tagNumber,
                        eventType = "ABORTION",
                        date = now,
                        notes = "Abortion Reason: $reason"
                    )
                    onSave(record)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
            ) {
                Text("Save Abortion Record")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * 5. Calving Form
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCalvingDialog(
    cattle: CattleEntity,
    onDismiss: () -> Unit,
    onSave: (BreedingRecordEntity) -> Unit
) {
    var calfType by remember { mutableStateOf("Single") } // Single, Twins
    var calfGender by remember { mutableStateOf("FEMALE") } // MALE, FEMALE, STILLBORN
    var calfTag by remember { mutableStateOf("C-${(100..999).random()}") }
    var calfWeight by remember { mutableStateOf("28.5") }
    var dystocia by remember { mutableStateOf("No") } // Yes, No, Don't know
    val now = System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🐮 Record Calving Event", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Calf Result", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = calfType == "Single", onClick = { calfType = "Single" }, label = { Text("Single") })
                    FilterChip(selected = calfType == "Twins", onClick = { calfType = "Twins" }, label = { Text("Twins") })
                }

                Text("Calf Gender / Result", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = calfGender == "FEMALE", onClick = { calfGender = "FEMALE" }, label = { Text("Female ♀") })
                    FilterChip(selected = calfGender == "MALE", onClick = { calfGender = "MALE" }, label = { Text("Male ♂") })
                    FilterChip(selected = calfGender == "STILLBORN", onClick = { calfGender = "STILLBORN" }, label = { Text("Stillborn") })
                }

                OutlinedTextField(
                    value = calfTag,
                    onValueChange = { calfTag = it },
                    label = { Text("Calf Tag / Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = calfWeight,
                    onValueChange = { calfWeight = it },
                    label = { Text("Calf Birth Weight (KG)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Text("Dystocia (Calving Difficulty)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("No", "Yes", "Don't know").forEach { d ->
                        FilterChip(selected = dystocia == d, onClick = { dystocia = d }, label = { Text(d) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val record = BreedingRecordEntity(
                        id = "BREED-${UUID.randomUUID().toString().take(8).uppercase()}",
                        businessId = cattle.businessId,
                        cattleId = cattle.id,
                        cattleTag = cattle.tagNumber,
                        eventType = "CALVING",
                        date = now,
                        calfGender = calfGender,
                        calfTag = calfTag,
                        notes = "Result: $calfType, Weight: $calfWeight kg, Dystocia: $dystocia"
                    )
                    onSave(record)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GrassGreen)
            ) {
                Text("Save Calving Event")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * 6. Dry Period Form
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDryPeriodDialog(
    cattle: CattleEntity,
    onDismiss: () -> Unit,
    onSave: (BreedingRecordEntity) -> Unit
) {
    val now = System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🥛 Start Dry Period", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Confirm starting dry period for Tag #${cattle.tagNumber} (${cattle.name.ifBlank { cattle.breed }}). This stops daily milking tracking and prepares cow for upcoming calving.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val record = BreedingRecordEntity(
                        id = "BREED-${UUID.randomUUID().toString().take(8).uppercase()}",
                        businessId = cattle.businessId,
                        cattleId = cattle.id,
                        cattleTag = cattle.tagNumber,
                        eventType = "DRY_OFF",
                        date = now,
                        notes = "Cow dried off for gestation rest."
                    )
                    onSave(record)
                },
                colors = ButtonDefaults.buttonColors(containerColor = OceanMidnight)
            ) {
                Text("Confirm Dry Off")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
