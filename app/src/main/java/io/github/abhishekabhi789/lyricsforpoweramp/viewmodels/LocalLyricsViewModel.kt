package io.github.abhishekabhi789.lyricsforpoweramp.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.BulkLyricsDownloader
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.LocalLyricsIndexer
import io.github.abhishekabhi789.lyricsforpoweramp.model.LocalLyricsEntry
import io.github.abhishekabhi789.lyricsforpoweramp.model.LocalLyricsMatch
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.utils.LocalLyricsSearch
import io.github.abhishekabhi789.lyricsforpoweramp.utils.MIN_SEARCH_QUERY_LENGTH
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LocalLyricsViewModel @Inject constructor(
    private val appPreference: AppPreference,
    private val indexer: LocalLyricsIndexer,
    private val bulkDownloader: BulkLyricsDownloader
) : ViewModel() {

    sealed interface IndexState {
        /** No folder picked yet. */
        data object NoFolder : IndexState
        data class Scanning(val found: Int) : IndexState
        data class Ready(val count: Int) : IndexState
        data object Failed : IndexState
    }

    val appTheme = appPreference.appTheme
    val accentColor = appPreference.accentColor

    val folder = appPreference.localLyricsFolder
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val playFromMatchedLine = appPreference.playFromMatchedLine
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setPlayFromMatchedLine(enabled: Boolean) {
        viewModelScope.launch {
            appPreference.setPreference(AppPreference.PLAY_FROM_MATCHED_LINE, enabled)
        }
    }

    private val _indexState = MutableStateFlow<IndexState>(IndexState.NoFolder)
    val indexState = _indexState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _results = MutableStateFlow<List<LocalLyricsMatch>>(emptyList())
    val results = _results.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    /** All indexed songs, alphabetical - shown as-is while the search box is empty. */
    private val _entries = MutableStateFlow<List<LocalLyricsEntry>>(emptyList())
    val browseEntries = _entries.asStateFlow()
    private val entries: List<LocalLyricsEntry> get() = _entries.value

    private val _bulkDownload = MutableStateFlow<BulkLyricsDownloader.Progress?>(null)
    val bulkDownload = _bulkDownload.asStateFlow()

    private var scanJob: Job? = null
    private var bulkDownloadJob: Job? = null

    init {
        viewModelScope.launch {
            folder.collectLatest { uri ->
                if (uri == null) {
                    _entries.value = emptyList()
                    _results.value = emptyList()
                    _indexState.value = IndexState.NoFolder
                } else {
                    loadThenRescan(uri)
                }
            }
        }
        observeQuery()
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() {
        viewModelScope.launch {
            _query.debounce(SEARCH_DEBOUNCE_MS).collectLatest { query ->
                if (query.trim().length < MIN_SEARCH_QUERY_LENGTH) {
                    _isSearching.value = false
                    _results.value = emptyList()
                    return@collectLatest
                }
                _isSearching.value = true
                val matches = withContext(Dispatchers.Default) {
                    LocalLyricsSearch.search(entries, query)
                }
                _results.value = matches
                _isSearching.value = false
            }
        }
    }

    private fun loadThenRescan(uri: Uri) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            val cached = indexer.loadCache(uri)
            if (cached.isNotEmpty()) {
                _entries.value = sortedByTitle(cached)
                _indexState.value = IndexState.Ready(cached.size)
                refreshResults()
            } else {
                _indexState.value = IndexState.Scanning(0)
            }
            runCatching {
                indexer.buildIndex(uri, known = cached) { found ->
                    if (cached.isEmpty()) _indexState.value = IndexState.Scanning(found)
                }
            }.onSuccess { scanned ->
                _entries.value = sortedByTitle(scanned)
                _indexState.value = IndexState.Ready(scanned.size)
                refreshResults()
            }.onFailure { error ->
                Log.e(TAG, "loadThenRescan: indexing failed", error)
                if (entries.isEmpty()) _indexState.value = IndexState.Failed
                else _indexState.value = IndexState.Ready(entries.size)
            }
        }
    }

    private suspend fun refreshResults() {
        val query = _query.value
        if (query.trim().length < MIN_SEARCH_QUERY_LENGTH) return
        _results.value = withContext(Dispatchers.Default) {
            LocalLyricsSearch.search(entries, query)
        }
    }

    private fun sortedByTitle(list: List<LocalLyricsEntry>) =
        list.sortedBy { LocalLyricsSearch.trLower(it.title) }

    fun setFolder(uri: Uri) {
        viewModelScope.launch {
            indexer.clearCache()
            appPreference.setPreference(AppPreference.LOCAL_LYRICS_FOLDER, uri.toString())
        }
    }

    fun rescan() {
        folder.value?.let { uri ->
            scanJob?.cancel()
            scanJob = viewModelScope.launch {
                _indexState.value = IndexState.Scanning(0)
                runCatching {
                    indexer.buildIndex(uri, known = entries) { found ->
                        _indexState.value = IndexState.Scanning(found)
                    }
                }.onSuccess {
                    _entries.value = sortedByTitle(it)
                    _indexState.value = IndexState.Ready(it.size)
                    refreshResults()
                }.onFailure { error ->
                    Log.e(TAG, "rescan: failed", error)
                    _indexState.value = IndexState.Failed
                }
            }
        }
    }

    fun updateQuery(query: String) {
        _query.value = query
    }

    /** Searches LRCLIB for every song in the folder that has no lyrics file yet, and saves the hits. */
    fun startBulkDownload() {
        val uri = folder.value ?: return
        if (bulkDownloadJob?.isActive == true) return
        bulkDownloadJob = viewModelScope.launch {
            bulkDownloader.run(uri) { progress -> _bulkDownload.value = progress }
            rescan()
        }
    }

    fun cancelBulkDownload() {
        bulkDownloadJob?.cancel()
        bulkDownloadJob = null
    }

    fun dismissBulkDownloadResult() {
        _bulkDownload.value = null
    }

    companion object {
        private const val TAG = "LocalLyricsViewModel"
        private const val SEARCH_DEBOUNCE_MS = 250L
    }
}
