package es.atm.gbee.modules

const val DIV   = 0xFF04 // Divider Register (16 bit Register, but only the upper 8 bit (0-7) are public to the developer)
const val TIMA  = 0xFF05 // Timer Counter
const val TMA   = 0xFF06 // Timer Module
const val TAC   = 0xFF07 // Timer Control

/* DIV - 16 Bit Register (Lower 8 bits are obscured in memory -> can't be accessed by developers)
 * 0	Incremented every machine cycle         (4 clock cycles)
 * 1	Incremented every 2 machine cycles      (8 clock cycles)
 * 2	Incremented every 4 machine cycles      (16 clock cycles)
 * 3	Incremented every 8 machine cycles      (32 clock cycles)
 * 4	Incremented every 16 machine cycles     (64 clock cycles)
 * 5	Incremented every 32 machine cycles     (128 clock cycles)
 * 6	Incremented every 64 machine cycles     (256 clock cycles)
 * 7	Incremented every 128 machine cycles    (512 clock cycles)
 * 8	Incremented every 256 machine cycles    (1024 clock cycles)
 * 9	Incremented every 512 machine cycles    (2048 clock cycles)
 * 10	Incremented every 1024 machine cycles   (4096 clock cycles)
 * 11	Incremented every 2048 machine cycles   (8192 clock cycles)
 * 12	Incremented every 4096 machine cycles   (16384 clock cycles)
 * 13	Incremented every 8192 machine cycles   (32768 clock cycles)
 * 14	Incremented every 16384 machine cycles  (65536 clock cycles)
 * 15	Incremented every 32768 machine cycles  (131072 clock cycles)
 */

/* TAC 1-0 Bits = Clock Select
 * 0b00 = 256 M-Cycles = 1024 Clock Cycles  - DIV Bit 9
 * 0b01 = 4 M-Cycles = 16 Clock Cycles      - DIV Bit 3
 * 0b10 = 16 M-Cycles = 64 Clock Cycles     - DIV Bit 5
 * 0b11 = 64 M-Cycles = 256 Clock Cycles    - DIV Bit 7
 */

object Timer {

    private var div16 : Int = 0x0000

    fun tick(){

        val prevDIV = div16

        div16 = (div16 + 1) and 0xFFFF

        var timerUpdate = false
        val tacValue = Memory.read(TAC).toInt() and 0xFF

        when(tacValue and 0b11){
            0b00 -> timerUpdate = (prevDIV and (1 shl 9) != 0) && (div16 and (1 shl 9) == 0)
            0b01 -> timerUpdate = (prevDIV and (1 shl 3) != 0) && (div16 and (1 shl 3) == 0)
            0b10 -> timerUpdate = (prevDIV and (1 shl 5) != 0) && (div16 and (1 shl 5) == 0)
            0b11 -> timerUpdate = (prevDIV and (1 shl 7) != 0) && (div16 and (1 shl 7) == 0)
        }

        if(timerUpdate && (tacValue and (1 shl 2) != 0)){
            var timaValue = Memory.read(TIMA).toInt() and 0xFF
            timaValue++
            Memory.write(TIMA, timaValue.toByte())

            if(timaValue == 0xFF){
                Memory.write(TIMA, Memory.read(TMA))
                Interrupt.requestInterrupt(Interrupt.InterruptType.TIMER.getByteMask())
            }
        }
    }

    fun readFromTimer(address: Int): Byte{
        when(address){
            DIV -> return ((div16 shr 8) and 0xFF).toByte()
            TIMA -> return Memory.read(TIMA)
            TMA -> return Memory.read(TMA)
            TAC -> return Memory.read(TAC)
        }

        return 0
    }

    fun writeToTimer(address: Int, value: Byte){
        when(address){
            DIV -> {
                div16 = (div16 and 0xFF00)
            }
            TIMA -> Memory.write(TIMA, value)
            TMA -> Memory.write(TMA, value)
            TAC -> Memory.write(TAC, value)
        }
    }

    fun reset(){
        div16 = 0x0000
        Memory.write(DIV, 0x00)
        Memory.write(TIMA, 0x00)
        Memory.write(TMA, 0x00)
        Memory.write(TAC, 0x00)
    }

}