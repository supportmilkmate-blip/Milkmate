package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures
import com.example.ui.util.DairyBusinessType

/**
 * Tailored Multi-Step Dairy Business Setup Wizard:
 * 1st: Farmer, Trader, or Collection Centre (If Farmer -> Feed & Fodder inventory complimentary)
 * 2nd: Supply to whom (Collection Centre, Trader, Individual, B2B Restaurant, Factory)
 * 3rd: Where is milk collected from (Own Farm, Other Farmers / Dairies)
 * 4th: Milk varieties handled (Buffalo, Cow, Both, Mix)
 * 5th: Dairy byproducts produced / sold (Paneer, Ghee/Butter, Curd/Chaas, Khoya/Sweets)
 * 6th: Dairy Profile, Name & Live Tailored Preview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstLaunchSetupWizard(
    viewModel: MilkMateViewModel,
    onComplete: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 6

    // 1st Question: Primary Role (FARMER, TRADER, COLLECTION_CENTER)
    var selectedRoleCode by remember { mutableStateOf("FARMER") }

    // 2nd Question: Supply to Whom? (Multiple selection)
    var destCollectionCenter by remember { mutableStateOf(true) }
    var destTrader by remember { mutableStateOf(false) }
    var destHouseholds by remember { mutableStateOf(true) }
    var destBulkCommercial by remember { mutableStateOf(true) }
    var destFactoryTankers by remember { mutableStateOf(false) }

    // 3rd Question: Where is milk collected from? (Multiple selection)
    var sourceOwnCattle by remember { mutableStateOf(true) }
    var sourceVillageFarmers by remember { mutableStateOf(false) }

    // 4th Question: Which milk species?
    var selectedMilkType by remember { mutableStateOf("BOTH") } // COW_ONLY, BUFFALO_ONLY, BOTH, MIXED

    // 5th Question: Dairy byproducts handled? (Multiple selection in ALL models)
    var hasPaneer by remember { mutableStateOf(true) }
    var hasGheeButter by remember { mutableStateOf(true) }
    var hasCurdChaas by remember { mutableStateOf(true) }
    var hasKhoyaSweets by remember { mutableStateOf(true) }

    // Complimentary feed inventory for farmers / cattle keepers
    val hasCattleFeed = selectedRoleCode == "FARMER" || sourceOwnCattle

    // 6th Question: Dairy Registration Details
    var businessName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedPricingMode by remember { mutableStateOf("DIRECT") } // DIRECT, FAT_SNF, FAT_ONLY
    var showCustomFormulaDialog by remember { mutableStateOf(false) }

    // Apply smart preset defaults when role changes
    fun applyRoleDefaults(roleCode: String) {
        selectedRoleCode = roleCode
        hasPaneer = true
        hasGheeButter = true
        hasCurdChaas = true
        hasKhoyaSweets = true
        when (roleCode) {
            "FARMER" -> {
                sourceOwnCattle = true
                sourceVillageFarmers = false
                destHouseholds = true
                destBulkCommercial = true
                destCollectionCenter = true
                destTrader = true
                destFactoryTankers = false
                selectedPricingMode = "DIRECT"
                if (businessName.isBlank() || businessName.contains("Collection") || businessName.contains("Trader")) {
                    businessName = "Shree Krishna Dairy Farm"
                }
            }
            "TRADER" -> {
                sourceOwnCattle = false
                sourceVillageFarmers = true
                destHouseholds = true
                destBulkCommercial = true
                destCollectionCenter = true
                destTrader = false
                destFactoryTankers = false
                selectedPricingMode = "FAT_SNF"
                if (businessName.isBlank() || businessName.contains("Farm") || businessName.contains("Collection")) {
                    businessName = "Kisan Milk Traders & Supply"
                }
            }
            "COLLECTION_CENTER" -> {
                sourceOwnCattle = false
                sourceVillageFarmers = true
                destHouseholds = false
                destBulkCommercial = false
                destCollectionCenter = false
                destTrader = false
                destFactoryTankers = true
                selectedPricingMode = "FAT_SNF"
                if (businessName.isBlank() || businessName.contains("Farm") || businessName.contains("Trader")) {
                    businessName = "Gokul Milk Collection Centre (BMC)"
                }
            }
        }
    }

    val computedModeCode = remember(
        selectedRoleCode,
        sourceOwnCattle,
        sourceVillageFarmers,
        destHouseholds,
        destCollectionCenter,
        destTrader,
        destBulkCommercial,
        destFactoryTankers
    ) {
        when (selectedRoleCode) {
            "TRADER" -> "TRADER"
            "COLLECTION_CENTER" -> "COLLECTION_CENTER"
            else -> {
                if (sourceOwnCattle && sourceVillageFarmers) "INTEGRATED"
                else if (sourceOwnCattle && !destHouseholds && !destBulkCommercial) "FARMER_WHOLESALE"
                else "FARMER"
            }
        }
    }

    val computedMode = remember(computedModeCode) {
        BusinessModeFeatures.getByType(computedModeCode)
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onCancel != null) {
                                IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Column {
                                Text(
                                    text = "MilkMate Tailored Setup",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RoyalBluePrimary
                                )
                                Text(
                                    text = when (currentStep) {
                                        1 -> "Step 1: Farmer, Trader or Collection Centre"
                                        2 -> "Step 2: Supply to Whom? (Sales Channels)"
                                        3 -> "Step 3: Where Milk is Collected (Sourcing)"
                                        4 -> "Step 4: Milk Species Handled"
                                        5 -> "Step 5: Dairy Byproducts Processing"
                                        else -> "Step 6: Profile & Tailored App Review"
                                    },
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = RoyalBlueLight
                        ) {
                            Text(
                                text = "$currentStep / $totalSteps",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = RoyalBluePrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 6-Step Progress Indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (1..totalSteps).forEach { step ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (step <= currentStep) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Previous")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Button(
                        onClick = {
                            if (currentStep < totalSteps) {
                                // Default fallbacks to prevent empty state
                                if (currentStep == 2 && !destHouseholds && !destCollectionCenter && !destTrader && !destBulkCommercial && !destFactoryTankers) {
                                    if (selectedRoleCode == "COLLECTION_CENTER") destFactoryTankers = true else destHouseholds = true
                                }
                                if (currentStep == 3 && !sourceOwnCattle && !sourceVillageFarmers) {
                                    if (selectedRoleCode == "FARMER") sourceOwnCattle = true else sourceVillageFarmers = true
                                }
                                currentStep++
                            } else {
                                val finalName = if (businessName.isBlank()) {
                                    when (selectedRoleCode) {
                                        "TRADER" -> "Kisan Milk Traders & Supply"
                                        "COLLECTION_CENTER" -> "Gokul Milk Collection Centre"
                                        else -> if (destHouseholds) "Shree Krishna Dairy Farm" else "Kisan Dairy Farm"
                                    }
                                } else businessName.trim()

                                viewModel.completeFirstLaunchWithCapabilities(
                                    businessName = finalName,
                                    ownerName = ownerName.trim().ifBlank { "Owner" },
                                    phone = phone.trim(),
                                    sourceOwnCattle = sourceOwnCattle,
                                    sourceVillageFarmers = sourceVillageFarmers,
                                    destHouseholds = destHouseholds,
                                    destCollectionCenter = destCollectionCenter || destTrader,
                                    destBulkCommercial = destBulkCommercial,
                                    destFactoryTankers = destFactoryTankers,
                                    hasPaneer = hasPaneer,
                                    hasCurdChaas = hasCurdChaas,
                                    hasGheeButter = hasGheeButter,
                                    hasKhoyaSweets = hasKhoyaSweets,
                                    hasCattleFeed = hasCattleFeed,
                                    supportedMilkTypes = selectedMilkType,
                                    cowRate = 50.0,
                                    buffaloRate = 65.0,
                                    pricingMode = selectedPricingMode
                                )
                                onComplete()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentStep == totalSteps) GrassGreen else RoyalBluePrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("wizard_next_btn")
                    ) {
                        Text(
                            text = if (currentStep == totalSteps) "🚀 Launch Customized App" else "Next Step",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            if (currentStep == totalSteps) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
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
            when (currentStep) {
                1 -> WizardStepRoleSelection(
                    selectedRoleCode = selectedRoleCode,
                    onSelectRole = { applyRoleDefaults(it) }
                )
                2 -> WizardStepSupplyToWhom(
                    roleCode = selectedRoleCode,
                    destCollectionCenter = destCollectionCenter,
                    onToggleCollectionCenter = { destCollectionCenter = it },
                    destTrader = destTrader,
                    onToggleTrader = { destTrader = it },
                    destHouseholds = destHouseholds,
                    onToggleHouseholds = { destHouseholds = it },
                    destBulkCommercial = destBulkCommercial,
                    onToggleBulkCommercial = { destBulkCommercial = it },
                    destFactoryTankers = destFactoryTankers,
                    onToggleFactoryTankers = { destFactoryTankers = it }
                )
                3 -> WizardStepMilkSourcing(
                    roleCode = selectedRoleCode,
                    sourceOwnCattle = sourceOwnCattle,
                    onToggleOwnCattle = { sourceOwnCattle = it },
                    sourceVillageFarmers = sourceVillageFarmers,
                    onToggleVillageFarmers = { sourceVillageFarmers = it }
                )
                4 -> WizardStepMilkSpecies(
                    selectedMilkType = selectedMilkType,
                    onSelectMilkType = { selectedMilkType = it }
                )
                5 -> WizardStepDairyByproducts(
                    hasPaneer = hasPaneer,
                    onTogglePaneer = { hasPaneer = it },
                    hasGheeButter = hasGheeButter,
                    onToggleGheeButter = { hasGheeButter = it },
                    hasCurdChaas = hasCurdChaas,
                    onToggleCurdChaas = { hasCurdChaas = it },
                    hasKhoyaSweets = hasKhoyaSweets,
                    onToggleKhoyaSweets = { hasKhoyaSweets = it }
                )
                6 -> WizardStepProfileAndLiveReview(
                    selectedRoleCode = selectedRoleCode,
                    computedMode = computedMode,
                    sourceOwnCattle = sourceOwnCattle,
                    sourceVillageFarmers = sourceVillageFarmers,
                    destHouseholds = destHouseholds,
                    destCollectionCenter = destCollectionCenter || destTrader,
                    destBulkCommercial = destBulkCommercial,
                    destFactoryTankers = destFactoryTankers,
                    hasPaneer = hasPaneer,
                    hasGheeButter = hasGheeButter,
                    hasCurdChaas = hasCurdChaas,
                    hasKhoyaSweets = hasKhoyaSweets,
                    hasCattleFeed = hasCattleFeed,
                    selectedMilkType = selectedMilkType,
                    businessName = businessName,
                    onBusinessNameChange = { businessName = it },
                    ownerName = ownerName,
                    onOwnerNameChange = { ownerName = it },
                    phone = phone,
                    onPhoneChange = { phone = it },
                    pricingMode = selectedPricingMode,
                    onPricingModeChange = { selectedPricingMode = it },
                    onOpenCustomFormulaStudio = { showCustomFormulaDialog = true }
                )
            }
        }
    }

    if (showCustomFormulaDialog) {
        CustomFormulaStudioDialog(
            business = null,
            viewModel = viewModel,
            onDismiss = {
                selectedPricingMode = "CUSTOM"
                showCustomFormulaDialog = false
            }
        )
    }
}

// ----------------- STEP 1: BUSINESS ROLE SELECTION -----------------
@Composable
private fun WizardStepRoleSelection(
    selectedRoleCode: String,
    onSelectRole: (String) -> Unit
) {
    val roles = listOf(
        Triple(
            "FARMER",
            "🚜 Dairy Farmer (Producer)",
            "I manage a dairy / cattle farm with cows or buffaloes. (Feed & fodder inventory complimentary!)"
        ),
        Triple(
            "TRADER",
            "🚚 Milk Trader / Wholesaler",
            "I collect milk from farmers or dairies and supply to household routes, restaurants & collection centres."
        ),
        Triple(
            "COLLECTION_CENTER",
            "🏢 Milk Collection Centre / BMC",
            "I procure milk from village farmers with FAT/SNF testing, chill in bulk, and dispatch tankers to factories."
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "1. Choose Your Business Role",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "Select whether you operate as a Farmer, Milk Trader, or Collection Centre. This configures the primary foundation for your app.",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        items(roles) { (code, title, desc) ->
            val isSelected = selectedRoleCode == code
            val themeColor = when (code) {
                "FARMER" -> GrassGreen
                "TRADER" -> GoldenOrange
                else -> RoyalBluePrimary
            }
            val icon = when (code) {
                "FARMER" -> Icons.Default.Agriculture
                "TRADER" -> Icons.Default.LocalShipping
                else -> Icons.Default.LocalDrink
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) themeColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                ),
                border = if (isSelected) BorderStroke(2.dp, themeColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectRole(code) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) themeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(icon, contentDescription = null, tint = themeColor, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text(desc, fontSize = 11.5.sp, color = TextSecondary, lineHeight = 15.sp)
                            }
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectRole(code) },
                            colors = RadioButtonDefaults.colors(selectedColor = themeColor)
                        )
                    }

                    if (code == "FARMER") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DairyGreenLight
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, tint = GrassGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "✨ Stock of Feed & Fodder Inventory is complimentary",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GrassGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------- STEP 2: SUPPLY TO WHOM? (SALES DESTINATIONS) -----------------
@Composable
private fun WizardStepSupplyToWhom(
    roleCode: String,
    destCollectionCenter: Boolean,
    onToggleCollectionCenter: (Boolean) -> Unit,
    destTrader: Boolean,
    onToggleTrader: (Boolean) -> Unit,
    destHouseholds: Boolean,
    onToggleHouseholds: (Boolean) -> Unit,
    destBulkCommercial: Boolean,
    onToggleBulkCommercial: (Boolean) -> Unit,
    destFactoryTankers: Boolean,
    onToggleFactoryTankers: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "2. Supply to Whom? (Sales Channels)",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "Select all the parties and destinations you supply milk to. You can select multiple options.",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        when (roleCode) {
            "FARMER" -> {
                item {
                    InteractiveActivityCard(
                        title = "🏢 Collection Centre",
                        subtitle = "Supply daily milk cans to village BMC, chilling Kendra, or Dairy society.",
                        activeFeatures = listOf(
                            "Daily Outward Milk Supply Slips",
                            "FAT/SNF & Rate Received Ledger",
                            "Center Passbook & Payment Tracking"
                        ),
                        checked = destCollectionCenter,
                        onToggle = onToggleCollectionCenter,
                        accentColor = RoyalBluePrimary
                    )
                }

                item {
                    InteractiveActivityCard(
                        title = "🚚 Milk Trader",
                        subtitle = "Supply morning & evening raw milk to local milk vendors or traders.",
                        activeFeatures = listOf(
                            "Trader Daily Supply Receipts",
                            "Commercial Rate Calculation",
                            "Trader Balance Khata"
                        ),
                        checked = destTrader,
                        onToggle = onToggleTrader,
                        accentColor = GoldenOrange
                    )
                }

                item {
                    InteractiveActivityCard(
                        title = "🏠 Individual (Households)",
                        subtitle = "Doorstep residential delivery subscription routes with daily run sheets.",
                        activeFeatures = listOf(
                            "Delivery Boy Route Navigation",
                            "Monthly Customer Khatas & WhatsApp Invoices",
                            "Shift Delivery Marking (Cow / Buffalo / Mix)"
                        ),
                        checked = destHouseholds,
                        onToggle = onToggleHouseholds,
                        accentColor = GrassGreen
                    )
                }

                item {
                    InteractiveActivityCard(
                        title = "🏨 B2B Restaurant / Hotels",
                        subtitle = "Supply bulk milk cans to tea stalls, hotels, cafes, restaurants & sweet shops.",
                        activeFeatures = listOf(
                            "B2B Commercial Accounts & Volume Rates",
                            "Consolidated Weekly / Monthly Bills",
                            "Advance Bookings & Order Slips"
                        ),
                        checked = destBulkCommercial,
                        onToggle = onToggleBulkCommercial,
                        accentColor = WarmHoney
                    )
                }
            }
            "TRADER" -> {
                item {
                    InteractiveActivityCard(
                        title = "🏢 Collection Centre",
                        subtitle = "Supply surplus or bulk milk to BMC collection centres.",
                        activeFeatures = listOf(
                            "Surplus Milk Dispatch Slips",
                            "FAT/SNF Testing & Rate Received",
                            "Center Reconciliation"
                        ),
                        checked = destCollectionCenter,
                        onToggle = onToggleCollectionCenter,
                        accentColor = RoyalBluePrimary
                    )
                }

                item {
                    InteractiveActivityCard(
                        title = "🏠 Individual (Households)",
                        subtitle = "Deliver milk directly to residential consumer routes.",
                        activeFeatures = listOf(
                            "Doorstep Route Delivery Run-Sheets",
                            "Monthly Customer Khatas",
                            "WhatsApp Invoices & UPI Payouts"
                        ),
                        checked = destHouseholds,
                        onToggle = onToggleHouseholds,
                        accentColor = GrassGreen
                    )
                }

                item {
                    InteractiveActivityCard(
                        title = "🏨 B2B Restaurant / Hotels",
                        subtitle = "Supply bulk milk to tea stalls, restaurants, sweet shops & cafes.",
                        activeFeatures = listOf(
                            "Commercial B2B Accounts & Ledger",
                            "Trading Margin Spread Calculation",
                            "Bulk Outward Slips"
                        ),
                        checked = destBulkCommercial,
                        onToggle = onToggleBulkCommercial,
                        accentColor = GoldenOrange
                    )
                }
            }
            "COLLECTION_CENTER" -> {
                item {
                    InteractiveActivityCard(
                        title = "🏭 Factory / Dairy Processing Plant",
                        subtitle = "Dispatch bulk chilled milk tankers to dairy factories and plants.",
                        activeFeatures = listOf(
                            "Bulk Tanker Outward Dispatch Challans",
                            "Chilling Center Spread: (Factory Sales − Farmer Outlays)",
                            "Dip Rod & Temperature Logs"
                        ),
                        checked = destFactoryTankers,
                        onToggle = onToggleFactoryTankers,
                        accentColor = RoyalBluePrimary
                    )
                }

                item {
                    InteractiveActivityCard(
                        title = "🏨 Local Bulk / Commercial Buyers",
                        subtitle = "Optionally supply local sweet shops, hotels, and dairies from the center.",
                        activeFeatures = listOf(
                            "Local B2B Accounts & Billing",
                            "Bulk Milk Gate Passes"
                        ),
                        checked = destBulkCommercial,
                        onToggle = onToggleBulkCommercial,
                        accentColor = WarmHoney
                    )
                }
            }
        }
    }
}

// ----------------- STEP 3: WHERE IS MILK COLLECTED? (SOURCING) -----------------
@Composable
private fun WizardStepMilkSourcing(
    roleCode: String,
    sourceOwnCattle: Boolean,
    onToggleOwnCattle: (Boolean) -> Unit,
    sourceVillageFarmers: Boolean,
    onToggleVillageFarmers: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "3. From Where is Milk Collected?",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "Select your milk sourcing channels. (Multiple selections allowed)",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        when (roleCode) {
            "FARMER" -> {
                item {
                    InteractiveActivityCard(
                        title = "🐄 Own Farm (Cattle Herd)",
                        subtitle = "Produce milk from your own cows/buffaloes herd with milking yields & breeding management.",
                        activeFeatures = listOf(
                            "Cattle Tag IDs & Milking Yield Logs",
                            "Breeding Stages & Lactation Tracking",
                            "✨ Cattle Feed & Fodder Inventory (Complimentary)",
                            "Veterinary Meds & Expense Khatas"
                        ),
                        checked = sourceOwnCattle,
                        onToggle = onToggleOwnCattle,
                        accentColor = GrassGreen
                    )
                }

                item {
                    InteractiveActivityCard(
                        title = "🧑‍🌾 Other Farmers (Village Collection)",
                        subtitle = "Also collect or purchase additional milk from nearby village farmers.",
                        activeFeatures = listOf(
                            "Farmer Milk Intake Desk (FAT, SNF, CLR)",
                            "Dynamic Shift Rate Charts",
                            "Farmer Passbooks & 1-Tap Payout Sheets"
                        ),
                        checked = sourceVillageFarmers,
                        onToggle = onToggleVillageFarmers,
                        accentColor = RoyalBluePrimary
                    )
                }
            }
            "TRADER" -> {
                item {
                    InteractiveActivityCard(
                        title = "🧑‍🌾 Farmers & Local Dairies",
                        subtitle = "Purchase raw milk cans from village farmers and dairy producers.",
                        activeFeatures = listOf(
                            "Farmer Intake Desk with FAT/SNF Rate Chart",
                            "Farmer Passbooks & Payout Ledgers",
                            "Milk Collection vs Sales Reconciliation"
                        ),
                        checked = sourceVillageFarmers,
                        onToggle = onToggleVillageFarmers,
                        accentColor = GoldenOrange
                    )
                }
            }
            "COLLECTION_CENTER" -> {
                item {
                    InteractiveActivityCard(
                        title = "🧑‍🌾 Village Farmers (BMC Kendra)",
                        subtitle = "Procure milk daily from hundreds of local village farmers.",
                        activeFeatures = listOf(
                            "High-Speed Village Intake Desk (FAT, SNF, CLR, Densi)",
                            "Automatic Shift Rate Calculation & Slip Generation",
                            "Farmer Directory, Passbooks & Deductions",
                            "Feed & Advance Loan Deductions"
                        ),
                        checked = sourceVillageFarmers,
                        onToggle = onToggleVillageFarmers,
                        accentColor = RoyalBluePrimary
                    )
                }
            }
        }
    }
}

// ----------------- STEP 4: WHICH MILK VARIETIES? -----------------
@Composable
private fun WizardStepMilkSpecies(
    selectedMilkType: String,
    onSelectMilkType: (String) -> Unit
) {
    val species = listOf(
        Triple("BUFFALO_ONLY", "🐃 Buffalo Milk Only", "Murrah, Jafrabadi, Mehsana high-fat milk"),
        Triple("COW_ONLY", "🐄 Cow Milk Only", "Desi Gir, Sahiwal, HF, Jersey cow milk"),
        Triple("BOTH", "🥛 Both (Cow & Buffalo)", "Separate tracking, rates, and shift entries for Cow & Buffalo"),
        Triple("MIXED", "🍶 Mix Milk (Blend)", "Common blend with standardized fat/snf or flat rate")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "4. Which Milk Varieties Do You Handle?",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "Select the milk species handled in your business.",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        items(species) { (code, title, desc) ->
            val isSelected = selectedMilkType == code
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) RoyalBlueLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                ),
                border = if (isSelected) BorderStroke(2.dp, RoyalBluePrimary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectMilkType(code) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectMilkType(code) },
                        colors = RadioButtonDefaults.colors(selectedColor = RoyalBluePrimary)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text(desc, fontSize = 11.5.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

// ----------------- STEP 5: DAIRY BYPRODUCTS (IN ALL ROLES) -----------------
@Composable
private fun WizardStepDairyByproducts(
    hasPaneer: Boolean,
    onTogglePaneer: (Boolean) -> Unit,
    hasGheeButter: Boolean,
    onToggleGheeButter: (Boolean) -> Unit,
    hasCurdChaas: Boolean,
    onToggleCurdChaas: (Boolean) -> Unit,
    hasKhoyaSweets: Boolean,
    onToggleKhoyaSweets: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "5. Dairy Byproducts & Value Addition",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "Option to handle and produce dairy byproducts is available in all roles. Select all the products you make or sell (multiple selectable):",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        item {
            ProductCardOption(
                title = "🧀 Fresh Paneer (Cottage Cheese)",
                subtitle = "Track raw milk to paneer yield conversion, stock inventory & sales in kg.",
                checked = hasPaneer,
                onToggle = onTogglePaneer
            )
        }

        item {
            ProductCardOption(
                title = "🧈 Pure Desi Ghee & Fresh Butter (Makhan)",
                subtitle = "Clarified butter jars, yellow makhan tubs, cream separation & sales.",
                checked = hasGheeButter,
                onToggle = onToggleGheeButter
            )
        }

        item {
            ProductCardOption(
                title = "🍶 Fresh Curd (Dahi) & Chaas (Buttermilk / Lassi)",
                subtitle = "Probiotic cups, pouches, matka curd, chilled buttermilk bottles.",
                checked = hasCurdChaas,
                onToggle = onToggleCurdChaas
            )
        }

        item {
            ProductCardOption(
                title = "🍬 Khoya / Mawa & Sweets",
                subtitle = "Condensed milk solids, khoa batch calculations & sweet shop sales.",
                checked = hasKhoyaSweets,
                onToggle = onToggleKhoyaSweets
            )
        }
    }
}

// ----------------- STEP 6: PROFILE & LIVE REVIEW -----------------
@Composable
private fun WizardStepProfileAndLiveReview(
    selectedRoleCode: String,
    computedMode: DairyBusinessType,
    sourceOwnCattle: Boolean,
    sourceVillageFarmers: Boolean,
    destHouseholds: Boolean,
    destCollectionCenter: Boolean,
    destBulkCommercial: Boolean,
    destFactoryTankers: Boolean,
    hasPaneer: Boolean,
    hasGheeButter: Boolean,
    hasCurdChaas: Boolean,
    hasKhoyaSweets: Boolean,
    hasCattleFeed: Boolean,
    selectedMilkType: String,
    businessName: String,
    onBusinessNameChange: (String) -> Unit,
    ownerName: String,
    onOwnerNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    pricingMode: String,
    onPricingModeChange: (String) -> Unit,
    onOpenCustomFormulaStudio: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "6. Profile Details & Tailored App Review",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "Enter your dairy details and review how MilkMate is automatically tailored for your operations.",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Dairy & Owner Information", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RoyalBluePrimary)

                    OutlinedTextField(
                        value = businessName,
                        onValueChange = onBusinessNameChange,
                        label = { Text("Dairy / Farm / Centre Name *") },
                        placeholder = { Text("e.g. Shree Krishna Dairy") },
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = RoyalBluePrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = onOwnerNameChange,
                        label = { Text("Owner / Manager Name") },
                        placeholder = { Text("e.g. Rajesh Kumar") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = RoyalBluePrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = onPhoneChange,
                        label = { Text("WhatsApp / Mobile Number") },
                        placeholder = { Text("10-digit mobile number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = RoyalBluePrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Pricing Rate Basis
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Milk Rate Basis / Pricing Formula", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RoyalBluePrimary)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = pricingMode == "DIRECT",
                            onClick = { onPricingModeChange("DIRECT") },
                            label = { Text("🏷️ Direct (₹/Litre)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = pricingMode == "FAT_SNF",
                            onClick = { onPricingModeChange("FAT_SNF") },
                            label = { Text("🧪 FAT / SNF Chart", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = pricingMode == "CUSTOM",
                            onClick = onOpenCustomFormulaStudio,
                            label = { Text("⚙️ Formula Studio", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Live Tailored Summary
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎯 Tailored Workspace Blueprint", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = RoyalBluePrimary)
                        Surface(shape = RoundedCornerShape(6.dp), color = DairyGreenLight) {
                            Text("Ready to Launch", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GrassGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Text("Selected Role: ${when(selectedRoleCode) { "FARMER" -> "🚜 Dairy Farmer"; "TRADER" -> "🚚 Milk Trader"; else -> "🏢 Collection Centre" }}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (sourceOwnCattle) {
                            BulletSummaryItem("🐄 Cattle Herd Milking, Breeding & Complimentary Feed Inventory")
                        }
                        if (sourceVillageFarmers) {
                            BulletSummaryItem("🧑‍🌾 Village Farmer Milk Collection Desk with FAT/SNF testing")
                        }
                        if (destHouseholds) {
                            BulletSummaryItem("🏠 Household Delivery Routes, Run-sheets & Monthly WhatsApp Invoices")
                        }
                        if (destBulkCommercial) {
                            BulletSummaryItem("🏨 B2B Commercial Accounts (Hotels, Cafes, Restaurants, Tea Stalls)")
                        }
                        if (destCollectionCenter) {
                            BulletSummaryItem("🏢 Outward Supply Slips to Collection Centre / Trader")
                        }
                        if (destFactoryTankers) {
                            BulletSummaryItem("🏭 Bulk Factory Tanker Dispatch Challans & Chilling Margins")
                        }
                        val byproductsList = mutableListOf<String>()
                        if (hasPaneer) byproductsList.add("Paneer")
                        if (hasGheeButter) byproductsList.add("Ghee/Butter")
                        if (hasCurdChaas) byproductsList.add("Curd/Chaas")
                        if (hasKhoyaSweets) byproductsList.add("Khoya")
                        if (byproductsList.isNotEmpty()) {
                            BulletSummaryItem("🧀 Dairy Products: ${byproductsList.joinToString(", ")}")
                        }
                    }
                }
            }
        }
    }
}

// ----------------- REUSABLE COMPOSABLES -----------------
@Composable
private fun InteractiveActivityCard(
    title: String,
    subtitle: String,
    activeFeatures: List<String>,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    accentColor: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) accentColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = if (checked) BorderStroke(2.dp, accentColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = TextPrimary)
                    Text(subtitle, fontSize = 11.5.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
                }
                Checkbox(
                    checked = checked,
                    onCheckedChange = onToggle,
                    colors = CheckboxDefaults.colors(checkedColor = accentColor)
                )
            }

            if (checked && activeFeatures.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        activeFeatures.forEach { feature ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = accentColor, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(feature, fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCardOption(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) RoyalBlueLight.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        border = if (checked) BorderStroke(1.5.dp, RoyalBluePrimary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextPrimary)
                Text(subtitle, fontSize = 11.sp, color = TextSecondary)
            }
            Checkbox(
                checked = checked,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(checkedColor = RoyalBluePrimary)
            )
        }
    }
}

@Composable
private fun BulletSummaryItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontSize = 11.5.sp, color = TextPrimary)
    }
}
