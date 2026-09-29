package com.example.ui

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CustomOnSuccessVariant
import java.text.NumberFormat
import java.util.Locale

/**
 * Tab Flujo de Caja: entradas vs salidas del día (efectivo real) + predicción
 * simple a 7 días. Todo se lee del ledger materializado; la proyección vive
 * solo en memoria y nunca escribe en la BD.
 */
@Composable
fun CashFlowScreen(financialViewModel: FinancialViewModel, viewModel: DashboardViewModel) {
    val state by financialViewModel.cashFlowState.collectAsStateWithLifecycle()
    val cashCounts by viewModel.cashCounts().collectAsStateWithLifecycle()
    val format = remember { NumberFormat.getCurrencyInstance(Locale("es", "CR")) }
    var showArqueo by remember { mutableStateOf(false) }

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
                            Text("💰", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Column {
                        Text(
                            "Flujo de Caja",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Entradas vs salidas del día",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (!state.hasLedger) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        "Inicia el día para ver el flujo de caja.",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Saldo inicial vs actual + neto del día.
            item {
                CashCard(
                    title = "CAJA DE HOY",
                    rows = listOf(
                        "Empezaste con" to format.format(state.initialCash),
                        "Tienes ahora" to format.format(state.currentCash)
                    ),
                    highlight = (if (state.netCashToday >= 0) "+" else "−") +
                        " Neto caja: ${format.format(kotlin.math.abs(state.netCashToday))}",
                    highlightColor = if (state.netCashToday >= 0) CustomOnSuccessVariant
                    else MaterialTheme.colorScheme.error
                )
            }

            // Entradas: ventas al contado del día por producto + abonos.
            item {
                val entryRows = state.salesByProduct
                    .filter { it.total > 0 }
                    .map { "${it.itemName} × ${it.quantity}" to format.format(it.total) }
                    .toMutableList()
                if (state.collectionsToday > 0) {
                    entryRows.add("Abonos cobrados" to format.format(state.collectionsToday))
                }
                CashCard(
                    title = "ENTRADAS · ${format.format(state.cashSalesToday + state.collectionsToday)}",
                    subtitle = "${state.invoiceCount} ventas · ticket prom. ${format.format(state.avgTicket)}",
                    rows = entryRows,
                    emptyText = "Sin entradas de efectivo hoy."
                )
            }

            // Fiado del día: reconocido como ingreso pero no es caja todavía.
            if (state.creditSalesToday > 0) {
                item {
                    CashCard(
                        title = "FIADO HOY · ${format.format(state.creditSalesToday)}",
                        subtitle = "Por cobrar: suma a ganancia, no a caja",
                        rows = emptyList(),
                        emptyText = null
                    )
                }
            }

            // Salidas: gastos del día por categoría.
            item {
                CashCard(
                    title = "SALIDAS · ${format.format(state.totalExpenses)}",
                    rows = state.expensesByCategory.map {
                        it.category.displayName() to format.format(it.total)
                    },
                    emptyText = "Sin gastos registrados hoy."
                )
            }

            // Neto caja vs ganancia real (educativo, evita confusiones).
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Neto caja ${format.format(state.netCashToday)} = contado + abonos − gastos (efectivo).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Ganancia real ${format.format(state.realNetProfit)} = ventas (contado + fiado) − costo − gastos − merma (el fiado, la merma y el costo no son caja hoy).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Arqueo: conteo físico vs sistema.
            item {
                val last = cashCounts.firstOrNull()
                CashCard(
                    title = "ARQUEO DE CAJA",
                    subtitle = if (last == null) "Sin arqueos hoy"
                    else "Último: contado ${format.format(last.counted)} vs sistema ${format.format(last.expected)} (dif. ${format.format(last.diff)})",
                    rows = emptyList(),
                    emptyText = null
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = { showArqueo = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("🧮 Hacer arqueo", fontWeight = FontWeight.Medium)
                }
            }

            // Predicción simple 7 días + alerta de quiebre.
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Predicción 7 días",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Ritmo: entradas prom. ${format.format(state.avgDailySales)}/día − gastos ${format.format(state.projectedDailyExpenses)}/día. Basado en ${state.daysWithData} de 7 días con registro.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        state.projection.forEach { day ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    day.label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    format.format(day.projectedCash),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (day.isNegative) MaterialTheme.colorScheme.error
                                    else CustomOnSuccessVariant
                                )
                            }
                        }
                        if (state.breakEvenDay != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "⚠️ Al ritmo actual la caja cruza cero en ${state.breakEvenDay} días.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Predicción simple: supone ritmo promedio, sin nuevos aportes ni ventas a crédito.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showArqueo && state.hasLedger) {
        ArqueoDialog(
            expected = state.currentCash,
            format = format,
            onConfirm = { counted, note ->
                viewModel.saveCashCount(state.currentCash, counted, note)
                showArqueo = false
            },
            onDismiss = { showArqueo = false }
        )
    }
}

/**
 * Arqueo: el operador cuenta el efectivo físico y queda registrada la
 * diferencia contra el sistema. No ajusta la caja (la diferencia se investiga).
 */
@Composable
private fun ArqueoDialog(
    expected: Double,
    format: NumberFormat,
    onConfirm: (Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var countedStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val counted = countedStr.toDoubleOrNull()
    val diff = if (counted != null) counted - expected else null

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Arqueo de caja", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Sistema dice: ${format.format(expected)}. Cuente el efectivo físico.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = countedStr,
                    onValueChange = { countedStr = it },
                    label = { Text("Efectivo contado (CRC)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                if (diff != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Diferencia: ${format.format(diff)}" +
                            if (diff == 0.0) " ✓ cuadra" else " (investigar)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (diff == 0.0) CustomOnSuccessVariant
                        else MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { if (counted != null) onConfirm(counted, note) },
                enabled = counted != null && counted >= 0,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Guardar arqueo") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun CashCard(    title: String,
    subtitle: String? = null,
    rows: List<Pair<String, String>>,
    emptyText: String? = null,
    highlight: String? = null,
    highlightColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (rows.isEmpty() && emptyText != null) {
                Text(
                    emptyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (highlight != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    highlight,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = highlightColor
                )
            }
        }
    }
}
