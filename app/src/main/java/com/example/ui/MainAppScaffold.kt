package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DairyStandardTopBar
import com.example.ui.components.PermissionGate
import com.example.ui.components.UniversalQuickActionSheet
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.util.AppStrings
import com.example.ui.util.BusinessModeFeatures
import com.example.ui.util.loc
import kotlinx.coroutines.launch

data class DynamicNavTab(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val permKey: String,
    val content: @Composable () -> Unit
)

sealed class BottomTab(val index: Int, val titleKey: String, val title: String, val icon: ImageVector) {
    object Home : BottomTab(0, "home", "Home", Icons.Default.Home)
    object Delivery : BottomTab(1, "delivery", "Delivery", Icons.Default.LocalShipping)
    object Customers : BottomTab(2, "customers", "Customers", Icons.Default.People)
    object Payment : BottomTab(3, "payment", "Payments", Icons.Default.Payment)
    object Manage : BottomTab(4, "manage", "Operations", Icons.Default.Tune)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: MilkMateViewModel) {
    var activeBottomTabId by remember { mutableStateOf("HOME") }
    var subScreen by remember { mutableStateOf<String?>(null) } // "PROFIT", "INVENTORY", "REPORTS", "SETTINGS", "SUBSCRIPTION", "STAFF", "AI_ASSISTANT", "EXPENSES", "ORDERS", "MANAGE", "COLLECTION_DESK"
    var aiInitialPrompt by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showBusinessModeDialog by remember { mutableStateOf(false) }
    var showHelpModal by remember { mutableStateOf(false) }
    var showBugModal by remember { mutableStateOf(false) }
    var showFeatureModal by remember { mutableStateOf(false) }
    var showQuickActionSheet by remember { mutableStateOf(false) }
    var showQuickStaffSwitchDialog by remember { mutableStateOf(false) }

    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val currentMode = business?.businessMode ?: "FARMER"
    val selectedShift by viewModel.selectedShift.collectAsStateWithLifecycle()
    val pendingSync by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val staffList by viewModel.staffList.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val dynamicTabs = remember(business, session, currentMode) {
        val list = mutableListOf<DynamicNavTab>()
        list.add(
            DynamicNavTab(
                id = "HOME",
                title = "Home",
                icon = Icons.Default.Home,
                permKey = "ALL",
                content = {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToTab = { idx ->
                            if (idx < list.size) {
                                activeBottomTabId = list[idx].id
                            }
                        },
                        onNavigateToTabId = { tabId ->
                            activeBottomTabId = tabId
                        },
                        onNavigateToDeliveries = {
                            if (list.any { it.id == "DELIVERY" }) {
                                activeBottomTabId = "DELIVERY"
                            } else {
                                subScreen = "PACKING_CONFIRMATION"
                            }
                        },
                        onOpenProfit = { subScreen = "PROFIT" },
                        onOpenInventory = { subScreen = "INVENTORY" },
                        onOpenReports = { subScreen = "REPORTS" },
                        onOpenManage = {
                            if (list.any { it.id == "MANAGE" }) {
                                activeBottomTabId = "MANAGE"
                            } else {
                                subScreen = "MANAGE"
                            }
                        },
                        onOpenExpenses = { subScreen = "EXPENSES" },
                        onOpenPayments = {
                            if (list.any { it.id == "PAYMENTS" }) {
                                activeBottomTabId = "PAYMENTS"
                            } else {
                                subScreen = "EXPENSES"
                            }
                        },
                        onOpenOrders = { subScreen = "ORDERS" },
                        onOpenCollectionDesk = { subScreen = "COLLECTION_DESK" },
                        onOpenCattleBreeding = { subScreen = "CATTLE_BREEDING" },
                        onOpenMilkProductionTable = { subScreen = "MILK_PRODUCTION_TABLE" },
                        onOpenFarmMilkAnalytics = { subScreen = "FARM_MILK_ANALYTICS" },
                        onOpenBusinessModeSelector = { subScreen = "SETTINGS" },
                        onOpenAIAssistant = { prompt ->
                            aiInitialPrompt = prompt
                            subScreen = "AI_ASSISTANT"
                        },
                        onOpenSettings = { subScreen = "SETTINGS" },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }
            )
        )

        if (BusinessModeFeatures.showHerdBreeding(business)) {
            list.add(
                DynamicNavTab(
                    id = "HERD",
                    title = "Herd",
                    icon = Icons.Default.Pets,
                    permKey = "CATTLE_HEALTH,HERD_MILKING",
                    content = {
                        CattleBreedingScreen(
                            viewModel = viewModel,
                            initialTab = 2,
                            onBack = { activeBottomTabId = "HOME" }
                        )
                    }
                )
            )
        }

        if (BusinessModeFeatures.showCollectionDesk(business)) {
            val title = if (currentMode == "COLLECTION_CENTER") "Intake" else "Collection"
            list.add(
                DynamicNavTab(
                    id = "COLLECTION",
                    title = title,
                    icon = Icons.Default.WaterDrop,
                    permKey = "MILK_COLLECTION",
                    content = {
                        FarmerCollectionScreen(
                            viewModel = viewModel,
                            initialTab = 0,
                            showInternalTabs = (currentMode == "TRADER" || currentMode == "INTEGRATED"),
                            onOpenSettings = { subScreen = "SETTINGS" },
                            onBack = { activeBottomTabId = "HOME" }
                        )
                    }
                )
            )
        }

        if (BusinessModeFeatures.showDeliveryRoutes(business)) {
            list.add(
                DynamicNavTab(
                    id = "DELIVERY",
                    title = "Deliveries",
                    icon = Icons.Default.LocalShipping,
                    permKey = "DELIVERIES",
                    content = {
                        DeliveryScreen(viewModel = viewModel)
                    }
                )
            )
        } else if (BusinessModeFeatures.showBulkDispatches(business)) {
            list.add(
                DynamicNavTab(
                    id = "DELIVERY",
                    title = "Dispatches",
                    icon = Icons.Default.LocalShipping,
                    permKey = "DELIVERIES",
                    content = {
                        DeliveryScreen(viewModel = viewModel)
                    }
                )
            )
        }

        if (list.size < 5 && BusinessModeFeatures.showBilling(business) && business?.destHouseholds == true) {
            list.add(
                DynamicNavTab(
                    id = "CUSTOMERS",
                    title = "Customers",
                    icon = Icons.Default.People,
                    permKey = "CUSTOMERS_KHATA",
                    content = {
                        CustomersScreen(viewModel = viewModel)
                    }
                )
            )
        }

        if (list.size < 5) {
            val payTitle = if (currentMode == "COLLECTION_CENTER") "Payouts" else "Payments"
            list.add(
                DynamicNavTab(
                    id = "PAYMENTS",
                    title = payTitle,
                    icon = Icons.Default.Payments,
                    permKey = "PAYMENTS_EXPENSES",
                    content = {
                        if (currentMode == "COLLECTION_CENTER") {
                            FarmerCollectionScreen(
                                viewModel = viewModel,
                                initialTab = 2,
                                showInternalTabs = false,
                                onOpenSettings = { subScreen = "SETTINGS" },
                                onBack = { activeBottomTabId = "HOME" }
                            )
                        } else {
                            PaymentsScreen(viewModel = viewModel)
                        }
                    }
                )
            )
        }

        if (list.size < 5) {
            list.add(
                DynamicNavTab(
                    id = "MANAGE",
                    title = "Operations",
                    icon = Icons.Default.DashboardCustomize,
                    permKey = "ALL,DELIVERIES,INVENTORY_FEED",
                    content = {
                        ManageHubScreen(
                            viewModel = viewModel,
                            onBack = { activeBottomTabId = "HOME" },
                            onOpenSettings = { subScreen = "SETTINGS" }
                        )
                    }
                )
            )
        }

        list.take(5)
    }

    val currentTab = dynamicTabs.find { it.id == activeBottomTabId } ?: dynamicTabs.first()
    val activeBottomTabIndex = dynamicTabs.indexOfFirst { it.id == activeBottomTabId }.coerceAtLeast(0)

    fun navigateToTab(tabId: String) {
        subScreen = null
        if (dynamicTabs.any { it.id == tabId }) {
            activeBottomTabId = tabId
        } else {
            when (tabId) {
                "HERD" -> subScreen = "CATTLE_BREEDING"
                "COLLECTION" -> subScreen = "COLLECTION_DESK"
                "DELIVERY" -> subScreen = "PACKING_CONFIRMATION"
                "CUSTOMERS" -> subScreen = "ORDERS"
                "PAYMENTS" -> subScreen = "EXPENSES"
                "MANAGE" -> subScreen = "MANAGE"
                else -> activeBottomTabId = "HOME"
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // BackHandler: Navigate back from subScreens or return to Home tab
    BackHandler(enabled = subScreen != null || activeBottomTabId != "HOME") {
        if (subScreen != null) {
            subScreen = null
        } else {
            activeBottomTabId = "HOME"
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. DRAWER HERO HEADER
                    Surface(
                        color = OceanMidnight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = FreshGold,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.WaterDrop,
                                            contentDescription = null,
                                            tint = OceanMidnight,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = (business?.businessName?.ifBlank { "MilkMate Dairy" } ?: "MilkMate Dairy").uppercase(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Smart Dairy Operating System",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            // Active Session Profile Chip with Quick Switch
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch { drawerState.close() }
                                        showQuickStaffSwitchDialog = true
                                    }
                                    .testTag("drawer_switch_profile_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (session.isStaff()) "📱 Staff: ${session.staffName}" else "👑 Dairy Owner Profile",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = if (session.isStaff()) "ID: ${session.staffId} • Tap to switch" else "Full Admin Access • Tap to switch",
                                            fontSize = 9.5.sp,
                                            color = Color.White.copy(alpha = 0.8f),
                                            maxLines = 1
                                        )
                                    }
                                    Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Profile", tint = FreshGold, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ================= HOME DASHBOARD =================
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        label = { Text("Home Dashboard", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp) },
                        badge = {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                Text("Live", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        },
                        selected = subScreen == null && activeBottomTabId == "HOME",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            navigateToTab("HOME")
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    // ================= ⚡ OPERATIONS CONTROL HUB (TOP FEATURED DRAWER ITEM) =================
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DairyGreenLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DairyGreen.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        NavigationDrawerItem(
                            icon = {
                                Surface(shape = CircleShape, color = DairyGreen, modifier = Modifier.size(28.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            label = {
                                Column {
                                    Text(
                                        "Operations Control Hub",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = DairyGreen
                                    )
                                    Text(
                                        "Shifts, Dispatch, Rates & Catalog",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            badge = {
                                Surface(shape = RoundedCornerShape(6.dp), color = DairyGreen) {
                                    Text(
                                        "Control",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            },
                            selected = subScreen == "MANAGE" || (subScreen == null && activeBottomTabId == "MANAGE"),
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                subScreen = "MANAGE"
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = DairyGreenLight
                            )
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    // ================= GROUP 1: MANAGE SALES =================
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MANAGE SALES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.tertiary,
                            letterSpacing = 0.5.sp
                        )
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text("Sales & Intake", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                        }
                    }

                    val isCollectionCore = BusinessModeFeatures.showCollectionDesk(business)
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.LocalDrink, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                        label = {
                            Column {
                                Text("Milk Collection Desk", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Record village farmer intake & FAT/SNF", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        badge = {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isCollectionCore) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (isCollectionCore) "Core Desk" else "Center/Trader",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCollectionCore) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        },
                        selected = subScreen == "COLLECTION_DESK",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "COLLECTION_DESK"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        label = {
                            Column {
                                Text("Deliveries & Distribution", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Routes, drop run-sheets & tankers", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        selected = subScreen == null && activeBottomTabId == "DELIVERY",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            navigateToTab("DELIVERY")
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        label = {
                            Column {
                                Text("Customers & Farmers Khata", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Ledgers, receivables & balances", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        badge = {
                            if (customers.isNotEmpty()) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text("${customers.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        },
                        selected = subScreen == null && activeBottomTabId == "CUSTOMERS",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            navigateToTab("CUSTOMERS")
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                        label = {
                            Column {
                                Text("Customer Orders & Slips", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Advance bookings & delivery notes", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        selected = subScreen == "ORDERS",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "ORDERS"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                        label = {
                            Column {
                                Text("Packing Confirmation & Recheck", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Pre-delivery checklist (leaves & orders)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        selected = subScreen == "PACKING_CONFIRMATION",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "PACKING_CONFIRMATION"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    // ================= GROUP 2: INVENTORY CONTROL =================
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INVENTORY CONTROL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 0.5.sp
                        )
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text("Stock & Ops", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                        }
                    }

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                        label = {
                            Column {
                                Text("Feed & Product Stock Inventory", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Cattle feed bags & low-stock alerts", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        badge = {
                            if (inventoryItems.isNotEmpty()) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text("${inventoryItems.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        },
                        selected = subScreen == "INVENTORY",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "INVENTORY"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    val isHerdCore = BusinessModeFeatures.showHerdBreeding(business)
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Pets, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                        label = {
                            Column {
                                Text("Cattle Herd & Breeding Lifecycle", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Lactation, AI breeding & vaccination", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        badge = {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isHerdCore) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (isHerdCore) "Core Herd" else "Farm Mode",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHerdCore) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        },
                        selected = (subScreen == null && activeBottomTabId == "HERD") || subScreen == "CATTLE_BREEDING",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            navigateToTab("HERD")
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        label = {
                            Column {
                                Text("Operations Control Hub", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Routes, shifts, rates & catalog", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        selected = subScreen == null && activeBottomTabId == "MANAGE",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            navigateToTab("MANAGE")
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        label = {
                            Column {
                                Text("Milk Wastage & Reconciliation", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Daily volume balance & spoilage loss", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        selected = subScreen == "MILK_WASTAGE",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "MILK_WASTAGE"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    // ================= GROUP 3: FINANCIAL REPORTS =================
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FINANCIAL REPORTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("P&L & Cash", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                        }
                    }

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                        label = {
                            Column {
                                Text("Payments Hub (Cash & UPI)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Record cash in, pay farmers & dues", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        selected = subScreen == null && activeBottomTabId == "PAYMENTS",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            navigateToTab("PAYMENTS")
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        label = {
                            Column {
                                Text("Dairy Operating Expenses", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Feed purchase, fuel & overheads", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        selected = subScreen == "EXPENSES",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "EXPENSES"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                        label = {
                            Column {
                                Text("Profit & Loss (P&L) Spread", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Revenue vs expenses & net margins", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        selected = subScreen == "PROFIT",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "PROFIT"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        label = {
                            Column {
                                Text("Analytics & Export Reports", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Periodic reports & PDF/Excel exports", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        selected = subScreen == "REPORTS",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "REPORTS"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    // ================= GROUP 4: SETTINGS =================
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SETTINGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("System & Tools", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                        }
                    }

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        label = {
                            Column {
                                Text("Dairy Settings & Milk Rates", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Configure formulas, rates & profile", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        selected = subScreen == "SETTINGS",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "SETTINGS"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                        label = {
                            Column {
                                Text("AI Dairy Advisor", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Gemini AI smart yield & profit advice", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        selected = subScreen == "AI_ASSISTANT",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            aiInitialPrompt = null
                            subScreen = "AI_ASSISTANT"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        label = {
                            Column {
                                Text("Staff & Attendance", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Daily worker roster & wage ledger", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        badge = {
                            if (staffList.isNotEmpty()) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text("${staffList.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        },
                        selected = subScreen == "STAFF",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "STAFF"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                        label = {
                            Column {
                                Text("MilkMate Pro Subscription", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Multi-user sync & premium tools", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        selected = subScreen == "SUBSCRIPTION",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "SUBSCRIPTION"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Support & Helpdesk
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.SupportAgent, contentDescription = null, tint = RoyalBluePrimary) },
                        label = { Text("WhatsApp & Customer Support", fontWeight = FontWeight.Medium, fontSize = 13.sp) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showHelpModal = true
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    // Report a Bug
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.BugReport, contentDescription = null, tint = DangerRed) },
                        label = { Text("Report a Bug", fontWeight = FontWeight.Medium, fontSize = 13.sp) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showBugModal = true
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    // Suggest a Feature
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Lightbulb, contentDescription = null, tint = FreshGold) },
                        label = { Text("Suggest a Feature", fontWeight = FontWeight.Medium, fontSize = 13.sp) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showFeatureModal = true
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Staff Switch Action
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = RoyalBluePrimary) },
                        label = { Text("Switch Staff / Profile", fontWeight = FontWeight.Bold, color = RoyalBluePrimary, fontSize = 13.sp) },
                        badge = {
                            Surface(shape = CircleShape, color = RoyalBlueLight) {
                                Text("Quick", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showQuickStaffSwitchDialog = true
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_switch_staff_item")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Logout Action
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        label = { Text("Logout Account", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showLogoutConfirm = true
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "MilkMate Dairy OS v2.0 • Offline-Ready",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    ) {
        val (currentHeaderTitle, currentHeaderSubtitle) = when {
            subScreen == "COLLECTION_DESK" -> "Milk Collection Desk" to "Farmer Milk Procurement & Testing"
            subScreen == "CATTLE_BREEDING" -> "Cattle Herd & Lifecycle" to "Milking, Insemination, CMT & Health"
            subScreen == "MILK_PRODUCTION_TABLE" -> "Milk Production Register" to "Batch Yield Entry by Animal"
            subScreen == "FARM_MILK_ANALYTICS" -> "Farm Milk Analytics" to "Daily Yield Trends & Cow/Buff Ratio"
            subScreen == "EXPENSES" -> "Dairy Operating Expenses" to "Feed, Fuel, Labor & Overheads"
            subScreen == "PROFIT" -> "Profit & Loss (P&L)" to "Revenue vs Expenses & Net Margins"
            subScreen == "REPORTS" -> "Analytics & Reports" to "Periodic Statements & PDF/Excel"
            subScreen == "SETTINGS" -> "Dairy Settings & Milk Rates" to "Formulas, Rates & Profile"
            subScreen == "AI_ASSISTANT" -> "AI Dairy Advisor" to "Gemini Smart Yield & Strategy"
            subScreen == "STAFF" -> "Staff & Attendance" to "Roster, Permissions & Activity Log"
            subScreen == "SUBSCRIPTION" -> "MilkMate Pro" to "Multi-User Sync & Premium Tools"
            subScreen == "INVENTORY" -> "Feed & Product Stock" to "Cattle Feed, Fodder & Byproducts"
            subScreen == "ORDERS" -> "Special Milk Orders" to "Advance Bookings & Events"
            subScreen == "MANAGE" -> "Operations" to "Routes, Shifts, Rates & Tools"
            subScreen == "MILK_WASTAGE" -> "Milk Wastage & Reconciliation" to "Daily Volume Balance & Spoilage"
            subScreen == "DELIVERY_PACKING" -> "Delivery Packing Confirmation" to "Morning/Evening Dispatch Prep"
            subScreen == "DAIRY_PROFILE_MODEL" -> "Dairy Profile & Model" to "Shop Identity, Modes & Pricing"
            subScreen == null && currentTab.id == "HERD" -> "Herd & Milking" to "Lactation, Daily Milk & Cattle Records"
            subScreen == null && currentTab.id == "COLLECTION" -> "Milk Collection Desk" to "Farmer Intake & FAT/SNF Testing"
            subScreen == null && currentTab.id == "DELIVERY" -> "Customer Deliveries" to "Doorstep Routes & Drop Ledger"
            subScreen == null && currentTab.id == "CUSTOMERS" -> "Customer Directory" to "Subscriptions, Khatas & Balances"
            subScreen == null && currentTab.id == "PAYMENTS" -> "Payments Hub" to "Cash, UPI, Dues & Advances"
            subScreen == null && currentTab.id == "MANAGE" -> "Operations" to "Rates, Shifts, Catalog & System Tools"
            else -> (business?.businessName?.ifBlank { "MilkMate Dairy" } ?: "MilkMate Dairy") to "Smart Dairy Operating System"
        }

        Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DairyStandardTopBar(
                title = currentHeaderTitle,
                subtitle = currentHeaderSubtitle,
                onBackClick = if (subScreen != null) { { subScreen = null } } else null,
                onMenuClick = if (subScreen == null) { { coroutineScope.launch { drawerState.open() } } } else null,
                businessMode = currentMode,
                onModeClick = { showBusinessModeDialog = true },
                selectedShift = selectedShift,
                onShiftToggle = {
                    val next = if (selectedShift == "MORNING") "EVENING" else "MORNING"
                    viewModel.setShift(next)
                },
                onQuickActionClick = if (subScreen != null || activeBottomTabId != "HOME") { { showQuickActionSheet = true } } else null,
                onAIClick = {
                    aiInitialPrompt = null
                    subScreen = "AI_ASSISTANT"
                },
                actions = {
                    // Notification Bell
                    IconButton(
                        onClick = { showNotificationDialog = true },
                        modifier = Modifier.size(30.dp).testTag("top_bar_notification_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = GoldenOrange,
                                    contentColor = Color.White
                                ) {
                                    Text("!", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications & Reminders",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    // Language Switcher
                    IconButton(
                        onClick = { showLanguageDialog = true },
                        modifier = Modifier.size(30.dp).testTag("top_bar_language_btn")
                    ) {
                        Icon(
                            Icons.Default.Translate,
                            contentDescription = "Language",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Settings Button
                    IconButton(
                        onClick = { subScreen = "SETTINGS" },
                        modifier = Modifier.size(30.dp).testTag("top_bar_settings_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (subScreen == "SETTINGS") RoyalBluePrimary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = if (subScreen == "SETTINGS") Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {},
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                tonalElevation = 4.dp
            ) {
                dynamicTabs.forEach { tabItem ->
                    val isSelected = subScreen == null && currentTab.id == tabItem.id
                    val localizedTitle = AppStrings.get(tabItem.title, session.languageCode).ifBlank { tabItem.title.loc() }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            subScreen = null
                            activeBottomTabId = tabItem.id
                        },
                        icon = {
                            Icon(
                                tabItem.icon,
                                contentDescription = localizedTitle,
                                modifier = Modifier.size(20.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = localizedTitle,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                subScreen == "COLLECTION_DESK" -> {
                    PermissionGate(
                        session = session,
                        permissionKey = "MILK_COLLECTION",
                        moduleTitle = "Milk Collection & Testing Desk",
                        onNavigateHome = { subScreen = null }
                    ) {
                        FarmerCollectionScreen(
                            viewModel = viewModel,
                            initialTab = 0,
                            showInternalTabs = false,
                            onOpenSettings = { subScreen = "SETTINGS" },
                            onBack = { subScreen = null }
                        )
                    }
                }
                subScreen == "CATTLE_BREEDING" -> {
                    PermissionGate(
                        session = session,
                        permissionKey = "CATTLE_HEALTH,HERD_MILKING",
                        moduleTitle = "Cattle Herd & Breeding Lifecycle",
                        onNavigateHome = { subScreen = null }
                    ) {
                        CattleBreedingScreen(viewModel = viewModel, initialTab = 2, onBack = { subScreen = null })
                    }
                }
                subScreen == "MILK_PRODUCTION_TABLE" -> {
                    PermissionGate(
                        session = session,
                        permissionKey = "HERD_MILKING",
                        moduleTitle = "Milk Production Register",
                        onNavigateHome = { subScreen = null }
                    ) {
                        CattleBreedingScreen(
                            viewModel = viewModel,
                            initialSubView = com.example.ui.screens.CattleSubView.MILK_PRODUCTION_TABLE,
                            onBack = { subScreen = null }
                        )
                    }
                }
                subScreen == "FARM_MILK_ANALYTICS" -> {
                    PermissionGate(
                        session = session,
                        permissionKey = "HERD_MILKING,REPORTS_ANALYTICS",
                        moduleTitle = "Farm Milk Production Analytics",
                        onNavigateHome = { subScreen = null }
                    ) {
                        CattleBreedingScreen(
                            viewModel = viewModel,
                            initialSubView = com.example.ui.screens.CattleSubView.FARM_MILK_ANALYTICS,
                            onBack = { subScreen = null }
                        )
                    }
                }
                subScreen == "MANAGE" -> {
                    PermissionGate(
                        session = session,
                        permissionKey = "ALL,INVENTORY_FEED,DELIVERIES",
                        moduleTitle = "Operations Control Hub",
                        onNavigateHome = { subScreen = null }
                    ) {
                        ManageHubScreen(
                            viewModel = viewModel,
                            onBack = { subScreen = null },
                            onOpenSettings = { subScreen = "SETTINGS" }
                        )
                    }
                }
                subScreen == "PROFIT" -> {
                    PermissionGate(
                        session = session,
                        permissionKey = "REPORTS_ANALYTICS,ALL",
                        moduleTitle = "Dairy Profit & Loss Analytics",
                        onNavigateHome = { subScreen = null }
                    ) {
                        ProfitScreen(viewModel = viewModel, onBack = { subScreen = null })
                    }
                }
                subScreen == "INVENTORY" -> {
                    PermissionGate(
                        session = session,
                        permissionKey = "INVENTORY_FEED",
                        moduleTitle = "Feed & Cattle Inventory",
                        onNavigateHome = { subScreen = null }
                    ) {
                        InventoryScreen(viewModel = viewModel, onBack = { subScreen = null })
                    }
                }
                subScreen == "REPORTS" -> {
                    PermissionGate(
                        session = session,
                        permissionKey = "REPORTS_ANALYTICS,ALL",
                        moduleTitle = "Financial Reports & Audit Slips",
                        onNavigateHome = { subScreen = null }
                    ) {
                        ReportsScreen(viewModel = viewModel, onBack = { subScreen = null })
                    }
                }
                subScreen == "MILK_WASTAGE" -> {
                    PermissionGate(
                        session = session,
                        permissionKey = "HERD_MILKING,MILK_COLLECTION",
                        moduleTitle = "Milk Wastage & Reconciliation",
                        onNavigateHome = { subScreen = null }
                    ) {
                        MilkWastageReconciliationScreen(viewModel = viewModel, onBack = { subScreen = null })
                    }
                }
                subScreen == "EXPENSES" -> {
                    PermissionGate(
                        session = session,
                        permissionKey = "PAYMENTS_EXPENSES",
                        moduleTitle = "Farm Expenses Register",
                        onNavigateHome = { subScreen = null }
                    ) {
                        ExpensesScreen(viewModel = viewModel)
                    }
                }
                subScreen == "ORDERS" -> {
                    OrdersScreen(viewModel = viewModel)
                }
                subScreen == "PACKING_CONFIRMATION" -> {
                    PermissionGate(
                        session = session,
                        permissionKey = "DELIVERIES,INVENTORY_FEED",
                        moduleTitle = "Packaging & Dispatch Batches",
                        onNavigateHome = { subScreen = null }
                    ) {
                        PackingConfirmationScreen(
                            viewModel = viewModel,
                            onBack = { subScreen = null },
                            onStartDelivery = {
                                navigateToTab("DELIVERY")
                            }
                        )
                    }
                }
                subScreen == "SETTINGS" -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToSubscription = { subScreen = "SUBSCRIPTION" },
                        onNavigateToStaff = { subScreen = "STAFF" },
                        onBack = { subScreen = null }
                    )
                }
                subScreen == "DAIRY_PROFILE_MODEL" -> {
                    DairyProfileAndModelScreen(
                        viewModel = viewModel,
                        onBack = { subScreen = "SETTINGS" }
                    )
                }
                subScreen == "SUBSCRIPTION" -> {
                    SubscriptionScreen(viewModel = viewModel, onBack = { subScreen = null })
                }
                subScreen == "STAFF" -> {
                    PermissionGate(
                        session = session,
                        permissionKey = "ALL",
                        moduleTitle = "Staff & Payroll Management",
                        onNavigateHome = { subScreen = null }
                    ) {
                        StaffScreen(viewModel = viewModel, onBack = { subScreen = null })
                    }
                }
                subScreen == "AI_ASSISTANT" -> {
                    GeminiChatScreen(
                        viewModel = viewModel,
                        onBack = { subScreen = null },
                        initialPrompt = aiInitialPrompt
                    )
                }
                else -> {
                    PermissionGate(
                        session = session,
                        permissionKey = currentTab.permKey,
                        moduleTitle = currentTab.title,
                        onNavigateHome = { activeBottomTabId = "HOME" }
                    ) {
                        currentTab.content()
                    }
                }
            }
        }
    }
    }

    // Logout Confirm Dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Logout Confirmation", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out from this MilkMate account?") },
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

    // Language Selection Modal
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

    // Notification & Reminders Dialog
    if (showNotificationDialog) {
        NotificationCenterDialog(
            viewModel = viewModel,
            onDismiss = { showNotificationDialog = false },
            onNavigateToDelivery = {
                showNotificationDialog = false
                navigateToTab("DELIVERY")
            },
            onNavigateToPayments = {
                showNotificationDialog = false
                navigateToTab("PAYMENTS")
            },
            onNavigateToExpenses = {
                showNotificationDialog = false
                subScreen = "EXPENSES"
            },
            onNavigateToFarm = {
                showNotificationDialog = false
                navigateToTab("HERD")
            },
            onNavigateToInventory = {
                showNotificationDialog = false
                subScreen = "INVENTORY"
            }
        )
    }

    // Universal Quick Action Sheet
    if (showQuickActionSheet) {
        UniversalQuickActionSheet(
            businessMode = currentMode,
            onDismiss = { showQuickActionSheet = false },
            onOpenOperationsHub = {
                subScreen = "MANAGE"
            },
            onRecordMilking = {
                subScreen = "MILK_PRODUCTION_TABLE"
            },
            onFarmerCollection = {
                navigateToTab("COLLECTION")
            },
            onCustomerDelivery = {
                navigateToTab("DELIVERY")
            },
            onRecordPayment = {
                navigateToTab("PAYMENTS")
            },
            onCattleHealth = {
                subScreen = "CATTLE_BREEDING"
            },
            onRecordExpense = {
                subScreen = "EXPENSES"
            },
            onStaffAttendance = {
                subScreen = "STAFF"
            },
            onOpenBusinessModeSelector = {
                showBusinessModeDialog = true
            }
        )
    }

    // Business Mode Selector Dialog
    if (showBusinessModeDialog) {
        BusinessModeSelectorDialog(
            currentMode = currentMode,
            onDismiss = { showBusinessModeDialog = false },
            onSelectMode = { newMode ->
                viewModel.switchBusinessMode(newMode)
                showBusinessModeDialog = false
                viewModel.showMessage("Switched to ${BusinessModeFeatures.getByType(newMode).title} ✓")
            }
        )
    }

    // Support & Helpdesk Modal
    if (showHelpModal) {
        SupportHelpModal(
            session = session,
            businessName = business?.businessName ?: "My Dairy Farm",
            onDismiss = { showHelpModal = false },
            onOpenBugReport = { showBugModal = true },
            onOpenFeatureSuggest = { showFeatureModal = true },
            onClearDemoData = {
                viewModel.clearAllDemoData()
                showHelpModal = false
            }
        )
    }

    // Report a Bug Dialog
    if (showBugModal) {
        ReportBugDialog(
            session = session,
            businessName = business?.businessName ?: "My Dairy Farm",
            onDismiss = { showBugModal = false }
        )
    }

    // Suggest a Feature Dialog
    if (showFeatureModal) {
        SuggestFeatureDialog(
            session = session,
            businessName = business?.businessName ?: "My Dairy Farm",
            onDismiss = { showFeatureModal = false }
        )
    }

    // Quick Staff Switch Dialog
    if (showQuickStaffSwitchDialog) {
        QuickStaffSwitchDialog(
            currentSession = session,
            business = business,
            staffList = staffList,
            onDismiss = { showQuickStaffSwitchDialog = false },
            onSwitchToOwner = {
                viewModel.switchToOwnerSession()
                showQuickStaffSwitchDialog = false
            },
            onSwitchToStaff = { staff, pin ->
                val bizId = business?.id?.ifBlank { session.businessId } ?: session.businessId
                viewModel.loginStaff(bizId, staff.id, pin) { success, errorMsg ->
                    if (success) {
                        viewModel.showMessage("Switched account to ${staff.name} (${staff.displayRole()}) ✓")
                        showQuickStaffSwitchDialog = false
                    } else {
                        viewModel.showMessage(errorMsg ?: "Failed to switch staff account")
                    }
                }
            }
        )
    }
}

/**
 * 🔄 Quick Modal for switching active staff or owner profiles on shared devices
 */
@Composable
fun QuickStaffSwitchDialog(
    currentSession: com.example.data.repository.UserSession,
    business: com.example.data.local.entity.BusinessEntity?,
    staffList: List<com.example.data.local.entity.StaffEntity>,
    onDismiss: () -> Unit,
    onSwitchToOwner: () -> Unit,
    onSwitchToStaff: (staff: com.example.data.local.entity.StaffEntity, pin: String) -> Unit
) {
    var selectedStaff by remember { mutableStateOf<com.example.data.local.entity.StaffEntity?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val activeStaffList = remember(staffList) {
        staffList.filter { it.pin.isNotBlank() && it.isActive }
    }
    val bizId = business?.id?.ifBlank { currentSession.businessId } ?: currentSession.businessId
    val dairyName = business?.businessName ?: "MilkMate Dairy"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = RoyalBlueLight,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.People, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Switch Active Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Dairy ID: $bizId • $dairyName", fontSize = 11.5.sp, color = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select account for shared delivery phones or collection counters:",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Owner Option
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentSession.isOwner() && selectedStaff == null) RoyalBlueLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (currentSession.isOwner() && selectedStaff == null) RoyalBluePrimary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSwitchToOwner() }
                        .testTag("switch_to_owner_card")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = FreshGold.copy(alpha = 0.2f), modifier = Modifier.size(32.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFD84315), modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Dairy Owner Profile", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Full Admin Access across all modules", fontSize = 10.5.sp, color = TextSecondary)
                            }
                        }
                        if (currentSession.isOwner()) {
                            Surface(shape = RoundedCornerShape(4.dp), color = DairyGreenLight) {
                                Text("ACTIVE", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = DairyGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text("Staff Accounts (App Login Enabled):", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                if (activeStaffList.isEmpty()) {
                    Text(
                        text = "No staff members with app login created yet. Add staff with 'Enable App Login' turned on in the Staff tab.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    activeStaffList.forEach { staff ->
                        val isSelected = selectedStaff?.id == staff.id
                        val isCurrentActive = currentSession.isStaff() && currentSession.staffId == staff.id

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) RoyalBlueLight else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) RoyalBluePrimary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedStaff = staff
                                    enteredPin = ""
                                    errorMessage = null
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = RoyalBlueLight,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    if (staff.role == "DELIVERY_BOY") Icons.Default.LocalShipping else Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = RoyalBluePrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(staff.name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                            Text(
                                                "${staff.displayRole()} • ID: ${staff.id}",
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    if (isCurrentActive) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = DairyGreenLight) {
                                            Text("ACTIVE", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = DairyGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }

                                if (isSelected && !isCurrentActive) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = RoyalBluePrimary.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = enteredPin,
                                            onValueChange = {
                                                if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                                    enteredPin = it
                                                    errorMessage = null
                                                }
                                            },
                                            label = { Text("4-Digit PIN") },
                                            placeholder = { Text("••••") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f).testTag("quick_switch_pin_input")
                                        )

                                        Button(
                                            onClick = {
                                                if (enteredPin == staff.pin) {
                                                    onSwitchToStaff(staff, enteredPin)
                                                } else {
                                                    errorMessage = "Incorrect PIN for ${staff.name}"
                                                }
                                            },
                                            enabled = enteredPin.length == 4,
                                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                            modifier = Modifier.testTag("quick_switch_confirm_btn")
                                        ) {
                                            Text("Switch")
                                        }
                                    }

                                    if (errorMessage != null) {
                                        Text(
                                            errorMessage!!,
                                            fontSize = 11.sp,
                                            color = DangerRed,
                                            fontWeight = FontWeight.SemiBold
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
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
