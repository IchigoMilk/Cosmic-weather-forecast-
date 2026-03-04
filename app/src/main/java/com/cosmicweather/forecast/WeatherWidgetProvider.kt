package com.cosmicweather.forecast

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.cosmicweather.forecast.api.AirQualityApi
import com.cosmicweather.forecast.api.SpaceWeatherApi
import com.cosmicweather.forecast.api.WeatherApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, WeatherWidgetProvider::class.java)
            )
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.cosmicweather.forecast.WIDGET_REFRESH"

        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val loc = Cities.default

            val refreshIntent = Intent(context, WeatherWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val pendingRefresh = PendingIntent.getBroadcast(
                context, widgetId, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingLaunch = PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Show "updating" state immediately
            val loadingViews = RemoteViews(context.packageName, R.layout.widget_weather)
            loadingViews.setOnClickPendingIntent(R.id.widget_refresh_btn, pendingRefresh)
            loadingViews.setOnClickPendingIntent(R.id.widget_root, pendingLaunch)
            loadingViews.setTextViewText(R.id.widget_location, "LOC: ${loc.name}")
            loadingViews.setTextViewText(R.id.widget_status, "SYS: UPDATING...")
            manager.updateAppWidget(widgetId, loadingViews)

            Thread {
                val views = RemoteViews(context.packageName, R.layout.widget_weather)
                views.setOnClickPendingIntent(R.id.widget_refresh_btn, pendingRefresh)
                views.setOnClickPendingIntent(R.id.widget_root, pendingLaunch)
                views.setTextViewText(R.id.widget_location, "LOC: ${loc.name}")

                try {
                    val w = WeatherApi.fetch(loc.lat, loc.lon)
                    val desc = WeatherApi.weatherDescription(w.weatherCode)
                    views.setTextViewText(
                        R.id.widget_temperature,
                        "${"%.1f".format(w.temperature)}\u00B0C"
                    )
                    views.setTextViewText(R.id.widget_condition, desc)
                } catch (e: Exception) {
                    views.setTextViewText(R.id.widget_temperature, "ERR")
                    views.setTextViewText(R.id.widget_condition, "Weather error")
                }

                try {
                    val sw = SpaceWeatherApi.fetch()
                    views.setTextViewText(
                        R.id.widget_kp,
                        "KP: ${"%.1f".format(sw.kpIndex)}"
                    )
                } catch (e: Exception) {
                    views.setTextViewText(R.id.widget_kp, "KP: ERR")
                }

                try {
                    val aq = AirQualityApi.fetch(loc.lat, loc.lon)
                    val cat = AirQualityApi.aqiCategory(aq.usAqi)
                    views.setTextViewText(R.id.widget_aqi, "AQI: ${aq.usAqi} [$cat]")
                } catch (e: Exception) {
                    views.setTextViewText(R.id.widget_aqi, "AQI: ERR")
                }

                val ts = SimpleDateFormat("HH:mm", Locale.US).format(Date())
                views.setTextViewText(R.id.widget_status, "SYS: UPDATED $ts")
                manager.updateAppWidget(widgetId, views)
            }.start()
        }
    }
}
