package com.plantapp

import org.junit.Assert.*
import org.junit.Test

class PhotoZoomTest {
    @Test fun zoomIsLimitedAndOffsetsCannotMoveImageOutsideViewport() {
        val zoom = updatePhotoZoom(PhotoZoom(), 20f, 99999f, -99999f, 400, 400, 1600, 1200)
        assertEquals(5f, zoom.scale, 0f)
        assertEquals(800f, zoom.x, 0f)
        assertEquals(-550f, zoom.y, 0f)
        assertEquals(PhotoZoom(), updatePhotoZoom(zoom, 0.01f, 0f, 0f, 400, 400, 1600, 1200))
    }
    @Test fun fittedLandscapeImageCannotPanVerticallyUntilItFillsViewport() {
        assertEquals(PhotoZoom(), updatePhotoZoom(PhotoZoom(), 1f, 500f, 500f, 400, 800, 1600, 800))
        val zoom = updatePhotoZoom(PhotoZoom(), 2f, 500f, 500f, 400, 800, 1600, 800)
        assertEquals(200f, zoom.x, 0f)
        assertEquals(0f, zoom.y, 0f)
    }
    @Test fun unavailableDimensionsAndInvalidGesturesAreSafe() {
        val current = PhotoZoom(2f, 0f, 0f)
        assertEquals(PhotoZoom(), updatePhotoZoom(current, 1f, 0f, 0f, 0, 800, 1600, 1200))
        assertEquals(current, updatePhotoZoom(current, Float.NaN, 0f, 0f, 400, 800, 1600, 1200))
    }
}
