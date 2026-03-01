# Cosmic Weather Forecast

An Android app for real-time cosmic/space weather forecasting with a hacker-style terminal UI.

## Features

- **Space Weather**: NOAA/SWPC Kp geomagnetic index, solar wind speed, density, and temperature
- **Weather Forecast**: Current temperature, humidity, wind speed, and conditions (Open-Meteo)
- **Air Quality**: US AQI, PM2.5, and PM10 (Open-Meteo Air Quality)
- **Terminal UI**: Black background with green monospace text, ASCII box borders
- **Default Location**: Kobe City, Japan (34.6913°N, 135.1830°E)
- **Location Selection**: GPS current location or choose from 10 Japanese cities
- **Auto-Refresh**: Data updates every 60 seconds

## APIs Used (all free, no API key required)

| Data | API |
|------|-----|
| Space Weather | [NOAA SWPC](https://www.swpc.noaa.gov/) |
| Weather | [Open-Meteo](https://open-meteo.com/) |
| Air Quality | [Open-Meteo Air Quality](https://open-meteo.com/en/docs/air-quality-api) |

## Installation

A pre-built APK is available in the [`release/`](release/) directory:

```
release/CosmicWeather-debug.apk
```

Install on an Android device (Android 8.0+):
```bash
adb install release/CosmicWeather-debug.apk
```

Or transfer the APK to your device and open it (enable "Install from unknown sources" in Settings).

## Build from Source

Requirements:
- Android SDK with API 34 platform and build-tools 34.0.0
- Kotlin compiler (`kotlinc`)

```bash
export ANDROID_SDK_ROOT=/path/to/android-sdk
./build_apk.sh
```

The APK will be created at `release/CosmicWeather-debug.apk`.

## Project Structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/cosmicweather/forecast/
│   ├── MainActivity.kt          # Main activity, terminal UI, location handling
│   ├── Cities.kt                # City list with coordinates
│   └── api/
│       ├── SpaceWeatherApi.kt   # NOAA SWPC space weather data
│       ├── WeatherApi.kt        # Open-Meteo weather data
│       └── AirQualityApi.kt     # Open-Meteo air quality data
└── res/
    ├── layout/activity_main.xml # Terminal-style layout
    ├── values/colors.xml        # Terminal color scheme
    ├── values/themes.xml        # Dark terminal theme
    └── xml/network_security_config.xml
```

## UI Preview

```
LOC: Kobe, Japan                         [GPS] [CITY]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
╔══════════════════════════════════════════════╗
║  COSMIC RAY / SPACE WEATHER                  ║
╠══════════════════════════════════════════════╣
║  KP INDEX   : 2.3  [UNSETTLED]               ║
║  KP LEVEL   : [██░░░░░░░░]                   ║
║  SOLAR WIND : 412 km/s                       ║
║  SW DENSITY : 5.1 p/cm³                     ║
║  SW TEMP    : 8.45e+04 K                     ║
║  TIMESTAMP  : 2024-01-15 12:00:00 UTC        ║
╚══════════════════════════════════════════════╝

╔══════════════════════════════════════════════╗
║  WEATHER FORECAST                            ║
╠══════════════════════════════════════════════╣
║  LOCATION   : Kobe, Japan                   ║
║  TEMPERATURE: 18.2 °C                       ║
║  HUMIDITY   : 68%                           ║
║  WIND SPEED : 3.8 m/s                       ║
║  PRECIP     : 0.0 mm                        ║
║  CONDITION  : Partly Cloudy                 ║
╚══════════════════════════════════════════════╝

╔══════════════════════════════════════════════╗
║  AIR QUALITY                                 ║
╠══════════════════════════════════════════════╣
║  AQI (US)   : 35 [GOOD]                     ║
║  PM2.5      : 6.2 μg/m³                    ║
║  PM10       : 12.5 μg/m³                   ║
╚══════════════════════════════════════════════╝
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
SYS: LAST UPDATE 2024-01-15 21:00:00 UTC+9
```
