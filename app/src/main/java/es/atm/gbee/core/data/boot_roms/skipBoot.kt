package es.atm.gbee.core.data.boot_roms

object SkipBoot : BootSequence {
    override var phase: Phase
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun init() {
        initializePostBootRegisters()
    }

    override fun tick(): Boolean = true

    private fun initializePostBootRegisters(){
        TODO("Not yet implemented")
    }
}