# CHANGELOG

## v0.3.0-creditos (2026-09-26, sprint final: lista para uso)
- Schema Room **v4 + migración 3→4 sin pérdida** (verificada en emulador con datos reales):
  `clients` += `phone/type/credit_limit` (phone hereda `contact_info`),
  `invoices` += `is_credit/paid_amount`, `daily_ledgers` += `total_collections`,
  tabla nueva `payments`. Adiós `fallbackToDestructiveMigration`, `exportSchema=true`.
- **Venta a crédito**: switch Contado/Fiado, cupo visible, bloqueo por sobre-cupo (UI + DAO).
  Contabilidad devengo: el fiado suma ganancia + COGS al vender, **no** toca caja.
- **Tab 💳 Créditos**: total por cobrar, saldos con antigüedad (0-7/8-15/16-30/+30),
  abonos (suman caja el día del pago, rechazan sobrepago, FIFO), cupo editable,
  WhatsApp de cobro con saldo real.
- **Tab Caja corregido**: neto = contado + abonos − gastos; FIADO HOY separado;
  proyección usa entradas de efectivo (no ventas a crédito).
- **Productos**: alta, edición de precios y entradas de stock desde Reportes.
- **Exportar CSV** del día (ventas/gastos/abonos + columna CABYS) vía share sheet + FileProvider.
- Loop QA E2E emulator-5580: día ₡200 000 → cupo ₡50 000 → fiado ₡12 000 (caja quieta
  en ₡212 000) → sobre-cupo bloqueado → abono ₡5 000 (caja ₡217 000, saldo ₡7 000) →
  CSV `tomateapp_2026-09-25.csv` generado → producto Cherry creado. 22/22 tests verdes.

## v0.2.0-caja-clientes (2026-09-26, QA agentes + features negocio)
- QA de 3 agentes: 7 FAILs confirmados (clientes, créditos, proveedores, predicciones,
  flujo, alta productos, reportes parcial) → implementada Fase 1.
- Nuevo tab 💰 Caja: entradas/salidas del día, neto caja vs ganancia real, predicción 7 días
  con alerta de quiebre (`projectCash7Days`, `findBreakEvenDay` + 3 tests).
- Alta de clientes con teléfono validado + WhatsApp por cliente y mensaje a proveedor.
- 22/22 tests verdes. E2E emulator-5580: día ₡200 000 → cliente SodaLaEsquina → venta
  2× Primera → Caja muestra entradas ₡12 000, neto +₡12 000, proyección ₡1 714,29/día.
- Créditos en diseño Fase 2 (`docs/CAJA_CLIENTES.md`), implementados en v0.3.0.

## v0.1.0-hoy (2026-09-26, tag `v0.1.0-hoy`)
- Generado Gradle wrapper 9.7.1; `./gradlew assembleDebug` verde; 15/15 unit tests verdes
  (`FinancialEngineTest` 13 + `ExampleRobolectricTest` + `ExampleUnitTest`).
- Fixes: asserts `Example*Test` actualizados, `Converters` explícito para `ExpenseCategory`,
  `initTestData()` solo `BuildConfig.DEBUG`, secretos des-trackeados
  (`.build-outputs/app-debug.apk`, `debug.keystore.base64`) y `.gitignore` ampliado.
- Verificación E2E en emulator-5580: abrir día ₡150 000 → venta 2× Caja Primera → ventas ₡12 000,
  ganancia real ₡2 000, caja ₡162 000, extraíble ₡127 700, semáforo saludable.
- Reescrito `README.md` + 10 docs en `docs/` (PRD, arquitectura, datos, motor financiero,
  dominio CR, build, testing, guía operador, roadmap, backlog).

## v0.0.1-ai-studio (previo)
- Proyecto AI Studio (`Nortex Dashboard`): ledger diario, CFO (runway/withdrawal/leakage),
  impresión ESC/POS básica, reportes. APK `.build-outputs/app-debug.apk` (15MB, etiqueta vieja
  "Dashboard Diario"). Sin wrapper, sin docs.
