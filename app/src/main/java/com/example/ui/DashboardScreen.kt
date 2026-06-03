package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val metrics by viewModel.currentMetrics.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.initTestData()
    }

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
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("📊", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        Column {
                            Text("Dashboard Diario", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                            Text("S24 Ultra Optimizado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🔔")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (!metrics.hasLedger) {
                StartDayCard(onSubmit = { viewModel.setInitialInvestment(it) })
            } else {
                DashboardMetricsView(
                    metrics = metrics,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun StartDayCard(onSubmit: (Double) -> Unit) {
    var initialInvestmentStr by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
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
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = initialInvestmentStr,
                onValueChange = { initialInvestmentStr = it },
                label = { Text("Inversión Inicial (CRC)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val amount = initialInvestmentStr.toDoubleOrNull()
                    if (amount != null) onSubmit(amount)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text("Registrar Inversión", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardMetricsView(metrics: DashboardMetrics, viewModel: DashboardViewModel) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            android.widget.Toast.makeText(context, "Permiso Bluetooth denegado", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    var selectedClient by remember { mutableStateOf<com.example.data.Client?>(null) }
    var selectedInventory by remember { mutableStateOf<com.example.data.Inventory?>(null) }
    var quantityStr by remember { mutableStateOf("") }
    
    // Auto-select first item if available
    LaunchedEffect(clients) {
        if (selectedClient == null && clients.isNotEmpty()) {
            selectedClient = clients.first()
        }
    }
    LaunchedEffect(inventory) {
        if (selectedInventory == null && inventory.isNotEmpty()) {
            selectedInventory = inventory.first()
        }
    }

    var clientExpanded by remember { mutableStateOf(false) }
    var inventoryExpanded by remember { mutableStateOf(false) }
    var stockError by remember { mutableStateOf(false) }

    val format = remember { NumberFormat.getCurrencyInstance(Locale("es", "CR")) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Inversión Inicial Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.CustomPrimaryBorder)
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
                        "INVERSIÓN INICIAL",
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
                }
                Button(
                    onClick = { /* TODO: Adjust logic */ },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text("Ajustar", fontWeight = FontWeight.Medium)
                }
            }
        }

        // Metrics Grid (Facturación and Ganancia)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Facturación
            Card(
                modifier = Modifier.weight(1f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🧾", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Facturación",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        format.format(metrics.totalSales),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Ganancia Neta
            Card(
                modifier = Modifier.weight(1f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.CustomSuccessContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.CustomSuccessBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("💰", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Ganancia Neta",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = com.example.ui.theme.CustomOnSuccessVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        format.format(metrics.netProfit),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.CustomOnSuccessContainer
                    )
                }
            }
        }

        // Registrar Venta (Nueva Venta)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Nueva Venta", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                
                Column(modifier = Modifier.weight(1f).padding(bottom = 8.dp)) {
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
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
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
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
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
                                        stockError = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { 
                            quantityStr = it
                            val qty = it.toIntOrNull() ?: 0
                            stockError = selectedInventory != null && qty > selectedInventory!!.current_stock
                        },
                        label = { Text("Cantidad") },
                        isError = stockError,
                        supportingText = if (stockError) { { Text("Cantidad excede stock") } } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Button(
                        onClick = {
                            val qty = quantityStr.toIntOrNull()
                            if (qty != null && selectedClient != null && selectedInventory != null && !stockError) {
                                viewModel.processSale(selectedClient!!, selectedInventory!!, qty)
                                
                                val clientName = selectedClient!!.name
                                val itemName = selectedInventory!!.item_name
                                val salePrice = selectedInventory!!.sale_price
                                val totalAmount = qty * salePrice
                                
                                val printLogic = {
                                    coroutineScope.launch {
                                        val service = com.example.printer.PrintService(context)
                                        val success = service.printReceipt(clientName, itemName, qty, salePrice, totalAmount)
                                        android.widget.Toast.makeText(context, if (success) "Impresión enviada" else "Error al imprimir. Verifique BT.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                                
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        printLogic()
                                    } else {
                                        permissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
                                    }
                                } else {
                                    printLogic()
                                }

                                quantityStr = ""
                            }
                        },
                        enabled = !stockError && quantityStr.isNotEmpty() && selectedClient != null && selectedInventory != null,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Text("Vender e Imprimir", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}
