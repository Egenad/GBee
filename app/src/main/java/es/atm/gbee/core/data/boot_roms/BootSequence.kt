package es.atm.gbee.core.data.boot_roms

enum class Phase {
    LOGO_SCROLL,
    LOGO_WAIT,
    FINISHED
}

interface BootSequence {
    var phase: Phase
    fun init()
    /** Returns true when the boot sequence has finished. */
    fun tick(): Boolean
}
