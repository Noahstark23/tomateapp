# TESTING_QA

## Pirámide hoy
- Unit JVM: `FinancialEngineTest.kt` (13 tests: profit, withdrawal, runway, leakage, proyección) —
  corre sin Android porque `FinancialEngine` es Kotlin puro.
- Robolectric/Roborazzi: declarados pero `ExampleRobolectricTest` espera `"My Application"` y
  `strings.xml` dice `"Nortex Dashboard"` → en rojo hasta actualizar el assert.
- Instrumentados: `ExampleInstrumentedTest` espera paquete `com.example` pero el `applicationId`
  es `com.aistudio.dashboard.nortex` → actualizar.

## Comandos
```bash
./gradlew testDebugUnitTest                 # serial, ~1-2 min
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.ExampleInstrumentedTest  # requiere emulator-5580
```

## QA manual bodega (15 min, emulator-5580)
1. Fresh install → "Director Financiero · TomateApp CFO de bolsillo" → "Iniciar Día Operativo".
2. Abrir día con ₡150 000 → dashboard con semáforo + KPIs en ₡0 + "Ajustar" capital.
3. Vender (Cliente A, Caja Primera, 2 uds) → total/ganancia preview, ticket BT falla con toast (esperado),
   ledger recalcula, stock baja.
4. Gastar TRANSPORTE ₡10 000 → caja y ganancia bajan ₡10 000.
5. Mermar 1 ud motivo "podrido" → stock y ganancia bajan, caja intacta.
6. Reportes: inventario valorizado a costo + P&L con margen = `real_net_profit/total_sales×100`.
7. Rotar pantalla, fondo/foreground, sin internet: 0 crash, 0 pérdida.
8. Capital extraíble ≤0 → mensaje descapitalización; fuga 7d visible; leakage >10% en rojo.

## Criterios de bloqueo release hoy
- `./gradlew assembleDebug` verde + APK instala en emulator-5580.
- `FinancialEngineTest` 13/13 verde.
- Cierre diario completo sin crash y matemáticas (profit/cash/withdrawal/runway/leakage) verificadas
  contra `docs/FINANCIAL_ENGINE.md` con 1 caso real (₡150k/venta/gasto/merma).
- Roborazzi/screenshots: opcional hoy (solo si sobra 30 min).
