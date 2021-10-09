package com.daerong.graduationproject.licenseplate

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import io.grpc.Attributes

class CustomViewRect(context : Context, attrs : AttributeSet): View(context, attrs) {
    private val eraser = Paint().apply{
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val path = Path()
    private val rect = RectF()
    private val stroke = Paint().apply {
        isAntiAlias = true
        strokeWidth = 4f
        color = Color.WHITE
        style = Paint.Style.STROKE
    }

    private fun setRect(canvas: Canvas?){
        rect.set(
            100f,
            500f,
            canvas!!.width-100f,
            900f
        )
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        drawBorder(canvas)
        drawHole(canvas)
    }

    private fun drawHole(canvas: Canvas?) {
        canvas?.drawRoundRect(
            rect.apply {
                setRect(canvas)
            },
            2.0f, 2.0f, eraser
        )
    }

    private fun drawBorder(canvas: Canvas?) {
        path.rewind()
        path.addRoundRect(
            rect.apply {
                setRect(canvas)
            },
            2.0f,
            2.0f,
            Path.Direction.CW
        )

        canvas?.drawPath(path, stroke)

    }
}