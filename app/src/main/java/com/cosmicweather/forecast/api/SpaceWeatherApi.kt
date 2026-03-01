package com.cosmicweather.forecast.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class SpaceWeatherData(
    val kpIndex: Double,
    val kpLevel: String,
    val solarWindSpeed: Double,
    val solarWindDensity: Double,
    val solarWindTemp: Double,
    val kpTimestamp: String,
    val swTimestamp: String
)

class SpaceWeatherApi(private val client: OkHttpClient) {

    suspend fun fetchSpaceWeather(): SpaceWeatherData = withContext(Dispatchers.IO) {
        val kpData = fetchKpIndex()
        val swData = fetchSolarWind()
        SpaceWeatherData(
            kpIndex = kpData.first,
            kpLevel = getKpLevel(kpData.first),
            solarWindSpeed = swData.second,
            solarWindDensity = swData.first,
            solarWindTemp = swData.third,
            kpTimestamp = kpData.second,
            swTimestamp = swData.fourth
        )
    }

    private suspend fun fetchKpIndex(): Pair<Double, String> {
        val url = "https://services.swpc.noaa.gov/products/noaa-planetary-k-index.json"
        val body = getResponseBody(url)
        val array = JSONArray(body)
        // array[0] is header row; get latest data entry
        val lastIndex = array.length() - 1
        val entry = array.getJSONArray(lastIndex)
        val timestamp = entry.getString(0)
        val kp = entry.getString(1).toDoubleOrNull() ?: 0.0
        return Pair(kp, timestamp)
    }

    private suspend fun fetchSolarWind(): Quadruple<Double, Double, Double, String> {
        val url = "https://services.swpc.noaa.gov/products/solar-wind/plasma-7-day.json"
        val body = getResponseBody(url)
        val array = JSONArray(body)
        // array[0] is header row; get latest data entry
        val lastIndex = array.length() - 1
        val entry = array.getJSONArray(lastIndex)
        val timestamp = entry.getString(0)
        val density = entry.getString(1).toDoubleOrNull() ?: 0.0
        val speed = entry.getString(2).toDoubleOrNull() ?: 0.0
        val temp = entry.getString(3).toDoubleOrNull() ?: 0.0
        return Quadruple(density, speed, temp, timestamp)
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

    private fun getKpLevel(kp: Double): String = when {
        kp < 2.0 -> "QUIET"
        kp < 4.0 -> "UNSETTLED"
        kp < 5.0 -> "ACTIVE"
        kp < 6.0 -> "MINOR STORM"
        kp < 7.0 -> "MODERATE STORM"
        kp < 8.0 -> "STRONG STORM"
        kp < 9.0 -> "SEVERE STORM"
        else     -> "EXTREME STORM"
    }
}

/** Simple 4-tuple to avoid adding a dependency. */
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
