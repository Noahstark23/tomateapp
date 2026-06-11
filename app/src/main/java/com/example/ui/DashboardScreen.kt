package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Client
import com.example.data.ExpenseCategory
import com.example.data.Inventory
import com.example.finance.BusinessHealth
import com.example.finance.RunwayResult
import com.example.finance.SafeWithdrawalResult
import com.example.printer.PrintService
import com.example.ui.theme.ChartWaste
import com.example.ui.theme.CustomOnSuccessContainer
import com.example.ui.theme.CustomOnSuccessVariant
import com.example.ui.theme.CustomSuccessBorder
import com.example.ui.theme.CustomSuccessContainer
import com.example.ui.theme.HealthAmber
import com.example.ui.theme.HealthAmberBorder
import com.example.ui.theme.HealthAmberContainer
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.HealthGreenBorder
import com.example.ui.theme.HealthGreenContainer
import com.example.ui.theme.HealthRed
import com.example.ui.theme.HealthRedBorder
import com.example.ui.theme.HealthRedContainer
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private enum class QuickAction { SALE, EXPENSE, WASTE, ADJUST_INVESTMENT }

private fun ExpenseCategory.displayName(): String = when (this) {
    ExpenseCategory.TRANSPORTE -> "Transporte"
    ExpenseCategory.SALARIO -> "Salario"
    ExpenseCategory.EMPAQUE -> "Empaque"
    ExpenseCategory.OTROS -> "Otros"
}

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, financialViewModel: FinancialViewModel) {
    val metrics by viewModel.currentMetrics.collectAsStateWithLifecycle()
    val cfoState by financialViewModel.cfoState.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()

    var activeDialog by remember { mutableStateOf<QuickAction?>(null) }

    val format = remember { NumberFormat.getCurrencyInstance(Locale("es", "CR")) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DashboardTopBar(runway = if (metrics.hasLedger) cfoState.runway else null)
        }
    ) { padding ->
        if (!metrics.hasLedger) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                StartDayCard(onSubmit = { viewModel.setInitialInvestment(it) })
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { BusinessHealthCard(runway = cfoState.runway) }
                item {
                    SafeWithdrawalCard(
                        result = cfoState.safeWithdrawal,
                        format = format
                    )
                }
                item {
                    KpiSection(
                        metrics = metrics,
                        leakagePercent = cfoState.leakagePercent,
                        format = format,
                        onAdjustInvestment = { activeDialog = QuickAction.ADJUST_INVESTMENT }
                    )
                }
                item {
                    LeakageChartCard(
                        breakdown = cfoState.weeklyBreakdown,
                        leakagePercent = cfoState.leakagePercent
                    )
                }
                item {
                    QuickActionsSection(
                        onSale = { activeDialog = QuickAction.SALE },
                        onExpense = { activeDialog = QuickAction.EXPENSE },
                        onWaste = { activeDialog = QuickAction.WASTE }
                    )
                }
            }
        }
    }

    when (activeDialog) {
        QuickAction.SALE -> SaleDialog(
            clients = clients,
            inventory = inventory,
            format = format,
            onConfirm = { client, item, qty -> viewModel.processSale(client, item, qty) },
            onDismiss = { activeDialog = null }
        )
        QuickAction.EXPENSE -> ExpenseDialog(
            onConfirm = { category, amount, description ->
                viewModel.registerExpense(category, amount, description)
            },
            onDismiss = { activeDialog = null }
        )
        QuickAction.WASTE -> WasteDialog(
            inventory = inventory,
            format = format,
            onConfirm = { item, qty, reason -> viewModel.registerWaste(item, qty, reason) },
            onDismiss = { activeDialog = null }
        )
        QuickAction.ADJUST_INVESTMENT -> AdjustInvestmentDialog(
            currentInvestment = metrics.initialInvestment,
            onConfirm = { viewModel.setInitialInvestment(it) },
            onDismiss = { activeDialog = null }
        )
        null -> Unit
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

@Composable
private fun DashboardTopBar(runway: RunwayResult?) {
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
                        Text("🍅", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Column {
                    Text(
                        "Director Financiero",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "TomateApp · CFO de bolsillo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Punto de estado que replica el semáforo financiero.
            val dotColor = when (runway?.health) {
                BusinessHealth.HEALTHY -> HealthGreen
                BusinessHealth.WARNING -> HealthAmber
                BusinessHealth.CRITICAL -> HealthRed
                null -> MaterialTheme.colorScheme.surfaceVariant
            }
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Semáforo: Salud del Negocio (algoritmo de prevención de quiebra)
// ---------------------------------------------------------------------------

private data class HealthPalette(val container: Color, val border: Color, val accent: Color)

@Composable
private fun BusinessHealthCard(runway: RunwayResult?) {
    val health = runway?.health ?: BusinessHealth.HEALTHY
    val palette = when (health) {
        BusinessHealth.HEALTHY -> HealthPalette(HealthGreenContainer, HealthGreenBorder, HealthGreen)
        BusinessHealth.WARNING -> HealthPalette(HealthAmberContainer, HealthAmberBorder, HealthAmber)
        BusinessHealth.CRITICAL -> HealthPalette(HealthRedContainer, HealthRedBorder, HealthRed)
    }
    val container = palette.container
    val border = palette.border
    val content = palette.accent
    val title = when (health) {
        BusinessHealth.HEALTHY -> "Negocio Saludable"
        BusinessHealth.WARNING -> "Atención: vigila tu liquidez"
        BusinessHealth.CRITICAL -> "⚠️ Riesgo de Quiebra"
    }
    val days = runway?.runwayDays
    val subtitle = when {
        runway == null -> "Inicia el día para analizar tu salud financiera"
        days == null -> "Sin quema de capital detectada en la última semana"
        health == BusinessHealth.CRITICAL ->
            "Tienes liquidez para ${days.roundToInt()} días al ritmo actual de gastos y mermas"
        else -> "Liquidez para ${days.roundToInt()} días al ritmo actual de gastos y mermas"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(content)
            )
            Column {
                Text(
                    "SALUD DEL NEGOCIO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = content,
                    letterSpacing = 1.sp
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = content
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = content
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Capital Extraíble (algoritmo de extracción segura)
// ---------------------------------------------------------------------------

@Composable
private fun SafeWithdrawalCard(result: SafeWithdrawalResult?, format: NumberFormat) {
    val isSafe = result?.isSafe == true
    val container = if (isSafe) CustomSuccessContainer else HealthRedContainer
    val border = if (isSafe) CustomSuccessBorder else HealthRedBorder
    val accent = if (isSafe) CustomOnSuccessContainer else HealthRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, border)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "CAPITAL EXTRAÍBLE HOY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                format.format(result?.amount ?: 0.0),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )

            if (result != null && !isSafe) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "🚫 No puedes retirar dinero, estás descapitalizando el negocio.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = HealthRed
                )
            }

            if (result != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = border)
                Spacer(modifier = Modifier.height(12.dp))
                BreakdownRow("Efectivo en caja", format.format(result.cashOnHand), accent)
                BreakdownRow("Reposición de inventario vendido", "- ${format.format(result.restockCost)}", accent)
                BreakdownRow("Gastos proyectados (próx. día)", "- ${format.format(result.projectedExpenses)}", accent)
                BreakdownRow("Fondo de emergencia (15%)", "- ${format.format(result.emergencyFund)}", accent)
            }
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = color)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = color)
    }
}

// ---------------------------------------------------------------------------
// KPIs del día
// ---------------------------------------------------------------------------

@Composable
private fun KpiSection(
    metrics: DashboardMetrics,
    leakagePercent: Double,
    format: NumberFormat,
    onAdjustInvestment: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard(
                modifier = Modifier.weight(1f),
                emoji = "🧾",
                label = "Ventas Hoy",
                value = format.format(metrics.totalSales)
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                emoji = "💰",
                label = "Ganancia Real",
                value = format.format(metrics.realNetProfit),
                valueColor = if (metrics.realNetProfit >= 0) CustomOnSuccessVariant else HealthRed
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard(
                modifier = Modifier.weight(1f),
                emoji = "💸",
                label = "Gastos Hoy",
                value = format.format(metrics.totalExpenses),
                valueColor = if (metrics.totalExpenses > 0) HealthAmber else MaterialTheme.colorScheme.onSurface
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                emoji = "🗑️",
                label = "Merma Hoy",
                value = format.format(metrics.totalWaste),
                valueColor = if (metrics.totalWaste > 0) ChartWaste else MaterialTheme.colorScheme.onSurface,
                detail = "Fuga 7d: ${String.format(Locale.US, "%.1f%%", leakagePercent)}"
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            border = BorderStroke(1.dp, com.example.ui.theme.CustomPrimaryBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "CAPITAL DE TRABAJO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        letterSpacing = 1.sp
                    )
                    Text(
                        format.format(metrics.initialInvestment),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Efectivo en caja: ${format.format(metrics.cashOnHand)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Button(
                    onClick = onAdjustInvestment,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ajustar", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    detail: String? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(emoji, style = MaterialTheme.typography.bodySmall)
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Gráfica de fugas y rentabilidad
// ---------------------------------------------------------------------------

@Composable
private fun LeakageChartCard(breakdown: List<DailyBreakdown>, leakagePercent: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "¿A dónde se va el dinero?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Ventas descompuestas en costo, gastos, merma y ganancia (últimos 7 días)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            LeakageStackedBarChart(days = breakdown)

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Fuga de inventario (merma vs. vendido)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    String.format(Locale.US, "%.1f%%", leakagePercent),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (leakagePercent > 10.0) HealthRed else CustomOnSuccessVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LeakageBar(leakagePercent = leakagePercent)
        }
    }
}

// ---------------------------------------------------------------------------
// Acciones rápidas
// ---------------------------------------------------------------------------

@Composable
private fun QuickActionsSection(
    onSale: () -> Unit,
    onExpense: () -> Unit,
    onWaste: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onSale,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("🧾  Registrar Venta", fontWeight = FontWeight.SemiBold)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onExpense,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("💸 Registrar Gasto", fontWeight = FontWeight.Medium)
            }
            OutlinedButton(
                onClick = onWaste,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("🗑️ Registrar Merma", fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Inicio del día
// ---------------------------------------------------------------------------

@Composable
fun StartDayCard(onSubmit: (Double) -> Unit) {
    var initialInvestmentStr by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Iniciar Día Operativo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Registra el capital de trabajo con el que abres hoy. No es un costo: es el efectivo inicial de tu caja.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = initialInvestmentStr,
                onValueChange = { initialInvestmentStr = it },
                label = { Text("Capital de Trabajo (CRC)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val amount = initialInvestmentStr.toDoubleOrNull()
                    if (amount != null) onSubmit(amount)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Iniciar Día", fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Diálogo: Registrar Venta (con impresión Bluetooth)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaleDialog(
    clients: List<Client>,
    inventory: List<Inventory>,
    format: NumberFormat,
    onConfirm: (Client, Inventory, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Permiso Bluetooth denegado", Toast.LENGTH_SHORT).show()
        }
    }

    var selectedClient by remember { mutableStateOf(clients.firstOrNull()) }
    var selectedInventory by remember { mutableStateOf(inventory.firstOrNull()) }
    var quantityStr by remember { mutableStateOf("") }
    var clientExpanded by remember { mutableStateOf(false) }
    var inventoryExpanded by remember { mutableStateOf(false) }

    val quantity = quantityStr.toIntOrNull() ?: 0
    val stockError = selectedInventory != null && quantity > (selectedInventory?.current_stock ?: 0)
    val canConfirm = quantity > 0 && !stockError && selectedClient != null && selectedInventory != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Venta", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (inventory.isEmpty()) {
                    Text(
                        "No hay productos en inventario. Agrégalos en la pestaña Gestión.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                ExposedDropdownMenuBox(
                    expanded = clientExpanded,
                    onExpandedChange = { clientExpanded = !clientExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedClient?.name ?: "Seleccione Cliente",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cliente") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = clientExpanded,
                        onDismissRequest = { clientExpanded = false }
                    ) {
                        clients.forEach { client ->
                            DropdownMenuItem(
                                text = { Text(client.name) },
                                onClick = {
                                    selectedClient = client
                                    clientExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = inventoryExpanded,
                    onExpandedChange = { inventoryExpanded = !inventoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedInventory?.item_name ?: "Seleccione Producto",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Producto (Stock: ${selectedInventory?.current_stock ?: 0})") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = inventoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = inventoryExpanded,
                        onDismissRequest = { inventoryExpanded = false }
                    ) {
                        inventory.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.item_name} - ${format.format(item.sale_price)}") },
                                onClick = {
                                    selectedInventory = item
                                    inventoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Cantidad") },
                    isError = stockError,
                    supportingText = if (stockError) {
                        { Text("Cantidad excede stock") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                val item = selectedInventory
                if (item != null && quantity > 0 && !stockError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val total = quantity * item.sale_price
                    val cost = quantity * item.purchase_price
                    Text(
                        "Total: ${format.format(total)} · Ganancia bruta: ${format.format(total - cost)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CustomOnSuccessVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val client = selectedClient ?: return@Button
                    val item = selectedInventory ?: return@Button
                    onConfirm(client, item, quantity)

                    val clientName = client.name
                    val itemName = item.item_name
                    val salePrice = item.sale_price
                    val totalAmount = quantity * salePrice

                    val printLogic = {
                        coroutineScope.launch {
                            val service = PrintService(context)
                            val success = service.printReceipt(clientName, itemName, quantity, salePrice, totalAmount)
                            Toast.makeText(
                                context,
                                if (success) "Impresión enviada" else "Error al imprimir. Verifique BT.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            printLogic()
                        } else {
                            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        }
                    } else {
                        printLogic()
                    }

                    onDismiss()
                },
                enabled = canConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Vender e Imprimir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ---------------------------------------------------------------------------
// Diálogo: Registrar Gasto
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDialog(
    onConfirm: (ExpenseCategory, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember { mutableStateOf(ExpenseCategory.TRANSPORTE) }
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val canConfirm = amount > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Gasto", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Los gastos salen de tu caja y reducen la ganancia real del día.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category.displayName(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        ExpenseCategory.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName()) },
                                onClick = {
                                    category = option
                                    categoryExpanded = false
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(category, amount, description)
                    onDismiss()
                },
                enabled = canConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Registrar Gasto")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ---------------------------------------------------------------------------
// Diálogo: Registrar Merma (tomate podrido/perdido)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WasteDialog(
    inventory: List<Inventory>,
    format: NumberFormat,
    onConfirm: (Inventory, Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedInventory by remember { mutableStateOf(inventory.firstOrNull()) }
    var quantityStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var inventoryExpanded by remember { mutableStateOf(false) }

    val quantity = quantityStr.toIntOrNull() ?: 0
    val stockError = selectedInventory != null && quantity > (selectedInventory?.current_stock ?: 0)
    val canConfirm = quantity > 0 && !stockError && selectedInventory != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Merma", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Producto dañado o perdido: descuenta stock y registra la pérdida a precio de costo (no toca tu caja).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (inventory.isEmpty()) {
                    Text(
                        "No hay productos en inventario. Agrégalos en la pestaña Gestión.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                ExposedDropdownMenuBox(
                    expanded = inventoryExpanded,
                    onExpandedChange = { inventoryExpanded = !inventoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedInventory?.item_name ?: "Seleccione Producto",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Producto (Stock: ${selectedInventory?.current_stock ?: 0})") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = inventoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = inventoryExpanded,
                        onDismissRequest = { inventoryExpanded = false }
                    ) {
                        inventory.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.item_name} - costo ${format.format(item.purchase_price)}") },
                                onClick = {
                                    selectedInventory = item
                                    inventoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Cantidad dañada") },
                    isError = stockError,
                    supportingText = if (stockError) {
                        { Text("Cantidad excede stock") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo (podrido, aplastado...)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                val item = selectedInventory
                if (item != null && quantity > 0 && !stockError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Pérdida financiera: ${format.format(quantity * item.purchase_price)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = HealthRed
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val item = selectedInventory ?: return@Button
                    onConfirm(item, quantity, reason)
                    onDismiss()
                },
                enabled = canConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Registrar Pérdida")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ---------------------------------------------------------------------------
// Diálogo: Ajustar capital de trabajo
// ---------------------------------------------------------------------------

@Composable
private fun AdjustInvestmentDialog(
    currentInvestment: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var investmentStr by remember { mutableStateOf(currentInvestment.toString()) }
    val amount = investmentStr.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustar Capital de Trabajo", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Corrige el efectivo con el que iniciaste el día. La caja y los indicadores se recalculan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = investmentStr,
                    onValueChange = { investmentStr = it },
                    label = { Text("Capital de Trabajo (CRC)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amount != null) onConfirm(amount)
                    onDismiss()
                },
                enabled = amount != null && amount >= 0,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
