package es.atm.gbee.modules.mbcs

import android.util.Log
import es.atm.gbee.modules.ENABLE_RAM_END
import es.atm.gbee.modules.EXTERNAL_RAM_START
import es.atm.gbee.modules.Memory
import es.atm.gbee.modules.RAM_BANK_NUMBER_END
import es.atm.gbee.modules.ROM
import es.atm.gbee.modules.ROM.BankingMode
import es.atm.gbee.modules.ROM.cartHasBattery
import es.atm.gbee.modules.ROM.cartHasRam
import es.atm.gbee.modules.ROM_BANK_NUMBER_END
import es.atm.gbee.modules.ROM_BANK_NUMBER_END_MBC5
import es.atm.gbee.modules.ROM_END
import es.atm.gbee.modules.ROM_START
import es.atm.gbee.modules.ROM_SW_START
import es.atm.gbee.modules.WRAM_START

const val MBC5_RAM_BANK_SIZE : Int = 8 * 1024   // 4 RAM Banks Max, 8 KiB each
const val MBC5_ROM_BANK_SIZE : Int = 16 * 1024  // 16 KiB each ROM bank

class MBC5(romBytes: ByteArray) : MBC() {
    override var ramBanks: Array<ByteArray>? = Array(ROM.getRamTotalBanks()){ByteArray(MBC5_RAM_BANK_SIZE)}
    override var romData: ByteArray? = romBytes

    override fun read(address: Int): Byte {
        when (address) {
            in ROM_START..<ROM_SW_START -> { // READ FROM ROM FIXED BANK
                return romData?.get(address) ?: 0xFF.toByte()
            }
            in ROM_SW_START..ROM_END -> return romData?.get(getROMSwBankAddr(address)) ?: 0xFF.toByte() // READ FROM SWITCHABLE ROM BANK
            in EXTERNAL_RAM_START..< WRAM_START -> { // READ FROM EXTERNAL RAM
                return if(ramEnabled) {
                    ramBanks
                        ?.getOrNull(currentRamBank)
                        ?.get(address - EXTERNAL_RAM_START)
                        ?: 0xFF.toByte()
                } else 0xFF.toByte()
            }
        }
        return 0xFF.toByte()
    }

    override fun write(address: Int, value: Byte) {
        val valueInt = value.toInt() and 0xFF

        Log.d("MBC5",
            "MBC5 write addr=${address.toString(16)} " +
                    "value=${valueInt.toString(16)} " +
                    "romBank=$currentRomBank ramBank=$currentRamBank"
        )

        when {
            address <= ENABLE_RAM_END -> {  // ENABLE RAM
                if (cartHasRam())
                    ramEnabled = valueInt and 0xF == 0xA
            }
            address in (ENABLE_RAM_END + 1)..ROM_BANK_NUMBER_END_MBC5 -> { // ROM LEAST 8 BIT BANK SELECTION
                // Replace Bits 0-7 but dont modify bit 8
                currentRomBank = (currentRomBank and 0x100) or valueInt
            }
            address in (ROM_BANK_NUMBER_END_MBC5 + 1)..ROM_BANK_NUMBER_END -> { // LAST 9 BIT ROM BANK SELECTION
                // Replace Bit 8 with 0-7 not modified
                currentRomBank = (currentRomBank and 0xFF) or ((valueInt and 0x01) shl 8)
            }
            address in (ROM_BANK_NUMBER_END + 1)..RAM_BANK_NUMBER_END -> { // RAM BANK SELECTION
                if (saveNeeded) {
                    saveExRAMToFile()
                }
                currentRamBank = valueInt and 0x0F
            }
            address in EXTERNAL_RAM_START..< WRAM_START -> {                                       // WRITE TO EXTERNAL RAM
                if(ramEnabled){
                    ramBanks!![currentRamBank][address - EXTERNAL_RAM_START] = value
                    if(cartHasBattery()) saveNeeded = true
                }
            }
        }
    }

    private fun getROMSwBankAddr(address: Int): Int{
        return currentRomBank * MBC5_ROM_BANK_SIZE + (address - ROM_SW_START)
    }
}