package com.example

import com.example.finance.BusinessHealth
import com.example.finance.FinancialEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialEngineTest {

    private val delta = 0.0001

    // --- Ganancia neta real -------------------------------------------------

    @Test
    fun `ganancia real descuenta cogs gastos y merma, no la inversion`() {
        // Ventas 100k, COGS 60k, gastos 15k, merma 5k => ganancia 20k
        val profit = FinancialEngine.realNetProfit(
            sales = 100_000.0, cogs = 60_000.0, expenses = 15_000.0, wasteValue = 5_000.0
        )
        assertEquals(20_000.0, profit, delta)
    }

    // --- Extracción segura --------------------------------------------------

    @Test
    fun `extraccion segura descuenta reposicion gastos y fondo de emergencia`() {
        // Caja 100k - (reposicion 40k + gastos 10k + fondo 15k) = 35k
        val result = FinancialEngine.computeSafeWithdrawal(
            cashOnHand = 100_000.0,
            restockCost = 40_000.0,
            projectedDailyExpenses = 10_000.0
        )
        assertEquals(15_000.0, result.emergencyFund, delta)
        assertEquals(35_000.0, result.amount, delta)
        assertTrue(result.isSafe)
    }

    @Test
    fun `extraccion negativa marca descapitalizacion`() {
        // Caja 50k - (reposicion 45k + gastos 10k + fondo 7.5k) = -12.5k
        val result = FinancialEngine.computeSafeWithdrawal(
            cashOnHand = 50_000.0,
            restockCost = 45_000.0,
            projectedDailyExpenses = 10_000.0
        )
        assertEquals(-12_500.0, result.amount, delta)
        assertFalse(result.isSafe)
    }

    @Test
    fun `con caja negativa el fondo de emergencia no se vuelve negativo`() {
        val result = FinancialEngine.computeSafeWithdrawal(
            cashOnHand = -10_000.0,
            restockCost = 0.0,
            projectedDailyExpenses = 0.0
        )
        assertEquals(0.0, result.emergencyFund, delta)
        assertEquals(-10_000.0, result.amount, delta)
        assertFalse(result.isSafe)
    }

    // --- Runway / prevención de quiebra --------------------------------------

    @Test
    fun `runway critico bajo 15 dias`() {
        // Burn promedio = 10k/día; capital 100k => 10 días => CRITICAL
        val result = FinancialEngine.computeRunway(
            currentCapital = 100_000.0,
            recentDailyOutflows = listOf(8_000.0, 12_000.0, 10_000.0)
        )
        assertEquals(10_000.0, result.dailyBurnRate, delta)
        assertEquals(10.0, result.runwayDays!!, delta)
        assertEquals(BusinessHealth.CRITICAL, result.health)
    }

    @Test
    fun `runway en advertencia entre 15 y 30 dias`() {
        // Burn 10k/día; capital 200k => 20 días => WARNING
        val result = FinancialEngine.computeRunway(
            currentCapital = 200_000.0,
            recentDailyOutflows = listOf(10_000.0)
        )
        assertEquals(20.0, result.runwayDays!!, delta)
        assertEquals(BusinessHealth.WARNING, result.health)
    }

    @Test
    fun `runway saludable sobre 30 dias`() {
        // Burn 10k/día; capital 400k => 40 días => HEALTHY
        val result = FinancialEngine.computeRunway(
            currentCapital = 400_000.0,
            recentDailyOutflows = listOf(10_000.0)
        )
        assertEquals(40.0, result.runwayDays!!, delta)
        assertEquals(BusinessHealth.HEALTHY, result.health)
    }

    @Test
    fun `sin quema de capital no hay runway finito`() {
        val result = FinancialEngine.computeRunway(
            currentCapital = 50_000.0,
            recentDailyOutflows = listOf(0.0, 0.0)
        )
        assertEquals(0.0, result.dailyBurnRate, delta)
        assertNull(result.runwayDays)
        assertEquals(BusinessHealth.HEALTHY, result.health)
    }

    @Test
    fun `caja negativa sin burn es critico`() {
        val result = FinancialEngine.computeRunway(
            currentCapital = -1_000.0,
            recentDailyOutflows = emptyList()
        )
        assertEquals(BusinessHealth.CRITICAL, result.health)
    }

    // --- Leakage Score --------------------------------------------------------

    @Test
    fun `leakage es la proporcion de merma sobre inventario consumido`() {
        // Merma 5k vs COGS 45k => 10% del inventario consumido se perdió
        assertEquals(10.0, FinancialEngine.computeLeakage(5_000.0, 45_000.0), delta)
    }

    @Test
    fun `leakage sin consumo es cero`() {
        assertEquals(0.0, FinancialEngine.computeLeakage(0.0, 0.0), delta)
    }

    @Test
    fun `leakage total cuando todo se pudre`() {
        assertEquals(100.0, FinancialEngine.computeLeakage(20_000.0, 0.0), delta)
    }

    // --- Proyección de gastos --------------------------------------------------

    @Test
    fun `proyeccion usa promedio de la ventana o el fallback`() {
        assertEquals(
            10_000.0,
            FinancialEngine.projectDailyExpenses(listOf(5_000.0, 15_000.0), fallback = 99.0),
            delta
        )
        assertEquals(
            99.0,
            FinancialEngine.projectDailyExpenses(emptyList(), fallback = 99.0),
            delta
        )
    }

    // --- Predicción simple de caja ---------------------------------------------

    @Test
    fun `proyeccion de caja suma el neto diario 7 dias`() {
        // Caja 150k, ventas promedio 12k, gastos 10k => neto +2k/día
        val projected = FinancialEngine.projectCash7Days(
            currentCash = 150_000.0, avgDailySales = 12_000.0, projectedDailyExpenses = 10_000.0
        )
        assertEquals(7, projected.size)
        assertEquals(152_000.0, projected[0], delta)
        assertEquals(164_000.0, projected[6], delta)
    }

    @Test
    fun `quiebre detecta el primer dia bajo cero`() {
        // Caja 10k, neto -4k/día => días: 6k, 2k, -2k => quiebre día 3
        val projected = FinancialEngine.projectCash7Days(
            currentCash = 10_000.0, avgDailySales = 0.0, projectedDailyExpenses = 4_000.0
        )
        assertEquals(3, FinancialEngine.findBreakEvenDay(projected))
    }

    @Test
    fun `sin quiebre devuelve null`() {
        val projected = FinancialEngine.projectCash7Days(
            currentCash = 150_000.0, avgDailySales = 12_000.0, projectedDailyExpenses = 10_000.0
        )
        assertNull(FinancialEngine.findBreakEvenDay(projected))
        assertNull(FinancialEngine.findBreakEvenDay(emptyList()))
    }
}
