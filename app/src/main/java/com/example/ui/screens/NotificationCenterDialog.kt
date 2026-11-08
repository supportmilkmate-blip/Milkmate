package com.example.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class AlertFilter {
    ALL, HERD_HEALTH, MILKING, DELIVERIES, PAYMENTS, FEED_STOCK
}

data class OperationalAlert(
    val id: String,
    val category: AlertFilter,
    val title: String,
    val message: String,
    val timeLabel: String,
    val priority: AlertPriority,
    val icon: ImageVector,
    val iconBg: Color,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

enum class AlertPriority {
    CRITICAL, HIGH, NORMAL, INFO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterDialog(
    viewModel: MilkMateViewModel,
    onDismiss: () -> Unit,
    onNavigateToDelivery: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToFarm: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            NotificationCenterContent(
                viewModel = viewModel,
                onDismiss = onDismiss,
                onNavigateToDelivery = onNavigateToDelivery,
                onNavigateToPayments = onNavigateToPayments,
                onNavigateToExpenses = onNavigateToExpenses,
                onNavigateToFarm = onNavigateToFarm,
                onNavigateToInventory = onNavigateToInventory
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterContent(
    viewModel: MilkMateViewModel,
    onDismiss: (() -> Unit)? = null,
    onNavigateToDelivery: () -> Unit = {},
    onNavigateToPayments: () -> Unit = {},
    onNavigateToExpenses: () -> Unit = {},
    onNavigateToFarm: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {}
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val deliveries by viewModel.todayDeliveries.collectAsStateWithLifecycle()
    val inventory by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val cattleList by viewModel.cattleList.collectAsStateWithLifecycle()
    val vaccinations by viewModel.allVaccinationRecords.collectAsStateWithLifecycle()
    val dewormings by viewModel.allDewormingRecords.collectAsStateWithLifecycle()
    val breedings by viewModel.allBreedingRecords.collectAsStateWithLifecycle()
    val milkingRecords by viewModel.allMilkingRecords.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf(AlertFilter.ALL) }

    // Calculate Today's Midnight for Daily Scoping & Auto-Clearing
    val now = remember { System.currentTimeMillis() }
    val todayMidnight = remember(now) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    var dismissedAlertIdsToday by remember { mutableStateOf(setOf<String>()) }
    var lastSeenMidnight by remember { mutableLongStateOf(todayMidnight) }

    // Auto-clear dismissed alerts list when a new day starts
    if (todayMidnight > lastSeenMidnight) {
        dismissedAlertIdsToday = emptySet()
        lastSeenMidnight = todayMidnight
    }

    val dateFormat = remember { SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()) }
    val dateStr = remember(todayMidnight) { dateFormat.format(Date(now)) }

    // Dynamic Live Alerts Generation - Strict Daily Focus
    val generatedAlerts = remember(
        customers, deliveries, inventory, cattleList, vaccinations, dewormings, breedings, milkingRecords,
        now, todayMidnight
    ) {
        val list = mutableListOf<OperationalAlert>()

        // 1. Today's Milking Logs Alerts
        val todayMilking = milkingRecords.filter { it.dateEpochMidnight >= todayMidnight }
        val lactatingCattle = cattleList.filter { it.lactationStage.equals("LACTATING", true) }

        if (lactatingCattle.isNotEmpty()) {
            val morningMilked = todayMilking.filter { it.shift == "MORNING" }.map { it.cattleId }.toSet()
            val morningMissing = lactatingCattle.filter { it.id !in morningMilked }
            if (morningMissing.isNotEmpty()) {
                list.add(
                    OperationalAlert(
                        id = "MILK_MORNING_MISSING_${todayMidnight}",
                        category = AlertFilter.MILKING,
                        title = "Morning Milking Pending Today",
                        message = "${morningMissing.size} lactating cattle missing Morning Shift log for today ($dateStr).",
                        timeLabel = "Today's Morning Shift",
                        priority = AlertPriority.HIGH,
                        icon = Icons.Default.Pets,
                        iconBg = RoyalBluePrimary,
                        actionLabel = "Record Milking",
                        onAction = {
                            onDismiss?.invoke()
                            onNavigateToFarm()
                        }
                    )
                )
            }

            val eveningMilked = todayMilking.filter { it.shift == "EVENING" }.map { it.cattleId }.toSet()
            val eveningMissing = lactatingCattle.filter { it.id !in eveningMilked }
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (currentHour >= 12 && eveningMissing.isNotEmpty()) {
                list.add(
                    OperationalAlert(
                        id = "MILK_EVENING_MISSING_${todayMidnight}",
                        category = AlertFilter.MILKING,
                        title = "Evening Milking Pending Today",
                        message = "${eveningMissing.size} lactating cattle missing Evening Shift log for today ($dateStr).",
                        timeLabel = "Today's Evening Shift",
                        priority = AlertPriority.HIGH,
                        icon = Icons.Default.Pets,
                        iconBg = RoyalBluePrimary,
                        actionLabel = "Record Milking",
                        onAction = {
                            onDismiss?.invoke()
                            onNavigateToFarm()
                        }
                    )
                )
            }
        }

        // 2. Today's Deliveries & Dispatches Alerts
        val todayPendingDeliveries = deliveries.filter { !it.isDelivered }
        if (todayPendingDeliveries.isNotEmpty()) {
            list.add(
                OperationalAlert(
                    id = "DELIVERY_TODAY_PENDING_${todayMidnight}",
                    category = AlertFilter.DELIVERIES,
                    title = "Today's Deliveries Pending",
                    message = "${todayPendingDeliveries.size} customer milk bottles remaining for today's live routes.",
                    timeLabel = "Today's Live Routes",
                    priority = AlertPriority.HIGH,
                    icon = Icons.Default.LocalShipping,
                    iconBg = FreshGold,
                    actionLabel = "Open Route",
                    onAction = {
                        onDismiss?.invoke()
                        onNavigateToDelivery()
                    }
                )
            )
        }

        // 3. Customer Payment Overdue Reminders
        val overdueCustomers = customers.filter { it.outstandingBalance > 500.0 }
            .sortedByDescending { it.outstandingBalance }
        overdueCustomers.take(4).forEach { cust ->
            list.add(
                OperationalAlert(
                    id = "PAY_DUE_${cust.id}_${todayMidnight}",
                    category = AlertFilter.PAYMENTS,
                    title = "Bill Overdue Today: ${cust.name}",
                    message = "Outstanding balance is ₹${cust.outstandingBalance.toInt()} (Route: ${cust.route.ifBlank { "Main" }}).",
                    timeLabel = "Due ₹${cust.outstandingBalance.toInt()}",
                    priority = if (cust.outstandingBalance > 1000.0) AlertPriority.CRITICAL else AlertPriority.HIGH,
                    icon = Icons.Default.AccountBalanceWallet,
                    iconBg = DangerRed,
                    actionLabel = "WhatsApp Bill 💬",
                    onAction = {
                        sendWhatsAppDueReminder(context, cust.name, cust.mobile, cust.outstandingBalance)
                    }
                )
            )
        }

        // 4. Herd Health - Due Today or Overdue
        vaccinations.forEach { vac ->
            val daysDiff = (vac.nextDueDate - now) / (1000L * 60L * 60L * 24L)
            if (vac.nextDueDate > 0 && daysDiff in -7..1) {
                val isOverdue = daysDiff < 0
                list.add(
                    OperationalAlert(
                        id = "VAC_${vac.id}_${todayMidnight}",
                        category = AlertFilter.HERD_HEALTH,
                        title = if (isOverdue) "🚨 Overdue Vaccine Today: ${vac.vaccineName}" else "💉 Vaccine Due Today: ${vac.vaccineName}",
                        message = "Cattle Tag #${vac.cattleTag} due for ${vac.vaccineName} booster dose.",
                        timeLabel = if (isOverdue) "Overdue" else "Due Today",
                        priority = if (isOverdue) AlertPriority.CRITICAL else AlertPriority.HIGH,
                        icon = Icons.Default.MedicalServices,
                        iconBg = if (isOverdue) DangerRed else RoyalBluePrimary,
                        actionLabel = "View Health",
                        onAction = {
                            onDismiss?.invoke()
                            onNavigateToFarm()
                        }
                    )
                )
            }
        }

        dewormings.forEach { dw ->
            val daysDiff = (dw.nextDueDate - now) / (1000L * 60L * 60L * 24L)
            if (dw.nextDueDate > 0 && daysDiff in -7..1) {
                val isOverdue = daysDiff < 0
                list.add(
                    OperationalAlert(
                        id = "DEW_${dw.id}_${todayMidnight}",
                        category = AlertFilter.HERD_HEALTH,
                        title = if (isOverdue) "💊 Deworming Overdue Today" else "💊 Deworming Due Today",
                        message = "Cattle Tag #${dw.cattleTag} scheduled for dose (${dw.dewormerSalt}).",
                        timeLabel = if (isOverdue) "Overdue" else "Due Today",
                        priority = if (isOverdue) AlertPriority.HIGH else AlertPriority.NORMAL,
                        icon = Icons.Default.Vaccines,
                        iconBg = GrassGreen,
                        actionLabel = "Administer Dose",
                        onAction = {
                            onDismiss?.invoke()
                            onNavigateToFarm()
                        }
                    )
                )
            }
        }

        // 5. Breeding & Calving Radar Today
        cattleList.forEach { c ->
            if (c.expectedCalvingDate > 0) {
                val daysToCalve = (c.expectedCalvingDate - now) / (1000L * 60L * 60L * 24L)
                if (daysToCalve in -2..2) {
                    list.add(
                        OperationalAlert(
                            id = "CALV_${c.id}_${todayMidnight}",
                            category = AlertFilter.HERD_HEALTH,
                            title = "🍼 Expected Calving Alert Today!",
                            message = "${c.name.ifBlank { "Animal" }} (#${c.tagNumber}) expected calving today. Prepare maternity stall.",
                            timeLabel = "Maternity Today",
                            priority = AlertPriority.CRITICAL,
                            icon = Icons.Default.ChildCare,
                            iconBg = WarmHoney,
                            actionLabel = "Calving Logs",
                            onAction = {
                                onDismiss?.invoke()
                                onNavigateToFarm()
                            }
                        )
                    )
                }
            }
        }

        // 6. Low Feed Stock Warning
        val lowStockItems = inventory.filter { it.stockAlertStatus != "NORMAL" || it.currentStock <= 5.0 }
        if (lowStockItems.isNotEmpty()) {
            list.add(
                OperationalAlert(
                    id = "INV_LOW_FEED_${todayMidnight}",
                    category = AlertFilter.FEED_STOCK,
                    title = "🌾 Low Stock Notice Today",
                    message = "${lowStockItems.size} feed items (${lowStockItems.take(2).joinToString { it.itemName }}) low in inventory today.",
                    timeLabel = "Low Stock Today",
                    priority = AlertPriority.HIGH,
                    icon = Icons.Default.Inventory2,
                    iconBg = GoldenOrange,
                    actionLabel = "Order / Restock",
                    onAction = {
                        onDismiss?.invoke()
                        onNavigateToInventory()
                    }
                )
            )
        }

        // 7. End-of-Day Closing Alert
        list.add(
            OperationalAlert(
                id = "EOD_CLOSING_${todayMidnight}",
                category = AlertFilter.ALL,
                title = "📊 End-of-Day Ledger & Expense Closing",
                message = "Record today's ($dateStr) feed, wages, and vendor cash expenses before closing dairy books.",
                timeLabel = "Today's Closing",
                priority = AlertPriority.INFO,
                icon = Icons.Default.ReceiptLong,
                iconBg = ForestGreenAccent,
                actionLabel = "Open Expense Ledger",
                onAction = {
                    onDismiss?.invoke()
                    onNavigateToExpenses()
                }
            )
        )

        list
    }

    // Filter out dismissed alerts for today
    val todayActiveAlerts = remember(generatedAlerts, dismissedAlertIdsToday) {
        generatedAlerts.filter { it.id !in dismissedAlertIdsToday }
    }

    val filteredAlerts = remember(todayActiveAlerts, selectedFilter) {
        if (selectedFilter == AlertFilter.ALL) todayActiveAlerts
        else todayActiveAlerts.filter { it.category == selectedFilter }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Banner
        Surface(
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Today's Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$dateStr • Auto-clears each new day",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                if (onDismiss != null) {
                    IconButton(onClick = { onDismiss.invoke() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }

        // Sub-Header Status Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${todayActiveAlerts.size} active for today",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (todayActiveAlerts.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            dismissedAlertIdsToday = generatedAlerts.map { it.id }.toSet()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All Today", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Category Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(AlertFilter.entries) { filter ->
                val label = when (filter) {
                    AlertFilter.ALL -> "All (${todayActiveAlerts.size})"
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

        // Main Notifications Content
        if (filteredAlerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = DairyGreen.copy(alpha = 0.15f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(36.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (todayActiveAlerts.isEmpty()) "All Notifications Clear for Today! 🎉" else "No Pending Alerts in Category",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (todayActiveAlerts.isEmpty())
                            "Today's operational items are clear or dismissed.\nNotifications will automatically reset when tomorrow starts."
                        else "Switch filter category above to view other active notifications.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredAlerts, key = { it.id }) { alert ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = if (alert.priority == AlertPriority.CRITICAL) BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)) else null
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = alert.iconBg,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(alert.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Column {
                                        Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                        Text(alert.timeLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        dismissedAlertIdsToday = dismissedAlertIdsToday + alert.id
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }

                            Text(
                                text = alert.message,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )

                            if (alert.actionLabel != null && alert.onAction != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { alert.onAction.invoke() },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = alert.iconBg)
                                    ) {
                                        Text(alert.actionLabel, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (onDismiss != null) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { onDismiss.invoke() }) {
                        Text("Close Notifications")
                    }
                }
            }
        }
    }
}

private fun sendWhatsAppDueReminder(context: Context, name: String, mobile: String, balance: Double) {
    try {
        val cleanMobile = mobile.filter { it.isDigit() }
        val message = "Namaste $name ji, gentle reminder from dairy for your outstanding milk bill of ₹${balance.toInt()}. Kindly arrange payment via UPI or cash. Thank you!"
        val url = "https://api.whatsapp.com/send?phone=+91$cleanMobile&text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Payment Reminder for $name: Pending amount is ₹${balance.toInt()}.")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Payment Reminder"))
    }
}
