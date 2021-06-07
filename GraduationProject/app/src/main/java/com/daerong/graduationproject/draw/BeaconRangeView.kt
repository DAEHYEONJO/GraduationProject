package com.daerong.graduationproject.draw

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageView

class BeaconRangeView(context: Context, attrs:AttributeSet?) : AppCompatImageView(context,attrs) {
    private val blackPaint : Paint = Paint()

    var cX : Float = 0f
    var cY : Float = 0f

    init{
        blackPaint.color = Color.BLACK
        blackPaint.strokeWidth = 10F
        blackPaint.style = Paint.Style.STROKE

    }

    override fun onDraw(canvas: Canvas?) {

        canvas?.drawCircle(cX,cY,100f,blackPaint)
        invalidate()
        super.onDraw(canvas)
    }
}