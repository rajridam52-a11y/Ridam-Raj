package com.example.data.remote

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

@JsonClass(generateAdapter = true)
data class OpenMeteoForecastResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String = "UTC",
    val current: CurrentWeatherDto? = null,
    val hourly: HourlyWeatherDto? = null,
    val daily: DailyWeatherDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
    @Json(name = "temperature_2m") val temperature2m: Double = 0.0,
    @Json(name = "relative_humidity_2m") val humidity: Int = 0,
    @Json(name = "apparent_temperature") val apparentTemperature: Double = 0.0,
    @Json(name = "weather_code") val weatherCode: Int = 0,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double = 0.0,
    @Json(name = "is_day") val isDay: Int = 1
)

@JsonClass(generateAdapter = true)
data class HourlyWeatherDto(
    val time: List<String> = emptyList(),
    @Json(name = "temperature_2m") val temperature2m: List<Double> = emptyList(),
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DailyWeatherDto(
    val time: List<String> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int> = emptyList(),
    @Json(name = "temperature_2m_max") val tempMax: List<Double> = emptyList(),
    @Json(name = "temperature_2m_min") val tempMin: List<Double> = emptyList(),
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList(),
    @Json(name = "uv_index_max") val uvIndexMax: List<Double> = emptyList(),
    @Json(name = "precipitation_probability_max") val precipProbMax: List<Int> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AirQualityResponse(
    val current: CurrentAirQualityDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentAirQualityDto(
    @Json(name = "us_aqi") val usAqi: Int = 0,
    @Json(name = "pm2_5") val pm25: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingResultDto>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResultDto(
    val name: String = "",
    val country: String = "",
    val admin1: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String? = null
)

interface OpenMeteoForecastService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,is_day",
        @Query("hourly") hourly: String = "temperature_2m,precipitation_probability,weather_code",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_probability_max",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") days: Int = 7
    ): OpenMeteoForecastResponse

    @GET("v1/forecast")
    suspend fun getBatchForecastRaw(
        @Query("latitude") latitudes: String,
        @Query("longitude") longitudes: String,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,is_day",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max",
        @Query("timezone") timezone: String = "Asia/Kolkata",
        @Query("forecast_days") days: Int = 1
    ): ResponseBody
}

interface OpenMeteoAirQualityService {
    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "us_aqi,pm2_5"
    ): AirQualityResponse
}

interface OpenMeteoGeocodingService {
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") query: String,
        @Query("count") count: Int = 8,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingResponse
}

data class HourlyForecastItem(
    val timeLabel: String,
    val tempCelsius: Double,
    val rainProbability: Int,
    val weatherCode: Int
)

data class DailyForecastItem(
    val dateLabel: String,
    val minTempC: Double,
    val maxTempC: Double,
    val rainProbability: Int,
    val weatherCode: Int,
    val uvMax: Double
)

data class WeatherSnapshot(
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val tempCelsius: Double,
    val feelsLikeCelsius: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val rainProbabilityPercent: Int,
    val conditionLabel: String,
    val weatherCode: Int,
    val isDay: Boolean,
    val sunrise: String,
    val sunset: String,
    val uvIndex: Double,
    val usAqi: Int,
    val aqiCategory: String,
    val alertBanner: String?,
    val hourly: List<HourlyForecastItem>,
    val daily: List<DailyForecastItem>,
    val fetchedAtMillis: Long,
    val isFromCache: Boolean = false
)

enum class IndiaRegion(val label: String) {
    ALL("All India"),
    NORTH("North"),
    SOUTH("South"),
    WEST("West"),
    EAST("East"),
    CENTRAL("Central"),
    NORTHEAST("Northeast"),
    ISLANDS("Islands")
}

data class IndiaCitySpec(
    val name: String,
    val stateOrUt: String,
    val region: IndiaRegion,
    val latitude: Double,
    val longitude: Double
)

