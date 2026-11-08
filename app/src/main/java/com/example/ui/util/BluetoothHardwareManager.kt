package com.example.ui.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

data class BluetoothDeviceItem(
    val name: String,
    val address: String,
    val isPaired: Boolean = true,
    val majorClass: String = "GENERIC"
)

data class AnalyzerSampleData(
    val fat: Double,
    val snf: Double,
    val clr: Double,
    val weightKg: Double,
    val rawData: String,
    val timestamp: Long = System.currentTimeMillis()
)

object BluetoothHardwareManager {
    // Standard SPP (Serial Port Profile) UUID used by all Bluetooth Milk Analyzers, Scales & ESC/POS Printers
    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun isBluetoothSupported(): Boolean {
        return BluetoothAdapter.getDefaultAdapter() != null
    }

    fun isBluetoothEnabled(): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
        return adapter.isEnabled
    }

    fun hasBluetoothPermissions(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connectGranted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            return connectGranted
        }
        return true
    }

    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(context: Context): List<BluetoothDeviceItem> {
        if (!hasBluetoothPermissions(context)) return emptyList()
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return try {
            val bonded = adapter.bondedDevices ?: return emptyList()
            bonded.map { device ->
                val name = try { device.name ?: "Unknown Device" } catch (_: Exception) { "Unknown Device" }
                val address = device.address
                val majorClass = when (device.bluetoothClass?.majorDeviceClass) {
                    android.bluetooth.BluetoothClass.Device.Major.IMAGING -> "PRINTER"
                    android.bluetooth.BluetoothClass.Device.Major.UNCATEGORIZED -> "SERIAL/SCALE"
                    else -> "DEVICE"
                }
                BluetoothDeviceItem(
                    name = name,
                    address = address,
                    isPaired = true,
                    majorClass = majorClass
                )
            }.sortedBy { it.name.lowercase(Locale.ROOT) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Attempts a real RFCOMM SPP connection to read live data from a Bluetooth Milk Analyzer or Scale.
     */
    @SuppressLint("MissingPermission")
    suspend fun readLiveAnalyzerData(
        context: Context,
        deviceAddress: String,
        protocol: String = "AUTO",
        timeoutMs: Long = 6000
    ): Result<AnalyzerSampleData> = withContext(Dispatchers.IO) {
        if (!hasBluetoothPermissions(context)) {
            return@withContext Result.failure(Exception("Bluetooth permission not granted. Please allow Nearby devices permission."))
        }
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: return@withContext Result.failure(Exception("Bluetooth is not supported on this device."))
        if (!adapter.isEnabled) {
            return@withContext Result.failure(Exception("Bluetooth is turned OFF. Please enable Bluetooth."))
        }
        if (deviceAddress.isBlank()) {
            return@withContext Result.failure(Exception("No Milk Analyzer device selected. Please pick a paired device."))
        }

        var socket: BluetoothSocket? = null
        try {
            val device = adapter.getRemoteDevice(deviceAddress)
            // Cancel discovery to avoid slowing down connection
            try { adapter.cancelDiscovery() } catch (_: Exception) {}

            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()

            val inputStream: InputStream = socket.inputStream
            val buffer = ByteArray(1024)
            val startTime = System.currentTimeMillis()
            val receivedBuilder = StringBuilder()

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (inputStream.available() > 0) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val chunk = String(buffer, 0, bytesRead)
                        receivedBuilder.append(chunk)

                        val parsed = parseAnalyzerString(receivedBuilder.toString(), protocol)
                        if (parsed != null && (parsed.fat > 0 || parsed.weightKg > 0)) {
                            return@withContext Result.success(parsed)
                        }
                    }
                } else {
                    kotlinx.coroutines.delay(100)
                }
            }

            // Fallback parse on what was gathered
            val parsed = parseAnalyzerString(receivedBuilder.toString(), protocol)
            if (parsed != null) {
                Result.success(parsed)
            } else if (receivedBuilder.isNotEmpty()) {
                Result.failure(Exception("Received raw data but failed to parse: $receivedBuilder"))
            } else {
                Result.failure(Exception("Connected to analyzer, but no sample data received within timeout. Make sure analyzer test cycle completed or push 'SEND' on machine."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.localizedMessage ?: "Device unreachable. Ensure it is powered on and in range."}"))
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Parses RS232 / Bluetooth ASCII output strings from various Milk Analyzers and Weighing Scales.
     */
    fun parseAnalyzerString(raw: String, protocol: String = "AUTO"): AnalyzerSampleData? {
        if (raw.isBlank()) return null

        var fat = 0.0
        var snf = 0.0
        var clr = 0.0
        var weight = 0.0

        // 1. FAT Regex
        val fatMatcher = Pattern.compile("(?i)(?:FAT|F|FT)[:=\\s]*([0-9]+\\.?[0-9]*)").matcher(raw)
        if (fatMatcher.find()) {
            fat = fatMatcher.group(1)?.toDoubleOrNull() ?: 0.0
        }

        // 2. SNF Regex
        val snfMatcher = Pattern.compile("(?i)(?:SNF|S|NF)[:=\\s]*([0-9]+\\.?[0-9]*)").matcher(raw)
        if (snfMatcher.find()) {
            snf = snfMatcher.group(1)?.toDoubleOrNull() ?: 0.0
        }

        // 3. CLR Regex
        val clrMatcher = Pattern.compile("(?i)(?:CLR|C|LR|DEN)[:=\\s]*([0-9]+\\.?[0-9]*)").matcher(raw)
        if (clrMatcher.find()) {
            clr = clrMatcher.group(1)?.toDoubleOrNull() ?: 0.0
        }

        // 4. Weight / Quantity Regex
        val wtMatcher = Pattern.compile("(?i)(?:WT|WEIGHT|W|QTY|QUANTITY|WT)[:=\\s+]*([0-9]+\\.?[0-9]*)").matcher(raw)
        if (wtMatcher.find()) {
            weight = wtMatcher.group(1)?.toDoubleOrNull() ?: 0.0
        }

        // If regex didn't match and it's comma-separated (e.g. Essae / REIL CSV: 4.20,8.50,28.0,25.4)
        if (fat == 0.0 && raw.contains(",")) {
            val parts = raw.split(",").mapNotNull { it.trim().toDoubleOrNull() }
            if (parts.size >= 2) {
                fat = parts[0]
                snf = parts[1]
                if (parts.size >= 3) clr = parts[2]
                if (parts.size >= 4) weight = parts[3]
            }
        }

        // If weight scale standard format (e.g. ST,GS,+  25.40kg or WT: 25.4)
        if (weight == 0.0) {
            val scaleMatcher = Pattern.compile("(?i)(?:\\+|\\s|^)([0-9]+\\.[0-9]{1,3})\\s*(?:kg|ltr|l)?").matcher(raw)
            if (scaleMatcher.find()) {
                val candidate = scaleMatcher.group(1)?.toDoubleOrNull() ?: 0.0
                if (candidate in 0.1..200.0) {
                    weight = candidate
                }
            }
        }

        // Auto calculate CLR if 0 and FAT & SNF are present
        if (clr == 0.0 && fat > 0 && snf > 0) {
            // Formula: SNF = 0.25 * CLR + 0.2 * FAT + 0.36  => CLR = (SNF - 0.2 * FAT - 0.36) / 0.25
            clr = String.format(Locale.US, "%.1f", ((snf - (0.2 * fat) - 0.36) / 0.25).coerceIn(20.0, 36.0)).toDoubleOrNull() ?: 28.0
        }

        if (fat > 0 || snf > 0 || weight > 0) {
            return AnalyzerSampleData(
                fat = String.format(Locale.US, "%.2f", fat).toDoubleOrNull() ?: fat,
                snf = String.format(Locale.US, "%.2f", snf).toDoubleOrNull() ?: snf,
                clr = String.format(Locale.US, "%.1f", clr).toDoubleOrNull() ?: clr,
                weightKg = String.format(Locale.US, "%.2f", weight).toDoubleOrNull() ?: weight,
                rawData = raw.trim()
            )
        }

        return null
    }

    /**
     * Generates a realistic simulated test sample for demonstration, laboratory verification,
     * or when physical hardware is not broadcasting during testing.
     */
    fun generateSimulatedSample(milkType: String = "COW"): AnalyzerSampleData {
        val isCow = milkType.uppercase() == "COW"
        val fat = if (isCow) (3.8 + (Math.random() * 0.8)) else (6.2 + (Math.random() * 1.5))
        val snf = if (isCow) (8.3 + (Math.random() * 0.6)) else (8.8 + (Math.random() * 0.7))
        val clr = if (isCow) (27.0 + (Math.random() * 2.0)) else (29.0 + (Math.random() * 2.5))
        val weight = (10.0 + (Math.random() * 20.0))

        val f = String.format(Locale.US, "%.2f", fat).toDouble()
        val s = String.format(Locale.US, "%.2f", snf).toDouble()
        val c = String.format(Locale.US, "%.1f", clr).toDouble()
        val w = String.format(Locale.US, "%.1f", weight).toDouble()

        val raw = "EKOMILK_BT>> FAT:${f}% SNF:${s}% CLR:${c} WT:${w}kg STATUS:OK"
        return AnalyzerSampleData(fat = f, snf = s, clr = c, weightKg = w, rawData = raw)
    }
}
