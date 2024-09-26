package es.atm.gbee.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import es.atm.gbee.modules.Memory
import es.atm.gbee.modules.VRAM_START

const val ROW_NUMBER = 24
const val COL_NUMBER = 16

class GameSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private val gameBoyWidth = 160
    private val gameBoyHeight = 144
    private var scale = 0

    private val debugMode = true

    private val tileColors : IntArray = intArrayOf(0xFFFFFFFF.toInt(), 0xFFAAAAAA.toInt(), 0xFF555555.toInt(), 0xFF000000.toInt())

    private var gameThread: GameThread? = null

    init {
        holder.addCallback(this)
        gameThread = GameThread(holder, this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        gameThread?.running = true
        gameThread?.start()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        val aspectRatio = gameBoyWidth.toFloat() / gameBoyHeight.toFloat()
        scale = aspectRatio.toInt()
        val newWidth: Int
        val newHeight: Int

        if (width / height > aspectRatio) {
            newWidth = (height * aspectRatio).toInt()
            newHeight = height
        } else {
            newWidth = width
            newHeight = (width / aspectRatio).toInt()
        }

        setMeasuredDimension(newWidth, newHeight)
    }

    fun update() {
    }

    fun render(canvas: Canvas) {
        canvas.drawColor(Color.BLACK) // Background

        var xDraw = 0
        var yDraw = 0
        var tileNum = 0

        if(debugMode){
            for (y in 0 until ROW_NUMBER) {
                for(x in 0 until COL_NUMBER){
                    displayTile(canvas, VRAM_START, tileNum, xDraw + (x * scale), yDraw + (y * scale))
                    xDraw += (8 * scale)
                    tileNum++
                }
                yDraw += (8 * scale)
                xDraw = 0
            }
        }
    }

    fun displayTile(canvas: Canvas, startLocation: Int, tileNumber: Int, x: Int, y: Int){

        val paint = Paint()

        for(tileY in 0 until 16){
            val b1 = Memory.read(startLocation + (tileNumber * 16) + tileY)
            val b2 = Memory.read(startLocation + (tileNumber * 16) + tileY + 1)

            for(bit in 7 downTo  0){
                val high = if (((b1.toInt() and 0xFF) and (1 shl bit)) != 0) 0b10 else 0
                val low = if (((b2.toInt() and 0xFF) and (1 shl bit)) != 0) 0b01 else 0

                val color = tileColors[high or low]
                paint.color = color

                val left = x + ((7 - bit) * scale)
                val top = y + (tileY / 2 * scale)
                val right = left + scale
                val bottom = top + scale

                canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
            }
        }
    }
}

class GameThread(private val surfaceHolder: SurfaceHolder, private val gameSurfaceView: GameSurfaceView) : Thread() {

    var running = false

    override fun run() {
        while (running) {
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                synchronized(surfaceHolder) {
                    gameSurfaceView.update()
                    gameSurfaceView.render(canvas)
                }
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
}