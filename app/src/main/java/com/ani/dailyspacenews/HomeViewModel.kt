package com.ani.dailyspacenews

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.edit
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ani.dailyspacenews.data.ApodEntity
import com.ani.dailyspacenews.data.EventEntity
import com.ani.dailyspacenews.data.LaunchEntity
import com.ani.dailyspacenews.data.NewsEntity
import com.ani.dailyspacenews.data.SpaceDatabase
import com.ani.dailyspacenews.glance.NextLaunchData
import com.ani.dailyspacenews.glance.NextLaunchWidget
import com.ani.dailyspacenews.util.dataStore
import com.ani.dailyspacenews.widget.ApodData
import com.ani.dailyspacenews.widget.ApodWidget
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class HomeViewModel : ViewModel() {
    private val _apodData = mutableStateOf<ApodResponse?>(null)
    val apodData: State<ApodResponse?> = _apodData

    val newsList = mutableStateOf<List<NewsArticle>>(emptyList())
    val launchList = mutableStateOf<List<Launch>>(emptyList())

    private val _eventsList = mutableStateOf<List<SpaceEvent>>(emptyList())
    val eventsList: State<List<SpaceEvent>> = _eventsList

    private val _galleryList = mutableStateOf<List<ApodResponse>>(emptyList())
    val galleryList: State<List<ApodResponse>> = _galleryList

    private val _selectedApod = mutableStateOf<ApodResponse?>(null)
    val selectedApod: State<ApodResponse?> = _selectedApod

    val isLoading = mutableStateOf(false)
    val apodIndex: Int = Random.nextInt(0, 12)

    private val fallbackApod = ApodResponse(
        "Earth and Moon",
        "https://images-assets.nasa.gov/image/PIA00342/PIA00342~orig.jpg",
        null,
        "A fallback image of our planet and its natural satellite.",
        "2026-01-01"
    )

    fun selectApod(apod: ApodResponse?) {
        _selectedApod.value = apod
    }

    fun fetchData(context: Context) {
        val dao = SpaceDatabase.getDatabase(context).spaceDao()
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Cache read on startup
            val cachedApodEntity = dao.getApod()
            val cachedNewsEntities = dao.getAllNews()
            val cachedLaunchEntities = dao.getAllLaunches()
            val cachedEventEntities = dao.getAllEvents()

            val cachedApod = cachedApodEntity?.let {
                ApodResponse(it.title, it.url, it.hdurl, it.explanation, it.date)
            }
            val cachedNews = cachedNewsEntities.map {
                NewsArticle(it.id, it.title, it.url, it.summary, it.imageUrl, it.publishedAt, it.newsSite)
            }
            val cachedLaunches = cachedLaunchEntities.filter {
                try { (sdf.parse(it.windowStart)?.time ?: 0L) > now } catch (_: Exception) { false }
            }.map {
                Launch(it.id, it.name, it.windowStart, it.image, null, Pad(Location(it.location)))
            }

            // For events, merge cache with static list to ensure they are always visible
            val cachedEventsList = cachedEventEntities.map {
                SpaceEvent(it.id, it.name, it.description, it.date, it.featureImage, EventType(it.typeName))
            }
            val initialEvents = (cachedEventsList + SpaceEvents2026.events)
                .distinctBy { it.id }
                .sortedBy { it.date }

            withContext(Dispatchers.Main) {
                _apodData.value = cachedApod ?: fallbackApod
                if (cachedNews.isNotEmpty()) newsList.value = cachedNews
                if (cachedLaunches.isNotEmpty()) launchList.value = cachedLaunches
                if (initialEvents.isNotEmpty()) _eventsList.value = initialEvents
            }

            // 2. Parallel network fetching
            try {
                withContext(Dispatchers.Main) { isLoading.value = true }
                coroutineScope {
                    launch {
                        Log.d("APOD", "fetching...")
                        val apodList = runCatching { RetrofitClient.nasaApi.getApod(15) }.getOrNull()
                        val freshApod = if (!apodList.isNullOrEmpty()) apodList[apodIndex % apodList.size] else null
                        if (!apodList.isNullOrEmpty()) {
                            withContext(Dispatchers.Main) {
                                _galleryList.value = apodList
                            }
                        }
                        freshApod?.let { apod ->
                            dao.insertApod(ApodEntity(0, apod.title, apod.url, apod.hdurl ?: "", apod.explanation ?: "", apod.date ?: ""))
                            context.dataStore.edit { prefs ->
                                prefs[ApodData.APOD_TITLE] = apod.title
                                prefs[ApodData.APOD_URL] = apod.url
                            }
                            ApodWidget.updateAll(context)
                            withContext(Dispatchers.Main) { _apodData.value = apod }
                        } ?: run {
                            if (_apodData.value == null) {
                                withContext(Dispatchers.Main) { _apodData.value = fallbackApod }
                            }
                        }
                    }

                    launch {
                        val freshNews = runCatching { RetrofitClient.spaceflightApi.getNews().results }.getOrNull()
                        freshNews?.let { news ->
                            if (news.isNotEmpty()) {
                                dao.insertNews(news.map {
                                    NewsEntity(it.id, it.title, it.url, it.summary, it.image_url, it.published_at, it.news_site)
                                })
                                withContext(Dispatchers.Main) { newsList.value = news }
                            }
                        }
                    }

                    launch {
                        val freshLaunchesRaw = runCatching { RetrofitClient.launchApi.getUpcomingLaunches().results }.getOrNull()
                        freshLaunchesRaw?.let { launches ->
                            val filteredLaunches = launches.filter {
                                try { (sdf.parse(it.window_start)?.time ?: 0L) > now } catch (_: Exception) { false }
                            }
                            if (filteredLaunches.isNotEmpty()) {
                                dao.insertLaunches(filteredLaunches.map {
                                    LaunchEntity(it.id, it.name, it.window_start, it.image, it.pad?.location?.name)
                                })

                                val nextLaunch = filteredLaunches[0]
                                context.dataStore.edit { prefs ->
                                    prefs[NextLaunchData.NEXT_LAUNCH_NAME] = nextLaunch.name
                                    prefs[NextLaunchData.NEXT_LAUNCH_TIME] = nextLaunch.window_start
                                }
                                NextLaunchWidget.updateAll(context)
                                withContext(Dispatchers.Main) { launchList.value = filteredLaunches }
                            }
                        }
                    }

                    launch {
                        val apiEvents = runCatching { RetrofitClient.launchApi.getUpcomingEvents(30).results }.getOrNull() ?: emptyList()
                        val merged = (apiEvents + SpaceEvents2026.events)
                            .distinctBy { it.id }
                            .sortedBy { it.date }

                        if (merged.isNotEmpty()) {
                            dao.insertEvents(merged.map {
                                EventEntity(it.id, it.name, it.description, it.date, it.featureImage, it.type?.name)
                            })
                            withContext(Dispatchers.Main) { _eventsList.value = merged }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) { isLoading.value = false }
            }
        }
    }
}
