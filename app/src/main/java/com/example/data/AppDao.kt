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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice)

    @Query("SELECT SUM(total_amount) FROM invoices WHERE ledger_date = :date")
    fun getTotalSalesForDate(date: String): Flow<Double?>
    
    @Query("SELECT * FROM invoices WHERE ledger_date = :date ORDER BY timestamp DESC")
    fun getInvoicesForDate(date: String): Flow<List<Invoice>>
}
