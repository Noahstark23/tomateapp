package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "inventory")
data class Inventory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val item_name: String,
    val purchase_price: Double,
    val sale_price: Double,
    val initial_stock: Int,
    val current_stock: Int
)

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val contact_info: String,
    /** Teléfono sanitizado (solo dígitos). En v3 vivía en contact_info. */
    val phone: String = "",
    /** TRAMO, FERIA, SODA, SUPER, PROVEEDOR. */
    val type: String = "TRAMO",
    /** Cupo de crédito en CRC. 0 = sin crédito. */
    val credit_limit: Double = 0.0
)

/**
 * Estado financiero diario (P&L + caja). Es una vista materializada:
 * los agregados se recalculan desde invoices/expenses/waste en cada
 * mutación (ver AppDao.recalculateLedger), nunca se suman incrementalmente.
 *
  * Modelo financiero:
  *  - real_net_profit = total_sales - total_cogs - total_expenses - total_waste_value
  *    (la inversión inicial es capital de trabajo, NO un costo; las ventas a
  *    crédito SÍ cuentan como ingreso + COGS al vender: criterio devengo)
  *  - cash_on_hand    = initial_investment + cash_sales + total_collections - total_expenses
  *    (la merma destruye valor de inventario pero no toca el efectivo; el
  *    crédito NO toca caja hasta que se cobra el abono)
  */
@Entity(tableName = "daily_ledgers")
data class DailyLedger(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val initial_investment: Double,
    val total_sales: Double = 0.0,
    val total_cogs: Double = 0.0,
    val total_expenses: Double = 0.0,
    val total_waste_value: Double = 0.0,
    val real_net_profit: Double = 0.0,
    val cash_on_hand: Double = 0.0,
    /** Abonos cobrados el día (sí son caja). */
    val total_collections: Double = 0.0
)

/**
 * Factura con trazabilidad de costos: unit_cost/total_cost congelan el
 * purchase_price del inventario al momento de la venta, de modo que un
 * cambio futuro de precios no altera la historia contable.
 * profit_margin = (total_amount - total_cost) / total_amount * 100.
 */
@Entity(
    tableName = "invoices",
    indices = [Index("ledger_date"), Index("inventory_id")]
)
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ledger_date: String,
    val client_id: Int,
    val inventory_id: Int,
    val quantity: Int,
    val unit_price: Double,
    val unit_cost: Double,
    val total_amount: Double,
    val total_cost: Double,
    val profit_margin: Double,
    /** true = fiado: no entró efectivo, se cobra con abonos. */
    val is_credit: Boolean = false,
    /** Monto ya cobrado de esta factura (abonos directos). */
    val paid_amount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
) {
    /** Saldo vivo de la factura. */
    val balance: Double get() = total_amount - paid_amount
}

enum class ExpenseCategory {
    TRANSPORTE,
    SALARIO,
    EMPAQUE,
    OTROS
}

/** Gasto operativo (fuga de efectivo): reduce cash_on_hand y la ganancia real. */
@Entity(tableName = "expenses", indices = [Index("ledger_date")])
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ledger_date: String,
    val category: ExpenseCategory,
    val amount: Double,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Merma de inventario (tomate podrido/aplastado/perdido): reduce stock y
 * ganancia real, pero no el efectivo. financial_loss congela el
 * purchase_price del momento del registro (quantity * purchase_price).
 */
@Entity(
    tableName = "waste",
    indices = [Index("ledger_date"), Index("inventory_id")]
)
data class Waste(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ledger_date: String,
    val inventory_id: Int,
    val quantity: Int,
    val financial_loss: Double,
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/** DTO para agrupar gastos por categoría (no es tabla). */
data class CategoryTotal(
    val category: ExpenseCategory,
    val total: Double
)

/** Entradas del día agrupadas por producto (no es tabla). */
data class SalesByProduct(
    val itemName: String,
    val inventoryId: Int,
    val quantity: Int,
    val total: Double
)

/** Resumen de facturación del día (no es tabla). */
data class SalesSummary(
    val invoiceCount: Int,
    val totalSales: Double,
    val avgTicket: Double
)

/**
 * Abono cobrado a un cliente. Suma caja en el `ledger_date` del día del pago
 * (no en el día de la venta). `invoice_id` null = abono global al cliente,
 * se reparte a sus facturas más viejas primero (FIFO).
 */
@Entity(
    tableName = "payments",
    indices = [Index("client_id"), Index("ledger_date")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val client_id: Int,
    val invoice_id: Int? = null,
    val ledger_date: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis()
)

/** Saldo vivo por cobrar de un cliente (derivado, no es tabla). */
data class ClientBalance(
    val clientId: Int,
    val balance: Double
)

/** Total por día para series de proyección (no es tabla). */
data class DateTotal(
    val date: String,
    val total: Double
)
