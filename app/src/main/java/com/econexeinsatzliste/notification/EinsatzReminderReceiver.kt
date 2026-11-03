package com.econexeinsatzliste.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class EinsatzReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("einsatz_id", 0L)
        val datumStr = intent.getStringExtra("datum_str") ?: ""
        val abZeit = intent.getStringExtra("ab_zeit") ?: ""
        val abBhf = intent.getStringExtra("ab_bhf") ?: ""
        val anBhf = intent.getStringExtra("an_bhf") ?: ""
        val linie = intent.getStringExtra("linie") ?: ""
        val tourNr = intent.getStringExtra("tour_nr") ?: ""

        val routeText = if (abBhf.isNotBlank() && anBhf.isNotBlank()) {
            "$abBhf ➔ $anBhf"
        } else {
            "Anstehender Einsatz"
        }

        val linieTourInfo = mutableListOf<String>()
        if (linie.isNotBlank()) linieTourInfo.add("Linie $linie")
        if (tourNr.isNotBlank()) linieTourInfo.add("Tour $tourNr")
        val extraInfo = if (linieTourInfo.isNotEmpty()) " (${linieTourInfo.joinToString(", ")})" else ""

        val message = "Morgen ($datumStr) ab $abZeit Uhr: $routeText$extraInfo"

        EinsatzNotificationHelper.showNotification(
            context = context,
            notificationId = id.toInt().coerceAtLeast(1001),
            title = "Erinnerung: Einsatz morgen!",
            message = message
        )
    }
}
