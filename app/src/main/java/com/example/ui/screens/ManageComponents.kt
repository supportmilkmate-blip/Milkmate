package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DeliveryEntity
import com.example.data.local.entity.StaffEntity
import com.example.ui.theme.*
import java.util.Locale

/**
 * Shared Attendance Pill Metric
 */
@Composable
fun AttendancePill(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (label.contains("Rate", ignoreCase = true)) "$count%" else count.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = color
            )
            Text(label, fontSize = 9.sp, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Shared Attendance Status Button (P, H, A, L)
 */
@Composable
fun StatusButton(label: String, isSelected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(34.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Shared Add Staff Dialog
 */
/**
 * Data class representing role preset for quick configuration
 */
data class RolePreset(
    val roleKey: String,
    val title: String,
    val iconName: String,
    val defaultPermissions: List<String>,
    val description: String
)

@Composable
fun AddStaffDialog(
    businessMode: String = "DAIRY_FARM",
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        name: String,
        mobile: String,
        pin: String,
        role: String,
        permissions: String,
        route: String,
        monthlySalary: Double,
        dailyWage: Double,
        salaryType: String,
        customRoleTitle: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var selectedRoleKey by remember { mutableStateOf("DELIVERY_BOY") }
    var customRoleTitle by remember { mutableStateOf("") }
    var isCustomRole by remember { mutableStateOf(false) }
    var route by remember { mutableStateOf("") }
    var salaryType by remember { mutableStateOf("MONTHLY") }
    var salaryText by remember { mutableStateOf("12000") }
    var wageText by remember { mutableStateOf("400") }

    var enableAppLogin by remember { mutableStateOf(false) }

    // Catalog of all permission modules
    val allModules = remember {
        listOf(
            "ALL" to "👑 Full Supervisor Access (Owner Level)",
            "HERD_MILKING" to "🐮 Herd & Milking Recording",
            "CATTLE_HEALTH" to "🩺 Vet, Deworming & Vaccination",
            "MILK_COLLECTION" to "🥛 Milk Collection & Fat/SNF Testing",
            "DELIVERIES" to "🚚 Deliveries & Tanker Dispatch",
            "CUSTOMERS_KHATA" to "👥 Customers & Khata Ledger",
            "INVENTORY_FEED" to "📦 Cattle Feed & Stock Register",
            "PAYMENTS_EXPENSES" to "💰 Payments & Farm Expenses",
            "REPORTS_ANALYTICS" to "📊 Reports & Analytics"
        )
    }

    // Role Presets based on Dairy Business Mode
    val presets: List<RolePreset> = remember(businessMode) {
        when (businessMode) {
            "COLLECTION_CENTER" -> listOf(
                RolePreset("SUPERVISOR", "Centre Manager", "👑", listOf("ALL"), "Full access to manage collection desk, farmers & milk dispatch"),
                RolePreset("LAB_TESTER", "Milk Intake / Tester", "🧪", listOf("MILK_COLLECTION"), "Record daily farmer milk collection, Fat & SNF analyzer testing"),
                RolePreset("ACCOUNTANT", "Billing & Payment Clerk", "💳", listOf("PAYMENTS_EXPENSES", "CUSTOMERS_KHATA"), "Manage farmer pay-cycles, cash disbursements & khata"),
                RolePreset("DISPATCHER", "Tanker Dispatcher", "🚛", listOf("DELIVERIES"), "Manage bulk milk dispatches & tanker outward supply")
            )
            "TRADER" -> listOf(
                RolePreset("SUPERVISOR", "Operations Supervisor", "👑", listOf("ALL"), "Full access to village collection, route deliveries & accounts"),
                RolePreset("COLLECTOR", "Village Milk Collector", "🏍️", listOf("MILK_COLLECTION"), "Collect milk from village households and local suppliers"),
                RolePreset("DELIVERY_BOY", "Retail Delivery Runner", "🚚", listOf("DELIVERIES"), "Morning & evening door-to-door customer bottle/pouch deliveries"),
                RolePreset("ACCOUNTANT", "Khata & Ledger Clerk", "📑", listOf("CUSTOMERS_KHATA", "PAYMENTS_EXPENSES"), "Customer payment collection, dues reminder & billing")
            )
            else -> listOf(
                RolePreset("SUPERVISOR", "Supervisor / Manager", "👑", listOf("ALL"), "Full owner-level operational access across all farm modules"),
                RolePreset("MILKER", "Milking Staff", "🥛", listOf("HERD_MILKING"), "Daily morning & evening cattle milking records & yield logs"),
                RolePreset("FEEDER", "Feeding & Silage Worker", "🌾", listOf("INVENTORY_FEED"), "Cattle feeding logs, green fodder, silage and concentrate stock"),
                RolePreset("VET", "Doctor / Vet Staff", "🩺", listOf("CATTLE_HEALTH"), "Deworming, vaccinations, AI breeding cycles & medical care"),
                RolePreset("CLEANER", "Caretaker / Cleaner", "🧹", listOf("HERD_MILKING", "CATTLE_HEALTH"), "Shed sanitization, cattle observations & general maintenance"),
                RolePreset("PROCESSING", "Milk Processing & Packing", "📦", listOf("INVENTORY_FEED", "DELIVERIES"), "Value addition (paneer, ghee, curd) and pouch batch packaging"),
                RolePreset("DELIVERY_BOY", "Delivery Boy / Driver", "🚚", listOf("DELIVERIES"), "Customer delivery route execution and daily fulfillment")
            )
        }
    }

    var selectedPermissions by remember {
        mutableStateOf(setOf("DELIVERIES"))
    }

    // Apply preset permissions when a preset role is tapped
    fun applyPreset(preset: RolePreset) {
        selectedRoleKey = preset.roleKey
        isCustomRole = false
        customRoleTitle = preset.title
        selectedPermissions = preset.defaultPermissions.toSet()
        enableAppLogin = when (preset.roleKey) {
            "SUPERVISOR", "DELIVERY_BOY", "DISPATCHER", "LAB_TESTER", "ACCOUNTANT" -> true
            else -> false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (enableAppLogin) "Add Staff (App Login Access)" else "Add Farm Worker",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = if (enableAppLogin) "Worker can log into MilkMate on their smartphone" else "Tracks attendance and wages without phone login",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Personal Information
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (enableAppLogin) "Staff Full Name *" else "Worker Full Name *") },
                    placeholder = { Text("e.g. Ramesh Kumar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("staff_name_input")
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text(if (enableAppLogin) "Mobile (Required for Login ID) *" else "Mobile Number (Optional)") },
                    placeholder = { Text("e.g. 9876543210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("staff_mobile_input")
                )

                // 🔑 Enable App Login Switch Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (enableAppLogin) RoyalBlueLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (enableAppLogin) RoyalBluePrimary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (enableAppLogin) RoyalBluePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (enableAppLogin) Icons.Default.VpnKey else Icons.Default.PersonOutline,
                                            contentDescription = null,
                                            tint = if (enableAppLogin) RoyalBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Enable App Login (ID & PIN)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (enableAppLogin) RoyalBluePrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        if (enableAppLogin) "Creates smartphone login for MilkMate" else "Worker only (Attendance & Wages, no login)",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = enableAppLogin,
                                onCheckedChange = { enableAppLogin = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = RoyalBluePrimary
                                ),
                                modifier = Modifier.testTag("toggle_staff_app_login")
                            )
                        }

                        if (enableAppLogin) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = RoyalBluePrimary.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = pin,
                                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                                    label = { Text("4-Digit PIN *") },
                                    placeholder = { Text("e.g. 1234") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("staff_pin_input")
                                )

                                Button(
                                    onClick = {
                                        pin = (1000..9999).random().toString()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Text("Auto PIN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "A unique Staff ID will be generated with 1-tap WhatsApp credential sharing.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Role Presets by Business Mode
                Text("Select Role / Job Preset:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.chunked(2).forEach { rowPresets ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            rowPresets.forEach { preset ->
                                FilterChip(
                                    selected = !isCustomRole && selectedRoleKey == preset.roleKey && customRoleTitle == preset.title,
                                    onClick = { applyPreset(preset) },
                                    label = { Text("${preset.iconName} ${preset.title}", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowPresets.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Custom Role Option
                    FilterChip(
                        selected = isCustomRole,
                        onClick = {
                            isCustomRole = true
                            selectedRoleKey = "CUSTOM"
                        },
                        label = { Text("✏️ Custom Role (Enter Manual Title)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isCustomRole) {
                    OutlinedTextField(
                        value = customRoleTitle,
                        onValueChange = { customRoleTitle = it },
                        label = { Text("Custom Role Title *") },
                        placeholder = { Text("e.g. Chilling Plant Incharge") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Granular Permissions (Only needed if app login is enabled)
                if (enableAppLogin) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Module Access Permissions:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = {
                                if (selectedPermissions.contains("ALL")) {
                                    selectedPermissions = emptySet()
                                } else {
                                    selectedPermissions = setOf("ALL")
                                }
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                if (selectedPermissions.contains("ALL")) "Revoke All" else "Grant All (Supervisor)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            allModules.forEach { (permKey, permLabel) ->
                                val isChecked = selectedPermissions.contains("ALL") || selectedPermissions.contains(permKey)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (permKey == "ALL") {
                                                selectedPermissions = if (isChecked) emptySet() else setOf("ALL")
                                            } else {
                                                val current = selectedPermissions.toMutableSet()
                                                current.remove("ALL")
                                                if (current.contains(permKey)) {
                                                    current.remove(permKey)
                                                } else {
                                                    current.add(permKey)
                                                }
                                                selectedPermissions = current
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (permKey == "ALL") {
                                                selectedPermissions = if (checked) setOf("ALL") else emptySet()
                                            } else {
                                                val current = selectedPermissions.toMutableSet()
                                                current.remove("ALL")
                                                if (checked) current.add(permKey) else current.remove(permKey)
                                                selectedPermissions = current
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(permLabel, fontSize = 11.sp, fontWeight = if (permKey == "ALL") FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = route,
                    onValueChange = { route = it },
                    label = { Text("Assigned Route (Optional)") },
                    placeholder = { Text("e.g. Village North Route / Sector 14") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Compensation & Wages
                Text("Wage / Compensation:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = salaryType == "MONTHLY",
                        onClick = { salaryType = "MONTHLY" },
                        label = { Text("Monthly Salary") }
                    )
                    FilterChip(
                        selected = salaryType == "DAILY",
                        onClick = { salaryType = "DAILY" },
                        label = { Text("Daily Wage") }
                    )
                }

                if (salaryType == "MONTHLY") {
                    OutlinedTextField(
                        value = salaryText,
                        onValueChange = { salaryText = it },
                        label = { Text("Monthly Salary (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = wageText,
                        onValueChange = { wageText = it },
                        label = { Text("Daily Wage Rate (₹/day)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            val isFormValid = if (enableAppLogin) {
                name.isNotBlank() && mobile.isNotBlank() && pin.length >= 4
            } else {
                name.isNotBlank()
            }

            Button(
                onClick = {
                    if (isFormValid) {
                        val sal = salaryText.toDoubleOrNull() ?: 12000.0
                        val wg = wageText.toDoubleOrNull() ?: 400.0
                        val permsString = if (enableAppLogin) {
                            if (selectedPermissions.isEmpty()) "DELIVERIES" else selectedPermissions.joinToString(",")
                        } else {
                            "NONE"
                        }
                        val finalRole = if (isCustomRole) "CUSTOM" else selectedRoleKey
                        val finalPin = if (enableAppLogin) pin else ""
                        onSave(null, name, mobile, finalPin, finalRole, permsString, route, sal, wg, salaryType, customRoleTitle)
                    }
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                modifier = Modifier.testTag("save_staff_member_btn")
            ) {
                Text(if (enableAppLogin) "Save & Generate Login ID" else "Save Farm Worker")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Backward compatibility overload
 */
@Composable
fun AddStaffDialog(
    onDismiss: () -> Unit,
    onSave: (String?, String, String, String, String, String, String, Double, Double, String) -> Unit
) {
    AddStaffDialog(
        businessMode = "DAIRY_FARM",
        onDismiss = onDismiss,
        onSave = { id, name, mobile, pin, role, perms, route, sal, wage, sType, _ ->
            onSave(id, name, mobile, pin, role, perms, route, sal, wage, sType)
        }
    )
}

/**
 * Shared Staff Routes Tab
 */
@Composable
fun StaffRoutesTab(
    staffList: List<StaffEntity>,
    deliveries: List<DeliveryEntity>
) {
    val deliveryBoys = staffList.filter { it.role == "DELIVERY_BOY" || it.role == "MANAGER" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Delivery Routes & Staff Coverage", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RoyalBluePrimary)
                    Text("Track delivery boy progress and assigned areas across daily morning & evening rounds.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (deliveryBoys.isEmpty()) {
            item {
                Text("No delivery boys registered in your staff team.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(deliveryBoys, key = { it.id }) { boy ->
                val assignedRoute = boy.assignedRoute.ifBlank { "All Areas" }
                val completedDeliveries = deliveries.count { it.isDelivered }
                val totalDeliveries = deliveries.size

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(boy.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Route: $assignedRoute", fontSize = 12.sp, color = RoyalBluePrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = DairyGreenLight) {
                                Text("ACTIVE DISPATCH", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DairyGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Assigned Round", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$totalDeliveries Households", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Column {
                                Text("Completed Today", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$completedDeliveries Delivered", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DairyGreen)
                            }

                            Column {
                                Text("Completion", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val pct = if (totalDeliveries > 0) (completedDeliveries * 100) / totalDeliveries else 0
                                Text("$pct%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Shared Notification Toggle Row
 */
@Composable
fun NotificationToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RoyalBluePrimary)
        )
    }
}

/**
 * Shared Alert Notification Card
 */
@Composable
fun AlertNotificationCard(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    message: String,
    timeText: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBg.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconBg, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(timeText, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$actionLabel →",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = RoyalBluePrimary,
                    modifier = Modifier.clickable { onAction() }
                )
            }
        }
    }
}

/**
 * 2D FAT × SNF Rate Chart Matrix Table
 */
@Composable
fun FatSnfRateMatrixTable(
    baseRate: Double,
    fatMultiplier: Double,
    snfMultiplier: Double,
    qualityBonus: Double,
    chillingDeduction: Double,
    pricingMode: String,
    fixedCowRate: Double,
    fixedBuffaloRate: Double,
    supportedMilkTypes: String = "BOTH"
) {
    val fatValues = listOf(3.5, 4.0, 4.5, 5.0, 6.0, 7.0, 8.0, 9.0)
    val snfValues = listOf(7.5, 8.0, 8.5, 9.0, 9.5)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 Live FAT × SNF Rate Chart (₹/L)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = RoyalBluePrimary
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = DairyGreenLight
                ) {
                    Text(
                        text = "Dynamic Matrix",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = DairyGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable 2D Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Column {
                    // Header Row: SNF labels
                    Row(
                        modifier = Modifier
                            .background(RoyalBluePrimary, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = "FAT \\ SNF",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.width(64.dp).padding(start = 6.dp)
                        )
                        snfValues.forEach { snf ->
                            Text(
                                text = "${snf}%",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.width(52.dp)
                            )
                        }
                    }

                    // Data Rows: FAT and calculated prices
                    fatValues.forEachIndexed { idx, fat ->
                        val rowBg = if (idx % 2 == 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                        Row(
                            modifier = Modifier
                                .background(rowBg)
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${fat}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = RoyalBluePrimary,
                                modifier = Modifier.width(64.dp).padding(start = 6.dp)
                            )

                            snfValues.forEach { snf ->
                                val calculatedRate = when (pricingMode) {
                                    "DIRECT" -> when (supportedMilkTypes) {
                                        "COW_ONLY" -> fixedCowRate
                                        "BUFFALO_ONLY" -> fixedBuffaloRate
                                        else -> if (fat >= 6.0) fixedBuffaloRate else fixedCowRate
                                    }
                                    "FAT_ONLY" -> baseRate + (fat * fatMultiplier)
                                    "FAT_SNF" -> (fat * fatMultiplier) + (snf * snfMultiplier)
                                    else -> baseRate + (fat * fatMultiplier) + (snf * snfMultiplier) + qualityBonus - chillingDeduction
                                }
                                Text(
                                    text = "₹${String.format(Locale.US, "%.1f", calculatedRate)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (calculatedRate >= 60.0) DairyGreen else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.width(52.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Share WhatsApp Rate Chart Formatter
 */
fun shareRateChartToWhatsApp(
    context: Context,
    dairyName: String,
    pricingMode: String,
    baseRate: Double,
    cowRate: Double,
    buffRate: Double,
    fatMul: Double,
    snfMul: Double,
    supportedMilkTypes: String = "BOTH"
) {
    val message = buildString {
        append("🥛 *${dairyName.uppercase()} - OFFICIAL MILK RATE CHART*\n")
        append("📅 Effective Date: Today\n")
        append("━━━━━━━━━━━━━━━━━━━━\n")
        if (pricingMode == "DIRECT") {
            if (supportedMilkTypes != "BUFFALO_ONLY") {
                append("🐄 *Cow Milk Fixed Rate:* ₹${cowRate.toInt()} / Liter\n")
            }
            if (supportedMilkTypes != "COW_ONLY") {
                append("🐃 *Buffalo Milk Fixed Rate:* ₹${buffRate.toInt()} / Liter\n")
            }
        } else {
            append("🧪 *Pricing Mode:* $pricingMode\n")
            append("• Base Rate: ₹$baseRate / L\n")
            append("• FAT Multiplier: ₹$fatMul per point\n")
            append("• SNF Multiplier: ₹$snfMul per point\n")
            append("\n*Sample Rates per Liter:*\n")
            if (supportedMilkTypes != "BUFFALO_ONLY") {
                append("• 3.5% FAT / 8.5% SNF (Standard): ₹${String.format(Locale.US, "%.2f", baseRate + (3.5 * fatMul) + (8.5 * snfMul))}\n")
                append("• 4.5% FAT / 8.5% SNF (Premium): ₹${String.format(Locale.US, "%.2f", baseRate + (4.5 * fatMul) + (8.5 * snfMul))}\n")
            }
            if (supportedMilkTypes != "COW_ONLY") {
                append("• 6.5% FAT / 9.0% SNF (Rich): ₹${String.format(Locale.US, "%.2f", baseRate + (6.5 * fatMul) + (9.0 * snfMul))}\n")
                append("• 7.5% FAT / 9.0% SNF (High Fat): ₹${String.format(Locale.US, "%.2f", baseRate + (7.5 * fatMul) + (9.0 * snfMul))}\n")
            }
        }
        append("━━━━━━━━━━━━━━━━━━━━\n")
        append("Generated by MilkMate Dairy Operations App")
    }

    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra("jid", "")
            setPackage("com.whatsapp")
        }
        context.startActivity(sendIntent)
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Milk Rate Chart"))
    }
}

/**
 * Share WhatsApp Staff Salary Slip Formatter
 */
fun shareStaffSalarySlipToWhatsApp(
    context: Context,
    dairyName: String,
    staffName: String,
    staffPhone: String,
    monthYear: String,
    effectiveDays: Double,
    grossSalary: Double,
    advances: Double,
    netPaid: Double,
    paymentMode: String
) {
    val message = buildString {
        append("🧾 *${dairyName.uppercase()} - SALARY VOUCHER*\n")
        append("━━━━━━━━━━━━━━━━━━━━\n")
        append("👤 *Staff Name:* $staffName\n")
        append("📅 *Month:* $monthYear\n")
        append("📊 *Working Days Recorded:* $effectiveDays days\n")
        append("━━━━━━━━━━━━━━━━━━━━\n")
        append("💵 *Gross Earnings:* ₹${grossSalary.toInt()}\n")
        append("💸 *Advances Deducted:* ₹${advances.toInt()}\n")
        append("✅ *Net Disbursed Amount:* ₹${netPaid.toInt()}\n")
        append("💳 *Payment Mode:* $paymentMode\n")
        append("━━━━━━━━━━━━━━━━━━━━\n")
        append("Thank you for your dedicated service to ${dairyName}!\n")
        append("Sent via MilkMate Dairy Management")
    }

    try {
        val url = "https://api.whatsapp.com/send?phone=+91${staffPhone.filter { it.isDigit() }}&text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Salary Slip"))
    }
}
