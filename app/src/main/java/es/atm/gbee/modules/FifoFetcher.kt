package es.atm.gbee.modules

enum class FetcherState{
    OBTAIN_TILE,
    LOW_DATA_TILE,
    HIGH_DATA_TILE,
    SLEEP,
    PUSH
}

enum class ObjFetcherState {
    GET_TILE, // Search for an OBJ tile that matches with actual X coordinate
    SYNC_BG_FETCHER, // Advance the BG/WIN fetcher state
    SCX_PENALTY, // Apply the SCX penalty on X=0
    ADVANCE_FIRST, // First advance, 1 dot
    ADVANCE_SECOND, // Second advance, 3 dots
    LOW_DATA_TILE,
    HIGH_DATA_TILE,
    PUSH,
    FINISH
}

data class FetchedTileData(
    var tileIndex: Int = 0,
    var lowData: Byte = 0,
    var highData: Byte = 0
)

open class FifoEntry(
    val value: Int,
    val colorIndex: Int,
    var next: FifoEntry? = null
)

class FifoEntrySprite(
    colorIndex: Int,
    val palette: Int,
    val priority: Int,
    val oamIndex: Int,
    val spriteX: Int,
    next: FifoEntry? = null
) : FifoEntry(colorIndex, colorIndex, next)

class Fifo {
    private var head: FifoEntry? = null
    private var tail: FifoEntry? = null
    private var size: Int = 0

    /** Adds a BG/WIN pixel, keeping both its rendered value and raw color index. */
    fun push(value: Int, colorIndex: Int) {
        val newEntry = FifoEntry(value, colorIndex)
        makePush(newEntry)
    }

    /** Pads the OBJ FIFO with transparent pixels so sprites can be merged by position. */
    fun ensureSpriteSize(targetSize: Int) {
        while (size < targetSize) {
            makePush(
                FifoEntrySprite(
                    colorIndex = 0,
                    palette = 0,
                    priority = 1,
                    oamIndex = Int.MAX_VALUE,
                    spriteX = Int.MAX_VALUE
                )
            )
        }
    }

    /**
     * Merges one non-transparent OBJ pixel at [index]. If another sprite already owns
     * that position, DMG priority uses X then OAM order, while CGB uses OAM order.
     */
    fun mergeSpriteAt(index: Int, sprite: FifoEntrySprite) {
        if (index !in 0 until size || sprite.colorIndex == 0) {
            return
        }

        var previous: FifoEntry? = null
        var current = head
        repeat(index) {
            previous = current
            current = current?.next
        }

        val existing = current as? FifoEntrySprite ?: return
        if (existing.colorIndex != 0) {
            val existingWins = if (ROM.isCGB()) {
                existing.oamIndex <= sprite.oamIndex
            } else {
                existing.spriteX < sprite.spriteX ||
                        (existing.spriteX == sprite.spriteX && existing.oamIndex <= sprite.oamIndex)
            }
            if (existingWins) return
        }

        sprite.next = existing.next
        if (previous == null) {
            head = sprite
        } else {
            previous.next = sprite
        }
        if (tail === existing) {
            tail = sprite
        }
    }

    /** Links [newEntry] at the tail and updates the FIFO size. */
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

    /** Removes and returns the entry at the head, or null when the FIFO is empty. */
    fun pop(): FifoEntry? {
        val dequeuedEntry = head
        head = head?.next
        if (head == null) tail = null
        if (dequeuedEntry != null) size--
        return dequeuedEntry
    }

    /** Removes the head of the OBJ FIFO and returns it as a sprite entry. */
    fun popSprite(): FifoEntrySprite? {
        val dequeuedEntry = pop()
        return dequeuedEntry as? FifoEntrySprite
    }

    /** Reports whether the FIFO contains no entries. */
    fun isEmpty(): Boolean {
        return size == 0
    }

    /** Returns the number of queued entries. */
    fun getSize(): Int{
        return size
    }

    /** Removes every queued entry and resets the linked-list pointers. */
    fun clear() {
        head = null
        tail = null
        size = 0
    }
}

