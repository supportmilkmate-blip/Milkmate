package com.example.ui.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.data.local.entity.FarmerEntity
import com.example.data.local.entity.MilkCollectionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EscPosThermalPrinter {

    // ESC/POS Command Constants
    private val ESC: Byte = 0x1B
    private val GS: Byte = 0x1D
    private val LF: Byte = 0x0A

    // Commands
    private val CMD_INIT = byteArrayOf(ESC, 0x40) // ESC @
    private val CMD_ALIGN_LEFT = byteArrayOf(ESC, 0x61, 0x00) // ESC a 0
    private val CMD_ALIGN_CENTER = byteArrayOf(ESC, 0x61, 0x01) // ESC a 1
    private val CMD_ALIGN_RIGHT = byteArrayOf(ESC, 0x61, 0x02) // ESC a 2
    private val CMD_BOLD_ON = byteArrayOf(ESC, 0x45, 0x01) // ESC E 1
    private val CMD_BOLD_OFF = byteArrayOf(ESC, 0x45, 0x00) // ESC E 0
    private val CMD_DOUBLE_SIZE = byteArrayOf(GS, 0x21, 0x11) // GS ! 0x11
    private val CMD_DOUBLE_HEIGHT = byteArrayOf(GS, 0x21, 0x01)
    private val CMD_NORMAL_SIZE = byteArrayOf(GS, 0x21, 0x00)
    private val CMD_FEED_AND_CUT = byteArrayOf(GS, 0x56, 0x41, 0x10) // GS V 65 16

    private fun getLineWidth(paperWidth: String): Int {
        return if (paperWidth.equals("80MM", ignoreCase = true)) 48 else 32
    }

    private fun getDivider(paperWidth: String): String {
        val width = getLineWidth(paperWidth)
        return "-".repeat(width)
    }

    private fun getDoubleDivider(paperWidth: String): String {
        val width = getLineWidth(paperWidth)
        return "=".repeat(width)
    }

    private fun padRow(left: String, right: String, paperWidth: String): String {
        val maxLen = getLineWidth(paperWidth)
        val l = left.trim()
        val r = right.trim()
        val spaceNeeded = maxLen - l.length - r.length
        return if (spaceNeeded > 0) {
            l + " ".repeat(spaceNeeded) + r
        } else {
            "$l $r".take(maxLen)
        }
    }

    private fun centerText(text: String, paperWidth: String): String {
        val maxLen = getLineWidth(paperWidth)
        val t = text.trim()
        if (t.length >= maxLen) return t.take(maxLen)
        val pad = (maxLen - t.length) / 2
        return " ".repeat(pad) + t
    }

    /**
     * Builds ESC/POS byte array for a printer test slip.
     */
    fun buildTestSlipBytes(
        dairyName: String,
        paperWidth: String = "58MM"
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val now = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault()).format(Date())

        out.write(CMD_INIT)
        out.write(CMD_ALIGN_CENTER)
        out.write(CMD_DOUBLE_SIZE)
        out.write(CMD_BOLD_ON)
        out.write("${dairyName.ifBlank { "MILKMATE DAIRY" }}\n".toByteArray())
        out.write(CMD_NORMAL_SIZE)
        out.write(CMD_BOLD_OFF)
        out.write("THERMAL SLIP PRINTER TEST\n".toByteArray())
        out.write("SPP Bluetooth 2.0 / 4.0\n".toByteArray())
        out.write("${getDivider(paperWidth)}\n".toByteArray())

        out.write(CMD_ALIGN_LEFT)
        out.write("${padRow("Date/Time:", now, paperWidth)}\n".toByteArray())
        out.write("${padRow("Paper Width:", if (paperWidth == "80MM") "80mm (3-Inch)" else "58mm (2-Inch)", paperWidth)}\n".toByteArray())
        out.write("${padRow("Print Status:", "CONNECTED / OK", paperWidth)}\n".toByteArray())
        out.write("${getDivider(paperWidth)}\n".toByteArray())

        out.write(CMD_ALIGN_CENTER)
        out.write(CMD_BOLD_ON)
        out.write("SAMPLE INTAKE SLIP\n".toByteArray())
        out.write(CMD_BOLD_OFF)
        out.write(CMD_ALIGN_LEFT)
        out.write("${padRow("Farmer:", "F-101 Ramesh Sharma", paperWidth)}\n".toByteArray())
        out.write("${padRow("Milk Type:", "COW MILK (Morning)", paperWidth)}\n".toByteArray())
        out.write("${padRow("Quantity:", "15.0 Liters", paperWidth)}\n".toByteArray())
        out.write("${padRow("FAT % / SNF %:", "4.20% / 8.50%", paperWidth)}\n".toByteArray())
        out.write("${padRow("CLR Reading:", "28.0", paperWidth)}\n".toByteArray())
        out.write("${padRow("Rate per Liter:", "Rs. 58.00 / L", paperWidth)}\n".toByteArray())
        out.write("${getDoubleDivider(paperWidth)}\n".toByteArray())

        out.write(CMD_BOLD_ON)
        out.write("${padRow("TOTAL AMOUNT:", "Rs. 870.00", paperWidth)}\n".toByteArray())
        out.write(CMD_BOLD_OFF)
        out.write("${getDoubleDivider(paperWidth)}\n".toByteArray())

        out.write(CMD_ALIGN_CENTER)
        out.write("Thank you for choosing pure milk!\n".toByteArray())
        out.write("Powered by MilkMate App\n\n\n\n".toByteArray())
        out.write(CMD_FEED_AND_CUT)

        return out.toByteArray()
    }

    /**
     * Builds ESC/POS byte array for a farmer milk collection intake slip.
     */
    fun buildFarmerIntakeSlipBytes(
        dairyName: String,
        entry: MilkCollectionEntity,
        farmer: FarmerEntity?,
        paperWidth: String = "58MM",
        copyTitle: String = "FARMER COPY"
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(entry.timestamp))

        out.write(CMD_INIT)
        out.write(CMD_ALIGN_CENTER)
        out.write(CMD_DOUBLE_SIZE)
        out.write(CMD_BOLD_ON)
        out.write("${dairyName.ifBlank { "MILK COLLECTION CENTER" }}\n".toByteArray())
        out.write(CMD_NORMAL_SIZE)
        out.write(CMD_BOLD_OFF)
        out.write("DAIRY MILK INTAKE SLIP\n".toByteArray())
        out.write("[ $copyTitle ]\n".toByteArray())
        out.write("${getDivider(paperWidth)}\n".toByteArray())

        out.write(CMD_ALIGN_LEFT)
        val slipNo = "SLIP #${entry.id.takeLast(6).uppercase(Locale.ROOT)}"
        out.write("${padRow(slipNo, entry.shift, paperWidth)}\n".toByteArray())
        out.write("${padRow("Date:", dateStr, paperWidth)}\n".toByteArray())
        out.write("${getDivider(paperWidth)}\n".toByteArray())

        val farmerCode = if (farmer?.farmerCode.isNullOrBlank()) "" else "[${farmer?.farmerCode}] "
        out.write(CMD_BOLD_ON)
        out.write("Farmer: $farmerCode${entry.farmerName}\n".toByteArray())
        out.write(CMD_BOLD_OFF)
        if (!farmer?.mobile.isNullOrBlank()) {
            out.write("Mobile: ${farmer?.mobile}\n".toByteArray())
        }
        out.write("${getDivider(paperWidth)}\n".toByteArray())

        out.write("${padRow("Milk Category:", entry.milkType + " MILK", paperWidth)}\n".toByteArray())
        out.write(CMD_BOLD_ON)
        out.write("${padRow("Quantity (Weight):", String.format(Locale.US, "%.1f L", entry.quantityLiters), paperWidth)}\n".toByteArray())
        out.write(CMD_BOLD_OFF)
        out.write("${padRow("FAT % / SNF %:", String.format(Locale.US, "%.2f%% / %.2f%%", entry.fat, entry.snf), paperWidth)}\n".toByteArray())
        if (entry.clr > 0) {
            out.write("${padRow("CLR Lactometer:", String.format(Locale.US, "%.1f", entry.clr), paperWidth)}\n".toByteArray())
        }
        out.write("${padRow("Applied Rate/L:", String.format(Locale.US, "Rs. %.2f", entry.ratePerLiter), paperWidth)}\n".toByteArray())
        out.write("${getDoubleDivider(paperWidth)}\n".toByteArray())

        out.write(CMD_DOUBLE_HEIGHT)
        out.write(CMD_BOLD_ON)
        out.write("${padRow("NET AMOUNT:", String.format(Locale.US, "Rs. %.2f", entry.totalAmount), paperWidth)}\n".toByteArray())
        out.write(CMD_NORMAL_SIZE)
        out.write(CMD_BOLD_OFF)
        out.write("${getDoubleDivider(paperWidth)}\n".toByteArray())

        if (farmer != null && farmer.balancePayable != 0.0) {
            val balText = if (farmer.balancePayable > 0) "Rs. %.2f (Due)" else "Rs. %.2f (Advance)"
            out.write("${padRow("Previous Bal:", String.format(Locale.US, balText, Math.abs(farmer.balancePayable)), paperWidth)}\n".toByteArray())
            out.write("${getDivider(paperWidth)}\n".toByteArray())
        }

        out.write(CMD_ALIGN_CENTER)
        out.write("Keep this slip safe for weekly bill.\n".toByteArray())
        out.write("MilkMate Dairy Solutions\n\n\n\n".toByteArray())
        out.write(CMD_FEED_AND_CUT)

        return out.toByteArray()
    }

    /**
     * Sends ESC/POS byte array over Bluetooth RFCOMM SPP socket to the selected device address.
     */
    @SuppressLint("MissingPermission")
    suspend fun printBytes(
        context: Context,
        deviceAddress: String,
        bytes: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!BluetoothHardwareManager.hasBluetoothPermissions(context)) {
            return@withContext Result.failure(Exception("Bluetooth permission not granted."))
        }
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: return@withContext Result.failure(Exception("Bluetooth hardware unavailable."))
        if (!adapter.isEnabled) {
            return@withContext Result.failure(Exception("Bluetooth is turned OFF. Please turn on Bluetooth."))
        }
        if (deviceAddress.isBlank()) {
            return@withContext Result.failure(Exception("No thermal printer selected in Settings."))
        }

        var socket: BluetoothSocket? = null
        try {
            val device = adapter.getRemoteDevice(deviceAddress)
            try { adapter.cancelDiscovery() } catch (_: Exception) {}

            socket = device.createRfcommSocketToServiceRecord(BluetoothHardwareManager.SPP_UUID)
            socket.connect()

            val outStream: OutputStream = socket.outputStream
            outStream.write(bytes)
            outStream.flush()

            // Short pause to allow printer buffer to consume bytes before closing socket
            kotlinx.coroutines.delay(500)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Print failed: ${e.localizedMessage ?: "Could not connect to printer. Ensure it is powered on & paired in Android settings."}"))
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Generates a clean plain-text slip for screen preview, clipboard copy, or WhatsApp sharing.
     */
    fun formatSlipText(
        dairyName: String,
        entry: MilkCollectionEntity,
        farmer: FarmerEntity?,
        paperWidth: String = "58MM"
    ): String {
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(entry.timestamp))
        val divider = getDivider(paperWidth)
        val dblDivider = getDoubleDivider(paperWidth)

        val sb = StringBuilder()
        sb.appendLine(centerText(dairyName.ifBlank { "MILK COLLECTION CENTER" }, paperWidth))
        sb.appendLine(centerText("DAIRY MILK INTAKE RECEIPT", paperWidth))
        sb.appendLine(divider)
        sb.appendLine(padRow("Slip #${entry.id.takeLast(6).uppercase(Locale.ROOT)}", entry.shift, paperWidth))
        sb.appendLine(padRow("Date/Time:", dateStr, paperWidth))
        sb.appendLine(divider)
        val codeStr = if (farmer?.farmerCode.isNullOrBlank()) "" else "[${farmer?.farmerCode}] "
        sb.appendLine("Farmer: $codeStr${entry.farmerName}")
        if (!farmer?.mobile.isNullOrBlank()) {
            sb.appendLine("Mobile: ${farmer?.mobile}")
        }
        sb.appendLine(divider)
        sb.appendLine(padRow("Milk Category:", "${entry.milkType} MILK", paperWidth))
        sb.appendLine(padRow("Quantity (L):", String.format(Locale.US, "%.1f L", entry.quantityLiters), paperWidth))
        sb.appendLine(padRow("FAT % / SNF %:", String.format(Locale.US, "%.2f%% / %.2f%%", entry.fat, entry.snf), paperWidth))
        if (entry.clr > 0) {
            sb.appendLine(padRow("CLR Reading:", String.format(Locale.US, "%.1f", entry.clr), paperWidth))
        }
        sb.appendLine(padRow("Rate per Liter:", String.format(Locale.US, "₹%.2f/L", entry.ratePerLiter), paperWidth))
        sb.appendLine(dblDivider)
        sb.appendLine(padRow("TOTAL AMOUNT:", String.format(Locale.US, "₹%.2f", entry.totalAmount), paperWidth))
        sb.appendLine(dblDivider)
        sb.appendLine(centerText("MilkMate Dairy Solutions", paperWidth))
        return sb.toString()
    }
}
