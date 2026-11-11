package com.example.animetracker

import com.example.animetracker.data.remote.DateDto
import com.example.animetracker.data.remote.EdgeDto
import com.example.animetracker.data.remote.MediaDto
import com.example.animetracker.data.remote.RelatedMediaDto
import com.example.animetracker.data.remote.RelationsDto
import com.example.animetracker.data.remote.isFranchiseRoot
import com.example.animetracker.data.resolveSeasonChain
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FranchiseResolverTest {

    private fun rel(
        id: Int,
        year: Int,
        format: String = "TV",
        episodes: Int? = 12,
        status: String = "FINISHED",
    ) = RelatedMediaDto(
        id = id,
        type = "ANIME",
        format = format,
        episodes = episodes,
        status = status,
        startDate = DateDto(year),
    )

    private fun edge(type: String, node: RelatedMediaDto) = EdgeDto(type, node)

    private fun media(id: Int, vararg edges: EdgeDto) =
        MediaDto(id = id, relations = RelationsDto(edges.toList()))

    /** Resolves [requested] against a fake AniList made of [entries]; also checks nothing is fetched twice. */
    private fun resolve(requested: Int, vararg entries: MediaDto): List<Int> {
        val byId = entries.associateBy { it.id }
        val loaded = mutableListOf<Int>()
        val chain = runBlocking {
            resolveSeasonChain(requested) { id ->
                loaded += id
                byId.getValue(id)
            }
        }
        assertEquals("each entry should be fetched once", loaded.distinct(), loaded)
        return chain.map { it.id }
    }

    @Test
    fun `one piece is not replaced by its one-episode prequel special`() {
        val onePiece = media(21, edge("PREQUEL", rel(100, 2022, format = "ONA", episodes = 1)))
        val monsters = media(100, edge("SEQUEL", rel(21, 1999)))
        assertEquals(listOf(21), resolve(21, onePiece, monsters))
    }

    @Test
    fun `asking for any season returns every season in order`() {
        val s1 = media(1, edge("SEQUEL", rel(2, 2020)))
        val s2 = media(2, edge("PREQUEL", rel(1, 2019)), edge("SEQUEL", rel(3, 2021)))
        val s3 = media(3, edge("PREQUEL", rel(2, 2020)))
        assertEquals(listOf(1, 2, 3), resolve(1, s1, s2, s3))
        assertEquals(listOf(1, 2, 3), resolve(2, s1, s2, s3))
        assertEquals(listOf(1, 2, 3), resolve(3, s1, s2, s3))
    }

    @Test
    fun `loops in the data do not hang`() {
        val a = media(1, edge("PREQUEL", rel(2, 2019)), edge("SEQUEL", rel(2, 2019)))
        val b = media(2, edge("PREQUEL", rel(1, 2019)), edge("SEQUEL", rel(1, 2019)))
        val chain = resolve(1, a, b)
        assertEquals(setOf(1, 2), chain.toSet())
        assertEquals(2, chain.size)
    }

    @Test
    fun `data that only points backwards still keeps the requested season`() {
        val requested = media(5, edge("PREQUEL", rel(4, 2019)))
        val earlier = media(4)
        assertEquals(listOf(4, 5), resolve(5, requested, earlier))
    }

    @Test
    fun `movies and OVAs are not seasons`() {
        val tv = media(
            1,
            edge("SEQUEL", rel(9, 2020, format = "MOVIE")),
            edge("PREQUEL", rel(8, 2018, format = "OVA")),
        )
        assertEquals(listOf(1), resolve(1, tv))
    }

    @Test
    fun `short specials are not seasons but unknown episode counts are allowed`() {
        assertTrue(media(10, edge("PREQUEL", rel(11, 2010, episodes = 1))).isFranchiseRoot())
        assertTrue(media(10, edge("PREQUEL", rel(11, 2010, episodes = 3))).isFranchiseRoot())
        assertFalse(media(10, edge("PREQUEL", rel(11, 2010, episodes = 4))).isFranchiseRoot())
        assertFalse(media(10, edge("PREQUEL", rel(11, 2010, episodes = null))).isFranchiseRoot())
        assertFalse(media(10, edge("PREQUEL", rel(11, 2010, episodes = 12))).isFranchiseRoot())
    }
}
