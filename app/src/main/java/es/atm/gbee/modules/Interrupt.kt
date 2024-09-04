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

    private var IME : Boolean = false               // Flag that enables or disables all interrupts.
    private var IF : Int = 0x00                     // Points out which interrupts are requested.
    private var IE : Int = 0x00                     // Points out which interrupts are enabled.

    fun getPendingInterrupts(): Int{
        return IF and IE and 0x1F
    }

    fun enableInterrupts(enabled: Boolean){
        IME = enabled
    }

    fun getInterruptEnabled(): Boolean{
        return IME
    }

    fun requestInterrupt(type: InterruptType){

    }
}