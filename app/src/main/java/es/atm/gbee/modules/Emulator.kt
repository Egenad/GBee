package es.atm.gbee.modules

import es.atm.gbee.core.data.boot_roms.BootController
import es.atm.gbee.core.data.boot_roms.DMGBoot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

object Emulator {
    private var running : Boolean = false
    private var paused : Boolean = false
    private var cpuScope: CoroutineScope? = null

    val audioSys: Audio = Audio()

    fun run(bytes : ByteArray?){
        if(bytes == null || bytes.isEmpty()){
            println("Error: No bytes passed")
            System.err.println("No file was selected / passed through input")
            return
        }

        cpuScope = CoroutineScope(Dispatchers.Default)
        cpuScope?.launch {
            runCpu(bytes)
        }
    }

    private suspend fun runCpu(bytes: ByteArray){
        running = true

        CPU.init()

        // LOAD GAME ROM
        if(!ROM.loadRom(bytes)){
            System.err.println("Failed to load ROM. Program must exit.")
            running = false
            return
        }

        // PPU initialization depends on the console type read from the ROM header.
        PPU.init()
        BootController.start(DMGBoot)

        while (running) {
            if (paused) {
                delay(10.milliseconds)
                continue
            }

            if (BootController.isRunning) {
                BootController.tick()
            } else {
                if (!CPU.tick()) {
                    System.err.println("CPU Error")
                    break
                }
            }
        }
    }

    fun pause(){
        println("Emulator - Paused")
        paused = true
    }

    fun resume(){
        println("Emulator - Resumed")
        paused = false
    }

    fun stop(){
        println("Emulator - Stopped")
        running = false
        cpuScope?.cancel()
        resetModules()
    }

    fun isRunning(): Boolean{
        return running
    }

    fun isPaused(): Boolean{
        return paused
    }

    private fun resetModules(){
        BootController.reset()
        CPU.reset()
        PPU.reset()
        Memory.reset()
        RAM.reset()
        Timer.reset()
        DMA.reset()
        Interrupt.reset()
    }
}
