package com.econexeinsatzliste

import com.econexeinsatzliste.ai.EinsatzCsvParser
import com.econexeinsatzliste.ai.EinsatzScreenshotParser
import com.econexeinsatzliste.data.Einsatz
import com.econexeinsatzliste.data.EinsatzStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EinsatzParserUnitTest {

    @Test
    fun testParseRow1_DinslakenToWesel() {
        val rowText = "Di, 10.11.2026 1 21:13 Dinslaken, Bahnhof 22:00 Wesel Bf 7504 2003017 2504 2 20,54"
        val einsatz = EinsatzScreenshotParser.parseRowText(rowText)

        assertNotNull(einsatz)
        einsatz?.let {
            assertEquals("Di, 10.11.2026", it.datumStr)
            assertEquals("21:13", it.abZeit)
            assertEquals("Dinslaken, Bahnhof", it.abBahnhof)
            assertEquals("22:00", it.anZeit)
            assertEquals("Wesel Bf", it.anBahnhof)
            assertEquals("7504", it.tourNr)
            assertEquals(20.54, it.betragEuro, 0.01)
        }
    }

    @Test
    fun testRescheduleDateParsingAndSorting() {
        val dateWithDotStr = "Mi., 04.11.2026"
        val dateWithoutDotStr = "Di, 10.11.2026"

        val millis1 = Einsatz.parseDateStrToMillis(dateWithDotStr)
        val millis2 = Einsatz.parseDateStrToMillis(dateWithoutDotStr)

        val e1 = Einsatz(datumStr = dateWithoutDotStr, datumMillis = millis2, abZeit = "21:13")
        val eRescheduled = e1.copy(datumStr = dateWithDotStr, datumMillis = millis1, status = EinsatzStatus.VERSCHOBEN)

        val eOther = Einsatz(datumStr = "Mi, 11.11.2026", datumMillis = Einsatz.parseDateStrToMillis("11.11.2026"), abZeit = "08:29")

        val list = listOf(eOther, eRescheduled)
        val sorted = list.sortedWith(compareBy({ it.getEffectiveMillis() }, { it.abZeit }))

        // 04.11.2026 must come before 11.11.2026!
        assertEquals("Mi., 04.11.2026", sorted[0].datumStr)
        assertEquals("Mi, 11.11.2026", sorted[1].datumStr)
    }

    @Test
    fun test30DayAutoDeletionLogic() {
        val now = System.currentTimeMillis()
        val thirtyOneDaysAgo = now - (31L * 24 * 60 * 60 * 1000)
        val tenDaysAgo = now - (10L * 24 * 60 * 60 * 1000)

        val oldEinsatz = Einsatz(datumMillis = thirtyOneDaysAgo)
        val recentEinsatz = Einsatz(datumMillis = tenDaysAgo)

        assertTrue(oldEinsatz.isOlderThan30Days(now))
        assertFalse(recentEinsatz.isOlderThan30Days(now))
    }

    @Test
    fun testChronologicalSorting_EarliestFirst() {
        val e1 = Einsatz(datumStr = "Fr, 13.11.2026", datumMillis = Einsatz.parseDateStrToMillis("13.11.2026"), abZeit = "14:04")
        val e2 = Einsatz(datumStr = "Di, 10.11.2026", datumMillis = Einsatz.parseDateStrToMillis("10.11.2026"), abZeit = "21:13")
        val e3 = Einsatz(datumStr = "Mi, 11.11.2026", datumMillis = Einsatz.parseDateStrToMillis("11.11.2026"), abZeit = "08:29")

        val list = listOf(e1, e2, e3)
        val sorted = list.sortedWith(compareBy({ it.getEffectiveMillis() }, { it.abZeit }))

        assertEquals("Di, 10.11.2026", sorted[0].datumStr)
        assertEquals("Mi, 11.11.2026", sorted[1].datumStr)
        assertEquals("Fr, 13.11.2026", sorted[2].datumStr)
    }
}
