package es.atm.gbee.etc

import es.atm.gbee.modules.ROM
import es.atm.gbee.modules.ROM.getRomTypeFromIndex

fun printROM(){
    println("Cart Title: " + ROM.getCartTitle())
    println("License: " + ROM.getLicenseCode())
    println("Cart Type: " + getRomTypeFromIndex(ROM.getCartType()))
    println("ROM Total Banks: " + ROM.getRomTotalBanks())
    println("ROM Version Number: " + ROM.getRomVersion())
    println("RAM Total Banks: " + ROM.getRamTotalBanks())
    println("Console Type: " + ROM.getConsole())
}