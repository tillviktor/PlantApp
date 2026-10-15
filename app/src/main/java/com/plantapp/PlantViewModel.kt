package com.plantapp

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class PlantViewModel @JvmOverloads constructor(
    application: Application,
    private val keyStore: ApiKeyStore = ApiKeyStore(File(application.filesDir, "api-key.enc"), AndroidApiKey::get),
) : AndroidViewModel(application) {
    private val store = HistoryStore(File(application.filesDir, "recognition-history.json"))
    private val historyMutex = Mutex()
    var history by mutableStateOf<List<HistoryEntry>>(emptyList()); private set
    var historyBusy by mutableStateOf(true); private set
    var historyError by mutableStateOf<String?>(null); private set
    var historySaveError by mutableStateOf<String?>(null); private set
    private fun text(id: Int) = getApplication<Application>().getString(id)


    fun reloadHistory() { viewModelScope.launch { changeHistory { store.load() } } }
    fun deleteHistoryEntry(id: String) { viewModelScope.launch { changeHistory { store.remove(id) } } }
    private val attemptedNames = mutableSetOf<String>()
    private var namesJob: Job? = null
    var historyNamesLoading by mutableStateOf(false); private set
    var historyNamesError by mutableStateOf<String?>(null); private set
    fun retryHistoryNames() {
        if (namesJob?.isActive == true) return
        attemptedNames.clear()
        enrichHistoryNames()
    }
    fun enrichHistoryNames() {
        if (namesJob?.isActive == true) return
        val names = history.flatMap { it.results }.filterNot { it.match.namesEnriched }.map { it.match.scientificName }.distinct().filterNot { it in attemptedNames }
        if (names.isEmpty()) return
        attemptedNames += names
        namesJob = viewModelScope.launch {
            historyNamesLoading = true
            historyNamesError = null
            try {
                names.chunked(40).forEach { batch ->
                    try {
                        val aliases = withContext(Dispatchers.IO) { PlantApi.alternativeNames(batch) }
                        if (aliases.isNotEmpty()) changeHistory { store.updateAlternativeNames(aliases) }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        historyNamesError = text(R.string.history_names_unavailable)
                    }
                }
            } finally { historyNamesLoading = false }
        }
    }
    fun clearHistory() { viewModelScope.launch { changeHistory { store.clear() } } }
    private suspend fun changeHistory(operation: () -> List<HistoryEntry>): Boolean = historyMutex.withLock {
        historyBusy = true
        try {
            history = withContext(Dispatchers.IO) { operation() }
            historyError = null
            historySaveError = null
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            historyError = text(R.string.history_error)
            false
        } finally { historyBusy = false }
    }

    var apiKey by mutableStateOf(""); private set
    var keyBusy by mutableStateOf(true); private set
    var hasStoredApiKey by mutableStateOf(false); private set
    var keyStatus by mutableStateOf<String?>(null); private set

    init {
        reloadHistory()
        viewModelScope.launch {
            try {
                apiKey = withContext(Dispatchers.IO) { keyStore.load() }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                keyStatus = text(R.string.key_load_error)
            } finally { hasStoredApiKey = keyStore.exists(); keyBusy = false }
        }
    }
    fun updateApiKey(value: String) { apiKey = value; keyStatus = null }
    fun saveApiKey() = changeApiKey(false)
    fun forgetApiKey() = changeApiKey(true)
    private fun changeApiKey(forget: Boolean) {
        if (keyBusy) return
        val value = apiKey.trim()
        viewModelScope.launch {
            keyBusy = true
            try {
                withContext(Dispatchers.IO) { if (forget) keyStore.clear() else keyStore.save(value) }
                apiKey = if (forget) "" else value
                hasStoredApiKey = keyStore.exists()
                keyStatus = text(if (forget) R.string.key_forgotten else R.string.key_saved)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                keyStatus = text(R.string.key_save_error)
            } finally { keyBusy = false }
        }
    }
    fun historyPhoto(entry: HistoryEntry): File? = store.photoFile(entry)
    var photo by mutableStateOf<File?>(null); private set
    var preview by mutableStateOf<Bitmap?>(null); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var results by mutableStateOf<List<PlantResult>>(emptyList()); private set

    fun acceptPhoto(file: File) {
        if (busy) return
        viewModelScope.launch {
            busy = true; error = null; results = emptyList(); historySaveError = null
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.path, bounds)
                    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "The camera did not produce a readable photo." }
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
                    val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
                        ?: error("Unable to read the photo.")
                    val exif = ExifInterface(file)
                    val matrix = Matrix().apply {
                        if (exif.isFlipped) postScale(-1f, 1f)
                        postRotate(exif.rotationDegrees.toFloat())
                    }
                    val oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                    // Limit upload size and remove location/EXIF metadata.
                    val sanitized = File(file.parentFile, "upload-${System.nanoTime()}.jpg")
                    sanitized.outputStream().use { check(oriented.compress(Bitmap.CompressFormat.JPEG, 88, it)) }
                    file.delete()
                    sanitized to oriented
                }
                photo?.delete(); photo = bitmap.first; preview = bitmap.second
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                error = text(R.string.photo_error)
            } finally { busy = false }
        }
    }
    fun reportCameraError() { error = text(R.string.camera_error) }
    fun identify() {
        val image = photo ?: return
        if (busy || apiKey.isBlank()) return
        viewModelScope.launch {
            busy = true; error = null; results = emptyList(); historySaveError = null
            try {
                val matches = withContext(Dispatchers.IO) { PlantApi.identify(image, apiKey.trim()) }
                if (matches.isEmpty()) { error = text(R.string.no_match); return@launch }
                val recognizedAt = System.currentTimeMillis()
                results = matches.map { PlantResult(it, null) }
                var entry = HistoryEntry(recognizedAtMillis = recognizedAt, results = results)
                val saved = changeHistory { store.add(entry, image) }
                if (saved) entry = history.first { it.id == entry.id }
                else historySaveError = text(R.string.history_save_error)
                val summary = withContext(Dispatchers.IO) {
                    try { PlantApi.summary(matches.first().scientificName) }
                    catch (e: Exception) { if (e is CancellationException) throw e; null }
                }
                results = results.mapIndexed { index, result -> if (index == 0) result.copy(summary = summary) else result }
                if (summary != null && !changeHistory { store.add(entry.copy(results = results), if (saved) null else image) }) historySaveError = text(R.string.history_save_error)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                error = text(when ((e as? IdentificationException)?.reason) {
                    IdentificationFailure.NO_MATCH -> R.string.no_match
                    IdentificationFailure.KEY_REJECTED -> R.string.key_rejected
                    IdentificationFailure.QUOTA -> R.string.quota_error
                    IdentificationFailure.SERVICE -> R.string.service_error
                    null -> R.string.network_error
                })
            } finally { busy = false }
        }
    }
}
