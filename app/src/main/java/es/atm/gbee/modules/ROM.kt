package es.atm.gbee.modules

import es.atm.gbee.etc.extractByteArray
import es.atm.gbee.etc.memcmp
import java.io.File
import kotlin.math.min

object ROM {

    // --- CARTRIDGE HEADER ---
    // 0100-0103    — Entry point
    // 0104-0133    — Nintendo logo
    // 0134-0143    — Title
    // 013F-0142    — Manufacturer code
    // 0143         — CGB flag
    // 0144–0145    — New licensee code
    // 0146         — SGB flag
    // 0147         — Cartridge type
    // 0148         — ROM size
    // 0149         — RAM size
    // 014A         — Destination code
    // 014B         — Old licensee code
    // 014C         — Mask ROM version number
    // 014D         — Header checksum
    // 014E-014F    — Global checksum

    fun load_rom(fileName: String): Boolean{

        try {
            val romFile = File(fileName)
            val romBytes = romFile.readBytes()

            if(romBytes.isNotEmpty()){
                val romSize = min(romBytes.size, ROM_END - ROM_START + 1)

                for (i in 0 until romSize) {
                    Memory.writeByteOnAddress(ROM_START + 1, romBytes[i])
                }

                return rom_init(romBytes, romSize)
            }
        }catch (_: Exception){}

        return false
    }

    fun rom_init(romBytes: ByteArray, romSize: Int): Boolean{

        // Compare Cartridge Header with the Boot fixed one

        // Get header section from the romBytes
        val bootByteArray = Memory.getNintendoLogo()
        val cartByteArray = extractByteArray(romBytes, 0x104, 0x134) // Nintendo Logo on Cartridge goes from 0x104 to 0x133

        if(memcmp(Memory.getNintendoLogo(), cartByteArray, bootByteArray.size) != 0){
            return false
        }



        return true
    }
}