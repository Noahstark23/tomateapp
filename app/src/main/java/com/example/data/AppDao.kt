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
        "SELECT * FROM lots WHERE inventory_id = :inventoryId AND qty_current > 0 " +
            "ORDER BY fecha_limite ASC, timestamp ASC"
    )
    suspend fun getLotsForConsumeSync(inventoryId: Int): List<Lot>

    @Query("UPDATE lots SET qty_current = qty_current - :quantity WHERE id = :lotId")
    suspend fun consumeLot(lotId: Int, quantity: Int)

    /**
     * Entrada de mercadería con lote: crea el lote y suma el stock del
     * producto en la misma transacción. Devuelve el id del lote.
     */
    @Transaction
    suspend fun registerLotEntry(inventoryId: Int, lot: Lot, quantity: Int): Long {
        val lotId = insertLot(lot.copy(qty_initial = quantity, qty_current = quantity))
        addStock(inventoryId, quantity)
        return lotId
    }

    /**
     * Consume stock de lotes por FEFO (vence-primero-sale-primero).
     * Devuelve el primer lote tocado (trazabilidad) o null si no hay lotes
     * (stock legacy sin trazabilidad: igual se vende del global).
     */
    suspend fun consumeFefo(inventoryId: Int, quantity: Int): Int? {
        var remaining = quantity
        var first: Int? = null
        for (lot in getLotsForConsumeSync(inventoryId)) {
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
        isCredit: Boolean = false
    ): Invoice? {
        val item = getInventoryByIdSync(inventoryId) ?: return null
        if (quantity <= 0 || quantity > item.current_stock) return null

        val totalAmount = quantity * item.sale_price
        if (isCredit) {
            val client = getClientByIdSync(clientId) ?: return null
            if (client.credit_limit <= 0) return null
            val balance = getClientBalanceSync(clientId)
            if (balance + totalAmount > client.credit_limit + 0.005) return null
        }

        subtractInventoryStock(inventoryId, quantity)
        consumeFefo(inventoryId, quantity)

        val totalCost = quantity * item.purchase_price
        val margin = if (totalAmount > 0) (totalAmount - totalCost) / totalAmount * 100.0 else 0.0
        val invoice = Invoice(
            ledger_date = date,
            client_id = clientId,
            inventory_id = inventoryId,
            quantity = quantity,
            unit_price = item.sale_price,
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
    suspend fun processWaste(date: String, inventoryId: Int, quantity: Int, reason: String): Waste? {
        val item = getInventoryByIdSync(inventoryId) ?: return null
        if (quantity <= 0 || quantity > item.current_stock) return null

        subtractInventoryStock(inventoryId, quantity)
        val lotId = consumeFefo(inventoryId, quantity)

        val waste = Waste(
            ledger_date = date,
            inventory_id = inventoryId,
            quantity = quantity,
            financial_loss = quantity * item.purchase_price,
            reason = reason,
            lot_id = lotId
        )
        insertWaste(waste)
        recalculateLedger(date)
        return waste
    }
}
