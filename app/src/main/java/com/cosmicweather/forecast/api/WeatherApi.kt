package com.cosmicweather.forecast.api

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherData(
    val temperature: Double,
    val humidity: Int,
    val windSpeed: Double,
    val weatherCode: Int,
    val precipitation: Double,
    val timestamp: String
)

object WeatherApi {

    fun fetch(lat: Double, lon: Double): WeatherData {
        val urlStr = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,precipitation" +
            "&wind_speed_unit=ms"

        val json = fetchJson(urlStr)
        val root = JSONObject(json)
        val current = root.getJSONObject("current")

        return WeatherData(
            temperature = current.getDouble("temperature_2m"),
            humidity = current.getInt("relative_humidity_2m"),
            windSpeed = current.getDouble("wind_speed_10m"),
            weatherCode = current.getInt("weather_code"),
            precipitation = current.getDouble("precipitation"),
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

    fun weatherDescription(code: Int): String = when (code) {
        0 -> "Clear Sky"
        1 -> "Mainly Clear"
        2 -> "Partly Cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        71, 73, 75 -> "Snow"
        80, 81, 82 -> "Showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm w/ Hail"
        else -> "Code $code"
    }
}
