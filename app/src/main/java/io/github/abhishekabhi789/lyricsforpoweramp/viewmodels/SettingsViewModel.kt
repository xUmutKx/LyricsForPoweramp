package io.github.abhishekabhi789.lyricsforpoweramp.viewmodels

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.translation.Translator
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val appPreference: AppPreference) :
    ViewModel() {
    private val _themeState = MutableStateFlow(appPreference.getTheme())
    val themeState: StateFlow<AppPreference.AppTheme> = _themeState.asStateFlow()

    private val _accessRequestedPath: MutableStateFlow<Uri?> = MutableStateFlow(null)
    val accessRequestedPath = _accessRequestedPath.asStateFlow()

    fun updateTheme(newTheme: AppPreference.AppTheme) {
        _themeState.value = newTheme
        appPreference.setTheme(newTheme)
    }

    fun setAccessRequestedPath(path: String?) {
        _accessRequestedPath.update {
            if (path.isNullOrBlank()) null else getStorageUriFromPath(path)
        }
    }

    private val _fallbackSearch = MutableStateFlow(appPreference.getSearchIfGetFailed())
    val fallbackToSearch = _fallbackSearch.asStateFlow()
    fun setFallbackToSearchMode(enabled: Boolean) {
        appPreference.setSearchIfGetFailed(enabled)
        _fallbackSearch.value = enabled
    }

    private val _showNotification = MutableStateFlow(appPreference.getShowNotification())
    val showNotification = _showNotification.asStateFlow()
    fun setShowNotification(enabled: Boolean) {
        appPreference.setShowNotification(enabled)
        _showNotification.value = enabled
    }

    private val _overwriteNotification = MutableStateFlow(appPreference.getOverwriteNotification())
    val overwriteNotification = _overwriteNotification.asStateFlow()
    fun setOverwriteNotification(enabled: Boolean) {
        appPreference.setOverwriteNotification(enabled)
        _overwriteNotification.value = enabled
    }

    private val _preferredLyricsType = MutableStateFlow(appPreference.getPreferredLyricsType())
    val preferredLyricsType = _preferredLyricsType.asStateFlow()
    fun setPreferredLyricsType(type: LyricsType) {
        appPreference.setPreferredLyricsType(type)
        _preferredLyricsType.value = type
    }

    private val _markInstrumental = MutableStateFlow(appPreference.getMarkInstrumental())
    val getMarkInstrumental = _markInstrumental.asStateFlow()
    fun setMarkInstrumental(enabled: Boolean) {
        appPreference.setMarkInstrumental(enabled)
        _markInstrumental.value = enabled
    }

    private val _sendLyricsToPoweramp = MutableStateFlow(appPreference.getSendLyricsToPoweramp())
    val sendLyricsToPoweramp = _sendLyricsToPoweramp.asStateFlow()
    fun setSendLyricsToPoweramp(enabled: Boolean) {
        appPreference.setSendLyricsToPoweramp(enabled)
        _sendLyricsToPoweramp.value = enabled
    }

    private val _saveAsFile = MutableStateFlow(appPreference.getSaveAsFile())
    val saveAsFile = _saveAsFile.asStateFlow()
    fun setSaveAsFile(enabled: Boolean) {
        appPreference.setSaveAsFile(enabled)
        _saveAsFile.value = enabled
    }

    private val _saveIdTagsInFile = MutableStateFlow(appPreference.getSaveIdTagsInFile())
    val saveIdTagsInFile = _saveIdTagsInFile.asStateFlow()
    fun setSaveIdTagsInFile(enabled: Boolean) {
        appPreference.setSaveIdTagsInFile(enabled)
        _saveIdTagsInFile.value = enabled
    }

    private val _embedLyricsIntoFile = MutableStateFlow(appPreference.getEmbedLyricsAsTag())
    val embedLyricsIntoFile = _embedLyricsIntoFile.asStateFlow()
    fun setEmbedLyricsIntoFile(enabled: Boolean) {
        appPreference.setEmbedLyricsAsTag(enabled)
        _embedLyricsIntoFile.value = enabled
    }

    private val _fixMetadata = MutableStateFlow(appPreference.getFixMetadata())
    val fixMetadata = _fixMetadata.asStateFlow()
    fun setFixMetadata(enabled: Boolean) {
        appPreference.setFixMetadata(enabled)
        _fixMetadata.value = enabled
    }

    private val _savedUris = MutableStateFlow(appPreference.getSavedUris())
    val savedUris = _savedUris.asStateFlow()
    fun saveNewUri(uri: Uri) {
        appPreference.saveFolderUri(uri)
        _savedUris.value.toMutableSet().apply { add(uri) }.distinct().toList().let {
            _savedUris.value = it
        }
        setAccessRequestedPath(null)
    }

    fun removeUri(uri: Uri) {
        appPreference.removeSavedFolder(uri)
        _savedUris.value.toMutableSet().apply { remove(uri) }.distinct().let {
            _savedUris.value = it
        }
    }

    private val _timestampDelta = MutableStateFlow(appPreference.getTimestampDelta())
    val timestampDelta = _timestampDelta.asStateFlow()
    fun setTimestampDelta(value: Int) {
        _timestampDelta.value = value
        appPreference.setTimestampDelta(value)
    }

    private val _translationApiKey =
        MutableStateFlow(Translator.entries.associateWith { appPreference.getTranslationApiKey(it) })
    val translationApiKey = _translationApiKey.asStateFlow()
    fun setTranslationApiKey(translator: Translator, apiKey: String) {
        _translationApiKey.update { map -> map.toMutableMap().apply { set(translator, apiKey) } }
        appPreference.setTranslatorApiKey(apiKey, translator)
    }

    private val _filters =
        MutableStateFlow(AppPreference.FilterType.entries.associateWith {
            appPreference.getFilter(it).lines()
        })
    val filters = _filters.asStateFlow()
    fun setFilter(type: AppPreference.FilterType, value: List<String>) {
        _filters.update { map -> map.toMutableMap().apply { set(type, value) } }
        appPreference.setFilter(
            type,
            value.let { if (it.isEmpty()) null else it.joinToString("\n") })
    }

    private fun getStorageUriFromPath(path: String): Uri {
        val baseUri = "content://com.android.externalstorage.documents/tree/"
        return when {
            path.startsWith("primary/") -> {
                val subPath = path.removePrefix("primary/")
                (baseUri + Uri.encode("primary:$subPath")).toUri()
            }

            else -> {
                val storageId = path.substringBefore(File.separatorChar)
                val subPath = path.removePrefix("$storageId${File.separator}")
                (baseUri + Uri.encode("$storageId:$subPath")).toUri()
            }
        }
    }
}
