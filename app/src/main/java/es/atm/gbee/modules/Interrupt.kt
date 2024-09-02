package es.atm.gbee.modules

object Interrupt {
    enum class InterruptType(val interruptBit: Int) {
        VBLANK(0),
        LCD_STAT(1),
        TIMER(2),
        SERIAL(3),
        JOYPAD(4);

        fun getInterruptMask(): Int {
            return 1 shl interruptBit
        }
    }

    private var interruptEnabled : Boolean = false

    fun enableInterrupts(enabled: Boolean){
        interruptEnabled = enabled
    }
}