# Caja, Clientes y Créditos (v0.2.0 → v0.3.0)

## v0.3.0: Créditos implementados (diseño Fase 2 hecho realidad)
- Migración 3→4 manual sin pérdida + `exportSchema=true` (adiós destructive).
- Venta fiada con cupo, tab Créditos con abonos/antigüedad/WhatsApp, Caja con
  neto = contado + abonos − gastos y proyección sobre entradas de efectivo.
- Detalle contable y QA E2E en `CHANGELOG.md` (v0.3.0).

## Lo que trae v0.2.0 (verificado en emulator-5580)
- **Tab 💰 Caja** (`ui/CashFlowScreen.kt`, ruta `cash`): saldo inicial vs actual y neto
  (`ventas − gastos`, leído del ledger, nunca recalculado en UI), entradas por producto
  (`AppDao.getSalesByProduct`), salidas por categoría, n° ventas + ticket promedio,
  tarjeta educativa neto-caja vs ganancia real, y **predicción simple 7 días**
  (`FinancialEngine.projectCash7Days` + `findBreakEvenDay`) con alerta de quiebre y disclaimer.
  Promedio de ventas con divisor 7 calendario + contador "X de 7 días con registro".
- **Alta de clientes** (`AddClientDialog`): nombre + teléfono tico validado
  (`PhoneUtils`: 8 dígitos o 506+8, sanitizado a dígitos en `Client.contact_info`, sin schema).
  Botón "+ Nuevo cliente" dentro de `SaleDialog` (queda seleccionado) y "+ Agregar cliente"
  en `ClientsDialog` (botón "👤 Clientes y Proveedores" en acciones rápidas).
- **WhatsApp**: icono por cliente en venta y en lista + sección "Mensaje a proveedor" con
  plantilla de cotización editable (`PhoneUtils.waLink/openWhatsApp`, intent `wa.me`, sin permisos).
- Tests: `projectCash7Days`/`findBreakEvenDay` (3) + `PhoneUtilsTest` (4). Total 22/22 verdes.

## Créditos Fase 2 (diseño aprobado, no implementado)
Requiere migración v3→v4 con `Migration(3,4)` manual (con `fallbackToDestructiveMigration`
subir versión **borra** la BD): `clients` += `phone, type(TRAMO/FERIA/PROVEEDOR), credit_limit,
credit_balance`; `invoices` += `is_credit, paid_amount`; tabla nueva `payments`
(abonos, `invoice_id` nullable, reparto FIFO). Fórmulas nuevas:
`cash_on_hand = inversión + ventas_contado + abonos − gastos`;
`real_net_profit` sin cambio (devengo: el crédito reconoce ingreso+COGS al vender).
Bug #1 a evitar: sumar `total_amount` sin filtrar `is_credit` infla caja/withdrawal/runway.
Venta bloqueada si `balance + total > limit`; abono suma caja en el `ledger_date` del pago.
Pantalla Créditos/Cobros con antigüedad 0-7/8-15/16-30/+30 y WhatsApp de cobro con saldo.
`safeWithdrawal`/`runway` deben basarse en caja real, no en `total_sales`.
