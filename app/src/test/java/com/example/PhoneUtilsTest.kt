package com.example

import com.example.ui.PhoneUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneUtilsTest {

    @Test
    fun `sanitiza formatos ticos comunes`() {
        assertEquals("88881234", PhoneUtils.sanitize("8888-1234"))
        assertEquals("50688881234", PhoneUtils.sanitize("+506 8888 1234"))
        assertEquals("88881234", PhoneUtils.sanitize("(8888) 1234"))
    }

    @Test
    fun `valida 8 digitos y 506 mas 8`() {
        assertTrue(PhoneUtils.isValidCrPhone("88881234"))
        assertTrue(PhoneUtils.isValidCrPhone("+506 8888-1234"))
        assertFalse(PhoneUtils.isValidCrPhone("123456"))
        assertFalse(PhoneUtils.isValidCrPhone(""))
    }

    @Test
    fun `wa number antepone 506 al local`() {
        assertEquals("50688881234", PhoneUtils.toWaNumber("8888-1234"))
        assertEquals("50688881234", PhoneUtils.toWaNumber("+50688881234"))
        assertNull(PhoneUtils.toWaNumber("123456"))
    }

    @Test
    fun `wa link codifica el mensaje`() {
        val link = PhoneUtils.waLink("88881234", "Hola ₡")
        assertTrue(link!!.startsWith("https://wa.me/50688881234?text="))
        assertTrue(link.contains("Hola"))
    }
}
