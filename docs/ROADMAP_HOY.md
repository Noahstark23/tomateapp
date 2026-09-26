# ROADMAP_HOY — terminar la app hoy (<8h)

## Objetivo + Definición de Done
Dejar TomateApp instalable, entendible y operable hoy en 1 bodega: APK debug fresco instalado en
emulator-5580, `FinancialEngineTest` verde, 10 docs en `docs/` + `README` + `CHANGELOG`,
guía de operador validada y backlog priorizado. Lo que no quepa va a `BACKLOG.md` sin culpa.

## Bloques (timebox)
| # | Bloque | Min | Estado |
|---|--------|-----|--------|
| 1 | Toolchain Mac (Java 17, SDK 36/36.1, emulador, wrapper 9.7.1) | 90 | ✅ hecho |
| 2 | Compilar e instalar APK fresco + screenshot | 30 | ✅ hecho (`BUILD SUCCESSFUL`, 11MB, "CFO de bolsillo") |
| 3 | Agentes: código + dominio CR + auditoría docs | 30 | ✅ hecho |
| 4 | Escribir 10 docs + README + CHANGELOG | 90 | ✅ en curso (este lote) |
| 5 | QA manual bodega (sección 8 de TESTING_QA) + fix asserts `Example*Test` | 60 | ⬜ pendiente |
| 6 | Endurecer P0 (TypeConverters, sacar `initTestData` de release, `.gitignore` keystore/APK) | 60 | ⬜ pendiente |
| 7 | Release interno `v0.1.0-hoy` (tag + APK + notas) + commit docs | 30 | ⬜ pendiente |
| Buffer | | 60 | |

## P0 (cierra hoy sí o sí)
- [x] `./gradlew assembleDebug` verde + instalado en emulator-5580
- [x] `docs/{PRD,ARCHITECTURE,DATA_MODEL,FINANCIAL_ENGINE,MODELO_BODEGA_CR,BUILD_RUN,TESTING_QA,USER_GUIDE_OPERADOR,ROADMAP_HOY,BACKLOG}.md` + `README` + `CHANGELOG`
- [x] QA checklist `TESTING_QA` pasado con caso ₡150k/venta(2×₡6000)/gasto/merma en emulator-5580 (verificado 2026-09-26: ventas ₡12 000, ganancia ₡2 000, caja ₡162 000, extraíble ₡127 700)
- [x] `ExampleRobolectricTest` + `ExampleInstrumentedTest` en verde (actualizado `"Nortex Dashboard"` y `applicationId`; Robolectric fijado a `sdk=[34]` porque SDK 36 exige Java 21 y el proyecto usa Java 17) — 15/15 tests verdes
- [x] `git rm --cached` de `.build-outputs/app-debug.apk` y `debug.keystore.base64` + `.gitignore` ampliado (`*.jks`, `*.p12`, `.build-outputs/`, `/app/build/`)

## P1 (si sobran 2h)
- [ ] `@TypeConverters` para `ExpenseCategory` + `exportSchema=true`
- [ ] `initTestData()` solo `BuildConfig.DEBUG`
- [ ] Selector de impresora + guardar MAC (DataStore) + ticket multi-línea
- [ ] Exportar CSV ventas/compras con CABYS para el contador (FE v4.4)

## Recortes explícitos si faltan 2h
Cae P1 completo y el bloque 6 se reduce a solo `.gitignore` + tag. El MVP contable de hoy
(ledger + CFO + impresión básica) ya opera sin esos cambios.

## Checklist de cierre
APK debug instala y abre en "CFO de bolsillo" · tests unitarios verdes · operador entiende la guía ·
`git status` limpio de secretos · tag `v0.1.0-hoy` creado.
