package com.econexeinsatzliste.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.econexeinsatzliste.data.EconexDatabaseHelper
import com.econexeinsatzliste.data.EinsatzStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val dbHelper = EconexDatabaseHelper(context.applicationContext)
                val activeList = dbHelper.getAllEinsatze().filter {
                    it.status != EinsatzStatus.ABGESCHLOSSEN && it.status != EinsatzStatus.ABGEBROCHEN
                }
                activeList.forEach { einsatz ->
                    EinsatzNotificationHelper.scheduleAllReminders(context.applicationContext, einsatz)
                }
            }
        }
    }
}
