package es.atm.gbee.etc

import es.atm.gbee.modules.LCDC_ADDR
import es.atm.gbee.modules.Memory
import es.atm.gbee.modules.ROM
import es.atm.gbee.modules.ROM.getRomTypeFromIndex
import es.atm.gbee.modules.SCX
import es.atm.gbee.modules.SCY

fun printROM(){
    println("Cart Title: " + ROM.getCartTitle())
    println("License: " + ROM.getLicenseCode())
    println("Cart Type: " + getRomTypeFromIndex(ROM.getCartType()))
    println("ROM Total Banks: " + ROM.getRomTotalBanks())
    println("ROM Version Number: " + ROM.getRomVersion())
    println("RAM Total Banks: " + ROM.getRamTotalBanks())
    println("Console Type: " + ROM.getConsole())
}

fun printVRAM(){
    println("SCX: ${Memory.read(SCX).toInt() and 0xFF}")
    println("SCY: ${Memory.read(SCY).toInt() and 0xFF}")
    println("LCDC: ${Memory.read(LCDC_ADDR).toInt() and 0xFF})")
}