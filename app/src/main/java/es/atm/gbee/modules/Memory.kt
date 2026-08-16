package es.atm.gbee.modules

/*  MEMORY MAP --------------
    Start   End
    0000	3FFF	16 KiB ROM bank 00 -- From cartridge, usually a fixed bank
    4000	7FFF	16 KiB ROM Bank 01–NN -- From cartridge, switchable bank via mapper (if any)
    8000	9FFF	8 KiB Video RAM (VRAM) -- In CGB mode, switchable bank 0/1
    A000	BFFF	8 KiB External RAM -- From cartridge, switchable bank if any
    C000	CFFF	4 KiB Work RAM (WRAM)
    D000	DFFF	4 KiB Work RAM (WRAM) -- In CGB mode, switchable bank 1–7
    E000	FDFF	Echo RAM (mirror of C000–DDFF) -- Nintendo says use of this area is prohibited.
    FE00	FE9F	Object attribute memory (OAM)
    FEA0	FEFF	Not Usable -- Nintendo says use of this area is prohibited.
    FF00	FF7F	I/O Registers
    FF80	FFFE	High RAM (HRAM)
    FFFF	FFFF	Interrupt Enable register (IE)
*/

const val ROM_START             = 0x0000
const val ROM_SW_START          = 0x4000
const val ROM_END               = 0x7FFF
const val BOOT_END              = 0x00FF
const val VRAM_START            = 0x8000
const val VRAM_END              = 0x9FFF
const val EXTERNAL_RAM_START    = 0xA000
const val WRAM_START            = 0xC000
const val FIXED_WRAM_END        = 0xCFFF
const val SWITCHABLE_WRAM_START = 0xD000
const val ECHO_RAM_START        = 0xE000
const val OAM_START             = 0xFE00
const val RESERVED_MEM_START    = 0xFEA0
const val IO_START              = 0xFF00
const val HRAM_START            = 0xFF80
const val HRAM_END              = 0xFFFE

const val MEMORY_SIZE   = 0x10000   // 64 KB
const val BOOT_SIZE     = 0xFF

object Memory {
    // Total Memory
    private val memory = ByteArray(MEMORY_SIZE)

    fun writeByteOnAddress(address: Int, value: Byte){
        if(address < VRAM_START) {                  // ROM DATA - 0x8000
            ROM.writeToROM(address, value)
        }else if(address < EXTERNAL_RAM_START) {    // VRAM DATA -
            PPU.writeToVRAM(address, value)
        }else if(address < WRAM_START) {            // EXTERNAL / CARTRIDGE RAM DATA
            ROM.writeToROM(address, value)
        }else if(address < ECHO_RAM_START){         // WRAM
            RAM.writeToWRAM(address, value)
        }else if(address < OAM_START) {             // ECHO RAM -- CANT BE USED !
            return
        }else if(address < RESERVED_MEM_START) {    // OAM DATA
            if(!DMA.transferring())
                PPU.writeToOAM(address, -1, value)
        }else if(address < IO_START) {              // RESERVED MEMORY - CANT BE USED !
            return
        }else if(address < HRAM_START) {            // IO DATA
            IO.writeToIO(address, value)
        }else if(address < IE){                     // HRAM DATA
            RAM.writeToHRAM(address, value)
        }else if(address == IE){                    // IE FLAG DATA
            write(address, value)
        }
    }

    fun getByteOnAddress(address: Int): Byte{
        if(address < VRAM_START) {                  // ROM DATA
            return ROM.readFromROM(address)
        }else if(address < EXTERNAL_RAM_START) {    // VRAM DATA
            return PPU.readFromVRAM(address)
        }else if(address < WRAM_START) {            // EXTERNAL / CARTRIDGE RAM DATA
            return ROM.readFromROM(address)
        }else if(address < ECHO_RAM_START){         // WRAM
            return RAM.readFromWRAM(address)
        }else if(address < OAM_START) {             // ECHO RAM -- CANT BE USED !
            return 0
        }else if(address < RESERVED_MEM_START) {    // OAM DATA
            if(DMA.transferring()) return 0xFF.toByte()
            return PPU.readFromOAM(address)
        }else if(address < IO_START) {              // RESERVED MEMORY - CANT BE USED !
            return 0
        }else if(address < HRAM_START) {            // IO DATA
            return IO.readFromIO(address)
        }else if(address < IE){                     // HRAM DATA
            return RAM.readFromHRAM(address)
        }else if(address == IE){                    // IE FLAG DATA
            return read(IE)
        }

        throw IllegalArgumentException("Not implemented $address") // TODO
    }

    // TESTING PURPOSES !!
    fun dumpMemory(startAddress: Int = 0x0000, endAddress: Int = 0xFFFF) {
        val bytesPerLine = 16
        var address = startAddress

        while (address <= endAddress) {

            print(String.format("%04X: ", address))

            for (i in 0 until bytesPerLine) {
                if (address + i <= endAddress) {
                    print(String.format("%02X ", memory[address + i].toInt() and 0xFF))
                } else {
                    print("   ")
                }
            }

            print(" | ")

            for (i in 0 until bytesPerLine) {
                if (address + i <= endAddress) {
                    val byteValue = memory[address + i].toInt() and 0xFF

                    if (byteValue in 32..126) {
                        print(byteValue.toChar())
                    } else {
                        print(".")
                    }
                }
            }

            println()
            address += bytesPerLine
        }
    }

    fun read(address: Int): Byte{
        return if(address in 0..< MEMORY_SIZE) memory[address] else 0xFF.toByte()
    }

    fun write(address: Int, value: Byte){
        if(address in 0..< MEMORY_SIZE) memory[address] = value
    }

    fun cleanVRAM() {
        for (address in VRAM_START..VRAM_END) {
            writeByteOnAddress(address, 0)
        }
    }

    fun reset() {
        memory.fill(0)
    }
}
