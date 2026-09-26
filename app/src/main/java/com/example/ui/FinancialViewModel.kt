package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CategoryTotal
import com.example.data.DailyLedger
import com.example.data.DashboardRepository
import com.example.data.Invoice
import com.example.data.SalesByProduct
import com.example.finance.FinancialEngine
import com.example.finance.RunwayResult
import com.example.finance.SafeWithdrawalResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

/** Caja proyectada de un día futuro (predicción simple, solo memoria). */
data class CashDayProjection(
    val date: String,
    val label: String,
    val projectedCash: Double,
    val isNegative: Boolean
)

/**
 * Estado del tab Flujo de Caja: todo se lee del ledger materializado y de
 * agregados de solo lectura; la proyección nunca escribe en la BD.
 */
data class CashFlowUiState(
    val hasLedger: Boolean = false,
    val initialCash: Double = 0.0,
    val currentCash: Double = 0.0,
    val netCashToday: Double = 0.0,
    val totalSales: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val realNetProfit: Double = 0.0,
    val salesByProduct: List<SalesByProduct> = emptyList(),
    val expensesByCategory: List<CategoryTotal> = emptyList(),
    val invoiceCount: Int = 0,
    val avgTicket: Double = 0.0,
    val avgDailySales: Double = 0.0,
    val projectedDailyExpenses: Double = 0.0,
    val projection: List<CashDayProjection> = emptyList(),
    val breakEvenDay: Int? = null,
    val daysWithData: Int = 0,
    val runway: RunwayResult? = null
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

    /**
     * Flujo de caja del día + proyección 7 días. Combina el ledger de hoy con
     * la ventana reciente y los desgloses por producto/categoría.
     */
    val cashFlowState: StateFlow<CashFlowUiState> = combine(
        repository.getLedgerForDate(currentDate),
        repository.getRecentLedgers(ANALYSIS_WINDOW_DAYS),
        repository.getSalesByProduct(currentDate),
        repository.getExpenseTotalsByCategory(currentDate),
        repository.getInvoicesForDate(currentDate)
    ) { today, recent, byProduct, byCategory, invoices ->
        buildCashState(today, recent, byProduct, byCategory, invoices)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CashFlowUiState()
    )

    private fun buildCashState(
        today: DailyLedger?,
        recentLedgers: List<DailyLedger>,
        byProduct: List<SalesByProduct>,
        byCategory: List<CategoryTotal>,
        invoices: List<Invoice>
    ): CashFlowUiState {
        if (today == null) return CashFlowUiState(hasLedger = false)

        // Promedio móvil de ventas: divisor 7 calendario (días sin ledger = 0)
        // para no inflar el promedio cuando hay días sin registro.
        val salesByDate = recentLedgers.associate { it.date to it.total_sales }
        var daysWithData = 0
        var salesSum = 0.0
        for (i in 0 until ANALYSIS_WINDOW_DAYS) {
            val day = LocalDate.now().minusDays(i.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val sales = salesByDate[day]
            if (sales != null) {
                daysWithData++
                salesSum += sales
            }
        }
        val avgDailySales = salesSum / ANALYSIS_WINDOW_DAYS

        val projectedExpenses = FinancialEngine.projectDailyExpenses(
            recentDailyExpenses = recentLedgers.map { it.total_expenses },
            fallback = today.total_expenses
        )
        val projected = FinancialEngine.projectCash7Days(
            currentCash = today.cash_on_hand,
            avgDailySales = avgDailySales,
            projectedDailyExpenses = projectedExpenses
        )
        val projection = projected.mapIndexed { index, cash ->
            val date = LocalDate.now().plusDays((index + 1).toLong())
            CashDayProjection(
                date = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                label = date.format(DateTimeFormatter.ofPattern("dd/MM")),
                projectedCash = cash,
                isNegative = cash < 0.0
            )
        }

        val runway = FinancialEngine.computeRunway(
            currentCapital = today.cash_on_hand,
            recentDailyOutflows = recentLedgers.map { it.total_expenses + it.total_waste_value }
        )

        return CashFlowUiState(
            hasLedger = true,
            initialCash = today.initial_investment,
            currentCash = today.cash_on_hand,
            // Invariante: neto caja = ventas - gastos (la merma y el COGS no tocan caja).
            netCashToday = today.total_sales - today.total_expenses,
            totalSales = today.total_sales,
            totalExpenses = today.total_expenses,
            realNetProfit = today.real_net_profit,
            salesByProduct = byProduct,
            expensesByCategory = byCategory.filter { it.total > 0.0 },
            invoiceCount = invoices.size,
            avgTicket = if (invoices.isNotEmpty()) invoices.sumOf { it.total_amount } / invoices.size else 0.0,
            avgDailySales = avgDailySales,
            projectedDailyExpenses = projectedExpenses,
            projection = projection,
            breakEvenDay = FinancialEngine.findBreakEvenDay(projected),
            daysWithData = daysWithData,
            runway = runway
        )
    }

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
