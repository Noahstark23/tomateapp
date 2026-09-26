# TomateApp — CFO de bolsillo para bodegas de tomate en Costa Rica

App Android nativa (Kotlin + Jetpack Compose + Room) para operar tramos y bodegas de abasto:
comprar cajas en CENADA de madrugada, vender por kg/caja en feria o punto fijo, controlar merma,
saber la ganancia real del día y cuánto efectivo se puede retirar sin descapitalizar el negocio.

- Package código: `com.example` · `applicationId: com.aistudio.dashboard.nortex`
- `minSdk 26`, `targetSdk 36`, `compileSdk 36.1`, AGP `9.1.1`, Kotlin `2.2.10`, Room `2.7.0` (DB **v4** con migración 3→4 sin pérdida, `exportSchema=true`)
- Tabs: Inicio (CFO) · 💰 Caja (flujo + predicción) · 💳 Créditos (fiado/abonos) · 📈 Reportes (lotes + FE)
- Lotes FEFO con vencimiento; FE v4.4 pre-firma (falta certificado .p12 y ATV para validez)
- Moneda: colones (CRC, `Locale("es","CR")`). Sin decimales en UI, redondeo feria a ₡50.
- Offline-first local (Room). Sin backend hoy: Retrofit/Moshi/OkHttp están declarados pero no usados.
- IA Gemini: declarada en `metadata.json` y `.env.example`, pero `firebase-ai` está comentado en
  `app/build.gradle.kts:95` y nunca se usa. Decisión pendiente (ver `docs/ROADMAP_HOY.md`).

## Quickstart (Mac, sin Android Studio)

```bash
export JAVA_HOME=/Users/stark/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
export ANDROID_HOME=$HOME/Library/Android/sdk

# 1. Wrapper (ya generado en el repo)
./gradlew assembleDebug

# 2. Instalar en el emulador ya encendido (NicaLab_API35, emulator-5580)
adb -s emulator-5580 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5580 shell am start -n com.aistudio.dashboard.nortex/com.example.MainActivity
```

Detalle completo: `docs/BUILD_RUN.md`.

## Qué hace hoy (verificado en emulador 2026-09-26)

1. **Iniciar Día Operativo**: pide Capital de Trabajo (CRC). No es un costo, es el efectivo inicial
   (`AppDao.setInitialInvestment`, `DashboardScreen.StartDayCard`).
2. **Dashboard**: semáforo salud (verde/ámbar/rojo), Capital Extraíble Hoy con desglose
   (caja − reposición − gastos proyectados − fondo 15%), KPIs (ventas, ganancia real, gastos, merma + fuga 7d),
   gráfica apilada 7 días, acciones rápidas.
3. **Registrar Venta**: contado o **fiado** (switch con cupo visible y bloqueo por sobre-cupo),
   valida stock, congela costo, calcula total/ganancia, intenta imprimir ticket ESC/POS por Bluetooth.
   Botón "+ Nuevo cliente" y "💬 WhatsApp" con pedido pre-llenado.
4. **Registrar Gasto**: categoría TRANSPORTE/SALARIO/EMPAQUE/OTROS + monto + descripción.
5. **Registrar Merma**: producto + cantidad + motivo, valora pérdida a costo (no toca caja).
6. **👤 Clientes y Proveedores**: alta con teléfono validado, WhatsApp por fila y mensaje libre a
   proveedor con plantilla de cotización.
7. **💰 Caja**: entradas (contado + abonos) vs salidas, FIADO HOY separado, neto caja vs ganancia
   real, predicción 7 días de entradas de efectivo con alerta de quiebre.
8. **💳 Créditos**: total por cobrar, saldos con antigüedad, abonos (suman caja el día del pago,
   FIFO, sin sobrepagos), cupo editable, WhatsApp de cobro con saldo.
9. **Reportes**: inventario (stock + valor a costo, tap para precios/CABYS/entradas, 📦 Lotes FEFO),
   "+ Producto", **Exportar CSV**, **Facturas electrónicas pre-firma** (XML v4.4 + selector IVA),
   y P&L diario últimos 30 días.

## Mapa de docs

| Doc | Para qué |
|---|---|
| `docs/PRD.md` | Visión, usuarios, alcance MVP y no-MVP |
| `docs/ARCHITECTURE.md` | Capas, flujo venta→ledger→UI, decisiones |
| `docs/DATA_MODEL.md` | Tablas Room, invariantes contables, fórmulas |
| `docs/FINANCIAL_ENGINE.md` | Motor CFO: fórmulas, constantes, edge cases |
| `docs/MODELO_BODEGA_CR.md` | Dominio CR: CENADA, precios, merma, fiscal, FEFO |
| `docs/BUILD_RUN.md` | Compilar/correr en Mac sin Android Studio |
| `docs/TESTING_QA.md` | Tests + checklist para declarar done hoy |
| `docs/USER_GUIDE_OPERADOR.md` | Hoja 1-página para el bodeguero |
| `docs/CAJA_CLIENTES.md` | Flujo de caja + clientes/WhatsApp v0.2.0 y Créditos v0.3.0 |
| `docs/FE_LOTES.md` | Lotes FEFO + FE v4.4 pre-firma v0.4.0 y pendiente (firma/ATV) |
| `docs/ROADMAP_HOY.md` | Plan <8h para terminar hoy |
| `docs/BACKLOG.md` | Parking post-hoy + deuda conocida |
| `CHANGELOG.md` | Historial de versiones |

## Estado y gaps P0 (hoy)

- ✅ Compila: `./gradlew assembleDebug` OK (38 tasks, 1m06s). APK fresco `app/build/.../app-debug.apk` (11MB).
- ✅ Corre en emulator-5580, pantalla "Director Financiero · TomateApp CFO de bolsillo".
- ⚠️ `ExpenseCategory` (enum Room) funciona hoy por KSP pero `AppDatabase` no registra `@TypeConverters`
  explícito — endurecer antes de agregar categorías.
- ⚠️ `fallbackToDestructiveMigration()` + `version=3, exportSchema=false`: cualquier cambio de entities
  borra datos reales. Activar migraciones antes del piloto.
- ⚠️ Dinero en `Double`, `initTestData()` inserta `Client A/B` en producción, paquete `com.example`,
  `debug.keystore` commiteado, `.build-outputs/app-debug.apk` desactualizado (decía "Dashboard Diario").
- ❌ Falta modelo bodega/lote: hoy el stock es global, sin bodega, lote, variedad, calibre, FEFO,
  ni trazabilidad proveedor→lote→venta. Ver `docs/MODELO_BODEGA_CR.md` y `docs/BACKLOG.md`.

Origen AI Studio: https://ai.studio/apps/89700435-b0a1-4def-9890-077a5f18505f
