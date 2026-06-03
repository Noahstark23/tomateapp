package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedger(ledger: DailyLedger)

    @Query("SELECT * FROM daily_ledgers WHERE date = :date LIMIT 1")
    fun getLedgerForDate(date: String): Flow<DailyLedger?>

    @Query("SELECT * FROM daily_ledgers ORDER BY date DESC")
    fun getLedgers(): Flow<List<DailyLedger>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice)

    @Query("SELECT SUM(total_amount) FROM invoices WHERE ledger_date = :date")
    fun getTotalSalesForDate(date: String): Flow<Double?>
    
    @Query("SELECT * FROM invoices WHERE ledger_date = :date ORDER BY timestamp DESC")
    fun getInvoicesForDate(date: String): Flow<List<Invoice>>

    @Query("SELECT * FROM clients")
    fun getClients(): Flow<List<Client>>

    @androidx.room.Insert
    suspend fun insertClient(client: Client)

    @Query("SELECT * FROM inventory")
    fun getInventory(): Flow<List<Inventory>>

    @androidx.room.Insert
    suspend fun insertInventory(inventory: Inventory)

    @Query("UPDATE inventory SET current_stock = current_stock - :quantity WHERE id = :inventoryId")
    suspend fun subtractInventoryStock(inventoryId: Int, quantity: Int)

    @Query("SELECT * FROM daily_ledgers WHERE date = :date LIMIT 1")
    suspend fun getLedgerForDateSync(date: String): DailyLedger?

    @androidx.room.Update
    suspend fun updateLedger(ledger: DailyLedger)

    @androidx.room.Transaction
    suspend fun processSale(invoice: Invoice, inventoryId: Int, quantity: Int) {
        subtractInventoryStock(inventoryId, quantity)
        insertInvoice(invoice)
        val ledger = getLedgerForDateSync(invoice.ledger_date)
        if (ledger != null) {
            val updatedTotalSales = ledger.total_sales + invoice.total_amount
            val updatedNetProfit = updatedTotalSales - ledger.initial_investment
            val updatedLedger = ledger.copy(
                total_sales = updatedTotalSales,
                net_profit = updatedNetProfit
            )
            updateLedger(updatedLedger)
        }
    }
}
