package com.example.animetracker.data

import com.example.animetracker.data.remote.MediaDto
import com.example.animetracker.data.remote.RelatedMediaDto
import com.example.animetracker.data.remote.isSeasonLike
import com.example.animetracker.data.remote.relatedSeason

private const val MAX_HOPS = 12

/**
 * AniList lists every season as its own entry, linked by PREQUEL / SEQUEL relations.
 * Given any one entry, this returns all seasons of that anime in watch order.
 *
 * The entry that was asked for is always part of the result: we walk back from it to find
 * earlier seasons and forward from it to find later ones, so it can never be replaced by a
 * different anime (for example a one-off prequel special).
 *
 * Movies, OVAs, specials and one-shots are standalone: they are returned on their own and
 * never joined to the series they belong to (those show up under "Related to" instead).
 *
 * [load] fetches one entry by id. Each id is requested at most once.
 */
suspend fun resolveSeasonChain(
    mediaId: Int,
    load: suspend (Int) -> MediaDto,
): List<MediaDto> {
    val requested = load(mediaId)
    if (!requested.isSeasonLike()) return listOf(requested)
    val visited = mutableSetOf(requested.id)

    // Earlier seasons, nearest first while walking, so we add each one to the front.
    val before = ArrayDeque<MediaDto>()
    var cursor = requested
    var hops = 0
    while (hops++ < MAX_HOPS) {
        val prev = cursor.relatedSeason("PREQUEL", earliest = false)?.takeIf { visited.add(it.id) } ?: break
        cursor = load(prev.id)
        before.addFirst(cursor)
    }

    val chain = mutableListOf<MediaDto>()
    chain += before
    chain += requested

    // Later seasons.
    cursor = requested
    hops = 0
    while (hops++ < MAX_HOPS) {
        val next = cursor.relatedSeason("SEQUEL", earliest = true)?.takeIf { visited.add(it.id) } ?: break
        cursor = load(next.id)
        chain += cursor
    }
    return chain
}

/**
 * Anime connected to any season in [chain] that is not itself one of those seasons:
 * movies, OVAs, specials, one-shots, spin-offs and so on. No duplicates, oldest first.
 * Manga/novels and "shares characters" links are left out.
 */
fun collectRelated(chain: List<MediaDto>, limit: Int = 30): List<RelatedMediaDto> {
    val inChain = chain.map { it.id }.toSet()
    return chain.asSequence()
        .flatMap { it.relations?.edges.orEmpty().asSequence() }
        .filter { it.relationType != "CHARACTER" }
        .mapNotNull { it.node }
        .filter { it.type == "ANIME" && it.id !in inChain && it.status != "CANCELLED" }
        .distinctBy { it.id }
        .sortedWith(compareBy<RelatedMediaDto> { it.startDate?.year ?: Int.MAX_VALUE }.thenBy { it.id })
        .take(limit)
        .toList()
}
