package com.widgeter.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** A month calendar with today highlighted. */
class CalendarWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) render(context, mgr, id)
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.calendar_widget)
        val today = LocalDate.now()
        val ym = YearMonth.from(today)
        views.setTextViewText(R.id.cal_title, today.format(DateTimeFormatter.ofPattern("MMMM yyyy")))

        // Grid is Sunday-first: MON=1..SUN=7 -> Sunday index 0.
        val firstOffset = ym.atDay(1).dayOfWeek.value % 7
        val daysInMonth = ym.lengthOfMonth()
        val onCard = ContextCompat.getColor(context, R.color.widget_on_card)

        for (i in 0 until 42) {
            val cellId = context.resources.getIdentifier("cal_d$i", "id", context.packageName)
            if (cellId == 0) continue
            val dayNum = i - firstOffset + 1
            if (dayNum in 1..daysInMonth) {
                views.setTextViewText(cellId, dayNum.toString())
                if (dayNum == today.dayOfMonth) {
                    views.setInt(cellId, "setBackgroundResource", R.drawable.today_bg)
                    views.setTextColor(cellId, Color.WHITE)
                } else {
                    views.setInt(cellId, "setBackgroundResource", 0)
                    views.setTextColor(cellId, onCard)
                }
            } else {
                views.setTextViewText(cellId, "")
                views.setInt(cellId, "setBackgroundResource", 0)
            }
        }

        views.setOnClickPendingIntent(R.id.cal_root, openAppPendingIntent(context, 9300))
        mgr.updateAppWidget(id, views)
    }
}
