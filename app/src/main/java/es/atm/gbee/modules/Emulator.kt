package es.atm.gbee.modules

import kotlinx.coroutines.delay
import kotlin.system.exitProcess

object Emulator {
    private var running : Boolean = true
    private var paused : Boolean = false
    private var ticks : Int = 0

    suspend fun run(args: Array<String>){

        if(args.isEmpty()){
            System.err.println("No file was selected / passed through input")
            return
        }

        Timer.init_timers()

        // BOOTSTRAP
        while(CPU.getBootstrapPending()){
            if(!CPU.tick()){
                System.err.println("An error on the boot process has occurred. Program must exit.")
                exitProcess(0)
            }
        }

        // LOAD GAME ROM
        if(!ROM.load_rom(args[0])){
            System.err.println("Failed to load ROM: ${args[0]} <rom>")
            return
        }

        //TODO: Init Graphics Framework

        while(running){
            if(paused){
                delay(10)
                continue
            }
            if(!CPU.tick()){
                System.err.println("CPU Error")
                break
            }
            ticks++
        }
    }
}