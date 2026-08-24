package com.example.mygallery

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.max
import kotlin.math.min

class ZoomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private val matrixValue = Matrix()

    private var minScale = 1f
    private var maxScale = 5f
    private var currentScale = 1f

    private var lastX = 0f
    private var lastY = 0f

    private val scaleDetector =
        ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val newScale =
                        (currentScale * detector.scaleFactor)
                            .coerceIn(minScale, maxScale)

                    val scaleFactor = newScale / currentScale

                    matrixValue.postScale(
                        scaleFactor,
                        scaleFactor,
                        detector.focusX,
                        detector.focusY
                    )

                    currentScale = newScale
                    fixTranslation()

                    imageMatrix = matrixValue

                    return true
                }
            }
        )

    private val gestureDetector =
        GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onDoubleTap(e: MotionEvent): Boolean {

                    if (currentScale > minScale) {
                        resetImage()
                    } else {
                        val targetScale = 2.5f
                        val scaleFactor = targetScale / currentScale

                        matrixValue.postScale(
                            scaleFactor,
                            scaleFactor,
                            e.x,
                            e.y
                        )

                        currentScale = targetScale
                        fixTranslation()

                        imageMatrix = matrixValue
                    }

                    return true
                }
            }
        )

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int
    ) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w > 0 && h > 0) {
            post {
                resetImage()
            }
        }
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)

        post {
            resetImage()
        }
    }

    private fun resetImage() {

        val drawable = drawable ?: return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        if (viewWidth <= 0f || viewHeight <= 0f) return

        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()

        if (imageWidth <= 0f || imageHeight <= 0f) return

        matrixValue.reset()

        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight

        // FIT_CENTER: show the complete image
        minScale = min(scaleX, scaleY)
        currentScale = minScale

        val scaledWidth = imageWidth * minScale
        val scaledHeight = imageHeight * minScale

        val dx = (viewWidth - scaledWidth) / 2f
        val dy = (viewHeight - scaledHeight) / 2f

        matrixValue.postScale(minScale, minScale)
        matrixValue.postTranslate(dx, dy)

        imageMatrix = matrixValue
    }

    private fun fixTranslation() {

        val drawable = drawable ?: return

        val values = FloatArray(9)
        matrixValue.getValues(values)

        val currentMatrixScale = values[Matrix.MSCALE_X]

        val imageWidth =
            drawable.intrinsicWidth * currentMatrixScale

        val imageHeight =
            drawable.intrinsicHeight * currentMatrixScale

        var dx = 0f
        var dy = 0f

        val rect = RectF(
            0f,
            0f,
            drawable.intrinsicWidth.toFloat(),
            drawable.intrinsicHeight.toFloat()
        )

        matrixValue.mapRect(rect)

        if (imageWidth <= width) {
            dx = width / 2f - rect.centerX()
        } else {
            if (rect.left > 0) dx = -rect.left
            if (rect.right < width) dx = width - rect.right
        }

        if (imageHeight <= height) {
            dy = height / 2f - rect.centerY()
        } else {
            if (rect.top > 0) dy = -rect.top
            if (rect.bottom < height) dy = height - rect.bottom
        }

        matrixValue.postTranslate(dx, dy)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        gestureDetector.onTouchEvent(event)
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_MOVE -> {

                if (!scaleDetector.isInProgress &&
                    currentScale > minScale
                ) {

                    val dx = event.x - lastX
                    val dy = event.y - lastY

                    matrixValue.postTranslate(dx, dy)
                    fixTranslation()

                    imageMatrix = matrixValue

                    lastX = event.x
                    lastY = event.y
                }
            }
        }

        return true
    }
}
