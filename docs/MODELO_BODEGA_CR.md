# MODELO_BODEGA_CR — dominio tomate / abasto Costa Rica

Síntesis de deep research 2025–2026 (PIMA/CENADA, SIMM, IICA/CENADA DIGITAL, Ley 8533, Hacienda v4.4).
Reglas listas para codificar cuando se agregue el modelo bodega/lote (ver `BACKLOG.md`).

## 1. Flujo
Productor (Cartago/Zarcero/Zona Norte) → transporte nocturno → CENADA Barreal Heredia
(plaza Dom–Vie desde ~8pm) → bodega/tramo compra por cajas → reclasifica primera/segunda/tercera →
vende por kg/caja a feria, verdulería, soda, restaurante, supermercado.

## 2. Producto y unidades
- Variedades: híbridos larga vida (Candela, Naty, Syta, Charleston, Tamaris, Airton, Pietro, Zodiac),
  Roma industrial Paipai; novedad 2026 UCR "blindado" tolerante a marchitez/TYLCV.
- Calidad CENADA: **primera / segunda / tercera** (calibre, firmeza, color, sin grietas).
- Unidad mayorista: **caja plástica 18 kg** (a veces 17 kg). Detalle: kg, ½ caja, unidad.
  Guardar siempre kg como fuente de verdad; cajas como presentación.
- Madurez: verde / pintón (ideal transporte) / rojo firme.

## 3. Precios de referencia (no asumir, importar diario)
- SIMM CENADA 19/08/2025, caja 18 kg: primera ₡12 625 (moda 13 000), segunda ₡11 600 (moda 12 000),
  tercera ₡10 166 (moda 10 000) → **₡701 / 644 / 565 por kg**.
- Detalle feria 2026: **₡900–1 600/kg**, muy sensible a lluvia. Tipo de cambio ₡505–515/USD.
- Regla MVP: `precio_mínimo = costoKg × 1.10 (merma esperada) × 1.15 (margen)`; alertar si venta < costoKg.
  Precio ref por defecto = moda SIMM última plaza. Redondeo feria a ₡50, sin decimales.

## 4. Merma y conservación
- Normal sin frío: **5–12%**; alerta **>12%**, crítica sostenida **>15%** (bloquear proveedor tras 3 lotes).
- Causas: pudrición 3–6%, aplastado 2–4% (no estibar >2–3 capas, bins off suelo), deshidratación 1–3%.
- Temperaturas: verde-maduro 12.5–15 °C, maduración 18–21 °C, rojo firme 10–12.5 °C 7–14 días,
  HR 85–95%. Nunca <10 °C prolongado (daño por frío). Ambiente <25 °C: máximo ~1 semana.
- MVP sin IoT: registro manual T°/HR 2×/día + foto termómetro; alerta si >25 °C.

## 5. FEFO y vida útil (codificar post-hoy)
- **FEFO obligatorio**: sugerir lote con `fechaLimiteVenta` más próxima y stock>0; bloquear venta
  de vencido → forzar a merma. Por lote: `fechaIngreso, madurez, fechaLimiteVenta (=ingreso+3d
  ambiente, +7d frío), fechaAlerta (=límite−1d)`.
- Trazabilidad mínima vendible: proveedor + finca + variedad + fecha ingreso (compatible CENADA DIGITAL,
  subasta electrónica lanzada 15/04/2026 con ficha calidad + SINPE BCCR).

## 6. Fiscal y permisos (no prometer facturación hoy)
- Hacienda **FE v4.4 obligatoria desde 01/06/2025**: comprobantes (FE, tiquete, NC/ND, factura compra/
  exportación, recibo pago nuevo), certificado `.p12`, clave 50 dígitos, consecutivo por sucursal,
  **CABYS por línea**, IVA por línea, envío ATV. Hoy: guardar CABYS + cédula + condición + IVA y
  **exportar CSV para el facturador** (verificar con contador si hortaliza lleva 1% o 13%).
- Permisos tramo: patente municipal + permiso sanitario + carné manipulación; ferias Ley 8533
  (80+ cantones, precio rotulado, balanza calibrada, solo productores acreditados).
  SENASA-CVO solo si maneja queso/carne/huevo.

## 7. Glosario (usar en UI/código)
CENADA, PIMA/SIMM, plaza, tramo/bodega de abasto, caja 18 kg, primera/segunda/tercera, pintón,
merma, FEFO, CABYS, clave, consecutivo, ATV, SINPE Móvil, feria/Centro Agrícola/Junta Nacional Ley 8533.
