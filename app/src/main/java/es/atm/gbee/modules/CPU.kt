package es.atm.gbee.modules

const val CYCLES_4 = 4
const val CYCLES_8 = 8
const val CYCLES_12 = 12
const val CYCLES_20 = 20

// Byte --> 8 Bits
// Int --> 32 Bits

class CPU {

    // 8 bits registers
    var A: Byte = 0
    var F: Byte = 0 // Contains the 4 flags (11110000 -> ZNHC0000)
    var B: Byte = 0
    var C: Byte = 0
    var D: Byte = 0
    var E: Byte = 0
    var H: Byte = 0
    var L: Byte = 0

    // 16 bits registers
    var SP: Int = 0xFFFE // Stack Pointer
    var PC: Int = 0 // Program Counter

    // Flags --> Booleans
    private var FLAG_Z = 0x80 // Zero Flag
    private var FLAG_N = 0x40 // Subtract Flag
    private var FLAG_H = 0x20 // Half Carry Flag
    private var FLAG_C = 0x10 // Carry Flag

    // Total Memory
    val memory = ByteArray(0x10000) // 64KB

    fun step() {
        val opcode = fetch()
        val cycles = execute(opcode)
        handleTimers(cycles)
        handleInterrupts()
    }

    private fun setFlag(flag: Int) {
        F = (F.toInt() or flag).toByte()
    }

    private fun clearFlag(flag: Int) {
        F = (F.toInt() and flag.inv()).toByte()
    }

    private fun updateFlag(flag: Int, condition: Boolean) {
        if (condition) {
            setFlag(flag)
        } else {
            clearFlag(flag)
        }
    }

    private fun flagIsSet(flag: Int): Boolean{
        return (F.toInt() and flag) != 0
    }

    private fun handleTimers(cycles: Int) {
        // Actualiza los temporizadores y otros eventos basados en ciclos
    }

    private fun handleInterrupts() {
        // Maneja interrupciones si están habilitadas
    }

    fun fetch(): Byte {
        val opcode = memory[PC]
        PC = (PC + 1) and 0xFFFF
        return opcode
    }

    fun fetch16(): Int {
        val low = fetch().toInt() and 0xFF
        val high = fetch().toInt() and 0xFF
        return (high shl 8) or low
    }

    fun execute(opcode: Byte): Int {
        return when (opcode.toInt() and 0xFF) {
            0x00 -> nop()
            0x01 -> ld_bc_nn()      // LD BC, nn
            0x02 -> ld_bc_a()       // LD [BC], A
            0x03 -> inc_bc()        // INC BC
            0x04 -> inc_b()         // INC B
            0x05 -> dec_b()         // DEC B
            0x06 -> ld_b_n()        // LD B, n
            0x07 -> rlca()          // RLCA
            0x08 -> ld_nn_sp()      // LD [nn], SP
            0x09 -> add_hl_bc()     // ADD HL, BC
            0x0A -> ld_a_bc()       // LD A, [BC]
            0x0B -> dec_bc()        // DEC BC
            0x0C -> inc_c()         // INC C
            0x0D -> dec_c()         // DEC C
            0x0E -> ld_c_n()        // LD C, n
            0x0F -> rrca()          // RRCA
            0x10 -> stop_0()        // STOP 0
            0x11 -> ld_de_nn()      // LD DE, nn
            0x12 -> ld_de_a()       // LD [DE], A
            0x13 -> inc_de()        // INC DE
            0x14 -> inc_d()         // INC D
            0x15 -> dec_d()         // DEC D
            0x16 -> ld_d_n()        // LD D, n
            0x17 -> rla()           // RLA
            0x18 -> jr_n()          // JR n
            0x19 -> add_hl_de()     // ADD HL, DE
            0x1A -> ld_a_de()       // LD A, [DE]
            0x1B -> dec_de()        // DEC DE
            0x1C -> inc_e()         // INC E
            0x1D -> dec_e()         // INC E
            0x1E -> ld_e_n()        // LD E, n
            0x1F -> rra()           // RRA
            0x20 -> jr_nz_n()       // JR NZ, n
            0x21 -> ld_hl_nn()      // LD HL, nn
            0x22 -> ld_hlp_a()      // LD (HL+), A
            0x23 -> inc_hl()        // INC HL
            else -> throw IllegalArgumentException("Instruction not supported: ${opcode.toInt() and 0xFF}")
        }
    }

