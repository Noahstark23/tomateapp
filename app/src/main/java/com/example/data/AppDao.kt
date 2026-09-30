package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // ---------------------------------------------------------------------
    // Ledgers (estado financiero diario)
    // ---------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedger(ledger: DailyLedger)

    @Update
    suspend fun updateLedger(ledger: DailyLedger)

    @Query("SELECT * FROM daily_ledgers WHERE date = :date LIMIT 1")
    fun getLedgerForDate(date: String): Flow<DailyLedger?>

    @Query("SELECT * FROM daily_ledgers WHERE date = :date LIMIT 1")
    suspend fun getLedgerForDateSync(date: String): DailyLedger?

    @Query("SELECT * FROM daily_ledgers ORDER BY date DESC")
    fun getLedgers(): Flow<List<DailyLedger>>

    @Query("SELECT * FROM daily_ledgers ORDER BY date DESC LIMIT :limit")
    fun getRecentLedgers(limit: Int): Flow<List<DailyLedger>>

    // ---------------------------------------------------------------------
    // Facturas
    // ---------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Query("SELECT * FROM invoices WHERE ledger_date = :date ORDER BY timestamp DESC")
    fun getInvoicesForDate(date: String): Flow<List<Invoice>>

    @Query(
        "SELECT i.id AS id, i.ledger_date AS ledger_date, i.client_id AS client_id, " +
            "c.name AS clientName, c.id_type AS idType, c.id_number AS idNumber, " +
            "c.email AS email, inv.item_name AS itemName, inv.cabys AS cabys, " +
            "inv.unit AS unit, i.quantity AS quantity, " +
            "i.unit_price AS unit_price, i.total_amount AS total_amount, " +
            "i.is_credit AS is_credit, i.paid_amount AS paid_amount, " +
            "i.consecutive AS consecutive, i.fe_status AS fe_status " +
            "FROM invoices i JOIN clients c ON c.id = i.client_id " +
            "JOIN inventory inv ON inv.id = i.inventory_id " +
            "WHERE i.ledger_date = :date ORDER BY i.timestamp DESC"
    )
    fun getInvoiceDetails(date: String): Flow<List<InvoiceDetail>>

    @Query("UPDATE invoices SET fe_status = :status WHERE id = :invoiceId")
    suspend fun setFeStatus(invoiceId: Int, status: String)

    // ---------------------------------------------------------------------
    // Gastos
    // ---------------------------------------------------------------------

    @Insert
    suspend fun insertExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE ledger_date = :date ORDER BY timestamp DESC")
    fun getExpensesForDate(date: String): Flow<List<Expense>>

    @Query("SELECT category, SUM(amount) AS total FROM expenses WHERE ledger_date = :date GROUP BY category")
    fun getExpenseTotalsByCategory(date: String): Flow<List<CategoryTotal>>

    // ---------------------------------------------------------------------
    // Merma
    // ---------------------------------------------------------------------

    @Insert
    suspend fun insertWaste(waste: Waste)

    @Query("SELECT * FROM waste WHERE ledger_date = :date ORDER BY timestamp DESC")
    fun getWasteForDate(date: String): Flow<List<Waste>>

    // ---------------------------------------------------------------------
    // Clientes e inventario
    // ---------------------------------------------------------------------

    @Query("SELECT * FROM clients")
    fun getClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :clientId LIMIT 1")
    suspend fun getClientByIdSync(clientId: Int): Client?

    @Insert
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    /**
     * Semillas de desarrollo SIN condición de carrera: cuenta en BD dentro de
     * la transacción en vez de leer StateFlow.value (que empieza vacío antes
     * de que Room emita y duplicaba clientes/productos en cada arranque).
     */
    @Query("SELECT COUNT(*) FROM clients")
    suspend fun countClients(): Int

    @Query("SELECT COUNT(*) FROM inventory")
    suspend fun countInventory(): Int

    @Transaction
    suspend fun ensureSeeds() {
        if (countClients() == 0) {
            insertClient(Client(name = "Client A", contact_info = "123456"))
            insertClient(Client(name = "Client B", contact_info = "654321"))
        }
        if (countInventory() == 0) {
            insertInventory(
                Inventory(
                    item_name = "Caja de Tomate Primera",
                    purchase_price = 5000.0, sale_price = 6000.0,
                    initial_stock = 100, current_stock = 100
                )
            )
            insertInventory(
                Inventory(
                    item_name = "Caja de Tomate Segunda",
                    purchase_price = 3000.0, sale_price = 4500.0,
                    initial_stock = 50, current_stock = 50
                )
            )
        }
    }

    @Query("SELECT * FROM inventory")
    fun getInventory(): Flow<List<Inventory>>

    @Insert
    suspend fun insertInventory(inventory: Inventory)

    @Query("SELECT * FROM inventory WHERE id = :inventoryId LIMIT 1")
    suspend fun getInventoryByIdSync(inventoryId: Int): Inventory?

    @Query("UPDATE inventory SET current_stock = current_stock - :quantity WHERE id = :inventoryId")
    suspend fun subtractInventoryStock(inventoryId: Int, quantity: Int)

    @Query(
        "UPDATE inventory SET sale_price = :salePrice, purchase_price = :costPrice " +
            "WHERE id = :inventoryId"
    )
    suspend fun updatePrices(inventoryId: Int, salePrice: Double, costPrice: Double)

    @Query("UPDATE inventory SET cabys = :cabys, unit = :unit WHERE id = :inventoryId")
    suspend fun updateCatalog(inventoryId: Int, cabys: String, unit: String)

    @Query(
        "UPDATE inventory SET current_stock = current_stock + :quantity, " +
            "initial_stock = initial_stock + :quantity WHERE id = :inventoryId"
    )
    suspend fun addStock(inventoryId: Int, quantity: Int)

    /** Entrada simple: suma stock global y de la bodega dada. */
    @Transaction
    suspend fun stockEntry(inventoryId: Int, quantity: Int, warehouseId: Int) {
        addStock(inventoryId, quantity)
        ensureStockRow(warehouseId, inventoryId)
        addWarehouseStock(warehouseId, inventoryId, quantity)
    }

    @Query("SELECT * FROM invoices WHERE id = :invoiceId LIMIT 1")
    suspend fun getInvoiceByIdSync(invoiceId: Int): Invoice?

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Insert
    suspend fun insertPayment(payment: Payment): Long

    @Query("SELECT * FROM payments WHERE ledger_date = :date ORDER BY timestamp DESC")
    fun getPaymentsForDate(date: String): Flow<List<Payment>>
    // ---------------------------------------------------------------------
    // Créditos y cobranza (derivado: el saldo nunca se edita a mano)
    // ---------------------------------------------------------------------

    @Query(
        "SELECT client_id AS clientId, SUM(total_amount - paid_amount) AS balance " +
            "FROM invoices WHERE is_credit = 1 GROUP BY client_id HAVING balance > 0.005"
    )
    fun getCreditBalances(): Flow<List<ClientBalance>>

    @Query(
        "SELECT COALESCE(SUM(total_amount - paid_amount), 0) FROM invoices " +
            "WHERE is_credit = 1 AND client_id = :clientId"
    )
    suspend fun getClientBalanceSync(clientId: Int): Double

    @Query(
        "SELECT * FROM invoices WHERE client_id = :clientId AND is_credit = 1 " +
            "AND (total_amount - paid_amount) > 0.005 ORDER BY ledger_date ASC, timestamp ASC"
    )
    fun getOpenCreditInvoices(clientId: Int): Flow<List<Invoice>>

    @Query(
        "SELECT * FROM invoices WHERE client_id = :clientId AND is_credit = 1 " +
            "AND (total_amount - paid_amount) > 0.005 ORDER BY ledger_date ASC, timestamp ASC"
    )
    suspend fun getOpenInvoicesSync(clientId: Int): List<Invoice>

    // ---------------------------------------------------------------------
    // Agregados (fuente de verdad para recalcular el ledger)
    // ---------------------------------------------------------------------

    @Query("SELECT COALESCE(SUM(total_amount), 0) FROM invoices WHERE ledger_date = :date")
    suspend fun sumSalesForDate(date: String): Double

    @Query("SELECT COALESCE(SUM(total_cost), 0) FROM invoices WHERE ledger_date = :date")
    suspend fun sumCogsForDate(date: String): Double

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE ledger_date = :date")
    suspend fun sumExpensesForDate(date: String): Double

    @Query("SELECT COALESCE(SUM(financial_loss), 0) FROM waste WHERE ledger_date = :date")
    suspend fun sumWasteForDate(date: String): Double

    /** Ventas al contado del día: lo único que entra a caja por ventas. */
    @Query(
        "SELECT COALESCE(SUM(total_amount), 0) FROM invoices " +
            "WHERE ledger_date = :date AND is_credit = 0"
    )
    suspend fun sumCashSalesForDate(date: String): Double

    /** Abonos cobrados el día: sí son caja, en el día del pago. */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE ledger_date = :date")
    suspend fun sumCollectionsForDate(date: String): Double

    // ---------------------------------------------------------------------
    // Flujo de caja (solo lectura: desgloses para el tab Caja)
    // ---------------------------------------------------------------------

    @Query(
        "SELECT inv.item_name AS itemName, i.inventory_id AS inventoryId, " +
            "SUM(i.quantity) AS quantity, SUM(i.total_amount) AS total " +
            "FROM invoices i JOIN inventory inv ON inv.id = i.inventory_id " +
            "WHERE i.ledger_date = :date AND i.is_credit = 0 " +
            "GROUP BY i.inventory_id, inv.item_name ORDER BY total DESC"
    )
    fun getSalesByProduct(date: String): Flow<List<SalesByProduct>>

    @Query(
        "SELECT COUNT(*) AS invoiceCount, COALESCE(SUM(total_amount), 0) AS totalSales, " +
            "COALESCE(AVG(total_amount), 0) AS avgTicket FROM invoices WHERE ledger_date = :date"
    )
    suspend fun getSalesSummary(date: String): SalesSummary

    @Query("SELECT * FROM daily_ledgers WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getLedgersBetween(start: String, end: String): Flow<List<DailyLedger>>

    /** Serie de ventas al contado por día (para proyectar entradas de caja). */
    @Query(
        "SELECT ledger_date AS date, COALESCE(SUM(total_amount), 0) AS total " +
            "FROM invoices WHERE is_credit = 0 AND ledger_date BETWEEN :start AND :end " +
            "GROUP BY ledger_date"
    )
    fun getCashSalesSeries(start: String, end: String): Flow<List<DateTotal>>

    /** Serie de abonos por día (para proyectar entradas de caja). */
    @Query(
        "SELECT ledger_date AS date, COALESCE(SUM(amount), 0) AS total " +
            "FROM payments WHERE ledger_date BETWEEN :start AND :end GROUP BY ledger_date"
    )
    fun getCollectionsSeries(start: String, end: String): Flow<List<DateTotal>>

    // ---------------------------------------------------------------------
    // Bodegas, stock por bodega y traslados
    // ---------------------------------------------------------------------

    @Query("SELECT * FROM warehouses ORDER BY id ASC")
    fun getWarehouses(): Flow<List<Warehouse>>

    @Insert
    suspend fun insertWarehouse(warehouse: Warehouse): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: Setting)

    @Query("SELECT `value` FROM settings WHERE `key` = :key LIMIT 1")
    fun getSetting(key: String): Flow<String?>

    @Query("SELECT * FROM warehouse_stock WHERE warehouse_id = :warehouseId")
    fun getWarehouseStock(warehouseId: Int): Flow<List<WarehouseStock>>

    @Query(
        "SELECT w.id AS warehouseId, w.name AS warehouseName, " +
            "i.id AS inventoryId, i.item_name AS itemName, " +
            "COALESCE(s.quantity, 0) AS quantity " +
            "FROM warehouses w CROSS JOIN inventory i " +
            "LEFT JOIN warehouse_stock s ON s.warehouse_id = w.id AND s.inventory_id = i.id " +
            "ORDER BY w.id ASC, i.id ASC"
    )
    fun getStockMatrix(): Flow<List<WarehouseStockDetail>>

    @Query("SELECT * FROM transfers WHERE ledger_date = :date ORDER BY timestamp DESC")
    fun getTransfers(date: String): Flow<List<Transfer>>

    @Query(
        "INSERT OR IGNORE INTO warehouse_stock (warehouse_id, inventory_id, quantity) " +
            "VALUES (:warehouseId, :inventoryId, 0)"
    )
    suspend fun ensureStockRow(warehouseId: Int, inventoryId: Int)

    @Query(
        "SELECT COALESCE((SELECT quantity FROM warehouse_stock " +
            "WHERE warehouse_id = :warehouseId AND inventory_id = :inventoryId), 0)"
    )
    suspend fun warehouseQty(warehouseId: Int, inventoryId: Int): Int

    @Query(
        "UPDATE warehouse_stock SET quantity = quantity + :quantity " +
            "WHERE warehouse_id = :warehouseId AND inventory_id = :inventoryId"
    )
    suspend fun addWarehouseStock(warehouseId: Int, inventoryId: Int, quantity: Int)

    @Query(
        "UPDATE warehouse_stock SET quantity = quantity - :quantity " +
            "WHERE warehouse_id = :warehouseId AND inventory_id = :inventoryId"
    )
    suspend fun subtractWarehouseStock(warehouseId: Int, inventoryId: Int, quantity: Int)

    @Insert
    suspend fun insertTransfer(transfer: Transfer): Long

    /**
     * Traslado entre bodegas: mueve stock y reubica lotes FEFO (conservando
     * sus vencimientos en la bodega destino). No toca caja. Null si origen
     * y destino coinciden, cantidad inválida o sin stock en origen.
     */
    @Transaction
    suspend fun processTransfer(
        date: String,
        fromWarehouseId: Int,
        toWarehouseId: Int,
        inventoryId: Int,
        quantity: Int,
        note: String
    ): Transfer? {
        if (fromWarehouseId == toWarehouseId || quantity <= 0) return null
        ensureStockRow(fromWarehouseId, inventoryId)
        ensureStockRow(toWarehouseId, inventoryId)
        if (warehouseQty(fromWarehouseId, inventoryId) < quantity) return null
        subtractWarehouseStock(fromWarehouseId, inventoryId, quantity)
        addWarehouseStock(toWarehouseId, inventoryId, quantity)
        // Reubica lotes FEFO conservando vencimiento/proveedor.
        var remaining = quantity
        var firstLimite: String? = null
        var firstSupplier = "Traslado"
        for (lot in getLotsForConsumeSync(inventoryId, fromWarehouseId)) {
            if (remaining <= 0) break
            val take = minOf(remaining, lot.qty_current)
            if (take > 0) {
                consumeLot(lot.id, take)
                remaining -= take
                if (firstLimite == null) {
                    firstLimite = lot.fecha_limite
                    firstSupplier = lot.supplier
                }
            }
        }
        val moved = quantity - remaining
        if (moved > 0) {
            val ingreso = java.time.LocalDate.parse(date).toString()
            insertLot(
                Lot(
                    inventory_id = inventoryId,
                    warehouse_id = toWarehouseId,
                    supplier = firstSupplier,
                    variedad = "",
                    calibre = "",
                    calidad = "Traslado",
                    qty_initial = moved,
                    qty_current = moved,
                    cost_total = 0.0,
                    fecha_ingreso = ingreso,
                    fecha_limite = firstLimite ?: ingreso
                )
            )
        }
        val transfer = Transfer(
            from_warehouse_id = fromWarehouseId,
            to_warehouse_id = toWarehouseId,
            inventory_id = inventoryId,
            quantity = quantity,
            ledger_date = date,
            note = note
        )
        insertTransfer(transfer)
        return transfer
    }

    // ---------------------------------------------------------------------
    // Lotes + FEFO
    // ---------------------------------------------------------------------

    @Insert
    suspend fun insertLot(lot: Lot): Long

    @Query(
        "SELECT * FROM lots WHERE inventory_id = :inventoryId AND qty_current > 0 " +
            "ORDER BY fecha_limite ASC, fecha_ingreso ASC"
    )
    fun getLotsForProduct(inventoryId: Int): Flow<List<Lot>>

    @Query(
        "SELECT * FROM lots WHERE inventory_id = :inventoryId AND warehouse_id = :warehouseId " +
            "AND qty_current > 0 ORDER BY fecha_limite ASC, timestamp ASC"
    )
    suspend fun getLotsForConsumeSync(inventoryId: Int, warehouseId: Int): List<Lot>

    @Query("UPDATE lots SET qty_current = qty_current - :quantity WHERE id = :lotId")
    suspend fun consumeLot(lotId: Int, quantity: Int)

    /**
     * Entrada de mercadería con lote: crea el lote y suma el stock (global y
     * de la bodega) en la misma transacción. Devuelve el id del lote.
     */
    @Transaction
    suspend fun registerLotEntry(inventoryId: Int, warehouseId: Int, lot: Lot, quantity: Int): Long {
        val lotId = insertLot(lot.copy(qty_initial = quantity, qty_current = quantity))
        addStock(inventoryId, quantity)
        ensureStockRow(warehouseId, inventoryId)
        addWarehouseStock(warehouseId, inventoryId, quantity)
        return lotId
    }

    /**
     * Consume stock de lotes por FEFO **de la bodega dada** (vence-primero-
     * sale-primero). Devuelve el primer lote tocado o null si no hay lotes
     * (stock legacy sin trazabilidad: igual se vende del global).
     */
    suspend fun consumeFefo(inventoryId: Int, warehouseId: Int, quantity: Int): Int? {
        var remaining = quantity
        var first: Int? = null
        for (lot in getLotsForConsumeSync(inventoryId, warehouseId)) {
            if (remaining <= 0) break
            val take = minOf(remaining, lot.qty_current)
            if (take > 0) {
                consumeLot(lot.id, take)
                remaining -= take
                if (first == null) first = lot.id
            }
        }
        return first
    }

    /**
     * Compra a proveedor al contado: crea el lote, suma el stock y registra
     * el gasto COMPRA_MERCADERIA en una sola transacción. La compra SÍ sale
     * de caja (es el egreso más grande de la bodega) y SÍ baja la ganancia.
     * Devuelve el id del lote o null si el producto no existe o montos inválidos.
     */
    @Transaction
    suspend fun processPurchase(
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
    ): Long? {
        val item = getInventoryByIdSync(inventoryId) ?: return null
        if (quantity <= 0 || costTotal < 0) return null
        val ingreso = java.time.LocalDate.parse(date)
        val limite = ingreso.plusDays(shelfLifeDays.coerceAtLeast(1).toLong())
        val lot = Lot(
            inventory_id = inventoryId,
            warehouse_id = warehouseId,
            supplier = supplier,
            variedad = variedad,
            calibre = calibre,
            calidad = calidad,
            cost_total = costTotal,
            fecha_ingreso = date,
            fecha_limite = limite.toString()
        )
        val lotId = registerLotEntry(inventoryId, warehouseId, lot, quantity)
        insertExpense(
            Expense(
                ledger_date = date,
                category = ExpenseCategory.COMPRA_MERCADERIA,
                amount = costTotal,
                description = "Compra a $supplier (${quantity} uds)"
            )
        )
        recalculateLedger(date)
        return lotId
    }

    // ---------------------------------------------------------------------
    // Arqueo de caja
    // ---------------------------------------------------------------------

    @Insert
    suspend fun insertCashCount(count: CashCount): Long

    @Query("SELECT * FROM cash_counts WHERE ledger_date = :date ORDER BY timestamp DESC")
    fun getCashCounts(date: String): Flow<List<CashCount>>

    // ---------------------------------------------------------------------
    // Turnos de caja
    // ---------------------------------------------------------------------

    @Insert
    suspend fun insertShift(shift: CashShift): Long

    @Update
    suspend fun updateShift(shift: CashShift)

    @Query("SELECT * FROM cash_shifts WHERE closed_at = 0 ORDER BY opened_at DESC LIMIT 1")
    fun getOpenShift(): Flow<CashShift?>

    @Query("SELECT * FROM cash_shifts WHERE ledger_date = :date ORDER BY opened_at DESC")
    fun getShifts(date: String): Flow<List<CashShift>>

    // ---------------------------------------------------------------------
    // Precios por canal
    // ---------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPriceRule(rule: PriceRule)

    @Query("DELETE FROM price_rules WHERE inventory_id = :inventoryId AND channel = :channel")
    suspend fun deletePriceRule(inventoryId: Int, channel: String)

    @Query("SELECT * FROM price_rules")
    fun getPriceRules(): Flow<List<PriceRule>>

    // ---------------------------------------------------------------------
    // Transacciones de negocio
    // ---------------------------------------------------------------------

    /**
     * Recalcula el estado financiero del día desde las tablas fuente.
     * Ganancia real = ventas (contado + crédito, devengo) - COGS - gastos - merma
     * (la inversión inicial es capital de trabajo, no un costo). La merma no
     * toca el efectivo. Caja = inversión + ventas al contado + abonos - gastos
     * (el crédito NO toca caja hasta que se cobra).
     */
    @Transaction
    suspend fun recalculateLedger(date: String) {
        val ledger = getLedgerForDateSync(date) ?: return
        val sales = sumSalesForDate(date)
        val cogs = sumCogsForDate(date)
        val expenses = sumExpensesForDate(date)
        val waste = sumWasteForDate(date)
        val cashSales = sumCashSalesForDate(date)
        val collections = sumCollectionsForDate(date)
        updateLedger(
            ledger.copy(
                total_sales = sales,
                total_cogs = cogs,
                total_expenses = expenses,
                total_waste_value = waste,
                real_net_profit = sales - cogs - expenses - waste,
                total_collections = collections,
                cash_on_hand = ledger.initial_investment + cashSales + collections - expenses
            )
        )
    }

    /** Crea el ledger del día o ajusta su capital de trabajo, manteniendo los agregados consistentes. */
    @Transaction
    suspend fun setInitialInvestment(date: String, investment: Double) {
        val existing = getLedgerForDateSync(date)
        if (existing == null) {
            insertLedger(
                DailyLedger(
                    date = date,
                    initial_investment = investment,
                    cash_on_hand = investment
                )
            )
        } else {
            updateLedger(existing.copy(initial_investment = investment))
        }
        recalculateLedger(date)
    }

    /**
     * Venta con trazabilidad de costos: congela el purchase_price vigente en
     * la factura (unit_cost/total_cost) y recalcula el ledger del día.
     * A crédito exige cupo (credit_limit > 0 y saldo + total <= límite).
     * Devuelve la factura creada, o null si no hay stock o no hay cupo.
     */
    @Transaction
    suspend fun processSale(
        date: String,
        clientId: Int,
        inventoryId: Int,
        quantity: Int,
        isCredit: Boolean = false,
        warehouseId: Int,
        unitPrice: Double? = null
    ): Invoice? {
        val item = getInventoryByIdSync(inventoryId) ?: return null
        if (quantity <= 0 || quantity > item.current_stock) return null
        ensureStockRow(warehouseId, inventoryId)
        if (warehouseQty(warehouseId, inventoryId) < quantity) return null

        // Precio por canal si viene (si no, base). Se congela en la factura.
        val price = unitPrice?.takeIf { it > 0 } ?: item.sale_price
        val totalAmount = quantity * price
        if (isCredit) {
            val client = getClientByIdSync(clientId) ?: return null
            if (client.credit_limit <= 0) return null
            val balance = getClientBalanceSync(clientId)
            if (balance + totalAmount > client.credit_limit + 0.005) return null
        }

        subtractInventoryStock(inventoryId, quantity)
        subtractWarehouseStock(warehouseId, inventoryId, quantity)
        consumeFefo(inventoryId, warehouseId, quantity)

        val totalCost = quantity * item.purchase_price
        val margin = if (totalAmount > 0) (totalAmount - totalCost) / totalAmount * 100.0 else 0.0
        val invoice = Invoice(
            ledger_date = date,
            client_id = clientId,
            inventory_id = inventoryId,
            quantity = quantity,
            unit_price = price,
            unit_cost = item.purchase_price,
            total_amount = totalAmount,
            total_cost = totalCost,
            profit_margin = margin,
            is_credit = isCredit
        )
        val rowId = insertInvoice(invoice)
        // Consecutivo FE interno: sucursal(3)+terminal(5)+tipo FE(2)+seq(10).
        // Válido tributariamente solo tras firma y envío a ATV.
        val consecutive = "001" + "00001" + "01" + rowId.toString().padStart(10, '0')
        val saved = invoice.copy(id = rowId.toInt(), consecutive = consecutive)
        updateInvoice(saved)
        recalculateLedger(date)
        return saved
    }

    /**
     * Registra un abono: suma caja en el día del pago (no en el de la venta).
     * Con `invoiceId` abona esa factura; sin él reparte FIFO a las más viejas.
     * Rechaza sobrepagos (monto > saldo vivo). Devuelve el pago o null.
     */
    @Transaction
    suspend fun processPayment(
        date: String,
        clientId: Int,
        invoiceId: Int?,
        amount: Double
    ): Payment? {
        if (amount <= 0) return null
        val openBalance = getClientBalanceSync(clientId)
        if (amount > openBalance + 0.005) return null

        var remaining = amount
        if (invoiceId != null) {
            val target = getInvoiceByIdSync(invoiceId) ?: return null
            if (!target.is_credit || target.client_id != clientId) return null
            val apply = minOf(remaining, target.balance)
            updateInvoice(target.copy(paid_amount = target.paid_amount + apply))
            remaining -= apply
        }
        if (remaining > 0.005) {
            val oldest = getOpenInvoicesSync(clientId)
            for (inv in oldest) {
                if (remaining <= 0.005) break
                val apply = minOf(remaining, inv.balance)
                updateInvoice(inv.copy(paid_amount = inv.paid_amount + apply))
                remaining -= apply
            }
        }
        val payment = Payment(
            client_id = clientId,
            invoice_id = invoiceId,
            ledger_date = date,
            amount = amount
        )
        insertPayment(payment)
        recalculateLedger(date)
        return payment
    }

    /** Registra un gasto operativo y recalcula el ledger (gasto sí reduce caja). */
    @Transaction
    suspend fun processExpense(expense: Expense) {
        insertExpense(expense)
        recalculateLedger(expense.ledger_date)
    }

    /**
     * Registra merma: descuenta stock global + lote FEFO y valora la pérdida
     * al purchase_price vigente. Devuelve el registro creado, o null si no
     * hay stock suficiente.
     */
    @Transaction
    suspend fun processWaste(
        date: String,
        inventoryId: Int,
        quantity: Int,
        reason: String,
        causa: String = "",
        etapa: String = "",
        warehouseId: Int
    ): Waste? {
        val item = getInventoryByIdSync(inventoryId) ?: return null
        if (quantity <= 0 || quantity > item.current_stock) return null
        ensureStockRow(warehouseId, inventoryId)
        if (warehouseQty(warehouseId, inventoryId) < quantity) return null

        subtractInventoryStock(inventoryId, quantity)
        subtractWarehouseStock(warehouseId, inventoryId, quantity)
        val lotId = consumeFefo(inventoryId, warehouseId, quantity)

        val waste = Waste(
            ledger_date = date,
            inventory_id = inventoryId,
            quantity = quantity,
            financial_loss = quantity * item.purchase_price,
            reason = reason,
            lot_id = lotId,
            causa = causa,
            etapa = etapa
        )
        insertWaste(waste)
        recalculateLedger(date)
        return waste
    }

    /** Lotes con stock que vencen en la fecha dada o antes (alertas). */
    @Query(
        "SELECT l.id AS lotId, inv.item_name AS itemName, l.qty_current AS qty, " +
            "l.fecha_limite AS fecha_limite FROM lots l " +
            "JOIN inventory inv ON inv.id = l.inventory_id " +
            "WHERE l.qty_current > 0 AND l.fecha_limite <= :upto ORDER BY l.fecha_limite ASC"
    )
    fun getExpiringLots(upto: String): Flow<List<LotAlert>>

    /** Merma del día agrupada por causa (causa vacía = sin clasificar). */
    @Query(
        "SELECT causa AS causa, SUM(financial_loss) AS total FROM waste " +
            "WHERE ledger_date = :date GROUP BY causa"
    )
    fun getWasteByCause(date: String): Flow<List<WasteCauseTotal>>
}
