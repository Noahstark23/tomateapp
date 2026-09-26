# FINANCIAL_ENGINE

Fuente: `app/src/main/java/com/example/finance/FinancialEngine.kt` (Kotlin puro, 126 líneas).
Consumido por `FinancialViewModel` (ventana 7 días) y pintado en `DashboardScreen`.

## Funciones
- `realNetProfit(sales, cogs, expenses, wasteValue) = sales − cogs − expenses − wasteValue`.
- `computeSafeWithdrawal(cashOnHand, restockCost=COGS hoy, projectedDailyExpenses, rate=0.15)`:
  `emergencyFund = max(cash,0)×rate`; `amount = cash − (restock + projected + fund)`;
  `isSafe = amount > 0`.
- `computeRunway(capital=caja hoy, outflows=[gastos+merma] 7d)`:
  `burn = average(outflows)`; si `burn ≤ 0` → `runwayDays=null`, `HEALTHY` si `capital ≥ 0` else `CRITICAL`;
  si no `days = max(capital,0)/burn`; `<15 CRITICAL`, `<30 WARNING`, else `HEALTHY`.
- `computeLeakage(waste, cogs) = waste/(waste+cogs)×100`; `0` si nada consumido. UI en rojo si `>10%`.
- `projectDailyExpenses(recents, fallback=gastos hoy) = average` o fallback si vacío.

## Constantes
`DEFAULT_EMERGENCY_FUND_RATE=0.15`, `CRITICAL_RUNWAY_DAYS=15`, `HEALTHY_RUNWAY_DAYS=30`,
`ANALYSIS_WINDOW_DAYS=7` (en `FinancialViewModel`).

## Ejemplo numérico
Caja ₡200 000, COGS hoy ₡60 000, gastos proyectados ₡25 000 → fondo ₡30 000 →
extraíble = 200 000 − (60 000+25 000+30 000) = **₡85 000** (`isSafe=true`).
Si la caja fuera ₡80 000 → 80 000 − 115 000 = **−₡35 000** → UI: "No puedes retirar,
estás descapitalizando". Outflows 7d `[20k,22k,18k,25k,21k,19k,23k]` → burn ₡21 143;
con caja ₡200 000 → runway 9.5d → **CRITICAL**.

## Edge cases
`amount` puede ser negativo (no clampear, la UI lo explica); `runwayDays=null` = sin quema
("Sin quema de capital detectada"); `leakage` con denominador 0 → `0.0`; `Double` para dinero
(redondear a colón en UI con `NumberFormat es_CR`; migrar a `Long` céntimos post-hoy).

## Cobertura
`FinancialEngineTest.kt`: 13 tests reales (profit, withdrawal, runway, leakage, proyección).
Falta hoy: test de `recalculateLedger` con datos bodega (venta+gasto+merma encadenados) y
screenshot test del semáforo. Ver `docs/TESTING_QA.md`.
