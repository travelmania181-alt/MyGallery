package com.example.mygallery

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
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

                    val newScale = (
                        currentScale * detector.scaleFactor
                    ).coerceIn(minScale, maxScale)

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

                    if (currentScale > minScale + 0.01f) {

                        resetImage()

                    } else {

                        val targetScale = min(2.5f, maxScale)

                        val scaleFactor =
                            targetScale / currentScale

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

                override fun onDown(e: MotionEvent): Boolean {
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

        if (w > 0 && h > 0) {
            post {
                resetImage()
            }
        }
    }

    override fun setImageDrawable(drawable: android.graphics.drawable.Drawable?) {
        super.setImageDrawable(drawable)

        post {
            resetImage()
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
    }

    private fun fixTranslation() {

        val d = drawable ?: return

        val values = FloatArray(9)

        matrixValue.getValues(values)

        val scale = values[Matrix.MSCALE_X]

        val imageWidth =
            d.intrinsicWidth * scale

        val imageHeight =
            d.intrinsicHeight * scale

        var dx = values[Matrix.MTRANS_X]
        var dy = values[Matrix.MTRANS_Y]

        if (imageWidth <= width) {

            dx = (width - imageWidth) / 2f

        } else {

            dx = dx.coerceIn(
                width - imageWidth,
                0f
            )
        }

        if (imageHeight <= height) {

            dy = (height - imageHeight) / 2f

        } else {

            dy = dy.coerceIn(
                height - imageHeight,
                0f
            )
        }

        values[Matrix.MTRANS_X] = dx
        values[Matrix.MTRANS_Y] = dy

        matrixValue.setValues(values)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        gestureDetector.onTouchEvent(event)
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {

                parent?.requestDisallowInterceptTouchEvent(true)

                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_MOVE -> {

                if (
                    !scaleDetector.isInProgress &&
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

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {

                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }

        return true
    }
}
