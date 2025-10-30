package io.github.abhishekabhi789.lyricsforpoweramp.ui.components

import androidx.compose.foundation.BasicTooltipBox
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.abhishekabhi789.lyricsforpoweramp.R

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CustomChip(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    tooltipDescription: String,
    icon: Any? = null,
    onClick: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val backgroundColor = if (selected) cs.primaryContainer else cs.surfaceContainer
    val contentColor = if (selected) cs.onPrimaryContainer else cs.onSurface

    BasicTooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above
        ),
        state = rememberBasicTooltipState(),
        tooltip = {
            Text(
                text = tooltipDescription,
                color = cs.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall
            )
        },
        modifier = modifier.requiredHeight(AssistChipDefaults.Height)
    ) {
        Surface(
            onClick = onClick,
            shape = AssistChipDefaults.shape,
            color = Color.Transparent,
            contentColor = contentColor,
            tonalElevation = 1.dp,
            modifier = Modifier
                .background(backgroundColor, AssistChipDefaults.shape)
                .border(1.dp, cs.outline.copy(alpha = 0.3f), AssistChipDefaults.shape)
                .padding(horizontal = 2.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (icon != null) {
                    when (icon) {
                        is Int -> Icon(
                            painter = painterResource(id = icon),
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )

                        is ImageVector -> Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewChip() {
    Row {
        CustomChip(
            label = stringResource(R.string.plain_lyrics),
            selected = true,
            tooltipDescription = stringResource(R.string.result_type_plain_description),
            icon = R.drawable.ic_plain_lyrics,
            onClick = {}
        )
        CustomChip(
            label = stringResource(R.string.synced_lyrics),
            selected = false,
            tooltipDescription = stringResource(R.string.result_type_synced_description),
            icon = R.drawable.ic_synced_lyrics,
            onClick = {}
        )
    }
}
