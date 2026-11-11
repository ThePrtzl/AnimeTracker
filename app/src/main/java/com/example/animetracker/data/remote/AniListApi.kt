package com.example.animetracker.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Minimal AniList GraphQL client (https://anilist.gitbook.io/anilist-apiv2-docs/).
 * No API key is needed for public reads. AniList allows roughly 90 requests per minute.
 */
class AniListApi(private val client: OkHttpClient) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun browse(
        page: Int,
        perPage: Int,
        search: String?,
        genre: String?,
        formats: List<String>,
    ): PageDto {
        val hasSearch = !search.isNullOrBlank()
        val variables = buildJsonObject {
            put("page", page)
            put("perPage", perPage)
            put("formats", buildJsonArray { formats.forEach { add(JsonPrimitive(it)) } })
            if (hasSearch) put("search", search!!.trim())
            if (genre != null) put("genre", genre)
            put(
                "sort",
                buildJsonArray { add(JsonPrimitive(if (hasSearch) "SEARCH_MATCH" else "POPULARITY_DESC")) },
            )
        }
        return execute<BrowseData>(BROWSE_QUERY, variables).page
    }

    suspend fun media(id: Int): MediaDto =
        execute<DetailData>(DETAIL_QUERY, buildJsonObject { put("id", id) }).media

    private suspend inline fun <reified T> execute(query: String, variables: JsonObject): T =
        withContext(Dispatchers.IO) {
            val payload = buildJsonObject {
                put("query", query)
                put("variables", variables)
            }.toString()
            val request = Request.Builder()
                .url(ENDPOINT)
                .header("Accept", "application/json")
                .post(payload.toRequestBody(JSON_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 429) {
                    throw IOException("AniList rate limit reached. Try again in a minute.")
                }
                val parsed = try {
                    json.decodeFromString<GqlResponse<T>>(text)
                } catch (e: Exception) {
                    throw IOException("Unexpected response from AniList (HTTP ${response.code}).", e)
                }
                parsed.data
                    ?: throw IOException(
                        parsed.errors?.firstOrNull()?.message?.takeIf { it.isNotBlank() }
                            ?: "Request failed (HTTP ${response.code}).",
                    )
            }
        }

    private companion object {
        const val ENDPOINT = "https://graphql.anilist.co"
        val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

        const val BROWSE_QUERY = """
            query Browse(${'$'}page: Int, ${'$'}perPage: Int, ${'$'}search: String, ${'$'}genre: String, ${'$'}formats: [MediaFormat], ${'$'}sort: [MediaSort]) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                pageInfo { hasNextPage }
                media(type: ANIME, format_in: ${'$'}formats, isAdult: false, search: ${'$'}search, genre: ${'$'}genre, sort: ${'$'}sort) {
                  id
                  title { romaji english }
                  coverImage { extraLarge color }
                  format
                  episodes
                  status
                  nextAiringEpisode { episode }
                  relations { edges { relationType node { id type format episodes status startDate { year } } } }
                }
              }
            }
        """

        const val DETAIL_QUERY = """
            query Detail(${'$'}id: Int) {
              Media(id: ${'$'}id, type: ANIME) {
                id
                title { romaji english }
                coverImage { extraLarge color }
                bannerImage
                description(asHtml: false)
                genres
                episodes
                format
                status
                startDate { year }
                nextAiringEpisode { episode }
                relations {
                  edges {
                    relationType
                    node { id type format episodes status startDate { year } title { romaji english } coverImage { large color } }
                  }
                }
              }
            }
        """
    }
}
