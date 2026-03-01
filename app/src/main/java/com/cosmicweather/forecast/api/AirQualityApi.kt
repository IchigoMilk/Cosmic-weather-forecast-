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

data class AirQualityData(
    val aqi: Int,
    val aqiLevel: String,
    val pm25: Double,
    val pm10: Double,
    val timestamp: String
)

class AirQualityApi(private val client: OkHttpClient) {

    suspend fun fetchAirQuality(lat: Double, lon: Double): AirQualityData = withContext(Dispatchers.IO) {
        val url = "https://air-quality-api.open-meteo.com/v1/air-quality" +
                "?latitude=$lat&longitude=$lon" +
                "&current=pm10,pm2_5,us_aqi"

        val body = getResponseBody(url)
        val json = JSONObject(body)
        val current = json.getJSONObject("current")

        val aqi = if (current.isNull("us_aqi")) 0 else current.getInt("us_aqi")
        val pm25 = if (current.isNull("pm2_5")) 0.0 else current.getDouble("pm2_5")
        val pm10 = if (current.isNull("pm10")) 0.0 else current.getDouble("pm10")

        AirQualityData(
            aqi = aqi,
            aqiLevel = aqiToLevel(aqi),
            pm25 = pm25,
            pm10 = pm10,
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

    private fun aqiToLevel(aqi: Int): String = when {
        aqi <= 50  -> "GOOD"
        aqi <= 100 -> "MODERATE"
        aqi <= 150 -> "UNHEALTHY FOR SENSITIVE"
        aqi <= 200 -> "UNHEALTHY"
        aqi <= 300 -> "VERY UNHEALTHY"
        else       -> "HAZARDOUS"
    }
}
