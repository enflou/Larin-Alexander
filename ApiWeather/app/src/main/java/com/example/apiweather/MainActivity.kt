package com.example.apiweather

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var cityEditText: EditText
    private lateinit var getWeatherButton: Button
    private lateinit var weatherTextView: TextView
    private lateinit var progressBar: ProgressBar

    private val weatherService = WeatherService.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupClickListener()
    }

    private fun initViews() {
        cityEditText = findViewById(R.id.cityEditText)
        getWeatherButton = findViewById(R.id.getWeatherButton)
        weatherTextView = findViewById(R.id.weatherTextView)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListener() {
        getWeatherButton.setOnClickListener {
            val city = cityEditText.text.toString().trim()
            if (city.isNotEmpty()) {
                getWeather(city)
            } else {
                Toast.makeText(this, "Введите название города", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getWeather(city: String) {
        progressBar.visibility = ProgressBar.VISIBLE
        getWeatherButton.isEnabled = false

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val weatherResponse = withContext(Dispatchers.IO) {
                    weatherService.getWeather(city, WeatherService.API_KEY)
                }

                displayWeather(weatherResponse)

            } catch (e: Exception) {
                weatherTextView.text = "Ошибка: ${e.message}"
                Toast.makeText(this@MainActivity, "Ошибка получения данных", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                progressBar.visibility = ProgressBar.GONE
                getWeatherButton.isEnabled = true
            }
        }
    }

    private fun displayWeather(weather: WeatherResponse) {
        val temperature = weather.main.temperature
        val humidity = weather.main.humidity
        val pressure = weather.main.pressure
        val description = weather.weather.firstOrNull()?.description ?: "Нет данных"

        val weatherInfo = """
            Город: ${weather.cityName}
            Температура: ${temperature}°C
            Влажность: ${humidity}%
            Давление: ${pressure} hPa
            Описание: ${description.replaceFirstChar { it.uppercase() }}
        """.trimIndent()

        weatherTextView.text = weatherInfo
    }
}