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

object PPU {

    fun tick(){


    }

    fun compare_LY_LYC(){
        val lyc = Memory.getByteOnAddress(LYC_ADDR)
        val ly = Memory.getByteOnAddress(LY_ADDR)
        val stat = Memory.getByteOnAddress(LCD_STAT)

        StatObj.COINCIDENCE_FLAG.set(stat, if (lyc == ly) 0x1 else 0x0)

        if(lyc == ly && StatObj.COINCIDENCE_INTERRUPT.get(stat) != 0){
            Interrupt.requestInterrupt(Interrupt.InterruptType.LCD_STAT.getByteMask())
        }
    }

    fun readFromVRAM(address: Int) : Byte{
        return 0 // TODO
    }

    fun writeToVRAM(address: Int, value: Byte){

    }

    fun readFromOAM(address: Int) : Byte{
        return 0 // TODO
    }

    fun writeToOAM(address: Int, value: Byte){

    }
}