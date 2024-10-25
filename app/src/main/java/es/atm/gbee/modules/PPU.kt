package es.atm.gbee.modules

import android.os.SystemClock
import kotlin.io.encoding.Base64

const val SIGNED_TILE_REGION : Int = 0x8800

const val TM_1_START : Int  = 0x9800 // TileMap 1 Start Address
const val TM_1_END : Int    = 0x9BFF // TileMap 1 End Address
const val TM_2_START : Int  = 0x9C00 // TileMap 2 Start Address
const val TM_2_END : Int    = 0x9FFF // TileMap 2 End Address
const val LCD_STAT : Int    = 0xFF41 // LCD STATUS
const val LCDC_ADDR : Int   = 0xFF40 // LCDC - LCD Control
const val LY_ADDR : Int     = 0xFF44 // LCD Y Coordinate, values range [0 - 153]. 144 to 153 = VBlank
const val LYC_ADDR : Int    = 0xFF45 // LY Comparation

const val SCY : Int         = 0xFF42 // Scroll Y Position - Values Range: [0 - 255]
const val SCX : Int         = 0xFF43 // Scroll X Position
const val WY : Int          = 0xFF4A // Window Y Position
const val WX : Int          = 0xFF4B // Window X Position

const val BGP : Int         = 0xFF47 // Background Palette - Non-CGB
const val OBP0 : Int        = 0xFF48 // Object Palette 0 - Non-CGB
const val OBP1 : Int        = 0xFF49 // Object Palette 1 - Non-CGB

const val C_BGP_INDEX : Int = 0xFF68 // Background Palette INDEX - CGB
const val C_BGP_DATA : Int  = 0xFF69 // Background Palette DATA - CGB
const val C_BGP_OCPS : Int  = 0xFF68 // Not Used - Reserved
const val C_BGP_OBPI : Int  = 0xFF68 // Not Used - Reserved

const val OAM_CYCLES            = 80
const val MAX_OBJ_PER_SCANLINE  = 10
const val OAM_OBJ_NUMBER        = 40
const val PIXEL_TRANSFER_CYCLES = 172 // TODO: Penalties Algorithm
const val HBLANK_CYCLES         = 204
const val LINE_TOTAL_TICKS      = 456

const val TOTAL_LINES           = 154

const val GB_X_RESOLUTION       = 160
const val GB_Y_RESOLUTION       = 144

const val GB_FPS                = 1000 / 60

// --- LCDC Bit Masks (0xFF40) ---
enum class LCDCObj(val shift: Int) {
    MASTER_ENABLE(0),   // Non-CGB: If 0, both BG and Window become white. OBJ are still displayed. CGB: BG and Window lose their priority. OBJs are always displayed on top.
    OBJ_ENABLE(1),      // If OBJs should be displayed
    OBJ_SIZE(2),        // OBJ size. 0 = 8x8, 1 = 8x16 pixels
    BG_TILEMAP(3),      // Similar to bit 6. If 0, the BG uses the tilemap in 9800, otherwise 9C00
    ADDRESS_MODE(4),    // Controls addressing mode for the BG and Window. 0 = 8800–97FF; 1 = 8000–8FFF
    WINDOW_ENABLE(5),   // Controls whether the Window shall be displayed
    WIN_TILEMAP(6),     // Controls which bg map the Window uses. 0 = 9800–9BFF; 1 = 9C00-9FFF
    LCDC_ENABLE(7);     // Controls whether the LCD is on and the PPU active

    fun get(value: Byte): Int {
        return (value.toInt() shr shift) and 0b1
    }

    fun set(value: Byte, newValue: Int): Byte {
        val clearedValue = value.toInt() and ((0b1 shl shift).inv())
        return (clearedValue or ((newValue and 0b1) shl shift)).toByte()
    }
}

// --- LCD STATUS Bit Masks (0xFF41) ---
enum class StatObj(val shift: Int, val mask: Int) {
    PPU_MODE(0, 0b11),                  // 0-1 bits. PPU current status. 00 (HBlank - Mode 0), 01 (VBlank - Mode 1), 10 (OAM Scan - Mode 2), 11 (Drawin Pixels - Mode 3)
    COINCIDENCE_FLAG(2, 0b1),           // Set if LY == LYC
    HBLANK_INTERRUPT(3, 0b1),           // If set, the interrupt will trigger when the LCD is in HBlank period (mode 0)
    VBLANK_INTERRUPT(4, 0b1),           // If set, the interrupt will trigger when the LCD is in VBlank period (mode 1)
    OAM_INTERRUPT(5, 0b1),              // If set, the interrupt will trigger when the PPU access to OAM (mode 2)
    COINCIDENCE_INTERRUPT(6, 0b1);      // If set, the interrupt will trigger on LYC == LY condition

