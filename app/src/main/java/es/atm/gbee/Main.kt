package es.atm.gbee

import es.atm.gbee.modules.BOOT_END
import es.atm.gbee.modules.CPU
import es.atm.gbee.modules.Memory
import es.atm.gbee.modules.ROM
import es.atm.gbee.modules.ROM_END
import es.atm.gbee.modules.ROM_START
import es.atm.gbee.modules.VRAM_END
import es.atm.gbee.modules.VRAM_START
import kotlin.system.exitProcess

fun main(args: Array<String>){

    /*if(args.isEmpty()){
        System.err.println("No file was selected / passed through input")
        return
    }*/

    Memory.dumpMemory(ROM_START, ROM_END) // Print Memory

    // BOOTSTRAP
    while(CPU.getBootstrapPending()){
        if(!CPU.step()){
            System.err.println("An error on the boot process has occurred. Program must exit.")
            exitProcess(0)
        }
    }

    Memory.dumpMemory(VRAM_START, VRAM_END) // Print Memory

    exitProcess(0)

    // LOAD GAME ROM
    if(!ROM.load_rom("D:/test_rom.gb"/*args[0]*/)){
        System.err.println("Failed to load ROM: ${args[0]} <rom>")
        return
    }

    while(true){
        if(!CPU.step()){
            break
        }
    }
}