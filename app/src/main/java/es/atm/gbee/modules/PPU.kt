package es.atm.gbee.modules

const val TM_1_START : Int  = 0x9800 // TileMap 1 Start Address
const val TM_1_END : Int    = 0x9BFF // TileMap 1 End Address
const val TM_2_START : Int  = 0x9C00 // TileMap 2 Start Address
const val TM_2_END : Int    = 0x9FFF // TileMap 2 End Address
const val LCD_STAT : Int    = 0xFF41 // LCD STATUS
const val LCDC_ADDR : Int   = 0xFF40 // LCDC - LCD Control
const val LY_ADDR : Int     = 0xFF44
const val LYC_ADDR : Int    = 0xFF45

const val SCY : Int         = 0xFF42 // Background Viewport Y Position
const val SCX : Int         = 0xFF43 // Background Viewport X Position
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
const val PIXEL_TRANSFER_CYCLES = 172
const val HBLANK_CYCLES         = 204
const val VBLANK_CYCLES         = 456

const val TOTAL_LINES           = 154

// --- LCDC Bit Masks (0xFF40) ---
enum class LCDCObj(val shift: Int) {
    MASTER_ENABLE(0),   // Non-CGB: If 0, both BG and Window become white. OBJ are still displayed. CGB: BG and Window lose their priority. OBJs are always displayed on top.
    OBJ_ENABLE(1),      // If OBJs should be displayed
    OBJ_SIZE(2),        // OBJ size. 0 = 8x8, 1 = 8x16 pixels
    BG_AREA(3),         // Similar to bit 6. If 0, the BG uses the tilemap in 9800, otherwise 9C00
    ADDRESS_MODE(4),    // Controls addressing mode for the BG and Window
    WINDOW_ENABLE(5),   // Controls whether the Window shall be displayed
    BG_TILEMAP(6),      // Controls which bg map the Window uses. 0 = 9800–9BFF; 1 = 9C
    LCDC_ENABLE(7);      // Controls whether the LCD is on and the PPU active

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

data class OAMObj(
    val x: Byte,
    val y: Byte,
    val tile: Byte,
    val flags: Byte
)

/*   OAM Obj Flags:
*    Bit 7 - Priority: 0 = No, 1 = BG and Window colors 1–3 are drawn over this OBJ
*    Bit 6 - Y flip: 0 = Normal, 1 = Entire OBJ is vertically mirrored
*    Bit 5 - X flip: 0 = Normal, 1 = Entire OBJ is horizontally mirrored
*    Bit 4 - DMG palette [Non CGB Mode only]: 0 = OBP0, 1 = OBP1
*    Bit 3 - Bank [CGB Mode Only]: 0 = Fetch tile from VRAM bank 0, 1 = Fetch tile from VRAM bank 1
*    Bits 2, 1, 0 - CGB palette [CGB Mode Only]: Which of OBP0–7 to use
*/

object PPU {

    private var currentFrame : Int = 0
    private var oamRam: ByteArray = ByteArray(40 * 4) // Max number: 40 OAM Objs * 4 Bytes
    private var vRam : ByteArray = ByteArray(2000)

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

    fun tick(){


    }

    fun compare_LY_LYC(){
        val lyc     = Memory.getByteOnAddress(LYC_ADDR)
        val ly      = Memory.getByteOnAddress(LY_ADDR)
        val stat    = Memory.getByteOnAddress(LCD_STAT)

        StatObj.COINCIDENCE_FLAG.set(stat, if (lyc == ly) 0x1 else 0x0)

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

    fun writeToOAM(address: Int, value: Byte){

        var arrayAddress = address and 0xFFFF

        if(address >= OAM_START){
            arrayAddress -= 0xFE00
        }

        oamRam[arrayAddress] = value
        Memory.write(address, value)
    }

    fun writeToLCD(address: Int, value: Byte){
        if(address == BGP){
            Memory.write(address, value)
        }
        if(address in OBP0 .. OBP1){
            Memory.write(address, ((value.toInt() and 0xFF) and 0b11111100).toByte())
        }
    }

    fun readFromVRAM(address: Int) : Byte{
        return vRam[address - VRAM_START]
    }

    fun writeToVRAM(address: Int, value: Byte){
        vRam[address - VRAM_START] = value
    }

    fun getPaletteColors(palette: PALETTE_TYPE) : IntArray{
        return palettes[palette]!!
    }

    fun getSelectedPalette(): IntArray{
        Memory.read()
    }
}