    fun get(value: Byte): Int {
        return (value.toInt() shr shift) and mask
    }

    fun set(value: Byte, newValue: Int): Byte {
        val clearedValue = value.toInt() and ((mask shl shift).inv())
        return (clearedValue or ((newValue and mask) shl shift)).toByte()
    }
}

enum class PPUMode(val number: Int){
    HBlank(0),
    VBlank(1),
    OAM(2),
    DRAW_LCD(3)
}

/*   OAM Obj Flags:
*    Bit 7 - Priority: 0 = No, 1 = BG and Window colors 1–3 are drawn over this OBJ
*    Bit 6 - Y flip: 0 = Normal, 1 = Entire OBJ is vertically mirrored
*    Bit 5 - X flip: 0 = Normal, 1 = Entire OBJ is horizontally mirrored
*    Bit 4 - DMG palette [Non CGB Mode only]: 0 = OBP0, 1 = OBP1
*    Bit 3 - Bank [CGB Mode Only]: 0 = Fetch tile from VRAM bank 0, 1 = Fetch tile from VRAM bank 1
*    Bits 2, 1, 0 - CGB palette [CGB Mode Only]: Which of OBP0–7 to use
*/
data class OAMObj(
    val y: Byte,
    val x: Byte,
    val tile: Byte,
    val flags: Byte
)

object PPU {

    private var currentFrame : Int          = 0
    private var frameCount : Int            = 0
    private var lineTicks : Int             = 0
    private var prevFrameTime : Long        = 0
    private var startTimer : Long           = 0

    private var oamRam: ByteArray           = ByteArray(OAM_OBJ_NUMBER * 4) { 0 } // Max number: 40 OAM Objs * 4 Bytes each
    private var vRam : ByteArray            = ByteArray((VRAM_END - VRAM_START) + 1)

    private var ppuEnabled : Boolean        = true          // Bit 7
    private var lcdEnabled : Boolean        = false
    private var winTilemapAddr : Int        = TM_1_START    // Bit 6
    private var enabledWindow : Boolean     = false         // Bit 5
    private var addrModeAddr : Int          = VRAM_START    // Bit 4 - Where the tiles are stored - 0 = 8800–97FF; 1 = 8000–8FFF
    private var bgTilemapAddr : Int         = TM_1_START    // Bit 3 - Where the BG TileMap is stored - 0 = 9800–9BFF; 1 = 9C00-9FFF
    private var objSize : Int               = 8             // Bit 2
    private var objEnabled : Boolean        = false         // Bit 1
    private var bgWinEnabled : Boolean      = true          // Bit 0

    private var lineSpriteCount: Int = 0
    private var objsFetched : Array<OAMObj> = Array(MAX_OBJ_PER_SCANLINE) { OAMObj(0, 0, 0, 0) }

    private val fifoFetcher : FifoFetcher = FifoFetcher()

    enum class PALETTE_TYPE(){
        BASIC_PL,
        GREENER_PL,
        C_DEFAULT_PL,
        C_RED_PL,
        C_BROWN_PL,
        C_BLUE_PL,
        C_GRAY_PL,
        C_PINK_PL,
        C_ORANGE_PL,
        C_YELLOW_PL,
        C_TEAL_PL,
        C_REDGREEN_PL,
        C_BLUERED_PL,
        C_MONOCHRMGRAY_PL,
        C_MONOCHRMYELLOW_PL
    }

    enum class COLOR_INDEX(number: Int){
        WHITE(0),
        LIGHT_GRAY(1),
        DARK_GRAY(2),
        BLACK(3)
    }

