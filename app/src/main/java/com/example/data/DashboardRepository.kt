package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DashboardRepository(private val appDao: AppDao) {
    fun getLedgerForDate(date: String): Flow<DailyLedger?> = appDao.getLedgerForDate(date)
    
    fun getLedgers(): Flow<List<DailyLedger>> = appDao.getLedgers()
    
    fun getTotalSalesForDate(date: String): Flow<Double> = appDao.getTotalSalesForDate(date).map { it ?: 0.0 }
    
    suspend fun insertLedger(ledger: DailyLedger) = appDao.insertLedger(ledger)
    
    suspend fun updateLedger(ledger: DailyLedger) = appDao.updateLedger(ledger)
    
    suspend fun getLedgerForDateSync(date: String): DailyLedger? = appDao.getLedgerForDateSync(date)
    
    suspend fun insertInvoice(invoice: Invoice) = appDao.insertInvoice(invoice)
    
    fun getInvoicesForDate(date: String): Flow<List<Invoice>> = appDao.getInvoicesForDate(date)

    fun getClients(): Flow<List<Client>> = appDao.getClients()

    suspend fun insertClient(client: Client) = appDao.insertClient(client)

    fun getInventory(): Flow<List<Inventory>> = appDao.getInventory()

    suspend fun insertInventory(inventory: Inventory) = appDao.insertInventory(inventory)

    suspend fun processSale(invoice: Invoice, inventoryId: Int, quantity: Int) {
        appDao.processSale(invoice, inventoryId, quantity)
    }
}
