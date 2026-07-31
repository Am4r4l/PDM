package com.weatherapp.ui

import android.annotation.SuppressLint
import androidx.browser.browseractions.BrowserServiceFileProvider.loadBitmap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.weatherapp.api.WeatherService
import com.weatherapp.api.toForecast
import com.weatherapp.api.toWeather
import com.weatherapp.db.fb.FBCity
import com.weatherapp.db.fb.FBDatabase
import com.weatherapp.db.fb.FBUser
import com.weatherapp.db.fb.toFBCity
import com.weatherapp.model.City
import com.weatherapp.model.Forecast
import com.weatherapp.model.User
import com.weatherapp.model.Weather
import com.weatherapp.monitor.ForecastMonitor
import com.weatherapp.repo.Repository
import com.weatherapp.ui.nav.Route

class MainViewModel (private val db: Repository, private val service : WeatherService, private val monitor: ForecastMonitor) : ViewModel(),
    Repository.Listener  {
    private val _cities = mutableStateMapOf<String, City>()
    val cities : List<City>
        get() = _cities.values.toList().sortedBy { it.name }
    private val _weather = mutableStateMapOf<String, Weather>()
    private val _user = mutableStateOf<User?> (null)
    val user : User?
        get() = _user.value

    private var _city = mutableStateOf<String?>(null)
    var city: String?
        get() = _city.value
        set(tmp) { _city.value = tmp }

    private var _page = mutableStateOf<Route>(Route.Home)
    var page: Route
        get() = _page.value
        set(tmp) { _page.value = tmp }

    private val _forecast = mutableStateMapOf<String, List<Forecast>?>()


    init {
        db.setListener(this)
    }
    fun remove(city: City) {
        db.remove(city)
    }
    fun add(name: String, location : LatLng? = null) {
        db.add(City(name = name, location = location))
    }
    override fun onUserLoaded(user: User) {
        _user.value = user
    }
    override fun onUserSignOut() {
        _user.value = null
        monitor.cancelAll()
    }
    override fun onCityAdded(city: City) {
        _cities[city.name!!] = city
        monitor.updateCity(city)
    }
    override fun onCityUpdated(city: City) {
        _cities.remove(city.name)
        _cities[city.name!!] = city
        monitor.updateCity(city)
    }
    override fun onCityRemoved(city: City) {
        _cities.remove(city.name)
        monitor.cancelCity(city)
    }

    fun addCity(name: String) {
        service.getLocation(name) { lat, lng ->
            if (lat != null && lng != null) {
                db.add(City(name=name, location=LatLng(lat, lng)))
            }
        }
    }
    fun addCity(location: LatLng) {
        service.getName(location.latitude, location.longitude) { name ->
            if (name != null) {
                db.add(City(name = name, location = location))
            }
        }
    }

    fun update(city: City) {
        db.update(city)
    }

    fun weather (name: String) = _weather.getOrPut(name) {
        loadWeather(name)
        Weather.LOADING // retorno
    }

    private fun loadWeather(name: String) {
        service.getWeather(name) { apiWeather ->
            apiWeather?.let {
                _weather[name] = apiWeather.toWeather()
                loadBitmap(name)
            }
        }
    }

    private fun loadBitmap(name: String) {
        _weather[name]?.let { weather ->
            service.getBitmap(weather.imgUrl) { bitmap ->
                _weather[name] = weather.copy(bitmap = bitmap)
            }
        }
    }


    fun forecast (name: String) = _forecast.getOrPut(name) {
        loadForecast(name)
        emptyList() // return
    }

    private fun loadForecast(name: String) {
        service.getForecast(name) { apiForecast ->
            apiForecast?.let {
                _forecast[name] = apiForecast.toForecast()
            }
        }
    }
}

class MainViewModelFactory(private val db : Repository, private val service : WeatherService, private val monitor: ForecastMonitor) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(db, service, monitor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
