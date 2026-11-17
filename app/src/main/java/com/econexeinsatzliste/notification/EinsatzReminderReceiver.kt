package com.econexeinsatzliste.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class EinsatzReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("einsatz_id", 0L)
        val isOneHourBefore = intent.getBooleanExtra("is_one_hour_before", false)
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

        val title = if (isOneHourBefore) "⚡ In 1 Stunde: Einsatz-Abfahrt!" else "Erinnerung: Einsatz heute!"
        val message = if (isOneHourBefore) {
            "Abfahrt in 1 Std. um $abZeit Uhr: $routeText$extraInfo"
        } else {
            "Heute ($datumStr) ab $abZeit Uhr: $routeText$extraInfo"
        }

        val notifId = if (isOneHourBefore) (id * 10 + 2).toInt() else (id * 10 + 1).toInt()

        EinsatzNotificationHelper.showNotification(
            context = context,
            notificationId = notifId.coerceAtLeast(1001),
            title = title,
            message = message
        )
    }
}
