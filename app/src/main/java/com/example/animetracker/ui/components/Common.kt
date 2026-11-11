package com.example.animetracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.animetracker.domain.LibraryStatus
import com.example.animetracker.ui.theme.Ink

// ---- posters -------------------------------------------------------------------------

private val fallbackPalette = listOf(
    0xFF2D2275L to 0xFFC94A78L,
    0xFF0B3B49L to 0xFFD9580EL,
    0xFF17483AL to 0xFF7EC2A0L,
    0xFF1B1B2FL to 0xFF7B2CBFL,
    0xFF6E1818L to 0xFFE8930AL,
    0xFF134E4AL to 0xFFA78BFAL,
    0xFF5A1F4AL to 0xFFF08AA0L,
    0xFF2B2F3AL to 0xFFB08A4AL,
)

/** Gradient shown behind (and while loading) a poster, from AniList's cover colour or a stable fallback. */
fun posterBrush(seed: Int, color: Long?): Brush {
    val colors = if (color != null) {
        val c = Color(color)
        listOf(lerp(c, Color.Black, 0.55f), c)
    } else {
        val (a, b) = fallbackPalette[seed.mod(fallbackPalette.size)]
        listOf(Color(a), Color(b))
    }
    return Brush.verticalGradient(colors)
}

@Composable
fun Poster(
    seed: Int,
    color: Long?,
    coverUrl: String?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier.background(posterBrush(seed, color))) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        content()
    }
}

@Composable
fun StatusBadge(status: LibraryStatus, modifier: Modifier = Modifier) {
    val label = status.label()
    Box(
        modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(status.color())
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(status.icon(), contentDescription = null, tint = Ink.OnAccent, modifier = Modifier.size(14.dp))
    }
}

/** Dark info strip laid over the bottom of a poster. */
@Composable
fun GlassInfo(title: String, meta: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(13.dp)
    Column(
        modifier
            .clip(shape)
            .background(Ink.PosterPanel)
            .border(1.dp, Ink.GlassBorder, shape)
            .padding(horizontal = 8.dp, vertical = 7.dp),
    ) {
        Text(
            title,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            meta,
            color = Ink.TextSoft,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Poster with a glass info strip and an optional status badge. Used by Browse and Library. */
@Composable
fun PosterTile(
    id: Int,
    title: String,
    meta: String,
    coverUrl: String?,
    color: Long?,
    status: LibraryStatus?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Poster(
        seed = id,
        color = color,
        coverUrl = coverUrl,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        if (status != null) {
            StatusBadge(status, Modifier.align(Alignment.TopEnd).padding(7.dp))
        }
        GlassInfo(
            title = title,
            meta = meta,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(5.dp),
        )
    }
}

// ---- controls ------------------------------------------------------------------------

@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    diameter: Dp,
    stroke: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = Color.White.copy(alpha = 0.14f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx),
            )
            if (progress > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/** 44dp round glass button with an icon. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Color = Ink.GlassFill,
) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(filled)
            .border(1.dp, Ink.GlassBorder, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/** Rounded filter chip. */
@Composable
fun ChipPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(19.dp)
    Box(
        modifier
            .height(38.dp)
            .clip(shape)
            .background(if (selected) Ink.Accent else Ink.GlassFill)
            .border(1.dp, if (selected) Ink.Accent else Ink.GlassBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Ink.OnAccent else Ink.Text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Small read-only tag, e.g. a genre. */
@Composable
fun TagPill(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Text(
        text,
        modifier = modifier
            .clip(shape)
            .background(Color(0x14FFFFFF))
            .border(1.dp, Ink.GlassBorder, shape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        color = Color(0xFFECEEFA),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .height(48.dp)
            .clip(shape)
            .background(Ink.GlassFill)
            .border(1.dp, Ink.GlassBorderSoft, shape)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = Ink.TextMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, color = Ink.Hint, fontSize = 15.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Ink.Text, fontSize = 15.sp),
                cursorBrush = SolidColor(Ink.Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = placeholder },
            )
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }) {
                Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = Ink.TextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Centered message with an optional retry action. */
@Composable
fun MessageState(message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(PaddingValues(horizontal = 32.dp, vertical = 48.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = Ink.TextSoft, fontSize = 15.sp, lineHeight = 21.sp)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            ChipPill(actionLabel, selected = true, onClick = onAction)
        }
    }
}
