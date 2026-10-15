package com.plantapp

import kotlin.math.min

internal data class PhotoZoom(val scale: Float = 1f, val x: Float = 0f, val y: Float = 0f)

internal fun updatePhotoZoom(
    current: PhotoZoom, factor: Float, panX: Float, panY: Float,
    viewportWidth: Int, viewportHeight: Int, imageWidth: Int, imageHeight: Int,
): PhotoZoom {
    if (viewportWidth <= 0 || viewportHeight <= 0 || imageWidth <= 0 || imageHeight <= 0) return PhotoZoom()
    if (!factor.isFinite() || !panX.isFinite() || !panY.isFinite()) return current
    val scale = (current.scale * factor).coerceIn(1f, 5f)
    if (scale == 1f) return PhotoZoom()
    val fit = min(viewportWidth.toFloat() / imageWidth, viewportHeight.toFloat() / imageHeight)
    val maxX = ((imageWidth * fit * scale - viewportWidth) / 2f).coerceAtLeast(0f)
    val maxY = ((imageHeight * fit * scale - viewportHeight) / 2f).coerceAtLeast(0f)
    val ratio = scale / current.scale
    return PhotoZoom(scale, (current.x * ratio + panX).coerceIn(-maxX, maxX), (current.y * ratio + panY).coerceIn(-maxY, maxY))
}
