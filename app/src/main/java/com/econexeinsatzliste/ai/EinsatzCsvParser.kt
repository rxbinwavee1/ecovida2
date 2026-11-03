package com.econexeinsatzliste.ai

import android.content.Context
import android.net.Uri
import com.econexeinsatzliste.data.Einsatz
import com.econexeinsatzliste.data.EinsatzStatus

object EinsatzCsvParser {

    /**
     * Reads a CSV file from Uri and parses it into a List of Einsatz objects.
     */
    fun parseCsvFromUri(context: Context, uri: Uri): List<Einsatz> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
            parseCsvText(content)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Parses raw CSV text into a List of Einsatz objects.
     * Line 1 is treated as the Header row defining column placement.
     * Processing starts from Line 2 onwards.
     */
    fun parseCsvText(csvContent: String): List<Einsatz> {
        if (csvContent.isBlank()) return emptyList()

        val lines = csvContent.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        // Detect delimiter (; , or tab)
        val sampleLine = lines.first()
        val delimiter = when {
            sampleLine.contains(";") -> ";"
            sampleLine.contains("\t") -> "\t"
            else -> ","
        }

        val headerTokens = lines.first().split(delimiter).map { it.trim().lowercase().removeSurrounding("\"") }

        // Find column indices from header row (Line 1)
        val idxDatum = findColumnIndex(headerTokens, "datum")
        val idxAbZeit = findColumnIndex(headerTokens, "ab-zeit", "abzeit", "ab_zeit")
        val idxAbBhf = findColumnIndex(headerTokens, "ab-bhf", "abbhf", "ab_bhf", "ab-bahnhof", "abfahrt")
        val idxAnZeit = findColumnIndex(headerTokens, "an-zeit", "anzeit", "an_zeit")
        val idxAnBhf = findColumnIndex(headerTokens, "an-bhf", "anbhf", "an_bhf", "an-bahnhof", "ankunft")
        val idxTour = findColumnIndex(headerTokens, "tour-nr", "tour_nr", "tournr", "tour")
        val idxLinie = findColumnIndex(headerTokens, "linie", "line")
        val idxEuro = findColumnIndex(headerTokens, "euro", "honorar", "betrag", "preis", "* €")
        val idxStatus = findColumnIndex(headerTokens, "status")

        val result = mutableListOf<Einsatz>()

        // Process data starting from Row 2 (index 1)
        for (i in 1 until lines.size) {
            val line = lines[i]
            val tokens = line.split(delimiter).map { it.trim().removeSurrounding("\"") }

            var datumStr = getFieldValue(tokens, idxDatum)
            var abZeit = getFieldValue(tokens, idxAbZeit)
            var abBahnhof = getFieldValue(tokens, idxAbBhf)
            var anZeit = getFieldValue(tokens, idxAnZeit)
            var anBahnhof = getFieldValue(tokens, idxAnBhf)
            var tourNr = getFieldValue(tokens, idxTour)
            val linie = getFieldValue(tokens, idxLinie)
            val euroStr = getFieldValue(tokens, idxEuro)
            val statusStr = getFieldValue(tokens, idxStatus)

            // Positional fallback if header indices were not matched
            if (idxDatum == -1) datumStr = tokens.getOrNull(0) ?: ""
            if (idxAbZeit == -1 && idxAbBhf == -1) {
                abZeit = tokens.getOrNull(2) ?: ""
                abBahnhof = tokens.getOrNull(3) ?: ""
            }
            if (idxAnZeit == -1 && idxAnBhf == -1) {
                anZeit = tokens.getOrNull(4) ?: ""
                anBahnhof = tokens.getOrNull(5) ?: ""
            }
            if (idxTour == -1) tourNr = tokens.getOrNull(6) ?: ""

            // Extract euro amount
            val betragEuro = parseEuroValue(euroStr)

            // If datum is empty, check if first token has date
            if (datumStr.isBlank() && tokens.isNotEmpty()) {
                datumStr = tokens.first()
            }

            val datumMillis = Einsatz.parseDateStrToMillis(datumStr)
            val status = parseStatus(statusStr)

            if (datumStr.isNotBlank() || abBahnhof.isNotBlank()) {
                result.add(
                    Einsatz(
                        datumStr = datumStr,
                        datumMillis = datumMillis,
                        abZeit = abZeit,
                        abBahnhof = abBahnhof,
                        anZeit = anZeit,
                        anBahnhof = anBahnhof,
                        tourNr = tourNr,
                        linie = linie,
                        betragEuro = betragEuro,
                        status = status
                    )
                )
            }
        }

        return result
    }

    private fun findColumnIndex(headers: List<String>, vararg keywords: String): Int {
        for (keyword in keywords) {
            val idx = headers.indexOfFirst { it == keyword || it.contains(keyword) }
            if (idx != -1) return idx
        }
        return -1
    }

    private fun getFieldValue(tokens: List<String>, index: Int): String {
        if (index < 0 || index >= tokens.size) return ""
        return tokens[index].trim()
    }

    private fun parseEuroValue(value: String): Double {
        if (value.isBlank()) return 0.0
        val clean = value.replace(Regex("""[^\d,.+]"""), "").replace(",", ".")
        return clean.toDoubleOrNull() ?: 0.0
    }

    private fun parseStatus(value: String): EinsatzStatus {
        val lower = value.lowercase()
        return when {
            lower.contains("abgeschlossen") || lower.contains("erledigt") -> EinsatzStatus.ABGESCHLOSSEN
            lower.contains("abgebrochen") || lower.contains("storniert") -> EinsatzStatus.ABGEBROCHEN
            lower.contains("verschoben") -> EinsatzStatus.VERSCHOBEN
            else -> EinsatzStatus.OFFEN
        }
    }
}
