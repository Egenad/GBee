package es.atm.gbee.modules.mbcs

import android.util.Log

class NoMBC(romBytes: ByteArray) : MBCInterface{

    private var romData: ByteArray? = romBytes

    override fun read(address: Int): Byte {
        if(romData != null) {
            if (address < romData!!.size) {
                return romData!![address]
            } else {
                throw IndexOutOfBoundsException("Out of bounds: $address")
            }
        }
        throw Exception("No Rom Data - $address")
    }

    override fun write(address: Int, value: Byte) {
        Log.w("NoMBC", "Ignored write to ROM (NoMBC does not allow writing)")
    }
}