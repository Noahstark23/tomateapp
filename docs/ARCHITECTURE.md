# ARCHITECTURE

## Stack real (verificado)
- UI: Jetpack Compose + Material3 + Navigation Compose (`dashboard`, `reports`), `enableEdgeToEdge()`.
- Estado: `ViewModel` + `StateFlow` + `collectAsStateWithLifecycle`. Sin Hilt/Koin: `DashboardViewModelFactory`.
- Datos: Room `2.7.0` con KSP (`nortex_database`, `version=3`, `fallbackToDestructiveMigration()`).
- Red: Retrofit `2.12.0` + Moshi + OkHttp declarados en `app/build.gradle.kts` pero **no usados**.
- Secrets: `secrets-gradle-plugin 2.0.1` lee `.env` (solo `GEMINI_API_KEY` placeholder).
- Build: `compileSdk 36.1`, `minSdk 26`, `targetSdk 36`, AGP `9.1.1`, Gradle wrapper `9.7.1` (generado hoy).

## Capas
```
ui/                  Compose screens + ViewModels (Dashboard, Reports, FinancialCharts, theme)
  │ collectAsState / eventos (setInitialInvestment, processSale, registerExpense/Waste)
data/                Room + Repository
  │ AppDatabase → AppDao → DashboardRepository → ViewModels
finance/             FinancialEngine (object Kotlin puro, sin Android)
printer/             PrintService (Bluetooth SPP + ESC/POS, suspend, Dispatchers.IO)
```

## Flujo de datos (venta)
1. `SaleDialog` valida `qty ≤ current_stock` en memoria.
2. `DashboardViewModel.processSale()` → `DashboardRepository.registerSale()` → `AppDao.processSale()` (transaction):
   `getInventoryByIdSync` → `subtractInventoryStock` → congela `unit_cost/purchase_price`,
   calcula `margin`, `insertInvoice` → `recalculateLedger(date)`.
3. `recalculateLedger` suma `invoices/expenses/waste` con `COALESCE(SUM,0)` y reescribe `DailyLedger`
   (vista materializada, nunca incremental).
4. `currentMetrics` (operativo) y `FinancialViewModel.cfoState` (ventana 7d) re-emiten; Compose recompone.

Gasto y merma siguen el mismo patrón (`processExpense`, `processWaste`).

## Decisiones
- `FinancialEngine` sin Android para testear sin Robolectric (13 tests reales).
- Ledger materializado por `YYYY-MM-DD`: simple y auditable; costo: re-lee agregados en cada mutación.
- `MainActivity` crea DB + repo + factory a mano; ambos ViewModels comparten factory.
- `DashboardScreen.kt` (1137 líneas) concentra top bar, KPIs, gráficas, 4 diálogos y `StartDayCard`.

## Qué no hacer hoy
No agregar DI (Hilt), no multi-módulo, no Vico/MPAndroid para gráficas, no backend/sync
(primero estabilizar ledger + migraciones). Extraer diálogos a archivos solo si sobra tiempo.
