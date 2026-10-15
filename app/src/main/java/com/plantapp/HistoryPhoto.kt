package com.plantapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

internal fun decodeHistoryPhoto(file: File?, maxEdge: Int): Bitmap? {
    require(maxEdge > 0)
    if (file == null || !file.isFile) return null
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxEdge) sample *= 2
        BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()
}

private data class PhotoLoad(val loading: Boolean = true, val bitmap: Bitmap? = null)

@Composable
private fun rememberPhoto(file: File?, maxEdge: Int): PhotoLoad {
    val result by produceState(PhotoLoad(), file?.path, maxEdge) {
        value = PhotoLoad()
        val bitmap = withContext(Dispatchers.IO) { decodeHistoryPhoto(file, maxEdge) }
        value = PhotoLoad(loading = false, bitmap = bitmap)
    }
    return result
}

@Composable
internal fun HistoryPhoto(file: File?, expanded: Boolean, onOpen: () -> Unit) {
    val image = rememberPhoto(file, if (expanded) 1600 else 512)
    val description = stringResource(if (expanded) R.string.history_photo else R.string.history_thumbnail)
    val openLabel = stringResource(R.string.open_full_photo)
    image.bitmap?.let {
        val click = if (expanded) Modifier.clickable(onClickLabel = openLabel, onClick = onOpen) else Modifier
        Image(it.asImageBitmap(), contentDescription = description,
            modifier = Modifier.fillMaxWidth().height(if (expanded) 320.dp else 144.dp).then(click), contentScale = ContentScale.Fit)
        if (expanded) Text(stringResource(R.string.tap_full_photo), style = MaterialTheme.typography.bodySmall)
    } ?: Text(stringResource(if (image.loading && file != null) R.string.loading_history_photo else R.string.no_history_photo), style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun FullPhotoViewer(file: File?, onClose: () -> Unit) {
    val photo = rememberPhoto(file, Int.MAX_VALUE)
    var zoom by remember(file?.path) { mutableStateOf(PhotoZoom()) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    fun transform(factor: Float, x: Float = 0f, y: Float = 0f) {
        val bitmap = photo.bitmap ?: return
        zoom = updatePhotoZoom(zoom, factor, x, y, viewport.width, viewport.height, bitmap.width, bitmap.height)
    }
    val gesture = rememberTransformableState { factor, pan, _ -> transform(factor, pan.x, pan.y) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().testTag("full-photo-viewer")) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.full_photo_title), color = Color.White, style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onClose, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text(stringResource(R.string.close)) }
            }
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().onSizeChanged {
                viewport = it
                zoom = PhotoZoom()
            }.transformable(gesture).pointerInput(file?.path, photo.bitmap) {
                detectTapGestures(onDoubleTap = { if (zoom.scale > 1f) zoom = PhotoZoom() else transform(2.5f) })
            }, contentAlignment = Alignment.Center) {
                photo.bitmap?.let { bitmap ->
                    Image(bitmap.asImageBitmap(), contentDescription = stringResource(R.string.full_history_photo), contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = zoom.scale; scaleY = zoom.scale; translationX = zoom.x; translationY = zoom.y })
                } ?: Text(stringResource(if (photo.loading) R.string.loading_history_photo else R.string.no_history_photo), color = Color.White)
            }
            if (photo.bitmap != null) {
                Text(stringResource(R.string.photo_zoom_percent, (zoom.scale * 100).roundToInt()), color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { transform(1.5f) }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text(stringResource(R.string.zoom_in)) }
                    TextButton(onClick = { transform(1f / 1.5f) }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text(stringResource(R.string.zoom_out)) }
                    TextButton(onClick = { zoom = PhotoZoom() }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text(stringResource(R.string.reset_zoom)) }
                }
            }
        }
    }
}
