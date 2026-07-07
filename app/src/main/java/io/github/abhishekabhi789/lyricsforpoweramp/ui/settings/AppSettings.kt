package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SettingsViewModel

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun AppSettings(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    onClose: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val res = LocalResources.current
    val interactionSource = remember { MutableInteractionSource() }
    val focusRemoverModifier = Modifier.clickable(
        interactionSource = interactionSource,
        indication = null
    ) { focusManager.clearFocus() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val viewmodel: SettingsViewModel = hiltViewModel()

    SharedTransitionLayout {
        //this surface hides the brief visibility of root level surface color
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = SettingsPage.Main,
                enterTransition = { slideInHorizontally { it } + fadeIn() },
                exitTransition = { slideOutHorizontally { -it / 3 } + fadeOut() },
                popEnterTransition = { slideInHorizontally { -it / 3 } + fadeIn() },
                popExitTransition = { slideOutHorizontally { it } + fadeOut() },
                modifier = Modifier.fillMaxSize()
            ) {
                composable<SettingsPage.Main> {
                    Scaffold(
                        topBar = {
                            LargeTopAppBar(
                                title = {
                                    Text(
                                        text = stringResource(R.string.top_bar_settings),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }, navigationIcon = {
                                    IconButton(onClick = onClose) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            tint = MaterialTheme.colorScheme.primary,
                                            contentDescription = stringResource(R.string.navigate_back_action)
                                        )
                                    }
                                }, scrollBehavior = scrollBehavior, modifier = focusRemoverModifier
                            )
                        },
                        modifier = modifier
                            .then(focusRemoverModifier)
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                    ) { contentPadding ->
                        LazyVerticalStaggeredGrid(
                            verticalItemSpacing = 8.dp,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            columns = StaggeredGridCells.Adaptive(350.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(contentPadding)
                                .consumeWindowInsets(contentPadding)
                                .padding(horizontal = 8.dp)
                        ) {
                            item(key = 1) {
                                MainListItem(
                                    label = stringResource(R.string.settings_app_ui_label),
                                    description = stringResource(R.string.settings_app_ui_description),
                                    icon = Icons.Default.ColorLens,
                                    animatedVisibilityScope = this@composable,
                                    onClick = { navController.navigate(SettingsPage.Theme) })
                            }
                            item(key = 2) {
                                MainListItem(
                                    label = stringResource(R.string.settings_lyrics_request_label),
                                    description = stringResource(R.string.settings_lyrics_request_description),
                                    icon = Icons.Default.Lyrics,
                                    animatedVisibilityScope = this@composable,
                                    onClick = { navController.navigate(SettingsPage.Request) })
                            }
                            item(key = 3) {
                                MainListItem(
                                    label = stringResource(R.string.settings_lyrics_storage_label),
                                    description = stringResource(R.string.settings_lyrics_storage_description),
                                    icon = Icons.Default.Storage,
                                    animatedVisibilityScope = this@composable,
                                    onClick = { navController.navigate(SettingsPage.Storage()) })
                            }
                            item(key = 4) {
                                MainListItem(
                                    label = stringResource(R.string.settings_lyrics_providers_label),
                                    description = stringResource(R.string.settings_lyrics_providers_description),
                                    icon = Icons.Default.CloudCircle,
                                    animatedVisibilityScope = this@composable,
                                    onClick = { navController.navigate(SettingsPage.LyricsProvider) })
                            }
                            item(key = 5) {
                                MainListItem(
                                    label = stringResource(R.string.settings_editor_label),
                                    description = stringResource(R.string.settings_editor_description),
                                    icon = Icons.Default.EditNote,
                                    animatedVisibilityScope = this@composable,
                                    onClick = { navController.navigate(SettingsPage.Editor) })
                            }
                            item(key = 6) {
                                MainListItem(
                                    label = stringResource(R.string.settings_filter_label),
                                    description = stringResource(R.string.settings_filter_description),
                                    icon = Icons.Default.FilterAlt,
                                    animatedVisibilityScope = this@composable,
                                    onClick = { navController.navigate(SettingsPage.Filter) })
                            }
                        }
                    }
                }
                composable<SettingsPage.Theme> {
                    AppThemeSettings(
                        viewmodel = viewmodel,
                        topbar = {
                            SettingsTopbar(
                                animatedVisibilityScope = this@composable,
                                title = res.getString(R.string.settings_app_ui_label)
                            ) { navController.popBackStack() }
                        }
                    )
                }
                composable<SettingsPage.Request> {
                    LyricsRequestSettings(
                        viewmodel = viewmodel,
                        topbar = {
                            SettingsTopbar(
                                animatedVisibilityScope = this@composable,
                                title = res.getString(R.string.settings_lyrics_request_label)
                            ) { navController.popBackStack() }
                        }
                    )
                }
                composable<SettingsPage.Storage> { backStackEntry ->
                    val navData = backStackEntry.toRoute<SettingsPage.Storage>()
                    LaunchedEffect(navData.accessRequestedPath) {
                        viewmodel.setAccessRequestedPath(navData.accessRequestedPath)
                    }
                    LyricsStorageSettings(
                        viewmodel = viewmodel,
                        topbar = {
                            SettingsTopbar(
                                animatedVisibilityScope = this@composable,
                                title = res.getString(R.string.settings_lyrics_storage_label)
                            ) { navController.popBackStack() }
                        }
                    )
                }
                composable<SettingsPage.LyricsProvider> {
                    LyricsProviderSettings(
                        viewmodel = viewmodel,
                        topbar = {
                            SettingsTopbar(
                                animatedVisibilityScope = this@composable,
                                title = res.getString(R.string.settings_lyrics_providers_label)
                            ) { navController.popBackStack() }
                        }
                    )
                }
                composable<SettingsPage.Editor> {
                    EditorSettings(
                        viewmodel = viewmodel,
                        topbar = {
                            SettingsTopbar(
                                animatedVisibilityScope = this@composable,
                                title = res.getString(R.string.settings_editor_label)
                            ) { navController.popBackStack() }
                        }
                    )
                }
                composable<SettingsPage.Filter> {
                    FilterSettings(
                        viewmodel = viewmodel,
                        topbar = {
                            SettingsTopbar(
                                animatedVisibilityScope = this@composable,
                                title = res.getString(R.string.settings_filter_label)
                            ) { navController.popBackStack() }
                        }
                    )
                }
            }
        }
    }
}
