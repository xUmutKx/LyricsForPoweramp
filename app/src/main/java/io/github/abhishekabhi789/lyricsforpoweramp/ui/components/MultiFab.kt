package io.github.abhishekabhi789.lyricsforpoweramp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Looks3
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.LooksTwo
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class FabData(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun FabItem(modifier: Modifier = Modifier, data: FabData, onCollapse: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(end = 4.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 4.dp
        ) {
            Text(
                text = data.label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        SmallFloatingActionButton(
            onClick = { onCollapse(); data.onClick.invoke() },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(data.icon, contentDescription = data.label)
        }
    }
}

@Composable
fun MultiFab(modifier: Modifier = Modifier, fabList: List<FabData>) {
    val animDuration = 50
    var fabExpanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (fabExpanded) 45f else 0f, label = "fabRotation")
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(16.dp)
    ) {

        fabList.asReversed().forEachIndexed { index, data ->
            AnimatedVisibility(
                visible = fabExpanded,
                enter = fadeIn(tween(delayMillis = index * animDuration)) +
                        slideInVertically(
                            animationSpec = tween(delayMillis = index * animDuration),
                            initialOffsetY = { it / 2 }
                        ),
                exit = fadeOut(tween(delayMillis = (fabList.size - 1 - index) * animDuration)) +
                        slideOutVertically(
                            animationSpec = tween(delayMillis = (fabList.size - 1 - index) * animDuration),
                            targetOffsetY = { it / 2 }
                        )
            ) {
                FabItem(data = data, onCollapse = { fabExpanded = false })
            }
        }


        FloatingActionButton(onClick = { fabExpanded = !fabExpanded }) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}


@Preview
@Composable
private fun PreviewMultiFab() {
    var message by remember { mutableStateOf("Click on a button") }

    val fabList = listOf(
        FabData("Item 1", Icons.Default.LooksOne) { message = "Clicked on Item 1" },
        FabData("Item 2", Icons.Default.LooksTwo) { message = "Clicked on Item 2" },
        FabData("Item 3", Icons.Default.Looks3) { message = "Clicked on Item 3" },
    )

    Scaffold(floatingActionButton = { MultiFab(fabList = fabList) }) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            Text(message)
        }
    }
}