    // Colors follows as: White - Light Gray - Dark Gray - Black
    private val palettes: HashMap<PALETTE_TYPE, IntArray> = hashMapOf(
        Pair(PALETTE_TYPE.BASIC_PL, intArrayOf(0xFFFFFFFF.toInt(), 0xFFAAAAAA.toInt(), 0xFF555555.toInt(), 0xFF000000.toInt())),
        Pair(PALETTE_TYPE.GREENER_PL, intArrayOf(0xFFE0F8D0.toInt(), 0xFF88C070.toInt(), 0xFF346856.toInt(), 0xFF081820.toInt())),
        Pair(PALETTE_TYPE.C_DEFAULT_PL, intArrayOf(0xFF000000.toInt(), 0xFF00A800.toInt(), 0xFF54FC54.toInt(), 0xFFFFFFFF.toInt())),
        Pair(PALETTE_TYPE.C_RED_PL, intArrayOf(0xFF000000.toInt(), 0xFFB80000.toInt(), 0xFFFF3030.toInt(), 0xFFFFFFFF.toInt())),
        Pair(PALETTE_TYPE.C_BROWN_PL, intArrayOf(0xFF000000.toInt(), 0xFF785000.toInt(), 0xFFD0A060.toInt(), 0xFFFFFFFF.toInt())),
        Pair(PALETTE_TYPE.C_BLUE_PL, intArrayOf(0xFF000000.toInt(), 0xFF0000B8.toInt(), 0xFF3030FF.toInt(), 0xFFFFFFFF.toInt())),
        Pair(PALETTE_TYPE.C_GRAY_PL, intArrayOf(0xFF000000.toInt(), 0xFF555555.toInt(), 0xFFAAAAAA.toInt(), 0xFFFFFFFF.toInt())),
        Pair(PALETTE_TYPE.C_PINK_PL, intArrayOf(0xFF000000.toInt(), 0xFFB800B8.toInt(), 0xFFFF54FF.toInt(), 0xFFFFFFFF.toInt())),
        Pair(PALETTE_TYPE.C_ORANGE_PL, intArrayOf(0xFF000000.toInt(), 0xFFB85400.toInt(), 0xFFFFAA30.toInt(), 0xFFFFFFFF.toInt())),
        Pair(PALETTE_TYPE.C_YELLOW_PL, intArrayOf(0xFF000000.toInt(), 0xFFB8B800.toInt(), 0xFFFFFF54.toInt(), 0xFFFFFFFF.toInt())),
        Pair(PALETTE_TYPE.C_TEAL_PL, intArrayOf(0xFF000000.toInt(), 0xFF00B8B8.toInt(), 0xFF54FFFF.toInt(), 0xFFFFFFFF.toInt())),
        Pair(PALETTE_TYPE.C_REDGREEN_PL, intArrayOf(0xFF000000.toInt(), 0xFF00B800.toInt(), 0xFFFF54FF.toInt(), 0xFFFFFFFF.toInt())),
        Pair(PALETTE_TYPE.C_BLUERED_PL, intArrayOf(0xFF000000.toInt(), 0xFF0000B8.toInt(), 0xFFFF3030.toInt(), 0xFFFFFFFF.toInt())),
        Pair(PALETTE_TYPE.C_MONOCHRMGRAY_PL, intArrayOf(0xFF000000.toInt(), 0xFF555555.toInt(), 0xFFAAAAAA.toInt(), 0xFFFFFFFF.toInt())),
        Pair(PALETTE_TYPE.C_MONOCHRMYELLOW_PL, intArrayOf(0xFF000000.toInt(), 0xFFB8B800.toInt(), 0xFFFFFF54.toInt(), 0xFFFFFFFF.toInt()))
    )

    private var selectedPalette = PALETTE_TYPE.BASIC_PL

    fun init() {
        Memory.write(LCD_STAT, 0x81.toByte())
        Memory.write(LCDC_ADDR, 0x91.toByte())
        Memory.write(SCY, 0)
        Memory.write(SCX, 0)
        Memory.write(LY_ADDR, 0)
        Memory.write(LYC_ADDR, 0)
        Memory.write(WY, 0)
        Memory.write(WX, 0)
        val consoleType = ROM.getConsole()

        if(consoleType == ROM.CONSOLE_TYPE.DMG || consoleType == ROM.CONSOLE_TYPE.DMG_CGB) {
            Memory.write(BGP, 0xFC.toByte())
            Memory.write(OBP0, 0xFF.toByte())
            Memory.write(OBP1, 0xFF.toByte())
        }else{
            //TODO - CGB
        }

        handleLCDC(0x91.toByte())
    }

    fun tick(){
        if(moduleIsActive()) {
            lineTicks++
            val stat = Memory.getByteOnAddress(LCD_STAT)

            when (StatObj.PPU_MODE.get(stat)) {
                PPUMode.HBlank.number -> hBlankMode(stat)
                PPUMode.VBlank.number -> vBlankMode(stat)
                PPUMode.OAM.number -> oamMode(stat)
                PPUMode.DRAW_LCD.number -> drawLCDMode(stat)
            }
        }
    }

    private fun oamMode(stat: Byte){
        if(lineTicks >= OAM_CYCLES){ // ENTER DRAWING PIXELS MODE
            Memory.write(LCD_STAT, StatObj.PPU_MODE.set(stat, PPUMode.DRAW_LCD.number))
            fifoFetcher.resetParams() // Reset FIFO Fetcher
        }else if(lineTicks == 1){ // Scanning -> Read OAM on the first tick
            lineSpriteCount = 0
            loadLineSprites()
        }
    }

