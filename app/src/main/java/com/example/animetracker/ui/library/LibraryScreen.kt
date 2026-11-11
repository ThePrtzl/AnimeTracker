package com.example.animetracker.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.animetracker.AnimeApp
import com.example.animetracker.domain.Franchise
import com.example.animetracker.domain.LibraryStatus
import com.example.animetracker.ui.components.CircleIconButton
import com.example.animetracker.ui.components.MessageState
import com.example.animetracker.ui.components.PosterTile
import com.example.animetracker.ui.components.SearchField
import com.example.animetracker.ui.components.color
import com.example.animetracker.ui.components.icon
import com.example.animetracker.ui.components.label
import com.example.animetracker.ui.theme.Ink

/** Library as grouped shelves (design 4c): Started, Completed and Dropped, each its own row. */
@Composable
fun LibraryScreen(onOpen: (Int) -> Unit) {
    val app = LocalContext.current.applicationContext as AnimeApp
    val vm: LibraryViewModel = viewModel(
        factory = viewModelFactory { initializer { LibraryViewModel(app.repository) } },
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    // Set when the user taps "See all" on a shelf.
    var expanded by rememberSaveable { mutableStateOf<LibraryStatus?>(null) }

    BackHandler(enabled = expanded != null) { expanded = null }

    val visible = ui.items.filter { query.isBlank() || it.title.contains(query.trim(), ignoreCase = true) }
    val groups = LibraryStatus.entries
        .map { status -> status to visible.filter { it.libraryStatus == status } }
        .filter { it.second.isNotEmpty() }
    val expandedGroup = groups.firstOrNull { it.first == expanded }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (expandedGroup != null) {
                    CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back to library", onClick = { expanded = null })
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    expandedGroup?.first?.label() ?: "Library",
                    color = Ink.Text,
                    fontSize = 30.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                )
            }
            CircleIconButton(
                icon = if (searching) Icons.Rounded.Close else Icons.Rounded.Search,
                description = if (searching) "Close search" else "Search library",
                onClick = {
                    searching = !searching
                    if (!searching) query = ""
                },
            )
        }

        if (searching) {
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search your library",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                !ui.loaded -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Ink.Accent)
                ui.items.isEmpty() -> MessageState(
                    message = "Nothing here yet. Open an anime and check off an episode, and it will show up in your library.",
                    modifier = Modifier.align(Alignment.Center),
                )
                groups.isEmpty() -> MessageState(
                    message = "No matches in your library.",
                    modifier = Modifier.align(Alignment.Center),
                )
                expandedGroup != null -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(expandedGroup.second, key = { it.rootId }) { franchise ->
                        LibraryTile(
                            franchise = franchise,
                            onOpen = onOpen,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(192.dp),
                        )
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(groups, key = { it.first }) { (status, list) ->
                        Shelf(
                            status = status,
                            items = list,
                            onSeeAll = { expanded = status },
                            onOpen = onOpen,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Shelf(
    status: LibraryStatus,
    items: List<Franchise>,
    onSeeAll: () -> Unit,
    onOpen: (Int) -> Unit,
) {
    Column(Modifier.padding(bottom = 14.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(status.color()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(status.icon(), contentDescription = null, tint = Ink.OnAccent, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(status.label(), color = Ink.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Text("${items.size}", color = Color(0xFFAAB0CC), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            if (items.size > 3) {
                Box(
                    Modifier
                        .heightIn(min = 44.dp)
                        .clickable(role = Role.Button, onClick = onSeeAll)
                        .padding(start = 12.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text("See all", color = Ink.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items, key = { it.rootId }) { franchise ->
                LibraryTile(
                    franchise = franchise,
                    onOpen = onOpen,
                    modifier = Modifier
                        .width(112.dp)
                        .height(150.dp),
                )
            }
        }
    }
}

@Composable
private fun LibraryTile(franchise: Franchise, onOpen: (Int) -> Unit, modifier: Modifier = Modifier) {
    PosterTile(
        id = franchise.rootId,
        title = franchise.title,
        meta = franchise.shortProgress(),
        coverUrl = franchise.coverUrl,
        color = franchise.coverColor,
        // The shelf itself says which status this is, so no corner badge here.
        status = null,
        onClick = { onOpen(franchise.rootId) },
        modifier = modifier,
    )
}
