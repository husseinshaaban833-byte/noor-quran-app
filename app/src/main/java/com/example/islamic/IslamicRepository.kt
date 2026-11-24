package com.example.islamic

import android.content.Context
import com.example.audio.RadioStation
import com.example.audio.Reciter
import org.json.JSONArray

data class CityOption(
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val tz: Double
)

data class DhikrItem(
    val id: String,
    val category: String,
    val title: String,
    val text: String,
    val repeat: Int,
    val source: String
)

class IslamicRepository(private val context: Context) {
    private var cachedReciters: List<Reciter>? = null
    private var cachedRadios: List<RadioStation>? = null
    private var cachedCities: List<CityOption>? = null
    private var cachedAdhkar: List<DhikrItem>? = null

    fun getReciters(): List<Reciter> {
        cachedReciters?.let { return it }
        val raw = context.assets.open("quran/reciters.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(raw)
        val list = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Reciter(
                id = o.getString("id"),
                name = o.getString("name"),
                style = o.getString("style"),
                ayahBaseUrl = o.getString("ayahBaseUrl"),
                surahBaseUrl = o.getString("surahBaseUrl")
            )
        }
        cachedReciters = list
        return list
    }

    fun getRadioStations(): List<RadioStation> {
        cachedRadios?.let { return it }
        val raw = context.assets.open("quran/radio_stations.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(raw)
        val list = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            RadioStation(
                id = o.getInt("id"),
                name = o.getString("name"),
                category = o.getString("category"),
                url = o.getString("url")
            )
        }
        cachedRadios = list
        return list
    }

    fun getCities(): List<CityOption> {
        cachedCities?.let { return it }
        val raw = context.assets.open("quran/cities.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(raw)
        val list = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            CityOption(
                name = o.getString("name"),
                country = o.getString("country"),
                lat = o.getDouble("lat"),
                lon = o.getDouble("lon"),
                tz = o.getDouble("tz")
            )
        }
        cachedCities = list
        return list
    }

    fun getAdhkar(): List<DhikrItem> {
        cachedAdhkar?.let { return it }
        val raw = context.assets.open("quran/adhkar_hisn.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(raw)
        val list = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            DhikrItem(
                id = o.getString("id"),
                category = o.getString("category"),
                title = o.getString("title"),
                text = o.getString("text"),
                repeat = o.getInt("repeat"),
                source = o.getString("source")
            )
        }
        cachedAdhkar = list
        return list
    }
}
