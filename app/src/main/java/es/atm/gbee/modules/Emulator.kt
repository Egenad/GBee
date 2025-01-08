package es.atm.gbee.modules

import es.atm.gbee.etc.printVRAM
import es.atm.gbee.modules.Memory.insertBootstrapToMemory
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

object Emulator {

    private var running : Boolean = false
    private var paused : Boolean = false
    private var lastCpuCycles : Int = 0

    private val audioSys: Audio = Audio()

    @OptIn(DelicateCoroutinesApi::class)
    fun run(bytes : ByteArray?){

        if(bytes == null || bytes.isEmpty()){
            System.err.println("No file was selected / passed through input")
            return
        }

        GlobalScope.launch {
            runCpu(bytes)
        }
    }

    private suspend fun runCpu(bytes: ByteArray){

        running = true

        PPU.init()

        // LOAD GAME ROM
        if(!ROM.loadRom(bytes)){
            System.err.println("Failed to load ROM. Program must exit.")
            return
        }

        Memory.dumpMemory(0x0134, 0x014D)

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

    fun pause(){
        paused = true
    }

    fun resume(){
        paused = false
    }

    fun stop(){
        running = false
    }

    fun isRunning(): Boolean{
        return running
    }

    fun isPaused(): Boolean{
        return paused
    }
}