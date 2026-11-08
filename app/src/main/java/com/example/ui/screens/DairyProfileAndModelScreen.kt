package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MilkMateViewModel
import com.example.ui.components.DairyStandardTopBar
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DairyProfileAndModelScreen(
    viewModel: MilkMateViewModel,
    onBack: () -> Unit
) {
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val session by viewModel.sessionState.collectAsStateWithLifecycle()

    // Editable state for Business Profile
    var businessNameInput by remember(business) { mutableStateOf(business?.businessName ?: "") }
    var ownerNameInput by remember(business) { mutableStateOf(business?.ownerName ?: "") }
    var phoneInput by remember(business) { mutableStateOf(business?.phone ?: "") }
    var upiIdInput by remember(session) { mutableStateOf(session.upiId) }

    // Editable state for Operating Model & Features
    var draftMode by remember(business?.businessMode) { mutableStateOf(business?.businessMode ?: "FARMER") }
    var draftSourceVillageFarmers by remember(business?.sourceVillageFarmers) { mutableStateOf(business?.sourceVillageFarmers ?: false) }
    var draftSourceOwnCattle by remember(business?.sourceOwnCattle) { mutableStateOf(business?.sourceOwnCattle ?: false) }
    var draftDestHouseholds by remember(business?.destHouseholds) { mutableStateOf(business?.destHouseholds ?: false) }
    var draftDestBulkCommercial by remember(business?.destBulkCommercial) { mutableStateOf(business?.destBulkCommercial ?: false) }
    var draftHasPaneer by remember(business?.hasPaneer) { mutableStateOf(business?.hasPaneer ?: false) }
    var draftHasCurdChaas by remember(business?.hasCurdChaas) { mutableStateOf(business?.hasCurdChaas ?: false) }
    var draftHasGheeButter by remember(business?.hasGheeButter) { mutableStateOf(business?.hasGheeButter ?: false) }
    var draftHasKhoyaSweets by remember(business?.hasKhoyaSweets) { mutableStateOf(business?.hasKhoyaSweets ?: false) }
    var draftHasCattleFeed by remember(business?.hasCattleFeed) { mutableStateOf(business?.hasCattleFeed ?: false) }

    var showCustomFormulaStudio by remember { mutableStateOf(false) }

    if (showCustomFormulaStudio) {
        CustomFormulaStudioDialog(
            business = business,
            viewModel = viewModel,
            onDismiss = { showCustomFormulaStudio = false }
        )
    }

    Scaffold(
        topBar = {
            DairyStandardTopBar(
                title = "Dairy Profile & Operating Model",
                subtitle = "Manage shop identity, operating modes & pricing formulas",
                onBackClick = onBack,
                businessMode = draftMode,
                onModeClick = {}
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            viewModel.updateBusinessProfile(
                                businessName = businessNameInput.ifBlank { "MilkMate Dairy" },
                                ownerName = ownerNameInput.ifBlank { "Owner" },
                                phone = phoneInput,
                                cowRate = business?.cowMilkRate ?: 55.0,
                                buffaloRate = business?.buffaloMilkRate ?: 75.0,
                                pricingMode = business?.pricingMode ?: "FAT_SNF"
                            )
                            if (draftMode != business?.businessMode) {
                                viewModel.switchBusinessMode(draftMode)
                            }
                            viewModel.updateBusinessCapabilities(
                                sourceOwnCattle = draftSourceOwnCattle,
                                sourceVillageFarmers = draftSourceVillageFarmers,
                                destHouseholds = draftDestHouseholds,
                                destBulkCommercial = draftDestBulkCommercial,
                                hasCattleFeed = draftHasCattleFeed,
                                hasPaneer = draftHasPaneer,
                                hasCurdChaas = draftHasCurdChaas,
                                hasGheeButter = draftHasGheeButter,
                                hasKhoyaSweets = draftHasKhoyaSweets
                            )
                            if (upiIdInput != session.upiId) {
                                viewModel.repository.sessionManager.setUpiId(upiIdInput)
                            }
                            viewModel.showMessage("Dairy Profile & Operating Model saved successfully! ✓")
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                        modifier = Modifier.weight(1f).height(46.dp).testTag("save_dairy_profile_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. SHOP IDENTITY CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = RoyalBluePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Dairy Shop & Owner Identity", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Name, contact and digital UPI GPay/PhonePe ID", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedTextField(
                        value = businessNameInput,
                        onValueChange = { businessNameInput = it },
                        label = { Text("Dairy / Farm Business Name") },
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ownerNameInput,
                            onValueChange = { ownerNameInput = it },
                            label = { Text("Owner Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Mobile Phone") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = upiIdInput,
                        onValueChange = { upiIdInput = it },
                        label = { Text("UPI VPA (e.g. 9876543210@paytm)") },
                        leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. OPERATING MODEL & MODULES CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = DairyGreenLight,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Dairy Operating Model", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Presets & customized feature modules", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = DairyGreenLight) {
                            Text(
                                "🛡️ 100% DATA SAFE",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DairyGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Preset Profiles
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("⚡ 1-Tap Preset Profiles:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val presets = listOf(
                                Triple("FARMER", "🚜 Dairy Farm", "Own Cattle + Local Delivery"),
                                Triple("COLLECTION_CENTER", "🏢 Collection Centre", "Farmer Intake + Tankers"),
                                Triple("TRADER", "🚚 Milk Trader", "Buy Farmers + Retail/Wholesale"),
                                Triple("INTEGRATED", "🌐 Integrated", "All Features Enabled"),
                                Triple("FARMER_WHOLESALE", "🚜 Farm to Center", "Herd + Center Supply")
                            )
                            presets.forEach { (modeCode, label, _) ->
                                val isSelected = draftMode == modeCode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        draftMode = modeCode
                                        when (modeCode) {
                                            "FARMER" -> {
                                                draftSourceOwnCattle = true
                                                draftDestHouseholds = true
                                                draftSourceVillageFarmers = false
                                                draftDestBulkCommercial = false
                                            }
                                            "COLLECTION_CENTER" -> {
                                                draftSourceVillageFarmers = true
                                                draftDestBulkCommercial = true
                                                draftSourceOwnCattle = false
                                                draftDestHouseholds = false
                                            }
                                            "TRADER" -> {
                                                draftSourceVillageFarmers = true
                                                draftDestHouseholds = true
                                                draftDestBulkCommercial = true
                                                draftSourceOwnCattle = false
                                            }
                                            "INTEGRATED" -> {
                                                draftSourceVillageFarmers = true
                                                draftSourceOwnCattle = true
                                                draftDestHouseholds = true
                                                draftDestBulkCommercial = true
                                                draftHasPaneer = true
                                                draftHasCattleFeed = true
                                            }
                                            "FARMER_WHOLESALE" -> {
                                                draftSourceOwnCattle = true
                                                draftDestBulkCommercial = true
                                                draftSourceVillageFarmers = false
                                                draftDestHouseholds = false
                                            }
                                        }
                                    },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RoyalBluePrimary.copy(alpha = 0.15f),
                                        selectedLabelColor = RoyalBluePrimary
                                    )
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Granular Toggles
                    Text("⚙️ Granular Feature Toggles:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Village Farmer Milk Intake (Collection Desk)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Record shift collection, FAT/SNF & payouts for suppliers", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = draftSourceVillageFarmers, onCheckedChange = { draftSourceVillageFarmers = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Own Cattle Herd & Breeding Records", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Manage cattle tags, daily milking yields & artificial insemination", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = draftSourceOwnCattle, onCheckedChange = { draftSourceOwnCattle = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Household Bottle Delivery & Khata", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Daily doorstep distribution routes & customer ledgers", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = draftDestHouseholds, onCheckedChange = { draftDestHouseholds = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bulk Commercial Dispatches (Tankers/Shops)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Sell bulk milk quantities to dairy plants or commercial cafes", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = draftDestBulkCommercial, onCheckedChange = { draftDestBulkCommercial = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cattle Feed & Inventory Shop", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Manage feed bags, cattle medicine & low stock alerts", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = draftHasCattleFeed, onCheckedChange = { draftHasCattleFeed = it })
                    }
                }
            }

            // 3. PRICING & FORMULA STUDIO CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = FreshGoldLight,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = WarmHoney, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Milk Pricing & Rate Formula Studio", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Configure FAT/SNF rate charts and custom pricing formulas", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Button(
                        onClick = { showCustomFormulaStudio = true },
                        colors = ButtonDefaults.buttonColors(containerColor = FreshGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Custom Rate Formula Studio 🧮", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}
