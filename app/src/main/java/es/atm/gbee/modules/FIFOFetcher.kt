package es.atm.gbee.modules

enum class FetcherState(val number: Int){
    OBTAIN_TILE(0),
    LOW_DATA_TILE(1),
    HIGH_DATA_TILE(2),
    SLEEP(3),
    PUSH(4)
}

class FifoEntry(val value: Int, var next: FifoEntry? = null)

class Fifo {
    private var head: FifoEntry? = null
    private var tail: FifoEntry? = null
    private var size: Int = 0

    fun push(value: Int) {
        val newEntry = FifoEntry(value)
        if (tail == null) { // First Entry
            head = newEntry
            tail = newEntry
        } else {
            tail?.next = newEntry
            tail = newEntry
        }
        size++
    }

    fun pop(): Int? {
        if (head == null)
            return null

        val dequeuedValue = head?.value
        head = head?.next
        if (head == null) {
            tail = null
        }
        size--
        return dequeuedValue
    }

    fun peek(): Int? {
        return head?.value
    }

    fun isEmpty(): Boolean {
        return size == 0
    }
}

class FifoFetcher {
    private var state : FetcherState = FetcherState.OBTAIN_TILE

    private var lineX : Int         = 0     // X position of the line
    private var actualTile : Int    = 0     // Tile to be fetched
    private var pushedPixels : Int  = 0     // Pixels pushed to the screen
    private var fifoPixels : Int    = 0     // Pixels in the fifo

    private var fifo : Fifo = Fifo()

    private var mapY: Byte = 0
    private var mapX: Byte = 0

    fun process(){
        mapY = ((Memory.getByteOnAddress(SCY).toInt() and 0xFF) + (Memory.getByteOnAddress(LY_ADDR).toInt() and 0xFF)).toByte()
        mapY = ((Memory.getByteOnAddress(SCX).toInt() and 0xFF) + actualTile).toByte()

    }

    private fun fetch_getTile(){
        val tilemapBaseAddress = if (isWindowTile()) 0x9C00 else 0x9800

        val scx = Memory.getByteOnAddress(SCX)
        val scy = Memory.getByteOnAddress(SCY)

        if(PPU.lcdIsEnabled()){

        }
    }

    private fun isWindowTile(): Boolean{
        return false
    }

    private fun windowIsVisible(): Boolean{

        val wx = Memory.getByteOnAddress(WX)
        val wy = Memory.getByteOnAddress(WY)

        return PPU.windowIsEnabled() && wx >= 0 && wx < GB_X_RESOLUTION && wy > 0
    }

    fun clear(){
        while(!fifo.isEmpty()){
            fifo.pop()
        }
    }

    fun setState(state: FetcherState){
        this.state = state
    }

    fun getState(): FetcherState{
        return state
    }

    fun setLineX(x: Int){
        lineX = x
    }

    fun getLineX(): Int{
        return lineX
    }

    fun setActualTile(x: Int){
        actualTile = x
    }

    fun getActualTile(): Int{
        return actualTile
    }

    fun setPushedPixels(x: Int){
        pushedPixels = x
    }

    fun getPushedPixels(): Int{
        return pushedPixels
    }

    fun setFifoPixels(x: Int){
        fifoPixels = x
    }

    fun getFifoPixels(): Int{
        return fifoPixels
    }
}