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
    suspend fun insertInvoice(invoice: Invoice)

    @Query("SELECT * FROM invoices WHERE ledger_date = :date ORDER BY timestamp DESC")
    fun getInvoicesForDate(date: String): Flow<List<Invoice>>

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

    @Insert
    suspend fun insertClient(client: Client)

    @Query("SELECT * FROM inventory")
    fun getInventory(): Flow<List<Inventory>>

    @Insert
    suspend fun insertInventory(inventory: Inventory)

    @Query("SELECT * FROM inventory WHERE id = :inventoryId LIMIT 1")
    suspend fun getInventoryByIdSync(inventoryId: Int): Inventory?

    @Query("UPDATE inventory SET current_stock = current_stock - :quantity WHERE id = :inventoryId")
    suspend fun subtractInventoryStock(inventoryId: Int, quantity: Int)

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

    // ---------------------------------------------------------------------
    // Transacciones de negocio
    // ---------------------------------------------------------------------

    /**
     * Recalcula el estado financiero del día desde las tablas fuente.
     * Ganancia real = ventas - COGS - gastos - merma (la inversión inicial
     * es capital de trabajo, no un costo). La merma no toca el efectivo.
     */
    @Transaction
    suspend fun recalculateLedger(date: String) {
        val ledger = getLedgerForDateSync(date) ?: return
        val sales = sumSalesForDate(date)
        val cogs = sumCogsForDate(date)
        val expenses = sumExpensesForDate(date)
        val waste = sumWasteForDate(date)
        updateLedger(
            ledger.copy(
                total_sales = sales,
                total_cogs = cogs,
                total_expenses = expenses,
                total_waste_value = waste,
                real_net_profit = sales - cogs - expenses - waste,
                cash_on_hand = ledger.initial_investment + sales - expenses
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
     * Devuelve la factura creada, o null si no hay stock suficiente.
     */
    @Transaction
    suspend fun processSale(date: String, clientId: Int, inventoryId: Int, quantity: Int): Invoice? {
        val item = getInventoryByIdSync(inventoryId) ?: return null
        if (quantity <= 0 || quantity > item.current_stock) return null

        subtractInventoryStock(inventoryId, quantity)

        val totalAmount = quantity * item.sale_price
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
            profit_margin = margin
        )
        insertInvoice(invoice)
        recalculateLedger(date)
        return invoice
    }

    /** Registra un gasto operativo y recalcula el ledger (gasto sí reduce caja). */
    @Transaction
    suspend fun processExpense(expense: Expense) {
        insertExpense(expense)
        recalculateLedger(expense.ledger_date)
    }

    /**
     * Registra merma: descuenta stock y valora la pérdida al purchase_price
     * vigente. Devuelve el registro creado, o null si no hay stock suficiente.
     */
    @Transaction
    suspend fun processWaste(date: String, inventoryId: Int, quantity: Int, reason: String): Waste? {
        val item = getInventoryByIdSync(inventoryId) ?: return null
        if (quantity <= 0 || quantity > item.current_stock) return null

        subtractInventoryStock(inventoryId, quantity)

        val waste = Waste(
            ledger_date = date,
            inventory_id = inventoryId,
            quantity = quantity,
            financial_loss = quantity * item.purchase_price,
            reason = reason
        )
        insertWaste(waste)
        recalculateLedger(date)
        return waste
    }
}
