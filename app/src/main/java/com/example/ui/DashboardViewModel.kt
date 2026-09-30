package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.Client
import com.example.data.DailyLedger
import com.example.data.DashboardRepository
import com.example.data.Expense
import com.example.data.ExpenseCategory
import com.example.data.Inventory
import com.example.data.Waste
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ViewModel operativo: iniciar el día, registrar ventas/gastos/mermas y
 * exponer el estado financiero del día. El análisis CFO (extracción segura,
 * runway, leakage) vive en FinancialViewModel.
 */
class DashboardViewModel(private val repository: DashboardRepository) : ViewModel() {

    private val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    val ledgerForToday: StateFlow<DailyLedger?> = repository.getLedgerForDate(currentDate)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val clients: StateFlow<List<Client>> = repository.getClients()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val inventory: StateFlow<List<Inventory>> = repository.getInventory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val historicalLedgers: StateFlow<List<DailyLedger>> = repository.getLedgers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val expensesForToday: StateFlow<List<Expense>> = repository.getExpensesForDate(currentDate)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val wasteForToday: StateFlow<List<Waste>> = repository.getWasteForDate(currentDate)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val lastSaleDetails = MutableStateFlow<LastSaleDetails?>(null)

    /**
     * Métricas del día derivadas del ledger materializado. La ganancia real
     * es Ventas - COGS - Gastos - Merma; la inversión inicial es capital de
     * trabajo, no un costo.
     */
    val currentMetrics: StateFlow<DashboardMetrics> = ledgerForToday
        .map { ledger ->
            if (ledger != null) {
                DashboardMetrics(
                    hasLedger = true,
                    initialInvestment = ledger.initial_investment,
                    totalSales = ledger.total_sales,
                    totalCogs = ledger.total_cogs,
                    grossProfit = ledger.total_sales - ledger.total_cogs,
                    totalExpenses = ledger.total_expenses,
                    totalWaste = ledger.total_waste_value,
                    realNetProfit = ledger.real_net_profit,
                    cashOnHand = ledger.cash_on_hand
                )
            } else {
                DashboardMetrics(hasLedger = false)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardMetrics()
        )

    fun setInitialInvestment(investment: Double) {
        viewModelScope.launch {
            repository.setInitialInvestment(currentDate, investment)
        }
    }

    fun processSale(
        client: Client,
        inventoryItem: Inventory,
        quantity: Int,
        isCredit: Boolean = false,
        unitPrice: Double? = null
    ) {
        if (quantity > inventoryItem.current_stock || quantity <= 0) return

        viewModelScope.launch {
            val warehouseId = activeWarehouseId.first()
            val invoice = repository.registerSale(
                currentDate, client.id, inventoryItem.id, quantity, isCredit, warehouseId, unitPrice
            )
            if (invoice != null) {
                lastSaleDetails.value = LastSaleDetails(
                    clientName = client.name,
                    itemName = inventoryItem.item_name,
                    quantity = invoice.quantity,
                    salePrice = invoice.unit_price,
                    totalAmount = invoice.total_amount
                )
            }
        }
    }

    fun registerExpense(category: ExpenseCategory, amount: Double, description: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            repository.registerExpense(currentDate, category, amount, description)
        }
    }

    fun registerWaste(
        inventoryItem: Inventory,
        quantity: Int,
        reason: String,
        causa: String = "",
        etapa: String = ""
    ) {
        if (quantity > inventoryItem.current_stock || quantity <= 0) return
        viewModelScope.launch {
            val warehouseId = activeWarehouseId.first()
            repository.registerWaste(
                currentDate, inventoryItem.id, quantity, reason, causa, etapa, warehouseId
            )
        }
    }

    /**
     * Da de alta un cliente (nombre + teléfono ya sanitizado + cupo) y lo
     * devuelve con su id para poder seleccionarlo inmediatamente.
     */
    fun addClient(
        name: String,
        phone: String,
        creditLimit: Double = 0.0,
        type: String = "TRAMO",
        onAdded: (Client) -> Unit = {}
    ) {
        viewModelScope.launch {
            onAdded(repository.addClient(name, phone, creditLimit, type))
        }
    }

    fun setCreditLimit(clientId: Int, limit: Double) {
        viewModelScope.launch { repository.setCreditLimit(clientId, limit) }
    }

    // --- Créditos y cobranza --------------------------------------------------