    fun nop(): Int{
        return CYCLES_4
    }

    fun get_16bit_address(high: Byte, low: Byte): Int{
        return (high.toInt() shl 8) or (low.toInt() and 0xFF)
    }

    fun inc_8bit_register(register: Byte): Byte{
        val toReturn = (register.toInt() + 1).toByte()

        updateFlag(FLAG_Z, toReturn.toInt() == 0x00)
        clearFlag(FLAG_N)
        updateFlag(FLAG_H, ((register.toInt() and 0xF) + 1) and 0x10 != 0x00)

        return toReturn
    }

    fun dec_8bit_register(register: Byte): Byte{
        val toReturn = (register.toInt() - 1).toByte()

        updateFlag(FLAG_Z, toReturn.toInt() == 0x00)
        setFlag(FLAG_N)
        updateFlag(FLAG_H, (register.toInt() and 0xF == 0x00))

        return toReturn
    }

    fun ld_bc_nn(): Int{
        val low = fetch().toInt() and 0xFF
        val high = fetch().toInt() and 0xFF
        B = high.toByte()
        C = low.toByte()

        return CYCLES_12
    }

    fun ld_bc_a(): Int{
        val address = get_16bit_address(B, C)
        memory[address] = A
        return CYCLES_8
    }

    fun inc_bc(): Int{
        val oldValue = get_16bit_address(B, C)
        val newValue = (oldValue + 1) and 0xFFFF
        B = (newValue shr 8).toByte()
        C = newValue.toByte()
        return CYCLES_8
    }

    fun inc_b(): Int{
        B = inc_8bit_register(B)
        return CYCLES_4
    }

    fun dec_b(): Int{
        B = dec_8bit_register(B)
        return CYCLES_4
    }

    fun ld_b_n(): Int{
        val value = fetch()
        B = value
        return CYCLES_8
    }

    fun rlca(): Int{
        val carry = (A.toInt() and 0x80) != 0

        A = ((A.toInt() shl 1) or (if (carry) 1 else 0)).toByte()

        clearFlag(FLAG_Z)
        clearFlag(FLAG_N)
        clearFlag(FLAG_H)
        updateFlag(FLAG_C, carry)

        return CYCLES_4
    }

    fun ld_nn_sp(): Int{
        val address = fetch16()

        val spLow = SP and 0xFF
        val spHigh = (SP shr 8) and 0xFF

        memory[address] = spLow.toByte()
        memory[address + 1] = spHigh.toByte()

        return CYCLES_20
    }

    fun add_hl_bc():Int{
        val hl = get_16bit_address(H, L)
        val bc = get_16bit_address(B, C)

        val result = hl + bc

        H = (result shr 8).toByte()
        L = (result and 0xFF).toByte()

        clearFlag(FLAG_N)
        updateFlag(FLAG_H, ((hl and 0xFFF) + (bc and 0xFFF)) and 0x1000 != 0)
        updateFlag(FLAG_C, result and 0x10000 != 0)

        return CYCLES_8
    }

    fun ld_a_bc(): Int{
        val address = get_16bit_address(B, C)
        A = memory[address]

        return CYCLES_8
    }

    fun dec_bc(): Int{
        val bc = get_16bit_address(B, C)
        val newBc = (bc - 1) and 0xFFFF
        B = (newBc shr 8).toByte()
        C = (newBc and 0xFF).toByte()
        return CYCLES_8
    }

    fun inc_c(): Int{
        C = inc_8bit_register(C)
        return CYCLES_8
    }

    fun dec_c(): Int{
        C = dec_8bit_register(C)
        return CYCLES_4
    }

    fun ld_c_n(): Int{
        val value = fetch()
        C = value
        return CYCLES_8
    }

    fun rrca(): Int{
        val carry = A.toInt() and 1

        A = ((A.toInt() ushr 1) or (carry shl 7)).toByte()

        clearFlag(FLAG_Z)
        clearFlag(FLAG_N)
        clearFlag(FLAG_H)
        updateFlag(FLAG_C, carry != 0)

        return CYCLES_4
    }

