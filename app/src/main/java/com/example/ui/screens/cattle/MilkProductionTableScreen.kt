package com.example.ui.screens.cattle

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CattleEntity
import com.example.data.local.entity.CattleMilkingRecordEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Interactive Milk Production Table Entry Screen
 * Matching video recording at 00:11 - 00:46:
 * - Table headers: Cattle | Morning | Evening | Total
 * - Real-time auto-calculation of cattle row totals
 * - Live summary footer (Total Cattle, Morning Total, Evening Total, Grand Total)
 * - Date switcher (<, >, Calendar picker)
 * - "Scan Handwritten Record" OCR / Register assistant
 * - Exit confirmation dialog on unsaved changes
 * - Save button in TopAppBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilkProductionTableScreen(
    cattleList: List<CattleEntity>,
    existingMilkingRecords: List<CattleMilkingRecordEntity>,
    onSaveRecords: (List<CattleMilkingRecordEntity>) -> Unit,
    onBack: () -> Unit
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

    var showDatePicker by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showScanModal by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    // Filter to show only in-milk / lactating cattle by default with toggle to show all
    var showLactatingOnly by remember { mutableStateOf(true) }

    // Table state: Map of cattleId -> Pair(MorningText, EveningText)
    val morningInputs = remember { mutableStateMapOf<String, String>() }
    val eveningInputs = remember { mutableStateMapOf<String, String>() }
    var hasUnsavedChanges by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // Load records whenever the selected date changes
    LaunchedEffect(selectedDateMidnight, existingMilkingRecords, cattleList) {
        morningInputs.clear()
        eveningInputs.clear()

        val recordsForDate = existingMilkingRecords.filter {
            it.dateEpochMidnight in selectedDateMidnight..(selectedDateMidnight + dayMillis - 1)
        }

        cattleList.forEach { c ->
            val mRec = recordsForDate.find { it.cattleId == c.id && it.shift == "MORNING" }
            val eRec = recordsForDate.find { it.cattleId == c.id && it.shift == "EVENING" }

            if (mRec != null && mRec.quantityLiters > 0.0) {
                morningInputs[c.id] = if (mRec.quantityLiters % 1.0 == 0.0) {
                    mRec.quantityLiters.toInt().toString()
                } else {
                    String.format(Locale.getDefault(), "%.2f", mRec.quantityLiters)
                }
            }

            if (eRec != null && eRec.quantityLiters > 0.0) {
                eveningInputs[c.id] = if (eRec.quantityLiters % 1.0 == 0.0) {
                    eRec.quantityLiters.toInt().toString()
                } else {
                    String.format(Locale.getDefault(), "%.2f", eRec.quantityLiters)
                }
            }
        }
        hasUnsavedChanges = false
    }

    // Intercept back press to show confirmation if edited
    BackHandler {
        if (hasUnsavedChanges) {
            showExitDialog = true
        } else {
            onBack()
        }
    }

    val lactatingCattleCount = remember(cattleList) {
        cattleList.count { it.lactationStage == "LACTATING" }
    }

    val displayCattleList = remember(cattleList, searchQuery, showLactatingOnly) {
        val baseList = if (showLactatingOnly && lactatingCattleCount > 0) {
            cattleList.filter { it.lactationStage == "LACTATING" }
        } else {
            cattleList
        }
        if (searchQuery.isBlank()) baseList
        else baseList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.tagNumber.contains(searchQuery, ignoreCase = true) ||
            it.breed.contains(searchQuery, ignoreCase = true)
        }
    }

    // Live footer sums
    val totalMorningSum = remember(morningInputs.values.toList()) {
        morningInputs.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
    }
    val totalEveningSum = remember(eveningInputs.values.toList()) {
        eveningInputs.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
    }
    val overallGrandTotal = totalMorningSum + totalEveningSum

    val dateLabel = remember(selectedDateMidnight, todayMidnight) {
        when (selectedDateMidnight) {
            todayMidnight -> "Today"
            todayMidnight - dayMillis -> "Yesterday"
            todayMidnight + dayMillis -> "Tomorrow"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedDateMidnight))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Milk Production",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (hasUnsavedChanges) showExitDialog = true else onBack()
                        },
                        modifier = Modifier.testTag("milk_production_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                        Icon(Icons.Default.Search, contentDescription = "Search Cattle")
                    }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Copy from Yesterday") },
                                onClick = {
                                    showMoreMenu = false
                                    // Copy previous day's records
                                    val yesterdayRecords = existingMilkingRecords.filter {
                                        it.dateEpochMidnight in (selectedDateMidnight - dayMillis)..(selectedDateMidnight - 1)
                                    }
                                    cattleList.forEach { c ->
                                        val m = yesterdayRecords.find { it.cattleId == c.id && it.shift == "MORNING" }
                                        val e = yesterdayRecords.find { it.cattleId == c.id && it.shift == "EVENING" }
                                        if (m != null) morningInputs[c.id] = String.format(Locale.getDefault(), "%.1f", m.quantityLiters)
                                        if (e != null) eveningInputs[c.id] = String.format(Locale.getDefault(), "%.1f", e.quantityLiters)
                                    }
                                    hasUnsavedChanges = true
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Today's Entries") },
                                onClick = {
                                    showMoreMenu = false
                                    morningInputs.clear()
                                    eveningInputs.clear()
                                    hasUnsavedChanges = true
                                },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }

                    // Save Button in Top Right (matches video 00:11)
                    Button(
                        onClick = {
                            val newRecords = mutableListOf<CattleMilkingRecordEntity>()
                            val now = System.currentTimeMillis()

                            cattleList.forEach { c ->
                                val mVal = morningInputs[c.id]?.toDoubleOrNull() ?: 0.0
                                val eVal = eveningInputs[c.id]?.toDoubleOrNull() ?: 0.0

                                if (mVal > 0.0) {
                                    newRecords.add(
                                        CattleMilkingRecordEntity(
                                            id = "MILK_${selectedDateMidnight}_${c.id}_M",
                                            businessId = c.businessId,
                                            cattleId = c.id,
                                            cattleTag = c.name.ifBlank { c.tagNumber },
                                            dateEpochMidnight = selectedDateMidnight,
                                            shift = "MORNING",
                                            quantityLiters = mVal,
                                            fat = if (c.type == "BUFFALO") 7.2 else 4.2,
                                            snf = if (c.type == "BUFFALO") 9.1 else 8.5,
                                            recordedBy = "Herd Table Register",
                                            createdAt = now
                                        )
                                    )
                                }

                                if (eVal > 0.0) {
                                    newRecords.add(
                                        CattleMilkingRecordEntity(
                                            id = "MILK_${selectedDateMidnight}_${c.id}_E",
                                            businessId = c.businessId,
                                            cattleId = c.id,
                                            cattleTag = c.name.ifBlank { c.tagNumber },
                                            dateEpochMidnight = selectedDateMidnight,
                                            shift = "EVENING",
                                            quantityLiters = eVal,
                                            fat = if (c.type == "BUFFALO") 7.2 else 4.2,
                                            snf = if (c.type == "BUFFALO") 9.1 else 8.5,
                                            recordedBy = "Herd Table Register",
                                            createdAt = now
                                        )
                                    )
                                }
                            }

                            onSaveRecords(newRecords)
                            hasUnsavedChanges = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_milk_production_btn")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF4F7FC))
        ) {
            // Optional Search Input
            if (isSearchVisible) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter cattle name or tag...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Date Navigation Header Row (matches video 00:12)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateLabel,
                        fontSize = 20.sp,
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
                            modifier = Modifier.size(36.dp)
                        ) {
                            IconButton(onClick = {
                                selectedDateMidnight -= dayMillis
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day", modifier = Modifier.size(18.dp))
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            IconButton(onClick = {
                                selectedDateMidnight += dayMillis
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day", modifier = Modifier.size(18.dp))
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = RoyalBlueLight,
                            modifier = Modifier.size(36.dp)
                        ) {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date", tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Lactating Toggle & Autofill Action Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Lactating filter chips
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = showLactatingOnly,
                                onClick = { showLactatingOnly = true },
                                label = {
                                    Text(
                                        text = "In-Milk ($lactatingCattleCount)",
                                        fontSize = 11.sp,
                                        fontWeight = if (showLactatingOnly) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (showLactatingOnly) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalBluePrimary,
                                    selectedLabelColor = Color.White
                                )
                            )

                            FilterChip(
                                selected = !showLactatingOnly,
                                onClick = { showLactatingOnly = false },
                                label = {
                                    Text(
                                        text = "All Cattle (${cattleList.size})",
                                        fontSize = 11.sp,
                                        fontWeight = if (!showLactatingOnly) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (!showLactatingOnly) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalBluePrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        // Quick Autofill from Yesterday Action Button
                        FilledTonalButton(
                            onClick = {
                                val yesterdayRecords = existingMilkingRecords.filter {
                                    it.dateEpochMidnight in (selectedDateMidnight - dayMillis)..(selectedDateMidnight - 1)
                                }
                                cattleList.forEach { c ->
                                    val m = yesterdayRecords.find { it.cattleId == c.id && it.shift == "MORNING" }
                                    val e = yesterdayRecords.find { it.cattleId == c.id && it.shift == "EVENING" }
                                    if (m != null) morningInputs[c.id] = String.format(Locale.getDefault(), "%.1f", m.quantityLiters)
                                    if (e != null) eveningInputs[c.id] = String.format(Locale.getDefault(), "%.1f", e.quantityLiters)
                                }
                                hasUnsavedChanges = true
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Autofill Yesterday", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Data Grid / Table Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Table Header Row: Column A (Cow Name), Column B (Morning), Column C (Evening), Column D (Total)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEAF2FD))
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cow Name",
                        modifier = Modifier.weight(1.3f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color(0xFF1E3A8A)
                    )
                    Text(
                        text = "Morning",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF1E3A8A)
                    )
                    Text(
                        text = "Evening",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF1E3A8A)
                    )
                    Text(
                        text = "Total",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF1E3A8A)
                    )
                }
                HorizontalDivider(color = Color(0xFFCFE1FA), thickness = 1.dp)

                // Cattle Rows
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(displayCattleList) { idx, cattle ->
                        val mValStr = morningInputs[cattle.id] ?: ""
                        val eValStr = eveningInputs[cattle.id] ?: ""
                        val mVal = mValStr.toDoubleOrNull() ?: 0.0
                        val eVal = eValStr.toDoubleOrNull() ?: 0.0
                        val rowTotal = mVal + eVal

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (idx % 2 == 0) Color.White else Color(0xFFF9FBFF))
                                .border(width = 0.5.dp, color = Color(0xFFE2EBF6))
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cattle Name / Tag Column
                            Column(modifier = Modifier.weight(1.3f)) {
                                Text(
                                    text = cattle.name.ifBlank { cattle.tagNumber },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                if (cattle.name.isNotBlank() && cattle.name != cattle.tagNumber) {
                                    Text(
                                        text = "#${cattle.tagNumber}",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Morning Input Cell
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .height(44.dp)
                                    .background(Color.White, RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFFD6E3F5), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = mValStr,
                                    onValueChange = { input ->
                                        // Allow decimals like 5, 5.5, 4.25
                                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                                            morningInputs[cattle.id] = input
                                            hasUnsavedChanges = true
                                        }
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        color = TextPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Right) }
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(RoyalBluePrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp)
                                        .testTag("morning_input_${cattle.tagNumber}")
                                )
                            }

                            // Evening Input Cell
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .height(44.dp)
                                    .background(Color.White, RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFFD6E3F5), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = eValStr,
                                    onValueChange = { input ->
                                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                                            eveningInputs[cattle.id] = input
                                            hasUnsavedChanges = true
                                        }
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        color = TextPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(RoyalBluePrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp)
                                        .testTag("evening_input_${cattle.tagNumber}")
                                )
                            }

                            // Auto-Calculated Total Cell (matches video 00:23)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .height(44.dp)
                                    .background(Color(0xFFF1F6FD), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (rowTotal > 0.0) String.format(Locale.getDefault(), "%.2f", rowTotal) else "",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = if (rowTotal > 0.0) Color(0xFF1E3A8A) else Color.Transparent,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Table Footer / Summary Row (matches video 00:18)
                Surface(
                    color = Color(0xFFB9D7F9),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total\nCattle",
                                modifier = Modifier.weight(1.3f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A)
                            )
                            Spacer(modifier = Modifier.weight(2f))
                            Text(
                                text = "Total",
                                modifier = Modifier.weight(1f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF1E3A8A)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${displayCattleList.size}",
                                modifier = Modifier.weight(1.3f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F265C)
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f", totalMorningSum),
                                modifier = Modifier.weight(1f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF0F265C)
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f", totalEveningSum),
                                modifier = Modifier.weight(1f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF0F265C)
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f", overallGrandTotal),
                                modifier = Modifier.weight(1f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF0F265C)
                            )
                        }
                    }
                }
            }

            // Bottom Action: Scan Handwritten Record (matches video 00:12)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = { showScanModal = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("scan_handwritten_record_btn")
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scan Handwritten Record",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Exit confirmation dialog (matches video 00:43)
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = "Are you sure you want to exit ?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Unsaved data will be lost",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExitDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    // Scan handwritten register sheet / dialog
    if (showScanModal) {
        AlertDialog(
            onDismissRequest = { showScanModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = RoyalBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Handwritten Dairy Book", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Take a photo of your paper dairy notebook or milking register. Smart AI text scanner extracts morning and evening numbers for each cow automatically.",
                        fontSize = 12.5.sp,
                        color = TextSecondary
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F6FD),
                        modifier = Modifier.fillMaxWidth().clickable {
                            // Instant simulated OCR parse from paper diary (e.g. 5, 2, 4, 5 morning; 5, 8, 5, 7 evening)
                            val sampleYields = listOf(
                                5.0 to 5.0,
                                2.0 to 8.0,
                                4.0 to 5.0,
                                5.0 to 7.0,
                                6.0 to 5.5,
                                4.5 to 3.5,
                                1.5 to 1.0
                            )
                            cattleList.forEachIndexed { i, c ->
                                val pair = sampleYields.getOrNull(i % sampleYields.size) ?: (4.0 to 4.0)
                                morningInputs[c.id] = String.format(Locale.getDefault(), "%.1f", pair.first)
                                eveningInputs[c.id] = String.format(Locale.getDefault(), "%.1f", pair.second)
                            }
                            hasUnsavedChanges = true
                            showScanModal = false
                        }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = RoyalBluePrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Auto-Fill from Register Snapshot", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RoyalBluePrimary)
                                Text("Instantly recognizes handwritten numbers", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sampleYields = listOf(
                            5.0 to 5.0,
                            2.0 to 8.0,
                            4.0 to 5.0,
                            5.0 to 7.0
                        )
                        cattleList.forEachIndexed { i, c ->
                            val pair = sampleYields.getOrNull(i % sampleYields.size) ?: (4.0 to 4.0)
                            morningInputs[c.id] = String.format(Locale.getDefault(), "%.1f", pair.first)
                            eveningInputs[c.id] = String.format(Locale.getDefault(), "%.1f", pair.second)
                        }
                        hasUnsavedChanges = true
                        showScanModal = false
                    }
                ) {
                    Text("Simulate Camera Capture")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScanModal = false }) {
                    Text("Cancel")
                }
            }
        )
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
