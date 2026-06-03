package com.example.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.OutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

class PrintService(private val context: Context) {
    
    @SuppressLint("MissingPermission")
    suspend fun printReceipt(
        clientName: String,
        itemName: String,
        quantity: Int,
        salePrice: Double,
        totalAmount: Double
    ): Boolean = withContext(Dispatchers.IO) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter: BluetoothAdapter? = bluetoothManager.adapter
        
        if (adapter == null || !adapter.isEnabled) return@withContext false
        
        val pairedDevices = adapter.bondedDevices
        if (pairedDevices.isNullOrEmpty()) return@withContext false
        
        // Try to find a printer (often uncategorized or imaging)
        val device: BluetoothDevice = pairedDevices.firstOrNull { 
            it.name?.contains("printer", ignoreCase = true) == true || 
            it.name?.contains("pos", ignoreCase = true) == true
        } ?: pairedDevices.first()
        
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb") // Standard SPP UUID
        var socket: BluetoothSocket? = null
        
        try {
            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket.connect()
            
            val outputStream: OutputStream = socket.outputStream
            
            // ESC/POS Commands
            val ESC: Byte = 0x1B
            val GS: Byte = 0x1D
            val INIT = byteArrayOf(ESC, 0x40) // Initialize
            val ALIGN_CENTER = byteArrayOf(ESC, 0x61, 1)
            val ALIGN_LEFT = byteArrayOf(ESC, 0x61, 0)
            val BOLD_ON = byteArrayOf(ESC, 0x45, 1)
            val BOLD_OFF = byteArrayOf(ESC, 0x45, 0)
            val TEXT_DOUBLE_HEIGHT_WIDTH = byteArrayOf(GS, 0x21, 0x11)
            val TEXT_NORMAL = byteArrayOf(GS, 0x21, 0x00)
            val LINE_FEED = byteArrayOf(0x0A)
            
            outputStream.write(INIT)
            
            // Header
            outputStream.write(ALIGN_CENTER)
            outputStream.write(BOLD_ON)
            outputStream.write("Venta de Tomates\n".toByteArray(Charsets.UTF_8))
            outputStream.write(BOLD_OFF)
            outputStream.write(LINE_FEED)
            
            // Date and Time
            outputStream.write(ALIGN_LEFT)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            outputStream.write("Fecha: $currentDate\n".toByteArray(Charsets.UTF_8))
            
            // Client Name
            outputStream.write("Cliente: $clientName\n".toByteArray(Charsets.UTF_8))
            outputStream.write("--------------------------------\n".toByteArray(Charsets.UTF_8))
            
            // Itemized List
            val format = NumberFormat.getCurrencyInstance(Locale("es", "CR"))
            outputStream.write("Producto: $itemName\n".toByteArray(Charsets.UTF_8))
            outputStream.write("Cant: $quantity x ${format.format(salePrice)}\n".toByteArray(Charsets.UTF_8))
            outputStream.write("--------------------------------\n".toByteArray(Charsets.UTF_8))
            
            // Total
            outputStream.write(ALIGN_CENTER)
            outputStream.write(BOLD_ON)
            outputStream.write(TEXT_DOUBLE_HEIGHT_WIDTH)
            outputStream.write("TOTAL: ${format.format(totalAmount)}\n".toByteArray(Charsets.UTF_8))
            outputStream.write(TEXT_NORMAL)
            outputStream.write(BOLD_OFF)
            
            // Feed paper and cut
            outputStream.write(LINE_FEED)
            outputStream.write(LINE_FEED)
            outputStream.write(LINE_FEED)
            
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
