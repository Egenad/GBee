package es.atm.gbee.modules

object Memory {

    // Total Memory
    private val memory = ByteArray(0x10000) // 64KB

    fun writeByteOnAddress(address: Int, value: Byte){
        // TODO LOGICA
        memory[address] = value
    }

    fun getByteOnAddress(address: Int): Byte{
        // TODO LOGICA
        return memory[address]
    }
}