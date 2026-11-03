package com.econexeinsatzliste.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.econexeinsatzliste.MainActivity
import com.econexeinsatzliste.R
import com.econexeinsatzliste.data.Einsatz
import java.util.Calendar

object EinsatzNotificationHelper {
    const val CHANNEL_ID = "econex_einsatz_reminders"
    private const val CHANNEL_NAME = "Einsatz-Erinnerungen"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Erinnerungen am Vorabend um 19 Uhr für anstehende Einsätze"
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Schedules an alarm at 19:00 (7 PM) the evening before the shift date.
     */
    fun scheduleEveningReminder(context: Context, einsatz: Einsatz) {
        if (einsatz.datumMillis <= 0L) return

        val calendar = Calendar.getInstance().apply {
            timeInMillis = einsatz.datumMillis
            add(Calendar.DAY_OF_YEAR, -1) // Evening BEFORE shift
            set(Calendar.HOUR_OF_DAY, 19)  // 19:00 (7 PM)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val targetMillis = calendar.timeInMillis
        // Only schedule if reminder time is in the future
        if (targetMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, EinsatzReminderReceiver::class.java).apply {
            putExtra("einsatz_id", einsatz.id)
            putExtra("datum_str", einsatz.datumStr)
            putExtra("ab_zeit", einsatz.abZeit)
            putExtra("ab_bhf", einsatz.abBahnhof)
            putExtra("an_bhf", einsatz.anBahnhof)
            putExtra("linie", einsatz.linie)
            putExtra("tour_nr", einsatz.tourNr)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            einsatz.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelEveningReminder(context: Context, einsatzId: Long) {
        val intent = Intent(context, EinsatzReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            einsatzId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}
