package es.atm.gbee.modules.mbcs

import es.atm.gbee.modules.ENABLE_RAM_END
import es.atm.gbee.modules.EXTERNAL_RAM_START
import es.atm.gbee.modules.Memory
import es.atm.gbee.modules.RAM_BANK_MODE_END
import es.atm.gbee.modules.RAM_BANK_NUMBER_END
import es.atm.gbee.modules.ROM.BankingMode
import es.atm.gbee.modules.ROM.cartHasBattery
import es.atm.gbee.modules.ROM.cartHasRam
import es.atm.gbee.modules.ROM_BANK_NUMBER_END
import es.atm.gbee.modules.WRAM_START

class MBC1 : MBC(){

    private var saveNeeded = false
    override var ramBanks: Array<ByteArray>? = Array(4){ByteArray(8 * 1024)} // MBC1 = 4 RAM Banks Max.

    override fun read(address: Int): Byte {
        return Memory.read(address)
    }

    override fun write(address: Int, value: Byte) {
        val valueInt = value.toInt() and 0xFF

        when {
            address <= ENABLE_RAM_END -> {  // ENABLE RAM
                if(cartHasRam())
                    ramEnabled = valueInt and 0xF == 0xA
            }
            address in (ENABLE_RAM_END + 1)..ROM_BANK_NUMBER_END -> {// ROM BANK SELECTION
                var romBank = valueInt and 0b11111

                if (bankingMode == BankingMode.MODE_0) { // MODE 0 -> Use RAM Bank 2 bit register as upper 5-6 bits of the ROM Bank
                    romBank += (currentRamBank shl 5)

                    if (romBank == 0 || romBank == 0x20 || romBank == 0x40 || romBank == 0x60) { // In MODE 0 isn't possible to access these banks
                        romBank++
                    }
                }

                currentRomBank = romBank
            }
            address in (ROM_BANK_NUMBER_END + 1)..RAM_BANK_NUMBER_END -> {  // RAM BANK SELECTION
                if (bankingMode == BankingMode.MODE_1 && saveNeeded) {
                    saveExRAMToFile()
                }
                currentRamBank = valueInt and 0b11
            }
            address in (RAM_BANK_NUMBER_END + 1)..RAM_BANK_MODE_END -> {  // BANKING MODE
                bankingMode = if (valueInt and 0b1 != 0) BankingMode.MODE_1 else BankingMode.MODE_0

                if(bankingMode == BankingMode.MODE_1 && saveNeeded){
                    saveExRAMToFile()
                }
            }
            address in EXTERNAL_RAM_START..< WRAM_START -> { // WRITE TO EXTERNAL RAM
                if(ramEnabled){
                    ramBanks!![currentRamBank][address - EXTERNAL_RAM_START] = value

                    if(cartHasBattery()) saveNeeded = true
                }
            }
        }
    }
}