    private fun drawLCDMode(stat: Byte){

        fifoFetcher.process()

        if(fifoFetcher.getPushedPixels() >= GB_X_RESOLUTION){ // ENTER HBLANK MODE

            fifoFetcher.clear()

            Memory.write(LCD_STAT, StatObj.PPU_MODE.set(stat, PPUMode.HBlank.number))

            if(StatObj.HBLANK_INTERRUPT.get(stat) != 0)
                Interrupt.requestInterrupt(Interrupt.InterruptType.LCD_STAT.getByteMask()) // ASK FOR LCD STAT INTERRUPT IF LCD_STAT HAS THE HBLANK BIT ACTIVATED
        }
    }

    private fun hBlankMode(stat: Byte){
        if(lineTicks >= LINE_TOTAL_TICKS){
            increment_LY()

            val ly = (Memory.getByteOnAddress(LY_ADDR).toInt()) and 0xFF

            if(ly >= GB_Y_RESOLUTION){ // ENTER VBLANK MODE
                Memory.write(LCD_STAT, StatObj.PPU_MODE.set(stat, PPUMode.VBlank.number))

                Interrupt.requestInterrupt(Interrupt.InterruptType.VBLANK.getByteMask()) // ASK FOR VBLANK INTERRUPT

                if(StatObj.VBLANK_INTERRUPT.get(stat) != 0)
                    Interrupt.requestInterrupt(Interrupt.InterruptType.LCD_STAT.getByteMask()) // ASK FOR LCD STAT INTERRUPT IF LCD_STAT HAS THE VBLANK BIT ACTIVATED

                currentFrame++

                calculateFPS()

            }else{ // RETURN TO OAM MODE
                Memory.write(LCD_STAT, StatObj.PPU_MODE.set(stat, PPUMode.OAM.number))

                if(StatObj.OAM_INTERRUPT.get(stat) != 0)
                    Interrupt.requestInterrupt(Interrupt.InterruptType.LCD_STAT.getByteMask()) // ASK FOR LCD STAT INTERRUPT IF LCD_STAT HAS THE OAM BIT ACTIVATED
            }

            lineTicks = 0
        }
    }

    private fun vBlankMode(stat: Byte){
        if(lineTicks >= LINE_TOTAL_TICKS){
            increment_LY()

            val ly = (Memory.getByteOnAddress(LY_ADDR).toInt()) and 0xFF

            if(ly >= TOTAL_LINES){ // RETURN TO OAM MODE
                Memory.write(LCD_STAT, StatObj.PPU_MODE.set(stat, PPUMode.OAM.number))
                Memory.write(LY_ADDR, 0)

                if(StatObj.OAM_INTERRUPT.get(stat) != 0)
                    Interrupt.requestInterrupt(Interrupt.InterruptType.LCD_STAT.getByteMask()) // ASK FOR LCD STAT INTERRUPT IF LCD_STAT HAS THE OAM BIT ACTIVATED
            }

            lineTicks = 0
        }
    }

    private fun increment_LY(){
        val ly = Memory.getByteOnAddress(LY_ADDR)
        val newLY = ((ly.toInt() and 0xFF) + 1).toByte()
        Memory.write(LY_ADDR, newLY)

        compare_LY_LYC()
    }

    private fun compare_LY_LYC(){
        val lyc     = Memory.getByteOnAddress(LYC_ADDR)
        val ly      = Memory.getByteOnAddress(LY_ADDR)
        val stat    = Memory.getByteOnAddress(LCD_STAT)

        Memory.write(LCD_STAT, StatObj.COINCIDENCE_FLAG.set(stat, if (lyc == ly) 0x1 else 0x0))

        if(lyc == ly && StatObj.COINCIDENCE_INTERRUPT.get(stat) != 0){
            Interrupt.requestInterrupt(Interrupt.InterruptType.LCD_STAT.getByteMask())
        }
    }

    fun readFromOAM(address: Int) : Byte{
        var arrayAddress = address and 0xFFFF

        if(address >= OAM_START){
            arrayAddress -= 0xFE00
        }

        return oamRam[arrayAddress]
    }

    fun writeToOAM(address: Int, startAddress: Int, value: Byte){

        var arrayAddress = address and 0xFFFF

        if(startAddress != -1)
            arrayAddress -= startAddress
        else if(address >= OAM_START)
            arrayAddress -= 0xFE00

        oamRam[arrayAddress] = value
        Memory.write(address, value)
    }

