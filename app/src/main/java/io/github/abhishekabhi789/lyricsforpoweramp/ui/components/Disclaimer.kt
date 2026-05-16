package io.github.abhishekabhi789.lyricsforpoweramp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun Disclaimer(
    modifier: Modifier = Modifier,
    textContent: AnnotatedString,
    icon: ImageVector,
    foregroundColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
    backgroundColor: Color = MaterialTheme.colorScheme.tertiaryContainer
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = foregroundColor
            )
            Spacer(modifier = Modifier.padding(4.dp))
            Text(
                text = textContent,
                color = foregroundColor,
                style = MaterialTheme.typography.bodyMedium,

                )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDisclaimer() {
    Disclaimer(
        textContent = AnnotatedString("This is the text content."),
        icon = Icons.Default.Info
    )
}
