package com.econexeinsatzliste.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.econexeinsatzliste.MainActivity
import com.econexeinsatzliste.R
import com.econexeinsatzliste.data.EconexDatabaseHelper
import com.econexeinsatzliste.data.EinsatzStatus

class NextShiftWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, NextShiftWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, NextShiftWidgetProvider::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.widget_next_shift)

    // Read from DB (disabled auto-delete trigger so we don't accidentally write to DB on home screen)
    val db = EconexDatabaseHelper(context)
    val all = db.getAllEinsatze(isAutoDeleteEnabled = false)
    val next = all.firstOrNull { it.status != EinsatzStatus.ABGESCHLOSSEN && it.status != EinsatzStatus.ABGEBROCHEN }

    if (next != null) {
        val bsLabel = next.getBsLabel()
        val bsPrefix = if (bsLabel.isNotBlank()) "[$bsLabel] " else ""
        views.setTextViewText(R.id.widget_date, "${next.datumStr} ab ${next.abZeit} Uhr")
        views.setTextViewText(R.id.widget_route, "$bsPrefix${next.abBahnhof} ➔ ${next.anBahnhof}")
    } else {
        views.setTextViewText(R.id.widget_date, "Kein anstehender Einsatz")
        views.setTextViewText(R.id.widget_route, "Tippe hier zum Planen")
    }

    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Make the entire widget clickable to launch the app
    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}
