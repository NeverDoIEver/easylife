package com.example.easylife.weather

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRepository {
    private val api: OpenMeteoApi by lazy {

        Retrofit
            .Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(
                GsonConverterFactory.create(),
            ).build()
            .create(OpenMeteoApi::class.java)
    }

    suspend fun getWeather(
        latitude: Double,
        longitude: Double,
    ): WeatherResponse =
        api.getWeather(
            latitude = latitude,
            longitude = longitude,
        )
}
