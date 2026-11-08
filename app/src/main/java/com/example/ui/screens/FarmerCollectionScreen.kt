package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.BulkDispatchEntity
import com.example.data.local.entity.FarmerEntity
import com.example.data.local.entity.MilkCollectionEntity
import com.example.data.repository.PricingEngine
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerCollectionScreen(
    viewModel: MilkMateViewModel,
    initialTab: Int = 0,
    showInternalTabs: Boolean = false,
    onOpenSettings: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) } // 0: Collection Desk, 1: Farmers & Passbooks, 2: Bulk Dispatches
    var showAddFarmerDialog by remember { mutableStateOf(false) }
    var showAddDispatchDialog by remember { mutableStateOf(false) }
    var selectedFarmerForPassbook by remember { mutableStateOf<FarmerEntity?>(null) }
    var showRecordFarmerPaymentDialog by remember { mutableStateOf<FarmerEntity?>(null) }
    var showCustomFormulaStudio by remember { mutableStateOf(false) }

    val farmers by viewModel.farmers.collectAsStateWithLifecycle()
    val todayCollections by viewModel.todayMilkCollections.collectAsStateWithLifecycle()
    val allCollections by viewModel.allMilkCollections.collectAsStateWithLifecycle()
    val bulkDispatches by viewModel.bulkDispatches.collectAsStateWithLifecycle()
    val selectedShift by viewModel.selectedShift.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val formattedDate = sdf.format(Date(selectedDate))

    val isTrader = business?.businessMode == "TRADER"

    val (headerTitle, headerSubtitle) = when {
        selectedTab == 1 -> "Farmers Directory & Khata" to "Member Farmers • Passbooks • Cycle Payouts"
        selectedTab == 2 -> "Bulk Dispatches" to "Chilling Unit Outward Challans & Factory Invoicing"
        else -> "Milk Collection Desk" to "Shift-wise Farmer Intake • FAT/SNF Testing • Receipts"
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showAddFarmerDialog = true },
                    containerColor = RoyalBluePrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_farmer_fab")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Farmer")
                }
            } else if (selectedTab == 2) {
                FloatingActionButton(
                    onClick = { showAddDispatchDialog = true },
                    containerColor = GrassGreen,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_dispatch_fab")
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = "New Bulk Dispatch")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showInternalTabs) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tabsList = listOf(
                            Triple(0, "🥛 Collection", Icons.Default.WaterDrop),
                            Triple(1, "👥 Farmers", Icons.Default.People),
                            Triple(2, "🚚 Dispatches", Icons.Default.LocalShipping)
                        )
                        tabsList.forEach { (idx, title, icon) ->
                            val isSel = selectedTab == idx
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = idx }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSel) Color.White else TextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = title.split(" ")[1],
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
            when (selectedTab) {
                0 -> CollectionDeskTab(
                    viewModel = viewModel,
                    farmers = farmers,
                    todayCollections = todayCollections,
                    selectedDate = formattedDate,
                    selectedShift = selectedShift,
                    onOpenFormulaStudio = { showCustomFormulaStudio = true },
                    onOpenSettings = { onOpenSettings?.invoke() },
                    onAddNewFarmer = { showAddFarmerDialog = true }
                )
                1 -> FarmersDirectoryTab(
                    farmers = farmers,
                    collections = allCollections,
                    onOpenPassbook = { selectedFarmerForPassbook = it },
                    onRecordPayment = { showRecordFarmerPaymentDialog = it },
                    onDeleteFarmer = { viewModel.deleteFarmer(it.id) }
                )
                2 -> BulkDispatchesTab(
                    dispatches = bulkDispatches,
                    onDeleteDispatch = { viewModel.deleteBulkDispatch(it.id) }
                )
            }
        }
    }
    }

    // Add Farmer Dialog
    if (showAddFarmerDialog) {
        AddFarmerDialog(
            onDismiss = { showAddFarmerDialog = false },
            onSave = { farmer ->
                viewModel.saveFarmer(farmer) {
                    showAddFarmerDialog = false
                }
            },
            businessId = business?.id ?: ""
        )
    }

    // Add Bulk Dispatch Dialog
    if (showAddDispatchDialog) {
        AddBulkDispatchDialog(
            onDismiss = { showAddDispatchDialog = false },
            onSave = { dispatch ->
                viewModel.saveBulkDispatch(dispatch) {
                    showAddDispatchDialog = false
                }
            },
            businessId = business?.id ?: "",
            selectedDateEpoch = selectedDate,
            shift = selectedShift
        )
    }

    // Farmer Passbook Modal
    selectedFarmerForPassbook?.let { farmer ->
        FarmerPassbookDialog(
            farmer = farmer,
            collections = allCollections.filter { it.farmerId == farmer.id },
            onDismiss = { selectedFarmerForPassbook = null },
            onPay = {
                selectedFarmerForPassbook = null
                showRecordFarmerPaymentDialog = farmer
            }
        )
    }

    // Record Farmer Payment Dialog
    showRecordFarmerPaymentDialog?.let { farmer ->
        RecordFarmerPaymentDialog(
            farmer = farmer,
            onDismiss = { showRecordFarmerPaymentDialog = null },
            onConfirmPayment = { amount, mode, ref ->
                viewModel.recordFarmerPayment(farmer.id, amount, mode, ref)
                showRecordFarmerPaymentDialog = null
            }
        )
    }

    if (showCustomFormulaStudio) {
        CustomFormulaStudioDialog(
            business = business,
            viewModel = viewModel,
            onDismiss = { showCustomFormulaStudio = false }
        )
    }
}

