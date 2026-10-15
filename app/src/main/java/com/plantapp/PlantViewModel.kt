package com.plantapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PlantResult(val match: PlantMatch, val summary: String?)
class PlantViewModel : ViewModel() {
    var apiKey by mutableStateOf("")
    var photo by mutableStateOf<File?>(null); private set
    var preview by mutableStateOf<Bitmap?>(null); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var results by mutableStateOf<List<PlantResult>>(emptyList()); private set

    fun acceptPhoto(file: File) {
        if (busy) return
        viewModelScope.launch {
            busy = true; error = null; results = emptyList()
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
                error = "Unable to prepare the photo. Please take another picture."
            } finally { busy = false }
        }
    }
    fun reportCameraError() { error = "No camera app is available on this device." }
    fun identify() {
        val image = photo ?: return
        if (busy || apiKey.isBlank()) return
        viewModelScope.launch {
            busy = true; error = null; results = emptyList()
            try {
                val matches = withContext(Dispatchers.IO) { PlantApi.identify(image, apiKey.trim()) }
                if (matches.isEmpty()) { error = "No match found. Try another leaf or flower."; return@launch }
                results = matches.map { PlantResult(it, null) }
                val summary = withContext(Dispatchers.IO) {
                    try { PlantApi.summary(matches.first().scientificName) }
                    catch (e: Exception) { if (e is CancellationException) throw e; null }
                }
                results = results.mapIndexed { index, result -> if (index == 0) result.copy(summary = summary) else result }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                error = if (e is IllegalStateException || e is IllegalArgumentException) e.message else "Couldn't connect. Check your internet connection and try again."
            } finally { busy = false }
        }
    }
}
