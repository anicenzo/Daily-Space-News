package com.ani.dailyspacenews

import com.ani.dailyspacenews.network.NasaAuthInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.File
import java.util.concurrent.TimeUnit

// --- API Response Wrappers ---
data class NewsResponse(val results: List<NewsArticle>)
data class LaunchResponse(val results: List<Launch>)
data class EventResponse(val results: List<SpaceEvent>)

// --- Retrofit API Interfaces ---
interface SpaceflightApi {
    @GET("articles/")
    suspend fun getNews(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): NewsResponse
}

interface NasaApi {
    @GET("planetary/apod")
    suspend fun getApod(
        @Query("count") count: Int = 1
    ): List<ApodResponse>
}

interface LaunchApi {
    @GET("launch/upcoming/")
    suspend fun getUpcomingLaunches(
        @Query("limit") limit: Int = 20
    ): LaunchResponse

    @GET("event/upcoming/")
    suspend fun getUpcomingEvents(
        @Query("limit") limit: Int = 20
    ): EventResponse
}

object RetrofitClient {
    lateinit var spaceflightApi: SpaceflightApi
        private set
    lateinit var nasaApi: NasaApi
        private set
    lateinit var launchApi: LaunchApi
        private set

    fun init(cacheDir: File) {
        val cache = Cache(File(cacheDir, "http_cache"), 10L * 1024 * 1024)
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

        val baseClient = OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val nasaClient = baseClient.newBuilder()
            .addInterceptor(NasaAuthInterceptor())
            .build()

        spaceflightApi = Retrofit.Builder()
            .baseUrl("https://api.spaceflightnewsapi.net/v4/")
            .client(baseClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpaceflightApi::class.java)

        nasaApi = Retrofit.Builder()
            .baseUrl("https://api.nasa.gov/")
            .client(nasaClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NasaApi::class.java)

        launchApi = Retrofit.Builder()
            .baseUrl("https://ll.thespacedevs.com/2.2.0/")
            .client(baseClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LaunchApi::class.java)
    }
}
