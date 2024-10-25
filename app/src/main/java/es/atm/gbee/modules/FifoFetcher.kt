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

    private val videoBuffer: IntArray = IntArray(GB_Y_RESOLUTION * GB_X_RESOLUTION) { 0 }
    private var tileData: ByteArray   = ByteArray(3) { 0 } // Fetched Tile Data

    // OBJs Data
    private var objData: ByteArray   = ByteArray(6) { 0 } // Fetched OBJ / Sprite Data

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

        pushPixeltoBuffer() // Push pixels to pipeline
    }

    private fun fetch(){
        when(state){
            FetcherState.OBTAIN_TILE -> fetchTile()         // Fetch the current tile identification in the BG Tilemap
            FetcherState.LOW_DATA_TILE -> fetchLowData()    // Fetch the low byte of the tile
            FetcherState.HIGH_DATA_TILE -> fetchHighData()  // Fetch the high byte of the tile
            FetcherState.SLEEP -> sleepState()
            FetcherState.PUSH -> pushState()
        }
    }

    private fun fetchTile(){
        if(PPU.lcdIsEnabled()){
            var tile = Memory.getByteOnAddress(PPU.getBGTilemapAddr() + (mapX / 8) + ((mapY / 8) * 32)) // 1 Tile == 8 Pixels
            if(PPU.getAddrModeAddr() == SIGNED_TILE_REGION){
                tile = ((tile.toInt() and 0xFF) + 128).toByte() // Signed Region [-128, 128] --> Transform to [0, 255]
            }
            tileData[0] = tile
        }

        state = FetcherState.LOW_DATA_TILE
        fetchX += 8
    }

    private fun fetchLowData(){
        tileData[1] = Memory.getByteOnAddress(PPU.getAddrModeAddr() + ((tileData[0].toInt() and 0xFF) * 16) + tileY)
        state = FetcherState.HIGH_DATA_TILE
    }

    private fun fetchHighData(){
        tileData[2] = Memory.getByteOnAddress(PPU.getAddrModeAddr() + ((tileData[0].toInt() and 0xFF) * 16) + (tileY + 1))
        state = FetcherState.SLEEP
    }

    private fun sleepState(){
        state = FetcherState.PUSH
    }

    private fun pushState(){
        if(pushPixelToFifo()){
            state = FetcherState.OBTAIN_TILE
        }
    }

    private fun pushPixelToFifo(): Boolean{
        if(fifo.getSize() > 8){
            return false // Fifo is full
        }

        val scx = Memory.getByteOnAddress(SCX).toInt() and 0xFF
        val x = fetchX - (8 - (scx % 8))

        for(i in 0..7){
            val bit = 7 - i
            val low = (((tileData[1].toInt() and 0xFF) shr bit) and 1)
            val high = ((((tileData[2].toInt() and 0xFF) shr bit) and 1) shl 1)
            val color = PPU.getColorIndex(high or low) // Pixel Color

            if(x >= 0){
                fifo.push(color)
                fifoPixels++
            }
        }

        return true
    }

    private fun pushPixeltoBuffer(){
        if(fifo.getSize() > 8){ // Process pixels if the FIFO has at least 8
            val pixelData = fifo.pop()

            if(lineX >= ((Memory.getByteOnAddress(SCX).toInt() and 0xFF) % 8) && pixelData != null){ // Check that Coordinate X is inside the visible region of the screen
                val ly = Memory.getByteOnAddress(LY_ADDR).toInt() and 0xFF
                val address = pushedPixels + (ly * GB_X_RESOLUTION)                                  // Address = Pixels already pushed + (Actual Line * X Resolution)

                putValueToVideoBuffer(address, pixelData)
                pushedPixels++
            }

            lineX++
        }
    }

    fun clear(){
        while(!fifo.isEmpty()){
            fifo.pop()
        }
    }

    fun resetParams(){
        state = FetcherState.OBTAIN_TILE
        lineX = 0
        fetchX = 0
        pushedPixels = 0
        fifoPixels = 0
    }

    fun getPushedPixels(): Int{
        return pushedPixels
    }

    private fun putValueToVideoBuffer(address: Int, value: Int){
        videoBuffer[address] = value
    }

    fun getValueFromVideoBuffer(address: Int): Int{
        return videoBuffer[address]
    }
}