package com.example.animetracker.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GqlResponse<T>(
    val data: T? = null,
    val errors: List<GqlError>? = null,
)

@Serializable
data class GqlError(val message: String = "")

@Serializable
data class BrowseData(@SerialName("Page") val page: PageDto)

@Serializable
data class DetailData(@SerialName("Media") val media: MediaDto)

@Serializable
data class PageDto(
    val pageInfo: PageInfoDto = PageInfoDto(),
    val media: List<MediaDto> = emptyList(),
)

@Serializable
data class PageInfoDto(val hasNextPage: Boolean = false)

@Serializable
data class MediaDto(
    val id: Int,
    val title: TitleDto = TitleDto(),
    val coverImage: CoverDto? = null,
    val bannerImage: String? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val episodes: Int? = null,
    val format: String? = null,
    val status: String? = null,
    val startDate: DateDto? = null,
    val nextAiringEpisode: NextAiringDto? = null,
    val relations: RelationsDto? = null,
)

@Serializable
data class TitleDto(val romaji: String? = null, val english: String? = null) {
    val display: String get() = english ?: romaji ?: "Untitled"
}

@Serializable
data class CoverDto(val extraLarge: String? = null, val color: String? = null)

@Serializable
data class DateDto(val year: Int? = null)

@Serializable
data class NextAiringDto(val episode: Int? = null)

@Serializable
data class RelationsDto(val edges: List<EdgeDto> = emptyList())

@Serializable
data class EdgeDto(
    val relationType: String? = null,
    val node: RelatedMediaDto? = null,
)

@Serializable
data class RelatedMediaDto(
    val id: Int,
    val type: String? = null,
    val format: String? = null,
    val episodes: Int? = null,
    val status: String? = null,
    val startDate: DateDto? = null,
    val title: TitleDto = TitleDto(),
)

// ---- helpers -------------------------------------------------------------------------

/** Formats that count as a "season" of a franchise. Movies, OVAs and specials are ignored. */
private val SEASON_FORMATS = setOf("TV", "ONA")

/**
 * Entries with fewer episodes than this are treated as one-off specials, not seasons
 * (e.g. the 1-episode "MONSTERS: 103 Mercies Dragon Damnation", which AniList lists as a prequel of One Piece).
 * An unknown episode count (still announcing) is allowed.
 */
private const val MIN_SEASON_EPISODES = 4

/** Episodes known so far. For a show that's still airing, that's the aired count. */
fun MediaDto.totalEpisodes(): Int = episodes ?: nextAiringEpisode?.episode?.minus(1) ?: 0

fun MediaDto.isAiring(): Boolean = status == "RELEASING"

/** True when this entry has no TV/ONA prequel, i.e. it's the first season of its franchise. */
fun MediaDto.isFranchiseRoot(): Boolean = relatedSeason("PREQUEL", earliest = false) == null

/**
 * The closest TV/ONA relation of [relationType], or null.
 * For SEQUEL pick the earliest start year; for PREQUEL pick the latest (the one right before this).
 * Sequels that haven't been released yet are ignored.
 */
fun MediaDto.relatedSeason(relationType: String, earliest: Boolean): RelatedMediaDto? {
    val candidates = relations?.edges.orEmpty()
        .asSequence()
        .filter { it.relationType == relationType }
        .mapNotNull { it.node }
        .filter { it.type == "ANIME" && it.format in SEASON_FORMATS }
        .filter { (it.episodes ?: MIN_SEASON_EPISODES) >= MIN_SEASON_EPISODES }
        .filter { it.status != "NOT_YET_RELEASED" && it.status != "CANCELLED" }
        .sortedBy { it.startDate?.year ?: Int.MAX_VALUE }
        .toList()
    return if (earliest) candidates.firstOrNull() else candidates.lastOrNull()
}

/** "#e4a15d" -> opaque ARGB Long, or null. */
fun String?.toArgbOrNull(): Long? =
    this?.removePrefix("#")?.takeIf { it.length == 6 }?.toLongOrNull(16)?.let { 0xFF000000L or it }

/** AniList descriptions contain a little HTML even when asHtml is false. */
fun cleanDescription(raw: String?): String =
    raw.orEmpty()
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
