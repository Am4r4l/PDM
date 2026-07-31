package com.weatherapp.repo

import com.weatherapp.db.fb.FBCity
import com.weatherapp.db.fb.FBDatabase
import com.weatherapp.db.fb.FBUser
import com.weatherapp.db.fb.toFBCity
import com.weatherapp.db.local.LocalDatabase
import com.weatherapp.db.local.toCity
import com.weatherapp.db.local.toLocalCity
import com.weatherapp.model.City
import com.weatherapp.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class Repository (
    private val fbDB: FBDatabase,
    private val localDB : LocalDatabase)
{
    private var ioScope : CoroutineScope = CoroutineScope(Dispatchers.IO)
    private var cityMap = emptyMap<String, City>()
    val cities = localDB.getCities().map {
            list -> list.map { city -> city.toCity() }
    }
    val user = fbDB.user.map { it.toUser() }
    init {
        ioScope.launch {
            // Inicializa cityMap com o conteúdo atual do banco local
            val initialCities = cities.first()
            cityMap = initialCities.associateBy { it.name }

            fbDB.cities.collect { fbCityList ->
                val cityList = fbCityList.map { it.toCity() }
                val nameList = cityList.map { it.name }

                // Cidades que estão no mapa local mas não vieram do Firebase devem ser deletadas
                val deletedCities = cityMap.filter { it.key !in nameList }
                deletedCities.forEach { localDB.delete(it.value.toLocalCity()) }

                // Cidades que estão no Firebase mas não no local são novas
                val newCities = cityList.filter { it.name !in cityMap.keys }
                newCities.forEach { localDB.insert(it.toLocalCity()) }

                // Cidades que estão em ambos devem ser atualizadas
                val updatedCities = cityList.filter { it.name in cityMap.keys }
                updatedCities.forEach { localDB.update(it.toLocalCity()) }

                cityMap = cityList.associateBy { it.name }
            }
        }
    }
    fun add(city: City) = fbDB.add(city.toFBCity())
    fun remove(city: City) = fbDB.remove(city.toFBCity())
    fun update(city: City) = fbDB.update(city.toFBCity())
}
