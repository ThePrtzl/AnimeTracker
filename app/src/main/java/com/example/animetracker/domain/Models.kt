package com.example.animetracker.domain

/** Where one season of an anime stands. Derived from episodes watched plus the "dropped" flag. */
enum class SeasonStatus { NotStarted, Started, Completed, Dropped }

/** Where a whole anime stands in the library. */
enum class LibraryStatus { Started, Completed, Dropped }

/**
 * Season rules:
 * - dropped flag wins
 * - nothing watched -> NotStarted
 * - every episode watched (and the season has finished airing) -> Completed
 * - anything in between -> Started
 */
fun seasonStatus(watched: Int, total: Int, airing: Boolean, dropped: Boolean): SeasonStatus = when {
    dropped -> SeasonStatus.Dropped
    watched <= 0 -> SeasonStatus.NotStarted
    total > 0 && watched >= total && !airing -> SeasonStatus.Completed
    else -> SeasonStatus.Started
}

/** Human label for an AniList format, e.g. MOVIE -> "Movie". Null/unknown -> null. */
fun formatLabel(format: String?): String? = when (format) {
    "TV" -> "TV"
    "TV_SHORT" -> "Short"
    "MOVIE" -> "Movie"
    "OVA" -> "OVA"
    "SPECIAL" -> "Special"
    "ONA" -> "ONA"
    "MUSIC" -> "Music"
    else -> null
}

/** "1 episode" / "12 episodes". */
fun episodeText(n: Int): String = if (n == 1) "1 episode" else "$n episodes"

/** An anime connected to this one (movie, OVA, special, one-shot, spin-off...). */
data class RelatedItem(
    val id: Int,
    val title: String,
    val format: String?,
    val year: Int?,
    val coverUrl: String?,
    val coverColor: Long?,
) {
    /** e.g. "Movie · 2019". */
    val meta: String
        get() = listOfNotNull(formatLabel(format), year?.toString()).joinToString(" · ").ifBlank { "Anime" }
}

data class Season(
    val id: Int,
    /** 1-based position in the franchise. */
    val number: Int,
    val title: String,
    /** 0 when AniList doesn't know the episode count yet. */
    val totalEpisodes: Int,
    /** Still releasing episodes, so it can't be "Completed" yet. */
    val airing: Boolean,
    /** Episodes 1..watched are checked off. */
    val watched: Int,
    val dropped: Boolean,
) {
    val status: SeasonStatus get() = seasonStatus(watched, totalEpisodes, airing, dropped)

    /** The next episode to watch, or null if the season is fully caught up. */
    val nextEpisode: Int?
        get() = (watched + 1).takeIf { totalEpisodes == 0 || it <= totalEpisodes }
}

data class Franchise(
    /** AniList id of the first season. Used as the library key. */
    val rootId: Int,
    val title: String,
    val coverUrl: String?,
    /** ARGB colour from AniList's cover palette, used as a poster fallback. */
    val coverColor: Long?,
    val synopsis: String,
    val genres: List<String>,
    val seasons: List<Season>,
    /** Epoch millis of the latest progress change; 0 if none. */
    val lastActivity: Long = 0L,
    /** AniList format of the first entry (TV, MOVIE, OVA...). */
    val format: String? = null,
    /** Movies, OVAs, specials, one-shots etc. connected to this anime. */
    val related: List<RelatedItem> = emptyList(),
) {
    val totalEpisodes: Int get() = seasons.sumOf { it.totalEpisodes }

    /** True for movies, OVAs, specials and one-shots (a single standalone entry, not a series). */
    val isStandalone: Boolean get() = format != null && format != "TV" && format != "ONA" || (seasons.size == 1 && totalEpisodes in 1..3)

    /** "Movie", "OVA", ... for standalone entries; null for ordinary series. */
    val kindLabel: String? get() = if (isStandalone) formatLabel(format) ?: "Special" else null

    /**
     * Library rules:
     * - no progress anywhere -> not in the library (null)
     * - any season dropped -> Dropped
     * - every season completed -> Completed
     * - otherwise -> Started (something in progress, or later seasons still to go)
     */
    val libraryStatus: LibraryStatus?
        get() {
            if (seasons.none { it.watched > 0 || it.dropped }) return null
            return when {
                seasons.any { it.status == SeasonStatus.Dropped } -> LibraryStatus.Dropped
                seasons.all { it.status == SeasonStatus.Completed } -> LibraryStatus.Completed
                else -> LibraryStatus.Started
            }
        }

    /** Short progress text for tiles, e.g. "S2 · 5/13", "9/24 eps", "Dropped S2". */
    fun shortProgress(): String {
        if (seasons.size == 1) {
            val s = seasons.first()
            return when {
                s.status == SeasonStatus.Completed -> if (s.totalEpisodes == 1) "1 ep" else "${s.totalEpisodes} eps"
                s.totalEpisodes > 0 -> "${s.watched}/${s.totalEpisodes} eps"
                else -> if (s.watched == 1) "1 ep" else "${s.watched} eps"
            }
        }
        seasons.firstOrNull { it.status == SeasonStatus.Dropped }?.let { return "Dropped S${it.number}" }
        seasons.firstOrNull { it.status == SeasonStatus.Started }?.let {
            val total = if (it.totalEpisodes > 0) it.totalEpisodes.toString() else "?"
            return "S${it.number} · ${it.watched}/$total"
        }
        val done = seasons.count { it.status == SeasonStatus.Completed }
        return "$done/${seasons.size} seasons"
    }
}
