package es.atm.gbee.modules

// These are CLOCK CYCLES, not MACHINE CYCLES
// 1 Machine Cycle = 4 Clock Cycles
const val CYCLES_4 = 4
const val CYCLES_8 = 8
const val CYCLES_12 = 12
const val CYCLES_20 = 20

// Nibble --> 4 Bits
// Byte --> 8 Bits
// Int --> 32 Bits

object CPU {

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

    // Flags description:
    // Z = Set to 1 if the result of the last operation is zero. Cleared to 0 if the result is not zero.
    // N = Set to 1 if the last operation was a subtraction. Cleared to 0 if the last operation was an addition or not a subtraction.
    // H = Set to 1 if there was a carry between the low nibble and the high nibble in the last addition or subtraction operation. Specifically, it is set if the sum of the low nibbles produces a carry, i.e., if the result of adding the low nibbles exceeds 0x0F.
    // C = Set to 1 if there was a carry out of the most significant bit in the last addition or subtraction operation, or if there was a borrow in the last subtraction operation.

    // Total Memory
    val memory = ByteArray(0x10000) // 64KB

    init {
        println("CPU inicializada")
    }

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
            0x1D -> dec_e()         // DEC E
            0x1E -> ld_e_n()        // LD E, n
            0x1F -> rra()           // RRA
            0x20 -> jr_nz_n()       // JR NZ, n
            0x21 -> ld_hl_nn()      // LD HL, nn
            0x22 -> ldi_hl_a()      // LD [HL+], A
            0x23 -> inc_hl()        // INC HL
            0x24 -> inc_h()         // INC H
            0x25 -> dec_h()         // DEC H
            0x26 -> ld_h_n()        // LD H, n
            0x27 -> daa()           // DAA
            0x28 -> jr_z_n()        // JR Z,N
            0x29 -> add_hl_hl()     // ADD HL,HL
            0x2A -> ldi_a_hl()      // LD A,[HL+]
            0x2B -> dec_hl()        // DEC HL
            0x2C -> inc_l()         // INC L
            0x2D -> dec_l()         // DEC L
            0x2E -> ld_l_n()        // LD L, n
            0x2F -> cpl()           // CPL
            0x30 -> jr_nc_n()       // JR NC, n
            0x31 -> ld_sp_nn()      // LD SP, nn
            0x32 -> ldd_hl_a()      // LD [HL-], A
            0x33 -> inc_sp()        // INC SP
            0x34 -> inc_hl_v()      // INC [HL]
            0x35 -> dec_hl_v()      // DEC [HL]
            0x36 -> ld_hl_v_n()     // LD (HL), n
            0x37 -> scf()           // SCF
            0x38 -> jr_c_n()        // JR C, n
            0x39 -> add_hl_sp()     // ADD HL, SP
            0x3A -> ldd_a_hl()      // LD A, [HL-]
            0x3B -> dec_sp()        // DEC SP
            0x3C -> inc_a()         // INC A
            0x3D -> dec_a()         // DEC A
            0x3E -> ld_a_n()        // LD A, n
            0x3F -> ccf()           // CCF
            0x40 -> ld_b_b()        // LD B, B
            0x41 -> ld_b_c()        // LD B, C
            0x42 -> ld_b_d()        // LD B, D
            0x43 -> ld_b_e()        // LD B, E
            0x44 -> ld_b_h()        // LD B, H
            0x45 -> ld_b_l()        // LD B, H
            0x46 -> ld_b_hl()       // LD B, [HL]
            0x47 -> ld_b_a()        // LD B, A
            0x48 -> ld_c_b()        // LD C, B
            0x49 -> ld_c_c()        // LD C, C
            0x4A -> ld_c_d()        // LD C, D
            0x4B -> ld_c_e()        // LD C, E
            0x4C -> ld_c_h()        // LD C, H
            0x4D -> ld_c_l()        // LD C, L
            0x4E -> ld_c_hl()       // LD C, [HL]
            0x4F -> ld_c_a()        // LD C, A
            0x50 -> ld_d_b()        // LD D, B
            0x51 -> ld_d_c()        // LD D, C
            0x52 -> ld_d_d()        // LD D, D
            0x53 -> ld_d_e()        // LD D, E
            0x54 -> ld_d_h()        // LD D, H
            0x55 -> ld_d_l()        // LD D, L
            0x56 -> ld_d_hl()       // LD D, [HL]
            0x57 -> ld_d_a()        // LD D, A
            0x58 -> ld_e_b()        // LD E, B
            0x59 -> ld_e_c()        // LD E, C
            0x5A -> ld_e_d()        // LD E, D
            0x5B -> ld_e_e()        // LD E, E
            0x5C -> ld_e_h()        // LD E, H
            0x5D -> ld_e_l()        // LD E, L
            0x5E -> ld_e_hl()       // LD E, [HL]
            0x5F -> ld_e_a()        // LD E, A
            0x60 -> ld_h_b()        // LD H, B
            0x61 -> ld_h_c()        // LD H, C
            0x62 -> ld_h_d()        // LD H, D
            0x63 -> ld_h_e()        // LD H, E
            0x64 -> ld_h_h()        // LD H, H
            0x65 -> ld_h_l()        // LD H, L
            0x66 -> ld_h_hl()       // LD H, [HL]
            0x67 -> ld_h_a()        // LD H, A
            0x68 -> ld_l_b()        // LD L, B
            0x69 -> ld_l_c()        // LD L, C
            0x6A -> ld_l_d()        // LD L, D
            0x6B -> ld_l_e()        // LD L, E
            0x6C -> ld_l_h()        // LD L, H
            0x6D -> ld_l_l()        // LD L, L
            0x6E -> ld_l_hl()       // LD L, [HL]
            0x6F -> ld_l_a()        // LD L, A
            0x70 -> ld_hl_b()       // LD [HL], B
            0x71 -> ld_hl_c()       // LD [HL], C
            0x72 -> ld_hl_d()       // LD [HL], D
            0x73 -> ld_hl_e()       // LD [HL], E
            0x74 -> ld_hl_h()       // LD [HL], H
            0x75 -> ld_hl_l()       // LD [HL], L
            0x76 -> halt()          // HALT
            0x77 -> ld_hl_a()       // LD [HL], A
            0x78 -> ld_a_b()        // LD A, B
            0x79 -> ld_a_c()        // LD A, C
            0x7A -> ld_a_d()        // LD A, D
            0x7B -> ld_a_e()        // LD A, E
            0x7C -> ld_a_h()        // LD A, H
            0x7D -> ld_a_l()        // LD A, L
            0x7E -> ld_a_hl()       // LD A, [HL]
            0x7F -> ld_a_a()        // LD A, A
            0x80 -> add_a_b()       // ADD A, B
            0x81 -> add_a_c()       // ADD A, C
            0x82 -> add_a_d()       // ADD A, D
            0x83 -> add_a_e()       // ADD A, E
            0x84 -> add_a_h()       // ADD A, H
            0x85 -> add_a_l()       // ADD A, L
            0x86 -> add_a_hl()      // ADD A, [HL]
            0x87 -> add_a_a()       // ADD A, A
            0x88 -> adc_a_b()       // ADC A, B
            0x89 -> adc_a_c()       // ADC A, C
            0x8A -> adc_a_d()       // ADC A, D
            0x8B -> adc_a_e()       // ADC A, E
            0x8C -> adc_a_h()       // ADC A, H
            0x8D -> adc_a_l()       // ADC A, L
            0x8E -> adc_a_hl()      // ADC A, [HL]
            0x8F -> adc_a_a()       // ADC A, A
            0x90 -> sub_b()         // SUB B
            0x91 -> sub_c()         // SUB C
            0x92 -> sub_d()         // SUB D
            0x93 -> sub_e()         // SUB E
            0x94 -> sub_h()         // SUB H
            0x95 -> sub_l()         // SUB L
            0x96 -> sub_hl()        // SUB [HL]
            0x97 -> sub_a()         // SUB A
            0x98 -> sbc_a_b()       // SBC A, B
            0x99 -> sbc_a_c()       // SBC A, C
            0x9A -> sbc_a_d()       // SBC A, D
            0x9B -> sbc_a_e()       // SBC A, E
            0x9C -> sbc_a_h()       // SBC A, H
            0x9D -> sbc_a_l()       // SBC A, L
            else -> throw IllegalArgumentException("Instruction not supported: ${opcode.toInt() and 0xFF}")
        }
    }

    fun get_16bit_address(high: Byte, low: Byte): Int{
        return (high.toInt() shl 8) or (low.toInt() and 0xFF)
    }

    fun set_16bit_address_value(high: Byte, low: Byte, value: Byte){
        val address = (high.toInt() shl 8) or (low.toInt() and 0xFF)
        memory[address] = value
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

    fun updateAddOperationFlags(val1: Int, val2: Int, result: Int){
        updateFlag(FLAG_Z, result == 0)                               // Activated (1) if the result of the operation is 0.
        clearFlag(FLAG_N)                                                     // Set to 0
        updateFlag(FLAG_H, (val1 and 0xF) + (val2 and 0xF) > 0xF)     // Set (1) if there was a carry from the low nibble (the first 4 bits) during the operation.
        updateFlag(FLAG_C, result > 0xFF)                             // Set (1) if there was a carry during the addition (greater than 0xFF)
    }

    fun updateSubOperationFlags(val1: Int, val2: Int, result: Int){
        updateFlag(FLAG_Z, result == 0)                               // Activated (1) if the result of the operation is 0.
        updateFlag(FLAG_N, true)                                      // Activated (1) because a subtraction was performed.
        updateFlag(FLAG_H, (val1 and 0xF) < (val2 and 0xF))           // Set (1) if there was a carry from the low nibble (the first 4 bits) during the subtraction.
        updateFlag(FLAG_C, (val1 and 0xFF) < (val2 and 0xFF))         // Set (1) if there was a carry during the subtraction from the most significant bit (bit 7).
    }

    // -------------------------------- //
    //            OPCODES
    // -------------------------------- //

    fun nop(): Int{
        return CYCLES_4
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
        return CYCLES_4
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
        return CYCLES_4
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
        D = (newDe shr 8).toByte()
        E = (newDe and 0xFF).toByte()
        return CYCLES_8
    }

    fun inc_e(): Int{
        E = inc_8bit_register(E)
        return CYCLES_4
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

    fun ldi_hl_a(): Int{
        val hl = get_16bit_address(H, L)
        memory[hl] = A

        val newHL = (hl + 1) and 0xFFFF
        H = (newHL ushr 8).toByte()
        L = (newHL and 0xFF).toByte()

        return CYCLES_8
    }

    fun inc_hl(): Int{
        val hl = get_16bit_address(H, L)
        val newHL = hl + 1
        H = (newHL ushr 8).toByte()
        L = (newHL and 0xFF).toByte()
        return CYCLES_8
    }

    fun inc_h(): Int{
        D = inc_8bit_register(H)
        return CYCLES_4
    }

    fun dec_h(): Int{
        D = dec_8bit_register(H)
        return CYCLES_4
    }

    fun ld_h_n(): Int{
        val value = fetch()
        H = value
        return CYCLES_8
    }

    fun daa(): Int{

        var result = A.toInt() and 0xFF

        if (!flagIsSet(FLAG_N)) { // Addition

            if ((result and 0x0F) > 9 || flagIsSet(FLAG_H)) // Lower nibble
                result += 0x06

            if ((result and 0xF0) > 0x90 || flagIsSet(FLAG_C)) // Higher nibble
                result += 0x60

        }else{ // Substraction
            if (flagIsSet(FLAG_H)) // Lower nibble
                result -= 0x06

            if (flagIsSet(FLAG_C)) // Higher nibble
                result -= 0x60
        }

        updateFlag(FLAG_Z, (result and 0xFF) == 0x00)
        clearFlag(FLAG_H)
        updateFlag(FLAG_C, result > 0xFF)

        result = result and 0xFF
        A = result.toByte()

        return CYCLES_4
    }

    fun jr_z_n(): Int{
        val offset = fetch()
        if (flagIsSet(FLAG_Z)) {
            PC += offset.toInt()
            return CYCLES_12
        }
        return CYCLES_8
    }

    fun add_hl_hl(): Int{
        val hl = get_16bit_address(H, L)

        val result = hl + hl

        H = (result shr 8).toByte()
        L = (result and 0xFF).toByte()

        clearFlag(FLAG_N)
        updateFlag(FLAG_H, ((hl and 0xFFF) + (hl and 0xFFF)) and 0x1000 != 0)
        updateFlag(FLAG_C, result and 0x10000 != 0)

        return CYCLES_8
    }

    fun ldi_a_hl(): Int{
        val hl = get_16bit_address(H, L)
        A = memory[hl]

        val newHL = (hl + 1) and 0xFFFF
        H = (newHL ushr 8).toByte()
        L = (newHL and 0xFF).toByte()

        return CYCLES_8
    }

    fun dec_hl(): Int{
        val hl = get_16bit_address(H, L)
        val newHl = (hl - 1) and 0xFFFF
        H = (newHl shr 8).toByte()
        L = (newHl and 0xFF).toByte()
        return CYCLES_8
    }

    fun inc_l(): Int{
        L = inc_8bit_register(L)
        return CYCLES_4
    }

    fun dec_l(): Int{
        L = dec_8bit_register(L)
        return CYCLES_4
    }

    fun ld_l_n(): Int{
        val value = fetch()
        L = value
        return CYCLES_8
    }

    fun cpl(): Int{

        A = (A.toInt() xor 0xFF).toByte()

        setFlag(FLAG_N)
        setFlag(FLAG_H)

        return CYCLES_4
    }

    fun jr_nc_n(): Int{
        val offset = fetch()
        if (!flagIsSet(FLAG_C)) {
            PC += offset.toInt()
            return CYCLES_12
        }
        return CYCLES_8
    }

    fun ld_sp_nn(): Int{
        val low = fetch().toInt() and 0xFF
        val high = fetch().toInt() and 0xFF

        SP = (high shl 8) or low

        return CYCLES_12
    }

    fun ldd_hl_a(): Int{
        val hl = get_16bit_address(H, L)
        memory[hl] = A

        val newHL = (hl - 1) and 0xFFFF
        H = (newHL ushr 8).toByte()
        L = (newHL and 0xFF).toByte()

        return CYCLES_8
    }

    fun inc_sp(): Int{
        SP = (SP + 1) and 0xFFFF
        return CYCLES_8
    }

    fun inc_hl_v(): Int{
        val hl = get_16bit_address(H, L)

        memory[hl] = ((memory[hl] + 1) and 0xFF).toByte()

        return CYCLES_12
    }

    fun dec_hl_v(): Int{
        val hl = get_16bit_address(H, L)

        memory[hl] = ((memory[hl] - 1) and 0xFF).toByte()

        return CYCLES_12
    }

    fun ld_hl_v_n(): Int{
        val value = fetch()
        set_16bit_address_value(H, L, value)

        return CYCLES_12
    }

    fun scf(): Int{

        clearFlag(FLAG_N)
        clearFlag(FLAG_H)
        setFlag(FLAG_C)

        return CYCLES_4
    }

    fun jr_c_n(): Int{
        val offset = fetch()
        if (flagIsSet(FLAG_C)) {
            PC += offset.toInt()
            return CYCLES_12
        }
        return CYCLES_8
    }

    fun add_hl_sp(): Int{
        val hl = get_16bit_address(H, L)

        val result = hl + SP

        H = (result shr 8).toByte()
        L = (result and 0xFF).toByte()

        clearFlag(FLAG_N)
        updateFlag(FLAG_H, ((hl and 0xFFF) + (SP and 0xFFF)) and 0x1000 != 0)
        updateFlag(FLAG_C, result and 0x10000 != 0)

        return CYCLES_8
    }

    fun ldd_a_hl(): Int{
        val hl = get_16bit_address(H, L)
        A = memory[hl]

        val newHL = (hl - 1) and 0xFFFF
        H = (newHL ushr 8).toByte()
        L = (newHL and 0xFF).toByte()

        return CYCLES_8
    }

    fun dec_sp(): Int{
        SP = (SP - 1) and 0xFFFF
        return CYCLES_8
    }

    fun inc_a(): Int{
        A = inc_8bit_register(A)
        return CYCLES_4
    }

    fun dec_a(): Int{
        A = dec_8bit_register(A)
        return CYCLES_4
    }

    fun ld_a_n(): Int{
        val value = fetch()
        A = value
        return CYCLES_8
    }

    fun ccf(): Int{

        val newCarry = (F.toInt() and FLAG_C) == 0
        updateFlag(FLAG_C, newCarry)

        setFlag(FLAG_N)
        setFlag(FLAG_H)

        return CYCLES_4
    }

    fun ld_b_b(): Int {
        return CYCLES_4 // LD B, B -> B doesn't change, but we need to return the cycles
    }

    fun ld_b_c(): Int{
        B = C
        return CYCLES_4
    }

    fun ld_b_d(): Int{
        B = D
        return CYCLES_4
    }

    fun ld_b_e(): Int{
        B = E
        return CYCLES_4
    }

    fun ld_b_h(): Int{
        B = H
        return CYCLES_4
    }

    fun ld_b_l(): Int{
        B = L
        return CYCLES_4
    }

    fun ld_b_hl(): Int{
        val hl = get_16bit_address(H, L)
        B = memory[hl]
        return CYCLES_8
    }

    fun ld_b_a(): Int{
        B = A
        return CYCLES_4
    }

    fun ld_c_b(): Int{
        C = B
        return CYCLES_4
    }

    fun ld_c_c(): Int {
        return CYCLES_4 // LD C, C -> C doesn't change, but we need to return the cycles
    }

    fun ld_c_d(): Int{
        C = D
        return CYCLES_4
    }

    fun ld_c_e(): Int{
        C = E
        return CYCLES_4
    }

    fun ld_c_h(): Int{
        C = H
        return CYCLES_4
    }

    fun ld_c_l(): Int{
        C = L
        return CYCLES_4
    }

    fun ld_c_hl(): Int{
        val hl = get_16bit_address(H, L)
        C = memory[hl]
        return CYCLES_8
    }

    fun ld_c_a(): Int{
        C = A
        return CYCLES_4
    }

    fun ld_d_b(): Int{
        D = B
        return CYCLES_4
    }

    fun ld_d_c(): Int{
        D = C
        return CYCLES_4
    }

    fun ld_d_d(): Int {
        return CYCLES_4 // LD D, D -> D doesn't change, but we need to return the cycles
    }

    fun ld_d_e(): Int{
        D = E
        return CYCLES_4
    }

    fun ld_d_h(): Int{
        D = H
        return CYCLES_4
    }

    fun ld_d_l(): Int{
        D = L
        return CYCLES_4
    }

    fun ld_d_hl(): Int{
        val hl = get_16bit_address(H, L)
        D = memory[hl]
        return CYCLES_8
    }

    fun ld_d_a(): Int{
        D = A
        return CYCLES_4
    }

    fun ld_e_b(): Int{
        E = B
        return CYCLES_4
    }

    fun ld_e_c(): Int{
        E = C
        return CYCLES_4
    }

    fun ld_e_d(): Int{
        E = D
        return CYCLES_4
    }

    fun ld_e_e(): Int{
        return CYCLES_4 // LD E, E -> E doesn't change, but we need to return the cycles
    }

    fun ld_e_h(): Int{
        E = H
        return CYCLES_4
    }

    fun ld_e_l(): Int{
        E = L
        return CYCLES_4
    }

    fun ld_e_hl(): Int{
        val hl = get_16bit_address(H, L)
        E = memory[hl]
        return CYCLES_8
    }

    fun ld_e_a(): Int{
        E = A
        return CYCLES_4
    }

    fun ld_h_b(): Int{
        H = B
        return CYCLES_4
    }

    fun ld_h_c(): Int{
        H = C
        return CYCLES_4
    }

    fun ld_h_d(): Int{
        H = D
        return CYCLES_4
    }

    fun ld_h_e(): Int{
        H = E
        return CYCLES_4
    }

    fun ld_h_h(): Int{
        return CYCLES_4 // LD H, H -> H doesn't change, but we need to return the cycles
    }

    fun ld_h_l(): Int{
        H = L
        return CYCLES_4
    }

    fun ld_h_hl(): Int{
        val hl = get_16bit_address(H, L)
        H = memory[hl]
        return CYCLES_8
    }

    fun ld_h_a(): Int{
        H = A
        return CYCLES_4
    }

    fun ld_l_b(): Int{
        L = B
        return CYCLES_4
    }

    fun ld_l_c(): Int{
        L = C
        return CYCLES_4
    }

    fun ld_l_d(): Int{
        L = D
        return CYCLES_4
    }

    fun ld_l_e(): Int{
        L = E
        return CYCLES_4
    }

    fun ld_l_h(): Int{
        L = H
        return CYCLES_4
    }

    fun ld_l_l(): Int{
        return CYCLES_4 // LD L, L -> L doesn't change, but we need to return the cycles
    }

    fun ld_l_hl(): Int{
        val hl = get_16bit_address(H, L)
        L = memory[hl]
        return CYCLES_8
    }

    fun ld_l_a(): Int{
        L = A
        return CYCLES_4
    }

    fun ld_hl_b(): Int{
        set_16bit_address_value(H, L, B)
        return CYCLES_8
    }

    fun ld_hl_c(): Int{
        set_16bit_address_value(H, L, C)
        return CYCLES_8
    }

    fun ld_hl_d(): Int{
        set_16bit_address_value(H, L, D)
        return CYCLES_8
    }

    fun ld_hl_e(): Int{
        val hl = get_16bit_address(H, L)
        memory[hl] = E

        return CYCLES_8
    }

    fun ld_hl_h(): Int{
        set_16bit_address_value(H, L, H)
        return CYCLES_8
    }

    fun ld_hl_l(): Int{
        set_16bit_address_value(H, L, L)
        return CYCLES_8
    }

    fun halt(): Int{
        return CYCLES_4
    }

    fun ld_hl_a(): Int{
        set_16bit_address_value(H, L, A)
        return CYCLES_8
    }

    fun ld_a_b(): Int{
        A = B
        return CYCLES_4
    }

    fun ld_a_c(): Int{
        A = C
        return CYCLES_4
    }

    fun ld_a_d(): Int{
        A = D
        return CYCLES_4
    }

    fun ld_a_e(): Int{
        A = E
        return CYCLES_4
    }

    fun ld_a_h(): Int{
        A = H
        return CYCLES_4
    }

    fun ld_a_l(): Int{
        A = L
        return CYCLES_4
    }

    fun ld_a_hl(): Int{
        val hl = get_16bit_address(H, L)
        A = memory[hl]
        return CYCLES_8
    }

    fun ld_a_a(): Int{
        return CYCLES_4 // LD A, A -> A doesn't change, but we need to return the cycles
    }

    fun add_a_b(): Int{
        val intA = A.toInt()
        val intB = B.toInt()
        val result = intA + intB
        A = (result and 0xFF).toByte()

        updateAddOperationFlags(intA, intB, result)

        return CYCLES_4
    }

    fun add_a_c(): Int{
        val intA = A.toInt()
        val intC = C.toInt()
        val result = intA + intC
        A = (result and 0xFF).toByte()

        updateAddOperationFlags(intA, intC, result)

        return CYCLES_4
    }

    fun add_a_d(): Int{
        val intA = A.toInt()
        val intD = D.toInt()
        val result = intA + intD
        A = (result and 0xFF).toByte()

        updateAddOperationFlags(intA, intD, result)

        return CYCLES_4
    }

    fun add_a_e(): Int{
        val intA = A.toInt()
        val intE = E.toInt()
        val result = intA + intE
        A = (result and 0xFF).toByte()

        updateAddOperationFlags(intA, intE, result)

        return CYCLES_4
    }

    fun add_a_h(): Int{
        val intA = A.toInt()
        val intH = H.toInt()
        val result = intA + intH
        A = (result and 0xFF).toByte()

        updateAddOperationFlags(intA, intH, result)

        return CYCLES_4
    }

    fun add_a_l(): Int{
        val intA = A.toInt()
        val intL = L.toInt()
        val result = intA + intL
        A = (result and 0xFF).toByte()

        updateAddOperationFlags(intA, intL, result)

        return CYCLES_4
    }

    fun add_a_hl(): Int{
        val address = get_16bit_address(H, L)
        val value = memory[address].toInt()
        val intA = A.toInt()
        val result = intA + value
        A = (result and 0xFF).toByte()

        updateAddOperationFlags(intA, value, result)

        return CYCLES_8
    }

    fun add_a_a(): Int{
        val intA = A.toInt()
        val result = intA + intA
        A = (result and 0xFF).toByte()

        updateAddOperationFlags(intA, intA, result)

        return CYCLES_4
    }

    fun adc_a_b(): Int{
        val carry = if (flagIsSet(FLAG_C)) 1 else 0
        val intA = A.toInt()
        val intB = B.toInt()
        val result = intA + (intB + carry)
        A = (result and 0xFF).toByte()

        updateAddOperationFlags(intA, intB + carry, result)

        return CYCLES_4
    }

    fun adc_a_c(): Int{
        val carry = if (flagIsSet(FLAG_C)) 1 else 0
        val intA = A.toInt()
        val intC = C.toInt()
        val result = intA + (intC + carry)
        A = (result and 0xFF).toByte()

        updateAddOperationFlags(intA, intC + carry, result)

        return CYCLES_4
    }

    fun adc_a_d(): Int{
        val carry = if (flagIsSet(FLAG_C)) 1 else 0
        val intA = A.toInt()
        val intD = D.toInt()
        val result = intA + (intD + carry)
        A = (result and 0xFF).toByte()

        updateAddOperationFlags(intA, intD + carry, result)

        return CYCLES_4
    }

    fun adc_a_e(): Int{
        val carry = if (flagIsSet(FLAG_C)) 1 else 0
        val intA = A.toInt()
        val intE = E.toInt()
        val result = intA + (intE + carry)
        A = (result and 0xFF).toByte()

        updateAddOperationFlags(intA, intE + carry, result)

        return CYCLES_4
    }

    fun adc_a_h(): Int{
        val carry = if (flagIsSet(FLAG_C)) 1 else 0
        val intA = A.toInt()
        val intH = H.toInt()
        val result = intA + (intH + carry)
        A = (result and 0xFF).toByte()

        updateAddOperationFlags(intA, intH + carry, result)

        return CYCLES_4
    }

    fun adc_a_l(): Int{
        val carry = if (flagIsSet(FLAG_C)) 1 else 0
        val intA = A.toInt()
        val intL = L.toInt()
        val result = intA + (intL + carry)
        A = (result and 0xFF).toByte()

        updateAddOperationFlags(intA, intL + carry, result)

        return CYCLES_4
    }

    fun adc_a_hl(): Int{
        val carry = if (flagIsSet(FLAG_C)) 1 else 0
        val address = get_16bit_address(H, L)
        val value = memory[address].toInt()

        val intA = A.toInt()
        val result = intA + (value + carry)
        A = (result and 0xFF).toByte()

        updateAddOperationFlags(intA, value + carry, result)

        return CYCLES_8
    }

    fun adc_a_a(): Int{
        val carry = if (flagIsSet(FLAG_C)) 1 else 0
        val intA = A.toInt()
        val result = intA + (intA + carry)
        A = (result and 0xFF).toByte()

        updateAddOperationFlags(intA, intA + carry, result)

        return CYCLES_4
    }

    fun sub_b(): Int{
        val intA = A.toInt()
        val intB = B.toInt()
        val result = intA - intB
        A = (result and 0xFF).toByte()

        updateSubOperationFlags(intA, intB, result)

        return CYCLES_4
    }

    fun sub_c(): Int{
        val intA = A.toInt()
        val intC = C.toInt()
        val result = intA - intC
        A = (result and 0xFF).toByte()

        updateSubOperationFlags(intA, intC, result)

        return CYCLES_4
    }

    fun sub_d(): Int{
        val intA = A.toInt()
        val intD = D.toInt()
        val result = intA - intD
        A = (result and 0xFF).toByte()

        updateSubOperationFlags(intA, intD, result)

        return CYCLES_4
    }

    fun sub_e(): Int{
        val intA = A.toInt()
        val intE = E.toInt()
        val result = intA - intE
        A = (result and 0xFF).toByte()

        updateSubOperationFlags(intA, intE, result)

        return CYCLES_4
    }

    fun sub_h(): Int{
        val intA = A.toInt()
        val intH = H.toInt()
        val result = intA - intH
        A = (result and 0xFF).toByte()

        updateSubOperationFlags(intA, intH, result)

        return CYCLES_4
    }

    fun sub_l(): Int{
        val intA = A.toInt()
        val intL = L.toInt()
        val result = intA - intL
        A = (result and 0xFF).toByte()

        updateSubOperationFlags(intA, intL, result)

        return CYCLES_4
    }

    fun sub_hl(): Int{

        val address = get_16bit_address(H, L)

        val intA = A.toInt()
        val intMem = memory[address].toInt()
        val result = intA - intMem
        A = (result and 0xFF).toByte()

        updateSubOperationFlags(intA, intMem, result)

        return CYCLES_8
    }

    fun sub_a(): Int{
        val intA = A.toInt()
        A = 0x0

        updateSubOperationFlags(intA, intA, 0x0)

        return CYCLES_4
    }

    fun sbc_a_b(): Int{
        val carry = if (flagIsSet(FLAG_C)) 1 else 0
        val intA = A.toInt()
        val intB = B.toInt()
        val result = intA - (intB + carry)
        A = (result and 0xFF).toByte()

        updateSubOperationFlags(intA, intB + carry, result)

        return CYCLES_4
    }

    fun sbc_a_c(): Int{
        val carry = if (flagIsSet(FLAG_C)) 1 else 0
        val intA = A.toInt()
        val intC = C.toInt()
        val result = intA - (intC + carry)
        A = (result and 0xFF).toByte()

        updateSubOperationFlags(intA, intC + carry, result)

        return CYCLES_4
    }

    fun sbc_a_d(): Int{
        val carry = if (flagIsSet(FLAG_C)) 1 else 0
        val intA = A.toInt()
        val intD = D.toInt()
        val result = intA - (intD + carry)
        A = (result and 0xFF).toByte()

        updateSubOperationFlags(intA, intD + carry, result)

        return CYCLES_4
    }

    fun sbc_a_e(): Int{
        val carry = if (flagIsSet(FLAG_C)) 1 else 0
        val intA = A.toInt()
        val intE = E.toInt()
        val result = intA - (intE + carry)
        A = (result and 0xFF).toByte()

        updateSubOperationFlags(intA, intE + carry, result)

        return CYCLES_4
    }

    fun sbc_a_h(): Int{
        val carry = if (flagIsSet(FLAG_C)) 1 else 0
        val intA = A.toInt()
        val intH = H.toInt()
        val result = intA - (intH + carry)
        A = (result and 0xFF).toByte()

        updateSubOperationFlags(intA, intH + carry, result)

        return CYCLES_4
    }

    fun sbc_a_l(): Int{
        val carry = if (flagIsSet(FLAG_C)) 1 else 0
        val intA = A.toInt()
        val intL = L.toInt()
        val result = intA - (intL + carry)
        A = (result and 0xFF).toByte()

        updateSubOperationFlags(intA, intL + carry, result)

        return CYCLES_4
    }
}