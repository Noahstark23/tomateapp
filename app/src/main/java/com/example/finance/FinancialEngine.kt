package com.example.finance

import kotlin.math.max

/** Semáforo de salud del negocio derivado del runway de liquidez. */
enum class BusinessHealth { HEALTHY, WARNING, CRITICAL }

/**
 * Resultado del análisis de extracción segura.
 * amount puede ser negativo: retirar dinero descapitalizaría el negocio.
 */
data class SafeWithdrawalResult(
    val amount: Double,
    val cashOnHand: Double,
    val restockCost: Double,
    val projectedExpenses: Double,
    val emergencyFund: Double
) {
    val isSafe: Boolean get() = amount > 0.0
}

/**
 * Resultado del análisis de quema de capital.
 * runwayDays == null significa que no hay quema (burn rate cero).
 */
data class RunwayResult(
    val dailyBurnRate: Double,
    val runwayDays: Double?,
    val health: BusinessHealth
)

/**
 * Motor de análisis financiero de TomateApp. Kotlin puro y sin estado:
 * todas las funciones son deterministas y testeables sin Android.
 */
object FinancialEngine {

    /** Porcentaje del efectivo reservado como colchón ante imprevistos. */
    const val DEFAULT_EMERGENCY_FUND_RATE = 0.15

    /** Menos de 15 días de liquidez = riesgo de quiebra (alerta roja). */
    const val CRITICAL_RUNWAY_DAYS = 15.0

    /** Menos de 30 días de liquidez = precaución (alerta amarilla). */
    const val HEALTHY_RUNWAY_DAYS = 30.0

    /**
     * Ganancia neta real: Ventas - COGS - Gastos operativos - Merma.
     * La inversión inicial NO se resta: es capital de trabajo, no un costo.
     */
    fun realNetProfit(sales: Double, cogs: Double, expenses: Double, wasteValue: Double): Double =
        sales - cogs - expenses - wasteValue

    /**
     * Extracción Segura: cuánto efectivo puede retirar el dueño hoy sin
     * comprometer la operación de mañana.
     *
     *   Extracción = Caja - (Reposición del inventario vendido
     *                        + Gastos fijos proyectados
     *                        + Fondo de emergencia [% de la caja])
     *
     * @param cashOnHand efectivo real en caja hoy.
     * @param restockCost costo de reponer lo vendido hoy (COGS del día).
     * @param projectedDailyExpenses gastos fijos esperados para mañana.
     */
    fun computeSafeWithdrawal(
        cashOnHand: Double,
        restockCost: Double,
        projectedDailyExpenses: Double,
        emergencyFundRate: Double = DEFAULT_EMERGENCY_FUND_RATE
    ): SafeWithdrawalResult {
        val emergencyFund = max(cashOnHand, 0.0) * emergencyFundRate
        val amount = cashOnHand - (restockCost + projectedDailyExpenses + emergencyFund)
        return SafeWithdrawalResult(
            amount = amount,
            cashOnHand = cashOnHand,
            restockCost = restockCost,
            projectedExpenses = projectedDailyExpenses,
            emergencyFund = emergencyFund
        )
    }

    /**
     * Prevención de quiebra: burn rate = promedio diario de (gastos + merma)
     * de los últimos días; runway = capital actual / burn rate.
     *
     * @param currentCapital capital líquido disponible (caja).
     * @param recentDailyOutflows fuga diaria (gastos + merma) de los últimos
     *        días, típicamente una ventana de 7.
     */
    fun computeRunway(currentCapital: Double, recentDailyOutflows: List<Double>): RunwayResult {
        val burnRate = if (recentDailyOutflows.isEmpty()) 0.0 else recentDailyOutflows.average()
        if (burnRate <= 0.0) {
            val health = if (currentCapital >= 0.0) BusinessHealth.HEALTHY else BusinessHealth.CRITICAL
            return RunwayResult(dailyBurnRate = 0.0, runwayDays = null, health = health)
        }
        val days = max(currentCapital, 0.0) / burnRate
        val health = when {
            days < CRITICAL_RUNWAY_DAYS -> BusinessHealth.CRITICAL
            days < HEALTHY_RUNWAY_DAYS -> BusinessHealth.WARNING
            else -> BusinessHealth.HEALTHY
        }
        return RunwayResult(dailyBurnRate = burnRate, runwayDays = days, health = health)
    }

    /**
     * Leakage Score: porcentaje del capital de inventario consumido que se
     * perdió en merma en lugar de salir vendido.
     *
     *   Leakage = merma / (merma + COGS) * 100
     *
     * 0% = todo el inventario consumido se vendió; 100% = todo se pudrió.
     */
    fun computeLeakage(totalWasteValue: Double, totalCogs: Double): Double {
        val consumed = totalWasteValue + totalCogs
        if (consumed <= 0.0) return 0.0
        return totalWasteValue / consumed * 100.0
    }

    /**
     * Proyección de gastos fijos diarios: promedio de la ventana reciente,
     * o el fallback (gastos de hoy) si aún no hay historia.
     */
    fun projectDailyExpenses(recentDailyExpenses: List<Double>, fallback: Double): Double =
        if (recentDailyExpenses.isEmpty()) fallback else recentDailyExpenses.average()

    /**
     * Costo promedio ponderado (WAC) tras una compra de inventario: mezcla el
     * lote existente con el nuevo para valorar COGS y mermas futuras sin
     * corromper la historia (las facturas ya congelan su costo al vender).
     *
     *   WAC = (stock*costo_actual + compra*costo_nuevo) / (stock + compra)
     */
    fun weightedAverageCost(
        currentQty: Int,
        currentUnitCost: Double,
        addedQty: Int,
        addedUnitCost: Double
    ): Double {
        // Un stock corrupto (negativo) no debe envenenar el promedio.
        val safeCurrentQty = max(currentQty, 0)
        val totalQty = safeCurrentQty + addedQty
        if (totalQty <= 0) return addedUnitCost
        return (safeCurrentQty * currentUnitCost + addedQty * addedUnitCost) / totalQty
    }
}
