package com.ani.dailyspacenews.data

import android.content.Context
import androidx.room.*

@Entity(tableName = "news_articles")
data class NewsEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val url: String,
    val summary: String,
    val imageUrl: String?,
    val publishedAt: String,
    val newsSite: String
)

@Entity(tableName = "launches")
data class LaunchEntity(
    @PrimaryKey val id: String,
    val name: String,
    val windowStart: String,
    val image: String?,
    val location: String?
)

@Entity(tableName = "apod_cache")
data class ApodEntity(
    @PrimaryKey val id: Int = 0,
    val title: String,
    val url: String,
    val hdurl: String,
    val explanation: String,
    val date: String
)

@Entity(tableName = "events_cache")
data class EventEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?,
    val date: String,
    val featureImage: String?,
    val typeName: String?
)

@Dao
interface SpaceDao {
    @Query("SELECT * FROM news_articles ORDER BY publishedAt DESC")
    suspend fun getAllNews(): List<NewsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(news: List<NewsEntity>)

    @Query("DELETE FROM news_articles")
    suspend fun clearAllNews()

    @Query("SELECT * FROM launches ORDER BY windowStart ASC")
    suspend fun getAllLaunches(): List<LaunchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaunches(launches: List<LaunchEntity>)

    @Query("DELETE FROM launches")
    suspend fun clearAllLaunches()

    @Query("SELECT * FROM apod_cache WHERE id = 0")
    suspend fun getApod(): ApodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApod(apod: ApodEntity)

    @Query("DELETE FROM apod_cache")
    suspend fun clearAllApod()

    @Query("SELECT * FROM events_cache ORDER BY date ASC")
    suspend fun getAllEvents(): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Query("DELETE FROM events_cache")
    suspend fun clearAllEvents()
}

@Database(entities = [NewsEntity::class, LaunchEntity::class, ApodEntity::class, EventEntity::class], version = 4, exportSchema = false)
abstract class SpaceDatabase : RoomDatabase() {
    abstract fun spaceDao(): SpaceDao

    companion object {
        @Volatile
        private var INSTANCE: SpaceDatabase? = null

        fun getDatabase(context: Context): SpaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpaceDatabase::class.java,
                    "space_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
