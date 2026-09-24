package com.example.openmeteoweather

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.easylife.weather.LocationHelper
import com.example.easylife.weather.WeatherRepository
import com.example.easylife.weather.WeatherResponse
import com.example.weather.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var temperatureText: TextView
    private lateinit var weatherText: TextView
    private lateinit var feelsLikeText: TextView
    private lateinit var humidityText: TextView
    private lateinit var windText: TextView
    private lateinit var precipitationText: TextView
    private lateinit var locationText: TextView
    private lateinit var updatedText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var refreshButton: Button

    private lateinit var locationHelper: LocationHelper

    private val repository = WeatherRepository()

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->

            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {

                loadWeatherFromCurrentLocation()
            } else {

                Toast
                    .makeText(
                        this,
                        "Location permission is required for automatic weather.",
                        Toast.LENGTH_LONG,
                    ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        initializeViews()

        locationHelper = LocationHelper(this)

        refreshButton.setOnClickListener {
            loadWeatherFromCurrentLocation()
        }

        if (hasLocationPermission()) {
            loadWeatherFromCurrentLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun initializeViews() {
        temperatureText =
            findViewById(R.id.temperatureText)

        weatherText =
            findViewById(R.id.weatherText)

        feelsLikeText =
            findViewById(R.id.feelsLikeText)

        humidityText =
            findViewById(R.id.humidityText)

        windText =
            findViewById(R.id.windText)

        precipitationText =
            findViewById(R.id.precipitationText)

        locationText =
            findViewById(R.id.locationText)

        updatedText =
            findViewById(R.id.updatedText)

        progressBar =
            findViewById(R.id.progressBar)

        refreshButton =
            findViewById(R.id.refreshButton)
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    private fun loadWeatherFromCurrentLocation() {
        lifecycleScope.launch {
            showLoading(true)

            try {
                val location =
                    locationHelper.getCurrentLocation()

                if (location == null) {
                    Toast
                        .makeText(
                            this@MainActivity,
                            "Unable to determine your location.",
                            Toast.LENGTH_LONG,
                        ).show()

                    return@launch
                }

                saveLocation(
                    location.latitude,
                    location.longitude,
                )

                loadWeather(
                    location.latitude,
                    location.longitude,
                )
            } finally {
                showLoading(false)
            }
        }
    }

    private suspend fun loadWeather(
        latitude: Double,
        longitude: Double,
    ) {
        try {
            val weather =
                repository.getWeather(
                    latitude,
                    longitude,
                )

            updateUi(
                latitude,
                longitude,
                weather,
            )
        } catch (exception: Exception) {
            Toast
                .makeText(
                    this,
                    "Unable to load weather: ${exception.message}",
                    Toast.LENGTH_LONG,
                ).show()
        }
    }

    private fun updateUi(
        latitude: Double,
        longitude: Double,
        weather: WeatherResponse,
    ) {
        val current = weather.current

        if (current == null) {
            return
        }

        val temperature =
            current.temperature_2m ?: Double.NaN

        temperatureText.text =
            if (temperature.isNaN()) {
                "--"
            } else {
                "${temperature.toInt()}°C"
            }

        weatherText.text =
            weatherCodeToDescription(
                current.weather_code,
            )

        feelsLikeText.text =
            "Feels like: ${
                current.apparent_temperature
                    ?.let { "${it.toInt()}°C" }
                    ?: "--"
            }"

        humidityText.text =
            "Humidity: ${
                current.relative_humidity_2m
                    ?.let { "${it.toInt()}%" }
                    ?: "--"
            }"

        windText.text =
            "Wind: ${
                current.wind_speed_10m
                    ?.let { "${it.toInt()} km/h" }
                    ?: "--"
            }"

        precipitationText.text =
            "Precipitation: ${
                current.precipitation
                    ?.let { "$it mm" }
                    ?: "--"
            }"

        updatedText.text =
            "Updated: ${current.time ?: "--"}"

        lifecycleScope.launch {
            val locationName =
                getLocationName(
                    latitude,
                    longitude,
                )

            locationText.text =
                locationName
                    ?: "Lat %.4f, Lon %.4f"
                        .format(
                            Locale.US,
                            latitude,
                            longitude,
                        )
        }
    }

    private suspend fun getLocationName(
        latitude: Double,
        longitude: Double,
    ): String? =
        withContext(Dispatchers.IO) {
            try {
                val geocoder =
                    Geocoder(
                        this@MainActivity,
                        Locale.getDefault(),
                    )

                @Suppress("DEPRECATION")
                val addresses =
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                    )

                addresses
                    ?.firstOrNull()
                    ?.let { address ->

                        address.locality
                            ?: address.subAdminArea
                            ?: address.adminArea
                    }
            } catch (exception: Exception) {
                null
            }
        }

    private fun saveLocation(
        latitude: Double,
        longitude: Double,
    ) {
        getSharedPreferences(
            "weather_preferences",
            MODE_PRIVATE,
        ).edit()
            .putString(
                "latitude",
                latitude.toString(),
            ).putString(
                "longitude",
                longitude.toString(),
            ).apply()
    }

    private fun showLoading(loading: Boolean) {
        progressBar.visibility =
            if (loading) {
                ProgressBar.VISIBLE
            } else {
                ProgressBar.GONE
            }

        refreshButton.isEnabled = !loading
    }

    private fun weatherCodeToDescription(code: Int?): String =
        when (code) {
            0 -> {
                "Clear sky"
            }

            1 -> {
                "Mainly clear"
            }

            2 -> {
                "Partly cloudy"
            }

            3 -> {
                "Overcast"
            }

            45, 48 -> {
                "Fog"
            }

            51, 53, 55 -> {
                "Drizzle"
            }

            56, 57 -> {
                "Freezing drizzle"
            }

            61, 63, 65 -> {
                "Rain"
            }

            66, 67 -> {
                "Freezing rain"
            }

            71, 73, 75 -> {
                "Snow"
            }

            77 -> {
                "Snow grains"
            }

            80, 81, 82 -> {
                "Rain showers"
            }

            85, 86 -> {
                "Snow showers"
            }

            95 -> {
                "Thunderstorm"
            }

            96, 99 -> {
                "Thunderstorm with hail"
            }

            else -> {
                "Unknown"
            }
        }

    private val backgroundLocationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->

            if (granted) {

                Toast
                    .makeText(
                        this,
                        "Automatic location updates enabled.",
                        Toast.LENGTH_LONG,
                    ).show()
            } else {

                Toast
                    .makeText(
                        this,
                        "Background location was not enabled.",
                        Toast.LENGTH_LONG,
                    ).show()
            }
        }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLocationPermissionLauncher.launch(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            )
        }
    }
}
