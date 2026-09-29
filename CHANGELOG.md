# CHANGELOG

## v0.6.0-compras-arqueo (loop continuo)
- **Compra a proveedor** en una transacción: lote + stock + gasto COMPRA_MERCADERIA.
  La compra SÍ sale de caja y baja la ganancia (era el egreso invisible).
  E2E: día ₡200 000 → compra 5× ₡25 000 → caja ₡175 000, gastos ₡25 000.
- **Arqueo de caja** en tab Caja: conteo físico vs sistema con diferencia
  (no ajusta, se investiga) + historial del día. E2E: contado ₡175 000 → dif. ₡0 ✓.
- Migración v6→v7 (tabla `cash_counts`), schema `7.json`. 26/26 tests verdes.

## v0.5.0-robustez (2026-09-26, loop continuo: lista para operar)
- **Bug crítico corregido**: `initTestData` leía `StateFlow.value` (vacío antes del
  primer emit de Room) y duplicaba clientes/productos en cada arranque. Ahora
  `ensureSeeds()` transaccional con COUNT en BD. Verificado: reinicio en frío
  deja exactamente 1 Primera + 1 Segunda.
- **Respaldo/restauración**: exporta el `.db` (con checkpoint WAL) por share +
  importa desde picker con reinicio. Sin esto, perder el teléfono = perder todo.
- **Abono a factura específica** (antes solo FIFO global) con validación de saldo.
- **Merma codificada**: causa (PODRIDO/APLASTADO/DESHIDRATADO/OTRO) + etapa
  (COSECHA/TRANSPORTE/BODEGA/TRAMO), migración v5→v6, reporte por causa y CSV
  con columnas causa/etapa. Histórica = "Sin clasificar".
- **Tipos de cliente** (TRAMO/FERIA/SODA/SUPER/PROVEEDOR) en el alta + cupo editable
  también desde Clientes.
- **Alertas operativas** en Inicio: lotes por vencer (remate), sin stock y stock bajo.
- QA: ANR investigado con traces — fue input-timeout por atasco del emulador
  (main thread idle), no bug de la app. 26/26 tests verdes, E2E completo.

## v0.4.0-lotes-fe (2026-09-26, bloque FE + lotes, QA loop verde)
- DB **v5 + migración 4→5 sin pérdida** (tabla `lots`, `waste.lot_id`, receptor,
  CABYS/unidad, consecutivo/`fe_status`; schema `5.json` versionado).
- **Lotes FEFO**: entrada con vencimiento auto, consumo en venta y merma,
  badge SALE PRIMERO + alerta ≤1 día. E2E: 10 → 7 → 6 → 5/10.
- **FE v4.4 pre-firma**: receptor por cliente, CABYS por producto, consecutivo
  auto, `FeXml` + 4 tests, sección con selector IVA y botón XML con validación
  (cédula + CABYS). E2E: `FE-00100001010000000004.xml` → PENDIENTE.
- 26/26 tests verdes. Detalle en `docs/FE_LOTES.md`. Pendiente validez real:
  `FeConfig` emisor, tarifa IVA con contador, firma .p12 y envío ATV.

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