data class IndiaCityWeatherSummary(
    val city: IndiaCitySpec,
    val tempCelsius: Double,
    val feelsLikeCelsius: Double,
    val minTempC: Double,
    val maxTempC: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val rainProbabilityPercent: Int,
    val conditionLabel: String,
    val weatherCode: Int,
    val isDay: Boolean,
    val isFromCache: Boolean = false
)

object IndiaWeatherDirectory {
    val cities: List<IndiaCitySpec> = listOf(
        // North India
        IndiaCitySpec("New Delhi", "Delhi (NCT)", IndiaRegion.NORTH, 28.6139, 77.2090),
        IndiaCitySpec("Chandigarh", "Chandigarh / Punjab / Haryana", IndiaRegion.NORTH, 30.7333, 76.7794),
        IndiaCitySpec("Jaipur", "Rajasthan", IndiaRegion.NORTH, 26.9124, 75.7873),
        IndiaCitySpec("Lucknow", "Uttar Pradesh", IndiaRegion.NORTH, 26.8467, 80.9462),
        IndiaCitySpec("Varanasi", "Uttar Pradesh", IndiaRegion.NORTH, 25.3176, 82.9739),
        IndiaCitySpec("Amritsar", "Punjab", IndiaRegion.NORTH, 31.6340, 74.8723),
        IndiaCitySpec("Shimla", "Himachal Pradesh", IndiaRegion.NORTH, 31.1048, 77.1734),
        IndiaCitySpec("Dehradun", "Uttarakhand", IndiaRegion.NORTH, 30.3165, 78.0322),
        IndiaCitySpec("Srinagar", "Jammu & Kashmir", IndiaRegion.NORTH, 34.0837, 74.7973),
        IndiaCitySpec("Leh", "Ladakh", IndiaRegion.NORTH, 34.1526, 77.5771),

        // West India
        IndiaCitySpec("Mumbai", "Maharashtra", IndiaRegion.WEST, 19.0760, 72.8777),
        IndiaCitySpec("Pune", "Maharashtra", IndiaRegion.WEST, 18.5204, 73.8567),
        IndiaCitySpec("Nagpur", "Maharashtra", IndiaRegion.WEST, 21.1458, 79.0882),
        IndiaCitySpec("Ahmedabad", "Gujarat", IndiaRegion.WEST, 23.0225, 72.5714),
        IndiaCitySpec("Surat", "Gujarat", IndiaRegion.WEST, 21.1702, 72.8311),
        IndiaCitySpec("Panaji", "Goa", IndiaRegion.WEST, 15.4909, 73.8278),
        IndiaCitySpec("Daman", "Dadra & Nagar Haveli and Daman & Diu", IndiaRegion.WEST, 20.3974, 72.8328),

        // South India
        IndiaCitySpec("Bengaluru", "Karnataka", IndiaRegion.SOUTH, 12.9716, 77.5946),
        IndiaCitySpec("Chennai", "Tamil Nadu", IndiaRegion.SOUTH, 13.0827, 80.2707),
        IndiaCitySpec("Hyderabad", "Telangana", IndiaRegion.SOUTH, 17.3850, 78.4867),
        IndiaCitySpec("Amaravati", "Andhra Pradesh", IndiaRegion.SOUTH, 16.5131, 80.5165),
        IndiaCitySpec("Visakhapatnam", "Andhra Pradesh", IndiaRegion.SOUTH, 17.6868, 83.2185),
        IndiaCitySpec("Kochi", "Kerala", IndiaRegion.SOUTH, 9.9312, 76.2673),
        IndiaCitySpec("Thiruvananthapuram", "Kerala", IndiaRegion.SOUTH, 8.5241, 76.9366),
        IndiaCitySpec("Puducherry", "Puducherry", IndiaRegion.SOUTH, 11.9416, 79.8083),

        // East India
        IndiaCitySpec("Kolkata", "West Bengal", IndiaRegion.EAST, 22.5726, 88.3639),
        IndiaCitySpec("Patna", "Bihar", IndiaRegion.EAST, 25.5941, 85.1376),
        IndiaCitySpec("Ranchi", "Jharkhand", IndiaRegion.EAST, 23.3441, 85.3096),
        IndiaCitySpec("Bhubaneswar", "Odisha", IndiaRegion.EAST, 20.2961, 85.8245),
        IndiaCitySpec("Gangtok", "Sikkim", IndiaRegion.EAST, 27.3314, 88.6138),

        // Central India
        IndiaCitySpec("Bhopal", "Madhya Pradesh", IndiaRegion.CENTRAL, 23.2599, 77.4126),
        IndiaCitySpec("Indore", "Madhya Pradesh", IndiaRegion.CENTRAL, 22.7196, 75.8577),
        IndiaCitySpec("Raipur", "Chhattisgarh", IndiaRegion.CENTRAL, 21.2514, 81.6296),

        // Northeast India
        IndiaCitySpec("Guwahati", "Assam", IndiaRegion.NORTHEAST, 26.1445, 91.7362),
        IndiaCitySpec("Shillong", "Meghalaya", IndiaRegion.NORTHEAST, 25.5788, 91.8933),
        IndiaCitySpec("Imphal", "Manipur", IndiaRegion.NORTHEAST, 24.8170, 93.9368),
        IndiaCitySpec("Agartala", "Tripura", IndiaRegion.NORTHEAST, 23.8315, 91.2868),
        IndiaCitySpec("Kohima", "Nagaland", IndiaRegion.NORTHEAST, 25.6751, 94.1086),
        IndiaCitySpec("Aizawl", "Mizoram", IndiaRegion.NORTHEAST, 23.7271, 92.7176),
        IndiaCitySpec("Itanagar", "Arunachal Pradesh", IndiaRegion.NORTHEAST, 27.0844, 93.6053),

        // Island Union Territories
        IndiaCitySpec("Port Blair", "Andaman & Nicobar Islands", IndiaRegion.ISLANDS, 11.6234, 92.7265),
        IndiaCitySpec("Kavaratti", "Lakshadweep", IndiaRegion.ISLANDS, 10.5669, 72.6417)
    )
}

