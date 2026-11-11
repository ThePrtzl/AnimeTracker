package com.example.animetracker.data

import androidx.room.withTransaction
import com.example.animetracker.data.local.AppDatabase
import com.example.animetracker.data.local.FranchiseEntity
import com.example.animetracker.data.local.SeasonProgressEntity
import com.example.animetracker.data.remote.AniListApi
import com.example.animetracker.data.remote.MediaDto
import com.example.animetracker.data.remote.cleanDescription
import com.example.animetracker.data.remote.isAiring
import com.example.animetracker.data.remote.isFranchiseRoot
import com.example.animetracker.data.remote.relatedSeason
import com.example.animetracker.data.remote.toArgbOrNull
import com.example.animetracker.data.remote.totalEpisodes
import com.example.animetracker.domain.Franchise
import com.example.animetracker.domain.LibraryStatus
import com.example.animetracker.domain.Season
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

/** What the browse grid needs for one tile. */
data class BrowseItem(
    val id: Int,
    val title: String,
    val coverUrl: String?,
    val coverColor: Long?,
    val meta: String,
)

data class BrowseResult(
    val items: List<BrowseItem>,
    /** Pass this as startPage to get the next batch. */
    val nextPage: Int,
    val hasNext: Boolean,
)

/** Season info stored inside FranchiseEntity.seasonsJson. */
@Serializable
data class SeasonInfo(
    val id: Int,
    val title: String,
    val episodes: Int,
    val airing: Boolean,
)

