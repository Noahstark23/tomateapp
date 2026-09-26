# CHANGELOG

## v0.2.0-caja-clientes (2026-09-26, QA agentes + features negocio)
- QA de 3 agentes: 7 FAILs confirmados (clientes, créditos, proveedores, predicciones,
  flujo, alta productos, reportes parcial) → implementada Fase 1.
- Nuevo tab 💰 Caja: entradas/salidas del día, neto caja vs ganancia real, predicción 7 días
  con alerta de quiebre (`projectCash7Days`, `findBreakEvenDay` + 3 tests).
- Alta de clientes con teléfono validado + WhatsApp por cliente y mensaje a proveedor.
- 22/22 tests verdes. E2E emulator-5580: día ₡200 000 → cliente SodaLaEsquina → venta
  2× Primera → Caja muestra entradas ₡12 000, neto +₡12 000, proyección ₡1 714,29/día.
- Créditos quedan en diseño Fase 2 (`docs/CAJA_CLIENTES.md`): requiere migración v3→v4.

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
