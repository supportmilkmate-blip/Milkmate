package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.CustomerLeaveEntity
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLeaveDialog(
    viewModel: MilkMateViewModel,
    preSelectedCustomer: CustomerEntity? = null,
    onDismiss: () -> Unit,
    onGoToPackingConfirmation: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val allCustomers by viewModel.customers.collectAsStateWithLifecycle()
    val allLeaves by viewModel.customerLeaves.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedShift by viewModel.selectedShift.collectAsStateWithLifecycle()

    var selectedCustomer by remember { mutableStateOf(preSelectedCustomer ?: allCustomers.firstOrNull()) }
    var customerSearchQuery by remember { mutableStateOf("") }
    var isSelectingCustomer by remember { mutableStateOf(preSelectedCustomer == null) }

    var startDate by remember { mutableStateOf(selectedDate) }
    var endDate by remember { mutableStateOf(selectedDate) }
    var isMultiDay by remember { mutableStateOf(false) }
    var leaveShift by remember { mutableStateOf(selectedShift) } // "MORNING", "EVENING", "BOTH"
    var selectedReason by remember { mutableStateOf("Customer informed: Do not supply milk on this day") }
    var customReason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    val reasonPresets = listOf(
        "Customer informed: Do not supply milk on this day",
        "Out of town / Traveling",
        "No milk needed today",
        "Guest departed / Lower requirement",
        "Festival / Family function away",
        "Other"
    )

    // Calculate quantity saved per day for this customer
    val dailyQty = remember(selectedCustomer, leaveShift) {
        val cust = selectedCustomer ?: return@remember 0.0
        val baseCow = if (cust.milkType == "COW" || cust.milkType == "BOTH") cust.cowQuantity else 0.0
        val baseBuff = if (cust.milkType == "BUFFALO" || cust.milkType == "BOTH") cust.buffaloQuantity else 0.0
        val totalDaily = baseCow + baseBuff
        when (leaveShift) {
            "MORNING" -> if (cust.defaultShift == "BOTH") totalDaily / 2.0 else totalDaily
            "EVENING" -> if (cust.defaultShift == "BOTH") totalDaily / 2.0 else totalDaily
            else -> totalDaily
        }
    }

    // Existing active leaves for this customer
    val activeLeavesForCust = remember(selectedCustomer, allLeaves) {
        val cid = selectedCustomer?.id ?: return@remember emptyList()
        allLeaves.filter { it.customerId == cid && it.status == "ACTIVE" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PauseCircleFilled,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Customer Milk Leave / Vacation",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Pause delivery & update packing requirements",
                        fontSize = 11.sp,
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Customer Selector
                if (preSelectedCustomer == null || isSelectingCustomer) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Select Customer",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = customerSearchQuery,
                            onValueChange = { customerSearchQuery = it },
                            placeholder = { Text("Search by name or mobile...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        val matching = allCustomers.filter {
                            customerSearchQuery.isBlank() ||
                                    it.name.contains(customerSearchQuery, ignoreCase = true) ||
                                    it.mobile.contains(customerSearchQuery)
                        }.take(5)

                        matching.forEach { cust ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedCustomer?.id == cust.id) RoyalBluePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (selectedCustomer?.id == cust.id) androidx.compose.foundation.BorderStroke(1.5.dp, RoyalBluePrimary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCustomer = cust
                                        isSelectingCustomer = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${cust.route.ifBlank { "No Route" }} • ${cust.mobile}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = "${cust.defaultQuantity}L (${cust.milkType})",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = RoyalBluePrimary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Selected Customer Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RoyalBluePrimary.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = selectedCustomer?.name ?: "Customer",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${selectedCustomer?.route?.ifBlank { "Route" }} • Regular: ${selectedCustomer?.defaultQuantity ?: 1.0}L (${selectedCustomer?.milkType})",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { isSelectingCustomer = true }) {
                                Text("Change", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 2. Date Selection (Single Day vs Date Range)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Leave Date",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isMultiDay,
                                onCheckedChange = {
                                    isMultiDay = it
                                    if (!it) endDate = startDate
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Multiple Days Vacation", fontSize = 11.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Start Date Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val cal = Calendar.getInstance().apply { timeInMillis = startDate }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val c = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }
                                            startDate = c.timeInMillis
                                            if (!isMultiDay || endDate < startDate) {
                                                endDate = startDate
                                            }
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(if (isMultiDay) "Start Date" else "Date", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = RoyalBluePrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(dateFormat.format(Date(startDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (isMultiDay) {
                            // End Date Button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val cal = Calendar.getInstance().apply { timeInMillis = endDate }
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                val c = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }
                                                endDate = c.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("End Date", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp), tint = RoyalBluePrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(dateFormat.format(Date(endDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Shift Selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Applicable Shift",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("BOTH", "Full Day (All Shifts)", Icons.Default.AllInclusive),
                            Triple("MORNING", "Morning Shift Only", Icons.Default.WbSunny),
                            Triple("EVENING", "Evening Shift Only", Icons.Default.Nightlight)
                        ).forEach { (code, label, icon) ->
                            val isSel = leaveShift == code
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { leaveShift = code }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = code,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Reason Presets
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Reason Informed by Customer",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        reasonPresets.forEach { preset ->
                            FilterChip(
                                selected = selectedReason == preset,
                                onClick = { selectedReason = preset },
                                label = { Text(preset, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (selectedReason == "Other") {
                        OutlinedTextField(
                            value = customReason,
                            onValueChange = { customReason = it },
                            placeholder = { Text("Type reason...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // 5. Quantity Impact & Packing Notice
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF3C7),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Packing Impact: -${String.format(Locale.US, "%.1f", dailyQty)}L deducted",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "Packing Confirmation will automatically subtract this milk from today's loading checklist.",
                                fontSize = 10.5.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }

                // Existing Active Leaves (if any)
                if (activeLeavesForCust.isNotEmpty()) {
                    Text(
                        text = "Active Leaves for this Customer",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    activeLeavesForCust.forEach { l ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${dateFormat.format(Date(l.startDate))} - ${dateFormat.format(Date(l.endDate))} (${l.shift})",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp
                                    )
                                    Text(
                                        text = l.reason,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(
                                    onClick = { viewModel.cancelCustomerLeave(l.id) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
                                ) {
                                    Text("Resume Milk", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {
                        val cust = selectedCustomer ?: return@Button
                        val finalReason = if (selectedReason == "Other") customReason.ifBlank { "Customer informed not to supply" } else selectedReason
                        viewModel.recordCustomerLeave(
                            customerId = cust.id,
                            customerName = cust.name,
                            customerMobile = cust.mobile,
                            startDate = startDate,
                            endDate = if (isMultiDay) endDate else startDate,
                            shift = leaveShift,
                            reason = finalReason,
                            quantitySaved = dailyQty,
                            milkType = cust.milkType,
                            notes = notes
                        )
                        onDismiss()
                        onGoToPackingConfirmation?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GrassGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_save_leave_and_packing")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (onGoToPackingConfirmation != null) "Save & Go to Packing Recheck" else "Confirm & Save Leave",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                if (onGoToPackingConfirmation != null) {
                    OutlinedButton(
                        onClick = {
                            val cust = selectedCustomer ?: return@OutlinedButton
                            val finalReason = if (selectedReason == "Other") customReason.ifBlank { "Customer informed not to supply" } else selectedReason
                            viewModel.recordCustomerLeave(
                                customerId = cust.id,
                                customerName = cust.name,
                                customerMobile = cust.mobile,
                                startDate = startDate,
                                endDate = if (isMultiDay) endDate else startDate,
                                shift = leaveShift,
                                reason = finalReason,
                                quantitySaved = dailyQty,
                                milkType = cust.milkType,
                                notes = notes
                            )
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Leave Only", fontSize = 11.sp)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