    /** Saldo vivo por cobrar por cliente (derivado de facturas a crédito). */
    val creditBalances: StateFlow<Map<Int, Double>> = repository.getCreditBalances()
        .map { list -> list.associate { it.clientId to it.balance } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun openCreditInvoices(clientId: Int): StateFlow<List<com.example.data.Invoice>> =
        repository.getOpenCreditInvoices(clientId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Registra un abono (suma caja hoy). [onDone] recibe true si se guardó;
     * false = monto inválido o sobrepago.
     */
    fun registerPayment(clientId: Int, invoiceId: Int?, amount: Double, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            onDone(repository.registerPayment(currentDate, clientId, invoiceId, amount) != null)
        }
    }

    // --- Productos -------------------------------------------------------------

    fun addProduct(name: String, costPrice: Double, salePrice: Double, stock: Int) {
        if (name.isBlank() || costPrice < 0 || salePrice <= 0 || stock < 0) return
        viewModelScope.launch {
            repository.addProduct(name.trim(), costPrice, salePrice, stock)
        }
    }

    fun updatePrices(inventoryId: Int, salePrice: Double, costPrice: Double) {
        if (salePrice <= 0 || costPrice < 0) return
        viewModelScope.launch { repository.updatePrices(inventoryId, salePrice, costPrice) }
    }

    fun addStock(inventoryId: Int, quantity: Int) {
        if (quantity <= 0) return
        viewModelScope.launch {
            repository.addStock(inventoryId, quantity, activeWarehouseId.first())
        }
    }

    fun updateCatalog(inventoryId: Int, cabys: String, unit: String) {
        viewModelScope.launch { repository.updateCatalog(inventoryId, cabys.trim(), unit.trim()) }
    }

    // --- Lotes -------------------------------------------------------------------

    fun lotsFor(inventoryId: Int): StateFlow<List<com.example.data.Lot>> =
        repository.getLotsForProduct(inventoryId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Entrada de lote con vencimiento (límite = hoy + días de vida útil) y
     * suma de stock en la misma transacción.
     */
    fun registerLot(
        inventoryId: Int,
        supplier: String,
        variedad: String,
        calibre: String,
        calidad: String,
        quantity: Int,
        costTotal: Double,
        shelfLifeDays: Int,
        onDone: () -> Unit = {}
    ) {
        if (quantity <= 0 || costTotal < 0) return
        viewModelScope.launch {
            val warehouseId = activeWarehouseId.first()
            repository.registerLot(
                currentDate, inventoryId, warehouseId, supplier.trim(), variedad.trim(),
                calibre.trim(), calidad.trim(), quantity, costTotal, shelfLifeDays
            )
            onDone()
        }
    }

    // --- Datos FE del cliente ------------------------------------------------------

    fun updateClientFe(clientId: Int, idType: String, idNumber: String, email: String) {
        viewModelScope.launch {
            val client = repository.getClientById(clientId) ?: return@launch
            repository.updateClient(
                client.copy(
                    id_type = idType,
                    id_number = idNumber.filter { it.isDigit() },
                    email = email.trim()
                )
            )
        }
    }

    // --- XML FE v4.4 (pre-firma) ----------------------------------------------------

    val invoiceDetails: StateFlow<List<com.example.data.InvoiceDetail>> =
        repository.getInvoiceDetails(currentDate)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /** Merma de hoy agrupada por causa codificada. */
    val wasteByCause: StateFlow<List<com.example.data.WasteCauseTotal>> =
        repository.getWasteByCause(currentDate)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Genera el XML pre-firma de una factura y marca su estado PENDIENTE
     * (pendiente de firma .p12 y envío a ATV). Devuelve el Uri o null.
     */
    suspend fun exportInvoiceXml(
        context: android.content.Context,
        invoiceId: Int,
        ivaRate: Double
    ): android.net.Uri? {
        return try {
            val invoice = repository.getInvoiceById(invoiceId) ?: return null
            val client = repository.getClientById(invoice.client_id) ?: return null
            val item = repository.getInventoryById(invoice.inventory_id) ?: return null
            val xml = com.example.fe.FeXml.facturaXml(invoice, client, item, ivaRate)
            val dir = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
            val seq = invoice.consecutive.ifEmpty { invoiceId.toString() }
            val file = java.io.File(dir, "FE-$seq.xml")
            file.writeText(xml, Charsets.UTF_8)
            repository.setFeStatus(invoiceId, "PENDIENTE")
            androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
        } catch (e: Exception) {
            null
        }
    }

    // --- Bodega activa -------------------------------------------------------------------

    val warehouses: StateFlow<List<com.example.data.Warehouse>> =
        repository.getWarehouses()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /** Bodega donde se venden/merman/compran (id, default 1 = Tramo Principal). */
    val activeWarehouseId: StateFlow<Int> =
        repository.getSetting("active_warehouse")
            .map { it?.toIntOrNull() ?: 1 }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 1
            )

    /** Stock por producto en la bodega activa (para validar y mostrar). */
    val activeStock: StateFlow<Map<Int, Int>> =
        kotlinx.coroutines.flow.combine(
            activeWarehouseId,
            repository.getStockMatrix()
        ) { activeId, matrix ->
            matrix.filter { it.warehouseId == activeId }
                .associate { it.inventoryId to it.quantity }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun setActiveWarehouse(id: Int) {
        viewModelScope.launch { repository.setActiveWarehouse(id) }
    }

    fun addWarehouse(name: String, onDone: () -> Unit = {}) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.addWarehouse(name.trim())
            repository.setActiveWarehouse(id.toInt())
            onDone()
        }
    }

