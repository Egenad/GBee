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

    private var lineX : Int         = 0     // X position of the line. Actual X scanline coordinate.
    private var fetchX : Int        = 0     // Tile X Coordinate to be fetched. Used to calculate mapX and obtain tiles from VRAM.
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
            FetcherState.OBTAIN_TILE -> getTile()               // Fetch the current tile identification in the BG Tilemap
            FetcherState.LOW_DATA_TILE -> getTileLowData()      // Fetch the low byte of the tile
            FetcherState.HIGH_DATA_TILE -> getTileHighData()    // Fetch the high byte of the tile
            FetcherState.SLEEP -> sleepState()
            FetcherState.PUSH -> pushState()
        }
    }

    /**
     * Determines which background/window tile to fetch pixels from.
     * By default the tilemap used is the one at 0x9800.
     */
    private fun getTile(){
        if(PPU.lcdIsEnabled()){
            val ly = Memory.getByteOnAddress(LY_ADDR).toInt() and 0xFF
            val wy = Memory.getByteOnAddress(WY).toInt() and 0xFF
            val wx = Memory.getByteOnAddress(WX).toInt() and 0xFF - WIN_X_OFFSET

            // Obtain tilemap to use (BG or WIN)
            val windowTile = PPU.windowIsEnabled() && fetchX >= wx && ly >= wy
            val tilemapToUse = if(windowTile) PPU.getWinTilemapAddr() else PPU.getBGTilemapAddr()

            val xCoordinate = if (windowTile) ((fetchX - wx) / 8) else (mapX / 8) and 0x1F
            val yCoordinate = if (windowTile) ((ly - wy) / 8) else (mapY / 8)

            val address = tilemapToUse + xCoordinate + (yCoordinate * GB_X_TOTAL_TILES)
            var tile = Memory.getByteOnAddress(address) // 1 Tile == 8 Pixels

            if(PPU.getAddrModeAddr() == SIGNED_TILE_REGION){
                tile = ((tile.toInt() and 0xFF) + 128).toByte() // Signed Region [-128, 128] --> Transform to [0, 255]
            }
            tileData[0] = tile
        }

        state = FetcherState.LOW_DATA_TILE
        fetchX += 8
    }

    private fun getTileLowData(){

        val offset = calculeTileDataOffset()

        tileData[1] = Memory.getByteOnAddress(PPU.getAddrModeAddr() + ((tileData[0].toInt() and 0xFF) * 16) + offset)

        loadSpriteData(0)

        state = FetcherState.HIGH_DATA_TILE
    }

    private fun getTileHighData(){

        val offset = calculeTileDataOffset()

        tileData[2] = Memory.getByteOnAddress(PPU.getAddrModeAddr() + ((tileData[0].toInt() and 0xFF) * 16) + (offset + 1))

        loadSpriteData(1)

        state = FetcherState.SLEEP
    }

    private fun sleepState(){
        state = FetcherState.PUSH
    }

    private fun pushState(){
        if(pushPixelsToFifo()){
            state = FetcherState.OBTAIN_TILE
        }
    }

    private fun pushPixelsToFifo(): Boolean{
        if(fifo.getSize() > 8)
            return false // Fifo is full

        val scx = Memory.getByteOnAddress(SCX).toInt() and 0xFF
        val x = fetchX - (8 - (scx % 8))

        for(i in 0..7){
            val bit = 7 - i
            val low = (((tileData[1].toInt() and 0xFF) shr bit) and 1)
            val high = ((((tileData[2].toInt() and 0xFF) shr bit) and 1) shl 1)
            var color = PPU.getColorIndex(high or low) // Pixel Color

            /*if(PPU.bgWinIsEnabled()){
                color = PPU.getColorIndex(0)
            }
            if(PPU.objsAreEnabled() || ROM.isCGB()){ // CGB ignores this condition
                color = fetchSpritePixel(color, high or low)
            }*/

            if(x >= 0){
                fifo.push(color)
                fifoPixels++
            }
        }

        return true
    }

    private fun loadSpriteData(index: Int){
        //TODO
    }

    private fun fetchSpritePixel(color: Int, bgColor: Int): Int{

        var colorToReturn = color

        val fetchedObjs = PPU.getFetchedSpriteEntries().filterNotNull()
        val scx = Memory.getByteOnAddress(SCX).toInt() and 0xFF

        for(i in fetchedObjs.indices){
            val sprX = ((fetchedObjs[i].x.toInt() and 0xFF) - OAM_X_OFFSET) + (scx % 8)

            if(sprX + 8 < fifoPixels){ // Pixel past the current fifo line pixel count
                continue
            }

            val offset = fifoPixels - sprX

            if(offset < 0 || offset > 7){ // Out of bounds
                continue
            }

            var bit = 7 - offset

            if(ObjFlags.X_FLIP.get(fetchedObjs[i].flags) == 1){
                //bit = offset TODO: Check this
            }

            val low = (((tileData[i * 2].toInt() and 0xFF) shr bit) and 1)
            val high = ((((tileData[(i * 2) + 1].toInt() and 0xFF) shr bit) and 1) shl 1)

            /**
             * u8 hi = !!(ppu_get_context()->pfc.fetch_entry_data[i * 2] & (1 << bit));
             *         u8 lo = !!(ppu_get_context()->pfc.fetch_entry_data[(i * 2) + 1] & (1 << bit)) << 1;
             *
             *         bool bg_priority = ppu_get_context()->fetched_entries[i].f_bgp;
             *
             *         if (!(hi|lo)) {
             *             //transparent
             *             continue;
             *         }
             *
             *         if (!bg_priority || bg_color == 0) {
             *             color = (ppu_get_context()->fetched_entries[i].f_pn) ?
             *                 lcd_get_context()->sp2_colors[hi|lo] : lcd_get_context()->sp1_colors[hi|lo];
             *
             *             if (hi|lo) {
             *                 break;
             *             }
             *         }
             *     }
             *
             *     return color;
             *     */
        }

        //return colorToReturn
        return 0
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

    private fun isWindowTile(_fetchX: Int, _wx: Int, _wy: Int, _ly: Int): Boolean{
        return (PPU.windowIsEnabled() && _fetchX >= _wx && _ly >= _wy)
    }

    private fun calculeTileDataOffset(): Int{
        val ly = Memory.getByteOnAddress(LY_ADDR).toInt() and 0xFF
        val wy = Memory.getByteOnAddress(WY).toInt() and 0xFF
        val wx = Memory.getByteOnAddress(WX).toInt() and 0xFF - WIN_X_OFFSET

        val isWindowTile = isWindowTile(fetchX, wx, wy, ly)
        return if (isWindowTile) ((ly - wy) % 8) * 2 else tileY
    }
}