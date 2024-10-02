package es.atm.gbee.modules

import es.atm.gbee.modules.Memory.insertBootstrapToMemory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class Emulator {
    private var running : Boolean = true
    private var paused : Boolean = false
    private var lastCpuCycles : Int = 0

    fun run(bytes : ByteArray?){

        if(bytes == null || bytes.isEmpty()){
            System.err.println("No file was selected / passed through input")
            return
        }

        GlobalScope.launch {
            runCpu(bytes)
        }
    }

    suspend fun runCpu(bytes: ByteArray){

        PPU.init()

        // LOAD GAME ROM
        if(!ROM.load_rom(bytes)){
            System.err.println("Failed to load ROM. Program must exit.")
            return
        }

        insertBootstrapToMemory()
        println("Memory initialized - Ready to Boot")

        // BOOTSTRAP
        while(CPU.getBootstrapPending()){
            if(!CPU.tick()){
                System.err.println("An error on the boot process has occurred. Program must exit.")
                exitProcess(0)
            }
            updateEmuCycles()
        }

        println("ROM - Reload Boot Portion")
        ROM.reloadBootPortion()

        Memory.dumpMemory()

        while(running){
            if(paused){
                delay(10)
                continue
            }
            if(!CPU.tick()){
                System.err.println("CPU Error")
                break
            }

            delay(1000)

            updateEmuCycles()
        }
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
}