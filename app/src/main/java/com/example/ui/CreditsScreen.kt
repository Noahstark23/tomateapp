package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Días de antigüedad de una factura a crédito. */
private fun ageDays(ledgerDate: String): Long = try {
    ChronoUnit.DAYS.between(LocalDate.parse(ledgerDate), LocalDate.now())
} catch (e: Exception) {
    0L
}

private fun ageBucket(days: Long): String = when {
    days <= 7 -> "0-7 días"
    days <= 15 -> "8-15 días"
    days <= 30 -> "16-30 días"
    else -> "+30 días"
}

/**
 * Tab Créditos: saldos por cobrar, abonos (suman caja hoy) y cobro por
 * WhatsApp. Los saldos son derivados de facturas a crédito, nunca editados.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(viewModel: DashboardViewModel) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val balances by viewModel.creditBalances.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val format = remember { NumberFormat.getCurrencyInstance(Locale("es", "CR")) }

    var selectedClientId by remember { mutableStateOf<Int?>(null) }
    var showAbono by remember { mutableStateOf(false) }
    var showLimit by remember { mutableStateOf(false) }

    val abonoInvoicesFlow = remember(selectedClientId) {
        viewModel.openCreditInvoices(selectedClientId ?: -1)
    }
    val abonoInvoices by abonoInvoicesFlow.collectAsStateWithLifecycle()

    val debtors = remember(clients, balances) {
        clients.filter { (balances[it.id] ?: 0.0) > 0.005 }
            .sortedByDescending { balances[it.id] ?: 0.0 }
    }
    val totalDue = debtors.sumOf { balances[it.id] ?: 0.0 }
    val selectedClient = clients.firstOrNull { it.id == selectedClientId }

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("💳", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Column {
                        Text(
                            "Créditos y Cobros",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Fiado por cobrar · el abono suma caja hoy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (totalDue > 0)
                            MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "TOTAL POR COBRAR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            format.format(totalDue),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${debtors.size} clientes con saldo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (debtors.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            "Sin fiado pendiente. Para vender a crédito, asigne cupo al cliente (botón Cupo) y active Fiado en la venta.",
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            items(debtors.size) { index ->
                val client = debtors[index]
                val balance = balances[client.id] ?: 0.0
                val expanded = selectedClientId == client.id
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(client.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Debe ${format.format(balance)} · Cupo ${format.format(client.credit_limit)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = {
                                    PhoneUtils.openWhatsApp(
                                        context,
                                        client.contact_info.ifEmpty { client.phone },
                                        "Hola ${client.name}, le escribe TomateApp. Le recordamos el saldo pendiente de ${format.format(balance)}. ¿Podemos coordinar el pago hoy? Gracias."
                                    )
                                },
                                enabled = PhoneUtils.isValidCrPhone(
                                    client.contact_info.ifEmpty { client.phone }
                                )
                            ) { Text("💬 Cobrar") }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    selectedClientId = if (expanded) null else client.id
                                }
                            ) { Text(if (expanded) "Ocultar" else "Ver facturas") }
                            OutlinedButton(
                                onClick = {
                                    selectedClientId = client.id
                                    showAbono = true
                                }
                            ) { Text("Abonar") }
                            OutlinedButton(
                                onClick = {
                                    selectedClientId = client.id
                                    showLimit = true
                                }
                            ) { Text("Cupo") }
                        }
                        if (expanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            CreditInvoicesList(clientId = client.id, viewModel = viewModel, format = format)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showAbono && selectedClient != null) {
        AbonoDialog(
            clientName = selectedClient!!.name,
            balance = balances[selectedClient!!.id] ?: 0.0,
            invoices = abonoInvoices,
            format = format,
            onConfirm = { amount, invoiceId ->
                viewModel.registerPayment(selectedClient!!.id, invoiceId, amount) { ok ->
                    Toast.makeText(
                        context,
                        if (ok) "Abono registrado (suma caja hoy)" else "Monto inválido o sobrepago",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                showAbono = false
            },
            onDismiss = { showAbono = false }
        )
    }

    if (showLimit && selectedClient != null) {
        LimitDialog(
            clientName = selectedClient!!.name,
            currentLimit = selectedClient!!.credit_limit,
            onConfirm = { limit ->
                viewModel.setCreditLimit(selectedClient!!.id, limit)
                showLimit = false
            },
            onDismiss = { showLimit = false }
        )
    }
}

@Composable
private fun CreditInvoicesList(
    clientId: Int,
    viewModel: DashboardViewModel,
    format: NumberFormat
) {
    val invoices by viewModel.openCreditInvoices(clientId).collectAsStateWithLifecycle()
    if (invoices.isEmpty()) {
        Text(
            "Sin facturas abiertas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Column {
        invoices.forEach { inv ->
            val days = ageDays(inv.ledger_date)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${inv.ledger_date} · ${inv.quantity} uds × ${format.format(inv.unit_price)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "${ageBucket(days)} (${days}d)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (days > 15) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    format.format(inv.balance),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AbonoDialog(
    clientName: String,
    balance: Double,
    invoices: List<com.example.data.Invoice>,
    format: NumberFormat,
    onConfirm: (Double, Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var targetId by remember { mutableStateOf<Int?>(null) }
    var invoiceExpanded by remember { mutableStateOf(false) }
    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val targetBalance = targetId?.let { id -> invoices.firstOrNull { it.id == id }?.balance } ?: balance
    val canConfirm = amount > 0 && amount <= targetBalance + 0.005

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abonar a $clientName", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Saldo: ${format.format(balance)}. El abono suma a la caja de hoy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = invoiceExpanded,
                    onExpandedChange = { invoiceExpanded = !invoiceExpanded }
                ) {
                    OutlinedTextField(
                        value = targetId?.let { id ->
                            invoices.firstOrNull { it.id == id }?.let {
                                "${it.ledger_date} · saldo ${format.format(it.balance)}"
                            }
                        } ?: "Reparto FIFO (más viejas primero)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Aplicar a") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = invoiceExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = invoiceExpanded,
                        onDismissRequest = { invoiceExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Reparto FIFO (más viejas primero)") },
                            onClick = {
                                targetId = null
                                invoiceExpanded = false
                            }
                        )
                        invoices.forEach { inv ->
                            DropdownMenuItem(
                                text = { Text("${inv.ledger_date} · ${inv.quantity} uds · saldo ${format.format(inv.balance)}") },
                                onClick = {
                                    targetId = inv.id
                                    invoiceExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Monto (CRC)") },
                    isError = amountStr.isNotEmpty() && !canConfirm,
                    supportingText = if (amountStr.isNotEmpty() && !canConfirm) {
                        { Text("No puede exceder el saldo") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amount, targetId) },
                enabled = canConfirm,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Registrar abono") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun LimitDialog(
    clientName: String,
    currentLimit: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var limitStr by remember { mutableStateOf(if (currentLimit > 0) currentLimit.toString() else "") }
    val limit = limitStr.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cupo de $clientName", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "0 = sin crédito (no se puede fiar). El fiado se bloquea si saldo + venta excede el cupo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = { Text("Cupo (CRC)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (limit != null) onConfirm(limit) },
                enabled = limit != null && limit >= 0,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