class FifoFetcher {
    private var state : FetcherState = FetcherState.OBTAIN_TILE
    private var objState : ObjFetcherState = ObjFetcherState.GET_TILE

    private var lineX : Int         = 0     // X position of the line. Actual X scan-line coordinate.
    private var fetchX : Int        = 0     // Tile X Coordinate to be fetched. Used to calculate mapX and obtain tiles from VRAM.
    private var pushedPixels : Int  = 0     // Pixels pushed to the screen

    private var backgroundFifo : Fifo   = Fifo()
    private var spriteFifo : Fifo       = Fifo()
    private var fifoX : Int = 0

    private var mapY: Int  = 0              // Global Y position of the map
    private var mapX: Int  = 0              // Global X position of the map
    private var tileY: Int = 0              // Line of the tile to be fetched

    private val videoBuffer: IntArray = IntArray(GB_Y_RESOLUTION * GB_X_RESOLUTION)
    private var bgTileData: FetchedTileData = FetchedTileData() // Fetched Tile Data
    private var spriteTileData: FetchedTileData = FetchedTileData() // Fetched Sprite Tile Data

    private var activeSprite: OAMObj? = null
    private var objStateDotsRemaining = 0
    private val processedSprites = BooleanArray(OAM_OBJ_NUMBER)
    
    private var windowStartedThisLine: Boolean = false
    private var windowPixelsInFifo: Int = 0

    /**
     * Executes one Mode 3 dot. It updates screen/map coordinates, arbitrates between
     * BG/WIN and OBJ fetching, and outputs a pixel only when OBJ fetching is not stalling.
     */
    fun process(){
        initWindow()
        
        val scx = Memory.getByteOnAddress(SCX).toInt() and 0xFF
        val scy = Memory.getByteOnAddress(SCY).toInt() and 0xFF
        val ly  = Memory.getByteOnAddress(LY_ADDR).toInt() and 0xFF

        mapY = scy + ly
        mapX = scx + fetchX
        tileY = (mapY % 8) * 2

        val canRenderPixel = fetch()
        if (canRenderPixel) {
            pushPixelsToBuffer() // Push pixels to pipeline
        }
    }

    /**
     * Selects the fetcher that owns the current dot. BG/WIN advances on even dots;
     * an active OBJ fetch advances every dot and blocks pixel output until it finishes.
     *
     * @return true when the pixel FIFOs may advance during this dot.
     */
    private fun fetch(): Boolean {
        if (objState == ObjFetcherState.GET_TILE) {
            val sprite = findSpriteToFetch()

            if (sprite != null) {
                startSpriteFetch(sprite)
                return tickObjFetcher()
            } else if (PPU.getLineTicks() % 2 == 0) {
                tickBGWINFetcher()
            }

            return true
        }

        return tickObjFetcher()
    }

    /** Advances the BG/WIN state machine by one fetcher step. */
    private fun tickBGWINFetcher(){
        when(state){
            FetcherState.OBTAIN_TILE -> getTile()               // Fetch the current tile identification in the BG Tilemap
            FetcherState.LOW_DATA_TILE -> getTileLowData()      // Fetch the low byte of the tile
            FetcherState.HIGH_DATA_TILE -> getTileHighData()    // Fetch the high byte of the tile
            FetcherState.SLEEP -> sleepState()
            FetcherState.PUSH -> pushState()
        }
    }

