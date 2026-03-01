package com.cosmicweather.forecast

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cosmicweather.forecast.api.AirQualityApi
import com.cosmicweather.forecast.api.AirQualityData
import com.cosmicweather.forecast.api.SpaceWeatherApi
import com.cosmicweather.forecast.api.SpaceWeatherData
import com.cosmicweather.forecast.api.WeatherApi
import com.cosmicweather.forecast.api.WeatherData
import com.cosmicweather.forecast.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val spaceWeatherApi = SpaceWeatherApi(httpClient)
    private val weatherApi = WeatherApi(httpClient)
    private val airQualityApi = AirQualityApi(httpClient)

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var currentLat = 34.6913  // Kobe, Japan
    private var currentLon = 135.1830
    private var currentLocationName = "Kobe, Japan"

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshInterval = 60_000L // 60 seconds

    private val refreshRunnable = object : Runnable {
        override fun run() {
            fetchAllData()
            refreshHandler.postDelayed(this, refreshInterval)
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
        setupLocationBar()
        setupButtons()

        // Start auto-refresh loop
        refreshHandler.post(refreshRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshHandler.removeCallbacks(refreshRunnable)
        scope.cancel()
        httpClient.dispatcher.executorService.shutdown()
    }

    private fun setupHeader() {
        binding.tvHeader.text =
            "╔══════════════════════════════════════╗\n" +
            "║   COSMIC WEATHER TERMINAL  v1.0      ║\n" +
            "╚══════════════════════════════════════╝"
    }

    private fun setupLocationBar() {
        binding.tvLocation.text = "LOC: $currentLocationName"
    }

    private fun setupButtons() {
        binding.tvGpsBtn.setOnClickListener { requestGpsLocation() }
        binding.tvCityBtn.setOnClickListener {
            LocationPickerDialog(this) { city ->
                currentLat = city.lat
                currentLon = city.lon
                currentLocationName = city.name
                binding.tvLocation.text = "LOC: $currentLocationName"
                fetchAllData()
            }.show()
        }
    }

    private fun fetchAllData() {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        binding.tvStatus.text = "SYS: FETCHING DATA... [$timestamp]"

        scope.launch {
            // Fetch all three data sources concurrently
            var spaceResult: Result<SpaceWeatherData>? = null
            var weatherResult: Result<WeatherData>? = null
            var aqResult: Result<AirQualityData>? = null

            val job1 = launch {
                spaceResult = runCatching { spaceWeatherApi.fetchSpaceWeather() }
            }
            val job2 = launch {
                weatherResult = runCatching { weatherApi.fetchWeather(currentLat, currentLon) }
            }
            val job3 = launch {
                aqResult = runCatching { airQualityApi.fetchAirQuality(currentLat, currentLon) }
            }

            job1.join(); job2.join(); job3.join()

            withContext(Dispatchers.Main) {
                spaceResult?.fold(
                    onSuccess = { binding.tvSpaceWeather.text = formatSpaceWeather(it) },
                    onFailure = { binding.tvSpaceWeather.text = formatError("SPACE WEATHER", it.message) }
                )
                weatherResult?.fold(
                    onSuccess = { binding.tvWeather.text = formatWeather(it) },
                    onFailure = { binding.tvWeather.text = formatError("WEATHER FORECAST", it.message) }
                )
                aqResult?.fold(
                    onSuccess = { binding.tvAirQuality.text = formatAirQuality(it) },
                    onFailure = { binding.tvAirQuality.text = formatError("AIR QUALITY", it.message) }
                )

                val done = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                binding.tvStatus.text = "SYS: LAST UPDATE $done | NEXT IN 60s"
            }
        }
    }

    // ── Formatting helpers ──────────────────────────────────────────────────

    private val W = 40  // box inner width

    private fun boxLine(content: String): String {
        val padded = content.padEnd(W - 2)
        return "║  ${padded.take(W - 2)}  ║"
    }

    private fun formatSpaceWeather(d: SpaceWeatherData): String {
        val kpBar = buildKpBar(d.kpIndex)
        val tempStr = "%.2e".format(d.solarWindTemp)
        return buildString {
            appendLine("╔${"═".repeat(W)}╗")
            appendLine("║  ${"COSMIC RAY / SPACE WEATHER".padEnd(W - 2)}  ║")
            appendLine("╠${"═".repeat(W)}╣")
            appendLine(boxLine("KP INDEX   : ${"%.1f".format(d.kpIndex)}  [${d.kpLevel}]"))
            appendLine(boxLine("KP LEVEL   : $kpBar"))
            appendLine(boxLine("SOLAR WIND : ${"%.0f".format(d.solarWindSpeed)} km/s"))
            appendLine(boxLine("SW DENSITY : ${"%.2f".format(d.solarWindDensity)} p/cm\u00B3"))
            appendLine(boxLine("SW TEMP    : $tempStr K"))
            appendLine(boxLine("KP TIME    : ${d.kpTimestamp}"))
            append("╚${"═".repeat(W)}╝")
        }
    }

    private fun buildKpBar(kp: Double): String {
        val filled = (kp / 9.0 * 10).toInt().coerceIn(0, 10)
        return "[" + "█".repeat(filled) + "░".repeat(10 - filled) + "]"
    }

    private fun formatWeather(d: WeatherData): String {
        return buildString {
            appendLine()
            appendLine("╔${"═".repeat(W)}╗")
            appendLine("║  ${"WEATHER FORECAST".padEnd(W - 2)}  ║")
            appendLine("╠${"═".repeat(W)}╣")
            appendLine(boxLine("LOCATION  : $currentLocationName"))
            appendLine(boxLine("TEMP      : ${"%.1f".format(d.temperature)} \u00B0C"))
            appendLine(boxLine("HUMIDITY  : ${d.humidity}%"))
            appendLine(boxLine("WIND      : ${"%.1f".format(d.windSpeed)} m/s"))
            appendLine(boxLine("PRECIP    : ${"%.1f".format(d.precipitation)} mm"))
            appendLine(boxLine("CONDITION : ${d.condition}"))
            appendLine(boxLine("WX CODE   : ${d.weatherCode}"))
            appendLine(boxLine("TIME      : ${d.timestamp}"))
            append("╚${"═".repeat(W)}╝")
        }
    }

    private fun formatAirQuality(d: AirQualityData): String {
        val aqiColor = when {
            d.aqi <= 50  -> "▓"
            d.aqi <= 100 -> "▒"
            else         -> "░"
        }
        return buildString {
            appendLine()
            appendLine("╔${"═".repeat(W)}╗")
            appendLine("║  ${"AIR QUALITY".padEnd(W - 2)}  ║")
            appendLine("╠${"═".repeat(W)}╣")
            appendLine(boxLine("AQI (US)  : ${d.aqi}  [${d.aqiLevel}]  $aqiColor"))
            appendLine(boxLine("PM2.5     : ${"%.1f".format(d.pm25)} \u03BCg/m\u00B3"))
            appendLine(boxLine("PM10      : ${"%.1f".format(d.pm10)} \u03BCg/m\u00B3"))
            appendLine(boxLine("UPDATED   : ${d.timestamp}"))
            append("╚${"═".repeat(W)}╝")
        }
    }

    private fun formatError(section: String, message: String?): String {
        val title = section.padEnd(W - 2)
        val err = ("ERR: ${message ?: "Unknown error"}").take(W - 2).padEnd(W - 2)
        return buildString {
            appendLine()
            appendLine("╔${"═".repeat(W)}╗")
            appendLine("║  $title  ║")
            appendLine("╠${"═".repeat(W)}╣")
            appendLine("║  $err  ║")
            append("╚${"═".repeat(W)}╝")
        }
    }

    // ── GPS Location ────────────────────────────────────────────────────────

    private fun requestGpsLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            getGpsLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getGpsLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getGpsLocation() {
        binding.tvStatus.text = "SYS: ACQUIRING GPS SIGNAL..."
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLat = location.latitude
                    currentLon = location.longitude
                    currentLocationName = "GPS (${"%.4f".format(currentLat)}, ${"%.4f".format(currentLon)})"
                    binding.tvLocation.text = "LOC: $currentLocationName"
                    fetchAllData()
                } else {
                    binding.tvStatus.text = "SYS: GPS SIGNAL NOT AVAILABLE"
                }
            }
            .addOnFailureListener { e ->
                binding.tvStatus.text = "SYS: GPS ERROR - ${e.message}"
            }
    }
}