@Composable
fun CollectionDeskTab(
    viewModel: MilkMateViewModel,
    farmers: List<FarmerEntity>,
    todayCollections: List<MilkCollectionEntity>,
    selectedDate: String,
    selectedShift: String,
    onOpenFormulaStudio: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onAddNewFarmer: () -> Unit = {}
) {
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val supportedTypes = business?.supportedMilkTypes ?: "BOTH"

    var searchFarmerCode by remember { mutableStateOf("") }
    var selectedFarmer by remember { mutableStateOf<FarmerEntity?>(null) }
    var milkType by remember(supportedTypes) { mutableStateOf(if (supportedTypes == "BUFFALO_ONLY") "BUFFALO" else "COW") }
    var quantityText by remember { mutableStateOf("5.0") }
    var fatText by remember { mutableStateOf("4.2") }
    var snfText by remember { mutableStateOf("8.5") }
    var clrText by remember { mutableStateOf("28.0") }
    var sampleNo by remember { mutableStateOf("") }
    var showWhatsAppSlipModal by remember { mutableStateOf<MilkCollectionEntity?>(null) }
    var showOnScreenNumPad by remember { mutableStateOf(false) }
    var rosterFilter by remember { mutableStateOf("ALL") } // "ALL", "PENDING", "COLLECTED"
    var rosterViewMode by remember { mutableStateOf("ROW") } // "ROW", "GRID"
    var showAnalyzerSetupModal by remember { mutableStateOf(false) }
    var isCapturingFromAnalyzer by remember { mutableStateOf(false) }
    var showVoiceIntakeDialog by remember { mutableStateOf(false) }

    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val currentMode = business?.businessMode ?: "FARMER"
    val context = LocalContext.current

    val sortedFarmers = remember(farmers) {
        farmers.sortedWith(compareBy({ it.farmerCode.toIntOrNull() ?: 9999 }, { it.name }))
    }

    LaunchedEffect(sortedFarmers) {
        if (selectedFarmer == null && sortedFarmers.isNotEmpty()) {
            val f = sortedFarmers.first()
            selectedFarmer = f
            searchFarmerCode = f.farmerCode
            milkType = if (f.milkType == "BUFFALO") "BUFFALO" else "COW"
            quantityText = if (f.defaultQuantityEstimate > 0) f.defaultQuantityEstimate.toString() else "5.0"
        }
    }

    val filteredCollections = todayCollections.filter { it.shift == selectedShift }
    val totalShiftLiters = filteredCollections.sumOf { it.quantityLiters }
    val totalShiftPayout = filteredCollections.sumOf { it.totalAmount }
    val avgFat = if (filteredCollections.isNotEmpty()) filteredCollections.map { it.fat }.average() else 0.0
    val avgSnf = if (filteredCollections.isNotEmpty()) filteredCollections.map { it.snf }.average() else 0.0

    val collectedFarmerIds = remember(filteredCollections) {
        filteredCollections.map { it.farmerId }.toSet()
    }
    val pendingFarmers = remember(sortedFarmers, collectedFarmerIds) {
        sortedFarmers.filter { it.id !in collectedFarmerIds }
    }
    val collectedFarmers = remember(sortedFarmers, collectedFarmerIds) {
        sortedFarmers.filter { it.id in collectedFarmerIds }
    }

    val displayedRosterFarmers = when (rosterFilter) {
        "PENDING" -> pendingFarmers
        "COLLECTED" -> collectedFarmers
        else -> sortedFarmers
    }

    fun selectFarmer(f: FarmerEntity) {
        selectedFarmer = f
        searchFarmerCode = f.farmerCode
        milkType = if (f.milkType == "BUFFALO") "BUFFALO" else "COW"
        quantityText = if (f.defaultQuantityEstimate > 0) f.defaultQuantityEstimate.toString() else "5.0"
    }

    fun onCodeInputChanged(query: String) {
        searchFarmerCode = query
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            selectedFarmer = null
            return
        }
        val match = sortedFarmers.find { it.farmerCode.equals(trimmed, ignoreCase = true) }
            ?: sortedFarmers.find { it.farmerCode.startsWith(trimmed, ignoreCase = true) }
            ?: sortedFarmers.find { it.name.contains(trimmed, ignoreCase = true) || it.mobile.contains(trimmed) }

        if (match != null) {
            selectedFarmer = match
            milkType = if (match.milkType == "BUFFALO") "BUFFALO" else "COW"
            quantityText = if (match.defaultQuantityEstimate > 0) match.defaultQuantityEstimate.toString() else "5.0"
        }
    }

    fun appendNumPad(digit: String) {
        val next = searchFarmerCode + digit
        onCodeInputChanged(next)
    }

    fun backspaceNumPad() {
        if (searchFarmerCode.isNotEmpty()) {
            val next = searchFarmerCode.dropLast(1)
            onCodeInputChanged(next)
        }
    }

    fun clearNumPad() {
        onCodeInputChanged("")
    }

    // Auto calculate Rate & Total
    val qty = quantityText.toDoubleOrNull() ?: 0.0
    val fatVal = fatText.toDoubleOrNull() ?: 4.0
    val snfVal = snfText.toDoubleOrNull() ?: 8.5
    val clrVal = clrText.toDoubleOrNull() ?: 0.0

    val calculatedRate = viewModel.calculateProcurementRate(milkType, fatVal, snfVal, clrVal) + (selectedFarmer?.rateAdjustmentPerLiter ?: 0.0)
    val totalAmount = qty * calculatedRate

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Shift Inflow Summary Bar (Compact, high-efficiency)
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = OceanMidnight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TOTAL MILK", color = Color.White.copy(alpha = 0.65f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.1f", totalShiftLiters)} L",
                            color = FreshGold,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(modifier = Modifier.weight(1.1f)) {
                        Text("AVG FAT / SNF", color = Color.White.copy(alpha = 0.65f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.1f", avgFat)}% / ${String.format(Locale.getDefault(), "%.1f", avgSnf)}%",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PAYOUT", color = Color.White.copy(alpha = 0.65f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "₹${String.format(Locale.getDefault(), "%.0f", totalShiftPayout)}",
                            color = GrassGreen,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(modifier = Modifier.weight(0.9f), horizontalAlignment = Alignment.End) {
                        Text("FARMERS", color = Color.White.copy(alpha = 0.65f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${filteredCollections.size} poured",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Non-Serial Fast Farmer Intake Counter
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth().testTag("farmer_rapid_intake_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header & Active Counter Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🥛 Farmer Intake Counter",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Non-serial counter • Enter code or tap roster • Auto-resets for next farmer",
                                fontSize = 10.5.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { showVoiceIntakeDialog = true },
                                modifier = Modifier
                                    .background(RoyalBlueLight, CircleShape)
                                    .size(32.dp)
                                    .testTag("voice_intake_btn")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Intake", tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedFarmer != null) DairyGreenLight else RoyalBlueLight
                            ) {
                                Text(
                                    text = if (selectedFarmer != null) "#${selectedFarmer?.farmerCode} Selected" else "⚡ Counter Ready",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedFarmer != null) DairyGreen else RoyalBluePrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Direct Farmer Code & Quick Dialpad Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchFarmerCode,
                            onValueChange = { onCodeInputChanged(it) },
                            label = { Text("Farmer Code # or Name") },
                            placeholder = { Text("e.g. 14") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = RoyalBluePrimary) },
                            trailingIcon = {
                                if (searchFarmerCode.isNotBlank() || selectedFarmer != null) {
                                    IconButton(onClick = { clearNumPad() }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f).testTag("farmer_search_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedButton(
                            onClick = { showOnScreenNumPad = !showOnScreenNumPad },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            colors = if (showOnScreenNumPad) ButtonDefaults.outlinedButtonColors(containerColor = RoyalBlueLight) else ButtonDefaults.outlinedButtonColors(),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Icon(Icons.Default.Dialpad, contentDescription = null, modifier = Modifier.size(18.dp), tint = RoyalBluePrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showOnScreenNumPad) "Hide" else "NumPad", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                        }
                    }

                    // On-Screen Touch Keypad for Fast Counter Dialing
                    if (showOnScreenNumPad) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    listOf("1", "2", "3"),
                                    listOf("4", "5", "6"),
                                    listOf("7", "8", "9"),
                                    listOf("CLR", "0", "⌫")
                                ).forEach { rowKeys ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowKeys.forEach { key ->
                                            FilledTonalButton(
                                                onClick = {
                                                    when (key) {
                                                        "CLR" -> clearNumPad()
                                                        "⌫" -> backspaceNumPad()
                                                        else -> appendNumPad(key)
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = when (key) {
                                                        "CLR" -> DangerRed.copy(alpha = 0.15f)
                                                        "⌫" -> WarmHoney.copy(alpha = 0.2f)
                                                        else -> MaterialTheme.colorScheme.surface
                                                    }
                                                ),
                                                contentPadding = PaddingValues(vertical = 10.dp),
                                                modifier = Modifier.weight(1f).height(44.dp)
                                            ) {
                                                Text(
                                                    text = key,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (key) {
                                                        "CLR" -> DangerRed
                                                        "⌫" -> TextPrimary
                                                        else -> TextPrimary
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Farmer Code Roster (Non-Serial Speed-Dial / Roster View)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ Farmer Counter Speed Dial (${displayedRosterFarmers.size})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Toggle Grid vs Row View
                                IconButton(
                                    onClick = { rosterViewMode = if (rosterViewMode == "GRID") "ROW" else "GRID" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        if (rosterViewMode == "GRID") Icons.Default.ViewAgenda else Icons.Default.GridView,
                                        contentDescription = "Toggle Grid / Row",
                                        tint = RoyalBluePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                OutlinedButton(
                                    onClick = onAddNewFarmer,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("+ Farmer", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Filter Chips Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = rosterFilter == "ALL",
                                onClick = { rosterFilter = "ALL" },
                                label = { Text("All (${sortedFarmers.size})", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = rosterFilter == "PENDING",
                                onClick = { rosterFilter = "PENDING" },
                                label = { Text("⏳ Waiting (${pendingFarmers.size})", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = rosterFilter == "COLLECTED",
                                onClick = { rosterFilter = "COLLECTED" },
                                label = { Text("✓ Poured (${collectedFarmers.size})", fontSize = 10.sp) }
                            )
                        }

                        if (displayedRosterFarmers.isEmpty()) {
                            Text(
                                text = if (rosterFilter == "PENDING") "🎉 All registered farmers have poured their milk for this shift!" else "No farmers found.",
                                fontSize = 11.sp,
                                color = DairyGreen,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        } else if (rosterViewMode == "GRID") {
                            // Touch Speed-Dial Grid (3 Columns)
                            val chunkedFarmers = displayedRosterFarmers.chunked(3)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                chunkedFarmers.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowItems.forEach { f ->
                                            val isCollected = collectedFarmerIds.contains(f.id)
                                            val isSelected = selectedFarmer?.id == f.id

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = when {
                                                    isSelected -> RoyalBluePrimary
                                                    isCollected -> DairyGreenLight
                                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                },
                                                border = when {
                                                    isSelected -> androidx.compose.foundation.BorderStroke(1.5.dp, RoyalBluePrimary)
                                                    isCollected -> androidx.compose.foundation.BorderStroke(1.dp, DairyGreen.copy(alpha = 0.5f))
                                                    else -> null
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { selectFarmer(f) }
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "#${f.farmerCode}",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = when {
                                                                isSelected -> Color.White
                                                                isCollected -> DairyGreen
                                                                else -> RoyalBluePrimary
                                                            }
                                                        )
                                                        if (isCollected) {
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Icon(Icons.Default.Check, contentDescription = null, tint = if (isSelected) Color.White else DairyGreen, modifier = Modifier.size(11.dp))
                                                        }
                                                    }
                                                    Text(
                                                        text = f.name,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = if (isSelected || isCollected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) Color.White else TextPrimary,
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = if (f.milkType == "BUFFALO") "🐃 Buff" else "🐄 Cow",
                                                        fontSize = 9.sp,
                                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary
                                                    )
                                                }
                                            }
                                        }
                                        repeat(3 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        } else {
                            // Horizontal Scroll Row
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(displayedRosterFarmers) { f ->
                                    val isCollected = collectedFarmerIds.contains(f.id)
                                    val isSelected = selectedFarmer?.id == f.id

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when {
                                            isSelected -> RoyalBluePrimary
                                            isCollected -> DairyGreenLight
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        },
                                        border = when {
                                            isSelected -> androidx.compose.foundation.BorderStroke(1.5.dp, RoyalBluePrimary)
                                            isCollected -> androidx.compose.foundation.BorderStroke(1.dp, DairyGreen.copy(alpha = 0.5f))
                                            else -> null
                                        },
                                        modifier = Modifier.clickable { selectFarmer(f) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "#${f.farmerCode} ${f.name}",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected || isCollected) FontWeight.Bold else FontWeight.Medium,
                                                color = when {
                                                    isSelected -> Color.White
                                                    isCollected -> DairyGreen
                                                    else -> TextPrimary
                                                }
                                            )
                                            if (isCollected && !isSelected) {
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Icon(Icons.Default.Check, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Selected Farmer Details Banner & Milk Quality Intake Form
                    if (selectedFarmer != null) {
                        val f = selectedFarmer!!
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = RoyalBlueLight.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "#${f.farmerCode} ${f.name}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = RoyalBluePrimary,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (collectedFarmerIds.contains(f.id)) DairyGreenLight else WarmHoney.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (collectedFarmerIds.contains(f.id)) "✓ Already Poured" else "⏳ Waiting",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (collectedFarmerIds.contains(f.id)) DairyGreen else WarmHoney,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Village: ${f.village.ifBlank { "Local" }} • Mobile: ${f.mobile}",
                                        fontSize = 10.5.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Payable Due", fontSize = 9.sp, color = TextSecondary)
                                    Text(
                                        text = "₹${String.format(Locale.getDefault(), "%.1f", f.balancePayable)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DangerRed
                                    )
                                }
                            }
                        }

                        // Milk Type Toggle or Specialization Indicator
                        if (supportedTypes == "BOTH") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("COW" to "🐄 Cow Milk", "BUFFALO" to "🐃 Buffalo Milk").forEach { (type, label) ->
                                    val isSel = milkType == type
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSel) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { milkType = type }
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSel) Color.White else TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RoyalBlueLight,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (supportedTypes == "COW_ONLY") "🐄 Active Milk Specialization: Cow Milk Only" else "🐃 Active Milk Specialization: Buffalo Milk Only",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBluePrimary,
                                    modifier = Modifier.padding(10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        // Quantity Input + Quick Steppers
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Quantity (Liters)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("+1L" to 1.0, "+2L" to 2.0, "+5L" to 5.0, "+10L" to 10.0).forEach { (btnText, addQty) ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = RoyalBlueLight,
                                            modifier = Modifier.clickable {
                                                val current = quantityText.toDoubleOrNull() ?: 0.0
                                                quantityText = String.format(Locale.getDefault(), "%.1f", current + addQty)
                                            }
                                        ) {
                                            Text(
                                                text = btnText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RoyalBluePrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = quantityText,
                                onValueChange = { quantityText = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth().testTag("procure_quantity_input"),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = { Text("Liters", modifier = Modifier.padding(end = 12.dp), color = TextSecondary, fontSize = 12.sp) }
                            )
                        }

                        // Quality Parameters: FAT, SNF, CLR with Bluetooth Auto-Capture
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quality Parameters (FAT, SNF, CLR)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (session.analyzerDeviceAddress.isNotBlank()) DairyGreenLight else RoyalBlueLight,
                                border = BorderStroke(1.dp, if (session.analyzerDeviceAddress.isNotBlank()) DairyGreen else RoyalBluePrimary),
                                modifier = Modifier.clickable {
                                    if (session.analyzerDeviceAddress.isBlank()) {
                                        showAnalyzerSetupModal = true
                                    } else {
                                        isCapturingFromAnalyzer = true
                                        viewModel.captureLiveAnalyzerSample(context) { success, sample, msg ->
                                            isCapturingFromAnalyzer = false
                                            if (success && sample != null) {
                                                if (sample.fat > 0) fatText = String.format(Locale.US, "%.2f", sample.fat)
                                                if (sample.snf > 0) snfText = String.format(Locale.US, "%.2f", sample.snf)
                                                if (sample.clr > 0) clrText = String.format(Locale.US, "%.1f", sample.clr)
                                                if (sample.weightKg > 0) quantityText = String.format(Locale.US, "%.1f", sample.weightKg)
                                                viewModel.showMessage("✓ Auto-captured from ${session.analyzerDeviceName.ifBlank { "Analyzer" }}: FAT ${sample.fat}%, SNF ${sample.snf}%, ${sample.weightKg}L")
                                            } else {
                                                viewModel.showMessage(msg)
                                            }
                                        }
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isCapturingFromAnalyzer) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = DairyGreen)
                                        Text("Reading...", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                                    } else {
                                        Icon(
                                            Icons.Default.Bluetooth,
                                            contentDescription = null,
                                            tint = if (session.analyzerDeviceAddress.isNotBlank()) DairyGreen else RoyalBluePrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (session.analyzerDeviceAddress.isNotBlank()) "⚡ Auto-Capture" else "⚡ Connect Analyzer",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (session.analyzerDeviceAddress.isNotBlank()) DairyGreen else RoyalBluePrimary
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = fatText,
                                onValueChange = { fatText = it },
                                label = { Text("FAT %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("procure_fat_input"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = snfText,
                                onValueChange = { snfText = it },
                                label = { Text("SNF %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("procure_snf_input"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = clrText,
                                onValueChange = {
                                    clrText = it
                                    val clr = it.toDoubleOrNull() ?: 0.0
                                    val fat = fatText.toDoubleOrNull() ?: 4.0
                                    if (clr > 0) {
                                        val calcSnf = (clr / 4.0) + (fat * 0.25) + 0.36
                                        snfText = String.format(Locale.getDefault(), "%.2f", calcSnf)
                                    }
                                },
                                label = { Text("CLR (Lacto)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        // Live Calculated Rate & Amount Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = RoyalBluePrimary.copy(alpha = 0.15f)
                                        ) {
                                            val modeStr = business?.pricingMode ?: "STANDARD"
                                            val yieldInfo = when (modeStr.uppercase()) {
                                                "KHOA_YIELD", "KHOYA_YIELD" -> " • 🍬 Khoa: ${String.format(Locale.getDefault(), "%.1f", PricingEngine.calculateKhoaYieldGrams(fatVal, snfVal))}g/L"
                                                "PANEER_YIELD" -> " • 🧀 Paneer: ${String.format(Locale.getDefault(), "%.1f", PricingEngine.calculatePaneerYieldGrams(fatVal, snfVal))}g/L"
                                                "GHEE_YIELD" -> " • 🏺 Ghee: ${String.format(Locale.getDefault(), "%.1f", PricingEngine.calculateGheeYieldGrams(fatVal))}g/L"
                                                else -> ""
                                            }
                                            Text(
                                                text = "⚙️ $modeStr$yieldInfo",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RoyalBluePrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    TextButton(
                                        onClick = onOpenFormulaStudio,
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(12.dp), tint = RoyalBluePrimary)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Tweak Formula", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Formula Rate / L", fontSize = 10.sp, color = TextSecondary)
                                        Text(
                                            text = "₹${String.format(Locale.getDefault(), "%.2f", calculatedRate)} / L",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RoyalBluePrimary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Total Collection Amount", fontSize = 10.sp, color = TextSecondary)
                                        Text(
                                            text = "₹${String.format(Locale.getDefault(), "%.2f", totalAmount)}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = GrassGreen
                                        )
                                    }
                                }
                            }
                        }

                        // Fast Non-Serial Action Buttons
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Primary 1-Tap Save & Ready for Next Farmer
                            Button(
                                onClick = {
                                    val currentF = selectedFarmer
                                    if (currentF == null) {
                                        viewModel.showMessage("Please select or search a farmer first")
                                        return@Button
                                    }
                                    if (qty <= 0) {
                                        viewModel.showMessage("Please enter valid milk quantity")
                                        return@Button
                                    }

                                    val collection = MilkCollectionEntity(
                                        id = UUID.randomUUID().toString(),
                                        businessId = business?.id ?: "",
                                        farmerId = currentF.id,
                                        farmerCode = currentF.farmerCode,
                                        farmerName = currentF.name,
                                        dateEpochMidnight = MilkMateViewModel.getTodayMidnightMillis(),
                                        shift = selectedShift,
                                        milkType = milkType,
                                        quantityLiters = qty,
                                        fat = fatVal,
                                        snf = snfVal,
                                        clr = clrVal,
                                        ratePerLiter = calculatedRate,
                                        totalAmount = totalAmount,
                                        paymentStatus = "PENDING",
                                        sampleNo = sampleNo
                                    )

                                    viewModel.saveMilkCollection(collection) {
                                        if (session.printerAutoPrint && session.printerDeviceAddress.isNotBlank()) {
                                            viewModel.printFarmerIntakeSlip(context, collection, currentF)
                                        }
                                        viewModel.showMessage("✓ Saved ${qty}L for #${currentF.farmerCode} ${currentF.name} (₹${totalAmount.toInt()})")
                                        // Auto reset desk ready for next incoming farmer
                                        searchFarmerCode = ""
                                        selectedFarmer = null
                                        sampleNo = ""
                                        quantityText = "5.0"
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_procure_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⚡ Save & Ready for Next Farmer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            // Secondary: Save and generate WhatsApp slip / receipt
                            OutlinedButton(
                                onClick = {
                                    val currentF = selectedFarmer
                                    if (currentF == null) {
                                        viewModel.showMessage("Please select or search a farmer first")
                                        return@OutlinedButton
                                    }
                                    if (qty <= 0) {
                                        viewModel.showMessage("Please enter valid milk quantity")
                                        return@OutlinedButton
                                    }

                                    val collection = MilkCollectionEntity(
                                        id = UUID.randomUUID().toString(),
                                        businessId = business?.id ?: "",
                                        farmerId = currentF.id,
                                        farmerCode = currentF.farmerCode,
                                        farmerName = currentF.name,
                                        dateEpochMidnight = MilkMateViewModel.getTodayMidnightMillis(),
                                        shift = selectedShift,
                                        milkType = milkType,
                                        quantityLiters = qty,
                                        fat = fatVal,
                                        snf = snfVal,
                                        clr = clrVal,
                                        ratePerLiter = calculatedRate,
                                        totalAmount = totalAmount,
                                        paymentStatus = "PENDING",
                                        sampleNo = sampleNo
                                    )

                                    viewModel.saveMilkCollection(collection) {
                                        if (session.printerAutoPrint && session.printerDeviceAddress.isNotBlank()) {
                                            viewModel.printFarmerIntakeSlip(context, collection, currentF)
                                        }
                                        showWhatsAppSlipModal = collection
                                        searchFarmerCode = ""
                                        selectedFarmer = null
                                        sampleNo = ""
                                        quantityText = "5.0"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📄 Save & Open Slip / WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        // Empty State / Prompt when no farmer selected
                        if (sortedFarmers.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = RoyalBlueLight.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().clickable { onAddNewFarmer() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("No Farmers Registered Yet", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = RoyalBluePrimary)
                                        Text("Tap here to add your first member farmer and begin collecting milk.", fontSize = 10.5.sp, color = TextSecondary)
                                    }
                                    Button(
                                        onClick = onAddNewFarmer,
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
                                    ) {
                                        Text("+ Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Ready for incoming milk: Enter a Farmer Code above, use the keypad, or tap any farmer from the roster.",
                                        fontSize = 11.5.sp,
                                        color = TextSecondary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Today's Shift Collection Log
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 Today's $selectedShift Entries (${filteredCollections.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        if (filteredCollections.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No collection entries logged for this shift yet.", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(filteredCollections) { entry ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (entry.milkType == "BUFFALO") DeepOceanNavy else RoyalBlueLight,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "#${entry.farmerCode}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (entry.milkType == "BUFFALO") Color.White else RoyalBluePrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(entry.farmerName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text(
                                    text = "${entry.milkType} • FAT: ${entry.fat}% | SNF: ${entry.snf}% • @₹${String.format(Locale.getDefault(), "%.1f", entry.ratePerLiter)}/L",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${entry.quantityLiters} L",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = RoyalBluePrimary
                                )
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%.2f", entry.totalAmount)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GrassGreen
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { viewModel.deleteMilkCollection(entry.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVoiceIntakeDialog) {
        VoiceIntakeDialog(
            onDismiss = { showVoiceIntakeDialog = false },
            onVoiceParsed = { q, f, s ->
                quantityText = q.toString()
                fatText = f.toString()
                snfText = s.toString()
            }
        )
    }

    // Receipt / WhatsApp Slip Dialog
    showWhatsAppSlipModal?.let { entry ->
        AlertDialog(
            onDismissRequest = { showWhatsAppSlipModal = null },
            title = { Text("🥛 Milk Collection Slip Generated") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Farmer: ${entry.farmerName} (#${entry.farmerCode})", fontWeight = FontWeight.Bold)
                    Text("Shift: ${entry.shift} | Date: $selectedDate")
                    Text("Milk: ${entry.milkType} | Volume: ${entry.quantityLiters} Liters")
                    Text("FAT: ${entry.fat}% | SNF: ${entry.snf}%")
                    Text("Rate / L: ₹${String.format(Locale.getDefault(), "%.2f", entry.ratePerLiter)}")
                    Text("Total Amount: ₹${String.format(Locale.getDefault(), "%.2f", entry.totalAmount)}", fontWeight = FontWeight.ExtraBold, color = GrassGreen)
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Thermal Slip Print Button
                    Button(
                        onClick = {
                            val currentFarmer = farmers.find { it.id == entry.farmerId }
                            viewModel.printFarmerIntakeSlip(context, entry, currentFarmer) { success, msg ->
                                if (success) {
                                    viewModel.showMessage("✓ Thermal Slip Printed!")
                                } else {
                                    viewModel.showMessage(msg)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (session.printerDeviceAddress.isNotBlank())
                                "🖨️ Print Slip (${session.printerPaperWidth})"
                            else
                                "🖨️ Print Slip (Setup in Settings)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // WhatsApp Receipt Button
                    OutlinedButton(
                        onClick = {
                            val message = "🥛 *${business?.businessName ?: "Dairy Centre"} Collection Slip*\n" +
                                    "Farmer: ${entry.farmerName} (#${entry.farmerCode})\n" +
                                    "Date: $selectedDate (${entry.shift})\n" +
                                    "Milk: ${entry.milkType}\n" +
                                    "Qty: ${entry.quantityLiters} L\n" +
                                    "FAT: ${entry.fat}% | SNF: ${entry.snf}%\n" +
                                    "Rate: ₹${String.format(Locale.getDefault(), "%.2f", entry.ratePerLiter)}/L\n" +
                                    "Amount: ₹${String.format(Locale.getDefault(), "%.2f", entry.totalAmount)}\n" +
                                    "Thank you!"
                            val sendIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(message))
                            }
                            context.startActivity(sendIntent)
                            showWhatsAppSlipModal = null
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = GrassGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share on WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GrassGreen)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showWhatsAppSlipModal = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (showAnalyzerSetupModal) {
        BluetoothMilkAnalyzerDialog(
            viewModel = viewModel,
            onDismiss = { showAnalyzerSetupModal = false }
        )
    }
}

@Composable
fun FarmersDirectoryTab(
    farmers: List<FarmerEntity>,
    collections: List<MilkCollectionEntity>,
    onOpenPassbook: (FarmerEntity) -> Unit,
    onRecordPayment: (FarmerEntity) -> Unit,
    onDeleteFarmer: (FarmerEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = farmers.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.farmerCode.contains(searchQuery, ignoreCase = true) ||
        it.village.contains(searchQuery, ignoreCase = true)
    }

    val totalPayableDues = farmers.sumOf { it.balancePayable }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Dues Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Registered Farmers", fontSize = 11.sp, color = TextSecondary)
                        Text("${farmers.size} Farmers", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Pending Farmer Payouts", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            "₹${String.format(Locale.getDefault(), "%.2f", totalPayableDues)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DangerRed
                        )
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Farmers by Code, Name, Village") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().testTag("farmer_directory_search"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No farmers found. Tap '+' to add a new farmer.", color = TextSecondary)
                }
            }
        } else {
            items(filtered) { farmer ->
                val farmerCollections = collections.filter { it.farmerId == farmer.id }
                val totalLitersProcured = farmerCollections.sumOf { it.quantityLiters }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPassbook(farmer) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = RoyalBlueLight,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "#${farmer.farmerCode}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = RoyalBluePrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(farmer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    Text(
                                        text = "${farmer.village.ifBlank { "Village" }} • ${farmer.milkType} • ${farmer.mobile}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Balance Payable", fontSize = 10.sp, color = TextSecondary)
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%.2f", farmer.balancePayable)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (farmer.balancePayable > 0) DangerRed else GrassGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Supplied: ${String.format(Locale.getDefault(), "%.1f", totalLitersProcured)}L",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { onOpenPassbook(farmer) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("📖 Passbook", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { onRecordPayment(farmer) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GrassGreen),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("💰 Pay Farmer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BulkDispatchesTab(
    dispatches: List<BulkDispatchEntity>,
    onDeleteDispatch: (BulkDispatchEntity) -> Unit
) {
    val totalDispatchedLiters = dispatches.sumOf { it.totalLiters }
    val totalDispatchBilling = dispatches.sumOf { it.totalAmount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DeepOceanNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🚚 Bulk Dispatches & Tanker Outwards", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Shipments to Chilling Centres, Dairies & Wholesale Buyers", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Dispatched", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", totalDispatchedLiters)} L",
                                color = FreshGold,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Bulk Billing", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(
                                text = "₹${String.format(Locale.getDefault(), "%.2f", totalDispatchBilling)}",
                                color = GrassGreen,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Recent Tanker & Bulk Shipments (${dispatches.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        if (dispatches.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No bulk dispatches recorded yet. Tap '+' to log a shipment.", color = TextSecondary)
                }
            }
        } else {
            items(dispatches) { d ->
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val dateStr = sdf.format(Date(d.dateEpochMidnight))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(d.buyerOrPlantName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Text(
                                    text = "$dateStr (${d.shift}) • Vehicle: ${d.vehicleNo.ifBlank { "Direct" }}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${d.totalLiters} L",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = RoyalBluePrimary
                                )
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%.2f", d.totalAmount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = GrassGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Avg FAT: ${d.avgFat}% | SNF: ${d.avgSnf}% • @₹${d.ratePerLiter}/L",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )

                            IconButton(
                                onClick = { onDeleteDispatch(d) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddFarmerDialog(
    onDismiss: () -> Unit,
    onSave: (FarmerEntity) -> Unit,
    businessId: String
) {
    var farmerCode by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var milkType by remember { mutableStateOf("COW") }
    var paymentMode by remember { mutableStateOf("UPI") }
    var upiId by remember { mutableStateOf("") }
    var bankAccountNo by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    var defaultQty by remember { mutableStateOf("5.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("👥 Register New Farmer") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = farmerCode,
                            onValueChange = { farmerCode = it },
                            label = { Text("Code (e.g. 104)") },
                            modifier = Modifier.weight(1f).testTag("farmer_code_field"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = defaultQty,
                            onValueChange = { defaultQty = it },
                            label = { Text("Est. Qty (L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Farmer Full Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("farmer_name_field"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Mobile Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = village,
                        onValueChange = { village = it },
                        label = { Text("Village / Area") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    Text("Milk Type Supplied:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("COW", "BUFFALO", "BOTH").forEach { type ->
                            val isSel = milkType == type
                            FilterChip(
                                selected = isSel,
                                onClick = { milkType = type },
                                label = { Text(type, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Text("Payment Preference:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("UPI", "CASH", "BANK_TRANSFER").forEach { mode ->
                            val isSel = paymentMode == mode
                            FilterChip(
                                selected = isSel,
                                onClick = { paymentMode = mode },
                                label = { Text(mode.replace("_", " "), fontSize = 11.sp) }
                            )
                        }
                    }
                }

                if (paymentMode == "UPI") {
                    item {
                        OutlinedTextField(
                            value = upiId,
                            onValueChange = { upiId = it },
                            label = { Text("UPI ID (e.g. name@upi)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                } else if (paymentMode == "BANK_TRANSFER") {
                    item {
                        OutlinedTextField(
                            value = bankAccountNo,
                            onValueChange = { bankAccountNo = it },
                            label = { Text("Bank Account Number") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = ifscCode,
                            onValueChange = { ifscCode = it },
                            label = { Text("IFSC Code") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || farmerCode.isBlank()) return@Button
                    val entity = FarmerEntity(
                        id = UUID.randomUUID().toString(),
                        businessId = businessId,
                        farmerCode = farmerCode.trim(),
                        name = name.trim(),
                        mobile = mobile.trim(),
                        village = village.trim(),
                        milkType = milkType,
                        defaultQuantityEstimate = defaultQty.toDoubleOrNull() ?: 5.0,
                        paymentMode = paymentMode,
                        upiId = upiId.trim(),
                        bankAccountNo = bankAccountNo.trim(),
                        ifscCode = ifscCode.trim()
                    )
                    onSave(entity)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                modifier = Modifier.testTag("submit_farmer_button")
            ) {
                Text("Save Farmer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddBulkDispatchDialog(
    onDismiss: () -> Unit,
    onSave: (BulkDispatchEntity) -> Unit,
    businessId: String,
    selectedDateEpoch: Long,
    shift: String
) {
    var plantName by remember { mutableStateOf("") }
    var vehicleNo by remember { mutableStateOf("") }
    var driverName by remember { mutableStateOf("") }
    var totalLitersText by remember { mutableStateOf("500.0") }
    var avgFatText by remember { mutableStateOf("4.5") }
    var avgSnfText by remember { mutableStateOf("8.8") }
    var ratePerLiterText by remember { mutableStateOf("56.0") }

    val liters = totalLitersText.toDoubleOrNull() ?: 0.0
    val rate = ratePerLiterText.toDoubleOrNull() ?: 0.0
    val totalBilling = liters * rate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🚚 New Bulk / Tanker Dispatch") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = plantName,
                        onValueChange = { plantName = it },
                        label = { Text("Buyer / Dairy Plant Name *") },
                        placeholder = { Text("e.g. Mother Dairy Chilling Plant") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = totalLitersText,
                            onValueChange = { totalLitersText = it },
                            label = { Text("Total Liters *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = ratePerLiterText,
                            onValueChange = { ratePerLiterText = it },
                            label = { Text("Rate / Liter (₹) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = avgFatText,
                            onValueChange = { avgFatText = it },
                            label = { Text("Avg FAT %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = avgSnfText,
                            onValueChange = { avgSnfText = it },
                            label = { Text("Avg SNF %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = vehicleNo,
                            onValueChange = { vehicleNo = it },
                            label = { Text("Tanker / Vehicle No") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = driverName,
                            onValueChange = { driverName = it },
                            label = { Text("Driver Name") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RoyalBlueLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Invoice Amount:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                            Text(
                                "₹${String.format(Locale.getDefault(), "%.2f", totalBilling)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = GrassGreen
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (plantName.isBlank() || liters <= 0 || rate <= 0) return@Button
                    val entity = BulkDispatchEntity(
                        id = UUID.randomUUID().toString(),
                        businessId = businessId,
                        buyerOrPlantName = plantName.trim(),
                        dateEpochMidnight = selectedDateEpoch,
                        shift = shift,
                        totalLiters = liters,
                        ratePerLiter = rate,
                        totalAmount = totalBilling,
                        avgFat = avgFatText.toDoubleOrNull() ?: 4.5,
                        avgSnf = avgSnfText.toDoubleOrNull() ?: 8.8,
                        vehicleNo = vehicleNo.trim(),
                        driverName = driverName.trim(),
                        paymentStatus = "PENDING"
                    )
                    onSave(entity)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GrassGreen)
            ) {
                Text("Confirm Dispatch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun FarmerPassbookDialog(
    farmer: FarmerEntity,
    collections: List<MilkCollectionEntity>,
    onDismiss: () -> Unit,
    onPay: () -> Unit
) {
    val totalEarnings = collections.sumOf { it.totalAmount }
    val totalLiters = collections.sumOf { it.quantityLiters }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("📖 Farmer Passbook", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${farmer.name} (Code #${farmer.farmerCode})", fontSize = 12.sp, color = RoyalBluePrimary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Summary Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OceanMidnight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Liters", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                            Text("${String.format(Locale.getDefault(), "%.1f", totalLiters)} L", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column {
                            Text("Total Earnings", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                            Text("₹${String.format(Locale.getDefault(), "%.0f", totalEarnings)}", color = FreshGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Balance Due", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                            Text("₹${String.format(Locale.getDefault(), "%.0f", farmer.balancePayable)}", color = DangerRed, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }
                }

                Text("Supply History:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                if (collections.isEmpty()) {
                    Text("No collection logs yet for this farmer.", color = TextSecondary, fontSize = 12.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(collections) { c ->
                            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                            val dStr = sdf.format(Date(c.dateEpochMidnight))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("$dStr • ${c.shift} (${c.milkType})", fontWeight = FontWeight.Medium, fontSize = 11.sp)
                                        Text("${c.quantityLiters}L • FAT: ${c.fat}% • @₹${c.ratePerLiter}/L", fontSize = 10.sp, color = TextSecondary)
                                    }
                                    Text(
                                        "₹${String.format(Locale.getDefault(), "%.2f", c.totalAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = GrassGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPay,
                colors = ButtonDefaults.buttonColors(containerColor = GrassGreen)
            ) {
                Text("💰 Record Payment to Farmer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun RecordFarmerPaymentDialog(
    farmer: FarmerEntity,
    onDismiss: () -> Unit,
    onConfirmPayment: (amount: Double, mode: String, ref: String) -> Unit
) {
    var amountText by remember { mutableStateOf(String.format(Locale.getDefault(), "%.2f", farmer.balancePayable)) }
    var paymentMode by remember { mutableStateOf(farmer.paymentMode) }
    var refText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("💰 Pay Farmer Payout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Farmer: ${farmer.name} (Code #${farmer.farmerCode})", fontWeight = FontWeight.Bold)
                Text("Pending Balance Due: ₹${String.format(Locale.getDefault(), "%.2f", farmer.balancePayable)}", color = DangerRed)

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Text("Payment Method:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("UPI", "CASH", "BANK_TRANSFER").forEach { m ->
                        val isSel = paymentMode == m
                        FilterChip(
                            selected = isSel,
                            onClick = { paymentMode = m },
                            label = { Text(m.replace("_", " "), fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = refText,
                    onValueChange = { refText = it },
                    label = { Text("Transaction Reference / Note") },
                    placeholder = { Text("e.g. UPI Ref #827394") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt <= 0) return@Button
                    onConfirmPayment(amt, paymentMode, refText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GrassGreen)
            ) {
                Text("Confirm Payout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Voice Intake Dialog for 1-Tap speech milk entry (parsing Liters, Fat %, SNF %)
 */
@Composable
fun VoiceIntakeDialog(
    onDismiss: () -> Unit,
    onVoiceParsed: (qty: Double, fat: Double, snf: Double) -> Unit
) {
    var spokenText by remember { mutableStateOf("") }

    val parsedQty = remember(spokenText) {
        val match = Regex("""(\d+(\.\d+)?)\s*(liters|liter|l|ltrs|ltr)?""", RegexOption.IGNORE_CASE).find(spokenText)
            ?: Regex("""(\d+(\.\d+)?)""").find(spokenText)
        match?.groupValues?.get(1)?.toDoubleOrNull() ?: 5.0
    }
    val parsedFat = remember(spokenText) {
        val match = Regex("""fat\s*(\d+(\.\d+)?)""", RegexOption.IGNORE_CASE).find(spokenText)
            ?: Regex("""(\d+\.\d+)""").find(spokenText)
        match?.groupValues?.get(1)?.toDoubleOrNull() ?: 4.2
    }
    val parsedSnf = remember(spokenText) {
        val match = Regex("""snf\s*(\d+(\.\d+)?)""", RegexOption.IGNORE_CASE).find(spokenText)
        match?.groupValues?.get(1)?.toDoubleOrNull() ?: 8.5
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Mic, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(32.dp)) },
        title = { Text("🎤 1-Tap Voice Milk Intake", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Speak or tap sample commands like:\n\"10 Liters Fat 4.5 SNF 8.8\"",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = spokenText,
                    onValueChange = { spokenText = it },
                    placeholder = { Text("e.g. 12.5 L fat 4.2 snf 8.6") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("10 L Fat 4.5 SNF 8.8", "5.5 L Fat 4.2 SNF 8.5", "15 L Fat 6.5 SNF 9.0").forEach { preset ->
                        AssistChip(
                            onClick = { spokenText = preset },
                            label = { Text(preset, fontSize = 9.5.sp) }
                        )
                    }
                }

                Surface(
                    color = DairyGreenLight,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("QUANTITY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Text("${parsedQty} L", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBluePrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("FAT %", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Text("${parsedFat}%", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = DairyGreen)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SNF %", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Text("${parsedSnf}%", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = OceanMidnight)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onVoiceParsed(parsedQty, parsedFat, parsedSnf)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
            ) {
                Text("Apply Voice Values")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
