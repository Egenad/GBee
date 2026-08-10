package es.atm.gbee.modules

enum class FetcherState(val number: Int){
    OBTAIN_TILE(0),
    LOW_DATA_TILE(1),
    HIGH_DATA_TILE(2),
    SLEEP(3),
    PUSH(4)
}

enum class ObjFetcherState(val number: Int) {
    IDLE(0), // Search for an OBJ tile that matches with actual X coordinate
    SYNC_BG_FETCHER(1), // Advance the BG/WIN fetcher state
    SCX_PENALTY(2), // Apply the SCX penalty on X=0
    ADVANCE_FIRST(3), // First advance, 1 dot
    ADVANCE_SECOND(4), // Second advance, 3 dots
    LOW_DATA_TILE(5),
    HIGH_DATA_TILE(6),
    PUSH(7),
    FINISH(8)
}

data class FetchedTileData(
    var tileIndex: Int = 0,
    var lowData: Byte = 0,
    var highData: Byte = 0
)

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
    private var objState : ObjFetcherState = ObjFetcherState.IDLE

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
    private var bgTileData: FetchedTileData = FetchedTileData() // Fetched Tile Data
    private var spriteTileData: FetchedTileData = FetchedTileData() // Fetched Sprite Tile Data

    private var activeSprite: OAMObj? = null
    private var objStateDotsRemaining = 0
    private val processedSprites = BooleanArray(OAM_OBJ_NUMBER)
    
    private var windowStartedThisLine: Boolean = false
    private var windowPixelsInFifo: Int = 0

    fun process(){
        initWindow()
        
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

    private fun fetch() {
        if (objState == ObjFetcherState.IDLE) {
            val sprite = findSpriteToFetch()

            if (sprite != null) {
                startSpriteFetch(sprite)
                tickObjFetcher()
            } else {
                tickBGWINFetcher()
            }
        } else {
            tickObjFetcher()
        }
    }

    private fun tickBGWINFetcher(){
        when(state){
            FetcherState.OBTAIN_TILE -> getTile()               // Fetch the current tile identification in the BG Tilemap
            FetcherState.LOW_DATA_TILE -> getTileLowData()      // Fetch the low byte of the tile
            FetcherState.HIGH_DATA_TILE -> getTileHighData()    // Fetch the high byte of the tile
            FetcherState.SLEEP -> sleepState()
            FetcherState.PUSH -> pushState()
        }
    }

    private fun tickObjFetcher(){
        when (objState) {
            ObjFetcherState.IDLE -> Unit
            ObjFetcherState.SYNC_BG_FETCHER -> syncBgFetcher()
            ObjFetcherState.SCX_PENALTY -> applyScxPenalty()
            ObjFetcherState.ADVANCE_FIRST -> consumeDots(1)
            ObjFetcherState.ADVANCE_SECOND -> consumeDots(3)
            ObjFetcherState.LOW_DATA_TILE -> fetchObjLowData()
            ObjFetcherState.HIGH_DATA_TILE -> fetchObjHighData()
            ObjFetcherState.PUSH -> pushObjPixels()
            ObjFetcherState.FINISH -> finishObjFetch()
        }
    }

    private fun findSpriteToFetch() : OAMObj?{
        if (!PPU.objsAreEnabled()) return null

        return PPU.getFetchedSpriteEntries()
            .filterNotNull()
            .firstOrNull { obj ->
                val spriteX = (obj.x.toInt() and 0xFF) - OAM_X_OFFSET
                val triggerX = maxOf(0, spriteX)

                !processedSprites[obj.oamIndex] &&
                        triggerX == pushedPixels
            }
    }

    private fun startSpriteFetch(sprite: OAMObj) {
        activeSprite = sprite
        objState = ObjFetcherState.SYNC_BG_FETCHER
    }

    private fun finishObjFetch() {
        activeSprite?.let {
            processedSprites[it.oamIndex] = true
        }

        activeSprite = null
        objState = ObjFetcherState.IDLE
    }

    private fun getTile(){
        if(PPU.lcdIsEnabled())
            getBGTile()

        /*if(PPU.objsAreEnabled() && PPU.getFetchedSpriteEntries().isNotEmpty()) {
            getSpriteTile()
        }*/

        state = FetcherState.LOW_DATA_TILE
        fetchX += 8
    }

    /**
     * Determines which background/window tile to fetch pixels from.
     * By default the tilemap used is the one at 0x9800.
     */
    private fun getBGTile(){
        val ly = PPU.getLY()
        val wy = PPU.getWindowScreenY()

        // Obtain tilemap to use (BG or WIN)
        val windowTile = isWindowTile()
        val tilemapToUse = if(windowTile) PPU.getWinTilemapAddr() else PPU.getBGTilemapAddr()

        val xCoordinate = if (windowTile) (fetchX / 8) else (mapX / 8) and 0x1F
        val yCoordinate = if (windowTile) ((ly - wy) / 8) else (mapY / 8)

        val address = tilemapToUse + ((xCoordinate + (yCoordinate * GB_X_TOTAL_TILES)) and 0x3ff)
        var tile = Memory.getByteOnAddress(address) // 1 Tile == 8 Pixels

        if(PPU.getAddrModeAddr() == SIGNED_TILE_REGION){
            tile = ((tile.toInt() and 0xFF) + 128).toByte() // Signed Region [-128, 128] --> Transform to [0, 255]
        }
        bgTileData.tileIndex = tile.toInt() and 0xFF
    }

    private fun getTileLowData(){
        val offset = calculateTileDataOffset()
        bgTileData.lowData = Memory.getByteOnAddress(PPU.getAddrModeAddr() + ((bgTileData.tileIndex) * 16) + offset)
        state = FetcherState.HIGH_DATA_TILE
    }

    private fun getTileHighData(){
        val offset = calculateTileDataOffset()
        bgTileData.highData = Memory.getByteOnAddress(PPU.getAddrModeAddr() + ((bgTileData.tileIndex) * 16) + (offset + 1))
        state = FetcherState.SLEEP
    }

    private fun getObjTileLowData(){

        state = FetcherState.HIGH_DATA_TILE
    }

    private fun getObjTileHighData(){

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

    private fun pushBGPixelsToFifo(): Boolean{
        if(backgroundFifo.getSize() >= 8)
            return false // Fifo is full

        val scx = PPU.getScrollX()
        val x = if (windowStartedThisLine) fetchX - OAM_X_OFFSET
                else fetchX - (OAM_X_OFFSET - (scx % PIXELS_PER_TILE))

        for(i in 0 until PIXELS_PER_TILE){
            val bit = (PIXELS_PER_TILE - 1) - i

            val low = (((bgTileData.lowData.toInt() and 0xFF) shr bit) and 1)
            val high = ((((bgTileData.highData.toInt() and 0xFF) shr bit) and 1) shl 1)
            val color = if(PPU.bgWinIsEnabled()) PPU.getColorIndex(high or low) else PPU.getColorIndex(0) // Pixel Color

            if(PPU.objsAreEnabled()) {
                val pixelScreenX = if (windowStartedThisLine) {
                    PPU.getWindowScreenX() + windowPixelsInFifo
                } else {
                    fifoX - (scx % PIXELS_PER_TILE)
                }
                // color = obtainSpriteColor(color, pixelScreenX) TODO: implement independent FIFO
            }

            if(x >= 0){
                backgroundFifo.push(color)
                fifoX++
                if (windowStartedThisLine) windowPixelsInFifo++
            }
        }

        return true
    }

    private fun obtainSpriteColor(color: Int, screenX: Int): Int{

        for (i in 0 until fetchedSprites) {

            if(spriteTileData[i] != null) {

                val sprX = (spriteTileData[i]!!.x.toInt() and 0xFF) - OAM_X_OFFSET
                val offset = screenX - sprX

                if (offset !in 0 until PIXELS_PER_TILE) // Out of bounds
                    continue

                var bitToUse = (PIXELS_PER_TILE - 1) - offset

                if (ObjFlags.X_FLIP.get(spriteTileData[i]!!.flags) == 1)
                    bitToUse =  offset

                val low = ((objFetchedData[i * 2].toInt() and 0xFF) shr bitToUse) and 1
                val high = (((objFetchedData[(i * 2) + 1].toInt() and 0xFF) shr bitToUse) and 1) shl 1

                if ((high or low) == 0) {
                    continue
                }

                val bgPriority = ObjFlags.PRIORITY.get(spriteTileData[i]!!.flags)

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
        if(backgroundFifo.getSize() >= PIXELS_PER_TILE){ // Process pixels if the FIFO has at least 8
            val backgroundPixel = backgroundFifo.pop()?.value
            val spritePixel = if (!spriteFifo.isEmpty()) spriteFifo.popSprite() else null
            val scx = PPU.getScrollX()

            // Check that Coordinate X is inside the visible region of the screen
            // Window pixels are never discarded
            if((windowStartedThisLine || lineX >= (scx % PIXELS_PER_TILE)) && backgroundPixel != null){
                val ly = PPU.getLY()
                val address = pushedPixels + (ly * GB_X_RESOLUTION) // Address = Pixels already pushed + (Actual Line * X Resolution)

                val finalColor = mixPixels(backgroundPixel, spritePixel)

                putValueToVideoBuffer(address, finalColor)
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
        windowStartedThisLine = false
        windowPixelsInFifo = 0
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
    
    private fun initWindow(){
        val ly = PPU.getLY()
        val wy = PPU.getWindowScreenY()
        val wx = PPU.getWindowScreenX()
        val pushedPixels = getPushedPixels()
        
        if (PPU.windowIsEnabled() && ly >= wy && pushedPixels >= wx && !windowStartedThisLine){
            windowStartedThisLine = true
            windowPixelsInFifo = 0
            state = FetcherState.OBTAIN_TILE
            fetchX = 0
            while(!backgroundFifo.isEmpty()){
                backgroundFifo.pop()
            }
        }
    }

    private fun isWindowTile(): Boolean{
        return windowStartedThisLine
    }

    private fun calculateTileDataOffset(): Int{
        val ly = PPU.getLY()
        val wy = PPU.getWindowScreenY()

        val isWindowTile = isWindowTile()
        return if (isWindowTile) ((ly - wy) % 8) * 2 else tileY
    }
}