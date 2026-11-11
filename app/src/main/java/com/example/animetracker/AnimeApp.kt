package com.example.animetracker

import android.app.Application
import androidx.room.Room
import com.example.animetracker.data.AnimeRepository
import com.example.animetracker.data.local.AppDatabase
import com.example.animetracker.data.local.MIGRATION_1_2
import com.example.animetracker.data.remote.AniListApi
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Application class doubling as a tiny service locator (no DI framework needed for one repository). */
class AnimeApp : Application() {

    lateinit var repository: AnimeRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        val database = Room.databaseBuilder(this, AppDatabase::class.java, "anime.db")
            .addMigrations(MIGRATION_1_2)
            .build()
        repository = AnimeRepository(AniListApi(client), database)
    }
}