    fun writeToLCD(address: Int, value: Byte){
        when (address) {
            DMA_RGSTR -> {
                DMA.start(value)
            }
            LCDC_ADDR -> {
                Memory.write(address, value)
                handleLCDC(value)
            }
            in OBP0 .. OBP1 -> {
                Memory.write(address, ((value.toInt() and 0xFF) and 0b11111100).toByte())
            }
            else -> {
                Memory.write(address, value)
            }
        }
    }

    private fun handleLCDC(value: Byte){
        // Bit 7
        ppuEnabled = LCDCObj.LCDC_ENABLE.get(value) != 0
        lcdEnabled = ppuEnabled
        // Bit 6
        winTilemapAddr = if(LCDCObj.WIN_TILEMAP.get(value) == 0) TM_1_START else TM_2_START
        // Bit 5
        enabledWindow = LCDCObj.WINDOW_ENABLE.get(value) != 0
        // Bit 4
        addrModeAddr = if(LCDCObj.ADDRESS_MODE.get(value) == 0) SIGNED_TILE_REGION else VRAM_START
        // Bit 3
        bgTilemapAddr = if(LCDCObj.BG_TILEMAP.get(value) == 0) TM_1_START else TM_2_START
        // Bit 2
        objSize = if(LCDCObj.OBJ_SIZE.get(value) == 0) 8 else 16
        // Bit 1
        objEnabled = LCDCObj.OBJ_ENABLE.get(value) != 0
        // Bit 0
        bgWinEnabled = LCDCObj.MASTER_ENABLE.get(value) != 0
    }

    private fun loadLineSprites(){
        val ly = Memory.getByteOnAddress(LY_ADDR).toInt() and 0xFF
        val lcdc = Memory.getByteOnAddress(LCDC_ADDR)
        val objSize = LCDCObj.OBJ_SIZE.get(lcdc)
        val spriteHeight = if(objSize == 0) 8 else 16

        for(i in oamRam.indices step 4){

            if(lineSpriteCount >= MAX_OBJ_PER_SCANLINE){
                break
            }

            val y = oamRam[i].toInt() and 0xFF
            val x = oamRam[i + 1].toInt() and 0xFF
            val tile = oamRam[i + 2].toInt() and 0xFF
            val flags = oamRam[i + 3].toInt() and 0xFF

            if(x == 0){ // Sprite not visible
                continue
            }

            if(y <= ly + 16 && (y + spriteHeight) > ly + 16){ // Sprite on current line
                val fetched = OAMObj(y.toByte(), x.toByte(), tile.toByte(), flags.toByte())
                objsFetched[lineSpriteCount] = fetched
                lineSpriteCount++
            }
        }
    }

    fun readFromVRAM(address: Int) : Byte{
        return if(!ROM.isCGB()){
            Memory.read(address)
        }else{
            vRam[address - VRAM_START]
        }
    }

    fun writeToVRAM(address: Int, value: Byte){
        if(!ROM.isCGB()){
            Memory.write(address, value)
        }else{
            vRam[address - VRAM_START] = value
        }
    }

    fun getPaletteColors(palette: PALETTE_TYPE) : IntArray{
        return palettes[palette]!!
    }

    private fun calculateFPS(){
        val now = SystemClock.currentThreadTimeMillis()
        val frameTime = now - prevFrameTime

        if(frameTime < GB_FPS){
            Thread.sleep(GB_FPS - frameTime)
        }

        if(now - startTimer >= 1000){
            val fps = frameCount
            startTimer = now
            frameCount = 0

        }

        frameCount++
        prevFrameTime = SystemClock.currentThreadTimeMillis()
    }

    private fun moduleIsActive(): Boolean{
        return ppuEnabled
    }

    fun lcdIsEnabled(): Boolean{
        return lcdEnabled
    }

    fun windowIsEnabled(): Boolean{
        return enabledWindow
    }

    fun getLineTicks(): Int{
        return lineTicks
    }

    fun getBGTilemapAddr(): Int{
        return bgTilemapAddr
    }

    fun getWinTilemapAddr(): Int{
        return winTilemapAddr
    }

    fun getAddrModeAddr(): Int{
        return addrModeAddr
    }

    fun getColorIndex(index: Int): Int{
        val tileColors = getPaletteColors(selectedPalette)
        return tileColors[index]
    }

    fun getBufferPixelFromIndex(index: Int): Int{
        return fifoFetcher.getValueFromVideoBuffer(index)
    }
}