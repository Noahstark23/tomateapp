package com.example.data

import kotlinx.coroutines.flow.Flow

/**
 * Fachada de dominio sobre AppDao. Las operaciones de escritura delegan en
 * transacciones del DAO que mantienen el ledger diario consistente con las
 * tablas fuente (invoices/expenses/waste).
 */
class DashboardRepository(private val appDao: AppDao) {

    // --- Lecturas -----------------------------------------------------------

    fun getLedgerForDate(date: String): Flow<DailyLedger?> = appDao.getLedgerForDate(date)

    fun getLedgers(): Flow<List<DailyLedger>> = appDao.getLedgers()

    fun getRecentLedgers(limit: Int): Flow<List<DailyLedger>> = appDao.getRecentLedgers(limit)

    fun getInvoicesForDate(date: String): Flow<List<Invoice>> = appDao.getInvoicesForDate(date)

    fun getExpensesForDate(date: String): Flow<List<Expense>> = appDao.getExpensesForDate(date)

    fun getExpenseTotalsByCategory(date: String): Flow<List<CategoryTotal>> =
        appDao.getExpenseTotalsByCategory(date)

    fun getWasteForDate(date: String): Flow<List<Waste>> = appDao.getWasteForDate(date)

    fun getClients(): Flow<List<Client>> = appDao.getClients()

    fun getInventory(): Flow<List<Inventory>> = appDao.getInventory()

    // --- Escrituras (transaccionales) --------------------------------------

    /** Inicia el día operativo o ajusta el capital de trabajo. */
    suspend fun setInitialInvestment(date: String, investment: Double) =
        appDao.setInitialInvestment(date, investment)

    /** Registra una venta congelando el costo vigente. Null si no hay stock. */
    suspend fun registerSale(date: String, clientId: Int, inventoryId: Int, quantity: Int): Invoice? =
        appDao.processSale(date, clientId, inventoryId, quantity)

    /** Registra un gasto operativo (reduce caja y ganancia real). */
    suspend fun registerExpense(
        date: String,
        category: ExpenseCategory,
        amount: Double,
        description: String
    ) = appDao.processExpense(
        Expense(
            ledger_date = date,
            category = category,
            amount = amount,
            description = description
        )
    )

    /** Registra merma valorada a costo (reduce stock y ganancia, no caja). Null si no hay stock. */
    suspend fun registerWaste(date: String, inventoryId: Int, quantity: Int, reason: String): Waste? =
        appDao.processWaste(date, inventoryId, quantity, reason)

    suspend fun insertClient(client: Client) = appDao.insertClient(client)

    suspend fun insertInventory(inventory: Inventory) = appDao.insertInventory(inventory)
}
