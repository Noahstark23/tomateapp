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

    /** Registra una venta congelando costo y precio (canal). Null si no hay stock o cupo. */
    suspend fun registerSale(
        date: String,
        clientId: Int,
        inventoryId: Int,
        quantity: Int,
        isCredit: Boolean = false,
        warehouseId: Int,
        unitPrice: Double? = null
    ): Invoice? = appDao.processSale(
        date, clientId, inventoryId, quantity, isCredit, warehouseId, unitPrice
    )

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
    suspend fun registerWaste(
        date: String,
        inventoryId: Int,
        quantity: Int,
        reason: String,
        causa: String = "",
        etapa: String = "",
        warehouseId: Int
    ): Waste? = appDao.processWaste(date, inventoryId, quantity, reason, causa, etapa, warehouseId)

    fun getExpiringLots(upto: String): Flow<List<LotAlert>> = appDao.getExpiringLots(upto)

    fun getWasteByCause(date: String): Flow<List<WasteCauseTotal>> = appDao.getWasteByCause(date)

    suspend fun insertClient(client: Client): Long = appDao.insertClient(client)

    /** Semillas solo si las tablas están vacías (transaccional, sin carreras). */
    suspend fun ensureSeeds() = appDao.ensureSeeds()

    /** Da de alta un cliente y lo devuelve con su id para seleccionarlo de una vez. */
    suspend fun addClient(name: String, phone: String, creditLimit: Double = 0.0, type: String = "TRAMO"): Client {
        val id = appDao.insertClient(
            Client(name = name, contact_info = phone, phone = phone, credit_limit = creditLimit, type = type)
        )
        return Client(id = id.toInt(), name = name, contact_info = phone, phone = phone, credit_limit = creditLimit, type = type)
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

    /** Entrada de mercadería: suma stock global y de la bodega dada. */
    suspend fun addStock(inventoryId: Int, quantity: Int, warehouseId: Int) =
        appDao.stockEntry(inventoryId, quantity, warehouseId)

    suspend fun updateCatalog(inventoryId: Int, cabys: String, unit: String) =
        appDao.updateCatalog(inventoryId, cabys, unit)

    // --- Lotes ----------------------------------------------------------------

    fun getLotsForProduct(inventoryId: Int): Flow<List<Lot>> =
        appDao.getLotsForProduct(inventoryId)    /**
     * Registra entrada de lote con vencimiento (fecha_limite = ingreso + días
     * de vida útil) y suma el stock. Devuelve el id del lote.
     */
    suspend fun registerLot(
        date: String,
        inventoryId: Int,
        warehouseId: Int,
        supplier: String,
        variedad: String,
        calibre: String,
        calidad: String,
        quantity: Int,
        costTotal: Double,
        shelfLifeDays: Int
    ): Long {
        val ingreso = java.time.LocalDate.parse(date)
        val limite = ingreso.plusDays(shelfLifeDays.coerceAtLeast(1).toLong())
        return appDao.registerLotEntry(
            inventoryId,
            warehouseId,
            Lot(
                inventory_id = inventoryId,
                warehouse_id = warehouseId,
                supplier = supplier,
                variedad = variedad,
                calibre = calibre,
                calidad = calidad,
                cost_total = costTotal,
                fecha_ingreso = date,
                fecha_limite = limite.toString()
            ),
            quantity
        )
    }

    // --- FE v4.4 ----------------------------------------------------------------
    fun getInvoiceDetails(date: String): Flow<List<InvoiceDetail>> =
        appDao.getInvoiceDetails(date)
    suspend fun getInvoiceById(id: Int) = appDao.getInvoiceByIdSync(id)

    suspend fun getClientById(id: Int) = appDao.getClientByIdSync(id)

    suspend fun getInventoryById(id: Int) = appDao.getInventoryByIdSync(id)

    suspend fun setFeStatus(invoiceId: Int, status: String) =
        appDao.setFeStatus(invoiceId, status)

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

    // --- Compras y arqueo ----------------------------------------------------------

    suspend fun registerPurchase(
        date: String,
        inventoryId: Int,
        warehouseId: Int,
        supplier: String,
        variedad: String,
        calibre: String,
        calidad: String,
        quantity: Int,
        costTotal: Double,
        shelfLifeDays: Int
    ): Long? = appDao.processPurchase(
        date, inventoryId, warehouseId, supplier, variedad, calibre, calidad,
        quantity, costTotal, shelfLifeDays
    )

    suspend fun insertCashCount(count: CashCount): Long = appDao.insertCashCount(count)

    fun getCashCounts(date: String): Flow<List<CashCount>> = appDao.getCashCounts(date)

    // --- Turnos ------------------------------------------------------------------------

    suspend fun openShift(date: String, openingCash: Double): Long =
        appDao.insertShift(CashShift(ledger_date = date, opening_cash = openingCash))

    suspend fun closeShift(shift: CashShift, expectedCash: Double, countedCash: Double, note: String) =
        appDao.updateShift(
            shift.copy(
                closed_at = System.currentTimeMillis(),
                expected_cash = expectedCash,
                counted_cash = countedCash,
                diff = countedCash - expectedCash,
                note = note.trim()
            )
        )

    fun getOpenShift(): Flow<CashShift?> = appDao.getOpenShift()

    fun getShifts(date: String): Flow<List<CashShift>> = appDao.getShifts(date)

    // --- Precios por canal ---------------------------------------------------------------

    fun getPriceRules(): Flow<List<PriceRule>> = appDao.getPriceRules()

    suspend fun savePriceRule(inventoryId: Int, channel: String, price: Double?) {
        if (price == null || price <= 0) appDao.deletePriceRule(inventoryId, channel)
        else appDao.upsertPriceRule(PriceRule(inventoryId, channel, price))
    }

    // --- Bodegas ---------------------------------------------------------------------

    fun getWarehouses(): Flow<List<Warehouse>> = appDao.getWarehouses()

    suspend fun addWarehouse(name: String, location: String = ""): Long =
        appDao.insertWarehouse(Warehouse(name = name, location = location))

    fun getSetting(key: String): Flow<String?> = appDao.getSetting(key)

    suspend fun setActiveWarehouse(id: Int) =
        appDao.setSetting(Setting(key = "active_warehouse", value = id.toString()))

    fun getWarehouseStock(warehouseId: Int): Flow<List<WarehouseStock>> =
        appDao.getWarehouseStock(warehouseId)

    fun getStockMatrix(): Flow<List<WarehouseStockDetail>> = appDao.getStockMatrix()

    fun getTransfers(date: String): Flow<List<Transfer>> = appDao.getTransfers(date)

    suspend fun registerTransfer(
        date: String,
        fromWarehouseId: Int,
        toWarehouseId: Int,
        inventoryId: Int,
        quantity: Int,
        note: String
    ): Transfer? = appDao.processTransfer(
        date, fromWarehouseId, toWarehouseId, inventoryId, quantity, note
    )
}
