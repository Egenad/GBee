package es.atm.gbee.modules

const val JOYPAD        = 0xFF00 // Joy Pad information and System Type
const val SB            = 0xFF01 // Serial Transfer Data
const val SC            = 0xFF02 // Serial Transfer Control

const val DISABLE_ROM   = 0xFF50

enum class GamePadBits(val bit: Int) {
    A_BUTTON(0),
    RIGHT_PAD(0),
    B_BUTTON(1),
    LEFT_PAD(1),
    SELECT_BUTTON(2),
    UP_PAD(2),
    START_BUTTON(3),
    DOWN_PAD(3),
    SELECT_DPAD(4),
    SELECT_BUTTONS(5);

    fun get(value: Byte): Int {
        return value.toInt() and (0b1 shl bit)
    }
}

object IO {

    private var serialData = ByteArray(2)

    fun readFromIO(address: Int) : Byte{

        if(address == JOYPAD){
            return readJoyPad()
        }

        if(address == SB){
            return serialData[0]
        }

        if(address == SC){
            return serialData[1]
        }

        if(address in DIV..TAC){
            return Timer.readFromTimer(address)
        }

        if(address == IF || address in LCDC_ADDR..WX){
            return Memory.read(address)
        }

        println("IO - Unsupported read on address: $address")
        return 0
    }

    fun writeToIO(address: Int, value: Byte){

        if(address == JOYPAD){
            writeToJoyPad(value)
            return
        }

        if(address == SB){
            serialData[0] = value
            Memory.write(SB, value)
            return
        }

        if(address == SC){
            serialData[1] = value
            Memory.write(SC, value)
            return
        }

        if(address in DIV..TAC){
            Timer.writeToTimer(address, value)
            return
        }

        if(address == IF){
            Interrupt.set_IF(value.toInt())
            return
        }

        if(address in LCDC_ADDR..WX){
            PPU.writeToLCD(address, value)
            return
        }

        if(address == DISABLE_ROM){
            Memory.write(address, value)
            if(value == 0x1.toByte()){
                CPU.setBootstrapPending(false)
            }
            return
        }

        println("IO - Unkown write on address: $address")
    }

    fun readJoyPad(): Byte{
        return Memory.read(JOYPAD)
    }

    fun writeToJoyPad(value: Byte){
        val oldJoyPad = Memory.read(JOYPAD)
        val newJoyPad = ((oldJoyPad.toInt() and 0xFF) or (value.toInt() and 0x30)).toByte() // Write only to selectors (Bits 4-5)
        Memory.write(JOYPAD, newJoyPad)
    }
}