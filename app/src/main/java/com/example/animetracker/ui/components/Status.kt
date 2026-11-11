package com.example.animetracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.animetracker.domain.LibraryStatus
import com.example.animetracker.domain.SeasonStatus
import com.example.animetracker.ui.theme.Ink

fun SeasonStatus.color(): Color = when (this) {
    SeasonStatus.NotStarted -> Ink.NotStarted
    SeasonStatus.Started -> Ink.Started
    SeasonStatus.Completed -> Ink.Completed
    SeasonStatus.Dropped -> Ink.Dropped
}

fun SeasonStatus.label(): String = when (this) {
    SeasonStatus.NotStarted -> "Not started"
    SeasonStatus.Started -> "Started"
    SeasonStatus.Completed -> "Completed"
    SeasonStatus.Dropped -> "Dropped"
}

fun LibraryStatus.color(): Color = when (this) {
    LibraryStatus.Started -> Ink.Started
    LibraryStatus.Completed -> Ink.Completed
    LibraryStatus.Dropped -> Ink.Dropped
}

fun LibraryStatus.label(): String = when (this) {
    LibraryStatus.Started -> "Started"
    LibraryStatus.Completed -> "Completed"
    LibraryStatus.Dropped -> "Dropped"
}

/** Check / play / cross, so status is never conveyed by colour alone. */
fun LibraryStatus.icon(): ImageVector = when (this) {
    LibraryStatus.Started -> Icons.Rounded.PlayArrow
    LibraryStatus.Completed -> Icons.Rounded.Check
    LibraryStatus.Dropped -> Icons.Rounded.Close
}
