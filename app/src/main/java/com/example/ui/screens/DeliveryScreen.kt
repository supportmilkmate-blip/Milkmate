package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.CustomerLeaveEntity
import com.example.data.local.entity.DeliveryEntity
import com.example.data.local.entity.getQuickPresetsList
import com.example.data.repository.PricingEngine
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures
import com.example.ui.util.loc
import com.example.ui.util.appStr
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryScreen(viewModel: MilkMateViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedShift by viewModel.selectedShift.collectAsStateWithLifecycle()
    val deliveries by viewModel.currentShiftDeliveries.collectAsStateWithLifecycle()
    val todayDeliveries by viewModel.todayDeliveries.collectAsStateWithLifecycle()
    val allCustomers by viewModel.customers.collectAsStateWithLifecycle()

    val currentMode = business?.businessMode ?: "FARMER"
    val showRetail = BusinessModeFeatures.showDeliveryRoutes(business) && currentMode !in listOf("FARMER_WHOLESALE", "COLLECTION_CENTER")
    val defaultCat = when {
        currentMode == "FARMER_WHOLESALE" -> "SUPPLIER"
        currentMode == "COLLECTION_CENTER" -> "BULK_BUYER"
        currentMode == "TRADER" -> "BULK_BUYER"
        showRetail -> "INDIVIDUAL"
        else -> "BULK_BUYER"
    }

    // Screen State
    var screenMode by remember { mutableStateOf("LIST") } // "LIST", "STEP_BY_STEP"
    var activeViewMode by remember { mutableStateOf("ROUTE") } // "ROUTE" (Customer Cards), "TIMELINE" (Chronological Timeline)
    var activeCategory by remember(currentMode) { mutableStateOf(defaultCat) } // "INDIVIDUAL", "BULK_BUYER", "SUPPLIER"
    var selectedCustomerForStep by remember { mutableStateOf<CustomerEntity?>(null) }
    var currentCustomerIndex by remember { mutableIntStateOf(0) }
    var paymentCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showBulkDeliverConfirmDialog by remember { mutableStateOf(false) }
    var deliveryToEdit by remember { mutableStateOf<DeliveryEntity?>(null) }
    var customerForLeaveDialog by remember { mutableStateOf<CustomerEntity?>(null) }
    var showGlobalLeaveDialog by remember { mutableStateOf(false) }

    val accountStartDate = business?.accountStartDate ?: 0L
    val defaultCowRate = business?.cowMilkRate ?: 50.0
    val defaultBuffRate = business?.buffaloMilkRate ?: 65.0
    val staffList by viewModel.staffList.collectAsStateWithLifecycle()

    var showAddPartyDialog by remember { mutableStateOf(false) }
    var showImportContactsDialog by remember { mutableStateOf(false) }
    var showRouteSequenceDialog by remember { mutableStateOf(false) }
    var partyTypeToAdd by remember { mutableStateOf("INDIVIDUAL") }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val fullDateDisplay = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date(selectedDate))
    val shortDateDisplay = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(selectedDate))

    // Filter customers for active category, strictly sorted by Route Sequence (Stop #1, #2, #3...)
    val relevantCustomers = remember(allCustomers, activeCategory) {
        val list = if (activeCategory == "SUPPLIER") {
            allCustomers.filter { it.type == "SUPPLIER" }
        } else {
            allCustomers.filter { it.type == activeCategory }
        }
        list.sortedWith(
            compareBy(
                { if (it.sequenceOrder > 0) it.sequenceOrder else Int.MAX_VALUE },
                { it.name.lowercase() }
            )
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (screenMode == "LIST" && activeViewMode == "ROUTE") {
                ExtendedFloatingActionButton(
                    onClick = {
                        partyTypeToAdd = activeCategory
                        showAddPartyDialog = true
                    },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White) },
                    text = {
                        Text(
                            text = when (activeCategory) {
                                "BULK_BUYER" -> "+ Add B2B Buyer"
                                "SUPPLIER" -> "+ Add Center"
                                else -> "+ Add Customer"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    containerColor = GrassGreen,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_delivery_customer")
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (screenMode) {
                "LIST" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        // Top View Mode Switcher: Route Overview vs Chronological Timeline Feed vs Volume Analytics
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (activeViewMode == "ROUTE") RoyalBluePrimary else Color.Transparent,
                                    modifier = Modifier.weight(1f).clickable { activeViewMode = "ROUTE" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Map,
                                            contentDescription = null,
                                            tint = if (activeViewMode == "ROUTE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Route",
                                            fontSize = 11.sp,
                                            fontWeight = if (activeViewMode == "ROUTE") FontWeight.Bold else FontWeight.Medium,
                                            color = if (activeViewMode == "ROUTE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (activeViewMode == "PACKING") Color(0xFFD97706) else Color.Transparent,
                                    modifier = Modifier.weight(1.2f).clickable { activeViewMode = "PACKING" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = if (activeViewMode == "PACKING") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Packing",
                                            fontSize = 11.sp,
                                            fontWeight = if (activeViewMode == "PACKING") FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (activeViewMode == "PACKING") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (activeViewMode == "TIMELINE") FreshGold else Color.Transparent,
                                    modifier = Modifier.weight(1.1f).clickable { activeViewMode = "TIMELINE" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = null,
                                            tint = if (activeViewMode == "TIMELINE") Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Log (${todayDeliveries.size})",
                                            fontSize = 11.sp,
                                            fontWeight = if (activeViewMode == "TIMELINE") FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (activeViewMode == "TIMELINE") Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (activeViewMode == "ANALYTICS") DairyGreen else Color.Transparent,
                                    modifier = Modifier.weight(1.1f).clickable { activeViewMode = "ANALYTICS" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.PieChart,
                                            contentDescription = null,
                                            tint = if (activeViewMode == "ANALYTICS") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Analytics",
                                            fontSize = 11.sp,
                                            fontWeight = if (activeViewMode == "ANALYTICS") FontWeight.Bold else FontWeight.Medium,
                                            color = if (activeViewMode == "ANALYTICS") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        when (activeViewMode) {
                            "ROUTE" -> {
                                DeliveryDashboardContent(
                                    viewModel = viewModel,
                                    activeCategory = activeCategory,
                                    onCategoryChange = { activeCategory = it },
                                    selectedDate = selectedDate,
                                    selectedShift = selectedShift,
                                    fullDateDisplay = fullDateDisplay,
                                    shortDateDisplay = shortDateDisplay,
                                    accountStartDate = accountStartDate,
                                    customers = relevantCustomers,
                                    allCustomers = allCustomers,
                                    deliveries = deliveries,
                                    defaultCowRate = defaultCowRate,
                                    defaultBuffRate = defaultBuffRate,
                                    onSelectDate = { viewModel.setDate(it) },
                                    onShiftChange = { viewModel.setShift(it) },
                                    onStartStepByStep = {
                                        if (relevantCustomers.isNotEmpty()) {
                                            val firstPendingIndex = relevantCustomers.indexOfFirst { cust ->
                                                val cowDone = deliveries.any {
                                                    it.customerId == cust.id && it.deliveryDate == selectedDate && it.shift == selectedShift &&
                                                            (it.milkType == "COW" || (cust.milkType == "COW" && it.milkType != "BUFFALO")) && it.isDelivered
                                                }
                                                val buffDone = deliveries.any {
                                                    it.customerId == cust.id && it.deliveryDate == selectedDate && it.shift == selectedShift &&
                                                            (it.milkType == "BUFFALO" || (cust.milkType == "BUFFALO" && it.milkType != "COW")) && it.isDelivered
                                                }
                                                when (cust.milkType) {
                                                    "COW" -> !cowDone
                                                    "BUFFALO" -> !buffDone
                                                    "BOTH" -> !(cowDone && buffDone)
                                                    else -> !(cowDone || buffDone)
                                                }
                                            }
                                            val targetIndex = if (firstPendingIndex >= 0) firstPendingIndex else 0
                                            selectedCustomerForStep = relevantCustomers[targetIndex]
                                            currentCustomerIndex = targetIndex
                                            screenMode = "STEP_BY_STEP"
                                        }
                                    },
                                    onOpenCustomerEntry = { cust, idx ->
                                        val foundIdx = relevantCustomers.indexOfFirst { it.id == cust.id }
                                        val actualIdx = if (foundIdx >= 0) foundIdx else idx
                                        selectedCustomerForStep = cust
                                        currentCustomerIndex = actualIdx
                                        screenMode = "STEP_BY_STEP"
                                    },
                                    onRecordPayment = { cust ->
                                        paymentCustomer = cust
                                    },
                                    onBulkDeliverClick = {
                                        showBulkDeliverConfirmDialog = true
                                    },
                                    onAddNewParty = { type ->
                                        partyTypeToAdd = type
                                        showAddPartyDialog = true
                                    },
                                    onImportContacts = { type ->
                                        partyTypeToAdd = type
                                        showImportContactsDialog = true
                                    },
                                    onReorderRoute = {
                                        showRouteSequenceDialog = true
                                    },
                                    onOpenPackingConfirmation = {
                                        activeViewMode = "PACKING"
                                    },
                                    onInformLeave = { cust ->
                                        customerForLeaveDialog = cust
                                    },
                                    onOpenGlobalLeave = {
                                        showGlobalLeaveDialog = true
                                    }
                                )
                            }
                            "PACKING" -> {
                                PackingConfirmationScreen(
                                    viewModel = viewModel,
                                    onBack = { activeViewMode = "ROUTE" },
                                    onStartDelivery = {
                                        activeViewMode = "ROUTE"
                                        if (relevantCustomers.isNotEmpty()) {
                                            val firstPendingIndex = relevantCustomers.indexOfFirst { cust ->
                                                val cowDone = deliveries.any {
                                                    it.customerId == cust.id && it.deliveryDate == selectedDate && it.shift == selectedShift &&
                                                            (it.milkType == "COW" || (cust.milkType == "COW" && it.milkType != "BUFFALO")) && it.isDelivered
                                                }
                                                val buffDone = deliveries.any {
                                                    it.customerId == cust.id && it.deliveryDate == selectedDate && it.shift == selectedShift &&
                                                            (it.milkType == "BUFFALO" || (cust.milkType == "BUFFALO" && it.milkType != "COW")) && it.isDelivered
                                                }
                                                when (cust.milkType) {
                                                    "COW" -> !cowDone
                                                    "BUFFALO" -> !buffDone
                                                    "BOTH" -> !(cowDone && buffDone)
                                                    else -> !(cowDone || buffDone)
                                                }
                                            }
                                            val targetIndex = if (firstPendingIndex >= 0) firstPendingIndex else 0
                                            selectedCustomerForStep = relevantCustomers[targetIndex]
                                            currentCustomerIndex = targetIndex
                                            screenMode = "STEP_BY_STEP"
                                        }
                                    }
                                )
                            }
                            "TIMELINE" -> {
                                ChronologicalDeliveryTimelineView(
                                    todayDeliveries = todayDeliveries,
                                    selectedDate = selectedDate,
                                    selectedShift = selectedShift,
                                    shortDateDisplay = shortDateDisplay,
                                    onSelectDate = { dateVal: Long -> viewModel.setDate(dateVal) },
                                    onShiftChange = { shiftVal: String -> viewModel.setShift(shiftVal) },
                                    onEditDelivery = { del: DeliveryEntity -> deliveryToEdit = del },
                                    onDeleteDelivery = { del: DeliveryEntity ->
                                        viewModel.deleteDelivery(del.id)
                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Removed delivery for ${del.customerName}",
                                                actionLabel = "UNDO",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.restoreDelivery(del)
                                            }
                                        }
                                    }
                                )
                            }
                            "ANALYTICS" -> {
                                DailyMilkOutputAnalyticsView(
                                    todayDeliveries = todayDeliveries,
                                    selectedDate = selectedDate,
                                    selectedShift = selectedShift,
                                    shortDateDisplay = shortDateDisplay,
                                    fullDateDisplay = fullDateDisplay,
                                    accountStartDate = accountStartDate,
                                    businessName = business?.businessName,
                                    onSelectDate = { dateVal: Long -> viewModel.setDate(dateVal) }
                                )
                            }
                        }
                    }
                }

                "STEP_BY_STEP" -> {
                    val customer = selectedCustomerForStep
                    if (customer != null) {
                        val existingCowDelivery = deliveries.find {
                            it.customerId == customer.id && it.deliveryDate == selectedDate && it.shift == selectedShift &&
                                    (it.milkType == "COW" || (customer.milkType == "COW" && it.milkType != "BUFFALO"))
                        }
                        val existingBuffDelivery = deliveries.find {
                            it.customerId == customer.id && it.deliveryDate == selectedDate && it.shift == selectedShift &&
                                    (it.milkType == "BUFFALO" || (customer.milkType == "BUFFALO" && it.milkType != "COW"))
                        }

                        ModernStepDeliveryScreen(
                            customer = customer,
                            currentIndex = currentCustomerIndex + 1,
                            totalCount = relevantCustomers.size,
                            existingCowDelivery = existingCowDelivery,
                            existingBuffDelivery = existingBuffDelivery,
                            selectedDate = selectedDate,
                            selectedShift = selectedShift,
                            defaultCowRate = if (customer.cowRate > 0) customer.cowRate else (if (customer.rate > 0) customer.rate else defaultCowRate),
                            defaultBuffRate = if (customer.buffaloRate > 0) customer.buffaloRate else (if (customer.rate > 0) customer.rate else defaultBuffRate),
                            supportedMilkTypes = business?.supportedMilkTypes ?: "BOTH",
                            todayLeave = viewModel.isCustomerOnLeaveToday(customer, selectedDate, selectedShift),
                            onInformLeave = { customerForLeaveDialog = customer },
                            onBack = { screenMode = "LIST" },
                            onPrevious = {
                                if (currentCustomerIndex > 0) {
                                    currentCustomerIndex--
                                    selectedCustomerForStep = relevantCustomers[currentCustomerIndex]
                                }
                            },
                            onSkipToNext = {
                                if (currentCustomerIndex + 1 < relevantCustomers.size) {
                                    currentCustomerIndex++
                                    selectedCustomerForStep = relevantCustomers[currentCustomerIndex]
                                } else {
                                    screenMode = "LIST"
                                }
                            },
                            onDeleteDelivery = { deliveryId ->
                                viewModel.deleteDelivery(deliveryId)
                            },
                            onUpdatePresets = { newPresets ->
                                viewModel.updateCustomerQuickPresets(customer.id, newPresets)
                            },
                            onSaveDual = { includeCow, cowQty, cowRate, cowFat, cowSnf, cowClr, cowSkip,
                                          includeBuff, buffQty, buffRate, buffFat, buffSnf, buffClr, buffSkip ->
                                if (includeCow) {
                                    viewModel.saveDelivery(
                                        id = existingCowDelivery?.id,
                                        customerId = customer.id,
                                        customerName = customer.name,
                                        customerType = customer.type,
                                        date = selectedDate,
                                        shift = selectedShift,
                                        milkType = "COW",
                                        quantity = if (cowSkip) 0.0 else cowQty,
                                        fat = cowFat,
                                        snf = cowSnf,
                                        clr = cowClr,
                                        customRate = cowRate,
                                        isDelivered = !cowSkip,
                                        notes = if (cowSkip) "Skipped / Holiday" else "Delivered"
                                    )
                                } else if (existingCowDelivery != null) {
                                    viewModel.deleteDelivery(existingCowDelivery.id)
                                }

                                if (includeBuff) {
                                    viewModel.saveDelivery(
                                        id = existingBuffDelivery?.id,
                                        customerId = customer.id,
                                        customerName = customer.name,
                                        customerType = customer.type,
                                        date = selectedDate,
                                        shift = selectedShift,
                                        milkType = "BUFFALO",
                                        quantity = if (buffSkip) 0.0 else buffQty,
                                        fat = buffFat,
                                        snf = buffSnf,
                                        clr = buffClr,
                                        customRate = buffRate,
                                        isDelivered = !buffSkip,
                                        notes = if (buffSkip) "Skipped / Holiday" else "Delivered"
                                    )
                                } else if (existingBuffDelivery != null) {
                                    viewModel.deleteDelivery(existingBuffDelivery.id)
                                }

                                // Advance to next customer
                                if (currentCustomerIndex + 1 < relevantCustomers.size) {
                                    currentCustomerIndex++
                                    selectedCustomerForStep = relevantCustomers[currentCustomerIndex]
                                } else {
                                    screenMode = "LIST"
                                }
                            }
                        )
                    } else {
                        screenMode = "LIST"
                    }
                }
            }
        }
    }

    // Modal: Record Payment
    paymentCustomer?.let { cust ->
        RecordCustomerPaymentDialog(
            customer = cust,
            onDismiss = { paymentCustomer = null },
            onSavePayment = { amount, method, notes ->
                viewModel.savePayment(
                    customerId = cust.id,
                    customerName = cust.name,
                    paymentType = if (cust.type == "SUPPLIER") "SUPPLIER_PAYMENT" else "CUSTOMER_PAYMENT",
                    amount = amount,
                    method = method,
                    referenceNo = "",
                    notes = notes
                )
                paymentCustomer = null
            }
        )
    }

    // Modal: Edit Single Delivery
    deliveryToEdit?.let { del: DeliveryEntity ->
        EditSingleDeliveryDialog(
            delivery = del,
            onDismiss = { deliveryToEdit = null },
            onSave = { newQty: Double, newRate: Double, newFat: Double, newSnf: Double, newClr: Double, newNotes: String ->
                viewModel.saveDelivery(
                    id = del.id,
                    customerId = del.customerId,
                    customerName = del.customerName,
                    customerType = del.customerType,
                    date = del.deliveryDate,
                    shift = del.shift,
                    milkType = del.milkType,
                    quantity = newQty,
                    fat = newFat,
                    snf = newSnf,
                    clr = newClr,
                    customRate = newRate,
                    isDelivered = true,
                    notes = newNotes
                )
                deliveryToEdit = null
            },
            onDelete = {
                viewModel.deleteDelivery(del.id)
                deliveryToEdit = null
            }
        )
    }

    // Modal: Bulk Deliver All Pending Confirmation
    if (showBulkDeliverConfirmDialog) {
        val pendingCount = relevantCustomers.count { cust ->
            !deliveries.any { it.customerId == cust.id && it.deliveryDate == selectedDate && it.shift == selectedShift && it.isDelivered }
        }
        AlertDialog(
            onDismissRequest = { showBulkDeliverConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = FreshGold)
                    Text("Deliver All Pending?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Text(
                    "This will mark all remaining $pendingCount pending customer(s) as Delivered using their default daily milk quantity and rate.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.bulkDeliverAllPending(selectedDate, selectedShift, relevantCustomers, deliveries)
                        showBulkDeliverConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen)
                ) {
                    Text("Yes, Deliver All ($pendingCount)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeliverConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Add / Edit Party directly from Delivery Screen
    if (showAddPartyDialog) {
        ManualPartyDialog(
            customer = null,
            defaultType = partyTypeToAdd,
            staffList = staffList.map { it.name },
            supportedMilkTypes = business?.supportedMilkTypes ?: "BOTH",
            businessMode = currentMode,
            onDismiss = { showAddPartyDialog = false },
            onSave = { name, mobile, address, type, rate, milkType, defaultQty, defaultShift, route, notes, cowQty, cowRate, buffQty, buffRate, quickPresets, rateMethod, sequenceOrder ->
                viewModel.saveCustomer(
                    id = null,
                    name = name,
                    mobile = mobile,
                    address = address,
                    type = type,
                    rate = rate,
                    milkType = milkType,
                    defaultQty = defaultQty,
                    defaultShift = defaultShift,
                    route = route,
                    notes = notes,
                    cowQuantity = cowQty,
                    cowRate = cowRate,
                    buffaloQuantity = buffQty,
                    buffaloRate = buffRate,
                    quickPresets = quickPresets,
                    rateMethod = rateMethod,
                    sequenceOrder = sequenceOrder
                ) { success, err ->
                    if (success) {
                        showAddPartyDialog = false
                        activeCategory = type
                        viewModel.showMessage("✓ Added $name successfully!")
                    } else {
                        viewModel.showMessage(err ?: "Cannot save party")
                    }
                }
            }
        )
    }

    // Modal: Import Contacts directly from Delivery Screen
    if (showImportContactsDialog) {
        ImportContactsDialog(
            defaultType = partyTypeToAdd,
            supportedMilkTypes = business?.supportedMilkTypes ?: "BOTH",
            businessMode = currentMode,
            onDismiss = { showImportContactsDialog = false },
            onImport = { name, phone, type, milkType, qty, rate, route, rateMethod ->
                viewModel.saveCustomer(
                    id = null,
                    name = name,
                    mobile = phone,
                    address = "",
                    type = type,
                    rate = rate,
                    milkType = milkType,
                    defaultQty = qty,
                    defaultShift = "MORNING",
                    route = route,
                    notes = "Imported from Contacts",
                    rateMethod = rateMethod
                ) { success, _ ->
                    if (success) {
                        showImportContactsDialog = false
                        activeCategory = type
                        viewModel.showMessage("✓ Imported $name successfully!")
                    }
                }
            }
        )
    }

    // Modal: Manage Route Stops Order & Sequence
    if (showRouteSequenceDialog) {
        RouteSequenceManagerDialog(
            route = "Current Delivery Route",
            customers = relevantCustomers,
            onDismiss = { showRouteSequenceDialog = false },
            onSaveSequence = { orderedIds ->
                viewModel.updateRouteSequence(orderedIds) {
                    showRouteSequenceDialog = false
                }
            }
        )
    }

    if (customerForLeaveDialog != null || showGlobalLeaveDialog) {
        CustomerLeaveDialog(
            viewModel = viewModel,
            preSelectedCustomer = customerForLeaveDialog,
            onDismiss = {
                customerForLeaveDialog = null
                showGlobalLeaveDialog = false
            },
            onGoToPackingConfirmation = {
                customerForLeaveDialog = null
                showGlobalLeaveDialog = false
                activeViewMode = "PACKING"
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDashboardContent(
    viewModel: MilkMateViewModel,
    activeCategory: String,
    onCategoryChange: (String) -> Unit,
    selectedDate: Long,
    selectedShift: String,
    fullDateDisplay: String,
    shortDateDisplay: String,
    accountStartDate: Long,
    customers: List<CustomerEntity>,
    allCustomers: List<CustomerEntity>,
    deliveries: List<DeliveryEntity>,
    defaultCowRate: Double,
    defaultBuffRate: Double,
    onSelectDate: (Long) -> Unit,
    onShiftChange: (String) -> Unit,
    onStartStepByStep: () -> Unit,
    onOpenCustomerEntry: (CustomerEntity, Int) -> Unit,
    onRecordPayment: (CustomerEntity) -> Unit,
    onBulkDeliverClick: () -> Unit,
    onAddNewParty: (String) -> Unit,
    onImportContacts: (String) -> Unit,
    onReorderRoute: () -> Unit,
    onOpenPackingConfirmation: () -> Unit = {},
    onInformLeave: (CustomerEntity) -> Unit = {},
    onOpenGlobalLeave: () -> Unit = {}
) {
    val context = LocalContext.current
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val allLeaves by viewModel.customerLeaves.collectAsStateWithLifecycle()
    val allOrders by viewModel.orders.collectAsStateWithLifecycle()

    val todayLeaves = remember(allLeaves, selectedDate, selectedShift) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetDate = cal.timeInMillis
        allLeaves.filter { l ->
            l.status == "ACTIVE" &&
                    targetDate >= l.startDate &&
                    targetDate <= l.endDate &&
                    (l.shift == "BOTH" || l.shift.equals(selectedShift, ignoreCase = true))
        }
    }
    val leaveDeductedLiters = todayLeaves.sumOf { it.dailyQuantitySaved }

    var searchQuery by remember { mutableStateOf("") }
    var selectedRouteFilter by remember { mutableStateOf("All Routes") }
    var selectedMilkTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "COW", "BUFFALO", "BOTH"
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // "ALL", "PENDING", "COMPLETED"

    val availableRoutes = remember(customers) {
        listOf("All Routes") + customers.mapNotNull { it.route.trim().ifBlank { null } }.distinct()
    }

    // Calculations
    val deliveredCount = customers.count { c ->
        deliveries.any { d -> d.customerId == c.id && d.deliveryDate == selectedDate && d.shift == selectedShift && d.isDelivered }
    }
    val pendingCount = customers.size - deliveredCount
    val completionPercent = if (customers.isNotEmpty()) (deliveredCount * 100) / customers.size else 0

    val shiftDeliveries = deliveries.filter { it.deliveryDate == selectedDate && it.shift == selectedShift && it.isDelivered }
    val totalLiters = shiftDeliveries.sumOf { it.quantityLiters }
    val cowLiters = shiftDeliveries.filter { it.milkType == "COW" }.sumOf { it.quantityLiters }
    val buffLiters = shiftDeliveries.filter { it.milkType == "BUFFALO" }.sumOf { it.quantityLiters }
    val totalAmount = shiftDeliveries.sumOf { it.totalAmount }

    val filteredCustomers = customers.filter { customer ->
        val matchesSearch = searchQuery.isBlank() ||
                customer.name.contains(searchQuery, ignoreCase = true) ||
                customer.mobile.contains(searchQuery) ||
                customer.route.contains(searchQuery, ignoreCase = true)

        val matchesRoute = selectedRouteFilter == "All Routes" || customer.route.trim().equals(selectedRouteFilter, ignoreCase = true)

        val matchesMilkType = when (selectedMilkTypeFilter) {
            "COW" -> customer.milkType == "COW" || customer.milkType == "BOTH"
            "BUFFALO" -> customer.milkType == "BUFFALO" || customer.milkType == "BOTH"
            "BOTH" -> customer.milkType == "BOTH"
            else -> true
        }

        val isDone = deliveries.any { d -> d.customerId == customer.id && d.deliveryDate == selectedDate && d.shift == selectedShift && d.isDelivered }
        val matchesStatus = when (selectedStatusFilter) {
            "PENDING" -> !isDone
            "COMPLETED" -> isDone
            else -> true
        }

        matchesSearch && matchesRoute && matchesMilkType && matchesStatus
    }.sortedWith(
        compareBy(
            { if (it.sequenceOrder > 0) it.sequenceOrder else Int.MAX_VALUE },
            { it.name.lowercase() }
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ================= HERO SESSION COCKPIT =================
        val primaryColor = MaterialTheme.colorScheme.primary
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF0F172A), primaryColor, primaryColor.copy(alpha = 0.85f))
                    ),
                    shape = RoundedCornerShape(18.dp)
                ),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Shift & Date Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shift Switcher Pill
                    Row(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                            .padding(3.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedShift == "MORNING") FreshGold else Color.Transparent,
                            modifier = Modifier.clickable { onShiftChange("MORNING") }
                        ) {
                            Text(
                                text = "☀️ Morning",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedShift == "MORNING") Color.Black else Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedShift == "EVENING") Color(0xFF6366F1) else Color.Transparent,
                            modifier = Modifier.clickable { onShiftChange("EVENING") }
                        ) {
                            Text(
                                text = "🌙 Evening",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Date Picker Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp))
                            .clickable {
                                val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                                val dp = DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val newCal = Calendar.getInstance().apply { set(y, m, d, 12, 0, 0) }
                                        onSelectDate(newCal.timeInMillis)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                )
                                if (accountStartDate > 0) dp.datePicker.minDate = accountStartDate
                                dp.datePicker.maxDate = System.currentTimeMillis()
                                dp.show()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = shortDateDisplay, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Progress Bar and Liters Breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "$deliveredCount / ${customers.size} Completed",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        val supportedMilkTypes = business?.supportedMilkTypes ?: "BOTH"
                        val milkLitersBreakdown = when (supportedMilkTypes) {
                            "COW_ONLY" -> "🐄 ${String.format(Locale.US, "%.1f", cowLiters)}L Cow Milk"
                            "BUFFALO_ONLY" -> "🐃 ${String.format(Locale.US, "%.1f", buffLiters)}L Buffalo Milk"
                            else -> "🐄 ${String.format(Locale.US, "%.1f", cowLiters)}L Cow  •  🐃 ${String.format(Locale.US, "%.1f", buffLiters)}L Buff"
                        }
                        Text(
                            text = milkLitersBreakdown,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.US, "%.1f L", totalLiters),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = FreshGold
                        )
                        Text(
                            text = String.format(Locale.US, "₹%.0f Bill", totalAmount),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // Progress Indicator
                LinearProgressIndicator(
                    progress = { if (customers.isNotEmpty()) deliveredCount.toFloat() / customers.size else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = FreshGold,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )

                if (customers.isNotEmpty() && deliveredCount == customers.size) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DairyGreen,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🎉 Route Complete! All $deliveredCount deliveries done (100%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Pre-Delivery Packing & Customer Leave Alert Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (todayLeaves.isNotEmpty()) FreshGold else Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenPackingConfirmation() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = if (todayLeaves.isNotEmpty()) FreshGold else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = if (todayLeaves.isNotEmpty()) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "📦 Packing Confirmation & Recheck",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                    if (todayLeaves.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(shape = RoundedCornerShape(4.dp), color = FreshGold) {
                                            Text(
                                                text = "${todayLeaves.size} ON LEAVE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.Black,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (todayLeaves.isNotEmpty())
                                        "Taking ${todayLeaves.size} customer leaves into count (-${String.format(Locale.US, "%.1f", leaveDeductedLiters)}L deducted)"
                                    else
                                        "Verify packing qty & advance orders before departure",
                                    fontSize = 10.5.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = FreshGold, modifier = Modifier.size(18.dp))
                    }
                }

                // Quick Cockpit Actions
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onStartStepByStep,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(38.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (pendingCount > 0) "Deliver Route ($pendingCount left)" else "Review Route",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onBulkDeliverClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Color.White.copy(0.4f), Color.White.copy(0.4f)))),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deliver All", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    IconButton(
                        onClick = {
                            val bizMilkTypes = business?.supportedMilkTypes ?: "BOTH"
                            val msg = StringBuilder().apply {
                                append("🥛 *${business?.businessName ?: "Dairy"} Shift Summary*\n")
                                append("📅 Date: $shortDateDisplay ($selectedShift Shift)\n\n")
                                append("🚚 Completed: $deliveredCount / ${customers.size} (${completionPercent}%)\n")
                                if (bizMilkTypes != "BUFFALO_ONLY") {
                                    append("🐄 Cow Milk: ${String.format(Locale.US, "%.1f", cowLiters)} Liters\n")
                                }
                                if (bizMilkTypes != "COW_ONLY") {
                                    append("🐃 Buffalo Milk: ${String.format(Locale.US, "%.1f", buffLiters)} Liters\n")
                                }
                                append("🥛 Total Volume: ${String.format(Locale.US, "%.1f", totalLiters)} Liters\n")
                                append("💰 Total Shift Bill: ₹${totalAmount.toInt()}\n")
                            }.toString()

                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, msg)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Shift Summary"))
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Summary", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // ================= CATEGORY TABS =================
        val currentMode = business?.businessMode ?: "FARMER"
        val isTrader = currentMode == "TRADER"
        val isWholesaleFarmer = currentMode == "FARMER_WHOLESALE"
        val isCollectionCenter = currentMode == "COLLECTION_CENTER"

        val categoryTabs = buildList {
            when {
                isWholesaleFarmer -> {
                    add(Triple("SUPPLIER", "🏢 Center Supply", allCustomers.count { it.type == "SUPPLIER" }))
                    add(Triple("BULK_BUYER", "🏨 Commercial Buyers", allCustomers.count { it.type == "BULK_BUYER" }))
                }
                isCollectionCenter -> {
                    add(Triple("BULK_BUYER", "🏭 Plant Tankers", allCustomers.count { it.type == "BULK_BUYER" }))
                    add(Triple("SUPPLIER", "🏨 Local Buyers", allCustomers.count { it.type == "SUPPLIER" }))
                }
                isTrader -> {
                    add(Triple("INDIVIDUAL", "🏠 Household Routes", allCustomers.count { it.type == "INDIVIDUAL" }))
                    add(Triple("BULK_BUYER", "🏨 Restaurants & B2B", allCustomers.count { it.type == "BULK_BUYER" }))
                    add(Triple("SUPPLIER", "🏢 Center Supply", allCustomers.count { it.type == "SUPPLIER" }))
                }
                else -> {
                    // Standard Dairy Farmer & Integrated
                    add(Triple("INDIVIDUAL", "🏠 Household Routes", allCustomers.count { it.type == "INDIVIDUAL" }))
                    add(Triple("BULK_BUYER", "🏨 Restaurants & B2B", allCustomers.count { it.type == "BULK_BUYER" }))
                    add(Triple("SUPPLIER", "🏢 Center Supply", allCustomers.count { it.type == "SUPPLIER" }))
                }
            }
        }

        val categoryKeys = categoryTabs.map { it.first }
        LaunchedEffect(categoryKeys, currentMode) {
            if (activeCategory !in categoryKeys && categoryKeys.isNotEmpty()) {
                onCategoryChange(categoryKeys.first())
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categoryTabs.forEach { (catKey, catLabel, catCount) ->
                val isSelected = activeCategory == catKey
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onCategoryChange(catKey) }
                ) {
                    Text(
                        text = "$catLabel ($catCount)",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 7.dp)
                    )
                }
            }
        }

        // ================= MULTI-FACET FILTER & QUICK ADD BAR =================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedStatusFilter == "ALL" && selectedMilkTypeFilter == "ALL",
                    onClick = {
                        selectedStatusFilter = "ALL"
                        selectedMilkTypeFilter = "ALL"
                    },
                    label = { Text("All (${customers.size})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedStatusFilter == "PENDING",
                    onClick = { selectedStatusFilter = if (selectedStatusFilter == "PENDING") "ALL" else "PENDING" },
                    label = { Text("⏳ Pending ($pendingCount)", fontSize = 11.sp, fontWeight = if (selectedStatusFilter == "PENDING") FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FreshGoldLight, selectedLabelColor = FreshGold)
                )
                FilterChip(
                    selected = selectedStatusFilter == "COMPLETED",
                    onClick = { selectedStatusFilter = if (selectedStatusFilter == "COMPLETED") "ALL" else "COMPLETED" },
                    label = { Text("✓ Done ($deliveredCount)", fontSize = 11.sp, fontWeight = if (selectedStatusFilter == "COMPLETED") FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DairyGreenLight, selectedLabelColor = DairyGreen)
                )
                val supportedTypes = business?.supportedMilkTypes ?: "BOTH"
                if (supportedTypes == "COW_ONLY" || supportedTypes == "BOTH") {
                    FilterChip(
                        selected = selectedMilkTypeFilter == "COW",
                        onClick = { selectedMilkTypeFilter = if (selectedMilkTypeFilter == "COW") "ALL" else "COW" },
                        label = { Text("🐄 Cow", fontSize = 11.sp) }
                    )
                }
                if (supportedTypes == "BUFFALO_ONLY" || supportedTypes == "BOTH") {
                    FilterChip(
                        selected = selectedMilkTypeFilter == "BUFFALO",
                        onClick = { selectedMilkTypeFilter = if (selectedMilkTypeFilter == "BUFFALO") "ALL" else "BUFFALO" },
                        label = { Text("🐃 Buff", fontSize = 11.sp) }
                    )
                }
                if (supportedTypes == "BOTH") {
                    FilterChip(
                        selected = selectedMilkTypeFilter == "BOTH",
                        onClick = { selectedMilkTypeFilter = if (selectedMilkTypeFilter == "BOTH") "ALL" else "BOTH" },
                        label = { Text("🔄 Dual", fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Fast "+ Add" Action Button in Top Row
            Button(
                onClick = { onAddNewParty(activeCategory) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp).testTag("btn_top_add_customer")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = when (activeCategory) {
                        "BULK_BUYER" -> "+ Add B2B"
                        "SUPPLIER" -> "+ Add Center"
                        else -> "+ Add Customer"
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Route Filter & Stops Sequence Reordering Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                availableRoutes.forEach { routeName ->
                    FilterChip(
                        selected = selectedRouteFilter == routeName,
                        onClick = { selectedRouteFilter = routeName },
                        label = { Text(if (routeName == "All Routes") "📍 All Routes" else "📍 $routeName", fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            OutlinedButton(
                onClick = onReorderRoute,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp).testTag("btn_open_route_sequence")
            ) {
                Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(14.dp), tint = RoyalBluePrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stops Order", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by customer, route, or mobile...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
            trailingIcon = if (searchQuery.isNotBlank()) {
                { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp)) } }
            } else null,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        )

        // ================= CUSTOMER CARDS LIST OR CLEAR EMPTY STATE =================
        if (filteredCustomers.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth(0.95f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RoyalBlueLight,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (activeCategory) {
                                        "BULK_BUYER" -> Icons.Default.Storefront
                                        "SUPPLIER" -> Icons.Default.Hub
                                        else -> Icons.Default.PersonAdd
                                    },
                                    contentDescription = null,
                                    tint = RoyalBluePrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Text(
                            text = when (activeCategory) {
                                "BULK_BUYER" -> "No B2B / Restaurant Buyers Yet"
                                "SUPPLIER" -> "No Collection Centres Added Yet"
                                else -> "No Household Delivery Customers Yet"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            color = TextPrimary
                        )

                        Text(
                            text = when (activeCategory) {
                                "BULK_BUYER" -> "Add restaurants, cafes, tea stalls, and hotels to manage bulk milk supply and billing."
                                "SUPPLIER" -> "Add collection centres, BMC chilling points, or supply dairies to track outward milk supply slips."
                                else -> "Add your residential doorstep customers, daily morning/evening milk quantities, and monthly delivery run-sheets."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = { onAddNewParty(activeCategory) },
                            colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_empty_state_add_customer")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (activeCategory) {
                                    "BULK_BUYER" -> "+ Add B2B Restaurant / Hotel"
                                    "SUPPLIER" -> "+ Add Collection Centre / Party"
                                    else -> "+ Add Household Customer"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { onImportContacts(activeCategory) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_empty_state_import_contacts")
                        ) {
                            Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📱 Import from Phone Contacts", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(filteredCustomers, key = { _, c -> c.id }) { index, customer ->
                    ModernCustomerDeliveryCard(
                        customer = customer,
                        stopIndex = index + 1,
                        allRouteCustomers = filteredCustomers,
                        deliveries = deliveries,
                        viewModel = viewModel,
                        selectedDate = selectedDate,
                        selectedShift = selectedShift,
                        defaultCowRate = defaultCowRate,
                        defaultBuffRate = defaultBuffRate,
                        onOpenDetail = { onOpenCustomerEntry(customer, index) },
                        onRecordPayment = { onRecordPayment(customer) },
                        onReorderRoute = onReorderRoute,
                        onInformLeave = { onInformLeave(customer) }
                    )
                }
            }
        }
    }
}

@Composable
fun ModernCustomerDeliveryCard(
    customer: CustomerEntity,
    stopIndex: Int = 1,
    allRouteCustomers: List<CustomerEntity> = emptyList(),
    deliveries: List<DeliveryEntity>,
    viewModel: MilkMateViewModel,
    selectedDate: Long,
    selectedShift: String,
    defaultCowRate: Double,
    defaultBuffRate: Double,
    onOpenDetail: () -> Unit,
    onRecordPayment: () -> Unit,
    onReorderRoute: () -> Unit = {},
    onInformLeave: () -> Unit = {}
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPresetsModal by remember { mutableStateOf(false) }

    val quickPresets = customer.getQuickPresetsList()

    if (showPresetsModal) {
        ManageCustomerPresetsModal(
            customer = customer,
            onDismiss = { showPresetsModal = false },
            onSavePresets = { newPresets ->
                viewModel.updateCustomerQuickPresets(customer.id, newPresets)
                showPresetsModal = false
            }
        )
    }

    val customerDeliveries by viewModel.getCustomerDeliveriesFlow(customer.id).collectAsStateWithLifecycle(emptyList())
    val currentBiz by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val supportedMilkTypes = currentBiz?.supportedMilkTypes ?: "BOTH"

    // Lookups for deliveries for this customer on selectedDate and selectedShift
    val cowDelivery = deliveries.find {
        it.customerId == customer.id && it.deliveryDate == selectedDate && it.shift == selectedShift &&
                (it.milkType == "COW" || (customer.milkType == "COW" && it.milkType != "BUFFALO"))
    }
    val buffDelivery = deliveries.find {
        it.customerId == customer.id && it.deliveryDate == selectedDate && it.shift == selectedShift &&
                (it.milkType == "BUFFALO" || (customer.milkType == "BUFFALO" && it.milkType != "COW"))
    }

    val isCowDelivered = cowDelivery?.isDelivered == true
    val isBuffDelivered = buffDelivery?.isDelivered == true

    val cowRate = if (customer.cowRate > 0) customer.cowRate else (if (customer.rate > 0) customer.rate else defaultCowRate)
    val buffRate = if (customer.buffaloRate > 0) customer.buffaloRate else (if (customer.rate > 0) customer.rate else defaultBuffRate)

    val currentCowQty = cowDelivery?.quantityLiters ?: if (customer.cowQuantity > 0) customer.cowQuantity else customer.defaultQuantity
    val currentBuffQty = buffDelivery?.quantityLiters ?: if (customer.buffaloQuantity > 0) customer.buffaloQuantity else customer.defaultQuantity

    val cowTotal = if (isCowDelivered) (cowDelivery?.totalAmount ?: (currentCowQty * cowRate)) else 0.0
    val buffTotal = if (isBuffDelivered) (buffDelivery?.totalAmount ?: (currentBuffQty * buffRate)) else 0.0
    val totalRecordedAmount = cowTotal + buffTotal

    val isDeliveredOverall = when (customer.milkType) {
        "COW" -> isCowDelivered
        "BUFFALO" -> isBuffDelivered
        "BOTH" -> isCowDelivered && isBuffDelivered
        else -> isCowDelivered || isBuffDelivered
    }

    val isPartiallyDelivered = customer.milkType == "BOTH" && (isCowDelivered != isBuffDelivered)
    val todayLeave = viewModel.isCustomerOnLeaveToday(customer, selectedDate, selectedShift)

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DangerRed)
                    Text("Delete Delivery Record?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Text(
                    "Delete today's ($selectedShift shift) recorded delivery for ${customer.name}? This will reset status back to Pending and adjust customer balance.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (cowDelivery != null && buffDelivery != null) {
                        Button(
                            onClick = {
                                viewModel.deleteDelivery(cowDelivery.id)
                                viewModel.deleteDelivery(buffDelivery.id)
                                showDeleteConfirmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete Both (Cow + Buff)")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (cowDelivery != null) viewModel.deleteDelivery(cowDelivery.id)
                                if (buffDelivery != null) viewModel.deleteDelivery(buffDelivery.id)
                                showDeleteConfirmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                        ) {
                            Text("Yes, Delete Record")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (todayLeave != null) Color(0xFFFFFBEB) else MaterialTheme.colorScheme.surface),
        border = if (todayLeave != null) BorderStroke(1.dp, Color(0xFFF59E0B)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDeliveredOverall) 1.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Customer Leave Notification Banner (if on leave today)
            if (todayLeave != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PauseCircleFilled, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "🏖️ ON LEAVE TODAY: Do Not Supply Milk",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Reason: ${todayLeave.reason} (${todayLeave.shift})",
                                    fontSize = 10.sp,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                        TextButton(
                            onClick = { viewModel.cancelCustomerLeave(todayLeave.id) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Resume", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                        }
                    }
                }
            }
            // Header Row: Status Icon, Name, Milk Type, Amount Pill, Trash Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                when {
                                    isDeliveredOverall -> DairyGreenLight
                                    isPartiallyDelivered -> FreshGoldLight
                                    else -> RoyalBlueLight
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isDeliveredOverall -> Icons.Default.Check
                                isPartiallyDelivered -> Icons.Default.HourglassTop
                                else -> Icons.Default.WaterDrop
                            },
                            contentDescription = null,
                            tint = when {
                                isDeliveredOverall -> DairyGreen
                                isPartiallyDelivered -> FreshGold
                                else -> RoyalBluePrimary
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val stopNumber = if (customer.sequenceOrder > 0) customer.sequenceOrder else stopIndex
                            val isFirstStop = stopNumber == 1
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isFirstStop) DairyGreenLight else RoyalBlueLight,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isFirstStop) DairyGreen else RoyalBluePrimary.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable { onReorderRoute() }
                            ) {
                                Text(
                                    text = if (isFirstStop) "🏆 1st Stop" else "Stop #$stopNumber",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFirstStop) DairyGreen else RoyalBluePrimary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                            Text(
                                text = customer.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!isFirstStop && allRouteCustomers.size > 1) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = WarmHoney.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, WarmHoney.copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable {
                                        viewModel.setCustomerAsFirstStop(customer.id, allRouteCustomers)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = WarmHoney, modifier = Modifier.size(10.dp))
                                        Text("Set 1st", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = when {
                                    supportedMilkTypes == "COW_ONLY" -> "🐄 Cow (${customer.cowQuantity}L)"
                                    supportedMilkTypes == "BUFFALO_ONLY" -> "🐃 Buff (${customer.buffaloQuantity}L)"
                                    customer.milkType == "COW" -> "🐄 Cow (${customer.cowQuantity}L)"
                                    customer.milkType == "BUFFALO" -> "🐃 Buff (${customer.buffaloQuantity}L)"
                                    customer.milkType == "BOTH" -> "🥛 Dual (${customer.cowQuantity}L Cow + ${customer.buffaloQuantity}L Buff)"
                                    else -> "🥛 ${customer.milkType}"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (customer.route.isNotBlank()) {
                                Text(text = "• 📍 ${customer.route}", fontSize = 10.sp, color = RoyalBluePrimary)
                            }
                        }
                    }
                }

                // Amount / Status Pill & Trash
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isDeliveredOverall -> DairyGreenLight
                            isPartiallyDelivered -> FreshGoldLight
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = if (isDeliveredOverall || isPartiallyDelivered) String.format(Locale.US, "₹%.0f", totalRecordedAmount) else "Pending",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = when {
                                isDeliveredOverall -> DairyGreen
                                isPartiallyDelivered -> FreshGold
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (cowDelivery != null || buffDelivery != null) {
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Delivery", tint = DangerRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            // ================= INLINE MILK CONTROLS =================
            // Cow Control (if Cow or Both)
            if (supportedMilkTypes != "BUFFALO_ONLY" && (customer.milkType == "COW" || customer.milkType == "BOTH")) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isCowDelivered) DairyGreen.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🐄 Cow", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = "₹${cowRate.toInt()}/L",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Stepper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (cowDelivery != null) {
                                    viewModel.quickAdjustQuantity(cowDelivery, -0.5)
                                } else {
                                    val newQty = (currentCowQty - 0.5).coerceAtLeast(0.25)
                                    viewModel.saveDelivery(
                                        id = null, customerId = customer.id, customerName = customer.name, customerType = customer.type,
                                        date = selectedDate, shift = selectedShift, milkType = "COW", quantity = newQty,
                                        fat = 0.0, snf = 0.0, clr = 0.0, customRate = cowRate, isDelivered = true, notes = "Delivered"
                                    )
                                }
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("−", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Text(
                            text = String.format(Locale.US, "%.1f L", currentCowQty),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        IconButton(
                            onClick = {
                                if (cowDelivery != null) {
                                    viewModel.quickAdjustQuantity(cowDelivery, 0.5)
                                } else {
                                    val newQty = currentCowQty + 0.5
                                    viewModel.saveDelivery(
                                        id = null, customerId = customer.id, customerName = customer.name, customerType = customer.type,
                                        date = selectedDate, shift = selectedShift, milkType = "COW", quantity = newQty,
                                        fat = 0.0, snf = 0.0, clr = 0.0, customRate = cowRate, isDelivered = true, notes = "Delivered"
                                    )
                                }
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RoyalBluePrimary)
                        }
                    }

                    // 1-Tap Toggle Cow Deliver Button
                    Button(
                        onClick = {
                            if (cowDelivery != null) {
                                viewModel.quickToggleDelivery(cowDelivery)
                            } else {
                                viewModel.saveDelivery(
                                    id = null, customerId = customer.id, customerName = customer.name, customerType = customer.type,
                                    date = selectedDate, shift = selectedShift, milkType = "COW", quantity = currentCowQty,
                                    fat = 0.0, snf = 0.0, clr = 0.0, customRate = cowRate, isDelivered = true, notes = "Delivered"
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCowDelivered) DairyGreen else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(if (isCowDelivered) "✓ Done" else "Deliver", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Cow Quick Add Presets Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.clickable { showPresetsModal = true }
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = FreshGold, modifier = Modifier.size(12.dp))
                        Text("Quick Add:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                    }
                    quickPresets.forEach { presetQty ->
                        val isCurrent = isCowDelivered && Math.abs(currentCowQty - presetQty) < 0.01
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isCurrent) DairyGreen else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                viewModel.saveDelivery(
                                    id = cowDelivery?.id,
                                    customerId = customer.id,
                                    customerName = customer.name,
                                    customerType = customer.type,
                                    date = selectedDate,
                                    shift = selectedShift,
                                    milkType = "COW",
                                    quantity = presetQty,
                                    fat = 0.0, snf = 0.0, clr = 0.0,
                                    customRate = cowRate,
                                    isDelivered = true,
                                    notes = "Quick Add ${presetQty}L"
                                )
                            }
                        ) {
                            Text(
                                text = "${if (presetQty % 1.0 == 0.0) presetQty.toInt().toString() else presetQty.toString()}L",
                                fontSize = 10.sp,
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showPresetsModal = true },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Edit Quick Presets", tint = RoyalBluePrimary, modifier = Modifier.size(13.dp))
                    }
                }
            }

            // Buffalo Control (if Buffalo or Both)
            if (supportedMilkTypes != "COW_ONLY" && (customer.milkType == "BUFFALO" || customer.milkType == "BOTH")) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isBuffDelivered) DairyGreen.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🐃 Buff", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = "₹${buffRate.toInt()}/L",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Stepper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (buffDelivery != null) {
                                    viewModel.quickAdjustQuantity(buffDelivery, -0.5)
                                } else {
                                    val newQty = (currentBuffQty - 0.5).coerceAtLeast(0.25)
                                    viewModel.saveDelivery(
                                        id = null, customerId = customer.id, customerName = customer.name, customerType = customer.type,
                                        date = selectedDate, shift = selectedShift, milkType = "BUFFALO", quantity = newQty,
                                        fat = 0.0, snf = 0.0, clr = 0.0, customRate = buffRate, isDelivered = true, notes = "Delivered"
                                    )
                                }
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("−", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Text(
                            text = String.format(Locale.US, "%.1f L", currentBuffQty),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        IconButton(
                            onClick = {
                                if (buffDelivery != null) {
                                    viewModel.quickAdjustQuantity(buffDelivery, 0.5)
                                } else {
                                    val newQty = currentBuffQty + 0.5
                                    viewModel.saveDelivery(
                                        id = null, customerId = customer.id, customerName = customer.name, customerType = customer.type,
                                        date = selectedDate, shift = selectedShift, milkType = "BUFFALO", quantity = newQty,
                                        fat = 0.0, snf = 0.0, clr = 0.0, customRate = buffRate, isDelivered = true, notes = "Delivered"
                                    )
                                }
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RoyalBluePrimary)
                        }
                    }

                    // 1-Tap Toggle Buff Deliver Button
                    Button(
                        onClick = {
                            if (buffDelivery != null) {
                                viewModel.quickToggleDelivery(buffDelivery)
                            } else {
                                viewModel.saveDelivery(
                                    id = null, customerId = customer.id, customerName = customer.name, customerType = customer.type,
                                    date = selectedDate, shift = selectedShift, milkType = "BUFFALO", quantity = currentBuffQty,
                                    fat = 0.0, snf = 0.0, clr = 0.0, customRate = buffRate, isDelivered = true, notes = "Delivered"
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBuffDelivered) DairyGreen else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(if (isBuffDelivered) "✓ Done" else "Deliver", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Buffalo Quick Add Presets Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.clickable { showPresetsModal = true }
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = FreshGold, modifier = Modifier.size(12.dp))
                        Text("Quick Add:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6D4C41))
                    }
                    quickPresets.forEach { presetQty ->
                        val isCurrent = isBuffDelivered && Math.abs(currentBuffQty - presetQty) < 0.01
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isCurrent) DairyGreen else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                viewModel.saveDelivery(
                                    id = buffDelivery?.id,
                                    customerId = customer.id,
                                    customerName = customer.name,
                                    customerType = customer.type,
                                    date = selectedDate,
                                    shift = selectedShift,
                                    milkType = "BUFFALO",
                                    quantity = presetQty,
                                    fat = 0.0, snf = 0.0, clr = 0.0,
                                    customRate = buffRate,
                                    isDelivered = true,
                                    notes = "Quick Add ${presetQty}L"
                                )
                            }
                        ) {
                            Text(
                                text = "${if (presetQty % 1.0 == 0.0) presetQty.toInt().toString() else presetQty.toString()}L",
                                fontSize = 10.sp,
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showPresetsModal = true },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Edit Quick Presets", tint = RoyalBluePrimary, modifier = Modifier.size(13.dp))
                    }
                }
            }

            // Quick Actions Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Balance Tag
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Due:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format(Locale.US, "₹%.0f", customer.outstandingBalance),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (customer.outstandingBalance > 0) DangerRed else DairyGreen
                    )
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pause Milk / Leave Notice
                    OutlinedButton(
                        onClick = onInformLeave,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD97706)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.PauseCircleOutline, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFD97706))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (todayLeave != null) "Edit Leave" else "Pause Milk", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Fast Absent / 0L
                    OutlinedButton(
                        onClick = {
                            if (supportedMilkTypes != "BUFFALO_ONLY" && (customer.milkType == "COW" || customer.milkType == "BOTH")) {
                                viewModel.saveDelivery(
                                    id = cowDelivery?.id, customerId = customer.id, customerName = customer.name, customerType = customer.type,
                                    date = selectedDate, shift = selectedShift, milkType = "COW", quantity = 0.0,
                                    fat = 0.0, snf = 0.0, clr = 0.0, customRate = cowRate, isDelivered = false, notes = "Absent / Holiday"
                                )
                            }
                            if (supportedMilkTypes != "COW_ONLY" && (customer.milkType == "BUFFALO" || customer.milkType == "BOTH")) {
                                viewModel.saveDelivery(
                                    id = buffDelivery?.id, customerId = customer.id, customerName = customer.name, customerType = customer.type,
                                    date = selectedDate, shift = selectedShift, milkType = "BUFFALO", quantity = 0.0,
                                    fat = 0.0, snf = 0.0, clr = 0.0, customRate = buffRate, isDelivered = false, notes = "Absent / Holiday"
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("🏖️ Absent", fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Collect Payment
                    OutlinedButton(
                        onClick = onRecordPayment,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("💰 Pay", fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Open Full Detail Modal / Testing
                    Button(
                        onClick = onOpenDetail,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Details 📝", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Expand / Collapse history
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Expanded Ledger History & Quick Contacts
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Contact Buttons Row
                    if (customer.mobile.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.mobile}"))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call ${customer.mobile}", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val cleanNum = customer.mobile.replace("+", "").replace(" ", "").trim()
                                    val msg = "Namaste ${customer.name}, your balance with us is ₹${customer.outstandingBalance.toInt()}."
                                    val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanNum?text=${Uri.encode(msg)}"))
                                    context.startActivity(waIntent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    // Past Deliveries History
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🥛 Past Delivery History", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = RoyalBluePrimary)
                                val totalLitersPast = customerDeliveries.filter { it.isDelivered }.sumOf { it.quantityLiters }
                                Text("Total: ${String.format(Locale.US, "%.1f", totalLitersPast)}L", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                            }

                            if (customerDeliveries.isNotEmpty()) {
                                customerDeliveries.take(6).forEach { d ->
                                    val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(d.deliveryDate))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$dateStr (${d.shift.take(1)}) • ${if (d.milkType == "COW") "🐄 Cow" else "🐃 Buff"}",
                                            fontSize = 11.sp
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "${d.quantityLiters}L • ₹${d.totalAmount.toInt()}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = if (d.isDelivered) DairyGreen.copy(alpha = 0.15f) else DangerRed.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = if (d.isDelivered) "✓" else "Absent",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (d.isDelivered) DairyGreen else DangerRed,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }

                                            // Quick delete button for individual history row
                                            IconButton(
                                                onClick = { viewModel.deleteDelivery(d.id) },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Delete this delivery record",
                                                    tint = DangerRed.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text("No delivery history logged yet", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernStepDeliveryScreen(
    customer: CustomerEntity,
    currentIndex: Int,
    totalCount: Int,
    existingCowDelivery: DeliveryEntity?,
    existingBuffDelivery: DeliveryEntity?,
    selectedDate: Long,
    selectedShift: String,
    defaultCowRate: Double,
    defaultBuffRate: Double,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onSkipToNext: () -> Unit,
    onDeleteDelivery: (String) -> Unit,
    onUpdatePresets: (String) -> Unit = {},
    supportedMilkTypes: String = "BOTH",
    todayLeave: CustomerLeaveEntity? = null,
    onInformLeave: () -> Unit = {},
    onSaveDual: (
        includeCow: Boolean,
        cowQty: Double,
        cowRate: Double,
        cowFat: Double,
        cowSnf: Double,
        cowClr: Double,
        cowSkip: Boolean,
        includeBuff: Boolean,
        buffQty: Double,
        buffRate: Double,
        buffFat: Double,
        buffSnf: Double,
        buffClr: Double,
        buffSkip: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val isCowAlreadyDone = existingCowDelivery?.isDelivered == true
    val isBuffAlreadyDone = existingBuffDelivery?.isDelivered == true
    val isAnyAlreadyDone = isCowAlreadyDone || isBuffAlreadyDone

    var showStepPresetsModal by remember { mutableStateOf(false) }
    val quickPresets = customer.getQuickPresetsList()

    if (showStepPresetsModal) {
        ManageCustomerPresetsModal(
            customer = customer,
            onDismiss = { showStepPresetsModal = false },
            onSavePresets = { newPresets ->
                onUpdatePresets(newPresets)
                showStepPresetsModal = false
            }
        )
    }

    var includeCow by remember {
        mutableStateOf(supportedMilkTypes != "BUFFALO_ONLY" && (customer.milkType == "COW" || customer.milkType == "BOTH" || existingCowDelivery != null))
    }
    var includeBuff by remember {
        mutableStateOf(supportedMilkTypes != "COW_ONLY" && (customer.milkType == "BUFFALO" || customer.milkType == "BOTH" || existingBuffDelivery != null))
    }

    var cowQuantityText by remember {
        mutableStateOf(
            existingCowDelivery?.quantityLiters?.toString()
                ?: if (customer.cowQuantity > 0) customer.cowQuantity.toString() else "1.0"
        )
    }
    var cowRateText by remember {
        mutableStateOf(
            existingCowDelivery?.ratePerLiter?.toString()
                ?: (if (customer.cowRate > 0) customer.cowRate else defaultCowRate).toString()
        )
    }
    var cowFatText by remember {
        mutableStateOf(if ((existingCowDelivery?.fat ?: 0.0) > 0) existingCowDelivery!!.fat.toString() else "4.0")
    }
    var cowSnfText by remember {
        mutableStateOf(if ((existingCowDelivery?.snf ?: 0.0) > 0) existingCowDelivery!!.snf.toString() else "8.5")
    }
    var cowClrText by remember {
        mutableStateOf(if ((existingCowDelivery?.clr ?: 0.0) > 0) existingCowDelivery!!.clr.toString() else "28")
    }

    var buffQuantityText by remember {
        mutableStateOf(
            existingBuffDelivery?.quantityLiters?.toString()
                ?: if (customer.buffaloQuantity > 0) customer.buffaloQuantity.toString() else "1.0"
        )
    }
    var buffRateText by remember {
        mutableStateOf(
            existingBuffDelivery?.ratePerLiter?.toString()
                ?: (if (customer.buffaloRate > 0) customer.buffaloRate else defaultBuffRate).toString()
        )
    }
    var buffFatText by remember {
        mutableStateOf(if ((existingBuffDelivery?.fat ?: 0.0) > 0) existingBuffDelivery!!.fat.toString() else "6.5")
    }
    var buffSnfText by remember {
        mutableStateOf(if ((existingBuffDelivery?.snf ?: 0.0) > 0) existingBuffDelivery!!.snf.toString() else "9.0")
    }
    var buffClrText by remember {
        mutableStateOf(if ((existingBuffDelivery?.clr ?: 0.0) > 0) existingBuffDelivery!!.clr.toString() else "30")
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val cowQty = cowQuantityText.toDoubleOrNull() ?: 0.0
    val cowRate = cowRateText.toDoubleOrNull() ?: 0.0
    val cowFat = cowFatText.toDoubleOrNull() ?: 0.0
    val cowSnf = cowSnfText.toDoubleOrNull() ?: 0.0
    val cowClr = cowClrText.toDoubleOrNull() ?: 0.0
    val cowAmount = if (includeCow) cowQty * cowRate else 0.0

    val buffQty = buffQuantityText.toDoubleOrNull() ?: 0.0
    val buffRate = buffRateText.toDoubleOrNull() ?: 0.0
    val buffFat = buffFatText.toDoubleOrNull() ?: 0.0
    val buffSnf = buffSnfText.toDoubleOrNull() ?: 0.0
    val buffClr = buffClrText.toDoubleOrNull() ?: 0.0
    val buffAmount = if (includeBuff) buffQty * buffRate else 0.0

    val totalLiters = (if (includeCow) cowQty else 0.0) + (if (includeBuff) buffQty else 0.0)
    val totalAmount = cowAmount + buffAmount

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DangerRed)
                    Text("Delete Delivery Record?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Text(
                    "Are you sure you want to remove this delivery for ${customer.name}? This will reset today's ($selectedShift) delivery status to Pending and adjust balance.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (existingCowDelivery != null) onDeleteDelivery(existingCowDelivery.id)
                        if (existingBuffDelivery != null) onDeleteDelivery(existingBuffDelivery.id)
                        showDeleteConfirmDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Yes, Delete Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Route Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showStepPresetsModal = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.FlashOn, contentDescription = "Edit Quick Presets", tint = FreshGold, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = "📍 ${customer.route.ifBlank { "Main Route" }} • ${customer.type.replace("_", " ")}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = RoyalBluePrimary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "$currentIndex / $totalCount",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = RoyalBluePrimary
                )
            }
        }

        // Customer Leave Alert Card
        if (todayLeave != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                border = BorderStroke(1.dp, Color(0xFFF59E0B))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PauseCircleFilled, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "🏖️ DO NOT SUPPLY MILK TODAY",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "Customer informed: ${todayLeave.reason} (${todayLeave.shift})",
                                fontSize = 11.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                    Button(
                        onClick = onInformLeave,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("Edit Leave", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Status Card: Already Recorded vs Pending Alert
        if (isAnyAlreadyDone) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DairyGreen.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(18.dp))
                            Text("Already Recorded for $selectedShift Shift", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DairyGreen)
                        }

                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Record", tint = DangerRed, modifier = Modifier.size(18.dp))
                        }
                    }

                    Text(
                        text = "Saved: " + listOfNotNull(
                            if (existingCowDelivery != null) "🐄 Cow: ${existingCowDelivery.quantityLiters}L (₹${existingCowDelivery.totalAmount.toInt()})" else null,
                            if (existingBuffDelivery != null) "🐃 Buff: ${existingBuffDelivery.quantityLiters}L (₹${existingBuffDelivery.totalAmount.toInt()})" else null
                        ).joinToString(" + "),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onSkipToNext,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text("⏭️ Keep & Next", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.12f), contentColor = DangerRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text("🗑️ Delete Record", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏳ $selectedShift Shift Delivery (${dateFormat.format(Date(selectedDate))})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RoyalBluePrimary
                    )
                    TextButton(onClick = onSkipToNext, contentPadding = PaddingValues(0.dp)) {
                        Text("Skip ⏭️", fontSize = 11.sp)
                    }
                }
            }
        }

        // ================= SECTION 1: COW MILK =================
        if (supportedMilkTypes != "BUFFALO_ONLY") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (includeCow) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                elevation = if (includeCow) CardDefaults.cardElevation(2.dp) else CardDefaults.cardElevation(0.dp)
            ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(
                            checked = includeCow,
                            onCheckedChange = { includeCow = it }
                        )
                        Column {
                            Text("🐄 Cow Milk", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (includeCow && cowQty > 0) {
                                Text(
                                    text = String.format(Locale.US, "%.1f L @ ₹%.1f = ₹%.2f", cowQty, cowRate, cowAmount),
                                    fontSize = 11.sp,
                                    color = RoyalBluePrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (isCowAlreadyDone) {
                        Surface(shape = RoundedCornerShape(6.dp), color = DairyGreenLight) {
                            Text("✓ Saved", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DairyGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }

                if (includeCow) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cowQuantityText,
                            onValueChange = { cowQuantityText = it },
                            label = { Text("Cow Qty (L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = cowRateText,
                            onValueChange = { cowRateText = it },
                            label = { Text("Rate (₹/L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Cow Quick Qty Chips
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚡ Quick Add:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                        quickPresets.forEach { p ->
                            val pStr = if (p % 1.0 == 0.0) p.toInt().toString() else p.toString()
                            val isSelected = cowQuantityText == pStr || (cowQuantityText.toDoubleOrNull() == p)
                            FilterChip(
                                selected = isSelected,
                                onClick = { cowQuantityText = pStr },
                                label = { Text("${pStr}L", fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                        IconButton(onClick = { showStepPresetsModal = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Tune, contentDescription = "Edit Presets", tint = RoyalBluePrimary, modifier = Modifier.size(14.dp))
                        }
                    }

                    // Supplier FAT/SNF testing for Cow
                    if (customer.type == "SUPPLIER") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = cowFatText,
                                onValueChange = { cowFatText = it },
                                label = { Text("FAT %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = cowSnfText,
                                onValueChange = { cowSnfText = it },
                                label = { Text("SNF %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = cowClrText,
                                onValueChange = {
                                    cowClrText = it
                                    val c = it.toDoubleOrNull() ?: 0.0
                                    val f = cowFatText.toDoubleOrNull() ?: 0.0
                                    if (c > 0 && f > 0) {
                                        cowSnfText = String.format(Locale.US, "%.1f", (c / 4.0) + (0.21 * f) + 0.36)
                                    }
                                },
                                label = { Text("CLR") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

        // ================= SECTION 2: BUFFALO MILK =================
        if (supportedMilkTypes != "COW_ONLY") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (includeBuff) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                elevation = if (includeBuff) CardDefaults.cardElevation(2.dp) else CardDefaults.cardElevation(0.dp)
            ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(
                            checked = includeBuff,
                            onCheckedChange = { includeBuff = it }
                        )
                        Column {
                            Text("🐃 Buffalo Milk", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (includeBuff && buffQty > 0) {
                                Text(
                                    text = String.format(Locale.US, "%.1f L @ ₹%.1f = ₹%.2f", buffQty, buffRate, buffAmount),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (isBuffAlreadyDone) {
                        Surface(shape = RoundedCornerShape(6.dp), color = DairyGreenLight) {
                            Text("✓ Saved", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DairyGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }

                if (includeBuff) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = buffQuantityText,
                            onValueChange = { buffQuantityText = it },
                            label = { Text("Buff Qty (L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = buffRateText,
                            onValueChange = { buffRateText = it },
                            label = { Text("Rate (₹/L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Buffalo Quick Qty Chips
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚡ Quick Add:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        quickPresets.forEach { p ->
                            val pStr = if (p % 1.0 == 0.0) p.toInt().toString() else p.toString()
                            val isSelected = buffQuantityText == pStr || (buffQuantityText.toDoubleOrNull() == p)
                            FilterChip(
                                selected = isSelected,
                                onClick = { buffQuantityText = pStr },
                                label = { Text("${pStr}L", fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                        IconButton(onClick = { showStepPresetsModal = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Tune, contentDescription = "Edit Presets", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                        }
                    }

                    // Supplier FAT/SNF testing for Buffalo
                    if (customer.type == "SUPPLIER") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = buffFatText,
                                onValueChange = { buffFatText = it },
                                label = { Text("FAT %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = buffSnfText,
                                onValueChange = { buffSnfText = it },
                                label = { Text("SNF %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = buffClrText,
                                onValueChange = {
                                    buffClrText = it
                                    val c = it.toDoubleOrNull() ?: 0.0
                                    val f = buffFatText.toDoubleOrNull() ?: 0.0
                                    if (c > 0 && f > 0) {
                                        buffSnfText = String.format(Locale.US, "%.1f", (c / 4.0) + (0.21 * f) + 0.36)
                                    }
                                },
                                label = { Text("CLR") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

        // ================= TOTAL SUMMARY BANNER =================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (customer.type == "SUPPLIER") "Total Farmer Milk" else "Total Delivery Volume",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f Liters", totalLiters),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (customer.type == "SUPPLIER") "Total Payout" else "Session Bill",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "₹%.2f", totalAmount),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = RoyalBluePrimary
                    )
                }
            }
        }

        // ================= BOTTOM STICKY ACTION BUTTONS =================
        Button(
            onClick = {
                onSaveDual(
                    includeCow,
                    cowQty,
                    cowRate,
                    cowFat,
                    cowSnf,
                    cowClr,
                    false,
                    includeBuff,
                    buffQty,
                    buffRate,
                    buffFat,
                    buffSnf,
                    buffClr,
                    false
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = includeCow || includeBuff
        ) {
            Text(
                if (isAnyAlreadyDone) "✓ Update Entry & Next Customer →" else "✓ Save Delivery & Next Customer →",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = currentIndex > 1,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("◀ Prev", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = {
                    onSaveDual(
                        includeCow,
                        0.0,
                        cowRate,
                        cowFat,
                        cowSnf,
                        cowClr,
                        true,
                        includeBuff,
                        0.0,
                        buffRate,
                        buffFat,
                        buffSnf,
                        buffClr,
                        true
                    )
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1.2f)
            ) {
                Text("🏖️ Absent (0L)", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = onSkipToNext,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Skip ▶", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun RecordCustomerPaymentDialog(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onSavePayment: (Double, String, String) -> Unit
) {
    var amountText by remember {
        mutableStateOf(if (customer.outstandingBalance > 0) customer.outstandingBalance.toInt().toString() else "500")
    }
    var selectedMethod by remember { mutableStateOf("CASH") }
    var notesText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Collect Payment", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Due: ₹${customer.outstandingBalance.toInt()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (customer.outstandingBalance > 0) DangerRed else DairyGreen
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Customer: ${customer.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Amount Chips
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("100", "200", "500", "1000").forEach { p ->
                        FilterChip(
                            selected = amountText == p,
                            onClick = { amountText = p },
                            label = { Text("₹$p", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Payment Method Chips
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("CASH" to "💵 Cash", "UPI" to "📱 UPI", "BANK" to "🏦 Bank").forEach { (method, label) ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(label, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Note / Transaction ID (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onSavePayment(amt, selectedMethod, notesText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Text("✓ Save Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ShiftPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = title.loc(),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManageCustomerPresetsModal(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onSavePresets: (String) -> Unit
) {
    var presetsList by remember {
        mutableStateOf(customer.getQuickPresetsList())
    }
    var customQtyText by remember { mutableStateOf("") }
    val commonQuantities = listOf(0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = FreshGold.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = FreshGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Quick Add Presets",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Customer: ${customer.name}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Configure frequently delivered milk quantities (in Liters) for ${customer.name}. These will appear as 1-tap quick buttons on the delivery sheet.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                // Current Active Presets
                Text(
                    text = "ACTIVE PRESETS (TAP ✕ TO REMOVE)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBluePrimary
                )

                if (presetsList.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No presets configured. Select from common quantities below or add custom.",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetsList.forEach { qty ->
                            val label = if (qty % 1.0 == 0.0) "${qty.toInt()}L" else "${qty}L"
                            InputChip(
                                selected = true,
                                onClick = {
                                    presetsList = presetsList.filter { it != qty }
                                },
                                label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = RoyalBluePrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = RoyalBluePrimary
                                )
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Common Presets Quick Add
                Text(
                    text = "QUICK TEMPLATES (TAP TO ADD)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonQuantities.forEach { qty ->
                        val isAdded = presetsList.contains(qty)
                        val label = if (qty % 1.0 == 0.0) "${qty.toInt()}L" else "${qty}L"
                        FilterChip(
                            selected = isAdded,
                            onClick = {
                                presetsList = if (isAdded) {
                                    presetsList.filter { it != qty }
                                } else {
                                    (presetsList + qty).sorted()
                                }
                            },
                            label = {
                                Text(
                                    text = if (isAdded) "✓ $label" else "+ $label",
                                    fontSize = 11.sp,
                                    fontWeight = if (isAdded) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DairyGreen.copy(alpha = 0.15f),
                                selectedLabelColor = DairyGreen
                            )
                        )
                    }
                }

                // Add Custom Liters Input
                Text(
                    text = "ADD CUSTOM QUANTITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customQtyText,
                        onValueChange = { customQtyText = it },
                        label = { Text("Liters (e.g. 1.25)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            val parsed = customQtyText.toDoubleOrNull()
                            if (parsed != null && parsed > 0 && !presetsList.contains(parsed)) {
                                presetsList = (presetsList + parsed).sorted()
                                customQtyText = ""
                            }
                        },
                        enabled = (customQtyText.toDoubleOrNull() ?: 0.0) > 0.0,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+ Add")
                    }
                }

                // Presets Quick Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            presetsList = listOf(0.5, 1.0, 1.5, 2.0)
                        }
                    ) {
                        Text("🔄 Reset Defaults (0.5-2L)", fontSize = 11.sp)
                    }

                    TextButton(
                        onClick = {
                            presetsList = listOf(1.0, 2.0, 3.0, 5.0)
                        }
                    ) {
                        Text("🥛 Bulk Preset (1-5L)", fontSize = 11.sp)
                    }
                }

                // Live Preview
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Live Preview on Delivery Sheet:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚡ Quick Add:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                            if (presetsList.isEmpty()) {
                                Text("(no presets)", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                            } else {
                                presetsList.forEach { p ->
                                    val pStr = if (p % 1.0 == 0.0) p.toInt().toString() else p.toString()
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = RoyalBluePrimary.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            "${pStr}L",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RoyalBluePrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val presetsString = presetsList.joinToString(",") {
                        if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                    }
                    onSavePresets(presetsString)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("✓ Save Presets", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RouteSequenceManagerDialog(
    route: String,
    customers: List<CustomerEntity>,
    onDismiss: () -> Unit,
    onSaveSequence: (List<String>) -> Unit
) {
    var orderedList by remember(customers) {
        mutableStateOf(customers.sortedWith(
            compareBy(
                { if (it.sequenceOrder > 0) it.sequenceOrder else Int.MAX_VALUE },
                { it.name.lowercase() }
            )
        ))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Route, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delivery Route Order & Stops",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
                Text(
                    text = "Control which customer comes 1st, 2nd, 3rd in the delivery list & run-sheet.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Guidance Banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DairyGreenLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DairyGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "💡 Top customer is Stop #1. Tap 'Make 1st' on any customer to bring them to the start of the delivery route.",
                            fontSize = 10.5.sp,
                            color = DairyGreen,
                            lineHeight = 14.sp
                        )
                    }
                }

                // Info pill & 1-tap Auto-Sequence button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${orderedList.size} Customers in Route",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    OutlinedButton(
                        onClick = {
                            val renumbered = orderedList.mapIndexed { idx, cust ->
                                cust.copy(sequenceOrder = idx + 1)
                            }
                            orderedList = renumbered
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto-Number 1, 2, 3...", fontSize = 10.5.sp)
                    }
                }

                if (orderedList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No customers found to sequence.", color = TextSecondary, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(orderedList, key = { _, c -> c.id }) { index, cust ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        // Stop badge
                                        Surface(
                                            shape = CircleShape,
                                            color = if (index == 0) DairyGreen else RoyalBluePrimary,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = cust.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1
                                                )
                                                if (index == 0) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = DairyGreenLight
                                                    ) {
                                                        Text("1st Stop", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DairyGreen, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                    }
                                                }
                                            }
                                            Text(
                                                text = if (cust.address.isNotBlank()) cust.address else (if (cust.route.isNotBlank()) "Route: ${cust.route}" else "Mobile: ${cust.mobile}"),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        if (index > 0) {
                                            OutlinedButton(
                                                onClick = {
                                                    val mList = orderedList.toMutableList()
                                                    val item = mList.removeAt(index)
                                                    mList.add(0, item)
                                                    orderedList = mList
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalBluePrimary)
                                            ) {
                                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp), tint = WarmHoney)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("Make 1st", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Move Up Button
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    val mList = orderedList.toMutableList()
                                                    val item = mList.removeAt(index)
                                                    mList.add(index - 1, item)
                                                    orderedList = mList
                                                }
                                            },
                                            enabled = index > 0,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ArrowUpward,
                                                contentDescription = "Move Up",
                                                tint = if (index > 0) RoyalBluePrimary else Color.Gray.copy(alpha = 0.3f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Move Down Button
                                        IconButton(
                                            onClick = {
                                                if (index < orderedList.size - 1) {
                                                    val mList = orderedList.toMutableList()
                                                    val item = mList.removeAt(index)
                                                    mList.add(index + 1, item)
                                                    orderedList = mList
                                                }
                                            },
                                            enabled = index < orderedList.size - 1,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ArrowDownward,
                                                contentDescription = "Move Down",
                                                tint = if (index < orderedList.size - 1) RoyalBluePrimary else Color.Gray.copy(alpha = 0.3f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveSequence(orderedList.map { it.id })
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Route Sequence", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

