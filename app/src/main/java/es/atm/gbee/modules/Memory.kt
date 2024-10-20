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

    private val nintendoLogo: ByteArray = byteArrayOf(
        0xCE.toByte(), 0xED.toByte(), 0x66.toByte(), 0x66.toByte(), 0xCC.toByte(), 0x0D.toByte(), 0x00.toByte(), 0x0B.toByte(),
        0x03.toByte(), 0x73.toByte(), 0x00.toByte(), 0x83.toByte(), 0x00.toByte(), 0x0C.toByte(), 0x00.toByte(), 0x0D.toByte(),
        0x00.toByte(), 0x08.toByte(), 0x11.toByte(), 0x1F.toByte(), 0x88.toByte(), 0x89.toByte(), 0x00.toByte(), 0x0E.toByte(),
        0xDC.toByte(), 0xCC.toByte(), 0x6E.toByte(), 0xE6.toByte(), 0xDD.toByte(), 0xDD.toByte(), 0xD9.toByte(), 0x99.toByte(),
        0xBB.toByte(), 0xBB.toByte(), 0x67.toByte(), 0x63.toByte(), 0x6E.toByte(), 0x0E.toByte(), 0xEC.toByte(), 0xCC.toByte(),
        0xDD.toByte(), 0xDC.toByte(), 0x99.toByte(), 0x9F.toByte(), 0xBB.toByte(), 0xB9.toByte(), 0x33.toByte(), 0x3E.toByte()
    )

    private val bootstrapRom = byteArrayOf(
        0x31.toByte(), 0xFE.toByte(), 0xFF.toByte(),    // LD SP, 0xFFFE                ----- Initialize SP -----
        0xAF.toByte(),                                  // XOR A
        0x21.toByte(), 0xFF.toByte(), 0x9F.toByte(),    // LD HL, 0x9FFF
        0x32.toByte(),                                  // LD (HL-), A                  ----- LOOP START - Clean VRAM -----
        0xCB.toByte(), 0x7C.toByte(),                   // BIT 7, H
        0x20.toByte(), 0xFB.toByte(),                   // JR NZ, PC + 0xFB             ----- LOOP END -----
        0x21.toByte(), 0x26.toByte(), 0xFF.toByte(),    // LD HL, 0xFF26                ----- Sound Settings -----
        0x0E.toByte(), 0x11.toByte(),                   // LD C, 0x11
        0x3E.toByte(), 0x80.toByte(),                   // LD A, 0x80
        0x32.toByte(),                                  // LD [HL-], A
        0xE2.toByte(),                                  // LD (0xFF00+C), A
        0x0C.toByte(),                                  // INC C
        0x3E.toByte(), 0xF3.toByte(),                   // LD A, 0xF3
        0xE2.toByte(),                                  // LD (0xFF00+C), A
        0x32.toByte(),                                  // LD (HL-), A
        0x3E.toByte(), 0x77.toByte(),                   // LD A, 0x77
        0x77.toByte(),                                  // LD [HL], A                   ----- Sound Settings End -----
        0x3E.toByte(), 0xFC.toByte(),                   // LD A, 0xFC                   ----- A = Color Number's Mappings -----
        0xE0.toByte(), 0x47.toByte(),                   // LDH [0xFF00 + 0x47], A       ----- Initialize Palette -----
        0x11.toByte(), 0x04.toByte(), 0x01.toByte(),    // LD DE, 0x0104                ----- 0x0104 = Nintendo Logo Ptr Address Start -----
        0x21.toByte(), 0x10.toByte(), 0x80.toByte(),    // LD HL, 0x8010                ----- 0x8010 = VRAM Ptr Address Start -----
        0x1A.toByte(),                                  // LD A, [DE]                   ----- Load Byte from Nintendo Logo -----
        0xCD.toByte(), 0x95.toByte(), 0x00.toByte(),    // CALL 0x0095                  ----- Decompress, scale and write pixels to VRAM (1) -----
        0xCD.toByte(), 0x96.toByte(), 0x00.toByte(),    // CALL 0x0096                  ----- Decompress, scale and write pixels to VRAM (2) -----
        0x13.toByte(),                                  // INC DE
        0x7B.toByte(),                                  // LD A, E
        0xFE.toByte(), 0x34.toByte(),                   // CP 0x34
        0x20.toByte(), 0xF3.toByte(),                   // JR NZ, PC + 0xF3             ----- LOOP END IF COMPARATION HAS ENDED -----
        0x11.toByte(), 0xD8.toByte(), 0x00.toByte(),    // LD DE, 0x00D8                ----- Load 8 additional bytes into Video RAM (the tile for ®) -----
        0x06.toByte(), 0x08.toByte(),                   // LD B, 0x08
        0x1A.toByte(),                                  // LD A, [DE]
        0x13.toByte(),                                  // INC DE
        0x22.toByte(),                                  // LD [HL+], A
        0x23.toByte(),                                  // INC HL
        0x05.toByte(),                                  // DEC B
        0x20.toByte(), 0xF9.toByte(),                   // JR NZ, PC + 0xF9
        0x3E.toByte(), 0x19.toByte(),                   // LD A, 0x19                   ----- Setup background tilemap -----
        0xEA.toByte(), 0x10.toByte(), 0x99.toByte(),    // LD [0x9910], A
        0x21.toByte(), 0x2F.toByte(), 0x99.toByte(),    // LD HL, 0x992F
        0x0E.toByte(), 0x0C.toByte(),                   // LD C, 0x0C
        0x3D.toByte(),                                  // DEC A
        0x28.toByte(), 0x08.toByte(),                   // JR Z, PC + 0x08
        0x32.toByte(),                                  // LD (HL-), A
        0x0D.toByte(),                                  // DEC C
        0x20.toByte(), 0xF9.toByte(),                   // JR NZ, PC + 0xF9
        0x2E.toByte(), 0x0F.toByte(),                   // LD L, 0x0F
        0x18.toByte(), 0xF3.toByte(),                   // JR PC + 0xF3
        0x67.toByte(),                                  // LD H, A                      ----- Initialize scroll count, H=0 ------
        0x3E.toByte(), 0x64.toByte(),                   // LD A, 0x64
        0x57.toByte(),                                  // LD D, A
        0xE0.toByte(), 0x42.toByte(),                   // LDH [0xFF00 + 0x42], A       ----- Set vertical scroll register -----
        0x3E.toByte(), 0x91.toByte(),                   // LD A, 0x91
        0xE0.toByte(), 0x40.toByte(),                   // LDH [0xFF00 + 0x40], A       ----- Turn on LCD, showing Background -----
        0x04.toByte(),                                  // INC B
        0x1E.toByte(), 0x02.toByte(),                   // LD E, 0x02
        0x0E.toByte(), 0x0C.toByte(),                   // LD C, 0x0C
        0xF0.toByte(), 0x44.toByte(),                   // LDH A, [0xFF00 + 0x44]       ----- Wait for V-Blank period -----
        0xFE.toByte(), 0x90.toByte(),                   // CP 0x90
        0x20.toByte(), 0xFA.toByte(),                   // JR NZ, PC + 0xFA
        0x0D.toByte(),                                  // DEC C
        0x20.toByte(), 0xF7.toByte(),                   // JR NZ, PC + 0xF7
        0x1D.toByte(),                                  // DEC E
        0x20.toByte(), 0xF2.toByte(),                   // JR NZ, PC + 0xF2
        0x0E.toByte(), 0x13.toByte(),                   // LD C, 0x13
        0x24.toByte(),                                  // INC H
        0x7C.toByte(),                                  // LD A, H
        0x1E.toByte(), 0x83.toByte(),                   // LD E, 0x83
        0xFE.toByte(), 0x62.toByte(),                   // CP 0x62
        0x28.toByte(), 0x06.toByte(),                   // JR Z, PC + 0x06
        0x1E.toByte(), 0xC1.toByte(),                   // LD E, 0xC1
        0xFE.toByte(), 0x64.toByte(),                   // CP 0x64
        0x20.toByte(), 0x06.toByte(),                   // JR NZ, PC + 0x06
        0x7B.toByte(),                                  // LD A, E
        0xE2.toByte(),                                  // LD [0xFF00+C], A
        0x0C.toByte(),                                  // INC C
        0x3E.toByte(), 0x87.toByte(),                   // LD A, 0x87
        0xE2.toByte(),                                  // LD [0xFF00+C], A
        0xF0.toByte(), 0x42.toByte(),                   // LDH A, [0xFF00 + 0x42]
        0x90.toByte(),                                  // SUB B
        0xE0.toByte(), 0x42.toByte(),                   // LDH [0xFF00 + 0x42], A
        0x15.toByte(),                                  // DEC D
        0x20.toByte(), 0xD2.toByte(),                   // JR NZ, PC + 0xD2
        0x05.toByte(),                                  // DEC B
        0x20.toByte(), 0x4F.toByte(),                   // JR NZ, PC + 0x4F
        0x16.toByte(), 0x20.toByte(),                   // LD D, 0x20
        0x18.toByte(), 0xCB.toByte(),                   // JR PC + 0xCB
        0x4F.toByte(),                                  // LD C, A
        0x06.toByte(), 0x04.toByte(),                   // LD B, 0x04
        0xC5.toByte(),                                  // PUSH BC
        0xCB.toByte(), 0x11.toByte(),                   // RL C
        0x17.toByte(),                                  // RLA
        0xC1.toByte(),                                  // POP BC
        0xCB.toByte(), 0x11.toByte(),                   // RL C
        0x17.toByte(),                                  // RLA
        0x05.toByte(),                                  // DEC B
        0x20.toByte(), 0xF5.toByte(),                   // JR NZ, PC + 0xF5
        0x22.toByte(),                                  // LD [HL+], A
        0x23.toByte(),                                  // INC HL
        0x22.toByte(),                                  // LD [HL+], A
        0x23.toByte(),                                  // INC HL
        0xC9.toByte(),                                  // RET
        *nintendoLogo,                                                                                                                  // ----- NINTENDO LOGO END -----
        0x3C.toByte(), 0x42.toByte(), 0xB9.toByte(), 0xA5.toByte(), 0xB9.toByte(), 0xA5.toByte(), 0x42.toByte(), 0x3C.toByte(),         // ----- More video data (the tile data for ®) -----
        0x21.toByte(), 0x04.toByte(), 0x01.toByte(),                                                                                    // LD HL, 0x0104 -- Point HL to Nintendo logo in cart
        0x11.toByte(), 0xA8.toByte(), 0x00.toByte(),                                                                                    // LD DE, 0x00A8 -- Point DE to Nintendo logo in DMG rom
        0x1A.toByte(),                                  // LD A, [DE]
        0x13.toByte(),                                  // INC DE
        0xBE.toByte(),                                  // CP [HL]                      ----- Compare logo data in cart to DMG rom -----
        0x20.toByte(), 0xFE.toByte(),                   // JR NZ, PC + 0xFE             ----- If not a match, lock up here -----
        0x23.toByte(),                                  // INC HL
        0x7D.toByte(),                                  // LD A, L
        0xFE.toByte(), 0x34.toByte(),                   // CP 0x34
        0x20.toByte(), 0xF5.toByte(),                   // JR NZ, PC + 0xF5
        0x06.toByte(), 0x19.toByte(),                   // LD B, 0x19
        0x78.toByte(),                                  // LD A, B
        0x86.toByte(),                                  // ADD A, [HL]
        0x23.toByte(),                                  // INC HL
        0x05.toByte(),                                  // DEC B
        0x20.toByte(), 0xFB.toByte(),                   // JR NZ, PC + 0xFB
        0x86.toByte(),                                  // ADD A, [HL]
        0x20.toByte(), 0xFE.toByte(),                   // JR NZ, 0xFE
        0x3E.toByte(), 0x01.toByte(),                   // LD A, 0x01
        0xE0.toByte(), 0x50.toByte())                   // LDH [0xFF00 + 0x50], A       ----- Turn Off DMG ROM -----

    // Total Memory
    private val memory = ByteArray(MEMORY_SIZE)

    fun writeByteOnAddress(address: Int, value: Byte){
        if(address < VRAM_START) {                  // ROM DATA
            ROM.writeToROM(address, value)
        }else if(address < EXTERNAL_RAM_START) {    // VRAM DATA
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

    fun insertBootstrapToMemory(){
        for (i in bootstrapRom.indices) {
            memory[ROM_START + i] = bootstrapRom[i]
        }
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
        return memory[address]
    }

    fun write(address: Int, value: Byte){
        memory[address] = value
    }

    fun getNintendoLogo() : ByteArray{
        return nintendoLogo
    }

    // TESTING PURPOSES !!
    fun getMemory(): ByteArray{
        return memory
    }

    fun switchROMBank(){

    }
}