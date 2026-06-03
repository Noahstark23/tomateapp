package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "inventory")
data class Inventory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val item_name: String,
    val purchase_price: Double,
    val sale_price: Double,
    val current_stock: Int
)

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val contact_info: String
)

@Entity(tableName = "daily_ledgers")
data class DailyLedger(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val initial_investment: Double,
    val total_sales: Double = 0.0,
    val net_profit: Double = 0.0
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ledger_date: String,
    val client_id: Int,
    val total_amount: Double,
    val timestamp: Long = System.currentTimeMillis()
)
