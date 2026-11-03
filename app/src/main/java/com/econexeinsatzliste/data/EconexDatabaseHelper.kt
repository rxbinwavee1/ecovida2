package com.econexeinsatzliste.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class EconexDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "econex_einsatz.db"
        private const val DATABASE_VERSION = 4

        const val TABLE_EINSATZ = "einsatz"
        const val COLUMN_ID = "id"
        const val COLUMN_DATUM_STR = "datum_str"
        const val COLUMN_DATUM_MILLIS = "datum_millis"
        const val COLUMN_AB_ZEIT = "ab_zeit"
        const val COLUMN_AB_BAHNHOF = "ab_bahnhof"
        const val COLUMN_AN_ZEIT = "an_zeit"
        const val COLUMN_AN_BAHNHOF = "an_bahnhof"
        const val COLUMN_TOUR_NR = "tour_nr"
        const val COLUMN_LINIE = "linie"
        const val COLUMN_BETRAG_EURO = "betrag_euro"
        const val COLUMN_STATUS = "status"
        const val COLUMN_CREATED_AT_MILLIS = "created_at_millis"

        const val MILLIS_IN_30_DAYS = 30L * 24 * 60 * 60 * 1000
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_EINSATZ (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATUM_STR TEXT,
                $COLUMN_DATUM_MILLIS INTEGER,
                $COLUMN_AB_ZEIT TEXT,
                $COLUMN_AB_BAHNHOF TEXT,
                $COLUMN_AN_ZEIT TEXT,
                $COLUMN_AN_BAHNHOF TEXT,
                $COLUMN_TOUR_NR TEXT,
                $COLUMN_LINIE TEXT,
                $COLUMN_BETRAG_EURO REAL,
                $COLUMN_STATUS TEXT,
                $COLUMN_CREATED_AT_MILLIS INTEGER
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EINSATZ")
        onCreate(db)
    }

    fun deleteEntriesOlderThan30Days(nowMillis: Long = System.currentTimeMillis()): Int {
        val cutoffMillis = nowMillis - MILLIS_IN_30_DAYS
        val db = writableDatabase
        return db.delete(
            TABLE_EINSATZ,
            "($COLUMN_DATUM_MILLIS > 0 AND $COLUMN_DATUM_MILLIS < ?) OR ($COLUMN_DATUM_MILLIS <= 0 AND $COLUMN_CREATED_AT_MILLIS < ?)",
            arrayOf(cutoffMillis.toString(), cutoffMillis.toString())
        )
    }

    fun insertEinsatz(einsatz: Einsatz): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATUM_STR, einsatz.datumStr)
            put(COLUMN_DATUM_MILLIS, einsatz.datumMillis)
            put(COLUMN_AB_ZEIT, einsatz.abZeit)
            put(COLUMN_AB_BAHNHOF, einsatz.abBahnhof)
            put(COLUMN_AN_ZEIT, einsatz.anZeit)
            put(COLUMN_AN_BAHNHOF, einsatz.anBahnhof)
            put(COLUMN_TOUR_NR, einsatz.tourNr)
            put(COLUMN_LINIE, einsatz.linie)
            put(COLUMN_BETRAG_EURO, einsatz.betragEuro)
            put(COLUMN_STATUS, einsatz.status.name)
            put(COLUMN_CREATED_AT_MILLIS, einsatz.createdAtMillis)
        }
        return db.insert(TABLE_EINSATZ, null, values)
    }

    fun insertAll(list: List<Einsatz>): Int {
        var count = 0
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (item in list) {
                val id = insertEinsatz(item)
                if (id != -1L) count++
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return count
    }

    fun updateEinsatz(einsatz: Einsatz): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATUM_STR, einsatz.datumStr)
            put(COLUMN_DATUM_MILLIS, einsatz.datumMillis)
            put(COLUMN_AB_ZEIT, einsatz.abZeit)
            put(COLUMN_AB_BAHNHOF, einsatz.abBahnhof)
            put(COLUMN_AN_ZEIT, einsatz.anZeit)
            put(COLUMN_AN_BAHNHOF, einsatz.anBahnhof)
            put(COLUMN_TOUR_NR, einsatz.tourNr)
            put(COLUMN_LINIE, einsatz.linie)
            put(COLUMN_BETRAG_EURO, einsatz.betragEuro)
            put(COLUMN_STATUS, einsatz.status.name)
        }
        val rows = db.update(TABLE_EINSATZ, values, "$COLUMN_ID = ?", arrayOf(einsatz.id.toString()))
        return rows > 0
    }

    fun deleteEinsatz(id: Long): Boolean {
        val db = writableDatabase
        val rows = db.delete(TABLE_EINSATZ, "$COLUMN_ID = ?", arrayOf(id.toString()))
        return rows > 0
    }

    fun getAllEinsatze(): List<Einsatz> {
        deleteEntriesOlderThan30Days()
        val list = mutableListOf<Einsatz>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_EINSATZ,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_DATUM_MILLIS ASC, $COLUMN_AB_ZEIT ASC, $COLUMN_CREATED_AT_MILLIS ASC"
        )

        cursor.use { c ->
            val idIndex = c.getColumnIndexOrThrow(COLUMN_ID)
            val datumStrIndex = c.getColumnIndexOrThrow(COLUMN_DATUM_STR)
            val datumMillisIndex = c.getColumnIndexOrThrow(COLUMN_DATUM_MILLIS)
            val abZeitIndex = c.getColumnIndexOrThrow(COLUMN_AB_ZEIT)
            val abBahnhofIndex = c.getColumnIndexOrThrow(COLUMN_AB_BAHNHOF)
            val anZeitIndex = c.getColumnIndexOrThrow(COLUMN_AN_ZEIT)
            val anBahnhofIndex = c.getColumnIndexOrThrow(COLUMN_AN_BAHNHOF)
            val tourNrIndex = c.getColumnIndexOrThrow(COLUMN_TOUR_NR)
            val linieIndex = c.getColumnIndexOrThrow(COLUMN_LINIE)
            val betragIndex = c.getColumnIndexOrThrow(COLUMN_BETRAG_EURO)
            val statusIndex = c.getColumnIndexOrThrow(COLUMN_STATUS)
            val createdIndex = c.getColumnIndexOrThrow(COLUMN_CREATED_AT_MILLIS)

            while (c.moveToNext()) {
                val item = Einsatz(
                    id = c.getLong(idIndex),
                    datumStr = c.getString(datumStrIndex) ?: "",
                    datumMillis = c.getLong(datumMillisIndex),
                    abZeit = c.getString(abZeitIndex) ?: "",
                    abBahnhof = c.getString(abBahnhofIndex) ?: "",
                    anZeit = c.getString(anZeitIndex) ?: "",
                    anBahnhof = c.getString(anBahnhofIndex) ?: "",
                    tourNr = c.getString(tourNrIndex) ?: "",
                    linie = c.getString(linieIndex) ?: "",
                    betragEuro = c.getDouble(betragIndex),
                    status = EinsatzStatus.fromName(c.getString(statusIndex)),
                    createdAtMillis = c.getLong(createdIndex)
                )
                list.add(item)
            }
        }
        return list
    }

    fun deleteAll() {
        val db = writableDatabase
        db.delete(TABLE_EINSATZ, null, null)
    }
}
