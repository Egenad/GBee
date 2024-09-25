package es.atm.gbee.modules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CPUTest {

    @Test
    fun testLd_BC_NN() {
        CPU.PC = 0x101
        Memory.writeByteOnAddress(0x101, 0xFF.toByte())
        Memory.writeByteOnAddress(0x102, 0xF0.toByte())

        CPU.ld_bc_nn()

        assertEquals(0xF0.toByte(), CPU.B)
        assertEquals(0xFF.toByte(), CPU.C)
    }

    @Test
    fun testOpcodeExecute() {
        CPU.PC  = 0x0

        Memory.writeByteOnAddress(0x0, 0x01.toByte())
        Memory.writeByteOnAddress(0x1, 0xFF.toByte())
        Memory.writeByteOnAddress(0x2, 0xF0.toByte())

        val result = CPU.tick()

        assertEquals(0xF0.toByte(), CPU.B)
        assertEquals(0xFF.toByte(), CPU.C)
        assertEquals(result, true)
    }

    @Test
    fun testLd_BC_A() {
        CPU.B = 0x10.toByte()
        CPU.C = 0xFF.toByte()
        CPU.A = 0x80.toByte()

        CPU.ld_bc_a()

        assertEquals(0x80.toByte(), Memory.getByteOnAddress(0x10FF))
    }

    @Test
    fun testAddA_B() {
        CPU.A = 0x10.toByte()
        CPU.B = 0x20.toByte()

        CPU.add_a_b()

        assertEquals(0x30.toByte(), CPU.A)
        assertFalse(CPU.flagIsSet(FLAG_Z))
        assertFalse(CPU.flagIsSet(FLAG_N))
        assertFalse(CPU.flagIsSet(FLAG_H))
        assertFalse(CPU.flagIsSet(FLAG_C))
    }
}