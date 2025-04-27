package es.atm.gbee.modules

const val DMA_RGSTR : Int           = 0xFF46
const val TOTAL_BYTES_TO_COPY : Int = 0xA0

object DMA {

    private var active : Boolean = false
    private var offset : Int = 0
    private var startAddress : Int = 0
    private var startDelay : Byte = 0

    fun start(value: Byte){                             // Value: Indicates range (0xXX00 - 0xXX9F)
        active = true
        offset = 0
        startDelay = 2
        startAddress = (value.toInt() and 0xFF) shl 8   // If value is 0xC0, startAddress will be 0xC000
        Interrupt.enableInterrupts(false)               // Disable interrupts while copying
    }

    fun tick(){
        if (active){
            if(startDelay.toInt() == 0){
                val address = startAddress + offset
                PPU.writeToOAM(address, startAddress, Memory.getByteOnAddress(address))
                offset++
                active = (offset and 0xFF) < TOTAL_BYTES_TO_COPY

                if(!active)
                    Interrupt.enableInterrupts(true)
            }else{
                startDelay--
            }
        }
    }

    fun transferring(): Boolean {
        return active
    }

    fun reset(){
        active = false
        offset = 0
        startAddress = 0
        startDelay = 0
    }

}