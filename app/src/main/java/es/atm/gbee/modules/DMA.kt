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
                PPU.writeToOAM(address.toInt() and 0xFF, Memory.getByteOnAddress((value * 0x100) + address))
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