    /**
     * Advances the active OBJ state by one dot.
     *
     * @return false while OBJ is stalling the pixel pipeline; true when output may resume.
     */
    private fun tickObjFetcher(): Boolean {
        return when (objState) {
            ObjFetcherState.GET_TILE -> true
            ObjFetcherState.SYNC_BG_FETCHER -> {
                syncBgFetcher()
                false
            }
            ObjFetcherState.SCX_PENALTY -> {
                applyScxPenalty()
                false
            }
            ObjFetcherState.ADVANCE_FIRST -> {
                consumeDots(1, ObjFetcherState.ADVANCE_SECOND)
                false
            }
            ObjFetcherState.ADVANCE_SECOND -> {
                consumeDots(3, ObjFetcherState.LOW_DATA_TILE)
                false
            }
            ObjFetcherState.LOW_DATA_TILE -> {
                getObjTileLowData()
                false
            }
            ObjFetcherState.HIGH_DATA_TILE -> {
                getObjTileHighData()
                false
            }
            ObjFetcherState.PUSH -> {
                pushSpritePixels()
                false
            }
            ObjFetcherState.FINISH -> {
                finishObjFetch()
                true
            }
        }
    }

    /**
     * Finds the next unprocessed scan-line sprite whose visible left edge equals the
     * current output X. Sprites clipped by the left edge are triggered at X=0.
     */
    private fun findSpriteToFetch() : OAMObj?{
        if (!PPU.objsAreEnabled() && !ROM.isCGB()) return null

        return PPU.getFetchedSpriteEntries()
            .filterNotNull()
            .firstOrNull { obj ->
                val spriteX = (obj.x.toInt() and 0xFF) - OAM_X_OFFSET
                val triggerX = maxOf(0, spriteX)

                !processedSprites[obj.oamIndex] &&
                        triggerX == pushedPixels
            }
    }

    /** Initializes the temporary OBJ context and starts synchronization with BG/WIN. */
    private fun startSpriteFetch(sprite: OAMObj) {
        activeSprite = sprite
        spriteTileData.tileIndex = sprite.tile.toInt() and 0xFF
        spriteTileData.lowData = 0
        spriteTileData.highData = 0
        objStateDotsRemaining = 0
        objState = ObjFetcherState.SYNC_BG_FETCHER
    }

    /** Ensures that BG/WIN has pixels available before the OBJ fetch continues. */
    private fun syncBgFetcher() {
        if (backgroundFifo.isEmpty()) {
            tickBGWINFetcher()
        }

        if (!backgroundFifo.isEmpty()) {
            objState = ObjFetcherState.SCX_PENALTY
        }
    }

    /** Applies the `SCX & 7` timing penalty to an OBJ touching the left screen edge. */
    private fun applyScxPenalty() {
        val spriteX = ((activeSprite?.x?.toInt() ?: OAM_X_OFFSET) and 0xFF) - OAM_X_OFFSET
        val penalty = if (spriteX <= 0) PPU.getScrollX() and 0b111 else 0

        if (penalty == 0) {
            objState = ObjFetcherState.ADVANCE_FIRST
        } else {
            consumeDots(penalty, ObjFetcherState.ADVANCE_FIRST)
        }
    }

    /** Keeps the current OBJ state active for [dots] calls, then enters [nextState]. */
    private fun consumeDots(dots: Int, nextState: ObjFetcherState) {
        if (objStateDotsRemaining == 0) {
            objStateDotsRemaining = dots
        }

        objStateDotsRemaining--
        if (objStateDotsRemaining <= 0) {
            objStateDotsRemaining = 0
            objState = nextState
        }
    }

    /** Marks the active OAM entry as processed and releases the OBJ fetcher. */
    private fun finishObjFetch() {
        activeSprite?.let {
            processedSprites[it.oamIndex] = true
        }
        activeSprite = null
        objState = ObjFetcherState.GET_TILE
    }

    /** Fetches the next BG/WIN tile index and advances the tile-space X coordinate. */
    private fun getTile(){
        if(PPU.lcdIsEnabled())
            getBGTile()

        state = FetcherState.LOW_DATA_TILE
        fetchX += 8
    }

    /**
     * Determines which background/window tile to fetch pixels from.
     * By default, the tilemap used is the one at 0x9800.
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

    /** Reads the low bitplane byte for the selected BG/WIN tile row. */
    private fun getTileLowData(){
        val offset = calculateTileDataOffset()
        bgTileData.lowData = Memory.getByteOnAddress(PPU.getAddrModeAddr() + ((bgTileData.tileIndex) * 16) + offset)
        state = FetcherState.HIGH_DATA_TILE
    }

