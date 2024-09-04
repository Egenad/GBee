package es.atm.gbee.modules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CPUTest {
    @Test
    fun testAddA_B() {
        CPU.A = 0x10.toByte()
        CPU.B = 0x20.toByte()

        CPU.add_a_b()

        assertEquals(0x30.toByte(), CPU.A)
        assertFalse(CPU.flagIsSet(CPU.FLAG_Z))
        assertFalse(CPU.flagIsSet(CPU.FLAG_N))
        assertFalse(CPU.flagIsSet(CPU.FLAG_H))
        assertFalse(CPU.flagIsSet(CPU.FLAG_C))
    }
}