    fun stockMatrix(): StateFlow<List<com.example.data.WarehouseStockDetail>> =
        repository.getStockMatrix()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun transfersToday(): StateFlow<List<com.example.data.Transfer>> =
        repository.getTransfers(currentDate)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Traslado entre bodegas (no toca caja). onDone(true) si se movió.
     */
    fun registerTransfer(
        toWarehouseId: Int,
        inventoryId: Int,
        quantity: Int,
        note: String = "",
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val from = activeWarehouseId.first()
            onDone(
                repository.registerTransfer(
                    currentDate, from, toWarehouseId, inventoryId, quantity, note.trim()
                ) != null
            )
        }
    }

    // --- Compras y arqueo ---------------------------------------------------------------

    /**
     * Compra a proveedor: lote + stock + gasto COMPRA en una transacción.
     * Sale de caja y baja la ganancia (mercadería pagada al contado).
     */
    fun registerPurchase(
        inventoryId: Int,
        supplier: String,
        variedad: String,
        calibre: String,
        calidad: String,
        quantity: Int,
        costTotal: Double,
        shelfLifeDays: Int,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (quantity <= 0 || costTotal < 0) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            val warehouseId = activeWarehouseId.first()
            onDone(
                repository.registerPurchase(
                    currentDate, inventoryId, warehouseId, supplier.trim(), variedad.trim(),
                    calibre.trim(), calidad.trim(), quantity, costTotal, shelfLifeDays
                ) != null
            )
        }
    }

    fun cashCounts(): StateFlow<List<com.example.data.CashCount>> =
        repository.getCashCounts(currentDate)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /** Guarda un arqueo (conteo físico vs esperado). No ajusta la caja. */
    fun saveCashCount(expected: Double, counted: Double, note: String) {
        viewModelScope.launch {
            repository.insertCashCount(
                com.example.data.CashCount(
                    ledger_date = currentDate,
                    expected = expected,
                    counted = counted,
                    diff = counted - expected,
                    note = note.trim()
                )
            )
        }
    }

    // --- Turnos ----------------------------------------------------------------------------

    val openShift: StateFlow<com.example.data.CashShift?> =
        repository.getOpenShift()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    fun shiftsToday(): StateFlow<List<com.example.data.CashShift>> =
        repository.getShifts(currentDate)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun openShift(openingCash: Double, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.openShift(currentDate, openingCash)
            onDone()
        }
    }

    fun closeShift(
        shift: com.example.data.CashShift,
        expectedCash: Double,
        countedCash: Double,
        note: String
    ) {
        viewModelScope.launch {
            repository.closeShift(shift, expectedCash, countedCash, note)
        }
    }

    // --- Precios por canal --------------------------------------------------------------------

    val priceRules: StateFlow<List<com.example.data.PriceRule>> =
        repository.getPriceRules()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /** Precio vigente: regla del canal o base si no hay. */
    fun priceFor(rules: List<com.example.data.PriceRule>, inventoryId: Int, channel: String, base: Double): Double =
        rules.firstOrNull { it.inventory_id == inventoryId && it.channel == channel }?.price ?: base

    fun saveChannelPrices(inventoryId: Int, prices: Map<String, Double?>) {
        viewModelScope.launch {
            prices.forEach { (channel, price) -> repository.savePriceRule(inventoryId, channel, price) }
        }
    }

    // --- Alertas operativas ----------------------------------------------------------

    /**
     * Lotes que vencen mañana o antes (alerta de remate/merma inminente).
     * Incluye stock bajo global: la UI combina ambas señales.
     */
    val expiringLots: StateFlow<List<com.example.data.LotAlert>> =
        repository.getExpiringLots(
            LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Respaldo / restauración ----------------------------------------------------

    /**
     * Copia la BD SQLite (con checkpoint WAL previo) a cache/exports para
     * compartir. Devuelve el Uri o null.
     */
    suspend fun backupDatabase(context: android.content.Context): android.net.Uri? {
        return try {
            val db = com.example.data.AppDatabase.getDatabase(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)")
            val src = context.getDatabasePath("nortex_database")
            if (!src.exists()) return null
            val dir = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
            val dst = java.io.File(dir, "tomateapp_respaldo_${currentDate}.db")
            src.inputStream().use { input -> dst.outputStream().use { input.copyTo(it) } }
            androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", dst
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Restaura la BD desde un Uri (document picker). Cierra Room, reemplaza el
     * archivo y devuelve true. La UI debe reiniciar la app después.
     */
    suspend fun restoreDatabase(context: android.content.Context, uri: android.net.Uri): Boolean {
        return try {
            val db = com.example.data.AppDatabase.getDatabase(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)")
            com.example.data.AppDatabase.closeDatabase()
            val dst = context.getDatabasePath("nortex_database")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dst.outputStream().use { input.copyTo(it) }
            } ?: return false
            // Limpia WAL/SHM residuales para que Room reabra limpio.
            java.io.File(dst.path + "-wal").delete()
            java.io.File(dst.path + "-shm").delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    // --- Exportar CSV para el contador (FE v4.4) --------------------------------

    /**
     * Genera el CSV del día (ventas, gastos, abonos) con columna CABYS vacía
     * para que el contador lo mapee a su facturador. Devuelve el Uri o null.
     */
    suspend fun exportDayCsv(context: android.content.Context): android.net.Uri? {
        return try {
            val invoices = repository.getInvoicesForDate(currentDate).first()
            val expenses = repository.getExpensesForDate(currentDate).first()
            val payments = repository.getPaymentsForDate(currentDate).first()
            val clients = repository.getClients().first().associateBy { it.id }
            val items = repository.getInventory().first().associateBy { it.id }
            val sb = StringBuilder()
            sb.append("tipo,fecha,documento,cliente,producto,cantidad,precio_unit,total,es_credito,cobrado,categoria,monto,descripcion,cabys,causa,etapa\n")
            fun esc(v: String) = "\"" + v.replace("\"", "\"\"") + "\""
            invoices.forEach { inv ->
                sb.append(
                    listOf(
                        "VENTA", inv.ledger_date, inv.id.toString(),
                        esc(clients[inv.client_id]?.name ?: ""),
                        esc(items[inv.inventory_id]?.item_name ?: ""),
                        inv.quantity.toString(), inv.unit_price.toString(),
                        inv.total_amount.toString(),
                        if (inv.is_credit) "SI" else "NO", inv.paid_amount.toString(),
                        "", "", "", ""
                    ).joinToString(",")
                ).append("\n")
            }
            expenses.forEach { exp ->
                sb.append(
                    listOf(
                        "GASTO", exp.ledger_date, exp.id.toString(), "", "", "", "", "",
                        "", "", exp.category.name, exp.amount.toString(),
                        esc(exp.description), ""
                    ).joinToString(",")
                ).append("\n")
            }
            val waste = repository.getWasteForDate(currentDate).first()
            val itemsW = repository.getInventory().first().associateBy { it.id }
            waste.forEach { w ->
                sb.append(
                    listOf(
                        "MERMA", w.ledger_date, w.id.toString(), "",
                        esc(itemsW[w.inventory_id]?.item_name ?: ""),
                        w.quantity.toString(), "", w.financial_loss.toString(),
                        "", "", "", "", esc(w.reason), "",
                        w.causa, w.etapa
                    ).joinToString(",")
                ).append("\n")
            }
            payments.forEach { pay ->
                sb.append(
                    listOf(
                        "ABONO", pay.ledger_date, pay.id.toString(),
                        esc(clients[pay.client_id]?.name ?: ""), "", "", "",
                        pay.amount.toString(), "", pay.amount.toString(), "", "", "", ""
                    ).joinToString(",")
                ).append("\n")
            }
            val dir = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
            val file = java.io.File(dir, "tomateapp_$currentDate.csv")
            file.writeText(sb.toString(), Charsets.UTF_8)
            androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
        } catch (e: Exception) {
            null
        }
    }

    // Datos semilla solo para desarrollo: en release la bodega empieza vacía
    // y el operador registra sus propios clientes e inventario.
    // ensureSeeds() cuenta en BD dentro de una transacción (el .value del
    // StateFlow empieza vacío y duplicaba semillas en cada arranque).
    fun initTestData() {
        if (!BuildConfig.DEBUG) return
        viewModelScope.launch { repository.ensureSeeds() }
    }
}

data class DashboardMetrics(
    val hasLedger: Boolean = false,
    val initialInvestment: Double = 0.0,
    val totalSales: Double = 0.0,
    val totalCogs: Double = 0.0,
    val grossProfit: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalWaste: Double = 0.0,
    val realNetProfit: Double = 0.0,
    val cashOnHand: Double = 0.0
)

data class LastSaleDetails(
    val clientName: String,
    val itemName: String,
    val quantity: Int,
    val salePrice: Double,
    val totalAmount: Double
)