class AnimeRepository(
    private val api: AniListApi,
    private val db: AppDatabase,
) {
    private val dao = db.dao()
    private val json = Json { ignoreUnknownKeys = true }

    // ---- browse ----------------------------------------------------------------------

    /**
     * Loads up to a few AniList pages until there are enough tiles to fill the screen.
     * When browsing (no search text), sequels are filtered out so each anime appears once;
     * when searching, every match is shown and tapping a later season opens the whole anime.
     */
    suspend fun browse(startPage: Int, search: String?, genre: String?): BrowseResult {
        val searching = !search.isNullOrBlank()
        val items = mutableListOf<BrowseItem>()
        var page = startPage
        var hasNext = true
        var calls = 0
        while (hasNext && items.size < MIN_TILES && calls < MAX_PAGES_PER_LOAD) {
            val result = api.browse(page, PAGE_SIZE, search, genre)
            items += result.media
                .filter { searching || it.isFranchiseRoot() }
                .map { it.toBrowseItem() }
            hasNext = result.pageInfo.hasNextPage
            page++
            calls++
        }
        return BrowseResult(items, nextPage = page, hasNext = hasNext)
    }

    private fun MediaDto.toBrowseItem(): BrowseItem {
        val total = totalEpisodes()
        return BrowseItem(
            id = id,
            title = title.display,
            coverUrl = coverImage?.extraLarge,
            coverColor = coverImage?.color.toArgbOrNull(),
            meta = if (total > 0) "$total eps" else "Airing",
        )
    }

    // ---- franchise (an anime and its seasons) -----------------------------------------

    /**
     * Makes sure the franchise containing [mediaId] is cached locally and returns its root id.
     * Uses the cache when it's fresh, and falls back to a stale cache if the network is down.
     */
    suspend fun ensureFranchise(mediaId: Int): Int {
        val cached = dao.getFranchise(mediaId)
        if (cached != null && !cached.isStale()) return cached.rootId
        return try {
            val fresh = resolveFranchise(mediaId)
            dao.upsertFranchise(fresh)
            fresh.rootId
        } catch (e: IOException) {
            cached?.rootId ?: throw e
        }
    }

    /**
     * AniList lists every season as its own entry linked by PREQUEL/SEQUEL relations.
     * Walk back to the first season, then forward through the sequels.
     */
    private suspend fun resolveFranchise(mediaId: Int): FranchiseEntity {
        val seen = mutableSetOf<Int>()
        var root = api.media(mediaId)
        seen += root.id

        var hops = 0
        while (hops++ < MAX_HOPS) {
            val prev = root.relatedSeason("PREQUEL", earliest = false)?.takeIf { it.id !in seen } ?: break
            seen += prev.id
            root = api.media(prev.id)
        }

        val chain = mutableListOf(root)
        var tail = root
        hops = 0
        while (hops++ < MAX_HOPS) {
            val next = tail.relatedSeason("SEQUEL", earliest = true)?.takeIf { it.id !in seen } ?: break
            seen += next.id
            tail = api.media(next.id)
            chain += tail
        }

        val seasons = chain.map {
            SeasonInfo(
                id = it.id,
                title = it.title.display,
                episodes = it.totalEpisodes(),
                airing = it.isAiring(),
            )
        }
        return FranchiseEntity(
            rootId = root.id,
            title = root.title.display,
            coverUrl = root.coverImage?.extraLarge,
            coverColor = root.coverImage?.color.toArgbOrNull(),
            synopsis = cleanDescription(root.description),
            genres = root.genres.joinToString("|"),
            seasonsJson = json.encodeToString(seasons),
            fetchedAt = System.currentTimeMillis(),
        )
    }

    private fun FranchiseEntity.isStale(): Boolean {
        val age = System.currentTimeMillis() - fetchedAt
        val anyAiring = parseSeasons(seasonsJson).any { it.airing }
        return age > (if (anyAiring) ONE_DAY_MS else SEVEN_DAYS_MS)
    }

    private fun parseSeasons(raw: String): List<SeasonInfo> =
        runCatching { json.decodeFromString<List<SeasonInfo>>(raw) }.getOrDefault(emptyList())

    // ---- observing --------------------------------------------------------------------

    fun observeFranchise(rootId: Int): Flow<Franchise?> =
        combine(dao.observeFranchise(rootId), dao.observeProgress(rootId)) { franchise, progress ->
            franchise?.toDomain(progress)
        }

    /** Every anime that has any progress, newest activity first. */
    fun observeLibrary(): Flow<List<Franchise>> =
        combine(dao.observeAllFranchises(), dao.observeAllProgress()) { franchises, progress ->
            val byRoot = progress.groupBy { it.rootId }
            franchises
                .map { it.toDomain(byRoot[it.rootId].orEmpty()) }
                .filter { it.libraryStatus != null }
                .sortedByDescending { it.lastActivity }
        }

    /** rootId -> status, for badging tiles in Browse. */
    fun observeLibraryStatuses(): Flow<Map<Int, LibraryStatus>> =
        observeLibrary().map { list ->
            list.mapNotNull { f -> f.libraryStatus?.let { f.rootId to it } }.toMap()
        }

    private fun FranchiseEntity.toDomain(progress: List<SeasonProgressEntity>): Franchise {
        val byId = progress.associateBy { it.seasonId }
        val seasons = parseSeasons(seasonsJson).mapIndexed { index, info ->
            val p = byId[info.id]
            Season(
                id = info.id,
                number = index + 1,
                title = info.title,
                totalEpisodes = info.episodes,
                airing = info.airing,
                watched = p?.watched ?: 0,
                dropped = p?.dropped ?: false,
            )
        }
        return Franchise(
            rootId = rootId,
            title = title,
            coverUrl = coverUrl,
            coverColor = coverColor,
            synopsis = synopsis,
            genres = genres.split("|").filter { it.isNotBlank() },
            seasons = seasons,
            lastActivity = progress.maxOfOrNull { it.updatedAt } ?: 0L,
        )
    }

    // ---- progress ---------------------------------------------------------------------

    /** Marks episodes 1..[watched] as watched (0 clears the season). Clears "dropped" when progress moves. */
    suspend fun setWatched(season: Season, rootId: Int, watched: Int) {
        val max = if (season.totalEpisodes > 0) season.totalEpisodes else Int.MAX_VALUE
        val clamped = watched.coerceIn(0, max)
        db.withTransaction {
            val current = dao.getProgress(season.id)
            dao.upsertProgress(
                SeasonProgressEntity(
                    seasonId = season.id,
                    rootId = rootId,
                    watched = clamped,
                    dropped = if (clamped != (current?.watched ?: 0)) false else current?.dropped ?: false,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun setDropped(season: Season, rootId: Int, dropped: Boolean) {
        db.withTransaction {
            val current = dao.getProgress(season.id)
            dao.upsertProgress(
                SeasonProgressEntity(
                    seasonId = season.id,
                    rootId = rootId,
                    watched = current?.watched ?: 0,
                    dropped = dropped,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    private companion object {
        const val PAGE_SIZE = 50
        const val MIN_TILES = 18
        const val MAX_PAGES_PER_LOAD = 3
        const val MAX_HOPS = 12
        const val ONE_DAY_MS = 24L * 60 * 60 * 1000
        const val SEVEN_DAYS_MS = 7 * ONE_DAY_MS
    }
}
