package com.cosmicweather.forecast.api

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AirQualityData(
    val usAqi: Int,
    val pm25: Double,
    val pm10: Double,
    val timestamp: String
)

object AirQualityApi {

    fun fetch(lat: Double, lon: Double): AirQualityData {
        val urlStr = "https://air-quality-api.open-meteo.com/v1/air-quality" +
            "?latitude=$lat&longitude=$lon" +
            "&current=pm10,pm2_5,us_aqi"

        val json = fetchJson(urlStr)
        val root = JSONObject(json)
        val current = root.getJSONObject("current")

        return AirQualityData(
            usAqi = current.getInt("us_aqi"),
            pm25 = current.getDouble("pm2_5"),
            pm10 = current.getDouble("pm10"),
            timestamp = current.getString("time")
        )
    }

    private fun fetchJson(urlStr: String): String {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "CosmicWeatherApp/1.0")
        return try {
            conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }

    fun aqiCategory(aqi: Int): String = when {
        aqi <= 50 -> "GOOD"
        aqi <= 100 -> "MODERATE"
        aqi <= 150 -> "UNHEALTHY FOR SENSITIVE"
        aqi <= 200 -> "UNHEALTHY"
        aqi <= 300 -> "VERY UNHEALTHY"
        else -> "HAZARDOUS"
    }
}
