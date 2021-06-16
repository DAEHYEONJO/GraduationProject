package com.daerong.graduationproject.draw

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatImageView

class BeaconRangeView(context: Context, attrs:AttributeSet?) : AppCompatImageView(context,attrs) {
    private val blackPaint : Paint = Paint()
    private val bluePaint : Paint = Paint()
    private val greenPaint : Paint = Paint()
    private val redPaint : Paint = Paint()

    var x1 : Float = 0f
    var y1 : Float = 0f
    var r1 : Float = 0f

    var x3 : Float = 936f
    var y3: Float = 468f
    var r3 : Float = 0f

    var x2 : Float = 0f
    var y2 : Float = 936f
    var r2 : Float = 0f

    var myx : Float = 500f
    var myy : Float = 500f
    var myr : Float = 10f



    init{
        blackPaint.color = Color.BLACK
        blackPaint.strokeWidth = 5F
        blackPaint.style = Paint.Style.STROKE

        bluePaint.color = Color.BLUE
        bluePaint.strokeWidth = 5F
        bluePaint.style = Paint.Style.STROKE

        greenPaint.color = Color.GREEN
        greenPaint.strokeWidth = 5F
        greenPaint.style = Paint.Style.STROKE

        redPaint.color = Color.RED
        redPaint.strokeWidth = 10F
        redPaint.style = Paint.Style.FILL

    }

    fun changeRadious(r1 : Float, r2 : Float , r3 : Float, myx : Float,myy:Float) {
        Log.d("testdraw","changeRadious : $r1")
        this.r1 = r1*150
        this.r2 = r2*150
        this.r3 = r3*150
        this.myx = myx*150
        this.myy = myy*150
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        Log.d("testdraw","ondraw : $r1")
        canvas?.drawCircle(x1,y1,r1,blackPaint)
        canvas?.drawCircle(x2,y2,r2,bluePaint)
        canvas?.drawCircle(x3,y3,r3,greenPaint)
        canvas?.drawCircle(myx,myy,myr,redPaint)
        super.onDraw(canvas)
    }
}