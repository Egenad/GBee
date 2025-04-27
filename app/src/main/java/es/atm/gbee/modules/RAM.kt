package es.atm.gbee.modules

const val CGB_SW_BANKS = 7

object RAM {

    private var wramBanks: Array<ByteArray> = Array(CGB_SW_BANKS) { ByteArray(ECHO_RAM_START - SWITCHABLE_WRAM_START) } // Not used if not in CGB mode
    private var wramBank = 0

    fun readFromWRAM(address: Int) : Byte{
        return if(!ROM.isCGB() || address < SWITCHABLE_WRAM_START){
            Memory.read(address)
        }else{
            wramBanks[wramBank][address - SWITCHABLE_WRAM_START]
        }
    }

    fun writeToWRAM(address: Int, value: Byte){
        if(!ROM.isCGB() || address < SWITCHABLE_WRAM_START){
            Memory.write(address, value)
        }else{
            wramBanks[wramBank][address - SWITCHABLE_WRAM_START] = value
        }
    }

    fun readFromHRAM(address: Int) : Byte{
        return Memory.read(address)
    }

    fun writeToHRAM(address: Int, value: Byte){
        Memory.write(address, value)
    }

    fun reset(){
        for(i in 0 until CGB_SW_BANKS){
            wramBanks[i] = ByteArray(ECHO_RAM_START - SWITCHABLE_WRAM_START)
        }
        wramBank = 0
    }
}