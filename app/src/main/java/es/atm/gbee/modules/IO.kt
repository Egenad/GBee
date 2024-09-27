package es.atm.gbee.modules

const val JOYPAD    = 0xFF00 // Joy Pad information and System Type
const val SB        = 0xFF01 // Serial Transfer Data
const val SC        = 0xFF02 // Serial Transfer Control
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

        if(address == IF){
            return Memory.read(IF)
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

        if(address == DMA_RGSTR){
            DMA.start(value)
            return
        }

        if(address == LY_ADDR){

            return
        }

        println("IO - Unkown write on address: $address")
    }

    fun readJoyPad(): Byte{
        var toReturn: Int = 0xCF
        val joyVal = Memory.read(JOYPAD)

        if(GamePadBits.SELECT_DPAD.get(joyVal) == 0){

        }

        return toReturn.toByte()
    }

    fun writeToJoyPad(value: Byte){

    }
}