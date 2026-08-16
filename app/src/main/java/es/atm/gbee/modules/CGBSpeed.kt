package es.atm.gbee.modules

object CGBSpeed {
    fun readKEY1(): Byte {
        return ((Memory.read(KEY1).toInt() and 0x81) or 0x7E).toByte()
    }

    fun writeKEY1(value: Byte) {
        val currentSpeed = Memory.read(KEY1).toInt() and 0x80
        val switch = value.toInt() and 0x01
        Memory.write(KEY1, (currentSpeed or switch).toByte())
    }

    fun executeStop(): Boolean {
        val key1Value = Memory.read(KEY1).toInt() and 0xFF
        val switchArmed = (key1Value and 0x01) != 0

        if (!switchArmed) {
            return false
        }

        Memory.write(KEY1, (key1Value xor 0x81).toByte())
        return true
    }

    fun isDoubleSpeed(): Boolean{
        val key1Value = Memory.read(KEY1).toInt() and 0xFF
        return (key1Value and 0x80) != 0

    }
}