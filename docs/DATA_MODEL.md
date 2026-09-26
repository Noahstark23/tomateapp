# DATA_MODEL

Fuente: `app/src/main/java/com/example/data/Entities.kt`, `AppDao.kt`, `AppDatabase.kt`.

## ER (texto)
```
Inventory 1──N Invoice N──1 Client        (sin FK declaradas)
Inventory 1──N Waste
DailyLedger 1──N Invoice / Expense / Waste   (por ledger_date = YYYY-MM-DD)
```

## Tablas
| Tabla | Campos clave | Notas |
|---|---|---|
| `inventory` | `id, item_name, purchase_price, sale_price, initial_stock, current_stock` | Stock global. Sin bodega/lote/unidad/variedad/proveedor/vencimiento. Dinero `Double`. |
| `clients` | `id, name, contact_info` | Sin tipo (tramo/feria/soda), teléfono, crédito/saldo. Seed `Client A/B` en `initTestData()`. |
| `daily_ledgers` | `date PK, initial_investment, total_sales, total_cogs, total_expenses, total_waste_value, real_net_profit, cash_on_hand` | Vista materializada. |
| `invoices` | `id, ledger_date, client_id, inventory_id, quantity, unit_price, unit_cost, total_amount, total_cost, profit_margin, timestamp` | 1 producto por factura. Costo congelado. Índices `(ledger_date, inventory_id)`. |
| `expenses` | `id, ledger_date, category TRANSPORTE/SALARIO/EMPAQUE/OTROS, amount, description, timestamp` | Enum sin `@TypeConverters` explícito en DB (endurecer). |
| `waste` | `id, ledger_date, inventory_id, quantity, financial_loss, reason, timestamp` | `reason` libre. Sin etapa codificada ni foto. |

## Invariantes (no romper)
1. `real_net_profit = total_sales − total_cogs − total_expenses − total_waste_value`.
   La inversión inicial es capital de trabajo, **no** un costo.
2. `cash_on_hand = initial_investment + total_sales − total_expenses`.
   La merma destruye inventario y ganancia pero **no** toca caja; el COGS tampoco sale de caja
   (ya salió al comprar/invertir).
3. `unit_cost/total_cost/financial_loss` se congelan al precio de compra vigente; un cambio futuro
   de `purchase_price` no reescribe historia.
4. Todo pasa por `AppDao.recalculateLedger(date)` con `SUM(COALESCE)`; jamás sumar incremental.
5. Venta/merma con `qty ≤ 0` o `qty > current_stock` → `null`, sin mutación.

## Índices y migraciones
- Índices en `ledger_date` e `inventory_id`. Sin FKs ni `ON DELETE`.
- `AppDatabase version=3, exportSchema=false, fallbackToDestructiveMigration()` → **cualquier cambio
  de entities borra la bodega real**. Antes del piloto: `exportSchema=true` + migraciones o AutoMigration.
- Evolución a bodega/lote (post-hoy): `warehouse`, `lot` (proveedor, variedad, calibre, calidad,
  fechas ingreso/límite/alerta), `stock_by_warehouse`, `movimiento` con causa codificada. Ver `BACKLOG.md`.
