package com.example.animetracker.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeDao {
    @Query("SELECT * FROM franchise WHERE rootId = :id")
    suspend fun getFranchise(id: Int): FranchiseEntity?

    @Query("SELECT * FROM franchise WHERE rootId = :id")
    fun observeFranchise(id: Int): Flow<FranchiseEntity?>

    @Query("SELECT * FROM franchise")
    fun observeAllFranchises(): Flow<List<FranchiseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFranchise(entity: FranchiseEntity)

    @Query("SELECT * FROM season_progress WHERE rootId = :rootId")
    fun observeProgress(rootId: Int): Flow<List<SeasonProgressEntity>>

    @Query("SELECT * FROM season_progress")
    fun observeAllProgress(): Flow<List<SeasonProgressEntity>>

    @Query("SELECT * FROM season_progress WHERE seasonId = :seasonId")
    suspend fun getProgress(seasonId: Int): SeasonProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(entity: SeasonProgressEntity)
}

@Database(
    entities = [FranchiseEntity::class, SeasonProgressEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AnimeDao
}

/** v2: franchises remember their format and related anime. Watch progress is untouched. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE franchise ADD COLUMN format TEXT")
        db.execSQL("ALTER TABLE franchise ADD COLUMN relatedJson TEXT")
    }
}
