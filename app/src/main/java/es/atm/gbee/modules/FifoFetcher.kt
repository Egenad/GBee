package es.atm.gbee.modules

enum class FetcherState(val number: Int){
    OBTAIN_TILE(0),
    LOW_DATA_TILE(1),
    HIGH_DATA_TILE(2),
    SLEEP(3),
    PUSH(4)
}

class FifoEntry(val value: Int, var next: FifoEntry? = null)

class Fifo {
    private var head: FifoEntry? = null
    private var tail: FifoEntry? = null
    private var size: Int = 0

    fun push(value: Int) {
        val newEntry = FifoEntry(value)
        if (tail == null) { // First Entry
            head = newEntry
            tail = newEntry
        } else {
            tail?.next = newEntry
            tail = newEntry
        }
        size++
    }

    fun pop(): Int? {
        if (head == null)
            return null

        val dequeuedValue = head?.value
        head = head?.next
        if (head == null) {
            tail = null
        }
        size--
        return dequeuedValue
    }

    fun peek(): Int? {
        return head?.value
    }

    fun isEmpty(): Boolean {
        return size == 0
    }

    fun getSize(): Int{
        return size
    }
}

class FifoFetcher {
    private var state : FetcherState = FetcherState.OBTAIN_TILE

    private var lineX : Int         = 0     // X position of the line
    private var fetchX : Int        = 0     // Tile X Coordinate to be fetched
    private var pushedPixels : Int  = 0     // Pixels pushed to the screen
    private var fifoPixels : Int    = 0     // Pixels in the fifo

    private var fifo : Fifo = Fifo()

    private var mapY: Int = 0               // Global Y position of the map
    private var mapX: Int = 0               // Global X position of the map
    private var tileY: Int = 0              // Line of the tile to be fetched

    private val videoBuffer: ByteArray      = ByteArray(GB_Y_RESOLUTION * GB_X_RESOLUTION * 4) { 0 }
    private var tileData: ByteArray         = ByteArray(3) { 0 } // Fetched Tile Data
    private var objData: ByteArray          = ByteArray(6) { 0 } // Fetched OBJ / Sprite Data

    fun process(){
        val scx = Memory.getByteOnAddress(SCX)
        val scy = Memory.getByteOnAddress(SCY)
        val ly = Memory.getByteOnAddress(LY_ADDR)

        mapY = ((scy.toInt() and 0xFF) + (ly.toInt() and 0xFF))
        mapX = ((scx.toInt() and 0xFF) + fetchX)
        tileY = (mapY % 8) * 2

        if(PPU.getLineTicks() % 2 == 0){
            fetch()
        }

        pushPixel() // Push pixels to pipeline
    }

    private fun fetch(){
        when(state){
            FetcherState.OBTAIN_TILE -> getTile()   // Fetch the current tile for the location we're on the map
            FetcherState.LOW_DATA_TILE -> TODO()
            FetcherState.HIGH_DATA_TILE -> TODO()
            FetcherState.SLEEP -> TODO()
            FetcherState.PUSH -> TODO()
        }
    }

    private fun getTile(){
        if(PPU.lcdIsEnabled()){
            var tile = Memory.getByteOnAddress(PPU.getBGTilemapAddr() + (mapX / 8) + ((mapY / 8) * 32)) // 1 Tile == 8 Pixels
            if(PPU.getAddrModeAddr() == SIGNED_TILE_REGION) tile = ((tile.toInt() and 0xFF) + 128).toByte() // Signed Region [-128, 128] --> Transform to [0, 255]
            tileData[0] = tile
        }

        state = FetcherState.LOW_DATA_TILE
        fetchX += 8
    }

    private fun pushPixel(){
        if(fifo.getSize() > 8){ // Process pixels if the FIFO has at least 8
            val pixelData = fifo.pop()

            if(lineX >= ((Memory.getByteOnAddress(SCX).toInt() and 0xFF) % 8) && pixelData != null){ // Check that Coordinate X is inside the visible region of the screen
                val ly = Memory.getByteOnAddress(LY_ADDR).toInt() and 0xFF
                val address = pushedPixels + (ly * GB_X_RESOLUTION)                                  // Address = Pixels already pushed + (Actual Line * X Resolution)

                putValueToVideoBuffer(address, (pixelData and 0xFF).toByte())
                pushedPixels++
            }

            lineX++
        }
    }

    private fun isWindowTile(): Boolean{
        return false
    }

    private fun windowIsVisible(): Boolean{

        val wx = Memory.getByteOnAddress(WX)
        val wy = Memory.getByteOnAddress(WY)

        return PPU.windowIsEnabled() && wx >= 0 && wx < GB_X_RESOLUTION && wy > 0
    }

    fun clear(){
        while(!fifo.isEmpty()){
            fifo.pop()
        }
    }

    fun setState(state: FetcherState){
        this.state = state
    }

    fun getState(): FetcherState{
        return state
    }

    fun setLineX(x: Int){
        lineX = x
    }

    fun getLineX(): Int{
        return lineX
    }

    fun setFetchX(x: Int){
        fetchX = x
    }

    fun getFetchX(): Int{
        return fetchX
    }

    fun setPushedPixels(x: Int){
        pushedPixels = x
    }

    fun getPushedPixels(): Int{
        return pushedPixels
    }

    fun setFifoPixels(x: Int){
        fifoPixels = x
    }

    fun getFifoPixels(): Int{
        return fifoPixels
    }

    fun putValueToVideoBuffer(address: Int, value: Byte){
        videoBuffer[address] = value
    }
}