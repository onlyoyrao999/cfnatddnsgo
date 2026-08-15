package com.example.data.network

import com.example.data.model.LocationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

object CloudflareLocations {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val cachedLocations = mutableMapOf<String, LocationInfo>()

    // Built-in static fallback dataset for major Cloudflare edge centers worldwide
    val DEFAULT_LOCATIONS = mapOf(
        "HKG" to LocationInfo("HKG", 22.308, 113.918, "HK", "Asia", "Hong Kong"),
        "SJC" to LocationInfo("SJC", 37.362, -121.929, "US", "North America", "San Jose"),
        "LAX" to LocationInfo("LAX", 33.942, -118.408, "US", "North America", "Los Angeles"),
        "NRT" to LocationInfo("NRT", 35.764, 140.386, "JP", "Asia", "Tokyo Narita"),
        "HND" to LocationInfo("HND", 35.549, 139.779, "JP", "Asia", "Tokyo Haneda"),
        "KIX" to LocationInfo("KIX", 34.434, 135.244, "JP", "Asia", "Osaka"),
        "SIN" to LocationInfo("SIN", 1.364, 103.991, "SG", "Asia", "Singapore"),
        "TPE" to LocationInfo("TPE", 25.077, 121.232, "TW", "Asia", "Taipei"),
        "KUL" to LocationInfo("KUL", 2.745, 101.709, "MY", "Asia", "Kuala Lumpur"),
        "CGK" to LocationInfo("CGK", -6.125, 106.655, "ID", "Asia", "Jakarta"),
        "BKK" to LocationInfo("BKK", 13.681, 100.747, "TH", "Asia", "Bangkok"),
        "ICN" to LocationInfo("ICN", 37.460, 126.440, "KR", "Asia", "Seoul Incheon"),
        "SFO" to LocationInfo("SFO", 37.618, -122.374, "US", "North America", "San Francisco"),
        "SEA" to LocationInfo("SEA", 47.450, -122.311, "US", "North America", "Seattle"),
        "ORD" to LocationInfo("ORD", 41.974, -87.907, "US", "North America", "Chicago"),
        "JFK" to LocationInfo("JFK", 40.641, -73.778, "US", "North America", "New York JFK"),
        "EWR" to LocationInfo("EWR", 40.692, -74.168, "US", "North America", "Newark"),
        "IAD" to LocationInfo("IAD", 38.953, -77.456, "US", "North America", "Washington D.C."),
        "MIA" to LocationInfo("MIA", 25.795, -80.287, "US", "North America", "Miami"),
        "FRA" to LocationInfo("FRA", 50.037, 8.562, "DE", "Europe", "Frankfurt"),
        "LHR" to LocationInfo("LHR", 51.470, -0.454, "GB", "Europe", "London Heathrow"),
        "CDG" to LocationInfo("CDG", 49.009, 2.547, "FR", "Europe", "Paris CDG"),
        "AMS" to LocationInfo("AMS", 52.310, 4.768, "NL", "Europe", "Amsterdam"),
        "SYD" to LocationInfo("SYD", -33.946, 151.177, "AU", "Oceania", "Sydney"),
        "MEL" to LocationInfo("MEL", -37.669, 144.841, "AU", "Oceania", "Melbourne"),
        "BNE" to LocationInfo("BNE", -27.384, 153.117, "AU", "Oceania", "Brisbane"),
        "GRU" to LocationInfo("GRU", -23.435, -46.473, "BR", "South America", "Sao Paulo"),
        "JNB" to LocationInfo("JNB", -26.139, 28.246, "ZA", "Africa", "Johannesburg")
    )

    init {
        cachedLocations.putAll(DEFAULT_LOCATIONS)
    }

    suspend fun loadLocations(): Map<String, LocationInfo> = withContext(Dispatchers.IO) {
        if (cachedLocations.size > DEFAULT_LOCATIONS.size) return@withContext cachedLocations
        try {
            val request = Request.Builder()
                .url("https://www.baipiao.eu.org/cloudflare/locations")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrEmpty()) {
                        val array = JSONArray(body)
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val iata = obj.optString("iata")
                            if (iata.isNotEmpty()) {
                                val loc = LocationInfo(
                                    iata = iata,
                                    lat = obj.optDouble("lat", 0.0),
                                    lon = obj.optDouble("lon", 0.0),
                                    cca2 = obj.optString("cca2", ""),
                                    region = obj.optString("region", ""),
                                    city = obj.optString("city", iata)
                                )
                                cachedLocations[iata.uppercase()] = loc
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Keep default locations
        }
        return@withContext cachedLocations
    }

    fun getLocation(iata: String): LocationInfo {
        val key = iata.trim().uppercase()
        return cachedLocations[key] ?: LocationInfo(iata = key, city = key)
    }
}
