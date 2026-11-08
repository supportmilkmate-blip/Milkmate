package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.DeliveryEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.PaymentEntity
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures
import com.example.ui.util.loc
import com.example.ui.util.appStr
import java.text.SimpleDateFormat
import java.util.*

enum class ChartTimeRange(val label: String, val days: Int) {
    TODAY("Today (Live)", 1),
    WEEK_7D("7-Day Trend", 7),
    MONTH_30D("30-Day Output", 30)
}

enum class ProductionChartType(val label: String, val icon: ImageVector) {
    DUAL_BAR("Cow vs Buffalo", Icons.Default.BarChart),
    STACKED("Stacked Total", Icons.Default.StackedBarChart),
    RATIO_DONUT("Ratio Split", Icons.Default.PieChart)
}

data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

data class Tuple8<A, B, C, D, E, F, G, H>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
    val seventh: G,
    val eighth: H
)

data class DashboardSlideItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val themeColor: Color
)

data class DynamicFrequentTask(
    val id: String,
    val label: String,
    val fullLabel: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
    val action: () -> Unit
)

data class DashQuickAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tintColor: Color,
    val bgColor: Color,
    val onClick: () -> Unit
)

data class UniversalQuickActionItem(
    val title: String,
    val subtitle: String,
    val tag: String,
    val tagColor: Color,
    val icon: ImageVector,
    val iconColor: Color,
    val testTag: String,
    val onClick: () -> Unit
)

