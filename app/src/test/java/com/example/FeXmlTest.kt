package com.example

import com.example.data.Client
import com.example.data.Inventory
import com.example.data.Invoice
import com.example.fe.FeXml
import com.example.fe.condicionVenta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class FeXmlTest {

    private val invoice = Invoice(
        id = 7,
        ledger_date = "2026-09-26",
        client_id = 3,
        inventory_id = 1,
        quantity = 2,
        unit_price = 6000.0,
        unit_cost = 5000.0,
        total_amount = 12000.0,
        total_cost = 10000.0,
        profit_margin = 16.7,
        consecutive = "00100001010000000007"
    )
    private val client = Client(
        id = 3, name = "Soda La Esquina", contact_info = "88887777",
        phone = "88887777", id_type = "01", id_number = "102340567", email = "soda@ejemplo.cr"
    )
    private val item = Inventory(
        id = 1, item_name = "Caja de Tomate Primera",
        purchase_price = 5000.0, sale_price = 6000.0,
        initial_stock = 100, current_stock = 90,
        cabys = "0111400100101", unit = "Unid"
    )

    @Test
    fun `clave demo mide 50 caracteres`() {
        val clave = FeXml.buildClave(
            cedulaEmisor = "310000000000",
            consecutive = invoice.consecutive,
            dateTime = LocalDateTime.of(2026, 9, 26, 10, 0),
            securityCode = "12345678"
        )
        assertEquals(50, clave.length)
        assertTrue(clave.startsWith("506"))
    }

    @Test
    fun `condicion venta contado y credito`() {
        assertEquals("01", condicionVenta(false))
        assertEquals("02", condicionVenta(true))
    }

    @Test
    fun `xml trae clave consecutivo receptor cabys y totales`() {
        val xml = FeXml.facturaXml(
            invoice, client, item, ivaRate = 0.01,
            fechaEmision = "2026-09-26T10:00:00-06:00",
            clave = "CLAVE-DEMO-50-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
        )
        assertTrue(xml.contains("<NumeroConsecutivo>00100001010000000007</NumeroConsecutivo>"))
        assertTrue(xml.contains("<CodigoCABYS>0111400100101</CodigoCABYS>"))
        assertTrue(xml.contains("<Numero>102340567</Numero>"))
        assertTrue(xml.contains("<Cantidad>2</Cantidad>"))
        // Línea 12000 + IVA 1% (120) = 12120
        assertTrue(xml.contains("<MontoTotal>12000.0</MontoTotal>"))
        assertTrue(xml.contains("<Monto>120.0</Monto>"))
        assertTrue(xml.contains("<TotalComprobante>12120.0</TotalComprobante>"))
        assertTrue(xml.contains("DEMO SIN VALIDEZ TRIBUTARIA"))
    }

    @Test
    fun `sin receptor no emite bloque receptor`() {
        val xml = FeXml.facturaXml(
            invoice, client.copy(id_number = ""), item, ivaRate = 0.01,
            fechaEmision = "2026-09-26T10:00:00-06:00",
            clave = "CLAVE-DEMO-50-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
        )
        assertTrue(!xml.contains("<Receptor>"))
    }
}
