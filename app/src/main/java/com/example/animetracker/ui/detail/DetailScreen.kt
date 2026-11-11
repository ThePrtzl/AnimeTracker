package com.example.animetracker.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.animetracker.AnimeApp
import com.example.animetracker.domain.Franchise
import com.example.animetracker.domain.Season
import com.example.animetracker.domain.SeasonStatus
import com.example.animetracker.ui.components.CircleIconButton
import com.example.animetracker.domain.episodeText
import com.example.animetracker.ui.components.MessageState
import com.example.animetracker.ui.components.PosterTile
import com.example.animetracker.ui.components.Poster
import com.example.animetracker.ui.components.ProgressRing
import com.example.animetracker.ui.components.TagPill
import com.example.animetracker.ui.components.color
import com.example.animetracker.ui.components.label
import com.example.animetracker.ui.theme.Ink

private val PosterHeight = 340.dp
private val SheetOverlap = 280.dp

@Composable
fun DetailScreen(id: Int, onBack: () -> Unit, onOpen: (Int) -> Unit) {
    val app = LocalContext.current.applicationContext as AnimeApp
    val vm: DetailViewModel = viewModel(
        key = "detail-$id",
        factory = viewModelFactory { initializer { DetailViewModel(app.repository, id) } },
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    var openSeasonId by rememberSaveable { mutableStateOf<Int?>(null) }
    val franchise = ui.franchise

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink.Background),
    ) {
        when {
            franchise != null -> {
                DetailContent(
                    franchise = franchise,
                    onOpenSeason = { openSeasonId = it.id },
                    onMarkNext = vm::markNext,
                    onOpenRelated = onOpen,
                )
                val season = franchise.seasons.firstOrNull { it.id == openSeasonId }
                if (season != null) {
                    SeasonSheet(
                        season = season,
                        franchiseTitle = franchise.title,
                        multiSeason = franchise.seasons.size > 1,
                        onDismiss = { openSeasonId = null },
                        // Tap marks everything up to that episode; tapping the last watched one undoes it.
                        onTapEpisode = { n -> vm.setWatched(season, if (n == season.watched) n - 1 else n) },
                        onMarkAll = { vm.markAll(season) },
                        onToggleDrop = { vm.toggleDrop(season) },
                        onMarkNext = { vm.markNext(season) },
                    )
                }
            }
            ui.error != null -> MessageState(
                message = ui.error ?: "Something went wrong.",
                actionLabel = "Try again",
                onAction = vm::load,
                modifier = Modifier.align(Alignment.Center),
            )
            else -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Ink.Accent)
        }

        CircleIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            description = "Back",
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp),
            filled = Color(0x8C080812),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    franchise: Franchise,
    onOpenSeason: (Season) -> Unit,
    onMarkNext: (Season) -> Unit,
    onOpenRelated: (Int) -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    var expanded by rememberSaveable { mutableStateOf(false) }

    // The season the big button acts on: the first one that's in progress or not started and has an episode to watch.
    val target = franchise.seasons.firstOrNull {
        (it.status == SeasonStatus.Started || it.status == SeasonStatus.NotStarted) && it.nextEpisode != null
    }
    val seasonCount = franchise.seasons.size
    val episodeCount = franchise.totalEpisodes
    val kind = franchise.kindLabel
    val single = kind != null && seasonCount == 1
    val meta = if (single) {
        listOfNotNull(kind, episodeCount.takeIf { it > 0 }?.let { episodeText(it) }).joinToString(" · ")
    } else {
        buildString {
            append(if (seasonCount == 1) "1 season" else "$seasonCount seasons")
            if (episodeCount > 0) append(" · ${episodeText(episodeCount)}")
        }
    }
    val sheetShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)

    Box(Modifier.fillMaxSize()) {
        Poster(
            seed = franchise.rootId,
            color = franchise.coverColor,
            coverUrl = franchise.coverUrl,
            modifier = Modifier
                .fillMaxWidth()
                .height(PosterHeight),
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(SheetOverlap))
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = screenHeight - SheetOverlap)
                    .clip(sheetShape)
                    .background(Color(0xE6080812))
                    .border(1.dp, Ink.GlassBorder, sheetShape)
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color(0x4DFFFFFF)),
                )
                Text(
                    franchise.title,
                    modifier = Modifier.padding(top = 16.dp),
                    color = Ink.Text,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                )
                Text(
                    meta,
                    modifier = Modifier.padding(top = 4.dp),
                    color = Color(0xFFC0C5DE),
                    fontSize = 13.sp,
                )

                if (franchise.genres.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        franchise.genres.take(4).forEach { TagPill(it) }
                    }
                }

                if (franchise.synopsis.isNotBlank()) {
                    Text(
                        franchise.synopsis,
                        modifier = Modifier.padding(top = 16.dp),
                        color = Color(0xFFC9CDE4),
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        maxLines = if (expanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (franchise.synopsis.length > 220) {
                        Box(
                            Modifier
                                .heightIn(min = 44.dp)
                                .clickable(role = Role.Button) { expanded = !expanded },
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                if (expanded) "Show less" else "Read more",
                                color = Ink.Accent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                Text(
                    "Your progress",
                    modifier = Modifier.padding(top = 20.dp, bottom = 12.dp),
                    color = Ink.Text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    franchise.seasons.chunked(3).forEach { rowSeasons ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowSeasons.forEach { season ->
                                SeasonCard(
                                    season = season,
                                    label = if (single) kind!! else "Season ${season.number}",
                                    onClick = { onOpenSeason(season) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(3 - rowSeasons.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                if (franchise.related.isNotEmpty()) {
                    Text(
                        "Related to",
                        modifier = Modifier.padding(top = 24.dp),
                        color = Ink.Text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Movies, OVAs, specials and more",
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
                        color = Ink.TextMuted,
                        fontSize = 12.sp,
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(end = 20.dp),
                    ) {
                        items(franchise.related, key = { it.id }) { item ->
                            PosterTile(
                                id = item.id,
                                title = item.title,
                                meta = item.meta,
                                coverUrl = item.coverUrl,
                                color = item.coverColor,
                                status = null,
                                onClick = { onOpenRelated(item.id) },
                                modifier = Modifier.size(width = 112.dp, height = 156.dp),
                            )
                        }
                    }
                }

                // Room for the floating button.
                Spacer(Modifier.height(120.dp))
            }
        }

        // Floating primary action
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Ink.Background)))
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp),
        ) {
            Button(
                onClick = { target?.let(onMarkNext) },
                enabled = target != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ink.Accent,
                    contentColor = Ink.OnAccent,
                    disabledContainerColor = Color(0x1AFFFFFF),
                    disabledContentColor = Ink.TextMuted,
                ),
            ) {
                val episode = target?.nextEpisode
                Text(
                    when {
                        target == null || episode == null -> "All caught up"
                        single && episodeCount == 1 -> "Mark as watched"
                        seasonCount > 1 -> "Mark Season ${target.number}, Episode $episode watched"
                        else -> "Mark episode $episode watched"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** One season: progress ring, name and status. Tapping opens the episode sheet. */
@Composable
private fun SeasonCard(season: Season, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    val status = season.status
    val total = season.totalEpisodes
    val progress = if (total > 0) season.watched.toFloat() / total else 0f
    Column(
        modifier
            .clip(shape)
            .background(Ink.GlassFill)
            .border(1.dp, Ink.GlassBorderSoft, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ProgressRing(progress = progress, color = status.color(), diameter = 84.dp, stroke = 7.dp) {
            Text(
                "${season.watched}/${if (total > 0) total.toString() else "?"}",
                color = Ink.Text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(label, color = Ink.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(status.color()),
            )
            Spacer(Modifier.width(5.dp))
            Text(status.label(), color = status.color(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
