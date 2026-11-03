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
    fun testParseRealUserCsvFile() {
        val userCsv = """
            Datum;BS;Ab-Zeit;Ab-Bhf;An-Zeit;An-Bhf;Tour-Nr;Zug-Nr;Linie;Projekt;TN;HBD;Kl;Euro;Hinweis;Status;Hinweis
            Di, 10.11.2026;1;21:13;Dinslaken, Bahnhof;22:00;Wesel Bf;7504;2003017;SB3;2504;2;;;     20,54;;;
            Mi, 11.11.2026;1;08:29;Wesel Bf;08:51;Wesel, Schulzentrum Nord;6451;2078001;78;2504;2;;;     15,00;;;
            Fr, 13.11.2026;1;14:04;Xanten, Bahnhof;14:22;Kalkar, Markt;8060;2044036;44;2504;2;;;     22,00;;;
            Fr, 13.11.2026;3;14:35;Kalkar, Markt;14:55;Xanten, Bahnhof;8060;;X27;2504;;;;;;;
            Sa, 14.11.2026;1;17:28;Dinslaken, Bahnhof;18:15;Wesel Bf;7738;2003211;SB3;2504;2;;;     20,00;;;
        """.trimIndent()

        val parsed = EinsatzCsvParser.parseCsvText(userCsv)

        assertEquals(5, parsed.size)

        // Row 1
        assertEquals("Di, 10.11.2026", parsed[0].datumStr)
        assertEquals("21:13", parsed[0].abZeit)
        assertEquals("Dinslaken, Bahnhof", parsed[0].abBahnhof)
        assertEquals("22:00", parsed[0].anZeit)
        assertEquals("Wesel Bf", parsed[0].anBahnhof)
        assertEquals("7504", parsed[0].tourNr)
        assertEquals(20.54, parsed[0].betragEuro, 0.01)

        // Row 2
        assertEquals("Mi, 11.11.2026", parsed[1].datumStr)
        assertEquals("08:29", parsed[1].abZeit)
        assertEquals("Wesel Bf", parsed[1].abBahnhof)
        assertEquals("08:51", parsed[1].anZeit)
        assertEquals("Wesel, Schulzentrum Nord", parsed[1].anBahnhof)
        assertEquals("6451", parsed[1].tourNr)
        assertEquals(15.00, parsed[1].betragEuro, 0.01)

        // Row 3
        assertEquals("Xanten, Bahnhof", parsed[2].abBahnhof)
        assertEquals("Kalkar, Markt", parsed[2].anBahnhof)
        assertEquals("8060", parsed[2].tourNr)
        assertEquals(22.00, parsed[2].betragEuro, 0.01)

        // Row 4
        assertEquals("Kalkar, Markt", parsed[3].abBahnhof)
        assertEquals("Xanten, Bahnhof", parsed[3].anBahnhof)
        assertEquals("8060", parsed[3].tourNr)
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