    /** Reads the high bitplane byte for the selected BG/WIN tile row. */
    private fun getTileHighData(){
        val offset = calculateTileDataOffset()
        bgTileData.highData = Memory.getByteOnAddress(PPU.getAddrModeAddr() + ((bgTileData.tileIndex) * 16) + (offset + 1))
        state = FetcherState.SLEEP
    }

    /** Reads the low bitplane byte for the active OBJ row. */
    private fun getObjTileLowData(){
        val address = getObjTileDataAddress()
        spriteTileData.lowData = PPU.readFromVRAM(address)
        objState = ObjFetcherState.HIGH_DATA_TILE
    }

    /** Reads the high bitplane byte for the active OBJ row. */
    private fun getObjTileHighData(){
        val address = getObjTileDataAddress()
        spriteTileData.highData = PPU.readFromVRAM(address + 1)
        objState = ObjFetcherState.PUSH
    }

    /**
     * Resolves the VRAM address of the active OBJ row, including 8x16 tile selection
     * and vertical flipping.
     */
    private fun getObjTileDataAddress(): Int {
        val sprite = activeSprite ?: return VRAM_START
        val spriteHeight = if (LCDCObj.OBJ_SIZE.get(Memory.getByteOnAddress(LCDC_ADDR)) == 0) 8 else 16
        val spriteY = (sprite.y.toInt() and 0xFF) - OAM_Y_OFFSET
        var row = PPU.getLY() - spriteY

        if (ObjFlags.Y_FLIP.get(sprite.flags) == 1) {
            row = spriteHeight - 1 - row
        }

        var tileIndex = sprite.tile.toInt() and 0xFF
        if (spriteHeight == 16) {
            tileIndex = (tileIndex and 0xFE) + (row / PIXELS_PER_TILE)
            row %= PIXELS_PER_TILE
        }

        spriteTileData.tileIndex = tileIndex
        return VRAM_START + (tileIndex * 16) + (row * 2)
    }

    /** Advances BG/WIN from its idle fetch step to the FIFO push attempt. */
    private fun sleepState(){
        state = FetcherState.PUSH
    }

    /** Pushes a decoded BG/WIN row when space exists, then starts the next tile. */
    private fun pushState(){
        val bgPush = pushBGPixelsToFifo()
        if(bgPush){
            state = FetcherState.OBTAIN_TILE
        }
    }

    /**
     * Decodes both BG/WIN bitplanes into eight pixels and appends them to the BG FIFO.
     * Each entry retains its raw index for the later BG/OBJ priority decision.
     */
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
            val colorIndex = if(PPU.bgWinIsEnabled()) high or low else 0
            val color = PPU.getColorIndex(colorIndex)

