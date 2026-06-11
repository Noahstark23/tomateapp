package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DailyLedger
import com.example.data.DashboardRepository
import com.example.finance.FinancialEngine
import com.example.finance.RunwayResult
import com.example.finance.SafeWithdrawalResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Día de la gráfica de fugas: cómo se descompone la venta de cada jornada. */
data class DailyBreakdown(
    val date: String,
    val label: String,
    val sales: Double,
    val cogs: Double,
    val expenses: Double,
    val waste: Double,
    val profit: Double
)

/** Estado consolidado del análisis CFO que consume el dashboard. */
data class CfoUiState(
    val hasLedger: Boolean = false,
    val cashOnHand: Double = 0.0,
    val safeWithdrawal: SafeWithdrawalResult? = null,
    val runway: RunwayResult? = null,
    val leakagePercent: Double = 0.0,
    val weeklyBreakdown: List<DailyBreakdown> = emptyList()
)

/**
 * ViewModel de análisis financiero ("CFO de bolsillo"). Observa los ledgers
 * recientes y deriva extracción segura, runway de quiebra y leakage usando
 * FinancialEngine (lógica pura, testeada en FinancialEngineTest).
 */
class FinancialViewModel(repository: DashboardRepository) : ViewModel() {

    private val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    val cfoState: StateFlow<CfoUiState> = repository.getRecentLedgers(ANALYSIS_WINDOW_DAYS)
        .map { ledgers -> buildState(ledgers) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CfoUiState()
        )

    private fun buildState(recentLedgers: List<DailyLedger>): CfoUiState {
        val today = recentLedgers.firstOrNull { it.date == currentDate }
            ?: return CfoUiState(hasLedger = false)

        // Extracción segura: caja de hoy menos reposición de lo vendido,
        // gastos proyectados y fondo de emergencia.
        val projectedExpenses = FinancialEngine.projectDailyExpenses(
            recentDailyExpenses = recentLedgers.map { it.total_expenses },
            fallback = today.total_expenses
        )
        val safeWithdrawal = FinancialEngine.computeSafeWithdrawal(
            cashOnHand = today.cash_on_hand,
            restockCost = today.total_cogs,
            projectedDailyExpenses = projectedExpenses
        )

        // Runway: capital líquido / quema promedio (gastos + merma) de la ventana.
        val runway = FinancialEngine.computeRunway(
            currentCapital = today.cash_on_hand,
            recentDailyOutflows = recentLedgers.map { it.total_expenses + it.total_waste_value }
        )

        // Leakage de la ventana completa: merma vs inventario consumido.
        val leakage = FinancialEngine.computeLeakage(
            totalWasteValue = recentLedgers.sumOf { it.total_waste_value },
            totalCogs = recentLedgers.sumOf { it.total_cogs }
        )

        // Gráfica en orden cronológico ascendente (el query viene descendente).
        val breakdown = recentLedgers
            .sortedBy { it.date }
            .map { ledger ->
                DailyBreakdown(
                    date = ledger.date,
                    label = ledger.date.takeLast(5), // MM-DD
                    sales = ledger.total_sales,
                    cogs = ledger.total_cogs,
                    expenses = ledger.total_expenses,
                    waste = ledger.total_waste_value,
                    profit = ledger.real_net_profit
                )
            }

        return CfoUiState(
            hasLedger = true,
            cashOnHand = today.cash_on_hand,
            safeWithdrawal = safeWithdrawal,
            runway = runway,
            leakagePercent = leakage,
            weeklyBreakdown = breakdown
        )
    }

    companion object {
        /** Ventana de análisis para burn rate, leakage y gráfica. */
        const val ANALYSIS_WINDOW_DAYS = 7
    }
}
