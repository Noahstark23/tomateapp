package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.Client
import com.example.data.DailyLedger
import com.example.data.DashboardRepository
import com.example.data.Expense
import com.example.data.ExpenseCategory
import com.example.data.Inventory
import com.example.data.Waste
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ViewModel operativo: iniciar el día, registrar ventas/gastos/mermas y
 * exponer el estado financiero del día. El análisis CFO (extracción segura,
 * runway, leakage) vive en FinancialViewModel.
 */
class DashboardViewModel(private val repository: DashboardRepository) : ViewModel() {

    private val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    val ledgerForToday: StateFlow<DailyLedger?> = repository.getLedgerForDate(currentDate)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val clients: StateFlow<List<Client>> = repository.getClients()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val inventory: StateFlow<List<Inventory>> = repository.getInventory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val historicalLedgers: StateFlow<List<DailyLedger>> = repository.getLedgers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val expensesForToday: StateFlow<List<Expense>> = repository.getExpensesForDate(currentDate)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val wasteForToday: StateFlow<List<Waste>> = repository.getWasteForDate(currentDate)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val lastSaleDetails = MutableStateFlow<LastSaleDetails?>(null)

    /**
     * Métricas del día derivadas del ledger materializado. La ganancia real
     * es Ventas - COGS - Gastos - Merma; la inversión inicial es capital de
     * trabajo, no un costo.
     */
    val currentMetrics: StateFlow<DashboardMetrics> = ledgerForToday
        .map { ledger ->
            if (ledger != null) {
                DashboardMetrics(
                    hasLedger = true,
                    initialInvestment = ledger.initial_investment,
                    totalSales = ledger.total_sales,
                    totalCogs = ledger.total_cogs,
                    grossProfit = ledger.total_sales - ledger.total_cogs,
                    totalExpenses = ledger.total_expenses,
                    totalWaste = ledger.total_waste_value,
                    realNetProfit = ledger.real_net_profit,
                    cashOnHand = ledger.cash_on_hand
                )
            } else {
                DashboardMetrics(hasLedger = false)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardMetrics()
        )

    fun setInitialInvestment(investment: Double) {
        viewModelScope.launch {
            repository.setInitialInvestment(currentDate, investment)
        }
    }

    fun processSale(client: Client, inventoryItem: Inventory, quantity: Int, isCredit: Boolean = false) {
        if (quantity > inventoryItem.current_stock || quantity <= 0) return

        viewModelScope.launch {
            val invoice = repository.registerSale(currentDate, client.id, inventoryItem.id, quantity, isCredit)
            if (invoice != null) {
                lastSaleDetails.value = LastSaleDetails(
                    clientName = client.name,
                    itemName = inventoryItem.item_name,
                    quantity = invoice.quantity,
                    salePrice = invoice.unit_price,
                    totalAmount = invoice.total_amount
                )
            }
        }
    }

    fun registerExpense(category: ExpenseCategory, amount: Double, description: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            repository.registerExpense(currentDate, category, amount, description)
        }
    }

    fun registerWaste(inventoryItem: Inventory, quantity: Int, reason: String) {
        if (quantity > inventoryItem.current_stock || quantity <= 0) return
        viewModelScope.launch {
            repository.registerWaste(currentDate, inventoryItem.id, quantity, reason)
        }
    }

    /**
     * Da de alta un cliente (nombre + teléfono ya sanitizado + cupo) y lo
     * devuelve con su id para poder seleccionarlo inmediatamente.
     */
    fun addClient(name: String, phone: String, creditLimit: Double = 0.0, onAdded: (Client) -> Unit = {}) {
        viewModelScope.launch {
            onAdded(repository.addClient(name, phone, creditLimit))
        }
    }

    fun setCreditLimit(clientId: Int, limit: Double) {
        viewModelScope.launch { repository.setCreditLimit(clientId, limit) }
    }

    // --- Créditos y cobranza --------------------------------------------------

