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

    fun getSalesByProduct(date: String): Flow<List<SalesByProduct>> =
        appDao.getSalesByProduct(date)

    suspend fun getSalesSummary(date: String): SalesSummary = appDao.getSalesSummary(date)

    fun getLedgersBetween(start: String, end: String): Flow<List<DailyLedger>> =
        appDao.getLedgersBetween(start, end)

    fun getCashSalesSeries(start: String, end: String): Flow<List<DateTotal>> =
        appDao.getCashSalesSeries(start, end)

    fun getCollectionsSeries(start: String, end: String): Flow<List<DateTotal>> =
        appDao.getCollectionsSeries(start, end)

    fun getClients(): Flow<List<Client>> = appDao.getClients()

    fun getInventory(): Flow<List<Inventory>> = appDao.getInventory()

    // --- Escrituras (transaccionales) --------------------------------------

    /** Inicia el día operativo o ajusta el capital de trabajo. */
    suspend fun setInitialInvestment(date: String, investment: Double) =
        appDao.setInitialInvestment(date, investment)

    /** Registra una venta congelando el costo vigente. Null si no hay stock o cupo. */
    suspend fun registerSale(
        date: String,
        clientId: Int,
        inventoryId: Int,
        quantity: Int,
        isCredit: Boolean = false
    ): Invoice? = appDao.processSale(date, clientId, inventoryId, quantity, isCredit)

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

    suspend fun insertClient(client: Client): Long = appDao.insertClient(client)

    /** Da de alta un cliente y lo devuelve con su id para seleccionarlo de una vez. */
    suspend fun addClient(name: String, phone: String, creditLimit: Double = 0.0): Client {
        val id = appDao.insertClient(
            Client(name = name, contact_info = phone, phone = phone, credit_limit = creditLimit)
        )
        return Client(id = id.toInt(), name = name, contact_info = phone, phone = phone, credit_limit = creditLimit)
    }

    suspend fun updateClient(client: Client) = appDao.updateClient(client)

    suspend fun setCreditLimit(clientId: Int, limit: Double) {
        val client = appDao.getClientByIdSync(clientId) ?: return
        appDao.updateClient(client.copy(credit_limit = limit))
    }

    suspend fun insertInventory(inventory: Inventory) = appDao.insertInventory(inventory)

    /** Alta de producto con stock inicial. */
    suspend fun addProduct(name: String, costPrice: Double, salePrice: Double, stock: Int) =
        appDao.insertInventory(
            Inventory(
                item_name = name,
                purchase_price = costPrice,
                sale_price = salePrice,
                initial_stock = stock,
                current_stock = stock
            )
        )

    /** Ajusta precios de venta/costo vigentes (no reescribe historia). */
    suspend fun updatePrices(inventoryId: Int, salePrice: Double, costPrice: Double) =
        appDao.updatePrices(inventoryId, salePrice, costPrice)

    /** Entrada de mercadería: suma stock inicial y actual. */
    suspend fun addStock(inventoryId: Int, quantity: Int) = appDao.addStock(inventoryId, quantity)

    // --- Créditos y cobranza -------------------------------------------------

    fun getCreditBalances(): Flow<List<ClientBalance>> = appDao.getCreditBalances()

    fun getOpenCreditInvoices(clientId: Int): Flow<List<Invoice>> =
        appDao.getOpenCreditInvoices(clientId)

    suspend fun getClientBalance(clientId: Int): Double = appDao.getClientBalanceSync(clientId)

    /** Abono que suma caja el día del pago. Null si monto inválido o sobrepago. */
    suspend fun registerPayment(
        date: String,
        clientId: Int,
        invoiceId: Int?,
        amount: Double
    ): Payment? = appDao.processPayment(date, clientId, invoiceId, amount)

    fun getPaymentsForDate(date: String): Flow<List<Payment>> = appDao.getPaymentsForDate(date)
}
