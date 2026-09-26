# BACKLOG (post-hoy, MoSCoW)

## Sprint+1 — bodega real
- **Must**: modelo `warehouse` + `lot` (proveedor, finca, variedad, calibre GG/G/M, calidad 1/2/3,
  madurez, kg/cajas, costo total, fechas ingreso/límite/alerta) + FEFO + bloqueo de vencido.
  Merma por etapa codificada (COSECHA/TRANSPORTE/BODEGA/TRAMO × pudrición/aplastado/deshidratación) + foto.
- **Must**: migraciones Room (quitar `fallbackToDestructiveMigration`), dinero `Long` céntimos,
  FKs e índices, tests DAO con `room-testing`.
- **Must**: clientes por tipo (TRAMO/FERIA/SODA/SUPER) + crédito/límite/saldo + precio por canal;
  ventas multi-ítem + devoluciones/anulaciones + cuentas por cobrar/pagar + arqueo/cierre por turno.
- **Should**: importar moda SIMM diaria + precio mínimo `costo×1.10×1.15`; alertas vence-mañana,
  merma >12%, stock bajo; registro T°/HR 2×/día; exportar CSV/PDF con CABYS (FE v4.4 ready).
- **Should**: selector impresora + MAC guardada + reintento + ticket multi-línea con consecutivo.
- **Could**: roles/PIN (dueño/bodeguero/vendedor) + auditoría `created_by`; sync multi-dispositivo
  (WorkManager + backend, UUID + timestamp); modo oscuro madrugada; onboarding permisos.

## Deuda conocida (no olvidar)
1. Paquete `com.example` → renombrar a dominio real (`cr.tomateapp` o `applicationId` actual) +
   `rootProject.name "My Application"` → `"TomateApp"`.
2. `debug.keystore` + `.base64` commiteados y `local.properties` local; `.build-outputs/app-debug.apk`
   desactualizado (decía "Dashboard Diario", el fresco dice "CFO de bolsillo").
3. `targetSdk 36` en emulador API 35: funciona, pero probar en API 36 antes del piloto.
4. Deps sin uso (Retrofit/Moshi/OkHttp/Firebase/cámara) y `GEMINI_API_KEY` muerto: quitar o cablear
   asesor IA (precios/merma) con `BuildConfig` real.
5. `DashboardScreen.kt` 1137 líneas: extraer diálogos y `StartDayCard`; gráficas con Vico si crecen.
