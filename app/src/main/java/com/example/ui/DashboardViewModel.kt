package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    init {
        // Cliente de mostrador para ventas al contado; idempotente contra la BD.
        viewModelScope.launch { repository.ensureDefaultClient() }
    }

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

    fun processSale(client: Client, inventoryItem: Inventory, quantity: Int) {
        if (quantity > inventoryItem.current_stock || quantity <= 0) return

        viewModelScope.launch {
            val invoice = repository.registerSale(currentDate, client.id, inventoryItem.id, quantity)
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

    // --- Gestión de inventario ----------------------------------------------

    /**
     * Alta de producto. El stock inicial es mercancía que ya posees (como la
     * inversión inicial es efectivo que ya posees): NO descuenta caja. Las
     * reposiciones posteriores se registran como compras, que sí la descuentan.
     */
    fun addProduct(name: String, purchasePrice: Double, salePrice: Double, initialStock: Int) {
        if (name.isBlank() || purchasePrice < 0 || salePrice < 0 || initialStock < 0) return
        viewModelScope.launch {
            repository.insertInventory(
                Inventory(
                    item_name = name.trim(),
                    purchase_price = purchasePrice,
                    sale_price = salePrice,
                    initial_stock = initialStock,
                    current_stock = initialStock
                )
            )
        }
    }

    fun updateProduct(item: Inventory) {
        if (item.item_name.isBlank() || item.purchase_price < 0 || item.sale_price < 0) return
        viewModelScope.launch {
            repository.updateInventory(item.copy(item_name = item.item_name.trim()))
        }
    }

    /** Compra/reposición: aumenta stock (costo promedio ponderado) y descuenta caja. */
    fun restockProduct(item: Inventory, quantity: Int, unitCost: Double) {
        if (quantity <= 0 || unitCost < 0) return
        viewModelScope.launch {
            repository.registerPurchase(currentDate, item.id, quantity, unitCost)
        }
    }

    // --- Gestión de clientes --------------------------------------------------

    fun addClient(name: String, contactInfo: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertClient(Client(name = name.trim(), contact_info = contactInfo.trim()))
        }
    }

    fun updateClient(client: Client) {
        if (client.name.isBlank()) return
        viewModelScope.launch {
            repository.updateClient(client.copy(name = client.name.trim(), contact_info = client.contact_info.trim()))
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
