package com.example.fe

import com.example.data.Client
import com.example.data.Inventory
import com.example.data.Invoice
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Datos del emisor para el XML. PLACEHOLDERS de demo: antes de operar de
 * verdad hay que poner la cédula/nombre/activity reales, firmar con el
 * certificado .p12 y enviar a ATV. Sin eso el XML NO tiene validez tributaria.
 */
object FeConfig {
    const val NOMBRE = "MI BODEGA DEMO (configurar)"
    const val TIPO_CED = "02"
    const val CEDULA = "310000000000"
    const val EMAIL = "demo@ejemplo.cr"
    const val ACTIVIDAD = "011140"
}

/** Una línea del DetalleServicio para el XML. */
data class FeLine(
    val numero: Int,
    val cabys: String,
    val unidad: String,
    val detalle: String,
    val cantidad: Double,
    val precioUnitario: Double,
    val ivaRate: Double
)

/** Condición de venta Hacienda: 01 contado, 02 crédito. */
fun condicionVenta(isCredit: Boolean): String = if (isCredit) "02" else "01"

/**
 * Constructor de XML de Factura Electrónica v4.4 (PRE-FIRMA, demo).
 * Genera el documento listo para firmar con el .p12 y enviar a ATV;
 * sin firma ni envío NO es un comprobante válido. Ver docs/FE_CR.md.
 */
object FeXml {

    /** Clave demo de 50 caracteres (Hacienda solo la acepta firmada). */
    fun buildClave(
        cedulaEmisor: String,
        consecutive: String,
        dateTime: LocalDateTime = LocalDateTime.now(),
        securityCode: String = (1..8).map { ('0'..'9').random() }.joinToString("")
    ): String {
        val digits = cedulaEmisor.filter { it.isDigit() }.padStart(12, '0').takeLast(12)
        val stamp = dateTime.format(DateTimeFormatter.ofPattern("yyMMddHHmm"))
        val seq = consecutive.filter { it.isDigit() }.takeLast(10).padStart(10, '0')
        return ("506$stamp$digits$seq$securityCode").take(50).padEnd(50, '0')
    }

    fun facturaXml(
        invoice: Invoice,
        client: Client,
        item: Inventory,
        ivaRate: Double,
        fechaEmision: String = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss-06:00")),
        clave: String = buildClave(FeConfig.CEDULA, invoice.consecutive.ifEmpty { "0" })
    ): String {
        val lineTotal = invoice.quantity * invoice.unit_price
        val ivaMonto = lineTotal * ivaRate
        val total = lineTotal + ivaMonto
        val cabys = item.cabys.ifEmpty { "SIN-CABYS" }
        val condicion = condicionVenta(invoice.is_credit)
        val receptor = StringBuilder()
        if (client.id_number.isNotBlank()) {
            receptor.append("<Receptor>")
                .append("<Nombre>").append(esc(client.name)).append("</Nombre>")
                .append("<Identificacion><Tipo>").append(esc(client.id_type.ifEmpty { "01" }))
                .append("</Tipo><Numero>").append(esc(client.id_number))
                .append("</Numero></Identificacion>")
            if (client.email.isNotBlank()) {
                receptor.append("<CorreoElectronico>").append(esc(client.email))
                    .append("</CorreoElectronico>")
            }
            receptor.append("</Receptor>")
        }
        return """
        |<?xml version="1.0" encoding="UTF-8"?>
        |<!-- DEMO SIN VALIDEZ TRIBUTARIA: pre-firma, falta certificado .p12 y envío a ATV -->
        |<FacturaElectronica xmlns="https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/facturaElectronica">
        |<Clave>$clave</Clave>
        |<CodigoActividad>${FeConfig.ACTIVIDAD}</CodigoActividad>
        |<NumeroConsecutivo>${esc(invoice.consecutive)}</NumeroConsecutivo>
        |<FechaEmision>$fechaEmision</FechaEmision>
        |<Emisor><Nombre>${esc(FeConfig.NOMBRE)}</Nombre><Identificacion><Tipo>${FeConfig.TIPO_CED}</Tipo><Numero>${FeConfig.CEDULA}</Numero></Identificacion><CorreoElectronico>${FeConfig.EMAIL}</CorreoElectronico></Emisor>
        |$receptor<CondicionVenta>$condicion</CondicionVenta><PlazoCredito>${if (invoice.is_credit) "30" else "0"}</PlazoCredito><MedioPago>01</MedioPago>
        |<DetalleServicio><LineaDetalle><NumeroLinea>1</NumeroLinea><CodigoCABYS>$cabys</CodigoCABYS><Cantidad>${invoice.quantity}</Cantidad><UnidadMedida>${esc(item.unit.ifEmpty { "Unid" })}</UnidadMedida><Detalle>${esc(item.item_name)}</Detalle><PrecioUnitario>${invoice.unit_price}</PrecioUnitario><MontoTotal>$lineTotal</MontoTotal><Impuesto><Codigo>01</Codigo><CodigoTarifaIVA>08</CodigoTarifaIVA><Tarifa>$ivaRate</Tarifa><Monto>$ivaMonto</Monto></Impuesto><MontoTotalLinea>$total</MontoTotalLinea></LineaDetalle></DetalleServicio>
        |<ResumenFactura><CodigoTipoMoneda><CodigoMoneda>CRC</CodigoMoneda><TipoCambio>1</TipoCambio></CodigoTipoMoneda><TotalVenta>$lineTotal</TotalVenta><TotalDescuentos>0</TotalDescuentos><TotalVentaNeta>$lineTotal</TotalVentaNeta><TotalImpuesto>$ivaMonto</TotalImpuesto><TotalIVADevuelto>0</TotalIVADevuelto><TotalOtrosCargos>0</TotalOtrosCargos><TotalComprobante>$total</TotalComprobante></ResumenFactura>
        |</FacturaElectronica>
        """.trimMargin()
    }

    private fun esc(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