    fun stop_0(): Int{
        return CYCLES_4
    }

    fun ld_de_nn(): Int{
        val low = fetch().toInt() and 0xFF
        val high = fetch().toInt() and 0xFF
        D = high.toByte()
        E = low.toByte()

        return CYCLES_12
    }

    fun ld_de_a(): Int{
        val address = ((D.toInt() shl 8) or (E.toInt() and 0xFF)) and 0xFFFF
        memory[address] = A

        return CYCLES_8
    }

    fun inc_de(): Int{
        val oldValue = ((D.toInt() shl 8) or (E.toInt() and 0xFF))
        val newValue = (oldValue + 1) and 0xFFFF
        D = (newValue shr 8).toByte()
        E = newValue.toByte()
        return CYCLES_8
    }

    fun inc_d(): Int{
        D = inc_8bit_register(D)
        return CYCLES_8
    }

    fun dec_d(): Int{
        D = dec_8bit_register(D)
        return CYCLES_4
    }

    fun ld_d_n(): Int{
        val value = fetch()
        D = value
        return CYCLES_8
    }

    fun rla(): Int{
        val carry = if ((F.toInt() and FLAG_C) != 0) 1 else 0

        A = ((A.toInt() shl 1) or carry).toByte()

        clearFlag(FLAG_Z)
        clearFlag(FLAG_N)
        clearFlag(FLAG_H)
        updateFlag(FLAG_C, (A.toInt() and 0x80) != 0)

        return CYCLES_4
    }

    fun jr_n(): Int{
        val offset = fetch()
        PC += offset.toInt()
        return CYCLES_12
    }

    fun add_hl_de(): Int{
        val hl = get_16bit_address(H, L)
        val de = get_16bit_address(D, E)

        val result = hl + de

        H = (result shr 8).toByte()
        L = (result and 0xFF).toByte()

        clearFlag(FLAG_N)
        updateFlag(FLAG_H, ((hl and 0xFFF) + (de and 0xFFF)) and 0x1000 != 0)
        updateFlag(FLAG_C, result and 0x10000 != 0)

        return CYCLES_8
    }

    fun ld_a_de(): Int{
        val address = get_16bit_address(D, E)
        A = memory[address]

        return CYCLES_8
    }

    fun dec_de(): Int{
        val de = get_16bit_address(D, E)
        val newDe = (de - 1) and 0xFFFF
        B = (newDe shr 8).toByte()
        C = (newDe and 0xFF).toByte()
        return CYCLES_8
    }

    fun inc_e(): Int{
        E = inc_8bit_register(E)
        return CYCLES_8
    }

    fun dec_e(): Int{
        E = dec_8bit_register(E)
        return CYCLES_4
    }

    fun ld_e_n(): Int{
        val value = fetch()
        E = value
        return CYCLES_8
    }

    fun rra(): Int{

        val oldValue = A.toInt() and 0xFF
        val carry = if ((F.toInt() and FLAG_C) != 0) 1 else 0

        A = ((A.toInt() ushr 1) or (carry shl 7)).toByte()

        updateFlag(FLAG_Z, A == 0.toByte())
        clearFlag(FLAG_N)
        clearFlag(FLAG_H)
        updateFlag(FLAG_C, (oldValue and 0x01) != 0)

        return CYCLES_4
    }

    fun jr_nz_n(): Int{
        val offset = fetch()
        if (!flagIsSet(FLAG_Z)) {
            PC += offset.toInt()
            return CYCLES_12
        }
        return CYCLES_8
    }

    fun ld_hl_nn(): Int{
        val low = fetch().toInt() and 0xFF
        val high = fetch().toInt() and 0xFF
        H = high.toByte()
        L = low.toByte()

        return CYCLES_12
    }

    fun ld_hlp_a(): Int{
        val hl = get_16bit_address(H, L)
        memory[hl] = A

        val newHL = hl + 1
        H = (newHL ushr 8).toByte()
        L = (newHL and 0xFF).toByte()

        return CYCLES_8
    }

    fun inc_hl(): Int{
        val hl = get_16bit_address(H, L)
        val newHL = hl + 1
        H = (newHL ushr 8).toByte()
        L = (newHL and 0xFF).toByte()
        return CYCLES_4
    }


}