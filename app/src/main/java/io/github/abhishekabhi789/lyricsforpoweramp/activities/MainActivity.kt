package io.github.abhishekabhi789.lyricsforpoweramp.activities

import android.Manifest
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import dagger.hilt.android.AndroidEntryPoint
import io.github.abhishekabhi789.lyricsforpoweramp.BuildConfig
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.FirstTimeInfoDialog
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.PermissionDialog
import io.github.abhishekabhi789.lyricsforpoweramp.ui.main.AppMain
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.ui.utils.isDarkTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.makeToast
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.MainActivityViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainActivityViewModel by viewModels()
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            val accent by viewModel.accentColor.collectAsStateWithLifecycle()
            val useDarkTheme = isDarkTheme(theme = appTheme)
            val firstTimeInfoShown by viewModel.firstTimeInfo.collectAsStateWithLifecycle()
            var readyToShowFirstTimeInfo by rememberSaveable { mutableStateOf(false) }
            LyricsForPowerAmpTheme(
                useDarkTheme = useDarkTheme,
                amoled = appTheme == AppTheme.Amoled,
                accent = accent
            ) {
                /* should not ask from here if user disabled notifications from settings*/
                val shouldAskForNotificationPermission by viewModel.showNotification.collectAsStateWithLifecycle()
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
                                viewModel.setShowNotification(false)
                                makeToast(R.string.settings_permission_toast_notification_disabled)
                            }
                            showPermissionDialog = false
                            readyToShowFirstTimeInfo = true
                        }
                    )
                }
                if (!BuildConfig.DEBUG && readyToShowFirstTimeInfo && !firstTimeInfoShown)
                    FirstTimeInfoDialog(onDismiss = { viewModel.setFirstTimeInfoShown(true) })
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    LaunchedEffect(Unit) {
                        viewModel.updateLaunchIntent(intent)
                    }
                    AppMain(viewModel = viewModel)
                }
            }
        }
    }


    companion object {
        const val TAG = "MainActivity"
    }
}
