package es.atm.gbee.modules

enum class FetcherState(val number: Int){
    OBTAIN_TILE(0),
    LOW_DATA_TILE(1),
    HIGH_DATA_TILE(2),
    SLEEP(3),
    PUSH(4)
}

open class FifoEntry(val value: Int, var next: FifoEntry? = null)
class FifoEntrySprite(value: Int, next: FifoEntry? = null, val priority: Int) : FifoEntry(value, next)

class Fifo {
    private var head: FifoEntry? = null
    private var tail: FifoEntry? = null
    private var size: Int = 0

    fun push(value: Int) {
        val newEntry = FifoEntry(value)
        makePush(newEntry)
    }

    fun push(value: Int, priority: Int) {
        val newEntry = FifoEntrySprite(value, priority = priority)
        makePush(newEntry)
    }

    private fun makePush(newEntry: FifoEntry?){
        if (tail == null) { // First Entry
            head = newEntry
            tail = newEntry
        } else {
            tail?.next = newEntry
            tail = newEntry
        }
        size++
    }

    fun pop(): FifoEntry? {
        val dequeuedEntry = head
        head = head?.next
        if (head == null) tail = null
        if (dequeuedEntry != null) size--
        return dequeuedEntry
    }

    fun popSprite(): FifoEntrySprite? {
        val dequeuedEntry = pop()
        return dequeuedEntry as? FifoEntrySprite
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

    private var backgroundFifo : Fifo   = Fifo()
    private var spriteFifo : Fifo       = Fifo()
    private var fifoX : Int = 0

    private var mapY: Int  = 0              // Global Y position of the map
    private var mapX: Int  = 0              // Global X position of the map
    private var tileY: Int = 0              // Line of the tile to be fetched

    private val videoBuffer: IntArray = IntArray(GB_Y_RESOLUTION * GB_X_RESOLUTION) { 0 }
    private var tileData: ByteArray   = ByteArray(3) { 0 } // Fetched Tile Data

    private var objTileData : Array<OAMObj?> = Array(3) { null }
    private var objFetchedData = ByteArray(6)
    private var fetchedSprites: Int = 0

    fun process(){
        val scx = Memory.getByteOnAddress(SCX).toInt() and 0xFF
        val scy = Memory.getByteOnAddress(SCY).toInt() and 0xFF
        val ly  = Memory.getByteOnAddress(LY_ADDR).toInt() and 0xFF

        mapY = scy + ly
        mapX = scx + fetchX
        tileY = (mapY % 8) * 2

        if(PPU.getLineTicks() % 2 == 0){
            fetch()
        }
        pushPixelsToBuffer() // Push pixels to pipeline
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

    private fun getTile(){
        if(PPU.lcdIsEnabled())
            getBGTile()

        if(PPU.objsAreEnabled() && PPU.getFetchedSpriteEntries().isNotEmpty()) {
            fetchedSprites = 0
            getSpriteTile()
        }

        state = FetcherState.LOW_DATA_TILE
        fetchX += 8
    }

    /**
     * Determines which background/window tile to fetch pixels from.
     * By default the tilemap used is the one at 0x9800.
     */
    private fun getBGTile(){
        val ly = Memory.getByteOnAddress(LY_ADDR).toInt() and 0xFF
        val wy = Memory.getByteOnAddress(WY).toInt() and 0xFF
        val scx = Memory.getByteOnAddress(SCX).toInt() and 0xFF
        val wx = Memory.getByteOnAddress(WX).toInt() and 0xFF - WIN_X_OFFSET

        // Obtain tilemap to use (BG or WIN)
        val windowTile = isWindowTile()
        val tilemapToUse = if(windowTile) PPU.getWinTilemapAddr() else PPU.getBGTilemapAddr()

        val xCoordinate = if (windowTile) ((fetchX - wx) / 8) else (mapX / 8) and 0x1F
        val yCoordinate = if (windowTile) ((ly - wy) / 8) else (mapY / 8)

        val address = tilemapToUse + ((xCoordinate + (yCoordinate * GB_X_TOTAL_TILES)) and 0x3ff)
        var tile = Memory.getByteOnAddress(address) // 1 Tile == 8 Pixels

        if(PPU.getAddrModeAddr() == SIGNED_TILE_REGION){
            tile = ((tile.toInt() and 0xFF) + 128).toByte() // Signed Region [-128, 128] --> Transform to [0, 255]
        }
        tileData[0] = tile
    }

    private fun getSpriteTile(){

        val fetchedObjs = PPU.getFetchedSpriteEntries()

        for (obj in fetchedObjs) {
            if(obj != null) {
                val scx = Memory.getByteOnAddress(SCX).toInt() and 0xFF
                val sprX = ((obj.x.toInt() and 0xFF) - OAM_X_OFFSET) + (scx % PIXELS_PER_TILE)

                if ((sprX >= fetchX && sprX < fetchX + OAM_X_OFFSET) ||
                ((sprX + OAM_X_OFFSET) >= fetchX && (sprX + OAM_X_OFFSET) < fetchX + OAM_X_OFFSET)) {
                    objTileData[fetchedSprites] = obj
                    fetchedSprites++
                }

                if (fetchedSprites >= 3) break
            }
        }
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

        val bgPush = pushBGPixelsToFifo()

        if(bgPush){
            state = FetcherState.OBTAIN_TILE
        }
    }

    private fun loadSpriteData(offset: Int){
        val ly = Memory.getByteOnAddress(LY_ADDR).toInt() and 0xFF
        val spriteHeight = if (LCDCObj.OBJ_SIZE.get(Memory.getByteOnAddress(LCDC_ADDR)) == 1) 16 else 8

        for(i in 0 until fetchedSprites){
            if(objTileData[i] != null) {
                val flags = objTileData[i]!!.flags
                val spriteY = (objTileData[i]!!.y.toInt() and 0xFF) - OAM_Y_OFFSET
                var tileIndex = objTileData[i]!!.tile.toInt() and 0xFF

                if (spriteHeight == 16)
                    if ((ObjFlags.Y_FLIP.get(flags) == 0 && (ly - spriteY) >= 8) ||
                        (ObjFlags.Y_FLIP.get(flags) == 1 && (ly - spriteY) < 8)) {
                        tileIndex += 1
                    }

                val baseAddress = VRAM_START + (tileIndex * 16)
                val tileLine = (ly - spriteY) % 8
                val address = baseAddress + (tileLine * 2)

                objFetchedData[(i * 2) + offset] = Memory.getByteOnAddress(address + offset)
            }
        }
    }

    private fun pushBGPixelsToFifo(): Boolean{
        if(backgroundFifo.getSize() >= 8)
            return false // Fifo is full

        val scx = Memory.getByteOnAddress(SCX).toInt() and 0xFF
        val x = fetchX - (OAM_X_OFFSET - (scx % PIXELS_PER_TILE))

        for(i in 0..7){
            val bit = 7 - i

            val low = (((tileData[1].toInt() and 0xFF) shr bit) and 1)
            val high = ((((tileData[2].toInt() and 0xFF) shr bit) and 1) shl 1)
            var color = if(PPU.bgWinIsEnabled()) PPU.getColorIndex(high or low) else PPU.getColorIndex(0) // Pixel Color

            if(PPU.objsAreEnabled())
                color = obtainSpriteColor(color)

            if(x >= 0){
                backgroundFifo.push(color)
                fifoX++
            }
        }

        return true
    }

    private fun obtainSpriteColor(color: Int): Int{

        for (i in 0 until fetchedSprites) {

            if(objTileData[i] != null) {

                val scx = Memory.getByteOnAddress(SCX).toInt() and 0xFF
                val sprX = ((objTileData[i]!!.x.toInt() and 0xFF) - OAM_X_OFFSET) + (scx % PIXELS_PER_TILE)

                val offset = fifoX - sprX

                if (offset < 0 || offset > 7) // Out of bounds
                    continue

                var bitToUse = 7 - offset

                if (ObjFlags.X_FLIP.get(objTileData[i]!!.flags) == 1)
                    bitToUse =  offset

                val low = ((objFetchedData[i * 2].toInt() and 0xFF) shr bitToUse) and 1
                val high = (((objFetchedData[(i * 2) + 1].toInt() and 0xFF) shr bitToUse) and 1) shl 1

                if ((high or low) == 0) {
                    continue
                }

                val bgPriority = ObjFlags.PRIORITY.get(objTileData[i]!!.flags)

                if(!(color != PPU.getColorIndex(0) && bgPriority == 1))
                    return PPU.getColorIndex(high or low)

                //spriteFifo.push(color, ObjFlags.PRIORITY.get(objTileData[i]!!.flags))
                //if (spriteFifo.getSize() >= 8) break

            }
        }

        return color
    }

    private fun mixPixels(backgroundPixel: Int, spritePixel: FifoEntrySprite?): Int{
        if (spritePixel == null || spritePixel.value == PPU.getColorIndex(0))
            return backgroundPixel

        if(backgroundPixel != PPU.getColorIndex(0) && spritePixel.priority == 1)
            return backgroundPixel

        return spritePixel.value
    }

    /**
     * Mode 3: PPU transfers pixels to the LCD
     */
    private fun pushPixelsToBuffer(){
        if(backgroundFifo.getSize() >= 8){ // Process pixels if the FIFO has at least 8
            val pixelData = backgroundFifo.pop()?.value

            if(lineX >= ((Memory.getByteOnAddress(SCX).toInt() and 0xFF) % 8) && pixelData != null){ // Check that Coordinate X is inside the visible region of the screen
                val ly = Memory.getByteOnAddress(LY_ADDR).toInt() and 0xFF
                val address = pushedPixels + (ly * GB_X_RESOLUTION)                                  // Address = Pixels already pushed + (Actual Line * X Resolution)

                //val mixedPixel = mixPixels(pixelData, spritePixel)

                putValueToVideoBuffer(address, pixelData)
                pushedPixels++
            }

            lineX++
        }
    }

    fun clear(){
        while(!backgroundFifo.isEmpty()){
            backgroundFifo.pop()
        }
        while(!spriteFifo.isEmpty()){
            spriteFifo.pop()
        }
    }

    fun resetParams(){
        state = FetcherState.OBTAIN_TILE
        lineX = 0
        fetchX = 0
        pushedPixels = 0
        fifoX = 0
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

    private fun isWindowTile(): Boolean{

        val ly = Memory.getByteOnAddress(LY_ADDR).toInt() and 0xFF
        val wy = Memory.getByteOnAddress(WY).toInt() and 0xFF
        val wx = Memory.getByteOnAddress(WX).toInt() and 0xFF - WIN_X_OFFSET

        return (PPU.windowIsEnabled() && fetchX >= wx && ly >= wy)
    }

    private fun calculeTileDataOffset(): Int{
        val ly = Memory.getByteOnAddress(LY_ADDR).toInt() and 0xFF
        val wy = Memory.getByteOnAddress(WY).toInt() and 0xFF

        val isWindowTile = isWindowTile()
        return if (isWindowTile) ((ly - wy) % 8) * 2 else tileY
    }
}