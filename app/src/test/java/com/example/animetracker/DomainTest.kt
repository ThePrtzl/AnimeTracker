package com.example.animetracker

import com.example.animetracker.data.remote.EdgeDto
import com.example.animetracker.data.remote.MediaDto
import com.example.animetracker.data.remote.RelatedMediaDto
import com.example.animetracker.data.remote.RelationsDto
import com.example.animetracker.data.remote.cleanDescription
import com.example.animetracker.data.remote.isFranchiseRoot
import com.example.animetracker.data.remote.toArgbOrNull
import com.example.animetracker.domain.Franchise
import com.example.animetracker.domain.LibraryStatus
import com.example.animetracker.domain.Season
import com.example.animetracker.domain.SeasonStatus
import com.example.animetracker.domain.seasonStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTest {

    private fun season(id: Int, number: Int, total: Int, watched: Int, dropped: Boolean = false, airing: Boolean = false) =
        Season(id, number, "Season $number", total, airing, watched, dropped)

    private fun franchise(vararg seasons: Season) =
        Franchise(1, "Test", null, null, "", emptyList(), seasons.toList())

    // ---- season status ----

    @Test
    fun `no episodes watched is not started`() {
        assertEquals(SeasonStatus.NotStarted, seasonStatus(0, 12, airing = false, dropped = false))
    }

    @Test
    fun `first episode watched is started`() {
        assertEquals(SeasonStatus.Started, seasonStatus(1, 12, airing = false, dropped = false))
    }

    @Test
    fun `every episode watched is completed`() {
        assertEquals(SeasonStatus.Completed, seasonStatus(12, 12, airing = false, dropped = false))
    }

    @Test
    fun `caught up on a show that is still airing stays started`() {
        assertEquals(SeasonStatus.Started, seasonStatus(6, 6, airing = true, dropped = false))
    }

    @Test
    fun `dropped wins over progress`() {
        assertEquals(SeasonStatus.Dropped, seasonStatus(12, 12, airing = false, dropped = true))
    }

    @Test
    fun `next episode is null once a season is fully watched`() {
        assertNull(season(1, 1, total = 12, watched = 12).nextEpisode)
        assertEquals(6, season(1, 1, total = 12, watched = 5).nextEpisode)
        // Unknown total: there's always a next one.
        assertEquals(1, season(1, 1, total = 0, watched = 0).nextEpisode)
    }

    // ---- library status across seasons ----

    @Test
    fun `finished season 1 and started season 2 is started`() {
        val f = franchise(season(1, 1, 12, 12), season(2, 2, 13, 5), season(3, 3, 12, 0))
        assertEquals(LibraryStatus.Started, f.libraryStatus)
    }

    @Test
    fun `finished season 1 and dropped season 2 is dropped`() {
        val f = franchise(season(1, 1, 25, 25), season(2, 2, 12, 4, dropped = true))
        assertEquals(LibraryStatus.Dropped, f.libraryStatus)
    }

    @Test
    fun `all seasons finished is completed`() {
        val f = franchise(season(1, 1, 12, 12), season(2, 2, 12, 12))
        assertEquals(LibraryStatus.Completed, f.libraryStatus)
    }

    @Test
    fun `one season finished and the next untouched is still started`() {
        val f = franchise(season(1, 1, 12, 12), season(2, 2, 12, 0))
        assertEquals(LibraryStatus.Started, f.libraryStatus)
    }

    @Test
    fun `nothing watched means not in the library`() {
        assertNull(franchise(season(1, 1, 12, 0), season(2, 2, 12, 0)).libraryStatus)
    }

    @Test
    fun `short progress text`() {
        assertEquals("9/24 eps", franchise(season(1, 1, 24, 9)).shortProgress())
        assertEquals("26 eps", franchise(season(1, 1, 26, 26)).shortProgress())
        assertEquals("S2 · 5/13", franchise(season(1, 1, 12, 12), season(2, 2, 13, 5)).shortProgress())
        assertEquals("Dropped S2", franchise(season(1, 1, 25, 25), season(2, 2, 12, 4, dropped = true)).shortProgress())
    }

    // ---- AniList helpers ----

    private fun media(vararg edges: EdgeDto) = MediaDto(id = 10, relations = RelationsDto(edges.toList()))

    private fun edge(type: String, format: String = "TV", status: String = "FINISHED") =
        EdgeDto(type, RelatedMediaDto(id = 99, type = "ANIME", format = format, status = status))

    @Test
    fun `entry with a TV prequel is not a franchise root`() {
        assertFalse(media(edge("PREQUEL")).isFranchiseRoot())
    }

    @Test
    fun `movie or OVA prequels and sequels do not count as seasons`() {
        assertTrue(media(edge("PREQUEL", format = "MOVIE")).isFranchiseRoot())
        assertTrue(media(edge("PREQUEL", format = "OVA")).isFranchiseRoot())
        assertTrue(media(edge("SEQUEL")).isFranchiseRoot())
    }

    @Test
    fun `colour and description cleanup`() {
        assertEquals(0xFFE4A15DL, "#e4a15d".toArgbOrNull())
        assertNull(null.toArgbOrNull())
        assertEquals("One\nTwo", cleanDescription("One<br>Two"))
        assertEquals("Fish & chips", cleanDescription("<i>Fish</i> &amp; chips"))
    }
}
