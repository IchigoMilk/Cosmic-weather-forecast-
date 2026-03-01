package com.cosmicweather.forecast.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class WeatherData(
    val temperature: Double,
    val humidity: Int,
    val windSpeed: Double,
    val weatherCode: Int,
    val condition: String,
    val precipitation: Double,
    val timestamp: String
)

class WeatherApi(private val client: OkHttpClient) {

    suspend fun fetchWeather(lat: Double, lon: Double): WeatherData = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,precipitation" +
                "&wind_speed_unit=ms"

        val body = getResponseBody(url)
        val json = JSONObject(body)
        val current = json.getJSONObject("current")

        val code = current.getInt("weather_code")
        WeatherData(
            temperature = current.getDouble("temperature_2m"),
            humidity = current.getInt("relative_humidity_2m"),
            windSpeed = current.getDouble("wind_speed_10m"),
            weatherCode = code,
            condition = weatherCodeToDescription(code),
            precipitation = current.getDouble("precipitation"),
            timestamp = current.getString("time")
        )
    }

    private suspend fun getResponseBody(url: String): String =
        suspendCancellableCoroutine { cont ->
            val request = Request.Builder().url(url).build()
            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    cont.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            cont.resumeWithException(IOException("HTTP ${response.code}"))
                        } else {
                            cont.resume(response.body?.string() ?: "")
                        }
                    }
                }
            })
        }

    private fun weatherCodeToDescription(code: Int): String = when (code) {
        0            -> "Clear Sky"
        1            -> "Mainly Clear"
        2            -> "Partly Cloudy"
        3            -> "Overcast"
        45, 48       -> "Foggy"
        51, 53, 55   -> "Drizzle"
        61, 63, 65   -> "Rain"
        71, 73, 75   -> "Snow"
        77           -> "Snow Grains"
        80, 81, 82   -> "Rain Showers"
        85, 86       -> "Snow Showers"
        95           -> "Thunderstorm"
        96, 99       -> "Thunderstorm w/ Hail"
        else         -> "Unknown (code $code)"
    }
}