    /** Saldo vivo por cobrar por cliente (derivado de facturas a crédito). */
    val creditBalances: StateFlow<Map<Int, Double>> = repository.getCreditBalances()
        .map { list -> list.associate { it.clientId to it.balance } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun openCreditInvoices(clientId: Int): StateFlow<List<com.example.data.Invoice>> =
        repository.getOpenCreditInvoices(clientId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Registra un abono (suma caja hoy). [onDone] recibe true si se guardó;
     * false = monto inválido o sobrepago.
     */
    fun registerPayment(clientId: Int, invoiceId: Int?, amount: Double, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            onDone(repository.registerPayment(currentDate, clientId, invoiceId, amount) != null)
        }
    }

    // --- Productos -------------------------------------------------------------

    fun addProduct(name: String, costPrice: Double, salePrice: Double, stock: Int) {
        if (name.isBlank() || costPrice < 0 || salePrice <= 0 || stock < 0) return
        viewModelScope.launch {
            repository.addProduct(name.trim(), costPrice, salePrice, stock)
        }
    }

    fun updatePrices(inventoryId: Int, salePrice: Double, costPrice: Double) {
        if (salePrice <= 0 || costPrice < 0) return
        viewModelScope.launch { repository.updatePrices(inventoryId, salePrice, costPrice) }
    }

    fun addStock(inventoryId: Int, quantity: Int) {
        if (quantity <= 0) return
        viewModelScope.launch { repository.addStock(inventoryId, quantity) }
    }

    // --- Exportar CSV para el contador (FE v4.4) --------------------------------

    /**
     * Genera el CSV del día (ventas, gastos, abonos) con columna CABYS vacía
     * para que el contador lo mapee a su facturador. Devuelve el Uri o null.
     */
    suspend fun exportDayCsv(context: android.content.Context): android.net.Uri? {
        return try {
            val invoices = repository.getInvoicesForDate(currentDate).first()
            val expenses = repository.getExpensesForDate(currentDate).first()
            val payments = repository.getPaymentsForDate(currentDate).first()
            val clients = repository.getClients().first().associateBy { it.id }
            val items = repository.getInventory().first().associateBy { it.id }
            val sb = StringBuilder()
            sb.append("tipo,fecha,documento,cliente,producto,cantidad,precio_unit,total,es_credito,cobrado,categoria,monto,descripcion,cabys\n")
            fun esc(v: String) = "\"" + v.replace("\"", "\"\"") + "\""
            invoices.forEach { inv ->
                sb.append(
                    listOf(
                        "VENTA", inv.ledger_date, inv.id.toString(),
                        esc(clients[inv.client_id]?.name ?: ""),
                        esc(items[inv.inventory_id]?.item_name ?: ""),
                        inv.quantity.toString(), inv.unit_price.toString(),
                        inv.total_amount.toString(),
                        if (inv.is_credit) "SI" else "NO", inv.paid_amount.toString(),
                        "", "", "", ""
                    ).joinToString(",")
                ).append("\n")
            }
            expenses.forEach { exp ->
                sb.append(
                    listOf(
                        "GASTO", exp.ledger_date, exp.id.toString(), "", "", "", "", "",
                        "", "", exp.category.name, exp.amount.toString(),
                        esc(exp.description), ""
                    ).joinToString(",")
                ).append("\n")
            }
            payments.forEach { pay ->
                sb.append(
                    listOf(
                        "ABONO", pay.ledger_date, pay.id.toString(),
                        esc(clients[pay.client_id]?.name ?: ""), "", "", "",
                        pay.amount.toString(), "", pay.amount.toString(), "", "", "", ""
                    ).joinToString(",")
                ).append("\n")
            }
            val dir = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
            val file = java.io.File(dir, "tomateapp_$currentDate.csv")
            file.writeText(sb.toString(), Charsets.UTF_8)
            androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
        } catch (e: Exception) {
            null
        }
    }

    // Datos semilla solo para desarrollo: en release la bodega empieza vacía
    // y el operador registra sus propios clientes e inventario.
    fun initTestData() {
        if (!BuildConfig.DEBUG) return
        viewModelScope.launch {
            if (clients.value.isEmpty()) {
                repository.insertClient(Client(name = "Client A", contact_info = "123456"))
                repository.insertClient(Client(name = "Client B", contact_info = "654321"))
            }
            if (inventory.value.isEmpty()) {
                repository.insertInventory(Inventory(item_name = "Caja de Tomate Primera", purchase_price = 5000.0, sale_price = 6000.0, initial_stock = 100, current_stock = 100))
                repository.insertInventory(Inventory(item_name = "Caja de Tomate Segunda", purchase_price = 3000.0, sale_price = 4500.0, initial_stock = 50, current_stock = 5))
            }
        }
    }
}

data class DashboardMetrics(
    val hasLedger: Boolean = false,
    val initialInvestment: Double = 0.0,
    val totalSales: Double = 0.0,
    val totalCogs: Double = 0.0,
    val grossProfit: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalWaste: Double = 0.0,
    val realNetProfit: Double = 0.0,
    val cashOnHand: Double = 0.0
)

data class LastSaleDetails(
    val clientName: String,
    val itemName: String,
    val quantity: Int,
    val salePrice: Double,
    val totalAmount: Double
)
