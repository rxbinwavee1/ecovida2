package com.econexeinsatzliste.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.econexeinsatzliste.data.Einsatz
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object EinsatzScreenshotParser {

    /**
     * Parses a screenshot URI using ML Kit Text Recognition to extract:
     * Datum, Abfahrt-Bhf, Ankunft, Ankunfts-Bhf, Tour-Nr, Honorar (€).
     */
    suspend fun parseScreenshot(context: Context, imageUri: Uri): List<Einsatz> {
        val inputImage = try {
            InputImage.fromFilePath(context, imageUri)
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
        return processImage(inputImage)
    }

    suspend fun parseBitmap(bitmap: Bitmap): List<Einsatz> {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        return processImage(inputImage)
    }

    private suspend fun processImage(inputImage: InputImage): List<Einsatz> =
        suspendCancellableCoroutine { continuation ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val result = parseVisionText(visionText)
                    continuation.resume(result)
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    continuation.resume(emptyList())
                }
        }

    fun parseVisionText(visionText: Text): List<Einsatz> {
        val allElements = mutableListOf<TextElementInfo>()

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox
                val text = line.text.trim()
                if (text.isNotBlank() && !isHeaderRow(text)) {
                    val centerY = box?.centerY() ?: 0
                    val left = box?.left ?: 0
                    allElements.add(TextElementInfo(text, centerY, left))
                }
            }
        }

        if (allElements.isEmpty()) {
            return parseRawTextLines(visionText.text)
        }

        // Group lines by Y-coordinate (within threshold of ~25px) to reconstruct table rows
        val sortedElements = allElements.sortedBy { it.centerY }
        val rows = mutableListOf<MutableList<TextElementInfo>>()

        val yThreshold = 25
        for (elem in sortedElements) {
            val matchingRow = rows.find { row ->
                val avgY = row.map { it.centerY }.average()
                Math.abs(avgY - elem.centerY) <= yThreshold
            }
            if (matchingRow != null) {
                matchingRow.add(elem)
            } else {
                rows.add(mutableListOf(elem))
            }
        }

        val extractedList = mutableListOf<Einsatz>()

        for (row in rows) {
            row.sortBy { it.left }
            val rowText = row.joinToString(" ") { it.text }

            val einsatz = parseRowText(rowText)
            if (einsatz != null) {
                extractedList.add(einsatz)
            }
        }

        if (extractedList.isEmpty()) {
            return parseRawTextLines(visionText.text)
        }

        return extractedList
    }

    private fun isHeaderRow(text: String): Boolean {
        val lower = text.lowercase()
        return (lower.contains("datum") && lower.contains("ab")) ||
                (lower.contains("ab-bhf") && lower.contains("an-bhf")) ||
                (lower.contains("tour-nr") || lower.contains("honorar"))
    }

    private fun parseRawTextLines(fullText: String): List<Einsatz> {
        val list = mutableListOf<Einsatz>()
        val lines = fullText.split("\n")
        var currentBuffer = StringBuilder()

        val dateRegex = Regex("""(Mo|Di|Mi|Do|Fr|Sa|So)?,?\s*\d{2}\.\d{2}\.\d{4}""")

        for (line in lines) {
            val trimmed = line.trim()
            if (isHeaderRow(trimmed)) continue

            if (dateRegex.containsMatchIn(trimmed)) {
                if (currentBuffer.isNotEmpty()) {
                    val parsed = parseRowText(currentBuffer.toString())
                    if (parsed != null) list.add(parsed)
                    currentBuffer = StringBuilder()
                }
                currentBuffer.append(trimmed)
            } else {
                if (currentBuffer.isNotEmpty()) {
                    currentBuffer.append(" ").append(trimmed)
                }
            }
        }

        if (currentBuffer.isNotEmpty()) {
            val parsed = parseRowText(currentBuffer.toString())
            if (parsed != null) list.add(parsed)
        }

        return list
    }

    /**
     * Extracts an Einsatz focusing strictly on:
     * 1. Datum
     * 2. Abfahrt-Zeit & Abfahrt-Bahnhof
     * 3. Ankunft-Zeit & Ankunfts-Bahnhof
     * 4. Tour-Nummer
     * 5. Honorar (€)
     */
    fun parseRowText(rowText: String): Einsatz? {
        val dateMatch = Regex("""((?:Mo|Di|Mi|Do|Fr|Sa|So)?,?\s*\d{2}\.\d{2}\.\d{4})""").find(rowText)
            ?: Regex("""(\d{2}\.\d{2}\.\d{4})""").find(rowText)
            ?: return null

        val datumStr = dateMatch.value.trim()
        val datumMillis = Einsatz.parseDateStrToMillis(datumStr)

        val remainingAfterDate = rowText.substring(dateMatch.range.last + 1).trim()

        // 1. Extract Times (HH:mm)
        val timeMatches = Regex("""\b([01]?\d|2[0-3]):[0-5]\d\b""").findAll(remainingAfterDate).toList()
        val abZeit = timeMatches.getOrNull(0)?.value ?: ""
        val anZeit = timeMatches.getOrNull(1)?.value ?: ""

        // 2. Extract Honorar / Betrag (€) like 20,54 or 15,00
        val euroMatch = Regex("""\b(\d+[.,]\d{2})\b""").find(remainingAfterDate)
        val betragEuro = euroMatch?.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0

        // 3. Extract Tour-Nr (4-digit number)
        val allFourDigitNumbers = Regex("""\b(\d{4})\b""").findAll(remainingAfterDate).map { it.value }.toList()
        val tourNr = allFourDigitNumbers.firstOrNull() ?: ""

        // 4. Extract Stations (Abfahrts-Bhf & Ankunfts-Bhf)
        var abBahnhof = ""
        var anBahnhof = ""

        if (timeMatches.size >= 2) {
            val idxAbTime = remainingAfterDate.indexOf(abZeit)
            val idxAnTime = remainingAfterDate.indexOf(anZeit)

            if (idxAbTime != -1 && idxAnTime != -1 && idxAnTime > idxAbTime) {
                val betwTimes = remainingAfterDate.substring(idxAbTime + abZeit.length, idxAnTime).trim()
                abBahnhof = cleanStationName(betwTimes)

                val afterAnTime = remainingAfterDate.substring(idxAnTime + anZeit.length).trim()
                var endStationIdx = afterAnTime.length
                if (tourNr.isNotEmpty() && afterAnTime.indexOf(tourNr) != -1) {
                    endStationIdx = minOf(endStationIdx, afterAnTime.indexOf(tourNr))
                }
                if (euroMatch != null && afterAnTime.indexOf(euroMatch.value) != -1) {
                    endStationIdx = minOf(endStationIdx, afterAnTime.indexOf(euroMatch.value))
                }
                anBahnhof = cleanStationName(afterAnTime.substring(0, endStationIdx).trim())
            }
        }

        return Einsatz(
            datumStr = datumStr,
            datumMillis = datumMillis,
            abZeit = abZeit,
            abBahnhof = abBahnhof,
            anZeit = anZeit,
            anBahnhof = anBahnhof,
            tourNr = tourNr,
            betragEuro = betragEuro
        )
    }

    private fun cleanStationName(text: String): String {
        return text.replace(Regex("""^\d+\s*"""), "")
            .replace(Regex("""\s*\d{4,}.*$"""), "")
            .replace(Regex("""\s*[€*].*$"""), "")
            .trim()
    }

    private data class TextElementInfo(
        val text: String,
        val centerY: Int,
        val left: Int
    )
}
