package io.github.abhishekabhi789.lyricsforpoweramp.utils

import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.InterpreterMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.edit
import androidx.core.net.toUri
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.translation.Translator
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class AppPreference @Inject constructor(
    @Named(OTHER_PREF) private val appPreferenceProvider: Provider<SharedPreferences>,
    @Named(UI_PREF_NAME) private val uiPreferenceProvider: Provider<SharedPreferences>,
    @Named(FILTER_PREF_NAME) private val filterPreferenceProvider: Provider<SharedPreferences>
) {
    private val appPreference by lazy { appPreferenceProvider.get() }
    private val uiPreference by lazy { uiPreferenceProvider.get() }
    private val filterPreference by lazy { filterPreferenceProvider.get() }

    fun getFilter(filterType: FilterType): String {
        return filterPreference.getString(filterType.key, "") ?: ""
    }

    fun setFilter(filterType: FilterType, value: String?) {
        filterPreference.edit { putString(filterType.key, value) }
    }

    fun getFirstTimeInfoShown(): Boolean {
        return appPreference.getBoolean(FIRST_TIME_INFO_SHOWN, false)
    }

    fun setFirstTimeInfoShown(value: Boolean) {
        appPreference.edit { putBoolean(FIRST_TIME_INFO_SHOWN, value) }
    }

    fun getTheme(): AppTheme {
        val defaultTheme =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) AppTheme.Auto else AppTheme.Light
        val preferredTheme = uiPreference.getString(UI_THEME_KEY, defaultTheme.name)
        return AppTheme.valueOf(preferredTheme ?: defaultTheme.name)
    }


    fun setTheme(theme: AppTheme) {
        uiPreference.edit { putString(UI_THEME_KEY, theme.name) }
    }

    fun getSearchIfGetFailed(): Boolean {
        return appPreference.getBoolean(SEARCH_IF_GET_FAILED, false)
    }

    fun setSearchIfGetFailed(choice: Boolean) {
        appPreference.edit { putBoolean(SEARCH_IF_GET_FAILED, choice) }
    }

    fun getShowNotification(): Boolean {
        return appPreference.getBoolean(SHOW_LYRICS_REQUEST_NOTIFICATION, true)
    }

    fun setShowNotification(choice: Boolean) {
        appPreference.edit { putBoolean(SHOW_LYRICS_REQUEST_NOTIFICATION, choice) }
    }

    fun getOverwriteNotification(): Boolean {
        return appPreference.getBoolean(OVERWRITE_NOTIFICATION, false)
    }

    fun setOverwriteNotification(choice: Boolean) {
        appPreference?.edit { putBoolean(OVERWRITE_NOTIFICATION, choice) }
    }

    fun getPreferredLyricsType(): LyricsType {
        val defaultType = LyricsType.SYNCED
        val preferredType = appPreference.getString(PREFERRED_LYRICS_TYPE, defaultType.name)
        return LyricsType.valueOf(preferredType ?: defaultType.name)
    }

    fun setPreferredLyricsType(type: LyricsType) {
        appPreference.edit { putString(PREFERRED_LYRICS_TYPE, type.name) }
    }

    fun getMarkInstrumental(): Boolean {
        return appPreference.getBoolean(MARK_INSTRUMENTAL_LYRICS, false)
    }

    fun setMarkInstrumental(choice: Boolean) {
        appPreference.edit { putBoolean(MARK_INSTRUMENTAL_LYRICS, choice) }
    }

    fun getSendLyricsToPoweramp(): Boolean {
        return appPreference.getBoolean(SEND_LYRICS_TO_POWERAMP, true)
    }

    fun setSendLyricsToPoweramp(value: Boolean) {
        appPreference.edit { putBoolean(SEND_LYRICS_TO_POWERAMP, value) }
    }


    fun getSaveAsFile(): Boolean {
        return appPreference.getBoolean(SAVE_LYRICS_IN_FILE, false)
    }

    fun setSaveAsFile(value: Boolean) {
        appPreference.edit { putBoolean(SAVE_LYRICS_IN_FILE, value) }
    }

    fun getSaveIdTagsInFile(): Boolean {
        return appPreference.getBoolean(SAVE_ID_TAGS_IN_FILE, false)
    }

    fun setSaveIdTagsInFile(value: Boolean) {
        appPreference.edit { putBoolean(SAVE_ID_TAGS_IN_FILE, value) }
    }

    fun getEmbedLyricsAsTag(): Boolean {
        return appPreference.getBoolean(EMBED_LYRICS_AS_TAG, false)
    }

    fun setEmbedLyricsAsTag(value: Boolean) {
        appPreference.edit { putBoolean(EMBED_LYRICS_AS_TAG, value) }
    }

    fun getFixMetadata(): Boolean {
        return appPreference.getBoolean(FIX_METADATA, false)
    }

    fun setFixMetadata(value: Boolean) {
        appPreference.edit { putBoolean(FIX_METADATA, value) }
    }

    fun getSavedUris(): List<Uri> {
        return appPreference.getStringSet(FOLDER_URIS, emptySet<String>())
            ?.map { it.toUri() } ?: emptyList()
    }

    fun saveFolderUri(uri: Uri) {
        val savedUri = getSavedUris().toMutableSet().apply { add(uri) }
        val uriStringSet = savedUri.map { it.toString() }.toSet()
        appPreference.edit { putStringSet(FOLDER_URIS, uriStringSet) }
    }

    fun removeSavedFolder(uri: Uri): Boolean {
        val savedUris = getSavedUris().toMutableSet()
        val success = savedUris.remove(uri)
        savedUris.map { it.toString() }.let {
            appPreference.edit { putStringSet(FOLDER_URIS, it.toSet()) }
        }
        return success
    }

    fun setTranslatorApiKey(apiKey: String, translator: Translator) {
        val prefKey = when (translator) {
            Translator.GEMINI -> GEMINI_API_KEY
        }
        appPreference.edit { putString(prefKey, apiKey) }
    }

    fun getTranslationApiKey(translator: Translator): String {
        val prefKey = when (translator) {
            Translator.GEMINI -> GEMINI_API_KEY
        }
        return appPreference.getString(prefKey, null) ?: ""
    }

    fun setTimestampDelta(deltaInCentiseconds: Int) {
        appPreference.edit { putInt(TIMESTAMP_DELTA, deltaInCentiseconds) }
    }

    fun getTimestampDelta(): Int {
        return appPreference.getInt(TIMESTAMP_DELTA, 10)
    }

    fun setEditorFontSize(fontSize: Float) {
        appPreference.edit { putFloat(EDITOR_FONT_SIZE_SP, fontSize) }
    }

    fun getEditorFontSize(): Float? {
        return appPreference.getFloat(EDITOR_FONT_SIZE_SP, 0f).takeIf { it > 0f }
    }

    enum class AppTheme(val labelResId: Int) {
        Auto(R.string.settings_theme_auto_label),
        Light(R.string.settings_theme_light_label),
        Dark(R.string.settings_theme_dark_label)
    }

    enum class FilterType(val key: String, val labelResId: Int, val icon: ImageVector) {
        TITLE_FILTER("title_filter", R.string.settings_filter_title_label, Icons.Default.MusicNote),
        ARTISTS_FILTER(
            "artists_filter", R.string.settings_filter_artists_label, Icons.Default.InterpreterMode
        ),
        ALBUM_FILTER("album_filter", R.string.settings_filter_album_label, Icons.Default.Album),
    }

    companion object {
        const val FILTER_PREF_NAME = "filter_preference"
        const val UI_PREF_NAME = "ui_preference"
        const val OTHER_PREF = "other_preference"
        private const val FIRST_TIME_INFO_SHOWN = "first_time_info_shown"
        private const val UI_THEME_KEY = "app_theme"
        private const val SEARCH_IF_GET_FAILED = "perform_search_if_get_failed"
        private const val SHOW_LYRICS_REQUEST_NOTIFICATION = "lyrics_requests_show_notification"
        private const val OVERWRITE_NOTIFICATION = "lyrics_requests_overwrite_existing_notification"
        private const val PREFERRED_LYRICS_TYPE = "preferred_lyrics_type"
        private const val SEND_LYRICS_TO_POWERAMP = "send_lyrics_to_poweramp"
        private const val SAVE_LYRICS_IN_FILE = "save_lyrics_in_file"
        private const val SAVE_ID_TAGS_IN_FILE = "save_id_tags_in_file"
        private const val EMBED_LYRICS_AS_TAG = "embed_lyrics_as_tag"
        private const val FIX_METADATA = "fix_metadata_from_result"
        private const val FOLDER_URIS = "folder_uri_list"
        private const val MARK_INSTRUMENTAL_LYRICS = "mark_instrumental_lyrics"
        private const val GEMINI_API_KEY = "ai_key_gemini"
        private const val TIMESTAMP_DELTA = "timestamp_delta_in_centi_seconds"
        private const val EDITOR_FONT_SIZE_SP = "editor_font_size_sp"
        fun getThemes(): List<AppTheme> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) AppTheme.entries.toList()
            else listOf(AppTheme.Light, AppTheme.Dark)
        }
    }
}
