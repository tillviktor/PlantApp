package com.plantapp

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HistoryPhotoDecodeTest {
    @get:Rule val folder = TemporaryFolder()
    @Test fun thumbnailIsSampledAndDetailsAndViewerRetainSavedResolution() {
        val file = File(folder.root, "photo.jpg")
        val bitmap = Bitmap.createBitmap(1600, 1200, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }; bitmap.recycle()
        val thumbnail = requireNotNull(decodeHistoryPhoto(file, 512))
        val details = requireNotNull(decodeHistoryPhoto(file, 1600))
        val full = requireNotNull(decodeHistoryPhoto(file, Int.MAX_VALUE))
        assertEquals(400, thumbnail.width)
        assertEquals(1600, details.width); assertEquals(1200, details.height)
        assertEquals(1600, full.width); assertEquals(1200, full.height)
        thumbnail.recycle(); details.recycle(); full.recycle()
    }
    @Test fun absentPhotosDoNotCrashTheViewer() {
        assertNull(decodeHistoryPhoto(null, 512))
        assertNull(decodeHistoryPhoto(File(folder.root, "missing.jpg"), Int.MAX_VALUE))
    }
}
