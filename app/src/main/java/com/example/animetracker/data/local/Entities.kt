package com.example.animetracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Cached franchise info (one row per anime, keyed by its first season's AniList id). */
@Entity(tableName = "franchise")
data class FranchiseEntity(
    @PrimaryKey val rootId: Int,
    val title: String,
    val coverUrl: String?,
    val coverColor: Long?,
    val synopsis: String,
    /** Genres joined with '|'. */
    val genres: String,
    /** JSON list of SeasonInfo, in season order. */
    val seasonsJson: String,
    val fetchedAt: Long,
    /** AniList format of the first entry (TV, MOVIE, OVA...). Null for rows cached before v2. */
    val format: String? = null,
    /** JSON list of RelatedInfo. Null for rows cached before v2 (forces a refresh). */
    val relatedJson: String? = null,
)

/** What the user has watched, per season. Episodes 1..watched are checked off. */
@Entity(tableName = "season_progress", indices = [Index("rootId")])
data class SeasonProgressEntity(
    @PrimaryKey val seasonId: Int,
    val rootId: Int,
    val watched: Int,
    val dropped: Boolean,
    val updatedAt: Long,
)
