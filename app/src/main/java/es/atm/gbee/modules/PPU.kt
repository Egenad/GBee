package es.atm.gbee.modules

const val TM_1_START : Int  = 0x9800 // TileMap 1 Start Address
const val TM_1_END : Int    = 0x9BFF // TileMap 1 End Address
const val TM_2_START : Int  = 0x9C00 // TileMap 2 Start Address
const val TM_2_END : Int    = 0x9FFF // TileMap 2 End Address
const val OAM_START : Int   = 0xFE00 // OAM Start Address
const val OAM_END : Int     = 0xFE9F // OAM End Address
const val LCD_STAT : Int    = 0xFF41 // LCD STATUS
const val LCDC : Int        = 0xFF40
const val LY : Int          = 0xFF44
const val LYC : Int         = 0xFF45

const val SCY : Int         = 0xFF42 // Background Viewport Y Position
const val SCX : Int         = 0xFF43 // Background Viewport X Position
const val WY : Int          = 0xFF4A // Window Y Position
const val WX : Int          = 0xFF4B // Window X Position

const val BGP : Int         = 0xFF47 // Background Palette - Non-CGB
const val OBP0 : Int        = 0xFF48 // Object Palette 0 - Non-CGB
const val OBP1 : Int        = 0xFF49 // Object Palette 1 - Non-CGB

object PPU {

    fun updatePPU(){


    }
}