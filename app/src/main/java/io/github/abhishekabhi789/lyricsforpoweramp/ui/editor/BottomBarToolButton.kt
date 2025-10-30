package io.github.abhishekabhi789.lyricsforpoweramp.ui.editor

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomBarToolButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongPressChange: ((Boolean) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    TooltipBox(
        state = rememberTooltipState(),
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above
        ),
        focusable = false,
        tooltip = {
            PlainTooltip { Text(label) }
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) LocalContentColor.current
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(CircleShape)
                .padding(4.dp)
                .indication(interactionSource, ripple())
                .pointerInput(enabled) {
                    detectTapGestures(
                        onTap = {
                            if (!enabled) return@detectTapGestures
                            onClick()
                        },
                        onLongPress = {
                            if (!enabled) return@detectTapGestures
                            onLongPressChange?.invoke(true)
                        },
                        onPress = {
                            if (!enabled) return@detectTapGestures
                            try {
                                awaitRelease()
                            } finally {
                                onLongPressChange?.invoke(false)
                            }
                        }
                    )
                }
        )
    }
}
