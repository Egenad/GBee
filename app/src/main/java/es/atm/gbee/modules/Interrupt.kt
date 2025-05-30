package es.atm.gbee.modules

const val VBLANK_PTR    = 0x0040
const val LCD_STAT_PTR  = 0x0048
const val TIMER_PTR     = 0x0050
const val SERIAL_PTR    = 0x0048
const val JOYPAD_PTR    = 0x0060

const val IF            = 0xFF0F // Points out which interrupts are requested.
const val IE            = 0xFFFF // Points out which interrupts are enabled.

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

        fun getByteMask(): Byte{
            return getInterruptMask().toByte()
        }
    }

    private var IME : Boolean = false               // Flag that enables or disables all interrupts. (Interrupt Master Enable)

    fun getPendingInterrupts(): Int{

        val ifVal = Memory.getByteOnAddress(IF).toInt()
        val ieVal = Memory.getByteOnAddress(IE).toInt()

        return ifVal and ieVal and 0x1F
    }

    fun enableInterrupts(enabled: Boolean){
        IME = enabled
    }

    fun getInterruptEnabled(): Boolean{
        return IME
    }

    fun requestInterrupt(byte: Byte){
        val result = ((Memory.getByteOnAddress(IF).toInt() and 0xFF) or (byte.toInt() and 0xFF)).toByte()
        Memory.writeByteOnAddress(IF, result)
    }

    fun flush(){

        val activeInterrupts = getPendingInterrupts()

        if ((activeInterrupts and InterruptType.VBLANK.getInterruptMask()) != 0)        return handleInterrupt(VBLANK_PTR, InterruptType.VBLANK)
        else if ((activeInterrupts and InterruptType.LCD_STAT.getInterruptMask()) != 0) return handleInterrupt(LCD_STAT_PTR, InterruptType.LCD_STAT)
        else if ((activeInterrupts and InterruptType.TIMER.getInterruptMask()) != 0)    return handleInterrupt(TIMER_PTR, InterruptType.TIMER)
        else if ((activeInterrupts and InterruptType.SERIAL.getInterruptMask()) != 0)   return handleInterrupt(SERIAL_PTR, InterruptType.SERIAL)
        else if ((activeInterrupts and InterruptType.JOYPAD.getInterruptMask()) != 0)   return handleInterrupt(JOYPAD_PTR, InterruptType.JOYPAD)
    }

    private fun handleInterrupt(address: Int, type: InterruptType){

        enableInterrupts(false)       // IME gets disabled to prevent other interruptions from happening

        // Deactivate interrupt
        val bit = (type.getInterruptMask()).inv()
        val ifValue = (Memory.getByteOnAddress(IF).toInt() and 0xFF)
        set_IF(ifValue and bit)

        CPU.executeInterrupt(address)
    }

    fun set_IF(value: Int){
        val newValue = ((value or 0xE0) and 0xFF).toByte()
        Memory.write(IF, newValue)
    }

    fun reset(){
        IME = false
        Memory.write(IF, 0x00)
        Memory.write(IE, 0x00)
    }
}