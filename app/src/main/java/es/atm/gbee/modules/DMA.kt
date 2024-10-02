package es.atm.gbee.modules

const val DMA_RGSTR : Int = 0xFF46
object DMA {

    private var active : Boolean = false
    private var address : Byte = 0
    private var value : Byte = 0
    private var startDelay : Byte = 0

    fun start(value: Byte){
        println("DMA - Started")
        active = true
        address = 0
        startDelay = 2
        this.value = value
    }

    fun tick(){
        if (active){
            if(startDelay.toInt() == 0){
                val noSignAddress = address.toInt() and 0xFFFF
                val noSignVal = value.toInt() and 0xFF
                PPU.writeToOAM(noSignAddress, Memory.getByteOnAddress((noSignVal * 0x100) + noSignAddress))
                address++
                active = (address.toInt() and 0xFF) < 0xA0
            }else{
                startDelay--
            }
        }
    }

    fun transferring(): Boolean {
        return active
    }

}