package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.example.data.local.entity.BusinessEntity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import com.example.data.local.entity.StaffActivityLogEntity
import com.example.data.local.entity.StaffAttendanceEntity
import com.example.data.local.entity.StaffEntity
import com.example.data.local.entity.StaffPaymentEntity
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScreen(viewModel: MilkMateViewModel, onBack: (() -> Unit)? = null) {
    val staffList by viewModel.staffList.collectAsStateWithLifecycle()
    val todayAttendance by viewModel.todayAttendance.collectAsStateWithLifecycle()
    val allAttendance by viewModel.allAttendance.collectAsStateWithLifecycle()
    val staffPayments by viewModel.staffPayments.collectAsStateWithLifecycle()
    val activityLogs by viewModel.staffActivityLogs.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val deliveries by viewModel.todayDeliveries.collectAsStateWithLifecycle()
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newlyAddedStaffCredentials by remember { mutableStateOf<StaffCredentialsShareData?>(null) }
    var staffToEnableLogin by remember { mutableStateOf<StaffEntity?>(null) }
    val context = LocalContext.current

    Scaffold(
        floatingActionButton = {
            if (activeTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = RoyalBluePrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_staff_fab")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Staff Member")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Action Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${staffList.size} Staff Members • Roster & Wage",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (activeTab == 1 && staffList.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            viewModel.markAllStaffAttendance(staffList, selectedDate, "PRESENT")
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("✓ Mark All Present", fontWeight = FontWeight.Bold, color = DairyGreen, fontSize = 12.sp)
                    }
                }
            }
            // Segmented Tabs
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = RoyalBluePrimary,
                edgePadding = 12.dp
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("👥 Staff (${staffList.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("📅 Attendance", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("💰 Wages & Slips", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    text = { Text("🚚 Routes", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
                    text = { Text("⚡ Live Activity (${activityLogs.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (activeTab) {
                    0 -> StaffDirectoryTab(
                        staffList = staffList,
                        business = business,
                        sessionBusinessId = viewModel.sessionState.value.businessId,
                        onDeleteStaff = { viewModel.deleteStaff(it) },
                        onEnableLogin = { staffToEnableLogin = it },
                        onShareCredentials = { newlyAddedStaffCredentials = it }
                    )
                    1 -> StaffAttendanceTab(
                        staffList = staffList,
                        attendanceList = todayAttendance,
                        selectedDate = selectedDate,
                        onMarkAttendance = { staffId, name, status ->
                            viewModel.recordStaffAttendance(staffId, name, selectedDate, status)
                        },
                        onMarkAllPresent = {
                            viewModel.markAllStaffAttendance(staffList, selectedDate, "PRESENT")
                        },
                        onChangeDate = { viewModel.setDate(it) }
                    )
                    2 -> StaffSalariesTab(
                        staffList = staffList,
                        attendanceList = allAttendance,
                        paymentsList = staffPayments,
                        dairyName = business?.businessName ?: "MilkMate Dairy",
                        onRecordPayment = { staffId, name, type, amount, mode, monthYear, note ->
                            viewModel.recordStaffPayment(staffId, name, type, amount, mode, System.currentTimeMillis(), monthYear, note)
                        },
                        onDeletePayment = { viewModel.deleteStaffPayment(it) }
                    )
                    3 -> StaffRoutesTab(
                        staffList = staffList,
                        deliveries = deliveries
                    )
                    4 -> StaffActivityLogTab(
                        activityLogs = activityLogs,
                        staffList = staffList
                    )
                }
            }
        }

        if (showAddDialog) {
            AddStaffDialog(
                businessMode = business?.businessMode ?: "DAIRY_FARM",
                onDismiss = { showAddDialog = false },
                onSave = { staffId, name, phone, pin, role, permissions, route, salary, wage, sType, customTitle ->
                    val hasAppLogin = pin.isNotBlank()
                    val finalStaffId = staffId ?: if (hasAppLogin) {
                        "STF-${UUID.randomUUID().toString().take(6).uppercase()}"
                    } else {
                        "WKR-${UUID.randomUUID().toString().take(6).uppercase()}"
                    }
                    viewModel.saveStaff(
                        id = finalStaffId,
                        name = name,
                        mobile = phone,
                        pin = pin,
                        role = role,
                        permissions = permissions,
                        assignedRoute = route,
                        monthlySalary = salary,
                        dailyWage = wage,
                        salaryType = sType,
                        customRoleTitle = customTitle
                    )
                    showAddDialog = false
                    if (hasAppLogin) {
                        newlyAddedStaffCredentials = StaffCredentialsShareData(
                            dairyName = business?.businessName ?: "MilkMate Dairy",
                            businessId = business?.id ?: viewModel.sessionState.value.businessId,
                            staffId = finalStaffId,
                            staffName = name,
                            mobile = phone,
                            pin = pin,
                            role = if (customTitle.isNotBlank()) customTitle else role.replace("_", " "),
                            route = route
                        )
                    } else {
                        android.widget.Toast.makeText(context, "✓ $name added as farm worker (Attendance & Wages)", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // 📲 1-Tap WhatsApp Credentials Share Modal
        if (newlyAddedStaffCredentials != null) {
            StaffCredentialsShareDialog(
                data = newlyAddedStaffCredentials!!,
                onDismiss = { newlyAddedStaffCredentials = null }
            )
        }

        // 🔑 Quick Enable App Login Dialog for existing worker
        if (staffToEnableLogin != null) {
            val staff = staffToEnableLogin!!
            EnableAppLoginDialog(
                staff = staff,
                dairyName = business?.businessName ?: "MilkMate Dairy",
                businessId = business?.id ?: viewModel.sessionState.value.businessId,
                onDismiss = { staffToEnableLogin = null },
                onGranted = { updatedMobile, pin ->
                    val finalStaffId = if (staff.id.startsWith("WKR-")) {
                        "STF-${staff.id.removePrefix("WKR-")}"
                    } else staff.id
                    viewModel.saveStaff(
                        id = finalStaffId,
                        name = staff.name,
                        mobile = updatedMobile,
                        pin = pin,
                        role = staff.role,
                        permissions = if (staff.permissions == "NONE" || staff.permissions.isBlank()) "DELIVERIES" else staff.permissions,
                        assignedRoute = staff.assignedRoute,
                        monthlySalary = staff.monthlySalary,
                        dailyWage = staff.dailyWage,
                        salaryType = staff.salaryType,
                        customRoleTitle = staff.customRoleTitle
                    )
                    staffToEnableLogin = null
                    newlyAddedStaffCredentials = StaffCredentialsShareData(
                        dairyName = business?.businessName ?: "MilkMate Dairy",
                        businessId = business?.id ?: viewModel.sessionState.value.businessId,
                        staffId = finalStaffId,
                        staffName = staff.name,
                        mobile = updatedMobile,
                        pin = pin,
                        role = staff.displayRole(),
                        route = staff.assignedRoute
                    )
                }
            )
        }
    }
}

// TAB 1: STAFF DIRECTORY
@Composable
fun StaffDirectoryTab(
    staffList: List<StaffEntity>,
    business: BusinessEntity?,
    sessionBusinessId: String,
    onDeleteStaff: (String) -> Unit,
    onEnableLogin: (StaffEntity) -> Unit,
    onShareCredentials: (StaffCredentialsShareData) -> Unit
) {
    val context = LocalContext.current
    val bizId = business?.id?.ifBlank { sessionBusinessId } ?: sessionBusinessId
    val dairyName = business?.businessName ?: "MilkMate Dairy"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 🏢 Dairy Business ID & Staff Access Banner
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("staff_dairy_code_card")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = RoyalBlueLight,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Dairy Code / Business ID", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                Text(
                                    text = bizId,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RoyalBluePrimary,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Copy Button
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Dairy Business ID", bizId)
                                    clipboard?.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "Dairy Business ID copied: $bizId", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                            }

                            // WhatsApp Share Button
                            Button(
                                onClick = {
                                    val inviteText = "Hello! Here are the login details for $dairyName on MilkMate:\n\n" +
                                            "• Dairy Business ID: $bizId\n" +
                                            "• How to log in:\n" +
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
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share Code", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Text(
                        text = "Share this Dairy ID with your delivery boys and staff so they can log into the staff portal.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        if (staffList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No staff members added yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the + button below to add delivery boys, farm workers, or supervisors.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(staffList, key = { it.id }) { staff ->
                val hasAppLogin = staff.pin.isNotBlank()
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (staff.role == "SUPERVISOR" || staff.role == "MANAGER") Color(0xFF673AB7).copy(alpha = 0.15f) else RoyalBlueLight
                                ) {
                                    Icon(
                                        imageVector = if (staff.role == "DELIVERY_BOY") Icons.Default.LocalShipping else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (staff.role == "SUPERVISOR" || staff.role == "MANAGER") Color(0xFF673AB7) else RoyalBluePrimary,
                                        modifier = Modifier.padding(8.dp).size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = staff.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (staff.role == "SUPERVISOR") Color(0xFF673AB7).copy(alpha = 0.15f) else RoyalBlueLight
                                        ) {
                                            Text(
                                                text = staff.displayRole(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (staff.role == "SUPERVISOR") Color(0xFF673AB7) else RoyalBluePrimary,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }

                                        // Status badge: App Login vs Farm Worker
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (hasAppLogin) DairyGreenLight else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = if (hasAppLogin) "📱 App Login" else "👤 Farm Worker",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (hasAppLogin) DairyGreen else TextSecondary,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (hasAppLogin) {
                                        Text(
                                            text = "PIN: ${staff.pin} • ID: ${staff.id} • Perms: ${if (staff.permissions.contains("ALL")) "FULL ACCESS" else staff.permissions}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = "ID: ${staff.id} • Attendance & Wage Tracking Only",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            IconButton(onClick = { onDeleteStaff(staff.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Contact Phone", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(staff.mobile.ifBlank { "Not set" }, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }

                            Column {
                                Text("Assigned Route", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(staff.assignedRoute.ifBlank { "All Routes" }, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = RoyalBluePrimary)
                            }

                            Column {
                                Text("Base Compensation", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val comp = if (staff.salaryType == "DAILY") "₹${staff.dailyWage.toInt()}/day" else "₹${staff.monthlySalary.toInt()}/mo"
                                Text(comp, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DairyGreen)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (staff.mobile.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val uri = Uri.parse("tel:${staff.mobile}")
                                        try { context.startActivity(Intent(Intent.ACTION_DIAL, uri)) } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        val uri = Uri.parse("https://api.whatsapp.com/send?phone=91${staff.mobile}")
                                        try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) {}
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("💬 WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (hasAppLogin) {
                                FilledTonalButton(
                                    onClick = {
                                        onShareCredentials(
                                            StaffCredentialsShareData(
                                                dairyName = dairyName,
                                                businessId = bizId,
                                                staffId = staff.id,
                                                staffName = staff.name,
                                                mobile = staff.mobile,
                                                pin = staff.pin,
                                                role = staff.displayRole(),
                                                route = staff.assignedRoute
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = DairyGreenLight),
                                    modifier = Modifier.weight(1.1f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp), tint = DairyGreen)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share Login", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                                }
                            } else {
                                Button(
                                    onClick = { onEnableLogin(staff) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                    modifier = Modifier.weight(1.3f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Enable Login", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB 2: DAILY ATTENDANCE
@Composable
fun StaffAttendanceTab(
    staffList: List<StaffEntity>,
    attendanceList: List<StaffAttendanceEntity>,
    selectedDate: Long,
    onMarkAttendance: (String, String, String) -> Unit,
    onMarkAllPresent: () -> Unit,
    onChangeDate: (Long) -> Unit
) {
    val dateStr = remember(selectedDate) {
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(selectedDate))
    }

    val presentCount = attendanceList.count { it.status == "PRESENT" }
    val halfDayCount = attendanceList.count { it.status == "HALF_DAY" }
    val absentCount = attendanceList.count { it.status == "ABSENT" }
    val leaveCount = attendanceList.count { it.status == "LEAVE" }
    val totalCount = staffList.size
    val rate = if (totalCount > 0) (presentCount * 100) / totalCount else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date Selector Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onChangeDate(selectedDate - (24 * 3600 * 1000L)) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Daily Attendance Register", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dateStr, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                IconButton(onClick = { onChangeDate(selectedDate + (24 * 3600 * 1000L)) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                }
            }
        }

        // Attendance Metric Badges
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AttendancePill(label = "Present", count = presentCount, color = DairyGreen, modifier = Modifier.weight(1f))
            AttendancePill(label = "Half-Day", count = halfDayCount, color = FreshGold, modifier = Modifier.weight(1f))
            AttendancePill(label = "Absent", count = absentCount, color = DangerRed, modifier = Modifier.weight(1f))
            AttendancePill(label = "Present Rate", count = rate, color = RoyalBluePrimary, modifier = Modifier.weight(1f))
        }

        if (staffList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Add staff members in the Directory tab to begin recording attendance.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(staffList, key = { it.id }) { staff ->
                    val currentRecord = attendanceList.firstOrNull { it.staffId == staff.id }
                    val status = currentRecord?.status ?: "UNMARKED"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(staff.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = "${staff.role.replace("_", " ")} • ${staff.assignedRoute.ifBlank { "All Routes" }}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                StatusButton("P", status == "PRESENT", DairyGreen) {
                                    onMarkAttendance(staff.id, staff.name, "PRESENT")
                                }
                                StatusButton("H", status == "HALF_DAY", FreshGold) {
                                    onMarkAttendance(staff.id, staff.name, "HALF_DAY")
                                }
                                StatusButton("A", status == "ABSENT", DangerRed) {
                                    onMarkAttendance(staff.id, staff.name, "ABSENT")
                                }
                                StatusButton("L", status == "LEAVE", RoyalBluePrimary) {
                                    onMarkAttendance(staff.id, staff.name, "LEAVE")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB 3: SALARIES & ADVANCES WITH SLIPS
@Composable
fun StaffSalariesTab(
    staffList: List<StaffEntity>,
    attendanceList: List<StaffAttendanceEntity>,
    paymentsList: List<StaffPaymentEntity>,
    dairyName: String,
    onRecordPayment: (String, String, String, Double, String, String, String) -> Unit,
    onDeletePayment: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedStaffForAction by remember { mutableStateOf<StaffEntity?>(null) }
    var actionType by remember { mutableStateOf("ADVANCE") }
    var showPaymentDialog by remember { mutableStateOf(false) }

    val currentMonthYear = remember {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Monthly Wage & Advance Register", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RoyalBluePrimary)
                    Text("Wages calculated dynamically from recorded attendance, minus advances during $currentMonthYear.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Staff Salary Cards
        items(staffList, key = { it.id }) { staff ->
            val staffDaysPresent = attendanceList.count { it.staffId == staff.id && it.status == "PRESENT" }
            val staffHalfDays = attendanceList.count { it.staffId == staff.id && it.status == "HALF_DAY" }
            val totalEffectiveDays = staffDaysPresent + (staffHalfDays * 0.5)

            val baseSalary = if (staff.salaryType == "DAILY") staff.dailyWage * totalEffectiveDays else staff.monthlySalary
            val advancesTaken = paymentsList.filter { it.staffId == staff.id && it.type == "ADVANCE" }.sumOf { it.amount }
            val salaryPaid = paymentsList.filter { it.staffId == staff.id && it.type == "SALARY_PAYOUT" }.sumOf { it.amount }
            val netPayable = maxOf(0.0, baseSalary - advancesTaken - salaryPaid)

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
                        Column {
                            Text(staff.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Base: ₹${staff.monthlySalary.toInt()}/mo • ₹${staff.dailyWage.toInt()}/day", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = DairyGreenLight) {
                            Text("Days: $totalEffectiveDays", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DairyGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Gross Earned", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${baseSalary.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Column {
                            Text("Advances Taken", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${advancesTaken.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DangerRed)
                        }
                        Column {
                            Text("Already Paid", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${salaryPaid.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF64B5F6))
                        }
                        Column {
                            Text("Net Payable", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${netPayable.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DairyGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                selectedStaffForAction = staff
                                actionType = "ADVANCE"
                                showPaymentDialog = true
                            },
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("💸 Advance", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                selectedStaffForAction = staff
                                actionType = "SALARY_PAYOUT"
                                showPaymentDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("✓ Pay Salary", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = {
                                shareStaffSalarySlipToWhatsApp(
                                    context = context,
                                    dairyName = dairyName,
                                    staffName = staff.name,
                                    staffPhone = staff.mobile,
                                    monthYear = currentMonthYear,
                                    effectiveDays = totalEffectiveDays,
                                    grossSalary = baseSalary,
                                    advances = advancesTaken,
                                    netPaid = salaryPaid,
                                    paymentMode = "Cash / UPI"
                                )
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "WhatsApp Slip", tint = DairyGreen)
                        }
                    }
                }
            }
        }

        // Recent Payment History
        item {
            Text("RECENT SALARY & ADVANCE PAYMENTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (paymentsList.isEmpty()) {
            item {
                Text("No advance or salary payments recorded yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(paymentsList, key = { it.id }) { pay ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${pay.staffName} • ${pay.type.replace("_", " ")}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Mode: ${pay.paymentMode} • ${pay.notes}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("₹${pay.amount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (pay.type == "ADVANCE") DangerRed else DairyGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(onClick = { onDeletePayment(pay.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPaymentDialog && selectedStaffForAction != null) {
        val targetStaff = selectedStaffForAction!!
        var amountText by remember { mutableStateOf("") }
        var mode by remember { mutableStateOf("CASH") }
        var noteText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = {
                Text(if (actionType == "ADVANCE") "Give Advance to ${targetStaff.name}" else "Disburse Salary to ${targetStaff.name}", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick amount chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf(500, 1000, 2000, 5000)) { pAmt ->
                            SuggestionChip(
                                onClick = { amountText = pAmt.toString() },
                                label = { Text("₹$pAmt", fontSize = 10.sp) }
                            )
                        }
                    }

                    Text("Payment Mode:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("CASH", "UPI", "BANK").forEach { m ->
                            FilterChip(
                                selected = mode == m,
                                onClick = { mode = m },
                                label = { Text(m) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Notes / Purpose (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onRecordPayment(targetStaff.id, targetStaff.name, actionType, amt, mode, currentMonthYear, noteText)
                            showPaymentDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (actionType == "ADVANCE") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                ) {
                    Text("Confirm Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// TAB 5: LIVE STAFF ACTIVITY LOG
@Composable
fun StaffActivityLogTab(
    activityLogs: List<StaffActivityLogEntity>,
    staffList: List<StaffEntity>
) {
    var selectedStaffFilter by remember { mutableStateOf<String?>(null) }
    var selectedModuleFilter by remember { mutableStateOf<String?>(null) }

    val filteredLogs = remember(activityLogs, selectedStaffFilter, selectedModuleFilter) {
        activityLogs.filter { log ->
            (selectedStaffFilter == null || log.staffId == selectedStaffFilter) &&
            (selectedModuleFilter == null || log.module == selectedModuleFilter)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Filter by Team Member:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedStaffFilter == null,
                        onClick = { selectedStaffFilter = null },
                        label = { Text("All Team", fontSize = 11.sp) }
                    )
                }
                items(staffList, key = { it.id }) { staff ->
                    FilterChip(
                        selected = selectedStaffFilter == staff.id,
                        onClick = { selectedStaffFilter = if (selectedStaffFilter == staff.id) null else staff.id },
                        label = { Text(staff.name, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text("Filter by Module:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            val modules = listOf(
                "HERD_MILKING" to "🐮 Milking",
                "CATTLE_HEALTH" to "🩺 Vet & Health",
                "MILK_COLLECTION" to "🥛 Collection",
                "DELIVERIES" to "🚚 Deliveries",
                "CUSTOMERS_KHATA" to "👥 Customers",
                "PAYMENTS_EXPENSES" to "💰 Payments",
                "STAFF" to "👥 Team"
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedModuleFilter == null,
                        onClick = { selectedModuleFilter = null },
                        label = { Text("All Modules", fontSize = 11.sp) }
                    )
                }
                items(modules) { (modKey, modLabel) ->
                    FilterChip(
                        selected = selectedModuleFilter == modKey,
                        onClick = { selectedModuleFilter = if (selectedModuleFilter == modKey) null else modKey },
                        label = { Text(modLabel, fontSize = 11.sp) }
                    )
                }
            }
        }

        HorizontalDivider()

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No staff activities recorded yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "When staff members log milking, collections, deliveries or health events, their real-time activity log will appear here instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
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
                            // Staff Avatar
                            Surface(
                                shape = CircleShape,
                                color = when (log.module) {
                                    "HERD_MILKING" -> RoyalBlueLight
                                    "CATTLE_HEALTH" -> Color(0xFFE8F5E9)
                                    "MILK_COLLECTION" -> Color(0xFFFFF3E0)
                                    "DELIVERIES" -> Color(0xFFE0F7FA)
                                    "PAYMENTS_EXPENSES" -> Color(0xFFF3E5F5)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val icon = when (log.module) {
                                        "HERD_MILKING" -> Icons.Default.WaterDrop
                                        "CATTLE_HEALTH" -> Icons.Default.MedicalServices
                                        "MILK_COLLECTION" -> Icons.Default.Science
                                        "DELIVERIES" -> Icons.Default.LocalShipping
                                        "PAYMENTS_EXPENSES" -> Icons.Default.Payments
                                        else -> Icons.Default.Person
                                    }
                                    val tint = when (log.module) {
                                        "HERD_MILKING" -> RoyalBluePrimary
                                        "CATTLE_HEALTH" -> Color(0xFF2E7D32)
                                        "MILK_COLLECTION" -> Color(0xFFE65100)
                                        "DELIVERIES" -> Color(0xFF00838F)
                                        "PAYMENTS_EXPENSES" -> Color(0xFF7B1FA2)
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(log.staffName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = RoyalBlueLight
                                        ) {
                                            Text(
                                                log.module.replace("_", " "),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RoyalBluePrimary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    val timeStr = remember(log.timestamp) {
                                        val sdf = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault())
                                        sdf.format(Date(log.timestamp))
                                    }
                                    Text(timeStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = log.actionSummary,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
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
 * Data holder for newly added staff credentials
 */
data class StaffCredentialsShareData(
    val dairyName: String,
    val businessId: String,
    val staffId: String,
    val staffName: String,
    val mobile: String,
    val pin: String,
    val role: String,
    val route: String
)

/**
 * 📲 1-Tap WhatsApp Credentials Share Modal
 */
@Composable
fun StaffCredentialsShareDialog(
    data: StaffCredentialsShareData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager

    val shareText = remember(data) {
        buildString {
            appendLine("Hello ${data.staffName}!")
            appendLine("You have been added to ${data.dairyName} on MilkMate.")
            appendLine()
            appendLine("Here are your staff login credentials:")
            appendLine("• Dairy Business ID: ${data.businessId}")
            appendLine("• Login ID / Mobile: ${if (data.mobile.isNotBlank()) data.mobile else data.staffId}")
            appendLine("• 4-Digit Security PIN: ${data.pin}")
            appendLine("• Assigned Role: ${data.role}")
            if (data.route.isNotBlank()) {
                appendLine("• Assigned Route: ${data.route}")
            }
            appendLine()
            appendLine("How to log in:")
            appendLine("1. Open the MilkMate App")
            appendLine("2. Tap 'Quick Log In' -> 'Staff Login'")
            append("3. Enter the Dairy Business ID, your mobile number, and PIN above to begin.")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = DairyGreenLight,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Staff Member Added!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Share login credentials with staff", fontSize = 11.5.sp, color = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Credential Summary Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Staff Name:", fontSize = 12.sp, color = TextSecondary)
                            Text(data.staffName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Dairy Business ID:", fontSize = 12.sp, color = TextSecondary)
                            Text(data.businessId, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = RoyalBluePrimary)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Login Mobile / ID:", fontSize = 12.sp, color = TextSecondary)
                            Text(if (data.mobile.isNotBlank()) data.mobile else data.staffId, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("4-Digit Security PIN:", fontSize = 12.sp, color = TextSecondary)
                            Text(data.pin, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = DairyGreen)
                        }
                        if (data.route.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Assigned Route:", fontSize = 12.sp, color = TextSecondary)
                                Text(data.route, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = TextPrimary)
                            }
                        }
                    }
                }

                // WhatsApp Send Button (Direct to staff member's mobile!)
                Button(
                    onClick = {
                        val cleanMobile = data.mobile.filter { it.isDigit() }
                        val url = if (cleanMobile.length >= 10) {
                            val finalNumber = if (cleanMobile.length == 10) "91$cleanMobile" else cleanMobile
                            "https://api.whatsapp.com/send?phone=$finalNumber&text=" + Uri.encode(shareText)
                        } else {
                            "https://api.whatsapp.com/send?text=" + Uri.encode(shareText)
                        }
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Login Credentials"))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("share_credentials_whatsapp_btn")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send to Staff on WhatsApp", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }

                // Copy Button
                OutlinedButton(
                    onClick = {
                        val clip = android.content.ClipData.newPlainText("Staff Credentials", shareText)
                        clipboard?.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Credentials copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Credentials Text", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    )
}

/**
 * 🔑 Quick Modal to enable app login credentials for an existing farm worker
 */
@Composable
fun EnableAppLoginDialog(
    staff: StaffEntity,
    dairyName: String,
    businessId: String,
    onDismiss: () -> Unit,
    onGranted: (mobile: String, pin: String) -> Unit
) {
    var mobile by remember { mutableStateOf(staff.mobile) }
    var pin by remember { mutableStateOf((1000..9999).random().toString()) }

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
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Grant App Login Access", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("For ${staff.name} (${staff.displayRole()})", fontSize = 11.5.sp, color = TextSecondary)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Create a smartphone login for this worker so they can log into MilkMate.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number (Login ID) *") },
                    placeholder = { Text("e.g. 9876543210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("enable_login_mobile_input")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                        label = { Text("4-Digit PIN *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("enable_login_pin_input")
                    )

                    Button(
                        onClick = { pin = (1000..9999).random().toString() },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Auto PIN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("• Dairy Business ID: $businessId", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RoyalBluePrimary)
                        Text("• Login ID / Mobile: ${mobile.ifBlank { "(Mobile required)" }}", fontSize = 11.sp, color = TextSecondary)
                        Text("• Security PIN: $pin", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (mobile.isNotBlank() && pin.length >= 4) {
                        onGranted(mobile.trim(), pin.trim())
                    }
                },
                enabled = mobile.isNotBlank() && pin.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                modifier = Modifier.testTag("confirm_enable_login_btn")
            ) {
                Text("Grant & Share on WhatsApp", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

