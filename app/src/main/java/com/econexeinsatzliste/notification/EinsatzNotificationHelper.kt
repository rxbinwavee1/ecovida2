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
    const val CHANNEL_ID = "econex_einsatz_reminders_v2"
    private const val CHANNEL_NAME = "Einsatz-Erinnerungen"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Erinnerungen am Vortag um 18 Uhr & 3 Std. vor Abfahrt"
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Schedules both:
     * 1) Day BEFORE shift at 18:00 (6:00 PM)
     * 2) 3 hours BEFORE departure time on shift date
     */
    fun scheduleAllReminders(context: Context, einsatz: Einsatz) {
        scheduleDayBeforeReminder(context, einsatz)
        scheduleThreeHoursBeforeReminder(context, einsatz)
    }

    fun scheduleDayBeforeReminder(context: Context, einsatz: Einsatz) {
        if (einsatz.datumMillis <= 0L) return

        val calendar = Calendar.getInstance().apply {
            timeInMillis = einsatz.datumMillis
            add(Calendar.DAY_OF_YEAR, -1) // Day before shift
            set(Calendar.HOUR_OF_DAY, 18)  // 18:00 (6 PM)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val targetMillis = calendar.timeInMillis
        if (targetMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, EinsatzReminderReceiver::class.java).apply {
            putExtra("einsatz_id", einsatz.id)
            putExtra("reminder_type", "DAY_BEFORE")
            putExtra("datum_str", einsatz.datumStr)
            putExtra("ab_zeit", einsatz.abZeit)
            putExtra("ab_bhf", einsatz.abBahnhof)
            putExtra("an_bhf", einsatz.anBahnhof)
            putExtra("linie", einsatz.linie)
            putExtra("tour_nr", einsatz.tourNr)
        }

        val alarmId = (einsatz.id * 10 + 1).toInt()
        setAlarm(context, alarmId, targetMillis, intent)
    }

    fun scheduleThreeHoursBeforeReminder(context: Context, einsatz: Einsatz) {
        if (einsatz.datumMillis <= 0L || einsatz.abZeit.isBlank()) return

        val timeParts = einsatz.abZeit.split(":")
        if (timeParts.size < 2) return

        val hour = timeParts[0].trim().toIntOrNull() ?: return
        val minute = timeParts[1].trim().toIntOrNull() ?: return

        val calendar = Calendar.getInstance().apply {
            timeInMillis = einsatz.datumMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.HOUR_OF_DAY, -3) // 3 Hours Before Departure
        }

        val targetMillis = calendar.timeInMillis
        if (targetMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, EinsatzReminderReceiver::class.java).apply {
            putExtra("einsatz_id", einsatz.id)
            putExtra("reminder_type", "THREE_HOURS_BEFORE")
            putExtra("datum_str", einsatz.datumStr)
            putExtra("ab_zeit", einsatz.abZeit)
            putExtra("ab_bhf", einsatz.abBahnhof)
            putExtra("an_bhf", einsatz.anBahnhof)
            putExtra("linie", einsatz.linie)
            putExtra("tour_nr", einsatz.tourNr)
        }

        val alarmId = (einsatz.id * 10 + 2).toInt()
        setAlarm(context, alarmId, targetMillis, intent)
    }

    private fun setAlarm(context: Context, alarmId: Int, targetMillis: Long, intent: Intent) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
                }
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun cancelReminder(context: Context, einsatzId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        listOf(1, 2).forEach { subId ->
            val intent = Intent(context, EinsatzReminderReceiver::class.java)
            val alarmId = (einsatzId * 10 + subId).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
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
