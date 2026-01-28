package io.github.abhishekabhi789.lyricsforpoweramp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.maxmpz.poweramp.player.PowerampAPI
import dagger.hilt.android.AndroidEntryPoint
import io.github.abhishekabhi789.lyricsforpoweramp.BuildConfig
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PowerampApiHelper
import io.github.abhishekabhi789.lyricsforpoweramp.model.InputState
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.FirstTimeInfoDialog
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.PermissionDialog
import io.github.abhishekabhi789.lyricsforpoweramp.ui.main.AppMain
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.utils.makeToast
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.MainActivityViewModel
import io.github.abhishekabhi789.lyricsforpoweramp.workers.LyricsRequestWorker.Companion.MANUAL_SEARCH_ACTION

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    @SuppressLint("InlinedApi")
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LaunchedEffect(Unit) {
                viewModel.updateTheme(AppPreference.getTheme(this@MainActivity))
            }
            val appTheme by viewModel.appTheme.collectAsState()
            val useDarkTheme = AppPreference.isDarkTheme(theme = appTheme)
            var firstTimeInfoShown by rememberSaveable {
                mutableStateOf(AppPreference.getFirstTimeInfoShown(this@MainActivity))
            }
            var readyToShowFirstTimeInfo by rememberSaveable { mutableStateOf(false) }
            LyricsForPowerAmpTheme(useDarkTheme = useDarkTheme) {
                /* should not ask from here if user disabled notifications from settings*/
                val shouldAskForNotificationPermission = rememberSaveable {
                    AppPreference.getShowNotification(this@MainActivity)
                }
                val permissionState = rememberPermissionState(
                    permission = Manifest.permission.POST_NOTIFICATIONS
                ) { isGranted ->
                    @StringRes val message =
                        if (isGranted) R.string.settings_permission_toast_granted
                        else R.string.settings_permission_toast_denied
                    makeToast(message)
                    readyToShowFirstTimeInfo = true
                }
                var showPermissionDialog by rememberSaveable { mutableStateOf(!permissionState.status.isGranted) }
                if (shouldAskForNotificationPermission && showPermissionDialog) {
                    PermissionDialog(
                        explanation = stringResource(R.string.settings_notification_permission_description),
                        allowToSuppress = true,
                        onConfirm = {
                            showPermissionDialog = false
                            if (permissionState.status.shouldShowRationale) {
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).run {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                                    startActivity(this)
                                }
                            } else {
                                permissionState.launchPermissionRequest()
                            }
                        },
                        onDismiss = { disableNotification ->
                            if (disableNotification) {
                                AppPreference.setShowNotification(this@MainActivity, false)
                                makeToast(R.string.settings_permission_toast_notification_disabled)
                            }
                            showPermissionDialog = false
                            readyToShowFirstTimeInfo = true
                        }
                    )
                }
                if (!BuildConfig.DEBUG && readyToShowFirstTimeInfo && !firstTimeInfoShown) FirstTimeInfoDialog {
                    AppPreference.setFirstTimeInfoShown(this@MainActivity, true)
                    firstTimeInfoShown = true
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    when (intent?.action) {
                        PowerampAPI.Lyrics.ACTION_LYRICS_LINK, MANUAL_SEARCH_ACTION -> {
                            PowerampApiHelper.makeTrack(this, intent)?.let { track ->
                                viewModel.updateInputState(
                                    InputState(
                                        queryString = track.trackName,
                                        queryTrack = track,
                                        searchMode = if (track.artistName.isNullOrEmpty() && track.albumName.isNullOrEmpty())
                                            InputState.SearchMode.Coarse else InputState.SearchMode.Fine
                                    )
                                )
                            }
                        }
                    }
                    AppMain(viewModel = viewModel)
                }
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        val preferredTheme = AppPreference.getTheme(this)
        viewModel.updateTheme(preferredTheme)
    }

    companion object {
        const val TAG = "MainActivity"
    }
}