data class DailyProductionPoint(
    val dayLabel: String,
    val fullDate: String,
    val cowLiters: Double,
    val buffLiters: Double,
    val totalLiters: Double,
    val cowRevenue: Double,
    val buffRevenue: Double,
    val totalRevenue: Double,
    val isToday: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MilkMateViewModel,
    onNavigateToTab: (Int) -> Unit = {},
    onNavigateToTabId: ((String) -> Unit)? = null,
    onNavigateToDeliveries: () -> Unit = {},
    onOpenProfit: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenManage: () -> Unit,
    onOpenExpenses: () -> Unit = {},
    onOpenPayments: () -> Unit = {},
    onOpenOrders: () -> Unit = {},
    onOpenCollectionDesk: () -> Unit = {},
    onOpenCattleBreeding: () -> Unit = {},
    onOpenMilkProductionTable: () -> Unit = {},
    onOpenFarmMilkAnalytics: () -> Unit = {},
    onOpenBusinessModeSelector: () -> Unit = {},
    onOpenAIAssistant: ((String?) -> Unit)? = null,
    onOpenSettings: () -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val currentMode = business?.businessMode ?: "FARMER"
    val todayDeliveries by viewModel.todayDeliveries.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val selectedShift by viewModel.selectedShift.collectAsStateWithLifecycle()
    
    // Farmer Collections & Dispatches flows
    val farmers by viewModel.farmers.collectAsStateWithLifecycle()
    val todayCollections by viewModel.todayMilkCollections.collectAsStateWithLifecycle()
    val allCollections by viewModel.allMilkCollections.collectAsStateWithLifecycle()
    val bulkDispatches by viewModel.bulkDispatches.collectAsStateWithLifecycle()
    val cattleList by viewModel.cattleList.collectAsStateWithLifecycle()
    val allMilkingRecords by viewModel.allMilkingRecords.collectAsStateWithLifecycle()
    val allDeliveries by viewModel.allDeliveries.collectAsStateWithLifecycle()

    val supportedTypes = business?.supportedMilkTypes ?: "BOTH"
    val isCowEnabled = BusinessModeFeatures.isCowEnabled(supportedTypes)
    val isBuffaloEnabled = BusinessModeFeatures.isBuffaloEnabled(supportedTypes)

    var selectedDayIndex by remember { mutableIntStateOf(3) }
    var showCustomFormulaStudio by remember { mutableStateOf(false) }
    var showMilkingChoiceDialog by remember { mutableStateOf(false) }

    val todayMidnight = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayEnd = remember(todayMidnight) { todayMidnight + 86400000L - 1L }

    // Milk Volumes (Live Today)
    val milkSoldDeliveries = todayDeliveries.filter { it.customerType != "SUPPLIER" && it.isDelivered }
    val cowSoldLiters = if (isCowEnabled) milkSoldDeliveries.filter { it.milkType == "COW" }.sumOf { it.quantityLiters } else 0.0
    val buffSoldLiters = if (isBuffaloEnabled) milkSoldDeliveries.filter { it.milkType == "BUFFALO" }.sumOf { it.quantityLiters } else 0.0
    val milkSoldLiters = cowSoldLiters + buffSoldLiters

    val milkProcuredDeliveries = todayDeliveries.filter { it.customerType == "SUPPLIER" && it.isDelivered }
    val milkPurchasedLiters = milkProcuredDeliveries.sumOf { it.quantityLiters }

    // Financial Metrics
    val totalSales = milkSoldDeliveries.sumOf { it.totalAmount }
    val supplierMilkPurchases = milkProcuredDeliveries.sumOf { it.totalAmount }
    val todayInventoryPurchases = expenses.filter {
        it.date in todayMidnight..todayEnd && (it.isInventoryPurchase || it.category == "INVENTORY_PURCHASE")
    }.sumOf { it.amount }
    val totalPurchases = supplierMilkPurchases + todayInventoryPurchases
    val totalTodayExpenses = expenses.filter {
        it.date in todayMidnight..todayEnd && it.isBusiness()
    }.sumOf { it.amount }
    val todayPersonalExpenses = expenses.filter {
        it.date in todayMidnight..todayEnd && it.isPersonal()
    }.sumOf { it.amount }

    val netCashMovement = totalSales - totalPurchases - totalTodayExpenses - todayPersonalExpenses

    // Outstanding Dues & Balances
    val totalReceivable = customers.filter { it.outstandingBalance > 0 }.sumOf { it.outstandingBalance }
    val totalPayable = customers.filter { it.outstandingBalance < 0 }.sumOf { -it.outstandingBalance }
    val todayCollectedPayments = payments.filter { it.date in todayMidnight..todayEnd }.sumOf { it.amount }

    // Delivery progress
    val totalCustomerTarget = customers.count { it.type != "SUPPLIER" }
    val completedDeliveries = milkSoldDeliveries.size

    // Inventory & Orders Status
    val lowStockItems = inventoryItems.filter { it.stockAlertStatus != "NORMAL" }
    val lowStockCount = lowStockItems.size

    // Shift Collections
    val shiftCollections = todayCollections.filter { it.shift == selectedShift }
    val totalCollectedLiters = shiftCollections.sumOf { it.quantityLiters }
    val avgProcureFat = if (shiftCollections.isNotEmpty()) shiftCollections.map { it.fat }.average() else 0.0
    val totalDispatchedLiters = bulkDispatches.filter { it.dateEpochMidnight == todayMidnight }.sumOf { it.totalLiters }

    // Cattle summary
    val scopedDashboardCattle = remember(cattleList, supportedTypes) {
        when (supportedTypes) {
            "COW_ONLY" -> cattleList.filter { it.type.equals("COW", ignoreCase = true) }
            "BUFFALO_ONLY" -> cattleList.filter { it.type.equals("BUFFALO", ignoreCase = true) }
            else -> cattleList
        }
    }
    val lactatingCattle = scopedDashboardCattle.count { it.lactationStage == "LACTATING" }
    val totalCattle = scopedDashboardCattle.size

    // 7-day trend data
    val productionDataPoints = remember(allMilkingRecords, cattleList, allDeliveries, business, supportedTypes) {
        val cowRate = business?.cowMilkRate ?: 55.0
        val buffRate = business?.buffaloMilkRate ?: 75.0
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("EEE", Locale.getDefault())
        val fullSdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val buffaloIdSet = cattleList.filter { it.type.equals("BUFFALO", ignoreCase = true) }.map { it.id }.toSet()

        (6 downTo 0).map { daysAgo ->
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startMs = dayCal.timeInMillis
            val endMs = startMs + 86400000L - 1L

            val dayMilking = allMilkingRecords.filter { it.dateEpochMidnight in startMs..endMs }
            val dayDeliveries = allDeliveries.filter { it.deliveryDate in startMs..endMs && it.isDelivered }

            val cowL = if (isCowEnabled) {
                val farmY = dayMilking.filter { it.cattleId !in buffaloIdSet }.sumOf { it.quantityLiters }
                if (farmY > 0.0) farmY else dayDeliveries.filter { it.milkType == "COW" }.sumOf { it.quantityLiters }
            } else 0.0

            val buffL = if (isBuffaloEnabled) {
                val farmY = dayMilking.filter { it.cattleId in buffaloIdSet || it.cattleTag.contains("BUF", true) }.sumOf { it.quantityLiters }
                if (farmY > 0.0) farmY else dayDeliveries.filter { it.milkType == "BUFFALO" }.sumOf { it.quantityLiters }
            } else 0.0

            val tot = cowL + buffL
            DailyProductionPoint(
                dayLabel = sdf.format(Date(startMs)),
                fullDate = fullSdf.format(Date(startMs)),
                cowLiters = cowL,
                buffLiters = buffL,
                totalLiters = tot,
                cowRevenue = cowL * cowRate,
                buffRevenue = buffL * buffRate,
                totalRevenue = (cowL * cowRate) + (buffL * buffRate),
                isToday = daysAgo == 0
            )
        }
    }

    val todayFormatted = remember {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
    }

    // Dynamic Task Frequency Tracking & Recommendation
    val context = LocalContext.current
    val sharedPrefs = remember(context) {
        context.getSharedPreferences("milkmate_frequent_tasks", Context.MODE_PRIVATE)
    }

    var milkIntakeClicks by remember { mutableIntStateOf(sharedPrefs.getInt("click_LOG_MILK", 5)) }
    var deliveryClicks by remember { mutableIntStateOf(sharedPrefs.getInt("click_ADD_DELIVERY", 3)) }
    var paymentClicks by remember { mutableIntStateOf(sharedPrefs.getInt("click_COLLECT_PAYMENT", 2)) }
    var expenseClicks by remember { mutableIntStateOf(sharedPrefs.getInt("click_LOG_EXPENSE", 1)) }
    var userOverrideTaskId by remember { mutableStateOf(sharedPrefs.getString("override_task_id", null)) }

    fun incrementTaskClick(taskId: String) {
        val editor = sharedPrefs.edit()
        when (taskId) {
            "LOG_MILK" -> {
                val next = milkIntakeClicks + 1
                milkIntakeClicks = next
                editor.putInt("click_LOG_MILK", next)
            }
            "ADD_DELIVERY" -> {
                val next = deliveryClicks + 1
                deliveryClicks = next
                editor.putInt("click_ADD_DELIVERY", next)
            }
            "COLLECT_PAYMENT" -> {
                val next = paymentClicks + 1
                paymentClicks = next
                editor.putInt("click_COLLECT_PAYMENT", next)
            }
            "LOG_EXPENSE" -> {
                val next = expenseClicks + 1
                expenseClicks = next
                editor.putInt("click_LOG_EXPENSE", next)
            }
        }
        editor.apply()
    }

    val allDynamicTasks = remember(
        milkIntakeClicks, deliveryClicks, paymentClicks, expenseClicks,
        todayCollections.size, todayDeliveries.size, payments.size, expenses.size,
        completedDeliveries, totalCustomerTarget, totalReceivable, todayCollectedPayments
    ) {
        val milkScore = (milkIntakeClicks * 3) + (todayCollections.size * 2) + (if (todayCollections.isEmpty()) 4 else 0)
        val deliveryScore = (deliveryClicks * 3) + (todayDeliveries.size * 2) + (if (completedDeliveries < totalCustomerTarget) 3 else 0)
        val paymentScore = (paymentClicks * 3) + (todayCollectedPayments.toInt() / 100) + (if (totalReceivable > 1000) 4 else 0)
        val expenseScore = (expenseClicks * 3) + (expenses.size)

        listOf(
            Triple("LOG_MILK", milkScore, DynamicFrequentTask(
                id = "LOG_MILK",
                label = "Log Milk",
                fullLabel = "Log Milk Intake",
                icon = Icons.Default.WaterDrop,
                containerColor = RoyalBluePrimary,
                contentColor = Color.White,
                action = {
                    incrementTaskClick("LOG_MILK")
                    onOpenCollectionDesk()
                }
            )),
            Triple("ADD_DELIVERY", deliveryScore, DynamicFrequentTask(
                id = "ADD_DELIVERY",
                label = "Add Delivery",
                fullLabel = "Add Delivery Route",
                icon = Icons.Default.LocalShipping,
                containerColor = Color(0xFF2E7D32),
                contentColor = Color.White,
                action = {
                    incrementTaskClick("ADD_DELIVERY")
                    onNavigateToDeliveries()
                }
            )),
            Triple("COLLECT_PAYMENT", paymentScore, DynamicFrequentTask(
                id = "COLLECT_PAYMENT",
                label = "Collect Payment",
                fullLabel = "Collect Payment & Dues",
                icon = Icons.Default.Payments,
                containerColor = Color(0xFFD97706),
                contentColor = Color.White,
                action = {
                    incrementTaskClick("COLLECT_PAYMENT")
                    onOpenPayments()
                }
            )),
            Triple("LOG_EXPENSE", expenseScore, DynamicFrequentTask(
                id = "LOG_EXPENSE",
                label = "Log Expense",
                fullLabel = "Log Farm Expense",
                icon = Icons.Default.ReceiptLong,
                containerColor = DangerRed,
                contentColor = Color.White,
                action = {
                    incrementTaskClick("LOG_EXPENSE")
                    onOpenExpenses()
                }
            ))
        ).sortedByDescending { it.second }
    }

    val currentDynamicTask = remember(allDynamicTasks, userOverrideTaskId) {
        if (userOverrideTaskId != null) {
            allDynamicTasks.find { it.first == userOverrideTaskId }?.third ?: allDynamicTasks.first().third
        } else {
            allDynamicTasks.first().third
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceBg)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        // ================= 1. REFINED EXECUTIVE GREETING & SHIFT STATUS =================
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val greeting = if (selectedShift == "MORNING") "☀️ Morning Session" else "🌆 Evening Session"
                    Text(
                        text = greeting,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text = todayFormatted,
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Tactile Shift Toggle Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedShift == "MORNING") FreshGoldLight else MaterialTheme.colorScheme.primaryContainer,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedShift == "MORNING") WarmHoney.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.clickable {
                            val next = if (selectedShift == "MORNING") "EVENING" else "MORNING"
                            viewModel.setShift(next)
                            viewModel.showMessage("Switched to $next Shift")
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedShift == "MORNING") Icons.Default.WbSunny else Icons.Default.NightsStay,
                                contentDescription = null,
                                tint = if (selectedShift == "MORNING") WarmHoney else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (selectedShift == "MORNING") "Morning" else "Evening",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedShift == "MORNING") WarmHoney else MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Switch Shift",
                                tint = if (selectedShift == "MORNING") WarmHoney else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    // Refresh / Sync Button
                    IconButton(
                        onClick = {
                            viewModel.triggerManualSync()
                            viewModel.showMessage("Data refreshed & synced ✓")
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Sync Data",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ================= 2. HERO TODAY SNAPSHOT (EXECUTIVE GRADIENT CARD) =================
        item {
            val primaryColor = MaterialTheme.colorScheme.primary
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF0F172A), primaryColor, primaryColor.copy(alpha = 0.85f))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Row with Title & Quick P&L Chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(FreshGold, CircleShape)
                            )
                            Text(
                                "TODAY'S OPERATIONS SNAPSHOT",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { onOpenProfit() }
                        ) {
                            Text(
                                "P&L Analysis →",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // 3-Column Key Metric Pillars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 1. Milk Volume
                        Column(modifier = Modifier.weight(1.1f)) {
                            val volumeLabel = if (currentMode == "COLLECTION_CENTER" || currentMode == "TRADER") "Milk Sourced" else "Milk Output"
                            val volumeVal = if (currentMode == "COLLECTION_CENTER") totalCollectedLiters else if (milkSoldLiters > 0) milkSoldLiters else totalCollectedLiters
                            Text(volumeLabel, color = Color.White.copy(alpha = 0.65f), fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${String.format(Locale.US, "%.1f", volumeVal)} L",
                                color = FreshGold,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (selectedShift == "MORNING") "🌅 Morning Shift" else "🌆 Evening Shift",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 9.5.sp
                            )
                        }

                        // 2. Today's Revenue / Cash In
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sales / Cash", color = Color.White.copy(alpha = 0.65f), fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "₹${totalSales.toInt()}",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "₹${todayCollectedPayments.toInt()} Received",
                                color = Color(0xFF4ADE80),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 3. Pending Khata Dues
                        Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
                            Text("Pending Dues", color = Color.White.copy(alpha = 0.65f), fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "₹${totalReceivable.toInt()}",
                                color = if (totalReceivable > 0) Color(0xFFFF8A80) else Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${customers.count { it.outstandingBalance > 0 }} Parties Due",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 9.5.sp
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                    // Net Margin & Expenses Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = if (netCashMovement >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (netCashMovement >= 0) Color(0xFF4ADE80) else Color(0xFFFF8A80),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Net Margin: ₹${netCashMovement.toInt()}",
                                color = if (netCashMovement >= 0) Color(0xFF4ADE80) else Color(0xFFFF8A80),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.clickable { onOpenExpenses() }
                        ) {
                            Text(
                                text = if (totalTodayExpenses > 0) "Expenses: ₹${totalTodayExpenses.toInt()} →" else "+ Log Expense",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // ================= 3. ACTIVE RATE ENGINE CHIP =================
        item {
            val biz = business
            val formulaLabel = when (biz?.pricingMode?.uppercase()) {
                "PANEER_YIELD" -> "🧀 Paneer Yield Pricing"
                "KHOA_YIELD", "KHOYA_YIELD" -> "🍬 Khoya Yield Pricing"
                "GHEE_YIELD" -> "🏺 Ghee Fat Recovery"
                "FAT_SNF" -> "🧪 Cooperative FAT + SNF Matrix"
                "FAT_ONLY" -> "🧈 Pure Fat Multiplier"
                "CUSTOM" -> "⚙️ ${biz.customFormulaName}"
                else -> "💵 Flat Rate (Cow ₹${biz?.cowMilkRate ?: 50.0} / Buff ₹${biz?.buffaloMilkRate ?: 65.0})"
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                shadowElevation = 0.5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCustomFormulaStudio = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                            color = RoyalBlueLight,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(15.dp))
                            }
                        }
                        Column {
                            Text("Active Rate Engine", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                            Text(formulaLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                        }
                    }
                    Text("Change ⚙️", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                }
            }
        }

        // ================= 4. HERD & MILKING DESK BANNER =================
        if (BusinessModeFeatures.showHerdBreeding(business)) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DairyGreenLight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GrassGreen.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().testTag("herd_milking_hub_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = CircleShape, color = GrassGreen, modifier = Modifier.size(32.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Pets, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                                    }
                                }
                                Column {
                                    Text("HERD & MILKING DESK 🐄", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = TextPrimary)
                                    Text("${scopedDashboardCattle.size} Total Cattle • $lactatingCattle In-Milk", fontSize = 10.5.sp, color = TextSecondary)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = GrassGreen,
                                modifier = Modifier.clickable { onOpenCattleBreeding() }
                            ) {
                                Text("Open Herd →", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }

                        // Quick Launch Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.weight(1f).clickable { onOpenCattleBreeding() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(13.dp))
                                    Text("Breeding & AI", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.weight(1f).clickable { onOpenMilkProductionTable() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.TableChart, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(13.dp))
                                    Text("Milk Entry", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.weight(1f).clickable { onOpenFarmMilkAnalytics() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.BarChart, contentDescription = null, tint = WarmHoney, modifier = Modifier.size(13.dp))
                                    Text("Analytics", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ================= 5. PRIMARY DAILY WORKFLOWS (2x2 TILES) =================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Daily Workflows",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val showCollection = BusinessModeFeatures.showCollectionDesk(business)
                    val showHerd = BusinessModeFeatures.showHerdBreeding(business)

                    // Action 1: Smart Sourcing Action
                    val action1 = when {
                        showHerd && !showCollection -> {
                            DashQuickAction("🐄 Herd Milking", "Record cattle yield", Icons.Default.Pets, GrassGreen, DairyGreenLight) {
                                showMilkingChoiceDialog = true
                            }
                        }
                        showCollection && !showHerd -> {
                            DashQuickAction("🥛 Intake Desk", "Procure farmer milk", Icons.Default.LocalDrink, DairyGreen, DairyGreenLight) {
                                onOpenCollectionDesk()
                            }
                        }
                        showHerd && showCollection -> {
                            DashQuickAction("🐄 Herd Milking", "Record cattle yield", Icons.Default.Pets, GrassGreen, DairyGreenLight) {
                                showMilkingChoiceDialog = true
                            }
                        }
                        else -> {
                            DashQuickAction("🥛 Daily Milking", "Record milk batch", Icons.Default.LocalDrink, DairyGreen, DairyGreenLight) {
                                if (showHerd) showMilkingChoiceDialog = true else onNavigateToTabId?.invoke("HERD") ?: onNavigateToTab(1)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = action1.bgColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, action1.tintColor.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(92.dp)
                            .clickable { action1.onClick() }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = CircleShape, color = action1.tintColor, modifier = Modifier.size(30.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(action1.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                                    }
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = action1.tintColor, modifier = Modifier.size(15.dp))
                            }
                            Column {
                                Text(action1.title, fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp, color = TextPrimary)
                                Text(action1.subtitle, fontSize = 9.5.sp, color = TextSecondary, maxLines = 1)
                            }
                        }
                    }

                    // Action 2: Delivery, Tanker or Farmer Desk
                    val (deliveryTitle, deliverySub, deliveryAction) = when (currentMode) {
                        "COLLECTION_CENTER" -> Triple("🚚 Tanker Dispatch", "${bulkDispatches.size} Dispatched", { onNavigateToDeliveries() })
                        "FARMER_WHOLESALE" -> Triple("📄 Supply Slips", "Center / Trader supply", { onNavigateToDeliveries() })
                        "TRADER" -> Triple("🚚 Deliveries", "$completedDeliveries Completed", { onNavigateToDeliveries() })
                        "INTEGRATED" -> Triple("🏢 Farmer Intake", "Village collection desk", onOpenCollectionDesk)
                        else -> Triple("🚚 Deliveries", "$completedDeliveries/$totalCustomerTarget Drops", { onNavigateToDeliveries() })
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = RoyalBlueLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(92.dp)
                            .clickable { deliveryAction() }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = CircleShape, color = RoyalBluePrimary, modifier = Modifier.size(30.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                                    }
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(15.dp))
                            }
                            Column {
                                Text(deliveryTitle, fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp, color = TextPrimary)
                                Text(deliverySub, fontSize = 9.5.sp, color = TextSecondary, maxLines = 1)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Action 3: Record Payment
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = FreshGoldLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmHoney.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(92.dp)
                            .clickable { onOpenPayments() }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = CircleShape, color = WarmHoney, modifier = Modifier.size(30.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                                    }
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = WarmHoney, modifier = Modifier.size(15.dp))
                            }
                            Column {
                                Text("💵 Record Payment", fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp, color = TextPrimary)
                                Text("Cash, UPI & Bank", fontSize = 9.5.sp, color = TextSecondary, maxLines = 1)
                            }
                        }
                    }

                    // Action 4: Khata / Parties
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFEDE7F6),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF673AB7).copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(92.dp)
                            .clickable { onNavigateToTabId?.invoke("CUSTOMERS") ?: onNavigateToTab(2) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = CircleShape, color = Color(0xFF673AB7), modifier = Modifier.size(30.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.People, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                                    }
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF673AB7), modifier = Modifier.size(15.dp))
                            }
                            Column {
                                Text("👥 Parties & Khata", fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp, color = TextPrimary)
                                Text("${customers.size} Accounts • Balances", fontSize = 9.5.sp, color = TextSecondary, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // ================= 6. REFINED OPERATIONS & TOOLS GRID =================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Operations & Management",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RoyalBlueLight,
                        modifier = Modifier.clickable { onOpenDrawer() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(13.dp))
                            Text("All Modules ☰", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                        }
                    }
                }

                // Clean 2-column Card Grid for Core Modules
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Tool 1: Feed & Stock Inventory
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        shadowElevation = 0.5.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenInventory() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = FreshGoldLight,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = WarmHoney, modifier = Modifier.size(18.dp))
                                }
                            }
                            Column {
                                Text("Feed & Stock", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                val stockLabel = if (lowStockCount > 0) "⚠️ $lowStockCount Low" else "${inventoryItems.size} Tracked"
                                Text(stockLabel, fontSize = 9.5.sp, color = if (lowStockCount > 0) DangerRed else TextSecondary, fontWeight = if (lowStockCount > 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    // Tool 2: Expenses
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        shadowElevation = 0.5.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenExpenses() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFEBEE),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                                }
                            }
                            Column {
                                Text("Expenses", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("₹${totalTodayExpenses.toInt()} Today", fontSize = 9.5.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Tool 3: Special Orders
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        shadowElevation = 0.5.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenOrders() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Assignment, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(18.dp))
                                }
                            }
                            Column {
                                Text("Orders & Slips", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("${orders.size} Advance", fontSize = 9.5.sp, color = TextSecondary)
                            }
                        }
                    }

                    // Tool 4: Financial Statements & Reports
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        shadowElevation = 0.5.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenReports() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = RoyalBlueLight,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Assessment, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                            Column {
                                Text("PDF Reports", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("P&L Statements", fontSize = 9.5.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Tool 5: Shift, Routes & Staff Control Hub
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        shadowElevation = 0.5.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenManage() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF3E5F5),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF9C27B0), modifier = Modifier.size(18.dp))
                                }
                            }
                            Column {
                                Text("Operations Hub", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Shifts & Routes", fontSize = 9.5.sp, color = TextSecondary)
                            }
                        }
                    }

                    // Tool 6: AI Dairy Advisor
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        shadowElevation = 0.5.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onOpenAIAssistant?.invoke("How can I optimize milk yield, reduce feed cost and maximize profit?")
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEDE7F6),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF673AB7), modifier = Modifier.size(18.dp))
                                }
                            }
                            Column {
                                Text("AI Advisor", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Yield & Nutrition", fontSize = 9.5.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // ================= 7. 7-DAY VOLUME & REVENUE TREND =================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
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
                        Column {
                            Text("📊 7-Day Production & Sales Trend", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("Tap any bar to view day details", fontSize = 10.sp, color = TextSecondary)
                        }
                        TextButton(onClick = onOpenReports) {
                            Text("Reports →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                        }
                    }

                    InteractiveDualBarChart(
                        dataPoints = productionDataPoints,
                        selectedIndex = selectedDayIndex,
                        onSelect = { selectedDayIndex = it },
                        supportedMilkTypes = supportedTypes
                    )
                }
            }
        }

        // ================= 7. TODAY'S RECENT ENTRIES & ACTIVITY =================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏱️ Today's Recent Transactions", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        TextButton(onClick = onOpenPayments) {
                            Text("View All →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                        }
                    }

                    val recentActivities = remember(todayCollections, milkSoldDeliveries, payments) {
                        val list = mutableListOf<String>()
                        todayCollections.take(3).forEach { c ->
                            list.add("🥛 Procured ${String.format(Locale.US, "%.1f", c.quantityLiters)}L from Farmer #${c.farmerId} • ₹${c.totalAmount.toInt()}")
                        }
                        milkSoldDeliveries.take(3).forEach { d ->
                            list.add("🚚 Delivered ${String.format(Locale.US, "%.1f", d.quantityLiters)}L to Customer #${d.customerId} • ₹${d.totalAmount.toInt()}")
                        }
                        payments.filter { it.date in todayMidnight..todayEnd }.take(2).forEach { p ->
                            list.add("💵 Payment received ₹${p.amount.toInt()} via ${p.method}")
                        }
                        list.take(4)
                    }

                    if (recentActivities.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("No shift entries recorded yet today", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                                Text("Use Quick Actions above to record milk collection or delivery", fontSize = 10.5.sp, color = TextMuted)
                            }
                        }
                    } else {
                        recentActivities.forEach { act ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = act,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        // Freely Draggable Icon-Only Quick Action Floating Button (No Text/Letters)
        var showQuickActionSheet by remember { mutableStateOf(false) }
        var fabOffsetX by remember { mutableFloatStateOf(0f) }
        var fabOffsetY by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset { IntOffset(fabOffsetX.roundToInt(), fabOffsetY.roundToInt()) }
                .padding(bottom = 20.dp, end = 20.dp)
                .pointerInput(Unit) {
                    var totalDragX = 0f
                    var totalDragY = 0f
                    detectDragGestures(
                        onDragStart = {
                            totalDragX = 0f
                            totalDragY = 0f
                        },
                        onDragEnd = {
                            if (kotlin.math.abs(totalDragX) < 10f && kotlin.math.abs(totalDragY) < 10f) {
                                showQuickActionSheet = true
                            }
                        },
                        onDragCancel = { },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y
                            fabOffsetX += dragAmount.x
                            fabOffsetY += dragAmount.y
                        }
                    )
                }
                .testTag("compact_quick_actions_fab")
        ) {
            FloatingActionButton(
                onClick = { showQuickActionSheet = true },
                containerColor = RoyalBluePrimary,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                ),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = "Quick Operational Actions",
                    tint = FreshGold,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (showQuickActionSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQuickActionSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = FreshGold,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Universal 1-Tap Quick Actions",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                "Record daily transactions from any screen",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(start = 30.dp, top = 2.dp)
                            )
                        }
                        IconButton(
                            onClick = { showQuickActionSheet = false },
                            modifier = Modifier.testTag("close_quick_action_sheet")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    val quickActionList = listOf(
                        UniversalQuickActionItem(
                            title = "Operations Control Hub",
                            subtitle = "Shifts, Dispatch, Rates & Catalog",
                            tag = "Control",
                            tagColor = RoyalBluePrimary,
                            icon = Icons.Default.Tune,
                            iconColor = RoyalBluePrimary,
                            testTag = "quick_action_control_hub",
                            onClick = {
                                showQuickActionSheet = false
                                onOpenManage()
                            }
                        ),
                        UniversalQuickActionItem(
                            title = "Record Milking",
                            subtitle = "AM/PM Cow & Buffalo yield",
                            tag = "Farm",
                            tagColor = DairyGreen,
                            icon = Icons.Default.WaterDrop,
                            iconColor = DairyGreen,
                            testTag = "quick_action_record_milking",
                            onClick = {
                                showQuickActionSheet = false
                                onOpenCollectionDesk()
                            }
                        ),
                        UniversalQuickActionItem(
                            title = "Log Delivery",
                            subtitle = "Route drop & bottle check",
                            tag = "Delivery",
                            tagColor = Color(0xFFD97706),
                            icon = Icons.Default.LocalShipping,
                            iconColor = Color(0xFFD97706),
                            testTag = "quick_action_log_delivery",
                            onClick = {
                                showQuickActionSheet = false
                                onNavigateToDeliveries()
                            }
                        ),
                        UniversalQuickActionItem(
                            title = "Record Payment",
                            subtitle = "Cash, UPI & Farmer payouts",
                            tag = "Finance",
                            tagColor = DairyGreen,
                            icon = Icons.Default.Payments,
                            iconColor = DairyGreen,
                            testTag = "quick_action_record_payment",
                            onClick = {
                                showQuickActionSheet = false
                                onOpenPayments()
                            }
                        ),
                        UniversalQuickActionItem(
                            title = "Cattle Health & AI",
                            subtitle = "Breeding, heat & vaccine",
                            tag = "Veterinary",
                            tagColor = DangerRed,
                            icon = Icons.Default.Pets,
                            iconColor = DangerRed,
                            testTag = "quick_action_cattle_health",
                            onClick = {
                                showQuickActionSheet = false
                                onOpenCattleBreeding()
                            }
                        ),
                        UniversalQuickActionItem(
                            title = "Log Expense",
                            subtitle = "Feed purchase, fuel & repair",
                            tag = "Outlay",
                            tagColor = DangerRed,
                            icon = Icons.Default.ReceiptLong,
                            iconColor = DangerRed,
                            testTag = "quick_action_log_expense",
                            onClick = {
                                showQuickActionSheet = false
                                onOpenExpenses()
                            }
                        ),
                        UniversalQuickActionItem(
                            title = "Staff & Attendance",
                            subtitle = "Worker roster & daily log",
                            tag = "Team",
                            tagColor = RoyalBluePrimary,
                            icon = Icons.Default.Badge,
                            iconColor = RoyalBluePrimary,
                            testTag = "quick_action_staff_attendance",
                            onClick = {
                                showQuickActionSheet = false
                                onNavigateToTabId?.invoke("STAFF")
                            }
                        ),
                        UniversalQuickActionItem(
                            title = "Business Mode",
                            subtitle = "Farm, Center, Trader setup",
                            tag = "Setup",
                            tagColor = RoyalBluePrimary,
                            icon = Icons.Default.Storefront,
                            iconColor = RoyalBluePrimary,
                            testTag = "quick_action_business_mode",
                            onClick = {
                                showQuickActionSheet = false
                                onOpenBusinessModeSelector()
                            }
                        )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        quickActionList.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { item ->
                                    Card(
                                        onClick = item.onClick,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag(item.testTag)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    item.icon,
                                                    contentDescription = null,
                                                    tint = item.iconColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = item.tag,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = item.tagColor
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = item.subtitle,
                                                fontSize = 10.sp,
                                                color = TextSecondary,
                                                lineHeight = 13.sp,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                                if (rowItems.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    if (showCustomFormulaStudio) {
            CustomFormulaStudioDialog(
                business = business,
                viewModel = viewModel,
                onDismiss = { showCustomFormulaStudio = false }
            )
        }

        if (showMilkingChoiceDialog) {
            val lactatingCount = cattleList.count { it.lactationStage == "LACTATING" }
            com.example.ui.screens.cattle.MilkingChoiceDialog(
                totalCattleCount = cattleList.size,
                lactatingCount = if (lactatingCount > 0) lactatingCount else cattleList.size,
                onDismiss = { showMilkingChoiceDialog = false },
                onSelectTableEntry = {
                    onOpenMilkProductionTable()
                },
                onSelectSingleEntry = {
                    onOpenCattleBreeding()
                }
            )
        }
    }

// -------------------------------------------------------------
// NATIVE COMPOSE CHART RENDERERS (Cow vs Buffalo Interactive Analytics)
// -------------------------------------------------------------

@Composable
fun InteractiveDualBarChart(
    dataPoints: List<DailyProductionPoint>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    supportedMilkTypes: String = "BOTH"
) {
    val maxSingle = remember(dataPoints) {
        val maxC = dataPoints.maxOfOrNull { it.cowLiters } ?: 50.0
        val maxB = dataPoints.maxOfOrNull { it.buffLiters } ?: 50.0
        maxOf(maxC, maxB).coerceAtLeast(10.0)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            dataPoints.forEachIndexed { index, point ->
                val isSelected = index == selectedIndex
                val cowFraction = (point.cowLiters / maxSingle).toFloat().coerceIn(0.08f, 1.0f)
                val buffFraction = (point.buffLiters / maxSingle).toFloat().coerceIn(0.08f, 1.0f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 2.dp)
                ) {
                    // Volume tooltip tag on selected
                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = RoyalBluePrimary
                        ) {
                            Text(
                                text = "${point.totalLiters.toInt()}L",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    } else {
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Dual Bars Container
                    Row(
                        modifier = Modifier.height(84.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val barWidth = if (supportedMilkTypes == "BOTH") 10.dp else 16.dp
                        // Cow Bar
                        if (supportedMilkTypes == "COW_ONLY" || supportedMilkTypes == "BOTH") {
                            Box(
                                modifier = Modifier
                                    .width(barWidth)
                                    .fillMaxHeight(cowFraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (isSelected) RoyalBlueSecondary else RoyalBlueSecondary.copy(alpha = 0.45f)
                                    )
                            )
                        }
                        // Buffalo Bar
                        if (supportedMilkTypes == "BUFFALO_ONLY" || supportedMilkTypes == "BOTH") {
                            Box(
                                modifier = Modifier
                                    .width(barWidth)
                                    .fillMaxHeight(buffFraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (isSelected) GoldenOrange else GoldenOrange.copy(alpha = 0.45f)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = point.dayLabel,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) RoyalBluePrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveStackedBarChart(
    dataPoints: List<DailyProductionPoint>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    supportedMilkTypes: String = "BOTH"
) {
    val maxTotal = remember(dataPoints) {
        (dataPoints.maxOfOrNull { it.totalLiters } ?: 100.0).coerceAtLeast(10.0)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            dataPoints.forEachIndexed { index, point ->
                val isSelected = index == selectedIndex
                val totalFraction = (point.totalLiters / maxTotal).toFloat().coerceIn(0.1f, 1.0f)
                val cowShare = if (point.totalLiters > 0) (point.cowLiters / point.totalLiters).toFloat() else 0.5f
                val buffShare = 1f - cowShare

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 2.dp)
                ) {
                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = RoyalBluePrimary
                        ) {
                            Text(
                                text = "${point.totalLiters.toInt()}L",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    } else {
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Stacked Bar
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(84.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .width(18.dp)
                                .fillMaxHeight(totalFraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        ) {
                            if (supportedMilkTypes == "BUFFALO_ONLY") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .background(if (isSelected) GoldenOrange else GoldenOrange.copy(alpha = 0.5f))
                                )
                            } else if (supportedMilkTypes == "COW_ONLY") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .background(if (isSelected) RoyalBlueSecondary else RoyalBlueSecondary.copy(alpha = 0.5f))
                                )
                            } else {
                                // Top part = Buffalo
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(buffShare.coerceAtLeast(0.01f))
                                        .background(if (isSelected) GoldenOrange else GoldenOrange.copy(alpha = 0.5f))
                                )
                                // Bottom part = Cow
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(cowShare.coerceAtLeast(0.01f))
                                        .background(if (isSelected) RoyalBlueSecondary else RoyalBlueSecondary.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = point.dayLabel,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) RoyalBluePrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveProductionDonut(
    cowLiters: Double,
    buffLiters: Double,
    morningLiters: Double = 0.0,
    eveningLiters: Double = 0.0,
    cowRate: Double,
    buffRate: Double,
    supportedMilkTypes: String = "BOTH"
) {
    val isCowOnly = supportedMilkTypes == "COW_ONLY"
    val isBuffOnly = supportedMilkTypes == "BUFFALO_ONLY"

    val (slice1Percent, slice2Percent, slice1Color, slice2Color, slice1Label, slice2Label, centerRatioText, centerSubText) = when {
        isCowOnly -> {
            val total = (morningLiters + eveningLiters).coerceAtLeast(1.0)
            val mFrac = (morningLiters / total).toFloat()
            val eFrac = (eveningLiters / total).toFloat()
            Tuple8(
                mFrac, eFrac,
                FreshGold, RoyalBlueSecondary,
                "🌅 Morning Cow: ${String.format(Locale.US, "%.1f L", morningLiters)} (${(mFrac * 100).toInt()}%)",
                "🌆 Evening Cow: ${String.format(Locale.US, "%.1f L", eveningLiters)} (${(eFrac * 100).toInt()}%)",
                "${(mFrac * 100).toInt()}% / ${(eFrac * 100).toInt()}%",
                "Morn / Eve"
            )
        }
        isBuffOnly -> {
            val total = (morningLiters + eveningLiters).coerceAtLeast(1.0)
            val mFrac = (morningLiters / total).toFloat()
            val eFrac = (eveningLiters / total).toFloat()
            Tuple8(
                mFrac, eFrac,
                FreshGold, GoldenOrange,
                "🌅 Morning Buff: ${String.format(Locale.US, "%.1f L", morningLiters)} (${(mFrac * 100).toInt()}%)",
                "🌆 Evening Buff: ${String.format(Locale.US, "%.1f L", eveningLiters)} (${(eFrac * 100).toInt()}%)",
                "${(mFrac * 100).toInt()}% / ${(eFrac * 100).toInt()}%",
                "Morn / Eve"
            )
        }
        else -> {
            val total = (cowLiters + buffLiters).coerceAtLeast(1.0)
            val cFrac = (cowLiters / total).toFloat()
            val bFrac = (buffLiters / total).toFloat()
            Tuple8(
                cFrac, bFrac,
                RoyalBlueSecondary, GoldenOrange,
                "🐄 Cow Milk: ${String.format(Locale.US, "%.1f L", cowLiters)} (${(cFrac * 100).toInt()}%)",
                "🐃 Buffalo Milk: ${String.format(Locale.US, "%.1f L", buffLiters)} (${(bFrac * 100).toInt()}%)",
                "${(cFrac * 100).toInt()}% / ${(bFrac * 100).toInt()}%",
                "Cow / Buff"
            )
        }
    }

    val sweep1 = slice1Percent * 360f
    val sweep2 = slice2Percent * 360f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular Donut
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(100.dp)) {
                drawArc(
                    color = slice1Color,
                    startAngle = -90f,
                    sweepAngle = sweep1,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Butt)
                )
                drawArc(
                    color = slice2Color,
                    startAngle = -90f + sweep1,
                    sweepAngle = sweep2,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Butt)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerRatioText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
                Text(
                    text = centerSubText,
                    fontSize = 9.sp,
                    color = TextSecondary
                )
            }
        }

        // Side Breakdown Table
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(12.dp).background(slice1Color, CircleShape))
                Text(
                    text = slice1Label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(12.dp).background(slice2Color, CircleShape))
                Text(
                    text = slice2Label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

// -------------------------------------------------------------
// DRILL-DOWN TILES & MINI COMPONENTS
// -------------------------------------------------------------

@Composable
fun DrillDownStatPill(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title.loc(), fontSize = 10.sp, color = TextSecondary, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
            Spacer(modifier = Modifier.height(1.dp))
            Text(text = subtitle.loc(), fontSize = 9.sp, color = TextSecondary, maxLines = 1)
        }
    }
}

@Composable
fun ExpenseMiniCategory(
    label: String,
    amount: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label.loc(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = amount, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarmHoney)
        }
    }
}

@Composable
fun SummaryMiniPill(
    label: String,
    value: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(badgeColor, shape = CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = label.loc(), color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun QuickActionRound(
    title: String,
    icon: ImageVector,
    badgeText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceCard,
                shadowElevation = 1.dp,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = title.loc(), tint = RoyalBluePrimary, modifier = Modifier.size(22.dp))
                }
            }
            if (badgeText != null) {
                Surface(
                    shape = CircleShape,
                    color = DairyGreen,
                    modifier = Modifier.offset(x = 4.dp, y = (-4).dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
        Text(text = title.loc(), fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, color = TextPrimary)
    }
}
