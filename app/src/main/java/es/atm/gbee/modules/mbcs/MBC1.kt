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

const val MBC1_RAM_BANK_SIZE : Int = 8 * 1024   // 4 RAM Banks Max, 8 KiB each
const val MBC1_ROM_BANK_SIZE : Int = 16 * 1024  // 16 KiB each ROM bank

class MBC1(romBytes: ByteArray) : MBC() {

    private var saveNeeded = false
    override var ramBanks: Array<ByteArray>? = Array(4){ByteArray(MBC1_RAM_BANK_SIZE)}
    override var romData: ByteArray?

    init {
       val romTotalBanks = ROM.getRomTotalBanks()

        when(romTotalBanks){
            2 -> romBankMask    = 0b1
            4 -> romBankMask    = 0xb11
            8 -> romBankMask    = 0xb111
            16 -> romBankMask   = 0xb1111
        }

        romData = romBytes
    }

    override fun read(address: Int): Byte {
        when (address) {
            in ROM_START..< ROM_SW_START -> { // READ FROM ROM FIXED BANK
                return when(bankingMode){
                    BankingMode.MODE_0 -> Memory.read(address)
                    BankingMode.MODE_1 -> romData?.get(getROM0BankAddr(address)) ?: 0xFF.toByte()
                }
            }
            in ROM_SW_START..ROM_END -> return romData?.get(getROMSwBankAddr(address)) ?: 0xFF.toByte() // READ FROM SWITCHABLE ROM BANK
            in EXTERNAL_RAM_START..< WRAM_START -> { // READ FROM EXTERNAL RAM
                return if(ramEnabled) {
                    ramBanks!![currentRamBank][address - EXTERNAL_RAM_START]
                } else 0xFF.toByte()
            }
        }

        return 0xFF.toByte()
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
                if (currentRomBank == 0) currentRomBank += 1
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

    private fun getROM0BankAddr(address: Int): Int {
        val totalRomBanks = ROM.getRomTotalBanks()
        var addressToReturn = address

        when (totalRomBanks) {
            in 33..64 -> { // 2 bit register used as complementary
                addressToReturn = ((currentRamBank and 0b1) shl 5) * address
            }
            in 65 .. 128 -> {
                addressToReturn = (currentRamBank shl 5) * address
            }
        }

        return addressToReturn
    }

    private fun getROMSwBankAddr(address: Int): Int{
        val totalRomBanks = ROM.getRomTotalBanks()
        var addressToReturn = (currentRomBank * MBC1_ROM_BANK_SIZE) + (address - ROM_SW_START)

        when (totalRomBanks) {
            in 33..64 -> { // 2 bit register used as complementary
                addressToReturn = ((((currentRamBank and 0b1) shl 5) + currentRomBank) * MBC1_ROM_BANK_SIZE) + (address - ROM_SW_START)
            }
            in 65 .. 128 -> {
                addressToReturn = (((currentRamBank shl 5) + currentRomBank) * MBC1_ROM_BANK_SIZE) * (address - ROM_SW_START)
            }
        }

        return addressToReturn
    }
}