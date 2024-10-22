package es.atm.gbee.modules.mbcs

interface MBCInterface {
    fun read(address: Int): Byte
    fun write(address: Int, value: Byte)
}