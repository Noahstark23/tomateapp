# PRD — TomateApp Bodega CR

## 1. Problema
El dueño de un tramo/bodega de tomate compra cajas de 18 kg en CENADA de madrugada,
vende por kg/caja en feria o punto fijo y al final del día no sabe si ganó o se descapitalizó:
la merma (pudrición/aplastado/deshidratación, 5–12% normal, >12% alerta) se come el margen,
el efectivo se mezcla con gastos (transporte, salario, empaque) y el precio (volátil ₡900–1600/kg
detalle vs ₡565–701/kg mayorista CENADA) se fija a ojo.

## 2. Usuarios
- **Operador/vendedor** (primario, no-técnico): registra entradas, ventas, gastos y mermas en <60 seg,
  con botones grandes, español tico, dual caja/kg, 3 toques por venta, modo madrugada.
- **Dueño/CFO** (secundario): abre el día con capital de trabajo, lee el semáforo de salud,
  decide cuánto retirar y revisa el cierre (ventas ₡/kg, merma %, top clientes, caja vs SINPE pendiente).

## 3. Alcance MVP (lo que ya hace hoy)
1. Abrir día con Capital de Trabajo CRC (`StartDayCard` → `setInitialInvestment`).
2. Vender con validación de stock + costo congelado + ticket Bluetooth (`SaleDialog` + `PrintService`).
3. Registrar gasto por categoría + merma con motivo (`ExpenseDialog`, `WasteDialog`).
4. Dashboard: semáforo runway, capital extraíble, KPIs, gráfica 7 días, capital de trabajo ajustable.
5. Reportes: inventario valorizado + P&L 30 días. 100% offline (Room).

## 4. No-MVP (hoy no)
Facturación electrónica directa a Hacienda v4.4, multi-bodega/lote con FEFO, sincronización
multi-dispositivo, roles/PIN, IoT temperatura, subasta CENADA DIGITAL, predicción de precios con IA.

## 5. Historias core (criterios de aceptación)
- **Abrir día**: dado un monto ≥0, el ledger `YYYY-MM-DD` queda con `cash_on_hand = monto`.
- **Vender**: dado stock suficiente, crea `Invoice` con `unit_cost` congelado y recalcula el ledger;
  si `qty > stock`, bloquea con "Cantidad excede stock".
- **Gastar**: todo gasto baja `cash_on_hand` y `real_net_profit` el mismo día.
- **Mermar**: descuenta stock y `real_net_profit`, no toca `cash_on_hand`; exige motivo.
- **Retiro seguro**: si `amount ≤ 0` muestra "No puedes retirar, estás descapitalizando".
- **Cierre**: el dueño ve ventas, COGS, gastos, merma, ganancia real y caja final sin salir de Reportes.

## 6. Métricas de éxito (piloto 1 bodega, 1 semana)
- Cierre diario <2 min, 0 descuadres caja vs conteo físico.
- 100% de ventas con costo congelado, 0 ventas bajo costo sin alerta.
- Merma medida todos los días; fuga 7d visible; meta <10%.
- App usable sin internet en CENADA/feria; 0 pérdida de datos al migrar (pendiente: quitar destructive migration).

## 7. Supuestos CR
Colones sin decimales; caja plástica 18 kg como unidad mayorista; calidades primera/segunda/tercera;
pago efectivo + SINPE Móvil; impresora térmica ESC/POS Bluetooth; permisos: patente municipal,
permiso sanitario y carné de manipulación (más CVO solo si maneja queso/carne/huevo).
Detalle fiscal/precios/merma: `docs/MODELO_BODEGA_CR.md`.
