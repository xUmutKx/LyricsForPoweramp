package io.github.abhishekabhi789.lyricsforpoweramp.ui.locallyrics

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.LocalArtLoader
import io.github.abhishekabhi789.lyricsforpoweramp.model.LocalLyricsMatch
import io.github.abhishekabhi789.lyricsforpoweramp.model.LocalLyricsMatchLine

@Composable
fun LocalLyricsItem(
    match: LocalLyricsMatch,
    onPlay: (positionMs: Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AlbumArt(audioUri = match.entry.audioUri)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = match.entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = listOf(match.entry.artist, match.entry.folder)
                            .filter { it.isNotBlank() }.distinct().joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.local_lyrics_hit_count, match.hits, match.hits
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (match.entry.hasAudio) {
                    IconButton(onClick = { onPlay(match.firstHitPositionMs) }) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.local_lyrics_play_in_poweramp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            match.lines.forEach { matchLine ->
                MatchedLineRow(
                    matchLine = matchLine,
                    playable = match.entry.hasAudio && matchLine.line.positionMs >= 0,
                    onPlay = { onPlay(matchLine.line.positionMs) }
                )
            }
        }
    }
}

/** Cover art from the audio file's tags, with a placeholder while it loads or when there's none. */
@Composable
private fun AlbumArt(audioUri: String?, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val context = LocalContext.current
    val art by produceState<ImageBitmap?>(initialValue = null, audioUri) {
        value = LocalArtLoader.load(context, audioUri)
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val cover = art
        if (cover != null) {
            Image(
                bitmap = cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MatchedLineRow(
    matchLine: LocalLyricsMatchLine,
    playable: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lineModifier = if (playable) modifier.clickable(onClick = onPlay) else modifier
    Row(
        modifier = lineModifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = formatPosition(matchLine.line.positionMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = highlighted(matchLine),
            style = MaterialTheme.typography.bodyMedium,
            color = if (matchLine.isMatch) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun highlighted(matchLine: LocalLyricsMatchLine): AnnotatedString {
    val text = matchLine.line.text
    if (!matchLine.isMatch || matchLine.highlightStart < 0) return AnnotatedString(text)
    val start = matchLine.highlightStart.coerceIn(0, text.length)
    val end = (start + matchLine.highlightLength).coerceIn(start, text.length)
    val style = SpanStyle(
        background = MaterialTheme.colorScheme.primaryContainer,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        fontWeight = FontWeight.Bold
    )
    return buildAnnotatedString {
        append(text)
        addStyle(style, start, end)
    }
}

private fun formatPosition(positionMs: Long): String {
    if (positionMs < 0) return "--:--"
    val totalSeconds = positionMs / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
