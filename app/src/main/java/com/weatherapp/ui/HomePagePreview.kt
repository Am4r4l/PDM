package com.weatherapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.weatherapp.R
import com.weatherapp.model.Forecast
import com.weatherapp.model.Weather
import java.text.DecimalFormat

@Composable
fun HomePage(modifier: Modifier = Modifier, viewModel: MainViewModel) {
    val cityName = viewModel.city
    if (cityName == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Blue)
                .wrapContentSize(Alignment.Center)
        ) {
            Text(
                text = "Selecione uma cidade!",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center,
                fontSize = 28.sp
            )
        }
    } else {
        val cities by viewModel.cities.collectAsStateWithLifecycle()
        val weatherMap by viewModel.weather.collectAsStateWithLifecycle(emptyMap())
        val forecastMap by viewModel.forecast.collectAsStateWithLifecycle(emptyMap())

        val city = cities[cityName]
        val weather = weatherMap[cityName] ?: Weather.LOADING
        val forecasts = forecastMap[cityName]

        LaunchedEffect(cityName) {
            viewModel.loadWeather(cityName)
            viewModel.loadForecast(cityName)
        }

        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = weather.imgUrl,
                    modifier = Modifier.size(100.dp),
                    error = painterResource(id = R.drawable.loading),
                    contentDescription = "Weather Icon"
                )
                Spacer(modifier = Modifier.size(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = cityName, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.size(8.dp))
                        val icon = if (city?.isMonitored == true) Icons.Filled.Notifications
                        else Icons.Outlined.Notifications
                        Icon(
                            imageVector = icon,
                            contentDescription = "Monitorada?",
                            modifier = Modifier.size(24.dp).clickable {
                                city?.let {
                                    viewModel.update(it.copy(isMonitored = !it.isMonitored))
                                }
                            }
                        )
                    }
                    Text(text = weather.desc, fontSize = 20.sp)
                    Text(text = "Temp: ${weather.temp}℃", fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.size(24.dp))

            forecasts?.let { list ->
                LazyColumn {
                    items(items = list) { forecast ->
                        ForecastItem(forecast, onClick = { })
                    }
                }
            }
        }
    }
}

@Composable
fun ForecastItem(
    forecast: Forecast,
    modifier: Modifier = Modifier,
    onClick: (Forecast) -> Unit
) {
    val format = DecimalFormat("#.0")
    val tempMin = format.format(forecast.tempMin)
    val tempMax = format.format(forecast.tempMax)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = { onClick(forecast) }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = forecast.imgUrl,
            modifier = Modifier.size(60.dp),
            error = painterResource(id = R.drawable.loading),
            contentDescription = "Forecast Icon"
        )
        Spacer(modifier = Modifier.size(16.dp))
        Column {
            Text(text = forecast.weather, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Row {
                Text(text = forecast.date, fontSize = 16.sp)
                Spacer(modifier = Modifier.size(12.dp))
                Text(text = "Min: $tempMin℃", fontSize = 14.sp)
                Spacer(modifier = Modifier.size(12.dp))
                Text(text = "Max: $tempMax℃", fontSize = 14.sp)
            }
        }
    }
}
