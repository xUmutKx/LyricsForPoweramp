package io.github.abhishekabhi789.lyricsforpoweramp.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.window.DialogProperties
import io.github.abhishekabhi789.lyricsforpoweramp.BuildConfig
import io.github.abhishekabhi789.lyricsforpoweramp.R
import kotlinx.coroutines.delay

@Composable
fun FirstTimeInfoDialog(
    onDismiss: () -> Unit
) {
    var buttonTimeout by rememberSaveable { mutableIntStateOf(10) }
    LaunchedEffect(Unit) {
        while (buttonTimeout > 0) {
            delay(1000)
            buttonTimeout -= 1
        }
    }
    val label = stringResource(R.string.got_it)
    val buttonLabel by remember {
        derivedStateOf {
            if (buttonTimeout == 0) label else "($buttonTimeout) $label"
        }
    }
    AlertDialog(
        properties = DialogProperties(
            dismissOnBackPress = false, dismissOnClickOutside = false
        ), onDismissRequest = onDismiss, confirmButton = {
            TextButton(onClick = onDismiss, enabled = buttonTimeout == 0) {
                Text(buttonLabel)
            }
        }, title = {
            Text(
                stringResource(R.string.first_time_info_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }, text = {
            val linkColor = if (isSystemInDarkTheme()) Color(0xFF64B5F6) else Color(0xFF1976D2)
            Text(
                buildAnnotatedString {
                    append(stringResource(R.string.first_time_info_dialog_content, ""))
                    withLink(
                        LinkAnnotation.Url(
                            "mailto:${BuildConfig.EMAIL}", TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                )
                            )
                        )
                    ) {
                        append(BuildConfig.EMAIL)
                    }
                })
        })
}
