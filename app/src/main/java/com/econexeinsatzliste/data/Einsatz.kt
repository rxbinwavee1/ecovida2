package com.econexeinsatzliste.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class EinsatzStatus(val label: String) {
    OFFEN("Offen"),
    ABGESCHLOSSEN("Abgeschlossen"),
    ABGEBROCHEN("Abgebrochen"),
    VERSCHOBEN("Verschoben");

    companion object {
        fun fromName(name: String?): EinsatzStatus {
            if (name == null) return OFFEN
            return try {
                valueOf(name.uppercase())
            } catch (e: Exception) {
                OFFEN
            }
        }
    }
}

/**
 * Data class representing a shift / deployment ("Einsatz") entry.
 */
data class Einsatz(
    val id: Long = 0,
    val datumStr: String = "",
    val datumMillis: Long = 0L,
    val abZeit: String = "",
    val abBahnhof: String = "",
    val anZeit: String = "",
    val anBahnhof: String = "",
    val tourNr: String = "",
    val linie: String = "",
    val betragEuro: Double = 0.0,
    val status: EinsatzStatus = EinsatzStatus.OFFEN,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun getEffectiveMillis(): Long {
        return if (datumMillis > 0L) datumMillis else createdAtMillis
    }

    fun isOlderThan30Days(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
        val effective = getEffectiveMillis()
        return (nowMillis - effective) > thirtyDaysInMillis
    }

    companion object {
        private val germanDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

        fun parseDateStrToMillis(dateStr: String): Long {
            if (dateStr.isBlank()) return System.currentTimeMillis()
            val cleanDate = dateStr.replace(Regex("^[A-Za-z]{2},\\s*"), "").trim()
            return try {
                val parsed = germanDateFormat.parse(cleanDate)
                parsed?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }

        fun formatDateMillisToStr(millis: Long): String {
            if (millis <= 0L) return ""
            val dayFormat = SimpleDateFormat("EEE, dd.MM.yyyy", Locale.GERMANY)
            return dayFormat.format(Date(millis))
        }
    }
}
