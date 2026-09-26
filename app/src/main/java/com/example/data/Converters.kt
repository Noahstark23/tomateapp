package com.example.data

import androidx.room.TypeConverter

/**
 * Conversores explícitos de Room. Sin esto, agregar o renombrar valores de
 * [ExpenseCategory] puede romper la lectura de gastos históricos; con esto el
 * contrato queda fijado: la categoría se guarda como su nombre (TEXT).
 */
class Converters {
    @TypeConverter
    fun fromExpenseCategory(value: ExpenseCategory): String = value.name

    @TypeConverter
    fun toExpenseCategory(value: String): ExpenseCategory =
        runCatching { ExpenseCategory.valueOf(value) }.getOrDefault(ExpenseCategory.OTROS)
}
