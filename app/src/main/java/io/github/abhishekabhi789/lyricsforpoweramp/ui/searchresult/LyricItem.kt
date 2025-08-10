package io.github.abhishekabhi789.lyricsforpoweramp.ui.searchresult

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BasicTooltipBox
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.InterpreterMode
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.CustomChip
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LyricItem(
    modifier: Modifier = Modifier,
    lyrics: Lyrics,
    isLaunchedFromPowerAmp: Boolean,
    onLyricChosen: (preferredLyricsType: LyricsType) -> Unit,
    onEditLyrics: (preferredLyricsType: LyricsType) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lyricPages = remember(lyrics) { listOfNotNull(lyrics.plainLyrics, lyrics.syncedLyrics) }
    val pagerState = rememberPagerState(pageCount = { lyricPages.size }, initialPage = 0)
    var expanded by remember { mutableStateOf(false) }
    val defaultCardColor = CardDefaults.elevatedCardColors().containerColor

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors()
            .copy(containerColor = if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh else defaultCardColor),
        modifier = modifier.scale(if (expanded) 1.01f else 1.0f)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(8.dp)
        ) {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Audiotrack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                        Text(
                            text = lyrics.trackName,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    lyrics.artistName?.let {
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
                                text = it,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    lyrics.albumName?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Album,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
                Text(
                    text = lyrics.getFormattedDuration(),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.weight(0.5f)
                ) {
                    if (lyrics.plainLyrics != null) {
                        CustomChip(
                            label = stringResource(R.string.plain_lyrics_short),
                            selected = lyricPages[pagerState.currentPage] == lyrics.plainLyrics,
                            tooltipDescription = stringResource(R.string.result_type_plain_description),
                            icon = R.drawable.ic_plain_lyrics
                        ) { scope.launch { pagerState.animateScrollToPage(0) } }
                    }
                    if (lyrics.syncedLyrics != null) {
                        CustomChip(
                            label = stringResource(R.string.synced_lyrics_short),
                            selected = lyricPages[pagerState.currentPage] == lyrics.syncedLyrics,
                            tooltipDescription = stringResource(R.string.result_type_synced_description),
                            icon = R.drawable.ic_synced_lyrics
                        ) { scope.launch { pagerState.animateScrollToPage(lyricPages.lastIndex) } }
                    }
                }
                if (isLaunchedFromPowerAmp) {
                    FlowRow(
                        horizontalArrangement = Arrangement.End,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(0.5f)
                            .height(IntrinsicSize.Min)
                    ) {
                        val preferredLyricsType =
                            remember { AppPreference.getPreferredLyricsType(context) }
                        val availableLyrics = buildList {
                            if (lyrics.syncedLyrics != null) add(LyricsType.SYNCED)
                            if (lyrics.plainLyrics != null) add(LyricsType.PLAIN)
                            if (lyrics.instrumental == true) add(LyricsType.INSTRUMENTAL)
                        }
                        SelectableButton(
                            actionIcon = Icons.Default.Edit,
                            tooltipDescription = stringResource(R.string.edit_lyrics),
                            preferred = preferredLyricsType,
                            allItems = availableLyrics,
                            onSend = onEditLyrics,
                            getLabel = { stringResource(it.shortLabelResId) })
                        SelectableButton(
                            actionIcon = Icons.Default.Done,
                            tooltipDescription = stringResource(R.string.save),
                            preferred = preferredLyricsType,
                            allItems = availableLyrics,
                            onSend = onLyricChosen,
                            getLabel = { stringResource(it.shortLabelResId) })
                    }
                }
            }
        }
        if (lyricPages.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(4.dp))
            Box(contentAlignment = Alignment.TopEnd) {
                HorizontalPager(
                    state = pagerState,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.animateContentSize()
                ) { pageIndex ->
                    val lyricsInView by remember(pageIndex) { derivedStateOf { lyricPages[pageIndex] } }
                    SelectionContainer {
                        Text(
                            text = if (expanded) lyricsInView
                            else lyricsInView.lines().run {
                                subList(0, size.coerceAtMost(6))
                                    .joinToString(separator = "\n", postfix = "\n...")
                            },
                            style = TextStyle(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable(interactionSource = null, indication = null) {
                                    expanded = !expanded
                                }
                        )
                    }
                }

                val rotationAnimation = animateFloatAsState(
                    targetValue = if (expanded) -180f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "expand icon rotation animation"
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 12.dp, top = 8.dp)
                        .graphicsLayer {
                            translationY = -60 * abs(pagerState.currentPageOffsetFraction)
                            rotationZ = rotationAnimation.value
                            alpha = abs(1 - abs(pagerState.currentPageOffsetFraction * 2))
                                .coerceIn(0f, 1f)
                        }
                        .background(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f), CircleShape
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun <T> SelectableButton(
    modifier: Modifier = Modifier,
    actionIcon: ImageVector,
    tooltipDescription: String,
    preferred: T,
    allItems: List<T>,
    onSend: (T) -> Unit,
    getLabel: @Composable (T) -> String,
) {
    var expanded by remember { mutableStateOf(false) }
    var chosenItem by remember {
        mutableStateOf(if (preferred in allItems) preferred else allItems.first())
    }
    BasicTooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        state = rememberBasicTooltipState(),
        tooltip = {
            ElevatedCard(
                modifier = Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CardDefaults.shape
                )
            ) {
                Text(
                    text = tooltipDescription,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }) {
        Surface(
            shape = InputChipDefaults.shape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = modifier
                .clip(MaterialTheme.shapes.small)
                .padding(vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(36.dp)
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = stringResource(R.string.send_lyrics_button),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onSend(chosenItem) }
                        .size(34.dp)
                        .padding(2.dp)
                )
                VerticalDivider()
                ExposedDropdownMenuBox(
                    modifier = Modifier,
                    expanded = expanded,
                    onExpandedChange = { expanded = it }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    ) {
                        Text(
                            text = getLabel(chosenItem),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Icon(
                            imageVector = Icons.Default.let { if (expanded) it.ArrowDropUp else it.ArrowDropDown },
                            contentDescription = stringResource(R.string.change_preferred_lyrics_type),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(IntrinsicSize.Max)
                    ) {
                        allItems.forEach { value ->
                            DropdownMenuItem(
                                text = { Text(text = getLabel(value)) },
                                colors = MenuDefaults.itemColors().copy(
                                    textColor = if (value == chosenItem) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                ),
                                onClick = {
                                    chosenItem = value
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewLyricItem() {
    val lyrics = LoremIpsum(words = 20).values.joinToString(" ")
    val data = Lyrics(
        trackName = "Track Name 1",
        artistName = "Artists Name 1",
        albumName = "Album Name 1",
        duration = 200.0,
        instrumental = false,
        plainLyrics = lyrics,
        syncedLyrics = lyrics
    )
    LyricItem(lyrics = data, isLaunchedFromPowerAmp = true, onLyricChosen = { }, onEditLyrics = {})
}
