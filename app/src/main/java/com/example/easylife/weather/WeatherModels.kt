package com.example.easylife.weather

data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val timezone_abbreviation: String?,
    val current: CurrentWeather?,
    val current_units: CurrentUnits?,
    val hourly: HourlyWeather?,
    val hourly_units: HourlyUnits?,
    val daily: DailyWeather?,
    val daily_units: DailyUnits?,
)

data class CurrentWeather(
    val time: String?,
    val temperature_2m: Double?,
    val relative_humidity_2m: Double?,
    val apparent_temperature: Double?,
    val precipitation: Double?,
    val weather_code: Int?,
    val wind_speed_10m: Double?,
)

data class CurrentUnits(
    val temperature_2m: String?,
    val relative_humidity_2m: String?,
    val apparent_temperature: String?,
    val precipitation: String?,
    val wind_speed_10m: String?,
)

data class HourlyWeather(
    val time: List<String>?,
    val temperature_2m: List<Double>?,
    val precipitation_probability: List<Double>?,
    val precipitation: List<Double>?,
    val weather_code: List<Int>?,
)

data class HourlyUnits(
    val temperature_2m: String?,
    val precipitation_probability: String?,
    val precipitation: String?,
)

data class DailyWeather(
    val time: List<String>?,
    val temperature_2m_max: List<Double>?,
    val temperature_2m_min: List<Double>?,
    val precipitation_sum: List<Double>?,
    val weather_code: List<Int>?,
    val sunrise: List<String>?,
    val sunset: List<String>?,
)

data class DailyUnits(
    val temperature_2m_max: String?,
    val temperature_2m_min: String?,
    val precipitation_sum: String?,
)
