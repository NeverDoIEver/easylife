package com.example.easylife.weather

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current")
        current: String = CURRENT_VARIABLES,
        @Query("hourly")
        hourly: String = HOURLY_VARIABLES,
        @Query("daily")
        daily: String = DAILY_VARIABLES,
        @Query("timezone")
        timezone: String = "auto",
        @Query("forecast_days")
        forecastDays: Int = 7,
        @Query("temperature_unit")
        temperatureUnit: String = "celsius",
        @Query("wind_speed_unit")
        windSpeedUnit: String = "kmh",
        @Query("precipitation_unit")
        precipitationUnit: String = "mm",
    ): WeatherResponse

    companion object {
        private const val CURRENT_VARIABLES =
            "temperature_2m," +
                "relative_humidity_2m," +
                "apparent_temperature," +
                "precipitation," +
                "weather_code," +
                "wind_speed_10m"

        private const val HOURLY_VARIABLES =
            "temperature_2m," +
                "precipitation_probability," +
                "precipitation," +
                "weather_code"

        private const val DAILY_VARIABLES =
            "temperature_2m_max," +
                "temperature_2m_min," +
                "precipitation_sum," +
                "weather_code," +
                "sunrise," +
                "sunset"
    }
}
