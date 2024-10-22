package es.atm.gbee.modules.mbcs

import es.atm.gbee.modules.ROM
import es.atm.gbee.modules.ROM.BankingMode
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

abstract class MBC : MBCInterface {

    abstract var ramBanks : Array<ByteArray>?

    var currentRamBank : Int = 0
    var currentRomBank : Int = 1

    var bankingMode: BankingMode = BankingMode.MODE_0

    var ramEnabled: Boolean         = false

    fun saveExRAMToFile(){
        if(currentRamBank >= 0){
            val batteryFilename = "${ROM.getCartTitle()}.sav"
            try {
                val file = File(batteryFilename)
                FileOutputStream(file).use { fos ->
                    fos.write(ramBanks!![currentRamBank])
                }
            } catch (e: IOException) {
                System.err.println("FAILED TO OPEN OR WRITE: $batteryFilename")
            }
        }
    }

    fun loadExRAMFromFile(){

    }
}