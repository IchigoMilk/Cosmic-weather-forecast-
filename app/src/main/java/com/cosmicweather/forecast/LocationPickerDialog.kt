package com.cosmicweather.forecast

import android.app.AlertDialog
import android.content.Context

data class City(val name: String, val lat: Double, val lon: Double)

class LocationPickerDialog(
    private val context: Context,
    private val onCitySelected: (City) -> Unit
) {

    private val cities = listOf(
        City("Kobe, Japan",      34.6913, 135.1830),
        City("Tokyo, Japan",     35.6762, 139.6503),
        City("Osaka, Japan",     34.6937, 135.5023),
        City("Kyoto, Japan",     35.0116, 135.7681),
        City("Nagoya, Japan",    35.1815, 136.9066),
        City("Sapporo, Japan",   43.0618, 141.3545),
        City("Fukuoka, Japan",   33.5904, 130.4017),
        City("Yokohama, Japan",  35.4437, 139.6380),
        City("Sendai, Japan",    38.2688, 140.8721),
        City("Hiroshima, Japan", 34.3853, 132.4553)
    )

    fun show() {
        val names = cities.map { it.name }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle("SELECT LOCATION")
            .setItems(names) { _, which ->
                onCitySelected(cities[which])
            }
            .setNegativeButton("CANCEL", null)
            .create()
            .apply {
                // Style the dialog for terminal look
                setOnShowListener {
                    window?.decorView?.setBackgroundResource(android.R.color.black)
                }
            }
            .show()
    }
}
