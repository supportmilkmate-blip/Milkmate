package com.example.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.AnalyzerSampleData
import com.example.ui.util.BluetoothDeviceItem
import com.example.ui.util.BluetoothHardwareManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothMilkAnalyzerDialog(
    viewModel: MilkMateViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val session by viewModel.sessionState.collectAsStateWithLifecycle()

    var hasPermission by remember { mutableStateOf(BluetoothHardwareManager.hasBluetoothPermissions(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPermission = perms.values.all { it }
    }

    var selectedDeviceAddress by remember { mutableStateOf(session.analyzerDeviceAddress) }
    var selectedDeviceName by remember { mutableStateOf(session.analyzerDeviceName) }
    var selectedProtocol by remember { mutableStateOf(session.analyzerProtocol.ifBlank { "AUTO" }) }

    var isConnecting by remember { mutableStateOf(false) }
    var testResultSample by remember { mutableStateOf<AnalyzerSampleData?>(null) }
    var connectionError by remember { mutableStateOf<String?>(null) }

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
                .fillMaxHeight(0.90f)
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
                                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = RoyalBluePrimary)
                            }
                        }
                        Column {
                            Text("Milk Analyzer & Scale", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Auto-capture FAT, SNF, CLR & Weight", fontSize = 11.5.sp, color = TextSecondary)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bluetooth Permission & Adapter Status Banner
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
                            Text(
                                "Allow Nearby Devices permission so MilkMate can discover your paired analyzer and weighing scale.",
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                            Button(
                                onClick = {
                                    permissionLauncher.launch(BluetoothHardwareManager.getRequiredPermissions())
                                },
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
                                Text("Bluetooth is turned OFF on this phone", fontSize = 11.5.sp, color = Color(0xFF92400E), fontWeight = FontWeight.SemiBold)
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

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Current Configuration Card
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
                                    Text("CURRENT ACTIVE DEVICE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selectedDeviceAddress.isNotBlank()) DairyGreen else TextSecondary)
                                    if (selectedDeviceAddress.isNotBlank()) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = DairyGreen) {
                                            Text("CONFIGURED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                if (selectedDeviceAddress.isNotBlank()) {
                                    Text(
                                        text = selectedDeviceName.ifBlank { "Bluetooth Milk Analyzer" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                    Text("MAC: $selectedDeviceAddress • Protocol: $selectedProtocol", fontSize = 11.sp, color = TextSecondary)
                                } else {
                                    Text("No analyzer device selected yet", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                                    Text("Pick your paired Ekomilk, Lactoscan, Essae or Weighing Scale below.", fontSize = 11.sp, color = TextSecondary)
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
                            Text("PAIRED BLUETOOTH DEVICES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
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
                                    Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(28.dp))
                                    Text("No Paired Bluetooth Devices Found", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = TextPrimary)
                                    Text("Pair your Milk Analyzer in Android Bluetooth Settings first, then tap Refresh.", fontSize = 11.sp, color = TextSecondary)
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
                                        testResultSample = null
                                        connectionError = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Bluetooth,
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

                    // Protocol Selector
                    item {
                        Text("ANALYZER / SCALE PROTOCOL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "AUTO" to "Auto-Detect Standard Formats (Recommended)",
                                "EKOMILK" to "Ekomilk / Milkotester RS232 Format",
                                "LACTOSCAN" to "Lactoscan Ultrasonic Analyzer Format",
                                "ESSAE" to "Essae / REIL CSV Serial Protocol",
                                "SCALE_ASCII" to "Digital Platform Weighing Scale (ASCII Kg)"
                            ).forEach { (proto, desc) ->
                                val isChosen = selectedProtocol == proto
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isChosen) FreshGoldLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    border = BorderStroke(1.dp, if (isChosen) FreshGold else Color.Transparent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedProtocol = proto }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        RadioButton(
                                            selected = isChosen,
                                            onClick = { selectedProtocol = proto },
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(proto, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                            Text(desc, fontSize = 10.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Live Connection & Test Area
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("HARDWARE VERIFICATION & SAMPLE TEST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Live Read Button
                                    Button(
                                        onClick = {
                                            if (selectedDeviceAddress.isBlank()) {
                                                connectionError = "Select a paired device above first."
                                                return@Button
                                            }
                                            isConnecting = true
                                            connectionError = null
                                            coroutineScope.launch {
                                                val res = BluetoothHardwareManager.readLiveAnalyzerData(
                                                    context = context,
                                                    deviceAddress = selectedDeviceAddress,
                                                    protocol = selectedProtocol,
                                                    timeoutMs = 6000
                                                )
                                                isConnecting = false
                                                if (res.isSuccess) {
                                                    testResultSample = res.getOrNull()
                                                } else {
                                                    connectionError = res.exceptionOrNull()?.localizedMessage
                                                }
                                            }
                                        },
                                        enabled = !isConnecting,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        if (isConnecting) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Connecting...", fontSize = 11.sp)
                                        } else {
                                            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Live Read Test", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Simulation Button
                                    OutlinedButton(
                                        onClick = {
                                            connectionError = null
                                            testResultSample = BluetoothHardwareManager.generateSimulatedSample()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Simulate Sample", fontSize = 11.5.sp)
                                    }
                                }

                                if (connectionError != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DangerRedLight,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                                            Text(connectionError ?: "", fontSize = 11.sp, color = DangerRed)
                                        }
                                    }
                                }

                                // Parsed Sample Card
                                testResultSample?.let { sample ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = DairyGreenLight,
                                        border = BorderStroke(1.dp, DairyGreen),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("✓ CAPTURED SAMPLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                                                Text("Verified OK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceAround
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("FAT", fontSize = 10.sp, color = TextSecondary)
                                                    Text("${sample.fat}%", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = TextPrimary)
                                                }
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("SNF", fontSize = 10.sp, color = TextSecondary)
                                                    Text("${sample.snf}%", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = TextPrimary)
                                                }
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("CLR", fontSize = 10.sp, color = TextSecondary)
                                                    Text("${sample.clr}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = TextPrimary)
                                                }
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("WEIGHT", fontSize = 10.sp, color = TextSecondary)
                                                    Text("${sample.weightKg} L", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = TextPrimary)
                                                }
                                            }
                                            Text(
                                                text = "Raw Serial: ${sample.rawData}",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedDeviceAddress.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                selectedDeviceAddress = ""
                                selectedDeviceName = ""
                                viewModel.updateAnalyzerSettings("", "", "AUTO")
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
                            viewModel.updateAnalyzerSettings(
                                address = selectedDeviceAddress,
                                name = selectedDeviceName,
                                protocol = selectedProtocol
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Analyzer", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
