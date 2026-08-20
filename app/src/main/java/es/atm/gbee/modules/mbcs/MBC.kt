package es.atm.gbee.modules.mbcs

import android.util.Log
import es.atm.gbee.modules.ROM
import es.atm.gbee.modules.ROM.BankingMode
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

abstract class MBC : MBCInterface {

    abstract var ramBanks : Array<ByteArray>?

    abstract var romData : ByteArray?

    var currentRamBank : Int = 0
    var currentRomBank : Int = 1

    var romBankMask : Int = 0b11111

    var bankingMode: BankingMode = BankingMode.MODE_0

    var ramEnabled: Boolean = false
    var saveNeeded: Boolean = false

    fun saveExRAMToFile(){
        if (!saveNeeded || !ROM.cartHasBattery()) return

        try {
            FileOutputStream("${ROM.getCartTitle()}.sav").use { output ->
                ramBanks?.forEach { bank ->
                    output.write(bank)
                }
            }
        } catch (e: IOException) {
            Log.e("MBC", "Failed to save External RAM to file")
        }

        saveNeeded = false
    }

    fun loadExRAMFromFile(){

    }
}