class WeatherRepository(private val context: Context) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val forecastListAdapter = moshi.adapter<List<OpenMeteoForecastResponse>>(
        Types.newParameterizedType(List::class.java, OpenMeteoForecastResponse::class.java)
    )
    private val singleForecastAdapter = moshi.adapter(OpenMeteoForecastResponse::class.java)

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val forecastApi = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OpenMeteoForecastService::class.java)

    private val airQualityApi = Retrofit.Builder()
        .baseUrl("https://air-quality-api.open-meteo.com/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OpenMeteoAirQualityService::class.java)

    private val geocodingApi = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OpenMeteoGeocodingService::class.java)

    private var cachedSnapshot: WeatherSnapshot? = null
    private var cachedAllIndia: List<IndiaCityWeatherSummary> = emptyList()

    suspend fun searchCities(query: String): Result<List<GeocodingResultDto>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext Result.success(emptyList())

        val localIndiaMatches = IndiaWeatherDirectory.cities.filter {
            it.name.contains(trimmed, ignoreCase = true) ||
                it.stateOrUt.contains(trimmed, ignoreCase = true)
        }.map {
            GeocodingResultDto(
                name = it.name,
                country = "India",
                admin1 = it.stateOrUt,
                latitude = it.latitude,
                longitude = it.longitude,
                timezone = "Asia/Kolkata"
            )
        }

        return@withContext try {
            val resp = geocodingApi.searchCity(trimmed)
            val remote = resp.results ?: emptyList()
            val indiaRemote = remote.filter { it.country.equals("India", ignoreCase = true) }
            val otherRemote = remote.filterNot { it.country.equals("India", ignoreCase = true) }
            val combined = (localIndiaMatches + indiaRemote + otherRemote)
                .distinctBy { "${it.name.lowercase()}_${it.admin1?.lowercase().orEmpty()}_${it.country.lowercase()}" }
            Result.success(combined)
        } catch (e: Exception) {
            if (localIndiaMatches.isNotEmpty()) Result.success(localIndiaMatches)
            else Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownCoordinates(): Pair<Double, Double>? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            val loc = providers.firstNotNullOfOrNull { p ->
                try { lm.getLastKnownLocation(p) } catch (_: SecurityException) { null }
            }
            loc?.let { it.latitude to it.longitude }
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentOrLastCoordinates(): Pair<Double, Double>? {
        getLastKnownCoordinates()?.let { return it }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val provider = when {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return null
        }
        return withTimeoutOrNull(4500L) {
            suspendCancellableCoroutine { cont ->
                val cs = CancellationSignal()
                cont.invokeOnCancellation { cs.cancel() }
                try {
                    LocationManagerCompat.getCurrentLocation(
                        lm,
                        provider,
                        cs,
                        ContextCompat.getMainExecutor(context)
                    ) { loc ->
                        if (cont.isActive) {
                            cont.resume(loc?.let { it.latitude to it.longitude })
                        }
                    }
                } catch (_: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }

    suspend fun fetchAllIndiaWeather(forceRefresh: Boolean = false): Result<List<IndiaCityWeatherSummary>> =
        withContext(Dispatchers.IO) {
            if (!forceRefresh && cachedAllIndia.isNotEmpty()) {
                return@withContext Result.success(cachedAllIndia)
            }
            try {
                val chunks = IndiaWeatherDirectory.cities.chunked(18)
                val allResults = coroutineScope {
                    chunks.map { chunk ->
                        async {
                            val lats = chunk.joinToString(",") { it.latitude.toString() }
                            val lons = chunk.joinToString(",") { it.longitude.toString() }
                            val raw = forecastApi.getBatchForecastRaw(latitudes = lats, longitudes = lons).string().trim()
                            val parsedList = if (raw.startsWith("[")) {
                                forecastListAdapter.fromJson(raw).orEmpty()
                            } else {
                                listOfNotNull(singleForecastAdapter.fromJson(raw))
                            }
                            chunk.mapIndexedNotNull { idx, citySpec ->
                                val resp = parsedList.getOrNull(idx) ?: return@mapIndexedNotNull null
                                val cur = resp.current ?: return@mapIndexedNotNull null
                                val daily = resp.daily
                                val wCode = cur.weatherCode
                                IndiaCityWeatherSummary(
                                    city = citySpec,
                                    tempCelsius = cur.temperature2m,
                                    feelsLikeCelsius = cur.apparentTemperature,
                                    minTempC = daily?.tempMin?.firstOrNull() ?: cur.temperature2m,
                                    maxTempC = daily?.tempMax?.firstOrNull() ?: cur.temperature2m,
                                    humidityPercent = cur.humidity,
                                    windSpeedKmh = cur.windSpeed10m,
                                    rainProbabilityPercent = daily?.precipProbMax?.firstOrNull() ?: 0,
                                    conditionLabel = wmoCodeToCondition(wCode),
                                    weatherCode = wCode,
                                    isDay = cur.isDay == 1,
                                    isFromCache = false
                                )
                            }
                        }
                    }.awaitAll().flatten()
                }
                if (allResults.isEmpty()) error("No live India weather entries returned.")
                cachedAllIndia = allResults
                Result.success(allResults)
            } catch (e: Exception) {
                if (cachedAllIndia.isNotEmpty()) {
                    Result.success(cachedAllIndia.map { it.copy(isFromCache = true) })
                } else {
                    Result.failure(e)
                }
            }
        }

    suspend fun fetchLiveWeather(cityName: String, lat: Double, lon: Double): Result<WeatherSnapshot> =
        withContext(Dispatchers.IO) {
            try {
                val forecast = forecastApi.getForecast(lat = lat, lon = lon)
                val aqiResp = try {
                    airQualityApi.getAirQuality(lat = lat, lon = lon)
                } catch (_: Exception) {
                    null
                }

                val current = forecast.current
                val daily = forecast.daily
                val hourly = forecast.hourly

                val wCode = current?.weatherCode ?: 0
                val uv = daily?.uvIndexMax?.firstOrNull() ?: 0.0
                val rainProb = daily?.precipProbMax?.firstOrNull() ?: 0
                val aqi = aqiResp?.current?.usAqi ?: 28

                val hourlyItems = if (hourly != null) {
                    hourly.time.indices.take(24).map { idx ->
                        val rawTime = hourly.time.getOrNull(idx) ?: ""
                        val shortTime = rawTime.substringAfter("T", rawTime).take(5)
                        HourlyForecastItem(
                            timeLabel = shortTime,
                            tempCelsius = hourly.temperature2m.getOrNull(idx) ?: 0.0,
                            rainProbability = hourly.precipitationProbability.getOrNull(idx) ?: 0,
                            weatherCode = hourly.weatherCode.getOrNull(idx) ?: 0
                        )
                    }
                } else emptyList()

                val dailyItems = if (daily != null) {
                    daily.time.indices.map { idx ->
                        DailyForecastItem(
                            dateLabel = daily.time.getOrNull(idx) ?: "",
                            minTempC = daily.tempMin.getOrNull(idx) ?: 0.0,
                            maxTempC = daily.tempMax.getOrNull(idx) ?: 0.0,
                            rainProbability = daily.precipProbMax.getOrNull(idx) ?: 0,
                            weatherCode = daily.weatherCode.getOrNull(idx) ?: 0,
                            uvMax = daily.uvIndexMax.getOrNull(idx) ?: 0.0
                        )
                    }
                } else emptyList()

                val alert = buildList {
                    if (wCode >= 95) add("Thunderstorm activity forecast nearby")
                    if (uv >= 8.0) add("Very High UV Index (${uv.toInt()}) — sun protection advised")
                    if (aqi >= 150) add("Unhealthy Air Quality (AQI $aqi)")
                    if (rainProb >= 75) add("High chance of rain ($rainProb%) today")
                }.firstOrNull()

                val snapshot = WeatherSnapshot(
                    cityName = cityName,
                    latitude = lat,
                    longitude = lon,
                    timezone = forecast.timezone,
                    tempCelsius = current?.temperature2m ?: 0.0,
                    feelsLikeCelsius = current?.apparentTemperature ?: 0.0,
                    humidityPercent = current?.humidity ?: 0,
                    windSpeedKmh = current?.windSpeed10m ?: 0.0,
                    rainProbabilityPercent = rainProb,
                    conditionLabel = wmoCodeToCondition(wCode),
                    weatherCode = wCode,
                    isDay = (current?.isDay ?: 1) == 1,
                    sunrise = daily?.sunrise?.firstOrNull()?.substringAfter("T")?.take(5) ?: "--:--",
                    sunset = daily?.sunset?.firstOrNull()?.substringAfter("T")?.take(5) ?: "--:--",
                    uvIndex = uv,
                    usAqi = aqi,
                    aqiCategory = aqiToLabel(aqi),
                    alertBanner = alert,
                    hourly = hourlyItems,
                    daily = dailyItems,
                    fetchedAtMillis = System.currentTimeMillis(),
                    isFromCache = false
                )
                cachedSnapshot = snapshot
                Result.success(snapshot)
            } catch (e: Exception) {
                val fallback = cachedSnapshot?.copy(isFromCache = true)
                if (fallback != null) {
                    Result.success(fallback)
                } else {
                    Result.failure(e)
                }
            }
        }

    companion object {
        fun wmoCodeToCondition(code: Int): String = when (code) {
            0 -> "Clear Sky"
            1, 2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing Rain"
            71, 73, 75, 77 -> "Snow"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Variable"
        }

        fun aqiToLabel(aqi: Int): String = when {
            aqi <= 50 -> "Good"
            aqi <= 100 -> "Moderate"
            aqi <= 150 -> "Sensitive"
            aqi <= 200 -> "Unhealthy"
            else -> "Hazardous"
        }
    }
}
