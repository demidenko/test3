package com.example.test3.account_view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.test3.account_manager.HandleColor
import com.example.test3.account_manager.RatedAccountManager
import com.example.test3.account_manager.RatingChange
import java.util.concurrent.TimeUnit

class RatingGraphView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap


    private lateinit var accountManager: RatedAccountManager

    fun setManager(ratedAccountManager: RatedAccountManager){
        accountManager = ratedAccountManager
    }

    private var ratingHistory: List<RatingChange> = listOf(RatingChange(0,0L))

    fun setHistory(history: List<RatingChange>) {
        ratingHistory = history.sortedBy { it.timeSeconds }
        drawRating()
        invalidate()
    }

    private fun drawRating(){

        if(width == 0|| height == 0) return

        if (::extraBitmap.isInitialized) extraBitmap.recycle()
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        extraCanvas.scale(1f, -1f, width/2f, height/2f)

        val minX = ratingHistory.minOf { it.timeSeconds } - TimeUnit.DAYS.toSeconds(1).toFloat()
        val minY = ratingHistory.minOf { it.rating } - 100f
        val maxX = ratingHistory.maxOf { it.timeSeconds } + TimeUnit.DAYS.toSeconds(1).toFloat()
        val maxY = ratingHistory.maxOf { it.rating } + 100f

        val m = Matrix()
        m.preScale(width/(maxX-minX), height/(maxY-minY))
        m.preTranslate(-minX, -minY)

        val ratingBounds = accountManager.ratingsUpperBounds.toMutableList()
        ratingBounds.add(Pair(Int.MAX_VALUE, HandleColor.RED))
        ratingBounds.reversed().forEachIndexed { index, (upper, ratingColor) ->

            val y = if(index == 0) height.toFloat() else {
                val arr = floatArrayOf(0f, upper.toFloat())
                m.mapPoints(arr)
                arr[1]
            }

            extraCanvas.drawRect(0f, 0f, width.toFloat(), y, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ratingColor.getARGB(accountManager)
                style = Paint.Style.FILL
            })
        }

        val path = Path()
        ratingHistory.mapIndexed { index, ratingChange ->
            val arr = floatArrayOf(ratingChange.timeSeconds.toFloat(), ratingChange.rating.toFloat())
            m.mapPoints(arr)
            val (x,y) = arr
            if(index == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }

        extraCanvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 5f
        })

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.BLACK
            strokeWidth = 3f
        }

        ratingHistory.mapIndexed { index, ratingChange ->
            val arr = floatArrayOf(ratingChange.timeSeconds.toFloat(), ratingChange.rating.toFloat())
            m.mapPoints(arr)
            val (x,y) = arr

            val ratingColor = accountManager.getHandleColor(ratingChange.rating)

            extraCanvas.drawCircle(x, y, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = ratingColor.getARGB(accountManager)
            })
            extraCanvas.drawCircle(x, y, 10f, circlePaint)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        drawRating()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(extraBitmap, 0f, 0f, null)
    }
}