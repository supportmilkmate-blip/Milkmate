package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures
import com.example.ui.util.loc
import com.example.ui.util.appStr
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    viewModel: MilkMateViewModel,
    onNavigateToSubscription: () -> Unit,
    onNavigateToStaff: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val deletedCustomers by viewModel.deletedCustomers.collectAsStateWithLifecycle()
    val deletedExpenses by viewModel.deletedExpenses.collectAsStateWithLifecycle()

    // Draft State for Settings Safety Confirmation
    var draftMode by remember(business?.businessMode) { mutableStateOf(business?.businessMode ?: "FARMER") }
    var draftSourceVillageFarmers by remember(business?.sourceVillageFarmers) { mutableStateOf(business?.sourceVillageFarmers ?: false) }
    var draftSourceOwnCattle by remember(business?.sourceOwnCattle) { mutableStateOf(business?.sourceOwnCattle ?: false) }
    var draftDestHouseholds by remember(business?.destHouseholds) { mutableStateOf(business?.destHouseholds ?: false) }
    var draftDestBulkCommercial by remember(business?.destBulkCommercial) { mutableStateOf(business?.destBulkCommercial ?: false) }
    var draftHasByproducts by remember(business?.hasPaneer, business?.hasCurdChaas, business?.hasGheeButter, business?.hasKhoyaSweets) {
        mutableStateOf(business?.hasPaneer == true || business?.hasCurdChaas == true || business?.hasGheeButter == true || business?.hasKhoyaSweets == true)
    }
    var draftHasCattleFeed by remember(business?.hasCattleFeed) { mutableStateOf(business?.hasCattleFeed ?: false) }

    val isDraftModified = remember(
        draftMode, draftSourceVillageFarmers, draftSourceOwnCattle, draftDestHouseholds,
        draftDestBulkCommercial, draftHasByproducts, draftHasCattleFeed, business
    ) {
        business != null && (
            draftMode != business?.businessMode ||
            draftSourceVillageFarmers != business?.sourceVillageFarmers ||
            draftSourceOwnCattle != business?.sourceOwnCattle ||
            draftDestHouseholds != business?.destHouseholds ||
            draftDestBulkCommercial != business?.destBulkCommercial ||
            draftHasByproducts != (business?.hasPaneer == true || business?.hasCurdChaas == true || business?.hasGheeButter == true || business?.hasKhoyaSweets == true) ||
            draftHasCattleFeed != business?.hasCattleFeed
        )
    }

    var showBusinessProfileDialog by remember { mutableStateOf(false) }
    var showEditStartDateDialog by remember { mutableStateOf(false) }
    var showUpiDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showExpenseCategoriesDialog by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showBugReportDialog by remember { mutableStateOf(false) }
    var showSuggestFeatureDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showCattleBreedingScreen by remember { mutableStateOf(false) }
    var showSetupWizard by remember { mutableStateOf(false) }
    var showCustomFormulaStudio by remember { mutableStateOf(false) }
    var showAnalyzerDialog by remember { mutableStateOf(false) }
    var showPrinterDialog by remember { mutableStateOf(false) }

    if (showCustomFormulaStudio) {
        CustomFormulaStudioDialog(
            business = business,
            viewModel = viewModel,
            onDismiss = { showCustomFormulaStudio = false }
        )
    }

    val currentMode = business?.businessMode ?: "FARMER"
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    val startDateStr = dateFormat.format(Date(business?.accountStartDate ?: System.currentTimeMillis()))

    if (showSetupWizard) {
        FirstLaunchSetupWizard(
            viewModel = viewModel,
            onComplete = { showSetupWizard = false },
            onCancel = { showSetupWizard = false }
        )
        return
    }

    if (showCattleBreedingScreen) {
        CattleBreedingScreen(
            viewModel = viewModel,
            onBack = { showCattleBreedingScreen = false }
        )
        return
    }

    // JSON file picker for Backup Restore
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonContent = reader.readText()
                reader.close()

                viewModel.restoreBackup(jsonContent) { success, msg ->
                    viewModel.showMessage(msg)
                }
            } catch (e: Exception) {
                viewModel.showMessage("Failed to read backup: ${e.message}")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {


        // Quick App Guided Tour & Feature Overview Banner
        var showQuickTourDetails by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier.fillMaxWidth().testTag("settings_quick_tour_banner"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✨ Quick App Guided Tour", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    TextButton(onClick = { showQuickTourDetails = !showQuickTourDetails }) {
                        Text(if (showQuickTourDetails) "Hide" else "Explore Features", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (showQuickTourDetails) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("1. 🥛 Intake Counter: 1-Tap Voice entry & Fat/SNF formula calculator", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("2. 🚚 Delivery Routes: Live route progress bar, packing helper & 1-tap WhatsApp slips", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("3. 📲 Dues & Khatas: Bulk WhatsApp khata reminders & Bharat UPI QR code", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("4. 🌟 MilkMate Pro: Monthly ₹49 / Yearly ₹499 with Razorpay integration & UPI 0% fee", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }

        // ================= INTUITIVE 6-TAB CATEGORY SELECTOR =================
        var selectedTab by remember { mutableStateOf(0) }
        val settingsTabs = listOf(
            Triple(0, "🏢 Shop Profile", Icons.Default.Storefront),
            Triple(1, "⚡ Active Modules", Icons.Default.Tune),
            Triple(2, "📱 Display & Theme", Icons.Default.Palette),
            Triple(3, "⚙️ Hardware & Devices", Icons.Default.Devices),
            Triple(4, "🔒 Security & PIN", Icons.Default.Security),
            Triple(5, "💾 Cloud Sync & Backup", Icons.Default.CloudSync)
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = RoyalBluePrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            settingsTabs.forEach { (idx, tabTitle, icon) ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            tabTitle,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.5.sp
                        )
                    },
                    icon = {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        when (selectedTab) {
            // ================= TAB 0: DAIRY SHOP PROFILE & PRO =================
            0 -> {
                // Dairy Shop Information Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Dairy Shop Profile", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            IconButton(onClick = { showBusinessProfileDialog = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                            }
                        }

                        Text(
                            text = business?.businessName?.ifBlank { "My Dairy Farm" } ?: "My Dairy Farm",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "👤 Owner: ${business?.ownerName ?: "Owner"} • 📱 Contact: ${business?.phone ?: "Not set"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { showBusinessProfileDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Dairy Profile Details", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }

                // 🏢 Dairy Business ID & Staff Access Card
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("settings_business_id_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = RoyalBlueLight, modifier = Modifier.size(32.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Badge, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Dairy Business ID (Staff Access)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = RoyalBlueLight) {
                                Text("STAFF CODE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        val bizId = business?.id?.ifBlank { session.businessId } ?: session.businessId
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = bizId,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RoyalBluePrimary,
                                    letterSpacing = 1.sp
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Dairy Business ID", bizId)
                                        clipboard?.setPrimaryClip(clip)
                                        viewModel.showMessage("Business ID copied: $bizId ✓")
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Text(
                            text = "Delivery staff and farm workers need this Dairy ID, their registered mobile number, and their 4-digit PIN to log into their staff account.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Dairy Business ID", bizId)
                                    clipboard?.setPrimaryClip(clip)
                                    viewModel.showMessage("Business ID copied: $bizId ✓")
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy ID", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    val dName = business?.businessName ?: "Our Dairy"
                                    val inviteText = "Hello! Here are the login details for $dName on MilkMate:\n\n" +
                                            "• Dairy Business ID: $bizId\n" +
                                            "• Staff Login Steps:\n" +
                                            "1. Open the MilkMate App\n" +
                                            "2. Tap 'Quick Log In' -> 'Staff Login'\n" +
                                            "3. Enter the Dairy Business ID above, your registered mobile number, and your 4-digit PIN."
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(inviteText))
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, inviteText)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Dairy Business ID"))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.3f).height(38.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share on WhatsApp", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Master Account Start Date Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Account Start Date", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        Text(text = startDateStr, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = "Synchronizes your starting balances. Records cannot be created before this date.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { showEditStartDateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change Account Start Date", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }

                // Digital UPI Payments & WhatsApp Billing
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("UPI QR Payments & WhatsApp Note", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (session.upiId.isNotBlank()) DairyGreenLight else Color(0xFFFFF3E0)
                            ) {
                                Text(
                                    text = if (session.upiId.isNotBlank()) "ACTIVE" else "NOT SET",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (session.upiId.isNotBlank()) DairyGreen else FreshGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = if (session.upiId.isNotBlank()) "UPI ID: ${session.upiId}" else "No UPI ID set. Add your GPay, PhonePe, or Paytm UPI ID for auto QR payments.",
                            fontSize = 12.sp,
                            fontWeight = if (session.upiId.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                            color = if (session.upiId.isNotBlank()) RoyalBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Greeting note: \"${session.billNote}\"",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { showUpiDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Configure UPI ID & WhatsApp Bill Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Subscription Plan Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SUBSCRIPTION STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(shape = RoundedCornerShape(4.dp), color = DairyGreenLight) {
                                Text(
                                    text = business?.subscriptionPlan ?: "FREE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DairyGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text("MilkMate Pro Experience", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = "Unlimited customers, bulk buyers, multi-route delivery personnel management, advanced P&L reports, and automatic Cloud Sync.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = onNavigateToSubscription,
                            colors = ButtonDefaults.buttonColors(containerColor = FreshGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Text("⭐ View Plans & Upgrade to Pro", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }

            // ================= TAB 1: ACTIVE MODULES =================
            1 -> {
                // 🌟 Modular Feature Controls (Data-Safe with Explicit Save Changes Button)
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("modular_features_card"),
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
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = CircleShape,
                                    color = RoyalBluePrimary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = RoyalBluePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Dairy Operations & Active Modules", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                    Text("Turn tools on/off anytime. Zero rigid models.", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DairyGreenLight
                            ) {
                                Text(
                                    "🛡️ 100% DATA SAFE",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DairyGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Data Safety Guarantee Banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DairyGreenLight.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, DairyGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Turn on what your dairy does. All cattle, farmer, customer, and billing records remain 100% intact even when tools are toggled off.",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF1B5E20),
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        // 1-Tap Preset Profiles
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "⚡ 1-Tap Preset Profiles (Quick Setup):",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
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
                                                    draftSourceOwnCattle = true
                                                    draftSourceVillageFarmers = true
                                                    draftDestHouseholds = true
                                                    draftDestBulkCommercial = true
                                                    draftHasByproducts = true
                                                    draftHasCattleFeed = true
                                                }
                                                "FARMER_WHOLESALE" -> {
                                                    draftSourceOwnCattle = true
                                                    draftDestBulkCommercial = true
                                                    draftDestHouseholds = false
                                                    draftSourceVillageFarmers = false
                                                }
                                            }
                                        },
                                        label = {
                                            Text(
                                                label,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = RoyalBluePrimary,
                                            selectedLabelColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // Toggles
                        ModularFeatureRow(
                            icon = Icons.Default.WaterDrop,
                            iconColor = RoyalBluePrimary,
                            title = "🥛 Village Farmer Milk Collection Desk",
                            description = "Record milk intake from neighbor farmers, test FAT/SNF, generate printed slips & farmer ledgers",
                            checked = draftSourceVillageFarmers,
                            onCheckedChange = { isChecked -> draftSourceVillageFarmers = isChecked }
                        )

                        ModularFeatureRow(
                            icon = Icons.Default.Pets,
                            iconColor = GrassGreen,
                            title = "🐄 Cattle Herd & Milking Records",
                            description = "Cow & buffalo profiles, milking yield records, AI breeding dates & veterinary health",
                            checked = draftSourceOwnCattle,
                            onCheckedChange = { isChecked -> draftSourceOwnCattle = isChecked }
                        )

                        ModularFeatureRow(
                            icon = Icons.Default.LocalShipping,
                            iconColor = GoldenOrange,
                            title = "🚚 Doorstep Customer Delivery Routes",
                            description = "Morning & evening home delivery routes, bottle/packet drops & monthly customer khatas",
                            checked = draftDestHouseholds,
                            onCheckedChange = { isChecked -> draftDestHouseholds = isChecked }
                        )

                        ModularFeatureRow(
                            icon = Icons.Default.Restaurant,
                            iconColor = OceanMidnight,
                            title = "🏨 Hotels, Restaurants & B2B Commercial Supply",
                            description = "Dedicated B2B commercial accounts, wholesale pricing, bulk delivery challans & commercial statements",
                            checked = draftDestBulkCommercial,
                            onCheckedChange = { isChecked -> draftDestBulkCommercial = isChecked }
                        )

                        ModularFeatureRow(
                            icon = Icons.Default.Fastfood,
                            iconColor = DairyGreen,
                            title = "🧀 Milk Byproducts & Product Inventory",
                            description = "Manufacture paneer, ghee, curd/dahi, butter & khoya from raw milk, track yield & manage stock",
                            checked = draftHasByproducts,
                            onCheckedChange = { isChecked -> draftHasByproducts = isChecked }
                        )

                        ModularFeatureRow(
                            icon = Icons.Default.Agriculture,
                            iconColor = Color(0xFF795548),
                            title = "🌾 Cattle Feed & Fodder Management",
                            description = "Track cattle feed bags (khal, binola, churi, pellets, silage), feeding logs & farmer supply",
                            checked = draftHasCattleFeed,
                            onCheckedChange = { isChecked -> draftHasCattleFeed = isChecked }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // 💾 Explicit Save Changes Action Bar
                        if (isDraftModified) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = FreshGold.copy(alpha = 0.15f),
                                border = BorderStroke(1.5.dp, FreshGold),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("⚠️ Unsaved Feature Changes", fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp, color = TextPrimary)
                                        Text("Click Save Changes below to update your app", fontSize = 10.5.sp, color = TextSecondary)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                draftMode = business?.businessMode ?: "FARMER"
                                                draftSourceVillageFarmers = business?.sourceVillageFarmers ?: false
                                                draftSourceOwnCattle = business?.sourceOwnCattle ?: false
                                                draftDestHouseholds = business?.destHouseholds ?: false
                                                draftDestBulkCommercial = business?.destBulkCommercial ?: false
                                                draftHasByproducts = business?.hasPaneer == true || business?.hasCurdChaas == true || business?.hasGheeButter == true || business?.hasKhoyaSweets == true
                                                draftHasCattleFeed = business?.hasCattleFeed ?: false
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Discard", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                if (draftMode != business?.businessMode) {
                                                    viewModel.switchBusinessMode(draftMode)
                                                }
                                                viewModel.updateBusinessCapabilities(
                                                    sourceVillageFarmers = draftSourceVillageFarmers,
                                                    sourceOwnCattle = draftSourceOwnCattle,
                                                    destHouseholds = draftDestHouseholds,
                                                    destBulkCommercial = draftDestBulkCommercial,
                                                    destFactoryTankers = draftDestBulkCommercial,
                                                    hasPaneer = draftHasByproducts,
                                                    hasCurdChaas = draftHasByproducts,
                                                    hasGheeButter = draftHasByproducts,
                                                    hasKhoyaSweets = draftHasByproducts,
                                                    hasCattleFeed = draftHasCattleFeed
                                                )
                                                viewModel.showMessage("✅ Settings saved successfully!")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save Changes", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.showMessage("All feature settings are saved & up to date.")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(38.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Feature Settings Saved & Up To Date", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // ================= TAB 2: DISPLAY & THEME =================
            2 -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Display & Appearance", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Text(
                            text = "Customize theme, accent palette, font size, and UI regional language across the entire application.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { showAppearanceDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🎨 Open Theme & Font Style Customizer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLanguageDialog = true }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Translate, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Regional Language", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Current: ${session.languageCode.uppercase()} • 13 Indian Languages supported", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            Text("Change →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                        }


                    }
                }
            }

            // ================= TAB 3: HARDWARE & DEVICES =================
            3 -> {
                // BMC Kendra & Chiller Setup
                if (currentMode == "COLLECTION_CENTER") {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("bmc_settings_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(shape = CircleShape, color = RoyalBlueLight, modifier = Modifier.size(36.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Tune, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Column {
                                        Text("BMC Collection Centre & Kendra", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                        Text("Chilling unit, society code, hardware", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = RoyalBlueLight) {
                                    Text("🏢 BMC", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }

                            HorizontalDivider(color = BorderSubtle)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Chiller Vat Specifications", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        Text("Vat Capacity: 5,000 Liters • Target Temp: 4.0°C • Auto Agitator: Active", fontSize = 11.sp, color = GrassGreen)
                                    }
                                    Icon(Icons.Default.AcUnit, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                // Hardware Integration Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dairy Hardware Integrations", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // 1. Bluetooth Milk Analyzer & Scale Item
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showAnalyzerDialog = true }
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (session.analyzerDeviceAddress.isNotBlank()) DairyGreenLight else RoyalBlueLight,
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Bluetooth,
                                                    contentDescription = null,
                                                    tint = if (session.analyzerDeviceAddress.isNotBlank()) DairyGreen else RoyalBluePrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text("Bluetooth Milk Analyzer & Scale", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(
                                                text = if (session.analyzerDeviceAddress.isNotBlank())
                                                    "✓ ${session.analyzerDeviceName.ifBlank { "Connected Device" }} (${session.analyzerProtocol})"
                                                else
                                                    "Auto-capture FAT, SNF, and weight into intake desk",
                                                fontSize = 10.5.sp,
                                                color = if (session.analyzerDeviceAddress.isNotBlank()) DairyGreen else TextSecondary
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (session.analyzerDeviceAddress.isNotBlank()) DairyGreenLight else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = if (session.analyzerDeviceAddress.isNotBlank()) "CONFIGURED" else "TAP TO SETUP",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (session.analyzerDeviceAddress.isNotBlank()) DairyGreen else RoyalBluePrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = "Setup", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                // 2. Thermal Slip Printer Item
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showPrinterDialog = true }
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (session.printerDeviceAddress.isNotBlank()) FreshGoldLight else RoyalBlueLight,
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Print,
                                                    contentDescription = null,
                                                    tint = if (session.printerDeviceAddress.isNotBlank()) WarmHoney else RoyalBluePrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text("Thermal Slip Printer (2\" / 3\")", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(
                                                text = if (session.printerDeviceAddress.isNotBlank())
                                                    "✓ ${session.printerDeviceName.ifBlank { "Connected Printer" }} • ${session.printerPaperWidth}"
                                                else
                                                    "Instant farmer intake slip with FAT, Rate & Amount",
                                                fontSize = 10.5.sp,
                                                color = if (session.printerDeviceAddress.isNotBlank()) WarmHoney else TextSecondary
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (session.printerDeviceAddress.isNotBlank()) FreshGoldLight else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = if (session.printerDeviceAddress.isNotBlank()) {
                                                    if (session.printerAutoPrint) "AUTO-PRINT ON" else "READY"
                                                } else "TAP TO SETUP",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (session.printerDeviceAddress.isNotBlank()) WarmHoney else RoyalBluePrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = "Setup", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Control Hub Banner Notice
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = OceanBlueLight.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, OceanBlueAccent.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Operations Control Hub Integration", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = RoyalBluePrimary)
                            Text("Milk rate charts, FAT/SNF formulas, and custom expense heads live in the Operations Control Hub for real-time daily execution.", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // ================= TAB 4: APP SECURITY & PIN =================
            4 -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "APP SECURITY & PIN PROTECTION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("App Lock PIN", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text(
                                    text = if (session.appLockEnabled) "Active: 4-digit PIN required on opening" else "Disabled: Instant direct access",
                                    fontSize = 11.sp,
                                    color = if (session.appLockEnabled) DairyGreen else TextSecondary
                                )
                            }
                            Switch(
                                checked = session.appLockEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked && session.securityPin.isBlank()) {
                                        showSecurityDialog = true
                                    } else {
                                        viewModel.setSecuritySettings(appLockEnabled = isChecked)
                                    }
                                }
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSecurityDialog = true }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Pin, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Change 4-Digit Security PIN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        if (session.securityPin.isNotBlank()) "PIN Configured (••••)" else "Not Set - Tap to configure",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Text("Configure →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToStaff() }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.People, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Staff Access Roles & Route PINs", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Assign collection desk and driver permissions", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            Text("Staff Hub →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                        }
                    }
                }
            }

            // ================= TAB 5: BACKUP, SECURITY & ACCOUNT =================
            5 -> {
                // Cloud Sync & Data Safety Hub
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cloud Sync & JSON Backup", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (pendingSyncCount == 0) DairyGreenLight else Color(0xFFFFF3E0)
                            ) {
                                Text(
                                    text = if (pendingSyncCount == 0) "ALL SYNCED ✓" else "$pendingSyncCount QUEUED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pendingSyncCount == 0) DairyGreen else FreshGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Local offline-first Room database is synchronized with cloud storage. Export backups to safeguard against phone loss.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.triggerManualSync() },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text("🔄 Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.exportBackup { file ->
                                        val intent = viewModel.repository.exportManager.shareFileIntent(file, "application/json")
                                        context.startActivity(Intent.createChooser(intent, "Save MilkMate Backup JSON"))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.2f).height(38.dp)
                            ) {
                                Text("📥 Export Backup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("application/json") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📤 Restore from Backup JSON File", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 1-Tap Sample Data Reload Card
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("reset_sample_data_card"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("🧪 Sample Demo Data", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Reload test farmers, customers, collections & khatas for testing", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.clearAllDemoData() },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Clear", fontSize = 10.5.sp)
                                    }
                                    Button(
                                        onClick = { viewModel.resetAndReloadSampleData() },
                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Reload Sample", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Sample & Demo Data Option (Manual Load / Remove)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Science, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SAMPLE & PRACTICE DATA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Text(
                            text = "Explore the app in a dedicated Demo Workspace preloaded with sample cattle, customers, and delivery records, or return anytime to your real user workspace.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        val isDemoWorkspace = session.businessId == "BIZ-DEMO-01"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!isDemoWorkspace) {
                                Button(
                                    onClick = { viewModel.switchToDemoWorkspace() },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("🧪 Open Demo Workspace", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.returnToUserWorkspace() },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenAccent),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("🔙 Back to My Workspace", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Confirm Before Delete Safety
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Confirm Before Deletion", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Displays a safety confirmation dialog before removing deliveries, purchases, or expenses.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = session.confirmDelete,
                            onCheckedChange = { viewModel.setConfirmDelete(it) }
                        )
                    }
                }

                // Recycle Bin & Trash
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTrashDialog = true }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = WarmHoney, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Recently Deleted / Recycle Bin", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Restore ${deletedCustomers.size + deletedExpenses.size} deleted records", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text("Open →", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                // SUPPORT & USER FEEDBACK SECTION (3 CARDS)
                Text(
                    text = "SUPPORT & USER FEEDBACK",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBluePrimary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Card 1: Dairy Helpdesk & WhatsApp Support
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showHelpDialog = true }.testTag("settings_support_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = CircleShape, color = RoyalBlueLight, modifier = Modifier.size(38.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column {
                                Text("Customer & WhatsApp Support", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextPrimary)
                                Text("Chat: +91 8847881109 • support.milkmate@gmail.com", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        Text("Get Help →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                    }
                }

                // Card 2: Report a Bug
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showBugReportDialog = true }.testTag("settings_bug_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = CircleShape, color = DangerRedLight, modifier = Modifier.size(38.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.BugReport, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column {
                                Text("Report a Bug", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextPrimary)
                                Text("Something not working? Submit diagnostics to engineers", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        Text("Report →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                    }
                }

                // Card 3: Suggest a Feature
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showSuggestFeatureDialog = true }.testTag("settings_suggest_feature_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = CircleShape, color = FreshGoldLight, modifier = Modifier.size(38.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = FreshGold, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column {
                                Text("Suggest a Feature", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextPrimary)
                                Text("Have an idea to improve your dairy? Share with us", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        Text("Suggest →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                    }
                }

                // Logout & Session Reset Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Business Session ID: ${session.businessId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Role: ${session.role} • Registered: ${session.registeredPhone.ifBlank { "Mobile Verified" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Button(
                            onClick = { showLogoutConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Logout / Switch Account", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // MODAL DIALOGS
    // -------------------------------------------------------------

    // Modal: Edit Business Profile & Base Milk Rates
    if (showBusinessProfileDialog) {
        var nameText by remember { mutableStateOf(business?.businessName ?: "") }
        var ownerText by remember { mutableStateOf(business?.ownerName ?: "") }
        var phoneText by remember { mutableStateOf(business?.phone ?: "") }
        var cowRateText by remember { mutableStateOf((business?.cowMilkRate ?: 50.0).toString()) }
        var buffaloRateText by remember { mutableStateOf((business?.buffaloMilkRate ?: 65.0).toString()) }
        var modeText by remember { mutableStateOf(business?.pricingMode ?: "DIRECT") }

        AlertDialog(
            onDismissRequest = { showBusinessProfileDialog = false },
            title = { Text("Edit Business & Rates", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("Dairy Farm / Business Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ownerText,
                        onValueChange = { ownerText = it },
                        label = { Text("Owner Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phoneText,
                        onValueChange = { phoneText = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val bizSupportedTypes = business?.supportedMilkTypes ?: "BOTH"
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (bizSupportedTypes != "BUFFALO_ONLY") {
                            OutlinedTextField(
                                value = cowRateText,
                                onValueChange = { cowRateText = it },
                                label = { Text("Cow Rate (₹/L)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (bizSupportedTypes != "COW_ONLY") {
                            OutlinedTextField(
                                value = buffaloRateText,
                                onValueChange = { buffaloRateText = it },
                                label = { Text("Buffalo Rate (₹/L)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text("Rate Calculation & Yield Mode:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "DIRECT" to "💵 Direct",
                            "FAT_SNF" to "🧪 FAT+SNF",
                            "PANEER_YIELD" to "🧀 Paneer Yield",
                            "KHOA_YIELD" to "🍬 Khoya Yield",
                            "GHEE_YIELD" to "🏺 Ghee Yield",
                            "FAT_ONLY" to "🧈 FAT Only"
                        ).forEach { (m, lbl) ->
                            FilterChip(
                                selected = modeText == m,
                                onClick = { modeText = m },
                                label = { Text(lbl, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cow = cowRateText.toDoubleOrNull() ?: 50.0
                        val buf = buffaloRateText.toDoubleOrNull() ?: 65.0
                        viewModel.updateBusinessProfile(nameText, ownerText, phoneText, cow, buf, modeText)
                        showBusinessProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBusinessProfileDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Modal: Edit Master Account Start Date
    if (showEditStartDateDialog) {
        val currentStart = business?.accountStartDate ?: System.currentTimeMillis()
        val c = Calendar.getInstance().apply { timeInMillis = currentStart }
        var selYear by remember { mutableIntStateOf(c.get(Calendar.YEAR)) }
        var selMonth by remember { mutableIntStateOf(c.get(Calendar.MONTH)) }
        var selDay by remember { mutableIntStateOf(c.get(Calendar.DAY_OF_MONTH)) }

        val previewCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selYear)
            set(Calendar.MONTH, selMonth)
            set(Calendar.DAY_OF_MONTH, selDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val previewDateStr = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(previewCal.time)

        AlertDialog(
            onDismissRequest = { showEditStartDateDialog = false },
            title = { Text("Master Account Start Date", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose the exact date when your dairy started tracking on MilkMate. All inventory, milk collections, and month selectors strictly enforce this starting boundary.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = DairyGreenLight)
                    ) {
                        Text(
                            text = "New Start Date: $previewDateStr",
                            fontWeight = FontWeight.Bold,
                            color = DairyGreen,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    // Year Selection
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (2024..2030).forEach { yr ->
                            FilterChip(
                                selected = selYear == yr,
                                onClick = { selYear = yr },
                                label = { Text(yr.toString(), fontSize = 11.sp) }
                            )
                        }
                    }

                    // Month Selection
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                        months.forEachIndexed { idx, m ->
                            FilterChip(
                                selected = selMonth == idx,
                                onClick = { selMonth = idx },
                                label = { Text(m, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Day Selection
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (1..31).forEach { d ->
                            FilterChip(
                                selected = selDay == d,
                                onClick = { selDay = d },
                                label = { Text(d.toString(), fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateAccountStartDate(previewCal.timeInMillis)
                        showEditStartDateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen)
                ) {
                    Text("✓ Confirm Start Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditStartDateDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Modal: UPI ID & Bill Note Configuration
    if (showUpiDialog) {
        var upiText by remember { mutableStateOf(session.upiId) }
        var noteText by remember { mutableStateOf(session.billNote) }

        AlertDialog(
            onDismissRequest = { showUpiDialog = false },
            title = { Text("UPI & WhatsApp Billing", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your dairy UPI ID (Google Pay, PhonePe, Paytm, or BHIM). Customers will see this directly on their WhatsApp bills to pay seamlessly.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = upiText,
                        onValueChange = { upiText = it },
                        label = { Text("Dairy UPI ID (e.g. 9876543210@paytm)") },
                        placeholder = { Text("yourdairy@okaxis") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Bill Greeting / Thank You Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setUpiId(upiText)
                        viewModel.setBillNote(noteText)
                        showUpiDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
                ) {
                    Text("Save UPI Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpiDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Modal: Appearance & Uniform Color Palette / Font Scale
    if (showAppearanceDialog) {
        val themeModes = listOf("SYSTEM" to "📱 System", "LIGHT" to "☀️ Light", "DARK" to "🌙 Dark")
        val accents = listOf(
            "BLUE" to ("Royal Blue" to Color(0xFF1E3A8A)),
            "GREEN" to ("Dairy Green" to Color(0xFF059669)),
            "GOLD" to ("Harvest Gold" to Color(0xFFD97706)),
            "PURPLE" to ("Deep Violet" to Color(0xFF7C3AED)),
            "TEAL" to ("Ocean Teal" to Color(0xFF0F766E)),
            "RED" to ("Crimson Red" to Color(0xFFDC2626))
        )
        val fontScales = listOf(
            "SMALL" to "Compact (90%)",
            "MEDIUM" to "Standard (100%)",
            "LARGE" to "Easy Read (115%)",
            "EXTRA_LARGE" to "Sunlight (130%)"
        )

        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("App Uniform Style & Design", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
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
                        "Changes made here apply uniformly across all screens, headers, cards, and buttons throughout the entire application.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 1. Display Theme Mode
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Display Theme Mode:", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = TextPrimary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            themeModes.forEach { (mode, label) ->
                                val isSelected = session.themeMode == mode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = BorderSubtle)

                    // 2. Primary Color Theme Palette
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("App Color Theme Palette:", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = TextPrimary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            accents.forEach { (key, pair) ->
                                val (label, col) = pair
                                val isSelected = session.accentTheme.equals(key, true)
                                Card(
                                    modifier = Modifier
                                        .width(92.dp)
                                        .clickable { viewModel.setAccentTheme(key) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) col.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    ),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, col) else androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .background(col, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(label, fontSize = 10.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = BorderSubtle)

                    // 3. Font Size Scale & Legibility
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Text Size & Font Scale:", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = TextPrimary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            fontScales.forEach { (scaleKey, label) ->
                                val isSelected = session.fontScale.equals(scaleKey, true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setFontScale(scaleKey) },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    // Live Preview Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("✨ Theme Live Preview", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                "MilkMate Dairy OS — Uniform Design System",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "All headers, cards, and primary action buttons update live across every section.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAppearanceDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply & Close ✓", fontWeight = FontWeight.Bold)
                }
            }
        )
    }



    // Modal: Expense Categories Manager (Add, Edit, Delete custom categories)
    if (showExpenseCategoriesDialog) {
        ExpenseCategoriesManagerModal(
            session = session,
            viewModel = viewModel,
            onDismiss = { showExpenseCategoriesDialog = false }
        )
    }

    // Modal: Recently Deleted (Trash Recovery)
    if (showTrashDialog) {
        AlertDialog(
            onDismissRequest = { showTrashDialog = false },
            title = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🗑️ Trash / Recently Deleted", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    IconButton(onClick = { showTrashDialog = false }) { Icon(Icons.Default.Close, contentDescription = null) }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Recover records deleted by mistake. Tap 'Restore' to return them to active lists.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (deletedCustomers.isEmpty() && deletedExpenses.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Trash is empty. No deleted records found. ✓", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(deletedCustomers, key = { it.id }) { cust ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Customer: ${cust.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("${cust.type} • Due: ₹${cust.outstandingBalance.toInt()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Button(
                                            onClick = { viewModel.restoreCustomer(cust.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Restore", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }

                            items(deletedExpenses, key = { it.id }) { exp ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Expense: ${exp.category}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("₹${exp.amount.toInt()} • ${exp.notes}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Button(
                                            onClick = { viewModel.restoreExpense(exp.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Restore", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrashDialog = false }) { Text("Close") }
            }
        )
    }

    // Modal: Language Dialog
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLang = session.languageCode,
            onDismiss = { showLanguageDialog = false },
            onSelect = {
                viewModel.repository.sessionManager.setLanguage(it)
                viewModel.showMessage("Language switched to ${it.uppercase()} ✓")
                showLanguageDialog = false
            }
        )
    }

    // Modal: Help & Support
    if (showHelpDialog) {
        SupportHelpModal(
            session = session,
            businessName = business?.businessName ?: "My Dairy Farm",
            onDismiss = { showHelpDialog = false },
            onOpenBugReport = { showBugReportDialog = true },
            onOpenFeatureSuggest = { showSuggestFeatureDialog = true },
            onClearDemoData = {
                viewModel.clearAllDemoData()
                showHelpDialog = false
            }
        )
    }

    // Modal: Report a Bug
    if (showBugReportDialog) {
        ReportBugDialog(
            session = session,
            businessName = business?.businessName ?: "My Dairy Farm",
            onDismiss = { showBugReportDialog = false }
        )
    }

    // Modal: Suggest a Feature
    if (showSuggestFeatureDialog) {
        SuggestFeatureDialog(
            session = session,
            businessName = business?.businessName ?: "My Dairy Farm",
            onDismiss = { showSuggestFeatureDialog = false }
        )
    }

    // Modal: Security & PIN Settings
    if (showSecurityDialog) {
        SecuritySettingsDialog(
            currentPin = session.securityPin,
            currentPassword = session.securityPassword,
            biometricEnabled = session.biometricEnabled,
            appLockEnabled = session.appLockEnabled,
            onDismiss = { showSecurityDialog = false },
            onSave = { newPin, newPass, bioEnabled, lockEnabled ->
                viewModel.setSecuritySettings(
                    appLockEnabled = lockEnabled,
                    pin = newPin,
                    password = newPass,
                    biometricEnabled = bioEnabled
                )
                showSecurityDialog = false
            }
        )
    }

    // Modal: Logout Confirm
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Logout Confirmation", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out of this dairy business account on this device?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout()
                        showLogoutConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showAnalyzerDialog) {
        BluetoothMilkAnalyzerDialog(
            viewModel = viewModel,
            onDismiss = { showAnalyzerDialog = false }
        )
    }

    if (showPrinterDialog) {
        ThermalPrinterSetupDialog(
            viewModel = viewModel,
            onDismiss = { showPrinterDialog = false }
        )
    }
}

@Composable
fun SettingsGridRow(
    leftTitle: String,
    leftSub: String,
    leftIcon: ImageVector,
    onLeftClick: () -> Unit,
    rightTitle: String,
    rightSub: String,
    rightIcon: ImageVector,
    onRightClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.weight(1f).clickable { onLeftClick() }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = leftIcon, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(leftTitle.loc(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(leftSub, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        Row(
            modifier = Modifier.weight(1f).clickable { onRightClick() }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = rightIcon, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(rightTitle.loc(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(rightSub, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLang: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val languages = listOf(
        "en" to "English",
        "hi" to "हिन्दी — Hindi",
        "or" to "ଓଡ଼ିଆ — Odia",
        "bn" to "বাংলা — Bengali",
        "mr" to "मराठी — Marathi",
        "gu" to "ગુજરાતી — Gujarati",
        "te" to "తెలుగు — Telugu",
        "ta" to "தமிழ் — Tamil",
        "kn" to "ಕನ್ನಡ — Kannada",
        "ml" to "മലയാളം — Malayalam",
        "pa" to "ਪੰਜਾਬੀ — Punjabi",
        "as" to "অসমীয়া — Assamese",
        "hr" to "हरियाणवी — Haryanvi"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🌐 Language", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Choose your preferred regional dairy operations language.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                languages.forEach { (code, label) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(code) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentLang == code) RoyalBlueLight else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = label, fontWeight = if (currentLang == code) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                            if (currentLang == code) {
                                Text("Selected ✓", color = RoyalBluePrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun ExpenseCategoriesManagerModal(
    session: com.example.data.repository.UserSession,
    viewModel: MilkMateViewModel,
    onDismiss: () -> Unit
) {
    var newCategoryText by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var editedCategoryText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Expense Categories", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${session.customExpenseCategories.size} Active Categories", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add, rename, or delete categories used in your dairy farm expense records and P&L statements.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Add New Category Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Add New Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newCategoryText,
                                onValueChange = { newCategoryText = it },
                                placeholder = { Text("e.g. Silage, Doctor Fees", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).height(50.dp)
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
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Text("+ Add", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Category List
                Text("Configured Categories:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                session.customExpenseCategories.forEach { category ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        if (editingCategory == category) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = editedCategoryText,
                                    onValueChange = { editedCategoryText = it },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).height(46.dp)
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
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(if (category.contains("Feed", ignoreCase = true)) FreshGold else RoyalBluePrimary, CircleShape)
                                    )
                                    Text(category, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            editingCategory = category
                                            editedCategoryText = category
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }

                                    if (session.customExpenseCategories.size > 1) {
                                        IconButton(
                                            onClick = { viewModel.deleteExpenseCategory(category) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = { viewModel.resetExpenseCategories() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Categories to Default", fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)) {
                Text("Done")
            }
        }
    )
}

@Composable
fun SecuritySettingsDialog(
    currentPin: String,
    currentPassword: String,
    biometricEnabled: Boolean,
    appLockEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (newPin: String, newPassword: String, bioEnabled: Boolean, lockEnabled: Boolean) -> Unit
) {
    var pin by remember { mutableStateOf(currentPin) }
    var confirmPin by remember { mutableStateOf(currentPin) }
    var password by remember { mutableStateOf(currentPassword) }
    var bioState by remember { mutableStateOf(biometricEnabled) }
    var lockState by remember { mutableStateOf(appLockEnabled) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("App Lock & Security PIN", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Secure MilkMate on this device with a 4-digit PIN and fingerprint/screen lock.",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )

                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = DangerRed, fontSize = 11.sp)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Enable App Lock Protection", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = lockState,
                            onCheckedChange = { lockState = it }
                        )
                    }
                }

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4) {
                            pin = it.filter { c -> c.isDigit() }
                            errorMsg = null
                        }
                    },
                    label = { Text("4-Digit Security PIN *") },
                    placeholder = { Text("e.g. 1234") },
                    leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = RoyalBluePrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 4) {
                            confirmPin = it.filter { c -> c.isDigit() }
                            errorMsg = null
                        }
                    },
                    label = { Text("Confirm 4-Digit PIN *") },
                    placeholder = { Text("Re-enter PIN") },
                    leadingIcon = { Icon(Icons.Default.LockClock, contentDescription = null, tint = RoyalBluePrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fingerprint / Screen Lock", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Use device biometric sensor to unlock", fontSize = 10.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = bioState,
                            onCheckedChange = { bioState = it }
                        )
                    }
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMsg = null },
                    label = { Text("Master Password (Optional)") },
                    placeholder = { Text("For fallback account recovery") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = RoyalBluePrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (lockState && pin.length != 4) {
                        errorMsg = "PIN must be exactly 4 digits."
                    } else if (lockState && pin != confirmPin) {
                        errorMsg = "PIN codes do not match."
                    } else {
                        onSave(pin, password, bioState, lockState)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
            ) {
                Text("Save Security Settings")
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
private fun ModularFeatureRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(17.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = TextPrimary)
                Text(description, fontSize = 10.sp, color = TextSecondary, lineHeight = 13.sp)
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = iconColor,
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

