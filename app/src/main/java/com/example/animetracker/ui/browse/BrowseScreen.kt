package com.example.animetracker.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.animetracker.AnimeApp
import com.example.animetracker.ui.components.ChipPill
import com.example.animetracker.ui.components.CircleIconButton
import com.example.animetracker.ui.components.MessageState
import com.example.animetracker.ui.components.PosterTile
import com.example.animetracker.ui.components.SearchField
import com.example.animetracker.ui.theme.Ink

/** AniList genre names. */
private val Genres = listOf(
    "Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror", "Mystery",
    "Romance", "Sci-Fi", "Slice of Life", "Sports", "Supernatural",
)

/** Label to show, AniList format to request. */
private val Formats = listOf(
    "TV" to "TV",
    "Movies" to "MOVIE",
    "OVA" to "OVA",
    "Specials" to "SPECIAL",
    "ONA" to "ONA",
    "Shorts" to "TV_SHORT",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrowseScreen(onOpen: (Int) -> Unit) {
    val app = LocalContext.current.applicationContext as AnimeApp
    val vm: BrowseViewModel = viewModel(
        factory = viewModelFactory { initializer { BrowseViewModel(app.repository) } },
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    val library by vm.libraryStatuses.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    // Ask for the next page when the user gets close to the end of the list.
    val nearEnd by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 7
        }
    }
    LaunchedEffect(nearEnd, ui.items.size) {
        if (nearEnd) vm.loadNext()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink.Background)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Discover",
                color = Ink.Text,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp,
            )
            CircleIconButton(Icons.Rounded.Tune, "Filters", onClick = { showFilters = true })
        }

        SearchField(
            value = ui.query,
            onValueChange = vm::onQueryChange,
            placeholder = "Search anime",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        )

        if (ui.genre != null || ui.format != null) {
            Row(
                Modifier.padding(start = 16.dp, top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ui.genre?.let { g ->
                    ChipPill(label = "$g  ✕", selected = true, onClick = { vm.onGenreChange(null) })
                }
                ui.format?.let { f ->
                    val label = Formats.firstOrNull { it.second == f }?.first ?: f
                    ChipPill(label = "$label  ✕", selected = true, onClick = { vm.onFormatChange(null) })
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                ui.items.isEmpty() && ui.loading -> CircularProgressIndicator(
                    Modifier.align(Alignment.Center),
                    color = Ink.Accent,
                )
                ui.items.isEmpty() && ui.error != null -> MessageState(
                    message = ui.error ?: "Something went wrong.",
                    actionLabel = "Try again",
                    onAction = vm::retry,
                    modifier = Modifier.align(Alignment.Center),
                )
                ui.items.isEmpty() -> MessageState(
                    message = "No anime found. Try a different search or filter.",
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(ui.items, key = { it.id }) { item ->
                        PosterTile(
                            id = item.id,
                            title = item.title,
                            meta = item.meta,
                            coverUrl = item.coverUrl,
                            color = item.coverColor,
                            status = library[item.id],
                            onClick = { onOpen(item.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(192.dp),
                        )
                    }
                    if (ui.loading) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Ink.Accent)
                            }
                        }
                    }
                    if (ui.error != null) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            MessageState(
                                message = ui.error ?: "Something went wrong.",
                                actionLabel = "Try again",
                                onAction = vm::retry,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            containerColor = Ink.Surface,
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .navigationBarsPadding(),
            ) {
                Text(
                    "Type",
                    color = Ink.Text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 24.dp),
                ) {
                    ChipPill(
                        label = "All",
                        selected = ui.format == null,
                        onClick = {
                            vm.onFormatChange(null)
                            showFilters = false
                        },
                    )
                    Formats.forEach { (label, value) ->
                        ChipPill(
                            label = label,
                            selected = ui.format == value,
                            onClick = {
                                vm.onFormatChange(value)
                                showFilters = false
                            },
                        )
                    }
                }
                Text(
                    "Genre",
                    color = Ink.Text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChipPill(
                        label = "All",
                        selected = ui.genre == null,
                        onClick = {
                            vm.onGenreChange(null)
                            showFilters = false
                        },
                    )
                    Genres.forEach { genre ->
                        ChipPill(
                            label = genre,
                            selected = ui.genre == genre,
                            onClick = {
                                vm.onGenreChange(genre)
                                showFilters = false
                            },
                        )
                    }
                }
            }
        }
    }
}
