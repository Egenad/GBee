package es.atm.gbee.modules

object Timer {

    private var lastCycles : Int = 0

    fun updateTimers(){

        val actualCycles = CPU.getCPUCycles()
        val delta = actualCycles - lastCycles   // Real cycles since last timers update
        lastCycles = actualCycles

        tick(delta)
    }

    fun tick(delta: Int){

    }

}