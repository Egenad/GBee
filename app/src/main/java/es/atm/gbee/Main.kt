package es.atm.gbee

import es.atm.gbee.modules.BIT_1
import es.atm.gbee.modules.BIT_2
import es.atm.gbee.modules.B_RG_N
import es.atm.gbee.modules.CPU

fun main(){
    CPU.PC = 0x0100

    CPU.memory[0x0100] = 0x34.toByte()
    CPU.memory[0x0101] = 0x12.toByte()

    CPU.A = 0x12.toByte()
    CPU.B = 0x34.toByte()
    CPU.C = 0x80.toByte()

    println("\n---------OPCODE TESTS-----------")
    println("--------------------------------\n")

    println("- Before LD A, B")
    println("A: ${CPU.A.toInt().toString(2)}")
    println("B: ${CPU.B.toInt().toString(2)}")

    CPU.ld_a_b()
    println("- After LD A, B")
    println("A: ${CPU.A.toInt().toString(2)}")

    println("\n--------------------------------\n")

    println("- Before SLA C")
    println("C: ${(CPU.C.toInt() and 0xFF).toString(2)}")
    println("Carry: ${CPU.flagIsSet(CPU.FLAG_C)}")

    CPU.sla_c()
    println("- After SLA C")
    println("C: ${(CPU.C.toInt() and 0xFF).toString(2)}")
    println("Carry: ${CPU.flagIsSet(CPU.FLAG_C)}")

    println("\n--------------------------------\n")

    CPU.clearFlag(CPU.FLAG_C)
    CPU.B = 0x81.toByte()

    println("- Before SRA B")
    println("B: ${(CPU.B.toInt() and 0xFF).toString(2)}")
    println("Carry: ${CPU.flagIsSet(CPU.FLAG_C)}")

    CPU.sra_b()
    println("- After SRA B")
    println("B: ${(CPU.B.toInt() and 0xFF).toString(2)}")
    println("Carry: ${CPU.flagIsSet(CPU.FLAG_C)}")

    println("\n--------------------------------\n")

    CPU.B = 0xC5.toByte()

    println("- Before SWAP B")
    println("B: ${(CPU.B.toInt() and 0xFF).toString(2)}")
    println("Carry: ${CPU.flagIsSet(CPU.FLAG_C)}")

    CPU.swap_b()
    println("- After SWAP B")
    println("B: ${(CPU.B.toInt() and 0xFF).toString(2)}")
    println("Carry: ${CPU.flagIsSet(CPU.FLAG_C)}")

    println("\n--------------------------------\n")

    CPU.B = 0x81.toByte()

    println("- Before SRL B")
    println("B: ${(CPU.B.toInt() and 0xFF).toString(2)}")
    println("Carry: ${CPU.flagIsSet(CPU.FLAG_C)}")

    CPU.srl_b()
    println("- After SRL B")
    println("B: ${(CPU.B.toInt() and 0xFF).toString(2)}")
    println("Carry: ${CPU.flagIsSet(CPU.FLAG_C)}")

    println("\n--------------------------------\n")

    CPU.B = 0x81.toByte()

    println("- Before BIT 1, B")
    println("B: ${(CPU.B.toInt() and 0xFF).toString(2)}")
    println("Zero: ${CPU.flagIsSet(CPU.FLAG_Z)}")

    CPU.bit_operation(B_RG_N, BIT_1)
    println("- After BIT 1, B")
    println("B: ${(CPU.B.toInt() and 0xFF).toString(2)}")
    println("Zero: ${CPU.flagIsSet(CPU.FLAG_Z)}")

    println("\n--------------------------------\n")

    CPU.B = 0x81.toByte()

    println("- Before RES 1, B")
    println("B: ${(CPU.B.toInt() and 0xFF).toString(2)}")

    CPU.res_operation(B_RG_N, BIT_1)
    println("- After RES 1, B")
    println("B: ${(CPU.B.toInt() and 0xFF).toString(2)}")

    println("\n--------------------------------\n")

    CPU.B = 0x80.toByte()

    println("- Before SET 2, B")
    println("B: ${(CPU.B.toInt() and 0xFF).toString(2)}")

    CPU.set_operation(B_RG_N, BIT_2)
    println("- After SET 2, B")
    println("B: ${(CPU.B.toInt() and 0xFF).toString(2)}")
}