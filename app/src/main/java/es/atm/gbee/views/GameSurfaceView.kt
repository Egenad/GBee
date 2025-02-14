package es.atm.gbee.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import es.atm.gbee.modules.GB_X_RESOLUTION
import es.atm.gbee.modules.GB_Y_RESOLUTION
import es.atm.gbee.modules.Memory
import es.atm.gbee.modules.PPU
import es.atm.gbee.modules.VRAM_START
import kotlin.math.ceil

const val ROW_NUMBER = 24
const val COL_NUMBER = 16

class GameSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private var scale : Float = 0f
    var debugMode = false
    private var gameThread: GameThread? = null

    init {
        holder.addCallback(this)
        gameThread = GameThread(holder, this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (gameThread?.isAlive != true) {
            gameThread?.running = true
            gameThread?.start()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
        holder.removeCallback(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec) // Android phone total width
        val height = MeasureSpec.getSize(heightMeasureSpec)  // Android phone total height

        val aspectRatio = GB_X_RESOLUTION.toFloat() / GB_Y_RESOLUTION.toFloat() // GB Aspect Ratio

        val newWidth: Int
        val newHeight: Int

        if (width / height > aspectRatio) { // Landscape
            newWidth = (height * aspectRatio).toInt()
            newHeight = height
            scale = height / GB_Y_RESOLUTION.toFloat()
        } else { // Straight
            newWidth = width
            newHeight = (width / aspectRatio).toInt()
            scale = width / GB_X_RESOLUTION.toFloat()
        }

        setMeasuredDimension(newWidth, newHeight)
    }

    fun update() {
    }

    fun render(canvas: Canvas) {
        if(PPU.lcdIsEnabled()) {
            canvas.drawColor(Color.WHITE) // Background
            if (debugMode) {
                renderTileMemory(canvas)
            } else {
                renderVRam(canvas)
            }
        }else{
            canvas.drawColor(Color.BLACK) // Background
        }
    }

    private fun renderVRam(canvas: Canvas){
        val paint = Paint()

        for (lineNum in 0 until GB_Y_RESOLUTION) {
            for (x in 0 until GB_X_RESOLUTION) {
                val left = Math.round(x * scale).toFloat()
                val top = Math.round(lineNum * scale).toFloat()
                val right = left + ceil(scale.toDouble()).toFloat()
                val bottom = top + ceil(scale.toDouble()).toFloat()

                val color = PPU.getBufferPixelFromIndex(x + (lineNum * GB_X_RESOLUTION))
                paint.color = color

                canvas.drawRect(left, top, right, bottom, paint)
            }
        }
    }

    private fun renderTileMemory(canvas: Canvas){
        var xDraw = 0f
        var yDraw = 0f
        var tileNum = 0

        for (y in 0 until ROW_NUMBER) {
            for(x in 0 until COL_NUMBER){
                displayTile(canvas, VRAM_START, tileNum, xDraw + (x * scale), yDraw + (y * scale))
                xDraw += (8 * scale)
                tileNum++
            }
            yDraw += (8 * scale)
            xDraw = 0f
        }
    }

    private fun displayTile(canvas: Canvas, startLocation: Int, tileNumber: Int, x: Float, y: Float){

        val paint = Paint()

        for(tileY in 0 until 16){
            val b1 = Memory.getByteOnAddress(startLocation + (tileNumber * 16) + tileY)
            val b2 = Memory.getByteOnAddress(startLocation + (tileNumber * 16) + tileY + 1)

            for(bit in 7 downTo  0){
                val high = if (((b1.toInt() and 0xFF) and (1 shl bit)) != 0) 0b10 else 0
                val low = if (((b2.toInt() and 0xFF) and (1 shl bit)) != 0) 0b01 else 0

                val color = PPU.getColorIndex(high or low)
                paint.color = color

                val left = x + ((7 - bit) * scale)
                val top = y + (tileY / 2 * scale)
                val right = left + scale
                val bottom = top + scale

                canvas.drawRect(left, top, right, bottom, paint)
            }
        }
    }

    fun pause() {
        gameThread?.let {
            it.running = false
            try {
                it.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    fun release() {
        pause()
        holder.removeCallback(this)
    }

    fun resume(){
        holder.addCallback(this)
        gameThread?.let {
            if(it.isAlive) {
                it.running = true
                it.start()
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
                if (canvas != null) {
                    synchronized(surfaceHolder) {
                        gameSurfaceView.update()
                        gameSurfaceView.render(canvas)
                    }
                }
            }catch (ex: Exception){
                ex.printStackTrace()
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
}