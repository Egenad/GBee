package es.atm.gbee.modules.mbcs

import es.atm.gbee.modules.ENABLE_RAM_END
import es.atm.gbee.modules.EXTERNAL_RAM_START
import es.atm.gbee.modules.Memory
import es.atm.gbee.modules.RAM_BANK_MODE_END
import es.atm.gbee.modules.RAM_BANK_NUMBER_END
import es.atm.gbee.modules.ROM
import es.atm.gbee.modules.ROM.BankingMode
import es.atm.gbee.modules.ROM.cartHasBattery
import es.atm.gbee.modules.ROM.cartHasRam
import es.atm.gbee.modules.ROM_BANK_NUMBER_END
import es.atm.gbee.modules.ROM_END
import es.atm.gbee.modules.ROM_START
import es.atm.gbee.modules.ROM_SW_START
import es.atm.gbee.modules.WRAM_START

class MBC1 : MBC(){

    private var saveNeeded = false
    override var ramBanks: Array<ByteArray>? = Array(4){ByteArray(8 * 1024)} // MBC1 = 4 RAM Banks Max, 8 KiB each
    override var romBanks: Array<ByteArray>?

    init {
       val romTotalBanks = ROM.getRomTotalBanks()

        when(romTotalBanks){
            2 -> romBankMask = 0b1
            4 -> romBankMask = 0xb11
            8 -> romBankMask = 0xb111
            16 -> romBankMask = 0xb1111
        }

        romBanks = Array(romTotalBanks){ ByteArray(16 * 1024) } // 16KiB each bank
    }

    override fun read(address: Int): Byte {
        when (address) {
            in ROM_START..< ROM_SW_START -> {                                              // READ FROM ROM FIXED BANK
                when(bankingMode){
                    BankingMode.MODE_0 -> return Memory.read(address)
                    BankingMode.MODE_1 -> {
                        val totalRomBanks = ROM.getRomTotalBanks()
                        var bank = currentRomBank

                        when (totalRomBanks) {
                            in 33..64 -> { // 2 bit register used as complementary
                                bank = ((currentRamBank and 0b1) shl 5) + currentRomBank
                            }
                            in 65 .. 128 -> {
                                bank = (currentRamBank shl 5) + currentRomBank
                            }
                        }
                        return romBanks!![bank][address]
                    }
                }
            }
            in ROM_SW_START..ROM_END -> {                                              // READ FROM ROM BANK SELECTION

            }
        }

        return Memory.read(address)
    }

    override fun write(address: Int, value: Byte) {
        val valueInt = value.toInt() and 0xFF

        when {
            address <= ENABLE_RAM_END -> {  // ENABLE RAM
                if(cartHasRam())
                    ramEnabled = valueInt and 0xF == 0xA
            }
            address in (ENABLE_RAM_END + 1)..ROM_BANK_NUMBER_END -> {                           // ROM BANK SELECTION
                var currentRomBank = valueInt and romBankMask
                if (currentRomBank == 0) currentRomBank++
            }
            address in (ROM_BANK_NUMBER_END + 1)..RAM_BANK_NUMBER_END -> {                      // RAM BANK SELECTION
                if (bankingMode == BankingMode.MODE_1 && saveNeeded) {
                    saveExRAMToFile()
                }
                currentRamBank = valueInt and 0b11
            }
            address in (RAM_BANK_NUMBER_END + 1)..RAM_BANK_MODE_END -> {                        // BANKING MODE
                bankingMode = if (valueInt and 0b1 != 0) BankingMode.MODE_1 else BankingMode.MODE_0

                if(bankingMode == BankingMode.MODE_1 && saveNeeded){
                    saveExRAMToFile()
                }
            }
            address in EXTERNAL_RAM_START..< WRAM_START -> {                                       // WRITE TO EXTERNAL RAM
                if(ramEnabled){
                    ramBanks!![currentRamBank][address - EXTERNAL_RAM_START] = value

                    if(cartHasBattery()) saveNeeded = true
                }
            }
        }
    }
}