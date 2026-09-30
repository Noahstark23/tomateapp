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

/**
 * Migración v5 → v6 (merma codificada). Solo columnas con DEFAULT: la merma
 * histórica queda con causa/etapa vacías (se reporta como "Sin clasificar").
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE waste ADD COLUMN causa TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE waste ADD COLUMN etapa TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * Migración v6 → v7 (arqueo de caja). Solo tabla nueva, sin tocar datos.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS cash_counts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "ledger_date TEXT NOT NULL, " +
                "expected REAL NOT NULL, " +
                "counted REAL NOT NULL, " +
                "diff REAL NOT NULL, " +
                "note TEXT NOT NULL DEFAULT '', " +
                "timestamp INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_cash_counts_date ON cash_counts(ledger_date)")
    }
}

/**
 * Migración v8 → v9 (turnos + precios por canal). DDL copiado exacto del
 * schema generado (app/schemas/.../9.json): sin DEFAULTs y con los nombres
 * de índice de Room, porque la validación compara columna por columna.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS cash_shifts (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`ledger_date` TEXT NOT NULL, " +
                "`opened_at` INTEGER NOT NULL, " +
                "`closed_at` INTEGER NOT NULL, " +
                "`opening_cash` REAL NOT NULL, " +
                "`expected_cash` REAL NOT NULL, " +
                "`counted_cash` REAL NOT NULL, " +
                "`diff` REAL NOT NULL, " +
                "`note` TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_cash_shifts_ledger_date " +
                "ON cash_shifts (ledger_date)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS price_rules (" +
                "`inventory_id` INTEGER NOT NULL, " +
                "`channel` TEXT NOT NULL, " +
                "`price` REAL NOT NULL, " +
                "PRIMARY KEY(inventory_id, channel))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_price_rules_inventory_id " +
                "ON price_rules (inventory_id)"
        )
    }
}

/**
 * Migración v7 → v8 (multi-bodega). Crea bodegas/stock/traslados/settings y
 * siembra "Tramo Principal" (id 1, primera fila AUTOINCREMENT) como bodega
 * activa, repartiendo el stock global actual en ella. Sin pérdida de datos.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS warehouses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "location TEXT NOT NULL DEFAULT '')"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS warehouse_stock (" +
                "warehouse_id INTEGER NOT NULL, " +
                "inventory_id INTEGER NOT NULL, " +
                "quantity INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY(warehouse_id, inventory_id))"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_wh_stock_product ON warehouse_stock(inventory_id)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS transfers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "from_warehouse_id INTEGER NOT NULL, " +
                "to_warehouse_id INTEGER NOT NULL, " +
                "inventory_id INTEGER NOT NULL, " +
                "quantity INTEGER NOT NULL, " +
                "ledger_date TEXT NOT NULL, " +
                "note TEXT NOT NULL DEFAULT '', " +
                "timestamp INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transfers_date ON transfers(ledger_date)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS settings (" +
                "`key` TEXT PRIMARY KEY NOT NULL, " +
                "`value` TEXT NOT NULL DEFAULT '')"
        )
        db.execSQL("ALTER TABLE lots ADD COLUMN warehouse_id INTEGER NOT NULL DEFAULT 1")
        db.execSQL("INSERT INTO warehouses (name, location) VALUES ('Tramo Principal', '')")
        db.execSQL(
            "INSERT INTO warehouse_stock (warehouse_id, inventory_id, quantity) " +
                "SELECT 1, id, current_stock FROM inventory"
        )
        db.execSQL("INSERT OR REPLACE INTO settings (`key`, `value`) VALUES ('active_warehouse', '1')")
    }
}
