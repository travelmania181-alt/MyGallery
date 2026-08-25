package com.example.mygallery

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.min

class ZoomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private val matrixValue = Matrix()
    private val matrixValues = FloatArray(9)

    private var minScale = 1f
    private val maxScale = 5f
    private var currentScale = 1f

    private var lastX = 0f
    private var lastY = 0f

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

            override fun onScale(detector: ScaleGestureDetector): Boolean {

                val oldScale = currentScale

                currentScale = (
                    currentScale * detector.scaleFactor
                ).coerceIn(minScale, maxScale)

                val scaleFactor = currentScale / oldScale

                matrixValue.postScale(
                    scaleFactor,
                    scaleFactor,
                    detector.focusX,
                    detector.focusY
                )

                fixTranslation()

                imageMatrix = matrixValue
                postInvalidateOnAnimation()

                return true
            }
        }
    )

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {

                if (currentScale > minScale + 0.01f) {

                    resetImage()

                } else {

                    val targetScale = min(2.5f, maxScale)
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
                    postInvalidateOnAnimation()
                }

                return true
            }
        }
    )

    init {
        scaleType = ScaleType.MATRIX
        imageMatrix = matrixValue
        isClickable = true
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int
    ) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w > 0 && h > 0 && drawable != null) {
            resetImage()
        }
    }

    override fun setImageDrawable(
        drawable: android.graphics.drawable.Drawable?
    ) {
        super.setImageDrawable(drawable)

        if (width > 0 && height > 0 && drawable != null) {
            post {
                resetImage()
            }
        }
    }

    private fun resetImage() {

        val d = drawable ?: return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val imageWidth = d.intrinsicWidth.toFloat()
        val imageHeight = d.intrinsicHeight.toFloat()

        if (
            viewWidth <= 0f ||
            viewHeight <= 0f ||
            imageWidth <= 0f ||
            imageHeight <= 0f
        ) {
            return
        }

        matrixValue.reset()

        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight

        minScale = min(scaleX, scaleY)
        currentScale = minScale

        val scaledWidth = imageWidth * minScale
        val scaledHeight = imageHeight * minScale

        val dx = (viewWidth - scaledWidth) / 2f
        val dy = (viewHeight - scaledHeight) / 2f

        matrixValue.postScale(minScale, minScale)
        matrixValue.postTranslate(dx, dy)

        imageMatrix = matrixValue
        postInvalidateOnAnimation()
    }

    private fun fixTranslation() {

        val d = drawable ?: return

        matrixValue.getValues(matrixValues)

        val scale = matrixValues[Matrix.MSCALE_X]

        val imageWidth = d.intrinsicWidth * scale
        val imageHeight = d.intrinsicHeight * scale

        var dx = matrixValues[Matrix.MTRANS_X]
        var dy = matrixValues[Matrix.MTRANS_Y]

        if (imageWidth <= width) {

            dx = (width - imageWidth) / 2f

        } else {

            val minX = width - imageWidth

            if (dx > 0f) {
                dx = 0f
            } else if (dx < minX) {
                dx = minX
            }
        }

        if (imageHeight <= height) {

            dy = (height - imageHeight) / 2f

        } else {

            val minY = height - imageHeight

            if (dy > 0f) {
                dy = 0f
            } else if (dy < minY) {
                dy = minY
            }
        }

        matrixValues[Matrix.MTRANS_X] = dx
        matrixValues[Matrix.MTRANS_Y] = dy

        matrixValue.setValues(matrixValues)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        gestureDetector.onTouchEvent(event)
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {

                lastX = event.x
                lastY = event.y

                parent?.requestDisallowInterceptTouchEvent(
                    currentScale > minScale + 0.01f
                )
            }

            MotionEvent.ACTION_MOVE -> {

                if (
                    !scaleDetector.isInProgress &&
                    currentScale > minScale + 0.01f
                ) {

                    parent?.requestDisallowInterceptTouchEvent(true)

                    val dx = event.x - lastX
                    val dy = event.y - lastY

                    if (dx != 0f || dy != 0f) {

                        matrixValue.postTranslate(dx, dy)

                        fixTranslation()

                        imageMatrix = matrixValue
                        postInvalidateOnAnimation()
                    }

                    lastX = event.x
                    lastY = event.y

                } else {

                    parent?.requestDisallowInterceptTouchEvent(false)
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {

                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {

                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }

        return true
    }
}
