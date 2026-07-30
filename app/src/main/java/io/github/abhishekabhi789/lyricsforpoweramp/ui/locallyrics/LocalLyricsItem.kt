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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InterpreterMode
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
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
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.CustomChip

/**
 * Styled to match [io.github.abhishekabhi789.lyricsforpoweramp.ui.searchresult.LyricItem] - same
 * card, icon rows and chip so the offline results don't look like a different app.
 */
@Composable
fun LocalLyricsItem(
    match: LocalLyricsMatch,
    onPlay: (positionMs: Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AlbumArt(audioUri = match.entry.audioUri)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Audiotrack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                        Text(
                            text = match.entry.title,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    if (match.entry.artist.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.InterpreterMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                            Text(
                                text = match.entry.artist,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    if (match.entry.folder.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                            Text(
                                text = match.entry.folder,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
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
            CustomChip(
                label = pluralStringResource(R.plurals.local_lyrics_hit_count, match.hits, match.hits)
            )
        }
        HorizontalDivider(modifier = Modifier.padding(4.dp))
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
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
