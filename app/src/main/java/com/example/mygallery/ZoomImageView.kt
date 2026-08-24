package com.example.mygallery

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

class ZoomImageView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    AppCompatImageView(context, attrs) {
    private val matrixValue = Matrix()
    private var scale = 1f
    private var lastX = 0f
    private var lastY = 0f
    private val scaler = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scale = (scale * detector.scaleFactor).coerceIn(1f, 5f)
            matrixValue.setScale(scale, scale, width / 2f, height / 2f)
            imageMatrix = matrixValue
            return true
        }
    })
    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            scale = if (scale > 1f) 1f else 2.5f
            matrixValue.setScale(scale, scale, e.x, e.y)
            imageMatrix = matrixValue
            return true
        }
    })
    init { scaleType = ScaleType.MATRIX }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestures.onTouchEvent(event); scaler.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lastX = event.x; lastY = event.y }
            MotionEvent.ACTION_MOVE -> if (!scaler.isInProgress && scale > 1f) {
                matrixValue.postTranslate(event.x - lastX, event.y - lastY)
                imageMatrix = matrixValue
                lastX = event.x; lastY = event.y
            }
        }
        return true
    }
}
