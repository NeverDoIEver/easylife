package com.example.openmeteoweather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.transition.TransitionInflater
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.alpha
import androidx.lifecycle.lifecycleScope
import com.example.easylife.weather.LocationHelper
import com.example.easylife.weather.WeatherRepository
import com.example.easylife.weather.WeatherResponse
import com.example.weather.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class SplashScreenActivity : AppCompatActivity() {
    private lateinit var temperatureText: TextView
    private lateinit var weatherText: TextView
    private lateinit var feelsLikeText: TextView

    private lateinit var locationText: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var weatherImage: ImageView

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

    private fun changeImageFromWeatherCode(code: Int?): Int =
        when (code) {
            0, 1 -> R.drawable.clear

            2, 3 -> R.drawable.clear

            // change to cloudy

            45, 48 -> R.drawable.cloudy

            51, 53, 55,
            56, 57,
            61, 63, 65,
            66, 67,
            80, 81, 82,
            -> R.drawable.rain

            71, 73, 75,
            77,
            85, 86,
            -> R.drawable.snow

            95, 96, 99 -> R.drawable.rain

            // change to thunderstorm

            else -> R.drawable.clear
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.weather)

        // Wait for 2 seconds, then open MainActivity
        window.decorView.postDelayed({
            val intent =
                Intent(
                    this,
                    MainActivity::class.java,
                )

            startActivity(intent)

            // Fade from SplashScreenActivity -> MainActivity
            overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
            )

            // Prevent going back to the splash screen
            finish()
        }, 10_000)
        locationHelper = LocationHelper(this)

        temperatureText = findViewById(R.id.temperatureText)
        weatherText = findViewById(R.id.weatherText)
        feelsLikeText = findViewById(R.id.feelsLikeText)
        locationText = findViewById(R.id.locationText)
        progressBar = findViewById(R.id.progressBar)

        weatherImage = findViewById(R.id.imageView)

        if (hasLocationPermission()) {
            loadWeatherFromCurrentLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun initializeViews() {
        temperatureText = findViewById(R.id.temperatureText)
        weatherText = findViewById(R.id.weatherText)
        feelsLikeText = findViewById(R.id.feelsLikeText)
        locationText = findViewById(R.id.locationText)
        progressBar = findViewById(R.id.progressBar)
        weatherImage = findViewById(R.id.imageView)
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
                val location = locationHelper.getCurrentLocation()

                if (location == null) {
                    Toast
                        .makeText(
                            this@SplashScreenActivity,
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

    @SuppressLint("SetTextI18n")
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

        weatherImage.setImageResource(
            changeImageFromWeatherCode(current.weather_code),
        )

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
                        this@SplashScreenActivity,
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

    @SuppressLint("UseKtx")
    private fun saveLocation(
        latitude: Double,
        longitude: Double,
    ) {
        getSharedPreferences(
            "weather_preferences",
            MODE_PRIVATE,
        ).edit {
            putString(
                "latitude",
                latitude.toString(),
            ).putString(
                "longitude",
                longitude.toString(),
            )
        }
    }

    private fun showLoading(loading: Boolean) {
        progressBar.visibility =
            if (loading) {
                ProgressBar.VISIBLE
            } else {
                ProgressBar.GONE
            }
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
