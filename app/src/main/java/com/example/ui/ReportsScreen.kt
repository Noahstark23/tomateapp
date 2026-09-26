package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Inventory
import com.example.ui.theme.CustomOnSuccessVariant
import com.example.ui.theme.CustomSuccessContainer
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch
import android.content.Intent
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: DashboardViewModel) {
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val ledgers by viewModel.historicalLedgers.collectAsStateWithLifecycle()
    val invoiceDetails by viewModel.invoiceDetails.collectAsStateWithLifecycle()
    val wasteByCause by viewModel.wasteByCause.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAddProduct by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Inventory?>(null) }
    var lotsItem by remember { mutableStateOf<Inventory?>(null) }
    var ivaRate by remember { mutableStateOf(0.01) }

    val restorePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = viewModel.restoreDatabase(context, uri)
                Toast.makeText(
                    context,
                    if (ok) "Respaldo restaurado: reinicie la app" else "No se pudo restaurar",
                    Toast.LENGTH_LONG
                ).show()
                if (ok) (context as? android.app.Activity)?.recreate()
            }
        }
    }

    val format = NumberFormat.getCurrencyInstance(Locale("es", "CR"))

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("📈", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        Column {
                            Text("Reportes y Rentabilidad", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                            Text("Estado financiero por día", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Text(
                    "Estado de Inventario",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        inventory.forEachIndexed { index, item ->
                            val isLowStock = item.current_stock < 10
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editingItem = item }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.item_name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Inicial: ${item.initial_stock} · Valor a costo: ${format.format(item.current_stock * item.purchase_price)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                val stockColor = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                val stockContainer = if (isLowStock) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer

                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        color = stockContainer,
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text(
                                            "${item.current_stock}",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = stockColor
                                        )
                                    }
                                    TextButton(onClick = { lotsItem = item }) {
                                        Text("📦 Lotes", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            if (index < inventory.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Estado Financiero Diario (Últimos 30 días)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAddProduct = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("+ Producto") }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val uri = viewModel.backupDatabase(context)
                                if (uri == null) {
                                    Toast.makeText(context, "No se pudo crear el respaldo", Toast.LENGTH_SHORT).show()
                                } else {
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/octet-stream"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(share, "Guardar respaldo"))
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("💾 Respaldo") }
                    OutlinedButton(
                        onClick = { restorePicker.launch("application/octet-stream") },
                        modifier = Modifier.weight(1f)
                    ) { Text("♻️ Restaurar") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val uri = viewModel.exportDayCsv(context)
                                if (uri == null) {
                                    Toast.makeText(context, "No se pudo generar el CSV", Toast.LENGTH_SHORT).show()
                                } else {
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(share, "Enviar CSV del día"))
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Exportar CSV") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (wasteByCause.isNotEmpty()) {
                    Text(
                        "Merma de hoy por causa",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    wasteByCause.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (row.causa.isBlank()) "Sin clasificar" else row.causa,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                format.format(row.total),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Text(
                    "Facturas electrónicas (pre-firma)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "XML v4.4 listo para firmar (.p12) y enviar a ATV. Sin firma no tiene validez. IVA: verificar con su contador.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.01 to "1%", 0.13 to "13%", 0.0 to "Exento").forEach { (rate, label) ->
                        OutlinedButton(
                            onClick = { ivaRate = rate },
                            modifier = Modifier.weight(1f),
                            border = if (ivaRate == rate) BorderStroke(
                                2.dp, MaterialTheme.colorScheme.primary
                            ) else null
                        ) { Text(label) }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(invoiceDetails) { inv ->
                val ready = inv.idNumber.isNotBlank() && inv.cabys.isNotBlank()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${inv.clientName} · ${inv.quantity}× ${inv.itemName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Cons. ...${inv.consecutive.takeLast(6)} · ${format.format(inv.total_amount)} · ${inv.fe_status}" +
                                    if (ready) "" else " · falta cédula/CABYS",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val uri = viewModel.exportInvoiceXml(context, inv.id, ivaRate)
                                    if (uri == null) {
                                        Toast.makeText(context, "No se pudo generar el XML", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val share = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/xml"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(share, "Enviar XML FE"))
                                    }
                                }
                            },
                            enabled = ready,
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("🧾 XML") }
                    }
                }
            }

            items(ledgers.take(30)) { ledger ->
                // Margen neto del día sobre ventas.
                val margin = if (ledger.total_sales > 0) {
                    ledger.real_net_profit / ledger.total_sales * 100
                } else 0.0
                val marginFormatted = String.format(Locale.US, "%.1f%%", margin)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "📅 ${ledger.date}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            val marginColor = if (margin >= 0) CustomOnSuccessVariant else MaterialTheme.colorScheme.error
                            val marginContainer = if (margin >= 0) CustomSuccessContainer else MaterialTheme.colorScheme.errorContainer

                            Surface(
                                color = marginContainer,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    "Margen: $marginFormatted",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = marginColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Ventas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(format.format(ledger.total_sales), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Costo Merc.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(format.format(ledger.total_cogs), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("Ganancia Real", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val profitColor = if (ledger.real_net_profit >= 0) CustomOnSuccessVariant else MaterialTheme.colorScheme.error
                                Text(
                                    format.format(ledger.real_net_profit),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = profitColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gastos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(format.format(ledger.total_expenses), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Merma", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(format.format(ledger.total_waste_value), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("Caja Final", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(format.format(ledger.cash_on_hand), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showAddProduct) {
        AddProductDialog(
            onConfirm = { name, cost, sale, stock ->
                viewModel.addProduct(name, cost, sale, stock)
                showAddProduct = false
            },
            onDismiss = { showAddProduct = false }
        )
    }

    editingItem?.let { item ->
        EditProductDialog(
            item = item,
            onConfirm = { sale, cost, entry, cabys, unit ->
                viewModel.updatePrices(item.id, sale, cost)
                viewModel.updateCatalog(item.id, cabys, unit)
                if (entry > 0) viewModel.addStock(item.id, entry)
                editingItem = null
            },
            onDismiss = { editingItem = null }
        )
    }

    lotsItem?.let { item ->
        LotsDialog(
            item = item,
            viewModel = viewModel,
            onDismiss = { lotsItem = null }
        )
    }
}

/** Alta de producto con precios y stock inicial. */
@Composable
private fun AddProductDialog(
    onConfirm: (String, Double, Double, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var saleStr by remember { mutableStateOf("") }
    var stockStr by remember { mutableStateOf("") }

    val cost = costStr.toDoubleOrNull()
    val sale = saleStr.toDoubleOrNull()
    val stock = stockStr.toIntOrNull()
    val canConfirm = name.trim().isNotEmpty() && cost != null && cost >= 0 &&
        sale != null && sale > 0 && stock != null && stock >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Producto", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                ProductNumberField("Nombre (ej. Caja Tomate Cherry)", name, { name = it }, KeyboardType.Text)
                Spacer(modifier = Modifier.height(8.dp))
                ProductNumberField("Costo por unidad (CRC)", costStr, { costStr = it }, KeyboardType.Number)
                Spacer(modifier = Modifier.height(8.dp))
                ProductNumberField("Precio venta (CRC)", saleStr, { saleStr = it }, KeyboardType.Number)
                Spacer(modifier = Modifier.height(8.dp))
                ProductNumberField("Stock inicial", stockStr, { stockStr = it }, KeyboardType.Number)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), cost!!, sale!!, stock!!) },
                enabled = canConfirm,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

/** Edita precios vigentes, CABYS/unidad FE y registra entrada de mercadería. */
@Composable
private fun EditProductDialog(
    item: Inventory,
    onConfirm: (Double, Double, Int, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var saleStr by remember { mutableStateOf(item.sale_price.toString()) }
    var costStr by remember { mutableStateOf(item.purchase_price.toString()) }
    var entryStr by remember { mutableStateOf("") }
    var cabys by remember { mutableStateOf(item.cabys) }
    var unit by remember { mutableStateOf(item.unit.ifEmpty { "Unid" }) }

    val sale = saleStr.toDoubleOrNull()
    val cost = costStr.toDoubleOrNull()
    val entry = entryStr.toIntOrNull() ?: 0
    val canConfirm = sale != null && sale > 0 && cost != null && cost >= 0 && entry >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.item_name, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Stock actual: ${item.current_stock}. Los cambios no alteran ventas pasadas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                ProductNumberField("Precio venta (CRC)", saleStr, { saleStr = it }, KeyboardType.Number)
                Spacer(modifier = Modifier.height(8.dp))
                ProductNumberField("Costo por unidad (CRC)", costStr, { costStr = it }, KeyboardType.Number)
                Spacer(modifier = Modifier.height(8.dp))
                ProductNumberField("Entrada de mercadería (+stock)", entryStr, { entryStr = it }, KeyboardType.Number)
                Spacer(modifier = Modifier.height(8.dp))
                ProductNumberField("CABYS Hacienda", cabys, { cabys = it }, KeyboardType.Text)
                Spacer(modifier = Modifier.height(8.dp))
                ProductNumberField("Unidad (Unid/kg/Caja)", unit, { unit = it }, KeyboardType.Text)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(sale!!, cost!!, entry, cabys, unit.ifBlank { "Unid" }) },
                enabled = canConfirm,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
/**
 * Lotes del producto con consumo FEFO: el primero de la lista es el próximo
 * en salir. Alerta en rojo si vence en 1 día o menos.
 */
@Composable
private fun LotsDialog(
    item: Inventory,
    viewModel: DashboardViewModel,
    onDismiss: () -> Unit
) {
    val lots by viewModel.lotsFor(item.id).collectAsStateWithLifecycle()
    var showEntry by remember { mutableStateOf(false) }
    val today = remember { java.time.LocalDate.now() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lotes · ${item.item_name}", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (lots.isEmpty()) {
                    Text(
                        "Sin lotes con trazabilidad. Registre la entrada para activar FEFO; mientras tanto las ventas salen del stock global.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                lots.forEachIndexed { index, lot ->
                    val daysLeft = try {
                        java.time.temporal.ChronoUnit.DAYS.between(
                            today, java.time.LocalDate.parse(lot.fecha_limite)
                        )
                    } catch (e: Exception) { 99L }
                    val urgent = daysLeft <= 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${lot.supplier.ifEmpty { "Sin proveedor" }}" +
                                    (if (lot.variedad.isNotBlank()) " · ${lot.variedad}" else "") +
                                    (if (lot.calibre.isNotBlank()) " ${lot.calibre}" else "") +
                                    (if (lot.calidad.isNotBlank()) " ${lot.calidad}" else ""),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${lot.qty_current}/${lot.qty_initial} uds · vence ${lot.fecha_limite}" +
                                    (if (index == 0) " · ⏩ SALE PRIMERO" else ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (urgent) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (urgent) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showEntry = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("+ Entrada de lote") }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )

    if (showEntry) {
        LotEntryDialog(
            onConfirm = { supplier, variedad, calibre, calidad, qty, cost, days ->
                viewModel.registerLot(
                    item.id, supplier, variedad, calibre, calidad, qty, cost, days
                ) { showEntry = false }
            },
            onDismiss = { showEntry = false }
        )
    }
}

/** Entrada de lote: proveedor, variedad, calibre, calidad, cantidad, costo y vida útil. */
@Composable
private fun LotEntryDialog(
    onConfirm: (String, String, String, String, Int, Double, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var supplier by remember { mutableStateOf("") }
    var variedad by remember { mutableStateOf("") }
    var calibre by remember { mutableStateOf("") }
    var calidad by remember { mutableStateOf("Primera") }
    var qtyStr by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var daysStr by remember { mutableStateOf("4") }

    val qty = qtyStr.toIntOrNull()
    val cost = costStr.toDoubleOrNull()
    val days = daysStr.toIntOrNull()
    val canConfirm = qty != null && qty > 0 && cost != null && cost >= 0 &&
        days != null && days in 1..30

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Entrada de lote", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                ProductNumberField("Proveedor / finca", supplier, { supplier = it }, KeyboardType.Text)
                Spacer(modifier = Modifier.height(8.dp))
                ProductNumberField("Variedad (ej. Candela)", variedad, { variedad = it }, KeyboardType.Text)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Primera", "Segunda", "Tercera").forEach { option ->
                        OutlinedButton(
                            onClick = { calidad = option },
                            modifier = Modifier.weight(1f),
                            border = if (calidad == option) BorderStroke(
                                2.dp, MaterialTheme.colorScheme.primary
                            ) else null
                        ) { Text(option, style = MaterialTheme.typography.labelSmall) }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                ProductNumberField("Calibre (ej. GG/G/M)", calibre, { calibre = it }, KeyboardType.Text)
                Spacer(modifier = Modifier.height(8.dp))
                ProductNumberField("Cantidad (uds)", qtyStr, { qtyStr = it }, KeyboardType.Number)
                Spacer(modifier = Modifier.height(8.dp))
                ProductNumberField("Costo total lote (CRC)", costStr, { costStr = it }, KeyboardType.Number)
                Spacer(modifier = Modifier.height(8.dp))
                ProductNumberField("Vida útil (días, límite = hoy + días)", daysStr, { daysStr = it }, KeyboardType.Number)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(supplier, variedad, calibre, calidad, qty!!, cost!!, days!!)
                },
                enabled = canConfirm,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Guardar lote") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ProductNumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboard: KeyboardType
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}
