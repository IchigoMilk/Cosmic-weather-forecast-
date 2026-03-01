package com.cosmicweather.forecast.api

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class SpaceWeatherData(
    val kpIndex: Double,
    val solarWindSpeed: Double,
    val solarWindDensity: Double,
    val solarWindTemp: Double,
    val timestamp: String
)

object SpaceWeatherApi {

    private const val KP_URL =
        "https://services.swpc.noaa.gov/products/noaa-planetary-k-index.json"
    private const val SOLAR_WIND_URL =
        "https://services.swpc.noaa.gov/products/solar-wind/plasma-7-day.json"

    fun fetch(): SpaceWeatherData {
        val kpData = fetchJson(KP_URL)
        val windData = fetchJson(SOLAR_WIND_URL)

        val kpArray = JSONArray(kpData)
        // Skip header row (index 0), get last real entry
        val lastKp = kpArray.getJSONArray(kpArray.length() - 1)
        val kpIndex = lastKp.getString(1).toDoubleOrNull() ?: 0.0
        val kpTimestamp = lastKp.getString(0)

        val windArray = JSONArray(windData)
        val lastWind = windArray.getJSONArray(windArray.length() - 1)
        val windDensity = lastWind.getString(1).toDoubleOrNull() ?: 0.0
        val windSpeed = lastWind.getString(2).toDoubleOrNull() ?: 0.0
        val windTemp = lastWind.getString(3).toDoubleOrNull() ?: 0.0

        return SpaceWeatherData(
            kpIndex = kpIndex,
            solarWindSpeed = windSpeed,
            solarWindDensity = windDensity,
            solarWindTemp = windTemp,
            timestamp = kpTimestamp
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
}