            if(x >= 0){
                backgroundFifo.push(color, colorIndex)
                fifoX++
                if (windowStartedThisLine) windowPixelsInFifo++
            }
        }

        return true
    }

    /**
     * Decodes the active sprite row and merges its eight screen-relative positions
     * into the OBJ FIFO, respecting transparency, X flip, and sprite overlap priority.
     */
    private fun pushSpritePixels(){
        val sprite = activeSprite
        if (sprite == null) {
            objState = ObjFetcherState.FINISH
            return
        }

        spriteFifo.ensureSpriteSize(PIXELS_PER_TILE)
        val spriteX = (sprite.x.toInt() and 0xFF) - OAM_X_OFFSET

        for (pixelIndex in 0 until PIXELS_PER_TILE) {
            val screenX = spriteX + pixelIndex
            val fifoOffset = screenX - pushedPixels
            if (fifoOffset !in 0 until PIXELS_PER_TILE) continue

            val bit = if (ObjFlags.X_FLIP.get(sprite.flags) == 1) {
                pixelIndex
            } else {
                (PIXELS_PER_TILE - 1) - pixelIndex
            }
            val low = ((spriteTileData.lowData.toInt() and 0xFF) shr bit) and 1
            val high = (((spriteTileData.highData.toInt() and 0xFF) shr bit) and 1) shl 1
            val colorIndex = high or low

            spriteFifo.mergeSpriteAt(
                fifoOffset,
                FifoEntrySprite(
                    colorIndex = colorIndex,
                    palette = ObjFlags.DMG_PALETTE.get(sprite.flags),
                    priority = ObjFlags.PRIORITY.get(sprite.flags),
                    oamIndex = sprite.oamIndex,
                    spriteX = spriteX
                )
            )
        }

        objState = ObjFetcherState.FINISH
    }

    /** Chooses BG/WIN or OBJ and applies the selected DMG object palette when needed. */
    private fun mixPixels(backgroundPixel: FifoEntry, spritePixel: FifoEntrySprite?): Int{
        if (spritePixel == null || spritePixel.colorIndex == 0)
            return backgroundPixel.value

        if(backgroundPixel.colorIndex != 0 && spritePixel.priority == 1)
            return backgroundPixel.value

        return PPU.getObjColorIndex(spritePixel.colorIndex, spritePixel.palette)
    }

    /**
     * Mode 3: PPU transfers pixels to the LCD
     */
    private fun pushPixelsToBuffer(){
        if(backgroundFifo.getSize() >= PIXELS_PER_TILE){ // Process pixels if the FIFO has at least 8
            val backgroundPixel = backgroundFifo.pop()
            val scx = PPU.getScrollX()

            // Check that Coordinate X is inside the visible region of the screen
            // Window pixels are never discarded
            if((windowStartedThisLine || lineX >= (scx % PIXELS_PER_TILE)) && backgroundPixel != null){
                val spritePixel = if (!spriteFifo.isEmpty()) spriteFifo.popSprite() else null
                val ly = PPU.getLY()
                val address = pushedPixels + (ly * GB_X_RESOLUTION) // Address = Pixels already pushed + (Actual Line * X Resolution)

                val finalColor = mixPixels(backgroundPixel, spritePixel)

                putValueToVideoBuffer(address, finalColor)
                pushedPixels++
            }
            lineX++
        }
    }

    /** Clears both pixel FIFOs without modifying the frame buffer. */
    fun clear(){
        backgroundFifo.clear()
        spriteFifo.clear()
    }

    /** Restores every per-scanline fetch state before entering Mode 3. */
    fun resetParams(){
        state = FetcherState.OBTAIN_TILE
        lineX = 0
        fetchX = 0
        pushedPixels = 0
        fifoX = 0
        windowStartedThisLine = false
        windowPixelsInFifo = 0
        objState = ObjFetcherState.GET_TILE
        activeSprite = null
        objStateDotsRemaining = 0
        processedSprites.fill(false)
        bgTileData = FetchedTileData()
        spriteTileData = FetchedTileData()
        clear()
    }

    /** Returns the number of visible pixels already written on the current scan-line. */
    fun getPushedPixels(): Int{
        return pushedPixels
    }

    /** Writes one final ARGB pixel into the frame buffer. */
    private fun putValueToVideoBuffer(address: Int, value: Int){
        videoBuffer[address] = value
    }

    /** Reads one ARGB pixel from the frame buffer. */
    fun getValueFromVideoBuffer(address: Int): Int{
        return videoBuffer[address]
    }
    
    /**
     * Switches the BG/WIN fetch source when the current output reaches `WX - 7` on an
     * eligible line. The BG FIFO is discarded, but OBJ pixels remain screen-aligned.
     */
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
            backgroundFifo.clear()
        }
    }

    /** Reports whether tile fetches currently come from the Window tilemap. */
    private fun isWindowTile(): Boolean{
        return windowStartedThisLine
    }

    /** Calculates the two-byte row offset for the active BG or Window tile. */
    private fun calculateTileDataOffset(): Int{
        val ly = PPU.getLY()
        val wy = PPU.getWindowScreenY()

        val isWindowTile = isWindowTile()
        return if (isWindowTile) ((ly - wy) % 8) * 2 else tileY
    }
}
