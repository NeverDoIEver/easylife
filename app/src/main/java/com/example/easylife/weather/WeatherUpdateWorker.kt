package com.example.easylife.weather

import android.content.Context
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(
        appContext,
        workerParams,
    ) {
    private val response = WeatherRepository()

    private val preferences =
        appContext.getSharedPreferences(
            "weather_preferences",
            Context.MODE_PRIVATE,
        )

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val locationHelper =
                    LocationHelper(applicationContext)

                /*
                 * Try to obtain the current location.
                 *
                 * This requires background location permission
                 * when executed while the app is in the background.
                 */
                val location =
                    if (
                        locationHelper.hasLocationPermission() &&
                        locationHelper.hasBackgroundLocationPermission()
                    ) {
                        locationHelper.getCurrentLocation()
                    } else {
                        null
                    }

                val latitude =
                    location?.latitude
                        ?: preferences
                            .getString("latitude", null)
                            ?.toDoubleOrNull()

                val longitude =
                    location?.longitude
                        ?: preferences
                            .getString("longitude", null)
                            ?.toDoubleOrNull()

                if (latitude == null || longitude == null) {
                    return@withContext Result.success()
                }

                /*
                 * Save latest location.
                 */
                preferences
                    .edit {
                        putString("latitude", latitude.toString())
                            .putString("longitude", longitude.toString())
                    }

                /*
                 * Request weather.
                 */
                val weather =
                    response.getWeather(
                        latitude,
                        longitude,
                    )

                /*
                 * Save a few values locally.
                 *
                 * The UI can use these if desired.
                 */
                preferences
                    .edit {
                        putString(
                            "temperature",
                            weather.current
                                ?.temperature_2m
                                ?.toString(),
                        ).putString(
                            "weather_code",
                            weather.current
                                ?.weather_code
                                ?.toString(),
                        ).putString(
                            "weather_time",
                            weather.current?.time,
                        )
                    }

                Result.success()
            } catch (exception: Exception) {
                exception.printStackTrace()

                Result.retry()
            }
        }
    }
}
