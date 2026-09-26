# CHANGELOG

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
