package es.atm.gbee.modules

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class Emulator {
    private var running : Boolean = true
    private var paused : Boolean = false
    private var lastCpuCycles : Int = 0

    fun run(path: String){

        if(path.isEmpty()){
            System.err.println("No file was selected / passed through input")
            return
        }

        PPU.init()

        // BOOTSTRAP
        while(CPU.getBootstrapPending()){
            if(!CPU.tick()){
                System.err.println("An error on the boot process has occurred. Program must exit.")
                exitProcess(0)
            }
            updateEmuCycles()
        }

        // LOAD GAME ROM
        if(!ROM.load_rom(path)){
            System.err.println("Failed to load ROM: ${path} <rom>")
            return
        }

        //TODO: Init Graphics Framework

        GlobalScope.launch {
            runCpu()
        }
    }

    suspend fun runCpu(){
        while(running){
            if(paused){
                delay(10)
                continue
            }
            if(!CPU.tick()){
                System.err.println("CPU Error")
                break
            }
            updateEmuCycles()
        }
    }

    fun updateEmuCycles(){
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