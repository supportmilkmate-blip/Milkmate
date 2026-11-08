package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.OrderEntity
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(viewModel: MilkMateViewModel) {
    val context = LocalContext.current
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val supportedMilkTypes = business?.supportedMilkTypes ?: "BOTH"

    var showAddDialog by remember { mutableStateOf(false) }
    var editingOrder by remember { mutableStateOf<OrderEntity?>(null) }
    var selectedFilter by remember { mutableStateOf("All") } // All, Pending, Today, Delivered, Cancelled

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val todayMidnight = MilkMateViewModel.getTodayMidnightMillis()
    val tomorrowMidnight = remember(todayMidnight) { todayMidnight + 86400000L }

    val pendingCount = orders.count { it.status == "PENDING" }
    val dueTodayCount = orders.count {
        it.status == "PENDING" && MilkMateViewModel.normalizeToMidnight(it.deliveryDate) == todayMidnight
    }
    val dueTomorrowCount = orders.count {
        it.status == "PENDING" && MilkMateViewModel.normalizeToMidnight(it.deliveryDate) == tomorrowMidnight
    }
    val totalThisMonth = orders.size
    val totalOrderValue = orders.sumOf { it.totalAmount }

    // Production Requirement Forecast for Tomorrow's Advance Orders
    val tomorrowOrders = remember(orders, tomorrowMidnight) {
        orders.filter { it.status != "CANCELLED" && MilkMateViewModel.normalizeToMidnight(it.deliveryDate) == tomorrowMidnight }
    }
    val tomorrowRawMilk = remember(tomorrowOrders) {
        tomorrowOrders.filter { it.productName.contains("Milk", ignoreCase = true) }.sumOf { it.quantity }
    }
    val tomorrowPaneer = remember(tomorrowOrders) {
        tomorrowOrders.filter { it.productName.contains("Paneer", ignoreCase = true) }.sumOf { it.quantity }
    }
    val tomorrowCurd = remember(tomorrowOrders) {
        tomorrowOrders.filter { it.productName.contains("Curd", ignoreCase = true) || it.productName.contains("Dahi", ignoreCase = true) }.sumOf { it.quantity }
    }
    val tomorrowKhoya = remember(tomorrowOrders) {
        tomorrowOrders.filter { it.productName.contains("Khoa", ignoreCase = true) || it.productName.contains("Khoya", ignoreCase = true) }.sumOf { it.quantity }
    }
    val totalMilkNeededTomorrow = remember(tomorrowRawMilk, tomorrowPaneer, tomorrowCurd, tomorrowKhoya) {
        tomorrowRawMilk + (tomorrowPaneer * 5.0) + (tomorrowCurd * 1.0) + (tomorrowKhoya * 5.0)
    }

    val filteredOrders = when (selectedFilter) {
        "Pending" -> orders.filter { it.status == "PENDING" }
        "Due Tomorrow" -> orders.filter { MilkMateViewModel.normalizeToMidnight(it.deliveryDate) == tomorrowMidnight }
        "Today" -> orders.filter { MilkMateViewModel.normalizeToMidnight(it.deliveryDate) == todayMidnight }
        "Delivered" -> orders.filter { it.status == "DELIVERED" }
        "Cancelled" -> orders.filter { it.status == "CANCELLED" }
        else -> orders
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header stats
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ADVANCE ORDERS & PRE-BOOKINGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBluePrimary
                    )
                    Text(
                        text = "Hotel B2B & Retail Booking Desk",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OrderStatBox("Pending", "$pendingCount", Modifier.weight(1f))
                        OrderStatBox("Tomorrow", "$dueTomorrowCount", Modifier.weight(1f))
                        OrderStatBox("Due Today", "$dueTodayCount", Modifier.weight(1f))
                        OrderStatBox("Value", "₹${totalOrderValue.toInt()}", Modifier.weight(1.2f))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            editingOrder = null
                            showAddDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("add_order_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Add Advance Order (Hotel / Retail)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 👨‍🍳 Tomorrow's Production & Milk Allocation Prep Sheet
            if (tomorrowOrders.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = DairyGreenLight,
                    border = BorderStroke(1.dp, DairyGreen.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fastfood, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("👨‍🍳 Tomorrow's Advance Milk Allocation Sheet", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1B5E20))
                        }
                        Text(
                            text = "Total Raw Milk Needed: ${totalMilkNeededTomorrow.toInt()} Liters\n" +
                                   "(Raw Milk: ${tomorrowRawMilk.toInt()} L" +
                                   (if (tomorrowPaneer > 0) " • Paneer: ${tomorrowPaneer.toInt()} kg (~${(tomorrowPaneer * 5).toInt()}L milk)" else "") +
                                   (if (tomorrowCurd > 0) " • Curd: ${tomorrowCurd.toInt()} kg (~${tomorrowCurd.toInt()}L milk)" else "") +
                                   (if (tomorrowKhoya > 0) " • Khoya: ${tomorrowKhoya.toInt()} kg (~${(tomorrowKhoya * 5).toInt()}L milk)" else "") +
                                   ")",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            lineHeight = 15.sp
                        )
                        Text(
                            text = "Ensure sufficient milk is collected tonight to fulfill advance hotel & retail bookings.",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("All", "Pending", "Due Tomorrow", "Today", "Delivered", "Cancelled").forEach { f ->
                    FilterChip(
                        selected = selectedFilter == f,
                        onClick = { selectedFilter = f },
                        label = { Text(f) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (filteredOrders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No orders found in '$selectedFilter'",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '+ Add Advance Order' to pre-book milk, paneer, curd or ghee.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredOrders, key = { it.id }) { order ->
                        OrderCard(
                            order = order,
                            deliveryDateStr = dateFormat.format(Date(order.deliveryDate)),
                            reminderDateStr = dateFormat.format(Date(order.reminderDate)),
                            onEdit = {
                                editingOrder = order
                                showAddDialog = true
                            },
                            onWhatsApp = {
                                val cust = customers.find { it.id == order.customerId }
                                val phone = cust?.mobile?.replace("+", "")?.replace(" ", "") ?: ""
                                val isHotel = cust?.type == "BULK_BUYER" || order.notes.contains("Hotel", ignoreCase = true)
                                val message = buildString {
                                    append("*🧾 MilkMate - ADVANCE ORDER VOUCHER*\n")
                                    if (isHotel) append("🏢 *Party / Hotel:* ${order.customerName}\n")
                                    else append("👤 *Customer:* ${order.customerName}\n")
                                    append("📦 *Product Item:* ${order.quantity} ${order.unit} ${order.productName}\n")
                                    append("🏷️ *Rate:* ₹${order.rate}/${order.unit} | *Total Amount:* ₹${order.totalAmount.toInt()}\n")
                                    append("📅 *Scheduled Delivery:* ${dateFormat.format(Date(order.deliveryDate))} (${order.reminderTime})\n")
                                    if (order.notes.isNotBlank()) append("📝 *Payment & Instructions:* ${order.notes}\n")
                                    append("\nThank you for choosing ${business?.businessName ?: "MilkMate"}!")
                                }
                                val url = "https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}"
                                val i = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) }
                                try {
                                    context.startActivity(i)
                                } catch (e: Exception) {
                                    viewModel.showMessage("WhatsApp not installed")
                                }
                            },
                            onToggleStatus = { newStatus ->
                                viewModel.updateOrderStatus(order.id, newStatus)
                                if (newStatus == "DELIVERED") {
                                    viewModel.showMessage("✓ Order for ${order.customerName} marked DELIVERED! Recorded in khata.")
                                } else if (newStatus == "PREPARED") {
                                    viewModel.showMessage("Order marked READY for dispatch to ${order.customerName}.")
                                }
                            },
                            onDelete = {
                                viewModel.deleteOrder(order.id)
                            }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddEditOrderDialog(
                order = editingOrder,
                customers = customers,
                supportedMilkTypes = supportedMilkTypes,
                onDismiss = { showAddDialog = false },
                onSave = { custId, custName, prod, qty, unit, rate, deliveryDate, reminderDate, reminderTime, notes ->
                    viewModel.saveOrder(
                        id = editingOrder?.id,
                        customerId = custId,
                        customerName = custName,
                        productName = prod,
                        quantity = qty,
                        unit = unit,
                        rate = rate,
                        deliveryDate = deliveryDate,
                        reminderDate = reminderDate,
                        reminderTime = reminderTime,
                        notes = notes
                    )
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun OrderStatBox(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OrderCard(
    order: OrderEntity,
    deliveryDateStr: String,
    reminderDateStr: String,
    onEdit: () -> Unit,
    onWhatsApp: () -> Unit,
    onToggleStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (order.status) {
        "DELIVERED" -> DairyGreen
        "CANCELLED" -> DangerRed
        "PREPARED" -> RoyalBluePrimary
        else -> FreshGold
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = order.customerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (order.notes.contains("HOTEL", ignoreCase = true) || order.customerName.contains("Hotel", ignoreCase = true) || order.customerName.contains("Restaurant", ignoreCase = true)) {
                            Surface(shape = RoundedCornerShape(4.dp), color = RoyalBlueLight) {
                                Text("🏨 B2B Hotel", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        } else {
                            Surface(shape = RoundedCornerShape(4.dp), color = FreshGoldLight) {
                                Text("👤 Retail", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = FreshGold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                    }
                    Text(
                        text = "${order.productName} • ${order.quantity} ${order.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = order.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "📅 Delivery: $deliveryDateStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Total: ₹${order.totalAmount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            if (order.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📝 ${order.notes}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onWhatsApp,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp Voucher", fontSize = 12.sp, color = DairyGreen, fontWeight = FontWeight.Bold)
                }

                Row {
                    if (order.status == "PENDING") {
                        FilledTonalButton(
                            onClick = { onToggleStatus("PREPARED") },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Ready", fontSize = 11.5.sp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    } else if (order.status == "PREPARED") {
                        Button(
                            onClick = { onToggleStatus("DELIVERED") },
                            colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Delivered ✓", fontSize = 11.5.sp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditOrderDialog(
    order: OrderEntity?,
    customers: List<CustomerEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, String, Double, Long, Long, String, String) -> Unit,
    supportedMilkTypes: String = "BOTH"
) {
    val context = LocalContext.current
    var selectedCustomer by remember { mutableStateOf(customers.find { it.id == order?.customerId } ?: customers.firstOrNull()) }
    val defaultProduct = when (supportedMilkTypes) {
        "BUFFALO_ONLY" -> "Buffalo Milk"
        "BLEND" -> "Blend Milk"
        else -> "Cow Milk"
    }
    var product by remember { mutableStateOf(order?.productName ?: defaultProduct) }
    var quantityText by remember { mutableStateOf(order?.quantity?.toString() ?: "5") }
    var rateText by remember { mutableStateOf(order?.rate?.toString() ?: (if (supportedMilkTypes == "BUFFALO_ONLY") "75" else if (supportedMilkTypes == "BLEND") "55" else "60")) }
    var unit by remember { mutableStateOf(order?.unit ?: "L") }
    var deliveryDateMillis by remember { mutableStateOf(order?.deliveryDate ?: (System.currentTimeMillis() + 86400000L)) }
    var reminderTime by remember { mutableStateOf(order?.reminderTime ?: "07:00 AM") }
    var advancePaidText by remember { mutableStateOf("") }
    var advancePaymentMode by remember { mutableStateOf("UPI") }
    var notes by remember { mutableStateOf(order?.notes ?: "") }

    val products = buildList {
        if (supportedMilkTypes == "BLEND") {
            add("Blend Milk")
        } else {
            if (supportedMilkTypes != "BUFFALO_ONLY") add("Cow Milk")
            if (supportedMilkTypes != "COW_ONLY") add("Buffalo Milk")
        }
        addAll(listOf("Paneer", "Ghee", "Curd", "Khoa", "Butter"))
    }
    val total = (quantityText.toDoubleOrNull() ?: 0.0) * (rateText.toDoubleOrNull() ?: 0.0)
    val advPaid = advancePaidText.toDoubleOrNull() ?: 0.0
    val balanceDue = (total - advPaid).coerceAtLeast(0.0)
    val displayDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (order == null) "Book Advance Order" else "Edit Advance Order", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (customers.isEmpty()) {
                    Text("No customers found. Please add customers first in Parties tab.", color = DangerRed)
                } else {
                    Text("Select Party / Buyer *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                    var expandedCust by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedCust = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            val custLabel = selectedCustomer?.let {
                                if (it.type == "BULK_BUYER") "🏨 ${it.name} (Hotel / B2B)" else "👤 ${it.name} (Household)"
                            } ?: "Select Customer"
                            Text(custLabel, fontWeight = FontWeight.SemiBold)
                        }
                        DropdownMenu(
                            expanded = expandedCust,
                            onDismissRequest = { expandedCust = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            customers.forEach { c ->
                                DropdownMenuItem(
                                    text = {
                                        Text(if (c.type == "BULK_BUYER") "🏨 ${c.name} [B2B Hotel]" else "👤 ${c.name} [Household]")
                                    },
                                    onClick = {
                                        selectedCustomer = c
                                        expandedCust = false
                                    }
                                )
                            }
                        }
                    }

                    Text("Product Item *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                    var expandedProd by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedProd = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            val icon = when {
                                product.contains("Paneer") -> "🧀"
                                product.contains("Ghee") -> "🧈"
                                product.contains("Curd") -> "🥣"
                                product.contains("Khoa") -> "🍮"
                                else -> "🥛"
                            }
                            Text("$icon $product", fontWeight = FontWeight.SemiBold)
                        }
                        DropdownMenu(
                            expanded = expandedProd,
                            onDismissRequest = { expandedProd = false }
                        ) {
                            products.forEach { p ->
                                DropdownMenuItem(
                                    text = {
                                        val icon = when {
                                            p.contains("Paneer") -> "🧀"
                                            p.contains("Ghee") -> "🧈"
                                            p.contains("Curd") -> "🥣"
                                            p.contains("Khoa") -> "🍮"
                                            else -> "🥛"
                                        }
                                        Text("$icon $p")
                                    },
                                    onClick = {
                                        product = p
                                        unit = if (p.contains("Milk") || p.contains("Curd")) "L" else "Kg"
                                        rateText = when (p) {
                                            "Cow Milk" -> "60"
                                            "Buffalo Milk" -> "75"
                                            "Paneer" -> "380"
                                            "Ghee" -> "650"
                                            "Curd" -> "80"
                                            "Khoa" -> "350"
                                            "Butter" -> "550"
                                            else -> "50"
                                        }
                                        expandedProd = false
                                    }
                                )
                            }
                        }
                    }

                    // Scheduled Delivery Date
                    Text("Target Delivery Date *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = deliveryDateMillis }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val newCal = Calendar.getInstance().apply {
                                            set(year, month, day, 0, 0, 0)
                                        }
                                        deliveryDateMillis = newCal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(displayDateFormat.format(Date(deliveryDateMillis)), fontSize = 12.sp)
                        }

                        FilledTonalButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                                deliveryDateMillis = cal.timeInMillis
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Tomorrow", fontSize = 11.sp)
                        }
                    }

                    // Quantity and Rate
                    Text("Order Quantity & Rate", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            label = { Text("Qty ($unit)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = rateText,
                            onValueChange = { rateText = it },
                            label = { Text("Rate (₹/$unit)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Advance Token Deposit
                    Text("Advance Token Deposit (Optional)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = advancePaidText,
                            onValueChange = { advancePaidText = it },
                            label = { Text("Advance Paid (₹)") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        var expandedPay by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                            OutlinedButton(
                                onClick = { expandedPay = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(advancePaymentMode, fontSize = 12.sp)
                            }
                            DropdownMenu(expanded = expandedPay, onDismissRequest = { expandedPay = false }) {
                                listOf("UPI", "Cash", "Bank Transfer").forEach { m ->
                                    DropdownMenuItem(text = { Text(m) }, onClick = { advancePaymentMode = m; expandedPay = false })
                                }
                            }
                        }
                    }

                    // Total & Balance Preview Banner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DairyGreenLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Order Value:", fontSize = 12.sp, color = TextPrimary)
                                Text("₹${total.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            if (advPaid > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Token Advance Paid ($advancePaymentMode):", fontSize = 11.5.sp, color = DairyGreen, fontWeight = FontWeight.SemiBold)
                                    Text("-₹${advPaid.toInt()}", fontSize = 12.sp, color = DairyGreen, fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(color = DairyGreen.copy(alpha = 0.3f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Balance Due on Delivery:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                                    Text("₹${balanceDue.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reminderTime,
                        onValueChange = { reminderTime = it },
                        label = { Text("Delivery Slot / Shift (e.g. 07:00 AM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Kitchen instructions / Delivery notes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cust = selectedCustomer ?: return@Button
                    val qty = quantityText.toDoubleOrNull() ?: 1.0
                    val rate = rateText.toDoubleOrNull() ?: 0.0
                    val formattedNotes = buildString {
                        if (advPaid > 0) {
                            append("[Token: ₹${advPaid.toInt()} via $advancePaymentMode | Balance: ₹${balanceDue.toInt()}] ")
                        }
                        if (notes.isNotBlank()) append(notes)
                    }
                    onSave(cust.id, cust.name, product, qty, unit, rate, deliveryDateMillis, deliveryDateMillis, reminderTime, formattedNotes)
                },
                enabled = selectedCustomer != null,
                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("✓ Save Advance Order", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
