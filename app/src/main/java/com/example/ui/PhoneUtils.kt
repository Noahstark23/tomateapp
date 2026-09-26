package com.example.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import java.net.URLEncoder

/**
 * Teléfonos de Costa Rica + WhatsApp sin cambiar el schema Room:
 * `Client.contact_info` guarda el teléfono sanitizado (solo dígitos).
 */
object PhoneUtils {

    /** Deja solo dígitos: "+506 8888-1234" -> "50688881234". */
    fun sanitize(raw: String): String = raw.filter { it.isDigit() }

    /** Teléfono válido tico: 8 dígitos locales o 506 + 8 dígitos. */
    fun isValidCrPhone(raw: String): Boolean {
        val d = sanitize(raw)
        return d.length == 8 || (d.length == 11 && d.startsWith("506"))
    }

    /** Número listo para wa.me: antepone 506 al formato local de 8 dígitos. */
    fun toWaNumber(raw: String): String? {
        val d = sanitize(raw)
        return when {
            d.length == 8 -> "506$d"
            d.length == 11 && d.startsWith("506") -> d
            else -> null
        }
    }

    fun waLink(rawPhone: String, message: String): String? {
        val number = toWaNumber(rawPhone) ?: return null
        val encoded = URLEncoder.encode(message, Charsets.UTF_8.name())
        return "https://wa.me/$number?text=$encoded"
    }

    /** Abre el chat de WhatsApp o avisa si no hay app que lo maneje. */
    fun openWhatsApp(context: Context, rawPhone: String, message: String): Boolean {
        val link = waLink(rawPhone, message) ?: return false
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri()))
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "WhatsApp no instalado", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
