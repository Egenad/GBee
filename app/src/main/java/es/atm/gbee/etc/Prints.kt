package es.atm.gbee.etc

import es.atm.gbee.modules.ROM
import es.atm.gbee.modules.ROM.getRomTypeFromIndex

fun printROM(){
    println("Cart Title: " + ROM.getCartTitle())
    println("License: " + ROM.getLicenseCode())
    println("Cart Type: " + getRomTypeFromIndex(ROM.getCartType()))
    println("ROM Size: " + ROM.getRomSize())
    println("ROM Version Number: " + ROM.getRomVersion())
    println("RAM Size: " + ROM.getRamSize())
    println("Console Type: " + ROM.getConsole())
}