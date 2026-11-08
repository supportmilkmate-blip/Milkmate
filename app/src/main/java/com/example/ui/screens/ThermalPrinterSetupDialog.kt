package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BluetoothHardwareManager
import com.example.ui.util.EscPosThermalPrinter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalPrinterSetupDialog(
    viewModel: MilkMateViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()

    var hasPermission by remember { mutableStateOf(BluetoothHardwareManager.hasBluetoothPermissions(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPermission = perms.values.all { it }
    }

    var selectedDeviceAddress by remember { mutableStateOf(session.printerDeviceAddress) }
    var selectedDeviceName by remember { mutableStateOf(session.printerDeviceName) }
    var selectedPaperWidth by remember { mutableStateOf(session.printerPaperWidth.ifBlank { "58MM" }) }
    var autoPrintEnabled by remember { mutableStateOf(session.printerAutoPrint) }
    var copiesCount by remember { mutableStateOf(session.printerCopies.coerceIn(1, 2)) }

    var isPrintingTest by remember { mutableStateOf(false) }
    var printStatusMessage by remember { mutableStateOf<String?>(null) }
    var isPrintSuccess by remember { mutableStateOf(false) }
    var showPreviewSheet by remember { mutableStateOf(false) }

    var pairedDevices by remember {
        mutableStateOf(if (hasPermission) BluetoothHardwareManager.getPairedDevices(context) else emptyList())
    }

    fun refreshDevices() {
        if (BluetoothHardwareManager.hasBluetoothPermissions(context)) {
            hasPermission = true
            pairedDevices = BluetoothHardwareManager.getPairedDevices(context)
        }
    }

    val isBluetoothOn = remember(hasPermission) { BluetoothHardwareManager.isBluetoothEnabled() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RoyalBlueLight,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Print, contentDescription = null, tint = RoyalBluePrimary)
                            }
                        }
                        Column {
                            Text("Thermal Slip Printer", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("ESC/POS Bluetooth 2\" (58mm) & 3\" (80mm)", fontSize = 11.5.sp, color = TextSecondary)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Permissions or Bluetooth Warnings
                if (!hasPermission) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DangerRedLight,
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                                Text("Bluetooth Permission Required", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DangerRed)
                            }
                            Text("Allow Nearby Devices permission to connect to your Bluetooth thermal printer.", fontSize = 11.sp, color = TextPrimary)
                            Button(
                                onClick = { permissionLauncher.launch(BluetoothHardwareManager.getRequiredPermissions()) },
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Grant Permission", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else if (!isBluetoothOn) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.BluetoothDisabled, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                Text("Bluetooth is turned OFF", fontSize = 11.5.sp, color = Color(0xFF92400E), fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                    } catch (_: Exception) {}
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Turn On", fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Scrollable Body
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Active Selected Printer Card
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedDeviceAddress.isNotBlank()) DairyGreenLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, if (selectedDeviceAddress.isNotBlank()) DairyGreen else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("CONFIGURED PRINTER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selectedDeviceAddress.isNotBlank()) DairyGreen else TextSecondary)
                                    if (selectedDeviceAddress.isNotBlank()) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = DairyGreen) {
                                            Text(
                                                text = if (autoPrintEnabled) "AUTO-PRINT ON" else "READY",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                if (selectedDeviceAddress.isNotBlank()) {
                                    Text(
                                        text = selectedDeviceName.ifBlank { "Bluetooth ESC/POS Printer" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                    Text("MAC: $selectedDeviceAddress • Width: $selectedPaperWidth • $copiesCount copy", fontSize = 11.sp, color = TextSecondary)
                                } else {
                                    Text("No printer configured", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                                    Text("Select your paired 2\" or 3\" thermal slip printer from the list below.", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }

                    // Paired Devices List
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("PAIRED PRINTERS & BLUETOOTH DEVICES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            TextButton(
                                onClick = { refreshDevices() },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Refresh", fontSize = 11.sp)
                            }
                        }
                    }

                    if (pairedDevices.isEmpty() && hasPermission) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.PrintDisabled, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(28.dp))
                                    Text("No Paired Printers Found", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = TextPrimary)
                                    Text("Turn ON your thermal printer, pair it via phone Bluetooth settings (PIN 0000 or 1234), and tap Refresh.", fontSize = 11.sp, color = TextSecondary)
                                    Button(
                                        onClick = {
                                            try {
                                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                            } catch (_: Exception) {}
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Open Bluetooth Settings", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        items(pairedDevices) { dev ->
                            val isSelected = selectedDeviceAddress == dev.address
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) RoyalBlueLight else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) RoyalBluePrimary else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedDeviceAddress = dev.address
                                        selectedDeviceName = dev.name
                                        printStatusMessage = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Print,
                                            contentDescription = null,
                                            tint = if (isSelected) RoyalBluePrimary else TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(dev.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                            Text(dev.address, fontSize = 10.5.sp, color = TextSecondary)
                                        }
                                    }
                                    if (isSelected) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = RoyalBluePrimary) {
                                            Text("SELECTED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Print Format & Preferences
                    item {
                        Text("PRINTING FORMAT & AUTOMATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))

                        // Paper Width
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Paper Width", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = TextPrimary)
                                        Text("Select roll size matching your printer", fontSize = 10.5.sp, color = TextSecondary)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("58MM" to "2\" (58mm)", "80MM" to "3\" (80mm)").forEach { (widthVal, label) ->
                                            val isChosen = selectedPaperWidth == widthVal
                                            FilterChip(
                                                selected = isChosen,
                                                onClick = { selectedPaperWidth = widthVal },
                                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal) }
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider()

                                // Auto Print Switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Auto-Print on Milk Intake", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = TextPrimary)
                                        Text("Instantly print receipt when logging farmer milk", fontSize = 10.5.sp, color = TextSecondary)
                                    }
                                    Switch(
                                        checked = autoPrintEnabled,
                                        onCheckedChange = { autoPrintEnabled = it }
                                    )
                                }

                                HorizontalDivider()

                                // Copies selector
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Receipt Copies", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = TextPrimary)
                                        Text("Number of duplicate slips per intake", fontSize = 10.5.sp, color = TextSecondary)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(1 to "1 Copy", 2 to "2 Copies (Farmer + Dairy)").forEach { (cnt, label) ->
                                            val isChosen = copiesCount == cnt
                                            FilterChip(
                                                selected = isChosen,
                                                onClick = { copiesCount = cnt },
                                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Test Print & Hardware Verification
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("HARDWARE VERIFICATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (selectedDeviceAddress.isBlank()) {
                                                printStatusMessage = "Please select a paired printer above first."
                                                isPrintSuccess = false
                                                return@Button
                                            }
                                            isPrintingTest = true
                                            printStatusMessage = null
                                            // Temporarily save selected printer so printTestSlip uses it
                                            viewModel.updatePrinterSettings(
                                                address = selectedDeviceAddress,
                                                name = selectedDeviceName,
                                                paperWidth = selectedPaperWidth,
                                                autoPrint = autoPrintEnabled,
                                                copies = copiesCount
                                            )
                                            viewModel.printTestSlip(context, selectedPaperWidth) { success, msg ->
                                                isPrintingTest = false
                                                isPrintSuccess = success
                                                printStatusMessage = msg
                                            }
                                        },
                                        enabled = !isPrintingTest,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                        modifier = Modifier.weight(1.2f).height(38.dp)
                                    ) {
                                        if (isPrintingTest) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Sending...", fontSize = 11.sp)
                                        } else {
                                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Print Test Slip Now", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { showPreviewSheet = !showPreviewSheet },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(0.8f).height(38.dp)
                                    ) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (showPreviewSheet) "Hide Slip" else "Preview", fontSize = 11.5.sp)
                                    }
                                }

                                if (printStatusMessage != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isPrintSuccess) DairyGreenLight else DangerRedLight,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPrintSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = if (isPrintSuccess) DairyGreen else DangerRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(printStatusMessage ?: "", fontSize = 11.sp, color = if (isPrintSuccess) DairyGreen else DangerRed)
                                        }
                                    }
                                }

                                // Interactive Slip Preview
                                if (showPreviewSheet) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color.LightGray),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "PRINTER SIMULATION (Width: $selectedPaperWidth)",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Gray
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val previewText = remember(selectedPaperWidth, business) {
                                                val dummyEntry = com.example.data.local.entity.MilkCollectionEntity(
                                                    id = "SLP-100234",
                                                    businessId = business?.id ?: "biz",
                                                    farmerId = "F-101",
                                                    farmerCode = "F-101",
                                                    farmerName = "Ramesh Sharma",
                                                    dateEpochMidnight = System.currentTimeMillis(),
                                                    milkType = "COW",
                                                    quantityLiters = 15.0,
                                                    fat = 4.20,
                                                    snf = 8.50,
                                                    clr = 28.0,
                                                    ratePerLiter = 58.0,
                                                    totalAmount = 870.0,
                                                    shift = "MORNING",
                                                    timestamp = System.currentTimeMillis()
                                                )
                                                EscPosThermalPrinter.formatSlipText(
                                                    dairyName = business?.businessName ?: "MILKMATE DAIRY",
                                                    entry = dummyEntry,
                                                    farmer = null,
                                                    paperWidth = selectedPaperWidth
                                                )
                                            }
                                            Text(
                                                text = previewText,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = if (selectedPaperWidth == "80MM") 9.sp else 10.sp,
                                                lineHeight = 13.sp,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedDeviceAddress.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                selectedDeviceAddress = ""
                                selectedDeviceName = ""
                                viewModel.updatePrinterSettings("", "", "58MM", false, 1)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.7f).height(44.dp)
                        ) {
                            Text("Disconnect", color = DangerRed, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.updatePrinterSettings(
                                address = selectedDeviceAddress,
                                name = selectedDeviceName,
                                paperWidth = selectedPaperWidth,
                                autoPrint = autoPrintEnabled,
                                copies = copiesCount
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Printer", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
