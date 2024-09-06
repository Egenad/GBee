package es.atm.gbee

import es.atm.gbee.modules.CPU
import es.atm.gbee.modules.Memory
import es.atm.gbee.modules.ROM_END
import es.atm.gbee.modules.ROM_START

fun main(){

    Memory.dumpMemory(ROM_START, ROM_END)

    while(true){
        if(!CPU.step()){
            break
        }
    }
}