package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Client
import com.example.data.Inventory
import com.example.ui.theme.CustomOnSuccessVariant
import com.example.ui.theme.HealthAmber
import java.text.NumberFormat
import java.util.Locale

private sealed interface ManagementDialog {
    data object AddProduct : ManagementDialog
    data class EditProduct(val item: Inventory) : ManagementDialog
    data class Restock(val item: Inventory) : ManagementDialog
    data object AddClient : ManagementDialog
    data class EditClient(val client: Client) : ManagementDialog
}

/**
 * Pantalla de gestión del catálogo: productos (alta, edición, reposición con
 * compra que descuenta caja) y clientes (alta, edición). Sin bajas: las
 * facturas, mermas y compras históricas referencian estos registros.
 */
@Composable
fun ManagementScreen(viewModel: DashboardViewModel) {
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val metrics by viewModel.currentMetrics.collectAsStateWithLifecycle()

    var activeDialog by remember { mutableStateOf<ManagementDialog?>(null) }

    val format = remember { NumberFormat.getCurrencyInstance(Locale("es", "CR")) }

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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("📦", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Column {
                        Text("Gestión", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                        Text("Inventario y clientes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader(
                    title = "Inventario",
                    actionLabel = "➕ Producto",
                    onAction = { activeDialog = ManagementDialog.AddProduct }
                )
            }

            if (inventory.isEmpty()) {
                item {
                    EmptyHintCard("No hay productos aún. Agrega tu primer producto para poder vender y registrar mermas.")
                }
            } else {
                items(inventory, key = { "inv-${it.id}" }) { item ->
                    ProductCard(
                        item = item,
                        format = format,
                        onRestock = { activeDialog = ManagementDialog.Restock(item) },
                        onEdit = { activeDialog = ManagementDialog.EditProduct(item) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Clientes",
                    actionLabel = "➕ Cliente",
                    onAction = { activeDialog = ManagementDialog.AddClient }
                )
            }

            if (clients.isEmpty()) {
                item { EmptyHintCard("No hay clientes aún. Agrega uno para registrar ventas.") }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            clients.forEachIndexed { index, client ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            client.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (client.contact_info.isNotBlank()) {
                                            Text(
                                                client.contact_info,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    TextButton(onClick = { activeDialog = ManagementDialog.EditClient(client) }) {
                                        Text("Editar")
                                    }
                                }
                                if (index < clients.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    when (val dialog = activeDialog) {
        ManagementDialog.AddProduct -> ProductFormDialog(
            title = "Agregar Producto",
            confirmLabel = "Agregar",
            showInitialStock = true,
            onConfirm = { name, cost, price, stock ->
                viewModel.addProduct(name, cost, price, stock)
            },
            onDismiss = { activeDialog = null }
        )
        is ManagementDialog.EditProduct -> ProductFormDialog(
            title = "Editar Producto",
            confirmLabel = "Guardar",
            showInitialStock = false,
            initial = dialog.item,
            onConfirm = { name, cost, price, _ ->
                viewModel.updateProduct(
                    dialog.item.copy(item_name = name, purchase_price = cost, sale_price = price)
                )
            },
            onDismiss = { activeDialog = null }
        )
        is ManagementDialog.Restock -> RestockDialog(
            item = dialog.item,
            cashOnHand = metrics.cashOnHand,
            hasLedger = metrics.hasLedger,
            format = format,
            onConfirm = { qty, unitCost -> viewModel.restockProduct(dialog.item, qty, unitCost) },
            onDismiss = { activeDialog = null }
        )
        ManagementDialog.AddClient -> ClientFormDialog(
            title = "Agregar Cliente",
            confirmLabel = "Agregar",
            onConfirm = { name, contact -> viewModel.addClient(name, contact) },
            onDismiss = { activeDialog = null }
        )
        is ManagementDialog.EditClient -> ClientFormDialog(
            title = "Editar Cliente",
            confirmLabel = "Guardar",
            initialName = dialog.client.name,
            initialContact = dialog.client.contact_info,
            onConfirm = { name, contact ->
                viewModel.updateClient(dialog.client.copy(name = name, contact_info = contact))
            },
            onDismiss = { activeDialog = null }
        )
        null -> Unit
    }
}

@Composable
private fun SectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        OutlinedButton(onClick = onAction, shape = RoundedCornerShape(12.dp)) {
            Text(actionLabel, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun EmptyHintCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            message,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProductCard(
    item: Inventory,
    format: NumberFormat,
    onRestock: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
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
                    item.item_name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                val isLowStock = item.current_stock < 10
                Surface(
                    color = if (isLowStock) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Stock: ${item.current_stock}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Costo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(format.format(item.purchase_price), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Precio Venta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(format.format(item.sale_price), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("Margen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val margin = if (item.sale_price > 0) {
                        (item.sale_price - item.purchase_price) / item.sale_price * 100
                    } else 0.0
                    Text(
                        String.format(Locale.US, "%.1f%%", margin),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (margin > 0) CustomOnSuccessVariant else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRestock,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🛒 Reponer", fontWeight = FontWeight.Medium)
                }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("✏️ Editar", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Diálogo: alta / edición de producto
// ---------------------------------------------------------------------------

@Composable
private fun ProductFormDialog(
    title: String,
    confirmLabel: String,
    showInitialStock: Boolean,
    initial: Inventory? = null,
    onConfirm: (name: String, purchasePrice: Double, salePrice: Double, initialStock: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.item_name ?: "") }
    var costStr by remember { mutableStateOf(initial?.purchase_price?.toString() ?: "") }
    var priceStr by remember { mutableStateOf(initial?.sale_price?.toString() ?: "") }
    var stockStr by remember { mutableStateOf("") }

    val cost = costStr.toDoubleOrNull()
    val price = priceStr.toDoubleOrNull()
    val stock = if (showInitialStock) stockStr.toIntOrNull() ?: 0 else 0
    val canConfirm = name.isNotBlank() && cost != null && cost >= 0 && price != null && price >= 0 &&
        (!showInitialStock || stockStr.isEmpty() || stockStr.toIntOrNull() != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del producto") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text("Costo unitario (CRC)") },
                    supportingText = { Text("Se usa para valorar costo de ventas y mermas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Precio de venta (CRC)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (showInitialStock) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("Stock inicial (opcional)") },
                        supportingText = { Text("Mercancía que ya posees: no descuenta caja. Las compras posteriores sí.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                val c = cost
                val p = price
                if (c != null && p != null && p > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Margen por unidad: ${String.format(Locale.US, "%.1f%%", (p - c) / p * 100)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (p >= c) CustomOnSuccessVariant else MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, cost ?: 0.0, price ?: 0.0, stock)
                    onDismiss()
                },
                enabled = canConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ---------------------------------------------------------------------------
// Diálogo: compra / reposición de stock
// ---------------------------------------------------------------------------

@Composable
private fun RestockDialog(
    item: Inventory,
    cashOnHand: Double,
    hasLedger: Boolean,
    format: NumberFormat,
    onConfirm: (quantity: Int, unitCost: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var quantityStr by remember { mutableStateOf("") }
    var unitCostStr by remember { mutableStateOf(item.purchase_price.toString()) }

    val quantity = quantityStr.toIntOrNull() ?: 0
    val unitCost = unitCostStr.toDoubleOrNull()
    val totalCost = if (unitCost != null) quantity * unitCost else 0.0
    val canConfirm = quantity > 0 && unitCost != null && unitCost >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reponer: ${item.item_name}", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "La compra sale de tu caja y el costo del producto se actualiza a promedio ponderado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Cantidad a comprar") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = unitCostStr,
                    onValueChange = { unitCostStr = it },
                    label = { Text("Costo unitario (CRC)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (canConfirm) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Saldrá de tu caja: ${format.format(totalCost)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Stock resultante: ${item.current_stock + quantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!hasLedger) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "ℹ️ Aún no iniciaste el día: la compra quedará registrada y se descontará de la caja al iniciar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = HealthAmber
                        )
                    } else if (totalCost > cashOnHand) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "⚠️ Este monto supera tu caja actual (${format.format(cashOnHand)}). Quedarás con efectivo negativo.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(quantity, unitCost ?: 0.0)
                    onDismiss()
                },
                enabled = canConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Comprar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ---------------------------------------------------------------------------
// Diálogo: alta / edición de cliente
// ---------------------------------------------------------------------------

@Composable
private fun ClientFormDialog(
    title: String,
    confirmLabel: String,
    initialName: String = "",
    initialContact: String = "",
    onConfirm: (name: String, contact: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var contact by remember { mutableStateOf(initialContact) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contacto (teléfono, opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, contact)
                    onDismiss()
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
