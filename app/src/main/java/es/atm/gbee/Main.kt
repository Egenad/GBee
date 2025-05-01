package es.atm.gbee

import es.atm.gbee.modules.CPU
import es.atm.gbee.modules.DMA
import es.atm.gbee.modules.Memory
import es.atm.gbee.modules.Memory.insertBootstrapToMemory
import es.atm.gbee.modules.PPU
import es.atm.gbee.modules.ROM
import es.atm.gbee.modules.Timer
import kotlin.system.exitProcess

private var lastCpuCycles : Int = 0

fun main(args: Array<String>){

    PPU.init()
    CPU.init()

    //ROM.load_rom_from_path("/home/angel/Documentos/Git/GBee/roms/GoldenSacra.gb")
    ROM.load_rom_from_path("/home/angel/Documentos/Git/GBee/roms/tetris.gb")

    insertBootstrapToMemory()
    Memory.dumpMemory(0x0000, 0x0500)

    while(CPU.getBootstrapPending()){
        if(!CPU.tick()){
            System.err.println("An error on the boot process has occurred. Program must exit.")
            exitProcess(0)
        }
        //updateEmuCycles()
    }

    println("ROM - Reload Boot Portion")
    ROM.reloadBootPortion()
}

private fun updateEmuCycles(){
    val currentCpuCycles = CPU.getCPUCycles()
    val cpuCycles = currentCpuCycles - lastCpuCycles
    lastCpuCycles = currentCpuCycles

    for (i in 0 until cpuCycles / 4) {
        for (n in 0 until 4) {
            Timer.tick()
            PPU.tick()
        }
        DMA.tick()
    }
}