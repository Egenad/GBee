package es.atm.gbee.modules

fun interface MachineCycleClock {
    fun advanceMachineCycle()
}

object EmulationClock : MachineCycleClock {
    override fun advanceMachineCycle() {
        repeat(4) {
            Timer.tick()
        }
        val ppuTicks = if (CGBSpeed.isDoubleSpeed()) 2 else 4
        repeat(ppuTicks) {
            PPU.tick()
        }

        DMA.tick()
    }
}