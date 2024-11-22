package es.atm.gbee.modules

const val JOYPAD        = 0xFF00 // Joy Pad information and System Type
const val SB            = 0xFF01 // Serial Transfer Data
const val SC            = 0xFF02 // Serial Transfer Control

const val DISABLE_ROM   = 0xFF50
const val CGB_WRAM_BANK = 0xFF70

const val START_BUTTON  = "START_BUTTON"
const val SELECT_BUTTON = "SELECT_BUTTON"
const val A_BUTTON      = "A_BUTTON"
const val B_BUTTON      = "B_BUTTON"
const val UP_DPAD       = "UP_DPAD"
const val DOWN_DPAD     = "DOWN_DPAD"
const val LEFT_DPAD     = "LEFT_DPAD"
const val RIGHT_DPAD    = "RIGHT_DPAD"

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
        return (value.toInt() and 0xFF) and (0b1 shl bit)
    }
}

object IO {

    private var serialData = ByteArray(2)

    data class GamepadState(
        var start: Boolean = false,
        var select: Boolean = false,
        var a: Boolean = false,
        var b: Boolean = false,
        var up: Boolean = false,
        var down: Boolean = false,
        var left: Boolean = false,
        var right: Boolean = false
    )

    private val gamePadState = GamepadState()

    init {
        Memory.write(JOYPAD, 0xFF.toByte())
    }

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

    private fun readJoyPad(): Byte{
        return Memory.read(JOYPAD)
    }

    private fun writeToJoyPad(value: Byte){
        Memory.write(JOYPAD, updateJoyPadReadOnlyValues(value))
    }

    private fun updateJoyPadReadOnlyValues(value: Byte): Byte{
        var toReturn = value.toInt() and 0x30

        // 0 == Button Pressed
        // 1 == Button Released

        val selectDPad      = GamePadBits.SELECT_DPAD.get(value)
        val selectButtons   = GamePadBits.SELECT_BUTTONS.get(value)

        if(selectDPad == selectButtons){ // Both selectors are either 0 or 1
            toReturn = toReturn or 0x0F
        }else{
            if(selectDPad == 0){    // DPad selected
                if(!gamePadState.up) toReturn = toReturn or (0b1 shl GamePadBits.UP_PAD.bit)
                if(!gamePadState.down) toReturn = toReturn or (0b1 shl GamePadBits.DOWN_PAD.bit)
                if(!gamePadState.left) toReturn = toReturn or (0b1 shl GamePadBits.LEFT_PAD.bit)
                if(!gamePadState.right) toReturn = toReturn or (0b1 shl GamePadBits.RIGHT_PAD.bit)
            }else{                  // Buttons selected
                if(!gamePadState.select) toReturn = toReturn or (0b1 shl GamePadBits.SELECT_BUTTON.bit)
                if(!gamePadState.start) toReturn = toReturn or (0b1 shl GamePadBits.START_BUTTON.bit)
                if(!gamePadState.a) toReturn = toReturn or (0b1 shl GamePadBits.A_BUTTON.bit)
                if(!gamePadState.b) toReturn = toReturn or (0b1 shl GamePadBits.B_BUTTON.bit)
            }
        }

        //println(Integer.toBinaryString(toReturn))
        return toReturn.toByte()
    }

    fun setButtonPressed(name: String, value: Boolean){
        when(name){
            START_BUTTON -> gamePadState.start = value
            SELECT_BUTTON -> gamePadState.select = value
            A_BUTTON -> gamePadState.a = value
            B_BUTTON -> gamePadState.b = value
            UP_DPAD -> gamePadState.up = value
            DOWN_DPAD -> gamePadState.down = value
            LEFT_DPAD -> gamePadState.left = value
            RIGHT_DPAD -> gamePadState.right = value
        }

        Memory.write(JOYPAD, updateJoyPadReadOnlyValues(Memory.read(JOYPAD)))
    }

}