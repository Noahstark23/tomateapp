package com.example.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migración v3 → v4 (créditos y abonos). Manual y sin pérdida de datos:
 * toda columna nueva trae DEFAULT y `phone` hereda lo que ya vivía en
 * `contact_info` (convención Fase 1). No usar destructive migration aquí.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE clients ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE clients ADD COLUMN type TEXT NOT NULL DEFAULT 'TRAMO'")
        db.execSQL("ALTER TABLE clients ADD COLUMN credit_limit REAL NOT NULL DEFAULT 0")
        // Los teléfonos ya digitados viven en contact_info: se heredan.
        db.execSQL("UPDATE clients SET phone = contact_info WHERE phone = ''")
        db.execSQL("ALTER TABLE invoices ADD COLUMN is_credit INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE invoices ADD COLUMN paid_amount REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE daily_ledgers ADD COLUMN total_collections REAL NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS payments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "client_id INTEGER NOT NULL, " +
                "invoice_id INTEGER, " +
                "ledger_date TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "timestamp INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_client ON payments(client_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_date ON payments(ledger_date)")
    }
}

/**
 * Migración v4 → v5 (lotes + base FE v4.4). Manual y sin pérdida:
 * tabla `lots` nueva, `waste.lot_id` nullable, receptor/CABYS/consecutivo con
 * DEFAULTs vacíos. Los consecutivos de facturas viejas se generan al exportar.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS lots (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "inventory_id INTEGER NOT NULL, " +
                "supplier TEXT NOT NULL DEFAULT '', " +
                "variedad TEXT NOT NULL DEFAULT '', " +
                "calibre TEXT NOT NULL DEFAULT '', " +
                "calidad TEXT NOT NULL DEFAULT '', " +
                "qty_initial INTEGER NOT NULL DEFAULT 0, " +
                "qty_current INTEGER NOT NULL DEFAULT 0, " +
                "cost_total REAL NOT NULL DEFAULT 0, " +
                "fecha_ingreso TEXT NOT NULL DEFAULT '', " +
                "fecha_limite TEXT NOT NULL DEFAULT '', " +
                "timestamp INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_lots_product ON lots(inventory_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_lots_limite ON lots(fecha_limite)")
        db.execSQL("ALTER TABLE waste ADD COLUMN lot_id INTEGER")
        db.execSQL("ALTER TABLE clients ADD COLUMN id_type TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE clients ADD COLUMN id_number TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE clients ADD COLUMN email TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE inventory ADD COLUMN cabys TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE inventory ADD COLUMN unit TEXT NOT NULL DEFAULT 'Unid'")
        db.execSQL("ALTER TABLE invoices ADD COLUMN consecutive TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE invoices ADD COLUMN fe_status TEXT NOT NULL DEFAULT 'BORRADOR'")
    }
}
