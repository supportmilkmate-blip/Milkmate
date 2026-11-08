package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.CustomerLeaveEntity
import com.example.data.local.entity.OrderEntity
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class PackingItemRequirement(
    val id: String,
    val name: String,
    val category: String, // "COW_MILK", "BUFFALO_MILK", "BYPRODUCTS"
    val unit: String,     // "Pouches (1L)", "Pouches (500ml)", "Liters (Can)", "Kg", "Units"
    val requiredQty: Double,
    val isCountUnit: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackingConfirmationScreen(
    viewModel: MilkMateViewModel,
    onBack: () -> Unit,
    onStartDelivery: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedShift by viewModel.selectedShift.collectAsStateWithLifecycle()

    val allCustomers by viewModel.customers.collectAsStateWithLifecycle()
    val allLeaves by viewModel.customerLeaves.collectAsStateWithLifecycle()
    val allOrders by viewModel.orders.collectAsStateWithLifecycle()

    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    val shortDateDisplay = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(selectedDate))

    var showAddLeaveDialog by remember { mutableStateOf(false) }
    var showLeavesListExpanded by remember { mutableStateOf(true) }
    var showOrdersListExpanded by remember { mutableStateOf(true) }
    var showShareSuccessDialog by remember { mutableStateOf(false) }

    // Normalize selectedDate to midnight
    val targetMidnight = remember(selectedDate) {
        Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // 1. Filter active customer leaves for this date & shift
    val activeLeavesToday = remember(allLeaves, targetMidnight, selectedShift) {
        allLeaves.filter { leave ->
            leave.status == "ACTIVE" &&
                    targetMidnight >= leave.startDate &&
                    targetMidnight <= leave.endDate &&
                    (leave.shift == "BOTH" || leave.shift.equals(selectedShift, ignoreCase = true))
        }
    }
    val leaveCustomerIds = remember(activeLeavesToday) {
        activeLeavesToday.map { it.customerId }.toSet()
    }

    // 2. Customers scheduled for this shift
    val scheduledCustomers = remember(allCustomers, selectedShift) {
        allCustomers.filter { cust ->
            cust.status == "ACTIVE" &&
                    (cust.defaultShift == "BOTH" || cust.defaultShift.equals(selectedShift, ignoreCase = true))
        }
    }

    // 3. Customers who are receiving milk today (scheduled minus leave)
    val deliveringCustomers = remember(scheduledCustomers, leaveCustomerIds) {
        scheduledCustomers.filter { !leaveCustomerIds.contains(it.id) }
    }
    val leaveCustomers = remember(scheduledCustomers, leaveCustomerIds) {
        scheduledCustomers.filter { leaveCustomerIds.contains(it.id) }
    }

    // Calculate regular baseline demands
    val baseCowLiters = remember(scheduledCustomers, selectedShift) {
        scheduledCustomers.filter { it.milkType == "COW" || it.milkType == "BOTH" }.sumOf { cust ->
            if (cust.defaultShift == "BOTH") cust.cowQuantity / 2.0 else cust.cowQuantity
        }
    }
    val baseBuffaloLiters = remember(scheduledCustomers, selectedShift) {
        scheduledCustomers.filter { it.milkType == "BUFFALO" || it.milkType == "BOTH" }.sumOf { cust ->
            if (cust.defaultShift == "BOTH") cust.buffaloQuantity / 2.0 else cust.buffaloQuantity
        }
    }
    val baseTotalLiters = baseCowLiters + baseBuffaloLiters

    // Calculate customer leave deductions
    val leaveCowLiters = remember(leaveCustomers, selectedShift) {
        leaveCustomers.filter { it.milkType == "COW" || it.milkType == "BOTH" }.sumOf { cust ->
            if (cust.defaultShift == "BOTH") cust.cowQuantity / 2.0 else cust.cowQuantity
        }
    }
    val leaveBuffaloLiters = remember(leaveCustomers, selectedShift) {
        leaveCustomers.filter { it.milkType == "BUFFALO" || it.milkType == "BOTH" }.sumOf { cust ->
            if (cust.defaultShift == "BOTH") cust.buffaloQuantity / 2.0 else cust.buffaloQuantity
        }
    }
    val leaveTotalLiters = leaveCowLiters + leaveBuffaloLiters

    // 4. Advance Orders for today
    val todayOrders = remember(allOrders, targetMidnight) {
        allOrders.filter { order ->
            order.status in listOf("PENDING", "CONFIRMED") &&
                    order.deliveryDate in (targetMidnight..(targetMidnight + 86400000L - 1L))
        }
    }

    val orderCowLiters = remember(todayOrders) {
        todayOrders.filter { it.productName.contains("Cow", ignoreCase = true) && it.unit == "L" }
            .sumOf { it.quantity }
    }
    val orderBuffaloLiters = remember(todayOrders) {
        todayOrders.filter { it.productName.contains("Buff", ignoreCase = true) && it.unit == "L" }
            .sumOf { it.quantity }
    }
    val orderGeneralMilkLiters = remember(todayOrders) {
        todayOrders.filter {
            (it.productName.equals("Milk", ignoreCase = true) || it.productName.contains("Fresh Milk", ignoreCase = true)) &&
                    !it.productName.contains("Cow", ignoreCase = true) && !it.productName.contains("Buff", ignoreCase = true)
        }.sumOf { it.quantity }
    }
    val orderTotalMilkLiters = orderCowLiters + orderBuffaloLiters + orderGeneralMilkLiters

    // Net Milk Required to Pack
    val netCowLiters = (baseCowLiters - leaveCowLiters + orderCowLiters).coerceAtLeast(0.0)
    val netBuffaloLiters = (baseBuffaloLiters - leaveBuffaloLiters + orderBuffaloLiters + orderGeneralMilkLiters).coerceAtLeast(0.0)
    val netTotalMilkLiters = netCowLiters + netBuffaloLiters

    // Packaging item breakdown:
    // Estimate 1L and 500ml pouch counts from customers delivering today
    val cow1LPouches = remember(deliveringCustomers, selectedShift) {
        deliveringCustomers.filter { it.milkType == "COW" || it.milkType == "BOTH" }
            .map { if (it.defaultShift == "BOTH") it.cowQuantity / 2.0 else it.cowQuantity }
            .filter { it % 1.0 == 0.0 && it > 0.0 }
            .sum().toInt()
    }
    val cow500mlPouches = remember(deliveringCustomers, selectedShift) {
        deliveringCustomers.filter { it.milkType == "COW" || it.milkType == "BOTH" }
            .map { if (it.defaultShift == "BOTH") it.cowQuantity / 2.0 else it.cowQuantity }
            .filter { (it % 1.0) == 0.5 }
            .size
    }
    val cowLooseLiters = (netCowLiters - cow1LPouches - (cow500mlPouches * 0.5)).coerceAtLeast(0.0)

    val buff1LPouches = remember(deliveringCustomers, selectedShift) {
        deliveringCustomers.filter { it.milkType == "BUFFALO" || it.milkType == "BOTH" }
            .map { if (it.defaultShift == "BOTH") it.buffaloQuantity / 2.0 else it.buffaloQuantity }
            .filter { it % 1.0 == 0.0 && it > 0.0 }
            .sum().toInt()
    }
    val buff500mlPouches = remember(deliveringCustomers, selectedShift) {
        deliveringCustomers.filter { it.milkType == "BUFFALO" || it.milkType == "BOTH" }
            .map { if (it.defaultShift == "BOTH") it.buffaloQuantity / 2.0 else it.buffaloQuantity }
            .filter { (it % 1.0) == 0.5 }
            .size
    }
    val buffLooseLiters = (netBuffaloLiters - buff1LPouches - (buff500mlPouches * 0.5)).coerceAtLeast(0.0)

    // Build the packing checklist items
    val packingItems = remember(
        netCowLiters, netBuffaloLiters, cow1LPouches, cow500mlPouches, cowLooseLiters,
        buff1LPouches, buff500mlPouches, buffLooseLiters, todayOrders
    ) {
        val list = mutableListOf<PackingItemRequirement>()

        if (netCowLiters > 0) {
            if (cow1LPouches > 0) {
                list.add(PackingItemRequirement("cow_1l", "Cow Milk (1 Litre Pouch / Bottle)", "COW_MILK", "Pouches", cow1LPouches.toDouble(), true))
            }
            if (cow500mlPouches > 0) {
                list.add(PackingItemRequirement("cow_500ml", "Cow Milk (500 mL Pouch / Bottle)", "COW_MILK", "Pouches", cow500mlPouches.toDouble(), true))
            }
            if (cowLooseLiters > 0) {
                list.add(PackingItemRequirement("cow_can", "Cow Milk (Can / Loose Dispense)", "COW_MILK", "Liters", cowLooseLiters, false))
            }
        }

        if (netBuffaloLiters > 0) {
            if (buff1LPouches > 0) {
                list.add(PackingItemRequirement("buff_1l", "Buffalo Milk (1 Litre Pouch / Bottle)", "BUFFALO_MILK", "Pouches", buff1LPouches.toDouble(), true))
            }
            if (buff500mlPouches > 0) {
                list.add(PackingItemRequirement("buff_500ml", "Buffalo Milk (500 mL Pouch / Bottle)", "BUFFALO_MILK", "Pouches", buff500mlPouches.toDouble(), true))
            }
            if (buffLooseLiters > 0) {
                list.add(PackingItemRequirement("buff_can", "Buffalo Milk (Can / Loose Dispense)", "BUFFALO_MILK", "Liters", buffLooseLiters, false))
            }
        }

        // Byproduct advance orders (Paneer, Curd, Ghee, etc.)
        todayOrders.filter { !it.productName.contains("Milk", ignoreCase = true) || it.unit != "L" }
            .groupBy { "${it.productName}__${it.unit}" }
            .forEach { (key, group) ->
                val prodName = group.first().productName
                val unit = group.first().unit
                val totalQty = group.sumOf { it.quantity }
                list.add(PackingItemRequirement("prod_$key", "$prodName (Orders)", "BYPRODUCTS", unit, totalQty, unit in listOf("Units", "Packets", "Jars")))
            }

        list
    }

    // State for user's actual packed counts
    val packedCounts = remember(packingItems) {
        mutableStateMapOf<String, Double>().apply {
            packingItems.forEach { item ->
                put(item.id, item.requiredQty) // Default: initialized to match required
            }
        }
    }

    // Verification status check
    val allMatched = remember(packingItems, packedCounts) {
        packingItems.all { item ->
            val packed = packedCounts[item.id] ?: 0.0
            kotlin.math.abs(packed - item.requiredQty) < 0.01
        }
    }

    val totalPackedMilk = remember(packingItems, packedCounts) {
        var total = 0.0
        packingItems.forEach { item ->
            val packed = packedCounts[item.id] ?: 0.0
            if (item.id == "cow_1l" || item.id == "buff_1l") total += packed * 1.0
            else if (item.id == "cow_500ml" || item.id == "buff_500ml") total += packed * 0.5
            else if (item.id == "cow_can" || item.id == "buff_can") total += packed
        }
        total
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📦 Packing Confirmation",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selectedShift == "MORNING") FreshGold else Color(0xFF6366F1)
                            ) {
                                Text(
                                    text = if (selectedShift == "MORNING") "☀️ Morning" else "🌙 Evening",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selectedShift == "MORNING") Color.Black else Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Pre-delivery packing verification taking customer leaves into count",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val bizName = business?.businessName?.ifBlank { "MilkMate Dairy" } ?: "MilkMate Dairy"
                            val slip = buildString {
                                append("📦 *${bizName} - PRE-DELIVERY PACKING SLIP*\n")
                                append("📅 Date: $shortDateDisplay (${selectedShift} Shift)\n")
                                append("────────────────────────\n")
                                append("🥛 *TOTAL MILK REQUIRED:* ${String.format(Locale.US, "%.1f", netTotalMilkLiters)} Liters\n")
                                if (netCowLiters > 0) append("  • 🐄 Cow Milk: ${String.format(Locale.US, "%.1f", netCowLiters)} L\n")
                                if (netBuffaloLiters > 0) append("  • 🐃 Buffalo Milk: ${String.format(Locale.US, "%.1f", netBuffaloLiters)} L\n")
                                append("\n📦 *PACKAGING COUNT:*\n")
                                packingItems.forEach { item ->
                                    val countStr = if (item.isCountUnit) "${item.requiredQty.toInt()} ${item.unit}" else "${String.format(Locale.US, "%.1f", item.requiredQty)} ${item.unit}"
                                    append("  [ ] ${item.name}: $countStr\n")
                                }

                                if (activeLeavesToday.isNotEmpty()) {
                                    append("\n🛑 *DO NOT PACK (ON LEAVE TODAY - ${activeLeavesToday.size} CUSTOMERS):*\n")
                                    activeLeavesToday.forEach { l ->
                                        append("  ❌ ${l.customerName} (-${String.format(Locale.US, "%.1f", l.dailyQuantitySaved)}L) - ${l.reason}\n")
                                    }
                                }

                                if (todayOrders.isNotEmpty()) {
                                    append("\n⭐ *PACK EXTRA FOR ADVANCE ORDERS (${todayOrders.size}):*\n")
                                    todayOrders.forEach { o ->
                                        append("  ➕ ${o.customerName}: ${o.quantity} ${o.unit} ${o.productName}\n")
                                    }
                                }

                                append("\n🚚 *Delivering Customers:* ${deliveringCustomers.size} stops\n")
                                append("✅ Verified & Packed for Route Loading")
                            }

                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, slip)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Packing Slip"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Packing Slip", tint = RoyalBluePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Packing Verification Status",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (allMatched) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (allMatched) GrassGreen else FreshGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (allMatched) "100% Matched & Ready" else "Quantity Discrepancy (${String.format(Locale.US, "%.1f", totalPackedMilk)}L packed)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (allMatched) GrassGreen else Color(0xFFD97706)
                                )
                            }
                        }

                        Button(
                            onClick = onStartDelivery,
                            colors = ButtonDefaults.buttonColors(containerColor = if (allMatched) GrassGreen else RoyalBluePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_confirm_packing_start_delivery")
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Start Delivery Route",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ================= 1. SHIFT & DATE CONTROLS =================
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date picker button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val c = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }
                                    viewModel.setDate(c.timeInMillis)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = dateFormat.format(Date(selectedDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Shift Switcher
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(10.dp))
                            .padding(2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedShift == "MORNING") FreshGold else Color.Transparent,
                            modifier = Modifier.clickable { viewModel.setShift("MORNING") }
                        ) {
                            Text(
                                text = "☀️ Morning",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedShift == "MORNING") Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedShift == "EVENING") Color(0xFF6366F1) else Color.Transparent,
                            modifier = Modifier.clickable { viewModel.setShift("EVENING") }
                        ) {
                            Text(
                                text = "🌙 Evening",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedShift == "EVENING") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // ================= 2. HERO RECONCILIATION SUMMARY CARD =================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = OceanMidnight),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL PACKING REQUIREMENT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.7f),
                                letterSpacing = 0.8.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = FreshGold
                            ) {
                                Text(
                                    text = "${deliveringCustomers.size} Customers Delivering",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Big Total Net Milk Display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = String.format(Locale.US, "%.1f Liters", netTotalMilkLiters),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = FreshGold
                                )
                                Text(
                                    text = "🐄 ${String.format(Locale.US, "%.1f", netCowLiters)}L Cow  •  🐃 ${String.format(Locale.US, "%.1f", netBuffaloLiters)}L Buffalo",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }

                            // Math Formula Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.12f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.End) {
                                    Text("Base: ${String.format(Locale.US, "%.1f", baseTotalLiters)}L", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                    Text("Leaves: -${String.format(Locale.US, "%.1f", leaveTotalLiters)}L", fontSize = 10.sp, color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold)
                                    if (orderTotalMilkLiters > 0) {
                                        Text("Orders: +${String.format(Locale.US, "%.1f", orderTotalMilkLiters)}L", fontSize = 10.sp, color = Color(0xFF86EFAC), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.15f))

                        // Explanatory breakdown row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Regular Subscribers", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                                Text("${scheduledCustomers.size} Households", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("Customer Leaves", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                                Text("${activeLeavesToday.size} Informed Off", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFCA5A5))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Extra Advance Orders", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                                Text("${todayOrders.size} Bookings", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF86EFAC))
                            }
                        }
                    }
                }
            }

            // ================= 3. CUSTOMER LEAVES CARD (TAKING LEAVE INTO COUNT) =================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (activeLeavesToday.isNotEmpty()) Color(0xFFFFFBEB) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, if (activeLeavesToday.isNotEmpty()) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFEF3C7),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.PauseCircleFilled,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Customer Leaves (${activeLeavesToday.size} informed)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = if (activeLeavesToday.isNotEmpty()) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (activeLeavesToday.isNotEmpty())
                                            "Milk deducted: ${String.format(Locale.US, "%.1f", leaveTotalLiters)}L not packed today"
                                        else "No customers on leave for this shift",
                                        fontSize = 11.sp,
                                        color = if (activeLeavesToday.isNotEmpty()) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { showAddLeaveDialog = true },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFFFEF3C7), shape = CircleShape)
                                        .testTag("btn_add_customer_leave_packing")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Leave", tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                }

                                if (activeLeavesToday.isNotEmpty()) {
                                    IconButton(
                                        onClick = { showLeavesListExpanded = !showLeavesListExpanded },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            if (showLeavesListExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = "Expand",
                                            tint = Color(0xFF92400E)
                                        )
                                    }
                                }
                            }
                        }

                        // Detailed list of customers on leave
                        if (activeLeavesToday.isNotEmpty() && showLeavesListExpanded) {
                            Divider(color = Color(0xFFFDE68A))

                            activeLeavesToday.forEach { leave ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = leave.customerName,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF92400E)
                                            )
                                            Text(
                                                text = "🛑 Reason: ${leave.reason} (${leave.shift})",
                                                fontSize = 10.5.sp,
                                                color = Color(0xFFB45309)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFEE2E2)
                                            ) {
                                                Text(
                                                    text = "-${String.format(Locale.US, "%.1f", leave.dailyQuantitySaved)}L",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFDC2626),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))
                                            TextButton(
                                                onClick = { viewModel.cancelCustomerLeave(leave.id) },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("Resume", fontSize = 11.sp, color = RoyalBluePrimary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= 4. ADVANCE ORDERS INCLUDED (TAKING ORDERS INTO COUNT) =================
            if (todayOrders.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFDCFCE7),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.ShoppingCart,
                                                contentDescription = null,
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Advance Orders Today (${todayOrders.size} orders)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = Color(0xFF166534)
                                        )
                                        Text(
                                            text = "Extra items included in packing checklist",
                                            fontSize = 11.sp,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { showOrdersListExpanded = !showOrdersListExpanded },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        if (showOrdersListExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Expand",
                                        tint = Color(0xFF166534)
                                    )
                                }
                            }

                            if (showOrdersListExpanded) {
                                Divider(color = Color(0xFFBBF7D0))

                                todayOrders.forEach { order ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(order.customerName, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                                Text(
                                                    text = "Item: ${order.productName} • ₹${order.totalAmount.toInt()}",
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFDCFCE7)
                                            ) {
                                                Text(
                                                    text = "+${order.quantity} ${order.unit}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF166534),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

            // ================= 5. INTERACTIVE PACKING CHECKLIST & VERIFICATION =================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Packing Checklist & Recheck",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Verify packed crates/pouches match total requirement",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = {
                            // Quick 1-click match: set all packed to required
                            packingItems.forEach { item ->
                                packedCounts[item.id] = item.requiredQty
                            }
                        }
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Match All", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(packingItems) { item ->
                val packed = packedCounts[item.id] ?: 0.0
                val isMatched = kotlin.math.abs(packed - item.requiredQty) < 0.01

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMatched) MaterialTheme.colorScheme.surface else Color(0xFFFEF2F2)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isMatched) MaterialTheme.colorScheme.outlineVariant else Color(0xFFFCA5A5)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val icon = when (item.category) {
                                    "COW_MILK" -> "🐄"
                                    "BUFFALO_MILK" -> "🐃"
                                    else -> "🧀"
                                }
                                Text(icon, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Required: " + (if (item.isCountUnit) "${item.requiredQty.toInt()} ${item.unit}" else "${String.format(Locale.US, "%.1f", item.requiredQty)} ${item.unit}"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Counter: [-] [Count] [+]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable {
                                        val cur = packedCounts[item.id] ?: 0.0
                                        val step = if (item.isCountUnit) 1.0 else 0.5
                                        packedCounts[item.id] = (cur - step).coerceAtLeast(0.0)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Remove, contentDescription = "Minus", modifier = Modifier.size(16.dp))
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isMatched) GrassGreen.copy(alpha = 0.12f) else Color(0xFFFEE2E2),
                                border = BorderStroke(1.dp, if (isMatched) GrassGreen else Color(0xFFEF4444)),
                                modifier = Modifier.widthIn(min = 52.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (item.isCountUnit) "${packed.toInt()}" else String.format(Locale.US, "%.1f", packed),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = if (isMatched) GrassGreen else Color(0xFFDC2626)
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable {
                                        val cur = packedCounts[item.id] ?: 0.0
                                        val step = if (item.isCountUnit) 1.0 else 0.5
                                        packedCounts[item.id] = cur + step
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(16.dp))
                                }
                            }

                            // Matched icon
                            Icon(
                                if (isMatched) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isMatched) GrassGreen else Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    if (showAddLeaveDialog) {
        CustomerLeaveDialog(
            viewModel = viewModel,
            onDismiss = { showAddLeaveDialog = false },
            onGoToPackingConfirmation = { showAddLeaveDialog = false }
        )
    }
}
