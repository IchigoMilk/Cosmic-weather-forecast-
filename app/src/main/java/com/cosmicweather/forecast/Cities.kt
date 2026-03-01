package com.cosmicweather.forecast

data class CityLocation(
    val name: String,
    val lat: Double,
    val lon: Double
)

object Cities {
    val list = listOf(
        CityLocation("Kobe, Japan",      34.6913, 135.1830),
        CityLocation("Tokyo, Japan",     35.6762, 139.6503),
        CityLocation("Osaka, Japan",     34.6937, 135.5023),
        CityLocation("Kyoto, Japan",     35.0116, 135.7681),
        CityLocation("Nagoya, Japan",    35.1815, 136.9066),
        CityLocation("Sapporo, Japan",   43.0642, 141.3469),
        CityLocation("Fukuoka, Japan",   33.5904, 130.4017),
        CityLocation("Yokohama, Japan",  35.4437, 139.6380),
        CityLocation("Sendai, Japan",    38.2682, 140.8694),
        CityLocation("Hiroshima, Japan", 34.3853, 132.4553)
    )

    val default = list[0]  // Kobe
}
