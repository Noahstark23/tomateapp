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

    fun startDay(investment: Double) {
        viewModelScope.launch {
            val ledger = DailyLedger(
                date = currentDate,
                initial_investment = investment
            )
            repository.insertLedger(ledger)
        }
    }
    
    fun addSale(amount: Double) {
        viewModelScope.launch {
            val invoice = Invoice(
                ledger_date = currentDate,
                client_id = 1, // Defaulting client
                total_amount = amount
            )
            repository.insertInvoice(invoice)
        }
    }
}

data class DashboardMetrics(
    val hasLedger: Boolean = false,
    val initialInvestment: Double = 0.0,
    val totalSales: Double = 0.0,
    val netProfit: Double = 0.0
)
