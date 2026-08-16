package es.atm.gbee.core.data.boot_roms

import es.atm.gbee.modules.EmulationClock

/** Runs a high-level boot sequence while the CPU is stopped. */
object BootController {
    private var activeBoot: BootSequence? = null

    val isRunning: Boolean
        get() = activeBoot != null

    fun start(bootSequence: BootSequence) {
        activeBoot = bootSequence
        bootSequence.init()
    }

    fun tick() {
        val bootSequence = activeBoot ?: return

        // The CPU is stopped during boot, so the controller advances the
        // rest of the hardware by one machine cycle.
        EmulationClock.advanceMachineCycle()

        if (bootSequence.tick()) {
            activeBoot = null
        }
    }

    fun reset() {
        activeBoot = null
    }
}
