package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DailyLedger
import com.example.data.DashboardRepository
import com.example.data.Invoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DashboardViewModel(private val repository: DashboardRepository) : ViewModel() {

    private val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    val ledgerForToday: StateFlow<DailyLedger?> = repository.getLedgerForDate(currentDate)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val totalSalesForToday: StateFlow<Double> = repository.getTotalSalesForDate(currentDate)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val clients: StateFlow<List<com.example.data.Client>> = repository.getClients()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val inventory: StateFlow<List<com.example.data.Inventory>> = repository.getInventory()
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

    val lastSaleDetails = MutableStateFlow<LastSaleDetails?>(null)

    val currentMetrics: StateFlow<DashboardMetrics> = combine(
        ledgerForToday,
        totalSalesForToday
    ) { ledger, totalSales ->
        if (ledger != null) {
            val netProfit = totalSales - ledger.initial_investment
            DashboardMetrics(
                hasLedger = true,
                initialInvestment = ledger.initial_investment,
                totalSales = totalSales,
                netProfit = netProfit
            )
        } else {
            DashboardMetrics(hasLedger = false)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardMetrics()
    )

    fun setInitialInvestment(investment: Double) {
        viewModelScope.launch {
            val existing = repository.getLedgerForDateSync(currentDate)
            if (existing == null) {
                val ledger = DailyLedger(
                    date = currentDate,
                    initial_investment = investment
                )
                repository.insertLedger(ledger)
            } else {
                repository.updateLedger(existing.copy(initial_investment = investment))
            }
        }
    }
    
    fun processSale(client: com.example.data.Client, inventoryItem: com.example.data.Inventory, quantity: Int) {
        if (quantity > inventoryItem.current_stock || quantity <= 0) return
        
        viewModelScope.launch {
            val totalAmount = quantity * inventoryItem.sale_price
            val invoice = Invoice(
                ledger_date = currentDate,
                client_id = client.id,
                total_amount = totalAmount
            )
            repository.processSale(invoice, inventoryItem.id, quantity)
            
            lastSaleDetails.value = LastSaleDetails(
                clientName = client.name,
                itemName = inventoryItem.item_name,
                quantity = quantity,
                salePrice = inventoryItem.sale_price,
                totalAmount = totalAmount
            )
        }
    }

    // Default data initializers for testing
    fun initTestData() {
        viewModelScope.launch {
            if (clients.value.isEmpty()) {
                repository.insertClient(com.example.data.Client(name = "Client A", contact_info = "123456"))
                repository.insertClient(com.example.data.Client(name = "Client B", contact_info = "654321"))
            }
            if (inventory.value.isEmpty()) {
                repository.insertInventory(com.example.data.Inventory(item_name = "Caja de Tomate Primera", purchase_price = 5000.0, sale_price = 6000.0, initial_stock = 100, current_stock = 100))
                repository.insertInventory(com.example.data.Inventory(item_name = "Caja de Tomate Segunda", purchase_price = 3000.0, sale_price = 4500.0, initial_stock = 50, current_stock = 5))
            }
        }
    }
}

data class DashboardMetrics(
    val hasLedger: Boolean = false,
    val initialInvestment: Double = 0.0,
    val totalSales: Double = 0.0,
    val netProfit: Double = 0.0
)

data class LastSaleDetails(
    val clientName: String,
    val itemName: String,
    val quantity: Int,
    val salePrice: Double,
    val totalAmount: Double
)
