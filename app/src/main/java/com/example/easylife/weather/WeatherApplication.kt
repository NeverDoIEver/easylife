package com.example.easylife.weather

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class WeatherApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        scheduleWeatherUpdates()
    }

    private fun scheduleWeatherUpdates() {
        val request =
            PeriodicWorkRequestBuilder<WeatherUpdateWorker>(
                1,
                TimeUnit.HOURS,
            ).build()

        WorkManager
            .getInstance(this)
            .enqueueUniquePeriodicWork(
                "hourly_weather_update",
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
    }
}
