package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsPageLayout(
    modifier: Modifier = Modifier,
    topbar: @Composable (() -> Unit),
    content: @Composable (ColumnScope.() -> Unit)
) {
    Scaffold(
        topBar = topbar,
        modifier = Modifier
    ) { contentPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
            modifier = modifier
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .padding(horizontal = 8.dp)
                .verticalScroll(rememberScrollState())
        )
    }
}
