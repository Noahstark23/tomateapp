package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DashboardRepository(private val appDao: AppDao) {
    fun getLedgerForDate(date: String): Flow<DailyLedger?> = appDao.getLedgerForDate(date)
    
    fun getTotalSalesForDate(date: String): Flow<Double> = appDao.getTotalSalesForDate(date).map { it ?: 0.0 }
    
    suspend fun insertLedger(ledger: DailyLedger) = appDao.insertLedger(ledger)
    
    suspend fun insertInvoice(invoice: Invoice) = appDao.insertInvoice(invoice)
    
    fun getInvoicesForDate(date: String): Flow<List<Invoice>> = appDao.getInvoicesForDate(date)
}
