package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.BusinessEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.DeliveryEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.StaffAttendanceEntity
import com.example.data.local.entity.StaffEntity
import com.example.data.local.entity.StaffPaymentEntity
import com.example.data.repository.UserSession
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures
import java.text.SimpleDateFormat
import java.util.*

sealed class ManageTab(val index: Int, val title: String, val icon: ImageVector, val tag: String) {
    object MilkRates : ManageTab(0, "Milk Rates & Formula", Icons.Default.Science, "rates")
    object ExpenseCategories : ManageTab(1, "Expense Categories", Icons.Default.Category, "expenses")
    object StaffRoster : ManageTab(2, "Staff & Attendance", Icons.Default.Badge, "staff")
    object RouteMatrix : ManageTab(3, "Route Allotment", Icons.Default.AltRoute, "routes")
    object ShiftsDispatch : ManageTab(4, "Shifts & Dispatch", Icons.Default.LocalShipping, "dispatch")
    object ProductsCatalog : ManageTab(5, "Products Catalog", Icons.Default.Inventory2, "catalog")
    object SmartAlerts : ManageTab(6, "Smart Notifications", Icons.Default.NotificationsActive, "alerts")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageHubScreen(
    viewModel: MilkMateViewModel,
    onBack: () -> Unit,
    initialTab: Int = 0,
    onOpenSettings: () -> Unit = {}
) {
    var activeTabIndex by remember { mutableIntStateOf(initialTab) }
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val staffList by viewModel.staffList.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val todayAttendance by viewModel.todayAttendance.collectAsStateWithLifecycle()
    val allAttendance by viewModel.allAttendance.collectAsStateWithLifecycle()
    val staffPayments by viewModel.staffPayments.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val todayDeliveries by viewModel.todayDeliveries.collectAsStateWithLifecycle()

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Modern Interactive Tab Selector
                    val bMode = business?.businessMode
                    val tabs = remember(bMode) {
                        buildList {
                            add(ManageTab.MilkRates)
                            add(ManageTab.ExpenseCategories)
                            add(ManageTab.StaffRoster)
                            if (BusinessModeFeatures.showDeliveryRoutes(bMode)) {
                                add(ManageTab.RouteMatrix)
                            }
                            add(ManageTab.ShiftsDispatch)
                            if (BusinessModeFeatures.showInventoryManagement(bMode)) {
                                add(ManageTab.ProductsCatalog)
                            }
                            add(ManageTab.SmartAlerts)
                        }
                    }

                    LaunchedEffect(tabs) {
                        if (tabs.none { it.index == activeTabIndex }) {
                            activeTabIndex = tabs.firstOrNull()?.index ?: 0
                        }
                    }

                    val selectedTabIndexInRow = tabs.indexOfFirst { it.index == activeTabIndex }.coerceAtLeast(0)

                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndexInRow,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = RoyalBluePrimary,
                        edgePadding = 12.dp,
                        divider = {}
                    ) {
                        tabs.forEach { tab ->
                            val isSelected = activeTabIndex == tab.index
                            Tab(
                                selected = isSelected,
                                onClick = { activeTabIndex = tab.index },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isSelected) RoyalBluePrimary else TextSecondary
                                        )
                                        Text(
                                            text = tab.title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isSelected) RoyalBluePrimary else TextSecondary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SurfaceBg)
        ) {
            AnimatedContent(
                targetState = activeTabIndex,
                label = "ManageTabAnimation"
            ) { targetTab ->
                when (targetTab) {
                    0 -> RatesAndFormulaTab(
                        viewModel = viewModel,
                        business = business,
                        customers = customers
                    )
                    1 -> ExpenseCategoriesHubTab(
                        viewModel = viewModel
                    )
                    2 -> StaffScreen(
                        viewModel = viewModel,
                        onBack = null
                    )
                    3 -> RouteAllotmentTab(
                        viewModel = viewModel,
                        customers = customers,
                        staffList = staffList
                    )
                    4 -> ShiftsAndDispatchTab(
                        viewModel = viewModel,
                        business = business,
                        todayDeliveries = todayDeliveries
                    )
                    5 -> ProductsCatalogTab(
                        viewModel = viewModel,
                        inventoryItems = inventoryItems
                    )
                    6 -> SmartNotificationsTab(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}



// -------------------------------------------------------------
// MODULE: EXPENSE CATEGORIES MANAGER
// -------------------------------------------------------------
@Composable
fun ExpenseCategoriesHubTab(
    viewModel: MilkMateViewModel
) {
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    var newCategoryText by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var editedCategoryText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth().testTag("manage_hub_expense_categories_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                                color = RoyalBlueLight,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Category,
                                        contentDescription = null,
                                        tint = RoyalBluePrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Dairy Expense Categories",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${session.customExpenseCategories.size} Active Categories Configured",
                                    fontSize = 11.5.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DairyGreenLight
                        ) {
                            Text(
                                text = "OPERATIONS HUB",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DairyGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = BorderSubtle)

                    Text(
                        text = "Customize expense heads used when logging farm expenditures, cattle feed purchases, medical bills, and monthly P&L audits.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    // Add New Category Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("➕ Add Custom Expense Head", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newCategoryText,
                                    onValueChange = { newCategoryText = it },
                                    placeholder = { Text("e.g. Silage, Doctor Fees, Fuel", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Button(
                                    onClick = {
                                        if (newCategoryText.isNotBlank()) {
                                            viewModel.addExpenseCategory(newCategoryText)
                                            newCategoryText = ""
                                        }
                                    },
                                    enabled = newCategoryText.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(50.dp)
                                ) {
                                    Text("+ Add", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        items(session.customExpenseCategories, key = { it }) { category ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                if (editingCategory == category) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = editedCategoryText,
                            onValueChange = { editedCategoryText = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        IconButton(onClick = {
                            if (editedCategoryText.isNotBlank()) {
                                viewModel.editExpenseCategory(category, editedCategoryText)
                                editingCategory = null
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save", tint = DairyGreen)
                        }
                        IconButton(onClick = { editingCategory = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = DangerRed)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (category.contains("Feed", ignoreCase = true) || category.contains("Fodder", ignoreCase = true)) WarmHoney
                                        else if (category.contains("Doctor", ignoreCase = true) || category.contains("Vet", ignoreCase = true)) DangerRed
                                        else RoyalBluePrimary,
                                        CircleShape
                                    )
                            )
                            Text(category, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    editingCategory = category
                                    editedCategoryText = category
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Category", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }

                            if (session.customExpenseCategories.size > 1) {
                                IconButton(
                                    onClick = { viewModel.deleteExpenseCategory(category) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Category", tint = DangerRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { viewModel.resetExpenseCategories() },
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Expense Categories to Default", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// MODULE 1: MILK RATES & FORMULA STUDIO
// -------------------------------------------------------------
@Composable
fun RatesAndFormulaTab(
    viewModel: MilkMateViewModel,
    business: BusinessEntity?,
    customers: List<CustomerEntity>
) {
    var pricingMode by remember(business?.pricingMode) {
        mutableStateOf(business?.pricingMode ?: "FAT_SNF")
    }
    var cowRateText by remember(business?.cowMilkRate) {
        mutableStateOf(String.format(Locale.US, "%.1f", business?.cowMilkRate ?: 55.0))
    }
    var buffaloRateText by remember(business?.buffaloMilkRate) {
        mutableStateOf(String.format(Locale.US, "%.1f", business?.buffaloMilkRate ?: 75.0))
    }
    var baseFatText by remember(business?.fatBaseRate) {
        mutableStateOf(String.format(Locale.US, "%.1f", business?.fatBaseRate ?: 3.5))
    }
    var baseSnfText by remember(business?.snfBaseRate) {
        mutableStateOf(String.format(Locale.US, "%.1f", business?.snfBaseRate ?: 8.5))
    }
    var fatRateFactorText by remember(business?.fatDiffRate) {
        mutableStateOf(String.format(Locale.US, "%.1f", business?.fatDiffRate ?: 7.5))
    }
    var snfRateFactorText by remember(business?.snfDiffRate) {
        mutableStateOf(String.format(Locale.US, "%.1f", business?.snfDiffRate ?: 4.0))
    }
    var paneerRateText by remember(business?.paneerRatePerKg) {
        mutableStateOf(String.format(Locale.US, "%.0f", if ((business?.paneerRatePerKg ?: 0.0) > 0) business?.paneerRatePerKg else 360.0))
    }
    var khoaRateText by remember(business?.khoaRatePerKg) {
        mutableStateOf(String.format(Locale.US, "%.0f", if ((business?.khoaRatePerKg ?: 0.0) > 0) business?.khoaRatePerKg else 330.0))
    }

    // Formula Simulator State
    var simFat by remember { mutableFloatStateOf(4.5f) }
    var simSnf by remember { mutableFloatStateOf(8.8f) }
    var simMilkType by remember { mutableStateOf("COW") }

    val baseRate = (if (simMilkType == "COW") cowRateText.toDoubleOrNull() else buffaloRateText.toDoubleOrNull()) ?: 55.0
    val stdFat = baseFatText.toDoubleOrNull() ?: 3.5
    val stdSnf = baseSnfText.toDoubleOrNull() ?: 8.5
    val fatFactor = fatRateFactorText.toDoubleOrNull() ?: 7.5
    val snfFactor = snfRateFactorText.toDoubleOrNull() ?: 4.0
    val paneerRate = paneerRateText.toDoubleOrNull() ?: 360.0
    val khoaRate = khoaRateText.toDoubleOrNull() ?: 330.0

    val simBusiness = remember(business, pricingMode, cowRateText, buffaloRateText, baseFatText, baseSnfText, fatRateFactorText, snfRateFactorText, paneerRateText, khoaRateText) {
        (business ?: BusinessEntity(id = "preview", ownerUid = "preview", businessName = "Dairy", ownerName = "Owner", phone = "", accountStartDate = 0L)).copy(
            pricingMode = pricingMode,
            cowMilkRate = cowRateText.toDoubleOrNull() ?: 55.0,
            buffaloMilkRate = buffaloRateText.toDoubleOrNull() ?: 75.0,
            fatBaseRate = stdFat,
            snfBaseRate = stdSnf,
            fatDiffRate = fatFactor,
            snfDiffRate = snfFactor,
            paneerRatePerKg = paneerRate,
            khoaRatePerKg = khoaRate
        )
    }

    val calculatedPrice = remember(simBusiness, simMilkType, simFat, simSnf) {
        com.example.data.repository.PricingEngine.calculateRatePerLiter(
            business = simBusiness,
            customer = null,
            milkType = simMilkType,
            fat = simFat.toDouble(),
            snf = simSnf.toDouble()
        )
    }

    val livePaneerYield = remember(simFat, simSnf) {
        com.example.data.repository.PricingEngine.calculatePaneerYieldGrams(simFat.toDouble(), simSnf.toDouble())
    }
    val liveKhoaYield = remember(simFat, simSnf) {
        com.example.data.repository.PricingEngine.calculateKhoaYieldGrams(simFat.toDouble(), simSnf.toDouble())
    }
    val liveGheeYield = remember(simFat) {
        com.example.data.repository.PricingEngine.calculateGheeYieldGrams(simFat.toDouble())
    }

    var showCustomFormulaStudio by remember { mutableStateOf(false) }

    if (showCustomFormulaStudio) {
        CustomFormulaStudioDialog(
            business = business,
            viewModel = viewModel,
            onDismiss = { showCustomFormulaStudio = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Hero Header Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = RoyalBluePrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Dynamic Pricing & Formula Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Configured for ${business?.businessName ?: "Dairy"}",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = when (pricingMode) {
                                    "FAT_SNF" -> "FAT + SNF Matrix"
                                    "PANEER_YIELD" -> "Paneer Yield / L"
                                    "KHOA_YIELD" -> "Khoya Yield / L"
                                    "GHEE_YIELD" -> "Ghee Yield / L"
                                    "FAT_ONLY" -> "Pure FAT Multiplier"
                                    "CLR_TS" -> "CLR Density"
                                    else -> "Flat Rate"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FreshGoldLight,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricTile(
                            title = "Cow Base",
                            value = "₹$cowRateText/L",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Buff Base",
                            value = "₹$buffaloRateText/L",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Active Customers",
                            value = "${customers.size}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Pricing Mode Selector
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select Pricing & Sales Method",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Choose how milk rates are calculated across collection desks and invoices:",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PricingModeChip(
                                title = "FAT + SNF",
                                subtitle = "Cooperative Standard",
                                isSelected = pricingMode == "FAT_SNF",
                                onClick = { pricingMode = "FAT_SNF" },
                                modifier = Modifier.weight(1f)
                            )
                            PricingModeChip(
                                title = "🧀 Paneer Yield",
                                subtitle = "Grams / Liter",
                                isSelected = pricingMode == "PANEER_YIELD",
                                onClick = { pricingMode = "PANEER_YIELD" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PricingModeChip(
                                title = "🍬 Khoya Yield",
                                subtitle = "Grams / Liter",
                                isSelected = pricingMode == "KHOA_YIELD" || pricingMode == "KHOYA_YIELD",
                                onClick = { pricingMode = "KHOA_YIELD" },
                                modifier = Modifier.weight(1f)
                            )
                            PricingModeChip(
                                title = "🏺 Ghee Yield",
                                subtitle = "Fat Recovery",
                                isSelected = pricingMode == "GHEE_YIELD",
                                onClick = { pricingMode = "GHEE_YIELD" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PricingModeChip(
                                title = "FAT Only",
                                subtitle = "Linear Multiplier",
                                isSelected = pricingMode == "FAT_ONLY",
                                onClick = { pricingMode = "FAT_ONLY" },
                                modifier = Modifier.weight(1f)
                            )
                            PricingModeChip(
                                title = "Fixed Rate",
                                subtitle = "Per-Liter Flat",
                                isSelected = pricingMode == "DIRECT" || pricingMode == "FIXED",
                                onClick = { pricingMode = "DIRECT" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PricingModeChip(
                                title = "⚙️ Custom Formula",
                                subtitle = "Build Your Own Equation",
                                isSelected = pricingMode == "CUSTOM",
                                onClick = {
                                    pricingMode = "CUSTOM"
                                    showCustomFormulaStudio = true
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        FilledTonalButton(
                            onClick = { showCustomFormulaStudio = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = OceanMidnight, contentColor = FreshGold),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.Functions, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📐 Open Custom Formula Studio (Equation Builder) →", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Standard Rates & Yield Configuration
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Base Milk Rates & Market Yield Values",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = cowRateText,
                            onValueChange = { cowRateText = it },
                            label = { Text("Cow Base (₹/L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = buffaloRateText,
                            onValueChange = { buffaloRateText = it },
                            label = { Text("Buffalo Base (₹/L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    if (pricingMode == "PANEER_YIELD" || pricingMode == "KHOA_YIELD" || pricingMode == "KHOYA_YIELD") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = paneerRateText,
                                onValueChange = { paneerRateText = it },
                                label = { Text("Paneer Rate (₹/kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = khoaRateText,
                                onValueChange = { khoaRateText = it },
                                label = { Text("Khoya Rate (₹/kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    if (pricingMode == "FAT_SNF" || pricingMode == "FAT_ONLY") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = baseFatText,
                                onValueChange = { baseFatText = it },
                                label = { Text("Std FAT (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = fatRateFactorText,
                                onValueChange = { fatRateFactorText = it },
                                label = { Text("FAT Mult (₹/%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        if (pricingMode == "FAT_SNF") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = baseSnfText,
                                    onValueChange = { baseSnfText = it },
                                    label = { Text("Std SNF (%)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = snfRateFactorText,
                                    onValueChange = { snfRateFactorText = it },
                                    label = { Text("SNF Mult (₹/%)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Quick Presets:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetChip(label = "Amul Pattern (3.5/8.5)") {
                            cowRateText = "54.0"
                            buffaloRateText = "72.0"
                            baseFatText = "3.5"
                            baseSnfText = "8.5"
                            fatRateFactorText = "7.8"
                            snfRateFactorText = "4.2"
                            pricingMode = "FAT_SNF"
                        }
                        PresetChip(label = "🧀 Paneer Yield Base") {
                            paneerRateText = "380"
                            pricingMode = "PANEER_YIELD"
                        }
                        PresetChip(label = "🍬 Khoya Yield Base") {
                            khoaRateText = "340"
                            pricingMode = "KHOA_YIELD"
                        }
                        PresetChip(label = "Premium Buffalo A2") {
                            cowRateText = "60.0"
                            buffaloRateText = "82.0"
                            baseFatText = "6.5"
                            baseSnfText = "9.0"
                            fatRateFactorText = "8.5"
                            snfRateFactorText = "4.5"
                            pricingMode = "FAT_SNF"
                        }
                        PresetChip(label = "Local Flat Rate") {
                            cowRateText = "55.0"
                            buffaloRateText = "75.0"
                            pricingMode = "DIRECT"
                        }
                    }
                }
            }
        }

        // Interactive Live Price Simulator with Product Yields
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = OceanBlueLight.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, OceanBlueAccent.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = OceanBlueAccent)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live Yield Simulator & Rate Lab",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = RoyalBluePrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                FilterChip(
                                    selected = simMilkType == "COW",
                                    onClick = { simMilkType = "COW" },
                                    label = { Text("Cow", fontSize = 11.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                FilterChip(
                                    selected = simMilkType == "BUFFALO",
                                    onClick = { simMilkType = "BUFFALO" },
                                    label = { Text("Buffalo", fontSize = 11.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Test FAT: ${String.format(Locale.US, "%.1f", simFat)}%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Standard: $baseFatText%", fontSize = 11.sp, color = TextSecondary)
                        }
                        Slider(
                            value = simFat,
                            onValueChange = { simFat = it },
                            valueRange = 2.5f..10.0f,
                            steps = 75,
                            colors = SliderDefaults.colors(
                                thumbColor = OceanBlueAccent,
                                activeTrackColor = OceanBlueAccent
                            )
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Test SNF: ${String.format(Locale.US, "%.1f", simSnf)}%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Standard: $baseSnfText%", fontSize = 11.sp, color = TextSecondary)
                        }
                        Slider(
                            value = simSnf,
                            onValueChange = { simSnf = it },
                            valueRange = 6.5f..11.0f,
                            steps = 45,
                            colors = SliderDefaults.colors(
                                thumbColor = DairyGreen,
                                activeTrackColor = DairyGreen
                            )
                        )
                    }

                    // Yield Output Diagnostics Row
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🧀 Paneer Yield", fontSize = 10.sp, color = TextSecondary)
                                Text("${String.format(Locale.US, "%.1f", livePaneerYield)} g/L", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🍬 Khoya Yield", fontSize = 10.sp, color = TextSecondary)
                                Text("${String.format(Locale.US, "%.1f", liveKhoaYield)} g/L", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🏺 Ghee Yield", fontSize = 10.sp, color = TextSecondary)
                                Text("${String.format(Locale.US, "%.1f", liveGheeYield)} g/L", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Calculated Price per Liter:", fontSize = 12.sp, color = TextSecondary)
                                Text(
                                    text = "₹${String.format(Locale.US, "%.2f", calculatedPrice)} / Liter",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DairyGreen
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Sample 10 Liters:", fontSize = 11.sp, color = TextSecondary)
                                Text(
                                    text = "₹${String.format(Locale.US, "%.2f", calculatedPrice * 10)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBluePrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Master Save & Apply Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val cow = cowRateText.toDoubleOrNull() ?: 55.0
                        val buff = buffaloRateText.toDoubleOrNull() ?: 75.0
                        val fat = baseFatText.toDoubleOrNull() ?: 3.5
                        val snf = baseSnfText.toDoubleOrNull() ?: 8.5
                        val fatMult = fatRateFactorText.toDoubleOrNull() ?: 7.5
                        val snfMult = snfRateFactorText.toDoubleOrNull() ?: 4.0
                        val pRate = paneerRateText.toDoubleOrNull() ?: 360.0
                        val kRate = khoaRateText.toDoubleOrNull() ?: 330.0

                        viewModel.saveCustomRateFormula(
                            cowRate = cow,
                            buffaloRate = buff,
                            baseFat = fat,
                            baseSnf = snf,
                            fatFactor = fatMult,
                            snfFactor = snfMult,
                            pricingMode = pricingMode,
                            paneerRatePerKg = pRate,
                            khoaRatePerKg = kRate
                        )
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Rates & Formula", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val cow = cowRateText.toDoubleOrNull() ?: 55.0
                        val buff = buffaloRateText.toDoubleOrNull() ?: 75.0
                        customers.forEach { cust ->
                            viewModel.saveCustomer(
                                id = cust.id,
                                name = cust.name,
                                mobile = cust.mobile,
                                address = cust.address,
                                type = cust.type,
                                rate = cust.rate,
                                milkType = cust.milkType,
                                defaultQty = cust.defaultQuantity,
                                defaultShift = cust.defaultShift,
                                route = cust.route,
                                notes = cust.notes,
                                cowQuantity = cust.cowQuantity,
                                cowRate = cow,
                                buffaloQuantity = cust.buffaloQuantity,
                                buffaloRate = buff
                            )
                        }
                        viewModel.showMessage("✓ Batch updated base rates for all ${customers.size} customers!")
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DairyGreen)
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp), tint = DairyGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apply to All Customers", fontWeight = FontWeight.Bold, color = DairyGreen, fontSize = 12.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MODULE 2: STAFF ROSTER, ATTENDANCE & PAYROLL
// -------------------------------------------------------------
@Composable
fun StaffAndAttendanceTab(
    viewModel: MilkMateViewModel,
    staffList: List<StaffEntity>,
    todayAttendance: List<StaffAttendanceEntity>,
    allAttendance: List<StaffAttendanceEntity>,
    staffPayments: List<StaffPaymentEntity>
) {
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var showAdvanceDialog by remember { mutableStateOf<StaffEntity?>(null) }
    val context = LocalContext.current

    val presentCount = todayAttendance.count { it.status == "PRESENT" }
    val totalStaff = staffList.size
    val totalPayrollDisbursed = staffPayments.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Staff Hero Metrics
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Staff Roster & Wage Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Today: $presentCount / $totalStaff Present on Duty",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Button(
                            onClick = { showAddStaffDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenOrange),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Staff", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricTile(
                            title = "Active Staff",
                            value = "$totalStaff Members",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Present Today",
                            value = "$presentCount On-Field",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Payroll Paid",
                            value = "₹${totalPayrollDisbursed.toInt()}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Attendance Action Bar
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Attendance Register",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Quick 1-touch attendance logger",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            viewModel.markAllStaffAttendance("PRESENT")
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = DairyGreenLight),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark All Present", fontWeight = FontWeight.Bold, color = DairyGreen, fontSize = 12.sp)
                    }
                }
            }
        }

        // Staff List Cards
        if (staffList.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Group,
                    title = "No Staff Members Added",
                    description = "Add drivers, delivery boys, and dairy workers to track daily attendance and wages."
                )
            }
        } else {
            items(staffList) { staff ->
                val currentAttendance = todayAttendance.find { it.staffId == staff.id }?.status ?: "ABSENT"
                val staffAttRecords = allAttendance.filter { it.staffId == staff.id }
                val staffPaidRecords = staffPayments.filter { it.staffId == staff.id }
                val totalAdvances = staffPaidRecords.sumOf { it.amount }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(RoyalBlueLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = staff.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = RoyalBluePrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = staff.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = TextPrimary
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (staff.role == "SUPERVISOR") Color(0xFF673AB7).copy(alpha = 0.15f) else RoyalBlueLight
                                        ) {
                                            Text(
                                                text = staff.displayRole(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (staff.role == "SUPERVISOR") Color(0xFF673AB7) else RoyalBluePrimary,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "PIN: ${staff.pin} • ${staff.assignedRoute.ifBlank { "All Routes" }} • Perms: ${if (staff.permissions.contains("ALL")) "FULL ACCESS" else staff.permissions}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = FreshGoldLight
                            ) {
                                Text(
                                    text = if (staff.salaryType == "MONTHLY") "₹${staff.monthlySalary.toInt()}/mo" else "₹${staff.dailyWage.toInt()}/day",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarmHoney,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Attendance Status Selector Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today's Status:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusPillButton(
                                    label = "P",
                                    title = "Present",
                                    isSelected = currentAttendance == "PRESENT",
                                    activeColor = DairyGreen
                                ) {
                                    viewModel.markStaffAttendance(staff.id, "PRESENT")
                                }
                                StatusPillButton(
                                    label = "H",
                                    title = "Half Day",
                                    isSelected = currentAttendance == "HALF_DAY",
                                    activeColor = GoldenOrange
                                ) {
                                    viewModel.markStaffAttendance(staff.id, "HALF_DAY")
                                }
                                StatusPillButton(
                                    label = "A",
                                    title = "Absent",
                                    isSelected = currentAttendance == "ABSENT",
                                    activeColor = DangerRed
                                ) {
                                    viewModel.markStaffAttendance(staff.id, "ABSENT")
                                }
                                StatusPillButton(
                                    label = "L",
                                    title = "Leave",
                                    isSelected = currentAttendance == "LEAVE",
                                    activeColor = OceanBlueAccent
                                ) {
                                    viewModel.markStaffAttendance(staff.id, "LEAVE")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = BorderSubtle)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Actions Row: Disburse Advance, WhatsApp Salary Slip, Call
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAdvanceDialog = staff },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pay Advance", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = {
                                    val daysPresent = staffAttRecords.count { it.status == "PRESENT" }
                                    val halfDays = staffAttRecords.count { it.status == "HALF_DAY" }
                                    val totalEarned = if (staff.salaryType == "MONTHLY") {
                                        (staff.monthlySalary / 30.0) * (daysPresent + halfDays * 0.5)
                                    } else {
                                        staff.dailyWage * (daysPresent + halfDays * 0.5)
                                    }
                                    val netPayable = (totalEarned - totalAdvances).coerceAtLeast(0.0)

                                    val slipText = """
                                        📋 *SALARY & ATTENDANCE SLIP*
                                        👤 *Staff:* ${staff.name} (${staff.role})
                                        📅 *Month:* ${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())}
                                        ---------------------------
                                        ✅ *Days Present:* $daysPresent days
                                        ⏳ *Half Days:* $halfDays days
                                        💵 *Gross Earned:* ₹${String.format(Locale.US, "%.0f", totalEarned)}
                                        📉 *Advance Deducted:* ₹${String.format(Locale.US, "%.0f", totalAdvances)}
                                        ---------------------------
                                        💰 *Net Balance Payable:* ₹${String.format(Locale.US, "%.0f", netPayable)}
                                        
                                        _MilkMate Dairy Management System_
                                    """.trimIndent()

                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://api.whatsapp.com/send?phone=91${staff.mobile.takeLast(10)}&text=${Uri.encode(slipText)}")
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = DairyGreenLight),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp Slip", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                            }

                            if (staff.mobile.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${staff.mobile}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call", tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Staff Dialog
    if (showAddStaffDialog) {
        AddStaffDialog(
            businessMode = business?.businessMode ?: "DAIRY_FARM",
            onDismiss = { showAddStaffDialog = false },
            onSave = { staffId, name, mobile, pin, role, permissions, route, monthly, daily, salaryType, customTitle ->
                viewModel.saveStaff(
                    id = staffId,
                    name = name,
                    mobile = mobile,
                    pin = pin,
                    role = role,
                    permissions = permissions,
                    assignedRoute = route,
                    monthlySalary = monthly,
                    dailyWage = daily,
                    salaryType = salaryType,
                    customRoleTitle = customTitle
                )
                showAddStaffDialog = false
                viewModel.showMessage("✓ Added new staff: $name")
            }
        )
    }

    // Advance Disbursement Dialog
    if (showAdvanceDialog != null) {
        val targetStaff = showAdvanceDialog!!
        var advanceAmountText by remember { mutableStateOf("1000") }
        var advanceNote by remember { mutableStateOf("Advance cash against wage") }

        AlertDialog(
            onDismissRequest = { showAdvanceDialog = null },
            title = { Text("Disburse Advance to ${targetStaff.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Record advance cash disbursement. This will automatically deduct from their next salary slip.")
                    OutlinedTextField(
                        value = advanceAmountText,
                        onValueChange = { advanceAmountText = it },
                        label = { Text("Advance Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = advanceNote,
                        onValueChange = { advanceNote = it },
                        label = { Text("Note / Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = advanceAmountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.recordStaffPayment(
                                staffId = targetStaff.id,
                                staffName = targetStaff.name,
                                type = "ADVANCE",
                                amount = amount,
                                notes = advanceNote
                            )
                        }
                        showAdvanceDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenOrange)
                ) {
                    Text("Confirm Advance")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdvanceDialog = null }) { Text("Cancel") }
            }
        )
    }
}

// -------------------------------------------------------------
// MODULE 3: SMART ROUTE & DELIVERY ALLOCATION MATRIX
// -------------------------------------------------------------
@Composable
fun RouteAllotmentTab(
    viewModel: MilkMateViewModel,
    customers: List<CustomerEntity>,
    staffList: List<StaffEntity>
) {
    var selectedRouteFilter by remember { mutableStateOf("ALL") }
    var selectedCustomerIds by remember { mutableStateOf(setOf<String>()) }
    var targetNewRoute by remember { mutableStateOf("Route 1") }
    var targetStaffId by remember { mutableStateOf("") }

    val distinctRoutes = remember(customers) {
        val routes = customers.map { it.route.ifBlank { "Unassigned" } }.distinct().sorted()
        listOf("ALL") + routes
    }

    val filteredCustomers = remember(customers, selectedRouteFilter) {
        if (selectedRouteFilter == "ALL") customers
        else customers.filter { (it.route.ifBlank { "Unassigned" }) == selectedRouteFilter }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Route Matrix Hero
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = RoyalBluePrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Smart Route Allocation & Dispatch Matrix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Assign delivery lines, delivery partners, and re-allocate customers in bulk",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricTile(
                            title = "Active Routes",
                            value = "${(distinctRoutes.size - 1).coerceAtLeast(0)} Lines",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Mapped Stops",
                            value = "${customers.count { it.route.isNotBlank() }} Stops",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Unassigned",
                            value = "${customers.count { it.route.isBlank() }} Users",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Bulk Reallocation Action Drawer
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Bulk Customer Route Re-allotment",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Selected: ${selectedCustomerIds.size} customers",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = targetNewRoute,
                            onValueChange = { targetNewRoute = it },
                            label = { Text("Assign to Route Name") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                if (selectedCustomerIds.isNotEmpty()) {
                                    val targetStaff = staffList.find { it.id == targetStaffId }
                                    viewModel.allotRouteAndStaffToCustomers(
                                        customerIds = selectedCustomerIds.toList(),
                                        route = targetNewRoute,
                                        staffId = targetStaff?.id ?: "",
                                        staffName = targetStaff?.name ?: ""
                                    )
                                    selectedCustomerIds = emptySet()
                                }
                            },
                            enabled = selectedCustomerIds.isNotEmpty(),
                            modifier = Modifier.align(Alignment.CenterVertically).height(54.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DairyGreen)
                        ) {
                            Text("Allot (${selectedCustomerIds.size})", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                selectedCustomerIds = filteredCustomers.map { it.id }.toSet()
                            }
                        ) {
                            Text("Select All in View (${filteredCustomers.size})", fontSize = 12.sp)
                        }

                        if (selectedCustomerIds.isNotEmpty()) {
                            TextButton(onClick = { selectedCustomerIds = emptySet() }) {
                                Text("Clear Selection", fontSize = 12.sp, color = DangerRed)
                            }
                        }
                    }
                }
            }
        }

        // Route Filter Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = distinctRoutes.indexOf(selectedRouteFilter).coerceAtLeast(0),
                containerColor = SurfaceCard,
                edgePadding = 8.dp,
                divider = {}
            ) {
                distinctRoutes.forEach { routeName ->
                    Tab(
                        selected = selectedRouteFilter == routeName,
                        onClick = { selectedRouteFilter = routeName },
                        text = {
                            Text(
                                text = routeName,
                                fontWeight = if (selectedRouteFilter == routeName) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }
        }

        // Customer Selection List
        items(filteredCustomers) { cust ->
            val isChecked = selectedCustomerIds.contains(cust.id)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isChecked) RoyalBlueLight else SurfaceCard
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isChecked) RoyalBluePrimary else BorderSubtle
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedCustomerIds = if (isChecked) {
                            selectedCustomerIds - cust.id
                        } else {
                            selectedCustomerIds + cust.id
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                selectedCustomerIds = if (checked) {
                                    selectedCustomerIds + cust.id
                                } else {
                                    selectedCustomerIds - cust.id
                                }
                            }
                        )
                        Column {
                            Text(
                                text = cust.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Route: ${cust.route.ifBlank { "Unassigned" }} • ${cust.defaultShift}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${cust.defaultQuantity}L ${cust.milkType}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = RoyalBluePrimary
                        )
                        Text(
                            text = "₹${cust.cowRate.toInt()}/L",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MODULE 4: SHIFTS & DISPATCH OPERATIONS
// -------------------------------------------------------------
@Composable
fun ShiftsAndDispatchTab(
    viewModel: MilkMateViewModel,
    business: BusinessEntity?,
    todayDeliveries: List<DeliveryEntity>
) {
    val selectedShift by viewModel.selectedShift.collectAsStateWithLifecycle()
    val morningDeliveries = todayDeliveries.filter { it.shift == "MORNING" }
    val eveningDeliveries = todayDeliveries.filter { it.shift == "EVENING" }

    val morningLiters = morningDeliveries.sumOf { it.quantityLiters }
    val eveningLiters = eveningDeliveries.sumOf { it.quantityLiters }
    val morningCompleted = morningDeliveries.count { it.isDelivered }
    val eveningCompleted = eveningDeliveries.count { it.isDelivered }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Shift Switcher Hero Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedShift == "MORNING") FreshGold else RoyalBluePrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (selectedShift == "MORNING") "🌅 Morning Shift Active" else "🌆 Evening Shift Active",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Dispatch & Counter Real-Time Monitoring",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Button(
                            onClick = {
                                val next = if (selectedShift == "MORNING") "EVENING" else "MORNING"
                                viewModel.setShift(next)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (selectedShift == "MORNING") "Switch to Evening" else "Switch to Morning",
                                color = if (selectedShift == "MORNING") FreshGold else RoyalBluePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricTile(
                            title = "Morning Volume",
                            value = "${String.format(Locale.US, "%.1f", morningLiters)} Liters",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Evening Volume",
                            value = "${String.format(Locale.US, "%.1f", eveningLiters)} Liters",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Total Stops",
                            value = "${todayDeliveries.size} Drops",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Shift Breakdown Comparison Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Shift Dispatch Progress & Crate Load",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    ShiftProgressBar(
                        title = "🌅 Morning Dispatch (05:00 - 09:00)",
                        completed = morningCompleted,
                        total = morningDeliveries.size,
                        liters = morningLiters,
                        color = FreshGold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ShiftProgressBar(
                        title = "🌆 Evening Dispatch (16:30 - 20:30)",
                        completed = eveningCompleted,
                        total = eveningDeliveries.size,
                        liters = eveningLiters,
                        color = RoyalBluePrimary
                    )
                }
            }
        }

        // Vehicle & Tanker Dispatch Checklist
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pre-Dispatch Van & Crate Checklist",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ChecklistItem(title = "Aluminum / Stainless Milk Cans Sealed & Sanitized", initial = true)
                    ChecklistItem(title = "Measuring Lactometers & FAT Sampling Kits Loaded", initial = true)
                    ChecklistItem(title = "Ice Gel Packs & Insulated Blankets Placed", initial = true)
                    ChecklistItem(title = "QR Payment Cards & Khata Bill Receipt Books Handed", initial = false)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MODULE 5: PRODUCTS & VALUE-ADDED INVENTORY CATALOG
// -------------------------------------------------------------
fun isConsumableOrFeed(item: InventoryItemEntity): Boolean {
    val name = item.itemName.lowercase()
    return name.contains("feed") || name.contains("chokar") || name.contains("bran") ||
           name.contains("silage") || name.contains("mineral") || name.contains("calcium") ||
           name.contains("medicine") || name.contains("deworm") || name.contains("straw") ||
           name.contains("bag") || name.contains("molasses") || name.contains("salt") ||
           name.contains("fodder") || name.contains("khali") || name.contains("cake") ||
           name.contains("makka") || name.contains("meal") || name.contains("doc") ||
           name.contains("khal") || name.contains("consumable")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsCatalogTab(
    viewModel: MilkMateViewModel,
    inventoryItems: List<InventoryItemEntity>
) {
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val displayItems = remember(inventoryItems, business?.businessMode) {
        BusinessModeFeatures.filterInventoryForMode(inventoryItems, business?.businessMode)
    }
    var catalogCategoryTab by remember { mutableIntStateOf(0) } // 0: Sale Products, 1: Farm Consumables & Feed, 2: All Items
    var showAddProductDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<InventoryItemEntity?>(null) }

    val saleProducts = remember(displayItems) { displayItems.filter { !isConsumableOrFeed(it) } }
    val consumableItems = remember(displayItems) { displayItems.filter { isConsumableOrFeed(it) } }

    val activeList = when (catalogCategoryTab) {
        0 -> saleProducts
        1 -> consumableItems
        else -> displayItems
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Hero Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ForestGreenAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (catalogCategoryTab) {
                                    0 -> "Dairy Products for Sale Catalog"
                                    1 -> "Farm Consumables & Cattle Feed"
                                    else -> "Complete Product & Feed Catalog"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = when (catalogCategoryTab) {
                                    0 -> "Paneer, Ghee, Curd, Butter, Khoya & SKUs"
                                    1 -> "Chokar (Wheat Bran), Oil Cakes & Feed Inputs"
                                    else -> "${saleProducts.size} Sale Products • ${consumableItems.size} Farm Feeds"
                                },
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { showAddProductDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = ForestGreenAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Item", color = ForestGreenAccent, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, maxLines = 1, softWrap = false)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricTile(
                            title = if (catalogCategoryTab == 0) "Sale SKUs" else if (catalogCategoryTab == 1) "Consumable Items" else "Total SKUs",
                            value = "${activeList.size} Items",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Low Stock Alert",
                            value = "${activeList.count { it.stockAlertStatus != "NORMAL" || it.currentStock <= 5.0 }} Critical",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Stock Value",
                            value = "₹${activeList.sumOf { (it.currentStock * it.costPerUnit).toInt() }}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Separate Category Tab Bar
        item {
            ScrollableTabRow(
                selectedTabIndex = catalogCategoryTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ForestGreenAccent,
                edgePadding = 0.dp,
                divider = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = catalogCategoryTab == 0,
                    onClick = { catalogCategoryTab = 0 },
                    text = {
                        Text(
                            "🛍️ Sale Products (${saleProducts.size})",
                            fontWeight = if (catalogCategoryTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                )
                Tab(
                    selected = catalogCategoryTab == 1,
                    onClick = { catalogCategoryTab = 1 },
                    text = {
                        Text(
                            "🌾 Consumables & Feed (${consumableItems.size})",
                            fontWeight = if (catalogCategoryTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                )
                Tab(
                    selected = catalogCategoryTab == 2,
                    onClick = { catalogCategoryTab = 2 },
                    text = {
                        Text(
                            "📑 All (${displayItems.size})",
                            fontWeight = if (catalogCategoryTab == 2) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                )
            }
        }

        if (activeList.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Inventory2,
                    title = if (catalogCategoryTab == 0) "No Dairy Products for Sale" else if (catalogCategoryTab == 1) "No Farm Consumables Found" else "Product Catalog Empty",
                    description = if (catalogCategoryTab == 0)
                        "Click '+ Add Item' above to add customer products like Paneer, Desi Ghee, Curd, or Chaas."
                    else if (catalogCategoryTab == 1)
                        "Click '+ Add Item' above to add farm consumables like Wheat Bran (Chokar), Cattle Feed, or Mineral Mixture."
                    else
                        "Add items to manage live stock and pricing."
                )
            }
        } else {
            items(activeList, key = { it.id }) { item ->
                val isLowStock = item.stockAlertStatus != "NORMAL" || item.currentStock <= item.alertDaysThreshold.toDouble()
                val isFeed = isConsumableOrFeed(item)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.itemName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isFeed) FreshGoldLight else RoyalBlueLight
                                ) {
                                    Text(
                                        text = if (isFeed) "🌾 CONSUMABLE" else "🛍️ FOR SALE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFeed) WarmHoney else RoyalBluePrimary,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }

                                if (isLowStock) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = DangerRedLight
                                    ) {
                                        Text(
                                            text = "LOW STOCK",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DangerRed,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Cost: ₹${item.costPerUnit.toInt()} / ${item.unit}" +
                                       if (item.dailyUsage != null && item.dailyUsage > 0) " • Daily: ${item.dailyUsage} ${item.unit}/day" else "",
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        }

                        // Inline Stock Adjuster
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceBg,
                                modifier = Modifier.size(32.dp).clickable {
                                    if (item.currentStock > 0) {
                                        viewModel.updateInventoryStock(
                                            itemId = item.id,
                                            quantityChange = -1.0,
                                            transactionType = "SALE",
                                            notes = "Catalog quick decrement"
                                        )
                                    }
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                                }
                            }

                            Text(
                                text = "${String.format(Locale.US, "%.1f", item.currentStock)} ${item.unit}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = if (isLowStock) DangerRed else TextPrimary
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DairyGreenLight,
                                modifier = Modifier.size(32.dp).clickable {
                                    viewModel.updateInventoryStock(
                                        itemId = item.id,
                                        quantityChange = 1.0,
                                        transactionType = "PURCHASE",
                                        notes = "Catalog quick increment"
                                    )
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                                }
                            }

                            IconButton(
                                onClick = { itemToDelete = item },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Item", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (itemToDelete != null) {
        val target = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DangerRed) },
            title = { Text("Delete Catalog Item", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete '${target.itemName}' from inventory? Current stock is ${target.currentStock} ${target.unit}.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteInventoryItem(target.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete Item", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddProductDialog) {
        val showFeed = BusinessModeFeatures.showCattleFeed(business?.businessMode)
        AddProductDialog(
            showFeed = showFeed,
            onDismiss = { showAddProductDialog = false },
            onSave = { name, unit, stock, cost, dailyUsage ->
                viewModel.saveInventoryItem(
                    id = null,
                    itemName = name,
                    unit = unit,
                    openingStock = stock,
                    costPerUnit = cost,
                    dailyUsage = dailyUsage
                )
                showAddProductDialog = false
                viewModel.showMessage("✓ Added $name to catalog!")
            }
        )
    }
}

// -------------------------------------------------------------
// MODULE 6: SMART ALERTS & AUTOMATED REMINDERS
// -------------------------------------------------------------
@Composable
fun SmartNotificationsTab(
    viewModel: MilkMateViewModel
) {
    NotificationCenterContent(
        viewModel = viewModel
    )
}

/* Legacy Notifications Code Removed in favor of unified NotificationCenterContent */
/*

    // State variables synchronized with session
    var notifMasterEnabled by remember(session.notificationsEnabled) { mutableStateOf(session.notificationsEnabled) }
    var morningEnabled by remember(session.morningReminderEnabled) { mutableStateOf(session.morningReminderEnabled) }
    var morningTime by remember(session.morningReminderTime) { mutableStateOf(session.morningReminderTime) }
    var afternoonEnabled by remember(session.afternoonReminderEnabled) { mutableStateOf(session.afternoonReminderEnabled) }
    var afternoonTime by remember(session.afternoonReminderTime) { mutableStateOf(session.afternoonReminderTime) }
    var eveningEnabled by remember(session.eveningReminderEnabled) { mutableStateOf(session.eveningReminderEnabled) }
    var eveningTime by remember(session.eveningReminderTime) { mutableStateOf(session.eveningReminderTime) }
    var advanceMinutes by remember(session.advanceAlertMinutes) { mutableIntStateOf(session.advanceAlertMinutes) }

    var milkingEnabled by remember(session.milkingReminderEnabled) { mutableStateOf(session.milkingReminderEnabled) }
    var deliveryEnabled by remember(session.deliveryReminderEnabled) { mutableStateOf(session.deliveryReminderEnabled) }
    var vaccineEnabled by remember(session.vaccineAlertEnabled) { mutableStateOf(session.vaccineAlertEnabled) }
    var dewormingEnabled by remember(session.dewormingAlertEnabled) { mutableStateOf(session.dewormingAlertEnabled) }
    var breedingEnabled by remember(session.breedingCalvingAlertEnabled) { mutableStateOf(session.breedingCalvingAlertEnabled) }
    var paymentDueEnabled by remember(session.paymentDueAlertsEnabled) { mutableStateOf(session.paymentDueAlertsEnabled) }
    var paymentDueThresholdText by remember(session.paymentDueThreshold) { mutableStateOf(session.paymentDueThreshold.toInt().toString()) }
    var lowStockEnabled by remember(session.lowStockAlertsEnabled) { mutableStateOf(session.lowStockAlertsEnabled) }
    var soundEnabled by remember(session.soundAlertsEnabled) { mutableStateOf(session.soundAlertsEnabled) }
    var vibrationEnabled by remember(session.vibrationAlertsEnabled) { mutableStateOf(session.vibrationAlertsEnabled) }
    var dailySummaryEnabled by remember(session.dailySummaryAlertsEnabled) { mutableStateOf(session.dailySummaryAlertsEnabled) }
    var dailySummaryTime by remember(session.dailySummaryTime) { mutableStateOf(session.dailySummaryTime) }

    val currentThreshold = paymentDueThresholdText.toDoubleOrNull() ?: 500.0
    val now = remember { System.currentTimeMillis() }
    val sdf = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    // Generate Dynamic Live Alerts
    val generatedAlerts = remember(
        customers, todayDeliveries, inventoryItems, cattleList, vaccinations, dewormings, breedings, milkingRecords,
        currentThreshold, now
    ) {
        val list = mutableListOf<OperationalAlert>()

        // 1. Milking Logs Alerts
        val todayMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayMilking = milkingRecords.filter { it.dateEpochMidnight >= todayMidnight }
        val lactatingCattle = cattleList.filter { it.lactationStage.equals("LACTATING", true) }

        if (lactatingCattle.isNotEmpty()) {
            val morningMilked = todayMilking.filter { it.shift == "MORNING" }.map { it.cattleId }.toSet()
            val morningMissing = lactatingCattle.filter { it.id !in morningMilked }
            if (morningMissing.isNotEmpty()) {
                list.add(
                    OperationalAlert(
                        id = "MILK_MORNING_MISSING",
                        category = AlertFilter.MILKING,
                        title = "Morning Milking Pending",
                        message = "${morningMissing.size} lactating cows/buffs missing Morning Shift logs.",
                        timeLabel = "Morning Shift",
                        priority = AlertPriority.HIGH,
                        icon = Icons.Default.Pets,
                        iconBg = RoyalBluePrimary
                    )
                )
            }
        }

        // 2. Deliveries Alerts
        val morningDeliveries = todayDeliveries.filter { it.shift == "MORNING" }
        val morningPending = morningDeliveries.filter { !it.isDelivered }
        if (morningPending.isNotEmpty()) {
            list.add(
                OperationalAlert(
                    id = "DELIVERY_MORNING_PENDING",
                    category = AlertFilter.DELIVERIES,
                    title = "Morning Deliveries Incomplete",
                    message = "${morningPending.size} of ${morningDeliveries.size} customer delivery bottles pending.",
                    timeLabel = "Live Route",
                    priority = AlertPriority.HIGH,
                    icon = Icons.Default.LocalShipping,
                    iconBg = FreshGold
                )
            )
        }

        // 3. Customer Payment Overdue Alerts
        val overdueCustomers = customers.filter { it.outstandingBalance > currentThreshold }
            .sortedByDescending { it.outstandingBalance }
        overdueCustomers.take(5).forEach { cust ->
            list.add(
                OperationalAlert(
                    id = "PAY_DUE_${cust.id}",
                    category = AlertFilter.PAYMENTS,
                    title = "Payment Overdue: ${cust.name}",
                    message = "Outstanding bill balance is ₹${cust.outstandingBalance.toInt()} (Route: ${cust.route.ifBlank { "Main" }}).",
                    timeLabel = "Due ₹${cust.outstandingBalance.toInt()}",
                    priority = if (cust.outstandingBalance > currentThreshold * 2) AlertPriority.CRITICAL else AlertPriority.HIGH,
                    icon = Icons.Default.AccountBalanceWallet,
                    iconBg = DangerRed,
                    actionLabel = "WhatsApp Blaster 💬",
                    onAction = {
                        try {
                            val msg = "Namaste ${cust.name} ji, gentle reminder from dairy for your outstanding milk bill of ₹${cust.outstandingBalance.toInt()}. Kindly arrange payment via UPI or cash. Thank you!"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=+91${cust.mobile.takeLast(10)}&text=${Uri.encode(msg)}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            viewModel.showMessage("WhatsApp link ready for ${cust.name}")
                        }
                    }
                )
            )
        }

        // 4. Low Feed Stock Alert
        val lowStockItems = inventoryItems.filter { it.stockAlertStatus != "NORMAL" || it.currentStock <= 5.0 }
        if (lowStockItems.isNotEmpty()) {
            list.add(
                OperationalAlert(
                    id = "INV_LOW_FEED",
                    category = AlertFilter.FEED_STOCK,
                    title = "🌾 Low Feed Stock Warning",
                    message = "${lowStockItems.size} items (${lowStockItems.take(2).joinToString { it.itemName }}) running low in barn inventory.",
                    timeLabel = "Stock Low",
                    priority = AlertPriority.HIGH,
                    icon = Icons.Default.Inventory2,
                    iconBg = GoldenOrange
                )
            )
        }

        // 5. Breeding & Calving Radar
        cattleList.forEach { c ->
            if (c.expectedCalvingDate > 0) {
                val daysToCalve = (c.expectedCalvingDate - now) / (1000L * 60L * 60L * 24L)
                if (daysToCalve in -5..14) {
                    list.add(
                        OperationalAlert(
                            id = "CALV_${c.id}",
                            category = AlertFilter.HERD_HEALTH,
                            title = if (daysToCalve <= 0) "🍼 Expected Calving Alert!" else "🤰 Calving Countdown (${c.tagNumber})",
                            message = "${c.name.ifBlank { "Animal" }} (#${c.tagNumber}) expected to calve ${if (daysToCalve <= 0) "today / overdue" else "in $daysToCalve days (${sdf.format(Date(c.expectedCalvingDate))})"}.",
                            timeLabel = "Maternity Alert",
                            priority = AlertPriority.CRITICAL,
                            icon = Icons.Default.ChildCare,
                            iconBg = WarmHoney
                        )
                    )
                }
            }
        }

        // 6. End-of-Day Closing
        list.add(
            OperationalAlert(
                id = "EOD_CLOSING",
                category = AlertFilter.ALL,
                title = "📊 Daily Ledger & Closing Wrap-up",
                message = "Log all daily feed, fuel, wages, and vendor expenses before closing.",
                timeLabel = "$dailySummaryTime Summary",
                priority = AlertPriority.INFO,
                icon = Icons.Default.ReceiptLong,
                iconBg = ForestGreenAccent
            )
        )

        list
    }

    val filteredAlerts = remember(generatedAlerts, selectedFilter) {
        if (selectedFilter == AlertFilter.ALL) generatedAlerts
        else generatedAlerts.filter { it.category == selectedFilter }
    }

    val morningHours = listOf("03:00", "03:30", "04:00", "04:30", "05:00", "05:30", "06:00", "06:30", "07:00", "07:30", "08:00")
    val afternoonHours = listOf("12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00")
    val eveningHours = listOf("16:00", "16:30", "17:00", "17:30", "18:00", "18:30", "19:00", "19:30", "20:00", "21:00")
    val closingHours = listOf("18:00", "19:00", "20:00", "20:30", "21:00", "21:30", "22:00", "23:00")
    val advanceOptions = listOf(
        0 to "Event time",
        15 to "15m before",
        30 to "30m before",
        60 to "1h before",
        120 to "2h before",
        240 to "4h before"
    )

    fun saveAllPreferences() {
        viewModel.updateNotificationPreferences(
            notificationsEnabled = notifMasterEnabled,
            morningEnabled = morningEnabled,
            morningTime = morningTime,
            afternoonEnabled = afternoonEnabled,
            afternoonTime = afternoonTime,
            eveningEnabled = eveningEnabled,
            eveningTime = eveningTime,
            advanceMinutes = advanceMinutes,
            milkingEnabled = milkingEnabled,
            deliveryEnabled = deliveryEnabled,
            vaccineEnabled = vaccineEnabled,
            dewormingEnabled = dewormingEnabled,
            breedingEnabled = breedingEnabled,
            paymentDueEnabled = paymentDueEnabled,
            paymentDueThreshold = currentThreshold,
            lowStockEnabled = lowStockEnabled,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            dailySummaryEnabled = dailySummaryEnabled,
            dailySummaryTime = dailySummaryTime
        )
        viewModel.showMessage("✅ Notification preferences updated & saved!")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header & Save Control
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = OceanBlueAccent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White)
                        }
                        Column {
                            Text(
                                text = "Smart Alerts & Notification Center",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (notifMasterEnabled) "🟢 Master Engine Active • ${generatedAlerts.size} Signals" else "🔴 Master Alerts Muted",
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                viewModel.showMessage("🔔 Test Notification: Shift & Payment alerts active!")
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Test", tint = Color.White)
                        }

                        Button(
                            onClick = { saveAllPreferences() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save ✓", color = OceanBlueAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatMetricTile(
                        title = "Pending Alerts",
                        value = "${generatedAlerts.size}",
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricTile(
                        title = "Overdue Bills",
                        value = "${customers.count { it.outstandingBalance > currentThreshold }}",
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricTile(
                        title = "Shift Reminders",
                        value = if (morningEnabled || eveningEnabled) "Active" else "Off",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Sub-Tab Switcher
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = RoyalBluePrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("🚨 Live Radar (${generatedAlerts.size})", fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("⏰ Shift Hours", fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("⚙️ Alert Modules", fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
            )
        }

        when (activeTab) {
            0 -> {
                // SUB-TAB 0: Live Alert Radar
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(AlertFilter.entries) { filter ->
                            val label = when (filter) {
                                AlertFilter.ALL -> "All (${generatedAlerts.size})"
                                AlertFilter.HERD_HEALTH -> "🩺 Health"
                                AlertFilter.MILKING -> "🥛 Milking"
                                AlertFilter.DELIVERIES -> "🚚 Delivery"
                                AlertFilter.PAYMENTS -> "💰 Due Bills"
                                AlertFilter.FEED_STOCK -> "🌾 Feed"
                            }
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    if (filteredAlerts.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("All Operational Items Clear! ✓", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("No active alerts in selected filter.", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredAlerts, key = { it.id }) { alert ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (alert.priority) {
                                            AlertPriority.CRITICAL -> DangerRedLight
                                            AlertPriority.HIGH -> FreshGoldLight
                                            AlertPriority.NORMAL -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            AlertPriority.INFO -> MintGreenLight
                                        }
                                    ),
                                    border = if (alert.priority == AlertPriority.CRITICAL) androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)) else null
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(alert.iconBg, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(alert.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = alert.iconBg.copy(alpha = 0.15f)
                                                ) {
                                                    Text(alert.timeLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = alert.iconBg, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(alert.message, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)

                                            if (alert.actionLabel != null && alert.onAction != null) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                FilledTonalButton(
                                                    onClick = alert.onAction,
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text(alert.actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

            1 -> {
                // SUB-TAB 1: Shift Hours & Alarm Clock
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "⏰ Operational Shift Timings & Clock Reminders",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = RoyalBluePrimary
                        )
                    }

                    item {
                        ShiftTimeCard(
                            title = "☀️ Morning Shift & Milking Hour",
                            subtitle = "Reminder triggers at $morningTime for morning herd milking & routes",
                            enabled = morningEnabled,
                            onToggle = {
                                morningEnabled = it
                                saveAllPreferences()
                            },
                            selectedTime = morningTime,
                            timeOptions = morningHours,
                            onSelectTime = {
                                morningTime = it
                                saveAllPreferences()
                            }
                        )
                    }

                    item {
                        ShiftTimeCard(
                            title = "🌤️ Afternoon Milking & Chilling Round",
                            subtitle = "Mid-day reminder at $afternoonTime for high-yield dairy herds",
                            enabled = afternoonEnabled,
                            onToggle = {
                                afternoonEnabled = it
                                saveAllPreferences()
                            },
                            selectedTime = afternoonTime,
                            timeOptions = afternoonHours,
                            onSelectTime = {
                                afternoonTime = it
                                saveAllPreferences()
                            }
                        )
                    }

                    item {
                        ShiftTimeCard(
                            title = "🌙 Evening Shift & Distribution Hour",
                            subtitle = "Evening reminder at $eveningTime for second shift delivery & milk collection",
                            enabled = eveningEnabled,
                            onToggle = {
                                eveningEnabled = it
                                saveAllPreferences()
                            },
                            selectedTime = eveningTime,
                            timeOptions = eveningHours,
                            onSelectTime = {
                                eveningTime = it
                                saveAllPreferences()
                            }
                        )
                    }

                    item {
                        ShiftTimeCard(
                            title = "📊 End-of-Day Accounts & P&L Closing",
                            subtitle = "Daily summary and ledger wrap-up alert at $dailySummaryTime",
                            enabled = dailySummaryEnabled,
                            onToggle = {
                                dailySummaryEnabled = it
                                saveAllPreferences()
                            },
                            selectedTime = dailySummaryTime,
                            timeOptions = closingHours,
                            onSelectTime = {
                                dailySummaryTime = it
                                saveAllPreferences()
                            }
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Advance Reminder Lead Time", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text("Get alerted ahead of time to prepare cans, feed, or transport vehicles.", fontSize = 11.sp, color = TextSecondary)

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(advanceOptions) { (mins, label) ->
                                        FilterChip(
                                            selected = advanceMinutes == mins,
                                            onClick = {
                                                advanceMinutes = mins
                                                saveAllPreferences()
                                            },
                                            label = { Text(label, fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // SUB-TAB 2: Alert Categories & Master Switches
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (notifMasterEnabled) DairyGreenLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Master Notification Engine", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(if (notifMasterEnabled) "🟢 Master Alert Engine Active" else "🔴 All Notifications Muted", fontSize = 11.sp, color = TextSecondary)
                                }
                                Switch(
                                    checked = notifMasterEnabled,
                                    onCheckedChange = {
                                        notifMasterEnabled = it
                                        saveAllPreferences()
                                    }
                                )
                            }
                        }
                    }

                    if (notifMasterEnabled) {
                        item {
                            Text("Select Alert Modules to Activate:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                        }

                        item {
                            CategoryToggleCard(
                                title = "🥛 Milking & Herd Yield Logs",
                                subtitle = "Alarms when shifts are active and cows haven't been milked",
                                checked = milkingEnabled,
                                onCheckedChange = {
                                    milkingEnabled = it
                                    saveAllPreferences()
                                }
                            )
                        }

                        item {
                            CategoryToggleCard(
                                title = "🚚 Milk Routes & Dispatches",
                                subtitle = "Undelivered milk alerts and delivery completion status",
                                checked = deliveryEnabled,
                                onCheckedChange = {
                                    deliveryEnabled = it
                                    saveAllPreferences()
                                }
                            )
                        }

                        item {
                            CategoryToggleCard(
                                title = "💉 Vaccination Booster Schedule",
                                subtitle = "7-day and 3-day advance alerts for FMD, HS, BQ, Anthrax",
                                checked = vaccineEnabled,
                                onCheckedChange = {
                                    vaccineEnabled = it
                                    saveAllPreferences()
                                }
                            )
                        }

                        item {
                            CategoryToggleCard(
                                title = "💊 90-Day Deworming Cycle",
                                subtitle = "Periodic salt rotation alerts (Albendazole, Fenbendazole, Ivermectin)",
                                checked = dewormingEnabled,
                                onCheckedChange = {
                                    dewormingEnabled = it
                                    saveAllPreferences()
                                }
                            )
                        }

                        item {
                            CategoryToggleCard(
                                title = "🧬 Breeding, Calving & PD Check",
                                subtitle = "Heat cycle, 60-day PD diagnosis, and 14-day calving countdowns",
                                checked = breedingEnabled,
                                onCheckedChange = {
                                    breedingEnabled = it
                                    saveAllPreferences()
                                }
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("💰 Customer Payment Due Alert", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Alerts when customer outstanding balance exceeds limit", fontSize = 10.sp, color = TextSecondary)
                                        }
                                        Switch(
                                            checked = paymentDueEnabled,
                                            onCheckedChange = {
                                                paymentDueEnabled = it
                                                saveAllPreferences()
                                            }
                                        )
                                    }

                                    if (paymentDueEnabled) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("Threshold:", fontSize = 11.sp, color = TextSecondary)
                                            listOf("100", "200", "500", "1000", "2000", "5000").forEach { amt ->
                                                FilterChip(
                                                    selected = paymentDueThresholdText == amt,
                                                    onClick = {
                                                        paymentDueThresholdText = amt
                                                        saveAllPreferences()
                                                    },
                                                    label = { Text("₹$amt", fontSize = 10.sp) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            CategoryToggleCard(
                                title = "🌾 Low Feed Stock Warnings",
                                subtitle = "Alert when cattle feed, cake, or mineral bags fall below buffer",
                                checked = lowStockEnabled,
                                onCheckedChange = {
                                    lowStockEnabled = it
                                    saveAllPreferences()
                                }
                            )
                        }

                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("🔊 Sound Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = soundEnabled,
                                            onCheckedChange = {
                                                soundEnabled = it
                                                saveAllPreferences()
                                            }
                                        )
                                    }
                                }
                                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("📳 Vibration", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = vibrationEnabled,
                                            onCheckedChange = {
                                                vibrationEnabled = it
                                                saveAllPreferences()
                                            }
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
*/

// -------------------------------------------------------------
// REUSABLE HELPER UI COMPONENTS
// -------------------------------------------------------------
@Composable
fun StatMetricTile(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            Text(text = title, fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
fun PricingModeChip(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) RoyalBlueLight else SurfaceBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) RoyalBluePrimary else BorderSubtle
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSelected) RoyalBluePrimary else TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = if (isSelected) RoyalBluePrimary.copy(alpha = 0.8f) else TextSecondary
            )
        }
    }
}

@Composable
fun PresetChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = RoyalBlueLight,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = RoyalBluePrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StatusPillButton(
    label: String,
    title: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeColor else SurfaceBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) activeColor else BorderSubtle
        ),
        modifier = Modifier
            .size(36.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSelected) Color.White else TextSecondary
            )
        }
    }
}

@Composable
fun ShiftProgressBar(
    title: String,
    completed: Int,
    total: Int,
    liters: Double,
    color: Color
) {
    val progress = if (total > 0) (completed.toFloat() / total).coerceIn(0f, 1f) else 0f

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("${(progress * 100).toInt()}% Done ($completed/$total)", fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Total volume scheduled: ${String.format(Locale.US, "%.1f", liters)} Liters",
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun ChecklistItem(title: String, initial: Boolean) {
    var checked by remember { mutableStateOf(initial) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { checked = !checked }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { checked = it })
        Text(
            text = title,
            fontSize = 13.sp,
            color = if (checked) TextSecondary else TextPrimary,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
fun NotificationSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
            Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DairyGreen)
        )
    }
}

@Composable
fun EmptyStateCard(icon: ImageVector, title: String, description: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(42.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun AddProductDialog(
    showFeed: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Double, Double?) -> Unit
) {
    var itemType by remember { mutableIntStateOf(0) } // 0: Sale Product, 1: Farm Consumable/Feed
    var name by remember { mutableStateOf("Fresh Paneer (Cottage Cheese)") }
    var unit by remember { mutableStateOf("kg") }
    var stockText by remember { mutableStateOf("10") }
    var costText by remember { mutableStateOf("360") }
    var dailyUsageText by remember { mutableStateOf("2") }

    val salePresets = listOf(
        Triple("Fresh Paneer (Cottage Cheese)", "kg", "360"),
        Triple("Pure Desi Ghee", "litre", "650"),
        Triple("Fresh Curd (Dahi)", "kg", "70"),
        Triple("Chilled Buttermilk (Chaas)", "litre", "30"),
        Triple("Khoya / Mawa", "kg", "320"),
        Triple("Fresh Milk Pouch", "litre", "55")
    )

    val feedPresets = listOf(
        Triple("Chokar (Wheat Bran)", "kg", "22"),
        Triple("Mustard Cake (Sarson Khal)", "kg", "32"),
        Triple("Cotton Cake / Binola Khal", "kg", "38"),
        Triple("Mineral Mixture", "kg", "120"),
        Triple("Cattle Feed Bags", "bag", "850"),
        Triple("Silage / Green Fodder", "kg", "8")
    )

    val currentPresets = if (itemType == 0) salePresets else feedPresets

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (itemType == 0) "Add Dairy Product for Sale" else "Add Farm Consumable / Cattle Feed", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Item Category:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextSecondary)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = itemType == 0,
                        onClick = {
                            itemType = 0
                            name = salePresets.first().first
                            unit = salePresets.first().second
                            costText = salePresets.first().third
                        },
                        label = { Text("🛍️ Product for Sale", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = itemType == 1,
                        onClick = {
                            itemType = 1
                            name = feedPresets.first().first
                            unit = feedPresets.first().second
                            costText = feedPresets.first().third
                        },
                        label = { Text("🌾 Consumable / Feed", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Quick Presets:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(currentPresets) { preset ->
                        PresetChip(
                            label = preset.first.split(" ").first() + " (₹" + preset.third + ")",
                            onClick = {
                                name = preset.first
                                unit = preset.second
                                costText = preset.third
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (kg/litre/bag/pkt)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text("Initial Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("Cost Per Unit (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dailyUsageText,
                        onValueChange = { dailyUsageText = it },
                        label = { Text("Daily Usage") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val stock = stockText.toDoubleOrNull() ?: 0.0
                        val cost = costText.toDoubleOrNull() ?: 0.0
                        val usage = dailyUsageText.toDoubleOrNull()
                        onSave(name, unit, stock, cost, usage)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenAccent)
            ) {
                Text("Add to Catalog")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
