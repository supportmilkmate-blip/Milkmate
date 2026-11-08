package com.example.ui.screens.cattle

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.data.local.entity.CattleMilkingRecordEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Milking Hub Screen matching the video recording (00:08 - 00:10 & 01:27 - 01:32):
 * - Unit Switcher (Litre vs Kilograms)
 * - Date Switcher (<, >, Calendar)
 * - Circular KPI status widgets (Production, Milk sale)
 * - Production Section with "+ Add for each cattle" and "+ Add for whole farm"
 * - Sales Section with "+ Add milk sales"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilkingHubScreen(
    cattleList: List<CattleEntity>,
    allMilkingRecords: List<CattleMilkingRecordEntity>,
    onOpenAddForEachCattle: () -> Unit,
    onOpenAddForWholeFarm: () -> Unit,
    onOpenMilkSales: () -> Unit,
    onOpenProductionAnalytics: () -> Unit,
    onBack: () -> Unit = {}
) {
    val dayMillis = 86400000L
    var selectedDateMidnight by remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        mutableLongStateOf(cal.timeInMillis)
    }

    val todayMidnight = remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        cal.timeInMillis
    }

    var selectedUnit by remember { mutableStateOf("LITRE") } // LITRE, KILOGRAMS
    var showUnitMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateLabel = remember(selectedDateMidnight, todayMidnight) {
        when (selectedDateMidnight) {
            todayMidnight -> "Today"
            todayMidnight - dayMillis -> "Yesterday"
            todayMidnight + dayMillis -> "Tomorrow"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedDateMidnight))
        }
    }

    // Milking records for the selected date
    val recordsForDate = remember(selectedDateMidnight, allMilkingRecords) {
        allMilkingRecords.filter {
            it.dateEpochMidnight in selectedDateMidnight..(selectedDateMidnight + dayMillis - 1)
        }
    }

    val totalLitersForDate = remember(recordsForDate) {
        recordsForDate.sumOf { it.quantityLiters }
    }

    val unitSuffix = if (selectedUnit == "LITRE") "L" else "Kg"
    val convertedYield = if (selectedUnit == "LITRE") totalLitersForDate else totalLitersForDate * 1.03 // approximate milk density

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Milking",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("milking_hub_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showUnitMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Units & Settings")
                        }
                        DropdownMenu(
                            expanded = showUnitMenu,
                            onDismissRequest = { showUnitMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = selectedUnit == "LITRE",
                                            onClick = { selectedUnit = "LITRE"; showUnitMenu = false }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Litre")
                                    }
                                },
                                onClick = { selectedUnit = "LITRE"; showUnitMenu = false }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = selectedUnit == "KILOGRAMS",
                                            onClick = { selectedUnit = "KILOGRAMS"; showUnitMenu = false }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Kilograms")
                                    }
                                },
                                onClick = { selectedUnit = "KILOGRAMS"; showUnitMenu = false }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF6F8FC))
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date Switcher Header Row (matches video 00:08)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateLabel,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            IconButton(onClick = { selectedDateMidnight -= dayMillis }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day", modifier = Modifier.size(16.dp))
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            IconButton(onClick = { selectedDateMidnight += dayMillis }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day", modifier = Modifier.size(16.dp))
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = RoyalBlueLight,
                            modifier = Modifier.size(34.dp)
                        ) {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date", tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Circular KPI Status Widgets (matches video 00:09)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Production Status Circle Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenProductionAnalytics() }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF0E5FC),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF7E22CE), modifier = Modifier.size(24.dp))
                                }
                            }
                            Text(
                                text = if (totalLitersForDate > 0) "${String.format(Locale.getDefault(), "%.2f", convertedYield)} $unitSuffix" else "No Data",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Production",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Milk Sale Status Circle Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenMilkSales() }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFFECEF),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(24.dp))
                                }
                            }
                            Text(
                                text = if (totalLitersForDate > 0) "₹${(totalLitersForDate * 50.0).toInt()}" else "No Data",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Milk sale",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Production Section Card (matches video 00:09)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenProductionAnalytics() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Production",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF3E8FF),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF7E22CE), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Text(
                            text = if (totalLitersForDate > 0) "${String.format(Locale.getDefault(), "%.2f", convertedYield)} $unitSuffix recorded" else "No Data",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )

                        // Two Blue Action Buttons (matches video 00:09)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onOpenAddForEachCattle,
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f).testTag("add_for_each_cattle_btn")
                            ) {
                                Text(
                                    text = "Add for each cattle",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = onOpenAddForWholeFarm,
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f).testTag("add_for_whole_farm_btn")
                            ) {
                                Text(
                                    text = "Add for whole farm",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Sales Section Card (matches video 00:09)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenMilkSales() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Sales",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFFECEF),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Sell, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Text(
                            text = if (totalLitersForDate > 0) "₹${(totalLitersForDate * 50.0).toInt()} (Estimated)" else "No Data",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )

                        Button(
                            onClick = onOpenMilkSales,
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().testTag("add_milk_sales_btn")
                        ) {
                            Text(
                                text = "Add milk sales",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMidnight)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = it
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        selectedDateMidnight = cal.timeInMillis
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
