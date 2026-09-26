# Lotes FEFO + FE v4.4 base (v0.4.0)

## Lotes (DB v5, migración 4→5 sin pérdida, verificada con datos reales)
- Tabla `lots` (proveedor, variedad, calibre, calidad, qty inicial/actual, costo total,
  fecha ingreso/límite `YYYY-MM-DD`) + `waste.lot_id` (trazabilidad de merma).
- `registerLotEntry` crea lote + suma stock en una transacción; `consumeFefo`
  descuenta por vencimiento más próximo en venta Y merma (stock legacy sin lotes
  sigue vendiéndose del global).
- UI: botón 📦 por producto en Reportes → lista con badge ⏩ SALE PRIMERO, alerta
  roja si vence en ≤1 día + formulario de entrada (vida útil en días → límite auto).
- E2E: lote 10 uds vence 30/09 → venta 3 → 7/10 → venta 1 → 6 → merma 1 → 5/10.

## FE v4.4 Costa Rica (pre-firma, honesto)
- Receptor por cliente (`id_type` 01/02/03/04, `id_number`, `email`, diálogo 🧾),
  `CABYS` + unidad por producto (diálogo de producto), consecutivo automático
  sucursal(001)+terminal(00001)+tipo(01)+seq(10) al vender, `fe_status`
  (BORRADOR → PENDIENTE al exportar).
- `fe/FeXml.kt` puro + 4 tests: XML v4.4 con Clave demo 50, emisor/receptor,
  `CodigoCABYS`, IVA (tarifa elegible 1%/13%/exento en UI) y totales. Banner
  `DEMO SIN VALIDEZ TRIBUTARIA` en archivo y pantalla.
- UI: sección "Facturas electrónicas (pre-firma)" en Reportes, botón 🧾 XML
  habilitado solo con cédula + CABYS (si no, dice qué falta), share del XML.
- E2E: factura SodaLaEsquina ...000004 → `FE-00100001010000000004.xml` → PENDIENTE.
- **Falta para validez real** (con contador + Hacienda): configurar `FeConfig`
  (nombre/cédula/actividad del emisor), tarifa IVA del tomate (1% vs 13%),
  firma XAdES con .p12 y envío a ATV con manejo ACEPTADO/RECHAZADO.
