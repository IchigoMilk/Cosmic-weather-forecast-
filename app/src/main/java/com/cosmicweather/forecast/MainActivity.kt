package com.cosmicweather.forecast

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.cosmicweather.forecast.api.AirQualityApi
import com.cosmicweather.forecast.api.SpaceWeatherApi
import com.cosmicweather.forecast.api.WeatherApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var tvLocation: TextView
    private lateinit var tvGpsBtn: TextView
    private lateinit var tvCityBtn: TextView
    private lateinit var tvSpaceWeather: TextView
    private lateinit var tvWeather: TextView
    private lateinit var tvAirQuality: TextView
    private lateinit var tvStatus: TextView
    private lateinit var scrollView: ScrollView

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentLocation = Cities.default
    private var isRefreshing = false

    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshAllData()
            mainHandler.postDelayed(this, 60_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLocation    = findViewById(R.id.tvLocation)
        tvGpsBtn      = findViewById(R.id.tvGpsBtn)
        tvCityBtn     = findViewById(R.id.tvCityBtn)
        tvSpaceWeather = findViewById(R.id.tvSpaceWeather)
        tvWeather     = findViewById(R.id.tvWeather)
        tvAirQuality  = findViewById(R.id.tvAirQuality)
        tvStatus      = findViewById(R.id.tvStatus)
        scrollView    = findViewById(R.id.scrollView)

        updateLocationLabel()

        tvGpsBtn.setOnClickListener { requestGpsLocation() }
        tvCityBtn.setOnClickListener { showCityPicker() }

        mainHandler.post(refreshRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(refreshRunnable)
    }

    private fun updateLocationLabel() {
        tvLocation.text = "LOC: ${currentLocation.name}"
    }

    private fun refreshAllData() {
        if (isRefreshing) return
        isRefreshing = true
        val now = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        tvStatus.text = "SYS: FETCHING DATA... [$now]"

        Thread {
            var swText: String
            var wText: String
            var aqText: String

            // Space Weather
            swText = try {
                val d = SpaceWeatherApi.fetch()
                buildSpaceWeatherText(d)
            } catch (e: Exception) {
                buildErrorBox("COSMIC RAY / SPACE WEATHER", e.message ?: "Network error")
            }

            // Weather
            wText = try {
                val d = WeatherApi.fetch(currentLocation.lat, currentLocation.lon)
                buildWeatherText(d)
            } catch (e: Exception) {
                buildErrorBox("WEATHER FORECAST", e.message ?: "Network error")
            }

            // Air Quality
            aqText = try {
                val d = AirQualityApi.fetch(currentLocation.lat, currentLocation.lon)
                buildAirQualityText(d)
            } catch (e: Exception) {
                buildErrorBox("AIR QUALITY", e.message ?: "Network error")
            }

            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            mainHandler.post {
                tvSpaceWeather.text = swText
                tvWeather.text = wText
                tvAirQuality.text = aqText
                tvStatus.text = "SYS: LAST UPDATE $ts UTC+9"
                isRefreshing = false
            }
        }.start()
    }

    // ──────────────────────────────────────────────
    // Terminal text builders
    // ──────────────────────────────────────────────

    private fun buildSpaceWeatherText(d: com.cosmicweather.forecast.api.SpaceWeatherData): String {
        val w = 44
        val kpLevel = kpBar(d.kpIndex)
        val kpStatus = kpStatus(d.kpIndex)
        val tempStr = "%.2e".format(d.solarWindTemp)
        return buildBox(
            "COSMIC RAY / SPACE WEATHER", w,
            listOf(
                "KP INDEX   : ${"%.1f".format(d.kpIndex)}  [$kpStatus]",
                "KP LEVEL   : $kpLevel",
                "SOLAR WIND : ${"%.0f".format(d.solarWindSpeed)} km/s",
                "SW DENSITY : ${"%.1f".format(d.solarWindDensity)} p/cm\u00B3",
                "SW TEMP    : $tempStr K",
                "TIMESTAMP  : ${d.timestamp} UTC"
            )
        )
    }

    private fun buildWeatherText(d: com.cosmicweather.forecast.api.WeatherData): String {
        val w = 44
        val desc = WeatherApi.weatherDescription(d.weatherCode)
        return buildBox(
            "WEATHER FORECAST", w,
            listOf(
                "LOCATION   : ${currentLocation.name}",
                "TEMPERATURE: ${"%.1f".format(d.temperature)} \u00B0C",
                "HUMIDITY   : ${d.humidity}%",
                "WIND SPEED : ${"%.1f".format(d.windSpeed)} m/s",
                "PRECIP     : ${"%.1f".format(d.precipitation)} mm",
                "CONDITION  : $desc",
                "TIME       : ${d.timestamp}"
            )
        )
    }

    private fun buildAirQualityText(d: com.cosmicweather.forecast.api.AirQualityData): String {
        val w = 44
        val cat = AirQualityApi.aqiCategory(d.usAqi)
        return buildBox(
            "AIR QUALITY", w,
            listOf(
                "AQI (US)   : ${d.usAqi} [$cat]",
                "PM2.5      : ${"%.1f".format(d.pm25)} \u03BCg/m\u00B3",
                "PM10       : ${"%.1f".format(d.pm10)} \u03BCg/m\u00B3",
                "TIME       : ${d.timestamp}"
            )
        )
    }

    private fun buildErrorBox(title: String, msg: String): String {
        return buildBox(title, 44, listOf("ERROR: $msg"))
    }

    private fun buildBox(title: String, width: Int, lines: List<String>): String {
        val sb = StringBuilder()
        sb.appendLine("\u2605 $title")
        for (line in lines) {
            sb.appendLine("  $line")
        }
        return sb.toString().trimEnd()
    }

    private fun kpBar(kp: Double): String {
        val filled = (kp / 9.0 * 10).toInt().coerceIn(0, 10)
        return "[" + "\u2588".repeat(filled) + "\u2591".repeat(10 - filled) + "]"
    }

    private fun kpStatus(kp: Double) = when {
        kp < 1.0 -> "QUIET"
        kp < 3.0 -> "UNSETTLED"
        kp < 5.0 -> "ACTIVE"
        kp < 6.0 -> "MINOR STORM"
        kp < 7.0 -> "MODERATE STORM"
        kp < 8.0 -> "STRONG STORM"
        kp < 9.0 -> "SEVERE STORM"
        else -> "EXTREME STORM"
    }

    // ──────────────────────────────────────────────
    // Location
    // ──────────────────────────────────────────────

    private fun showCityPicker() {
        val names = Cities.list.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select City")
            .setItems(names) { _, idx ->
                currentLocation = Cities.list[idx]
                updateLocationLabel()
                refreshAllData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestGpsLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQ_LOCATION
            )
            return
        }
        getGpsLocation()
    }

    @Suppress("MissingPermission")
    private fun getGpsLocation() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        tvStatus.text = "SYS: ACQUIRING GPS FIX..."

        val lastKnown = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (lastKnown != null) {
            applyLocation(lastKnown)
            return
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                lm.removeUpdates(this)
                applyLocation(loc)
            }
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        try {
            lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper())
        } catch (e: Exception) {
            try {
                lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper())
            } catch (e2: Exception) {
                Toast.makeText(this, "GPS unavailable", Toast.LENGTH_SHORT).show()
                tvStatus.text = "SYS: GPS ERROR"
            }
        }
    }

    private fun applyLocation(loc: Location) {
        currentLocation = CityLocation(
            "GPS (%.4f, %.4f)".format(loc.latitude, loc.longitude),
            loc.latitude,
            loc.longitude
        )
        updateLocationLabel()
        refreshAllData()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQ_LOCATION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getGpsLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQ_LOCATION = 1001
    }
}
