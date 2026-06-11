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
    val contact_info: String
)

/**
 * Estado financiero diario (P&L + caja). Es una vista materializada:
 * los agregados se recalculan desde invoices/expenses/waste/inventory_purchases
 * en cada mutación (ver AppDao.recalculateLedger), nunca se suman
 * incrementalmente.
 *
 * Modelo financiero:
 *  - real_net_profit = total_sales - total_cogs - total_expenses - total_waste_value
 *    (la inversión inicial es capital de trabajo, NO un costo; las compras de
 *    inventario tampoco son costo: su valor golpea el P&L al venderse o perderse)
 *  - cash_on_hand    = initial_investment + total_sales - total_expenses - total_purchases
 *    (la merma destruye valor de inventario pero no toca el efectivo)
 */
@Entity(tableName = "daily_ledgers")
data class DailyLedger(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val initial_investment: Double,
    val total_sales: Double = 0.0,
    val total_cogs: Double = 0.0,
    val total_expenses: Double = 0.0,
    val total_waste_value: Double = 0.0,
    val total_purchases: Double = 0.0,
    val real_net_profit: Double = 0.0,
    val cash_on_hand: Double = 0.0
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
    val timestamp: Long = System.currentTimeMillis()
)

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

/**
 * Compra/reposición de inventario: convierte efectivo en mercancía. Reduce
 * cash_on_hand pero NO la ganancia (no es un gasto: el costo golpea el P&L
 * cuando la mercancía se vende como COGS o se pierde como merma). Al
 * registrarla, el purchase_price del producto se actualiza a costo promedio
 * ponderado (ver FinancialEngine.weightedAverageCost).
 */
@Entity(
    tableName = "inventory_purchases",
    indices = [Index("ledger_date"), Index("inventory_id")]
)
data class InventoryPurchase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ledger_date: String,
    val inventory_id: Int,
    val quantity: Int,
    val unit_cost: Double,
    val total_cost: Double,
    val timestamp: Long = System.currentTimeMillis()
)

/** DTO para agrupar gastos por categoría (no es tabla). */
data class CategoryTotal(
    val category: ExpenseCategory,
    val total: Double
)
