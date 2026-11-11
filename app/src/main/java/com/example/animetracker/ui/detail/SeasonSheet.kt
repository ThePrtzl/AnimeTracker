package com.example.animetracker.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animetracker.domain.Season
import com.example.animetracker.ui.components.CircleIconButton
import com.example.animetracker.ui.components.ProgressRing
import com.example.animetracker.ui.components.color
import com.example.animetracker.ui.components.label
import com.example.animetracker.ui.theme.Ink
import kotlinx.coroutines.launch

/**
 * The episode-checking sheet (design 3c): tap an episode to mark everything up to it,
 * tap the last watched one again to undo it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonSheet(
    season: Season,
    franchiseTitle: String,
    multiSeason: Boolean,
    onDismiss: () -> Unit,
    onTapEpisode: (Int) -> Unit,
    onMarkAll: () -> Unit,
    onToggleDrop: () -> Unit,
    onMarkNext: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    val status = season.status
    val total = season.totalEpisodes
    // If AniList doesn't know the total yet (still airing), offer one episode past what's watched.
    val cellCount = if (total > 0) total else season.watched + 1
    val progress = if (total > 0) season.watched.toFloat() / total else 0f
    val heading = if (multiSeason) "Season ${season.number}" else franchiseTitle

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Ink.Surface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0x4DFFFFFF)) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            // Header: ring, season name + status, close
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(progress = progress, color = status.color(), diameter = 60.dp, stroke = 6.dp) {
                    Text(
                        "${season.watched}/${if (total > 0) total.toString() else "?"}",
                        color = Ink.Text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        heading,
                        color = Ink.Text,
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(status.color()))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            buildString {
                                append(status.label())
                                if (total > 0) append(" · $total episodes")
                            },
                            color = status.color(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (multiSeason && season.title != franchiseTitle) {
                        Text(
                            season.title,
                            color = Ink.TextMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                CircleIconButton(Icons.Rounded.Close, "Close", onClick = close)
            }

            // Hint
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text("Episodes", color = Color(0xFFD0D4EA), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Tap one to mark everything up to it", color = Color(0xFFAAB0CC), fontSize = 12.sp)
            }

            // Episode grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                items(cellCount) { index ->
                    val number = index + 1
                    EpisodeCell(
                        number = number,
                        watched = number <= season.watched,
                        next = number == season.watched + 1,
                        onClick = { onTapEpisode(number) },
                    )
                }
            }

            // Legend
            Row(
                Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendItem("Watched", Ink.Accent, outlined = false)
                LegendItem("Next up", Ink.Accent, outlined = true)
                LegendItem("Not watched", Color(0x66FFFFFF), outlined = true, thin = true)
            }

            Spacer(Modifier.height(20.dp))

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onMarkAll,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x12FFFFFF),
                        contentColor = Color(0xFFEEF0FB),
                    ),
                    border = BorderStroke(1.dp, Color(0x2EFFFFFF)),
                ) {
                    Text("Mark all watched", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onToggleDrop,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x1AFFA3A3),
                        contentColor = Color(0xFFFFB4B4),
                    ),
                    border = BorderStroke(1.dp, Color(0x73FFA3A3)),
                ) {
                    Text(
                        if (season.dropped) "Resume season" else "Drop season",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            val next = season.nextEpisode
            Button(
                onClick = onMarkNext,
                enabled = next != null,
                modifier = Modifier
                    .padding(top = 10.dp)
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
                Text(
                    if (next != null) "Mark episode $next watched" else if (total > 0) "Season complete" else "Caught up",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun EpisodeCell(number: Int, watched: Boolean, next: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(15.dp)
    val background = if (watched) Ink.Accent else Color(0x12FFFFFF)
    val textColor = when {
        watched -> Ink.OnAccent
        next -> Ink.Accent
        else -> Color(0xFFE6E8F5)
    }
    val borderColor = when {
        watched || next -> Ink.Accent
        else -> Color(0x29FFFFFF)
    }
    val borderWidth = if (next && !watched) 2.dp else 1.dp
    val description = "Episode $number, " + when {
        watched -> "watched"
        next -> "next up"
        else -> "not watched"
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(shape)
            .background(background)
            .border(borderWidth, borderColor, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text("$number", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (watched) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = Ink.OnAccent,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(12.dp),
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color, outlined: Boolean, thin: Boolean = false) {
    val shape = RoundedCornerShape(4.dp)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(12.dp)
                .clip(shape)
                .then(if (outlined) Modifier.border(if (thin) 1.dp else 2.dp, color, shape) else Modifier.background(color)),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color(0xFFC0C5DE), fontSize = 12.sp)
    }
}
