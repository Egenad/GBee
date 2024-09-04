package es.atm.gbee.modules

// These are CLOCK CYCLES, not MACHINE CYCLES
// 1 Machine Cycle = 4 Clock Cycles
const val CYCLES_4 = 4      // 1 MC
const val CYCLES_8 = 8      // 2 MC
const val CYCLES_12 = 12    // 3 MC
const val CYCLES_16 = 16    // 4 MC
const val CYCLES_20 = 20    // 5 MC
const val CYCLES_24 = 24    // 6 MC

const val B_RG_N    = 1
const val C_RG_N    = 2
const val D_RG_N    = 3
const val E_RG_N    = 4
const val H_RG_N    = 5
const val L_RG_N    = 6
const val HL_RG_N   = 7
const val A_RG_N    = 8

const val BIT_0     = 0
const val BIT_1     = 1
const val BIT_2     = 2
const val BIT_3     = 3
const val BIT_4     = 4
const val BIT_5     = 5
const val BIT_6     = 6
const val BIT_7     = 7

// Nibble   -->  4 Bits
// Byte     -->  8 Bits
// Int      --> 32 Bits

object CPU {

    // 8 bits registers
    var A: Byte = 0
    var F: Byte = 0             // Contains the 4 flags (11110000 -> ZNHC0000)
    var B: Byte = 0
    var C: Byte = 0
    var D: Byte = 0
    var E: Byte = 0
    var H: Byte = 0
    var L: Byte = 0

    // 16 bits registers
    var SP: Int = 0xFFFE        // Stack Pointer
    var PC: Int = 0             // Program Counter

    // Flags --> Booleans
    var FLAG_Z = 0x80           // Zero Flag
    var FLAG_N = 0x40           // Subtract Flag
    var FLAG_H = 0x20           // Half Carry Flag
    var FLAG_C = 0x10           // Carry Flag

    private var cycles = 0

    // Flags description:
    // Z = Set to 1 if the result of the last operation is zero. Cleared to 0 if the result is not zero.
    // N = Set to 1 if the last operation was a subtraction. Cleared to 0 if the last operation was an addition or not a subtraction.
    // H = Set to 1 if there was a carry between the low nibble and the high nibble in the last addition or subtraction operation. Specifically, it is set if the sum of the low nibbles produces a carry, i.e., if the result of adding the low nibbles exceeds 0x0F.
    // C = Set to 1 if there was a carry out of the most significant bit in the last addition or subtraction operation, or if there was a borrow in the last subtraction operation.

    // Total Memory
    val memory = ByteArray(0x10000) // 64KB

    private var cpu_halted = false

    init {
        println("CPU inicializada")
    }

    fun step() {
        val opcode = fetch()
        cycles += execute(opcode)
        handleTimers(cycles)
        handleInterrupts()
    }

    fun setFlag(flag: Int) {
        F = (F.toInt() or flag).toByte()
    }

    fun clearFlag(flag: Int) {
        F = (F.toInt() and flag.inv()).toByte()
    }

    fun updateFlag(flag: Int, condition: Boolean) {
        if (condition) {
            setFlag(flag)
        } else {
            clearFlag(flag)
        }
    }

    fun flagIsSet(flag: Int): Boolean{
        return (F.toInt() and flag) != 0
    }

    private fun handleTimers(cycles: Int) {
        // Actualiza los temporizadores y otros eventos basados en ciclos
    }

    private fun handleInterrupts() {
        // Maneja interrupciones si están habilitadas
    }

    fun fetch(): Byte {
        val byte = memory[PC]
        PC = (PC + 1) and 0xFFFF
        return byte
    }

    fun fetch16(): Int {
        val low = fetch().toInt() and 0xFF
        val high = fetch().toInt() and 0xFF
        return (high shl 8) or low
    }

    fun execute(opcode: Byte): Int {
        return when (opcode.toInt() and 0xFF) {
            0x00 -> nop()
            0x01 -> ld_bc_nn()              // LD BC, nn
            0x02 -> ld_bc_a()               // LD [BC], A
            0x03 -> inc_bc()                // INC BC
            0x04 -> inc_b()                 // INC B
            0x05 -> dec_b()                 // DEC B
            0x06 -> ld_b_n()                // LD B, n
            0x07 -> rlca()                  // RLCA
            0x08 -> ld_nn_sp()              // LD [nn], SP
            0x09 -> add_hl_bc()             // ADD HL, BC
            0x0A -> ld_a_bc()               // LD A, [BC]
            0x0B -> dec_bc()                // DEC BC
            0x0C -> inc_c()                 // INC C
            0x0D -> dec_c()                 // DEC C
            0x0E -> ld_c_n()                // LD C, n
            0x0F -> rrca()                  // RRCA
            0x10 -> stop_0()                // STOP 0
            0x11 -> ld_de_nn()              // LD DE, nn
            0x12 -> ld_de_a()               // LD [DE], A
            0x13 -> inc_de()                // INC DE
            0x14 -> inc_d()                 // INC D
            0x15 -> dec_d()                 // DEC D
            0x16 -> ld_d_n()                // LD D, n
            0x17 -> rla()                   // RLA
            0x18 -> jr_n()                  // JR n
            0x19 -> add_hl_de()             // ADD HL, DE
            0x1A -> ld_a_de()               // LD A, [DE]
            0x1B -> dec_de()                // DEC DE
            0x1C -> inc_e()                 // INC E
            0x1D -> dec_e()                 // DEC E
            0x1E -> ld_e_n()                // LD E, n
            0x1F -> rra()                   // RRA
            0x20 -> jr_nz_n()               // JR NZ, n
            0x21 -> ld_hl_nn()              // LD HL, nn
            0x22 -> ldi_hl_a()              // LD [HL+], A
            0x23 -> inc_hl()                // INC HL
            0x24 -> inc_h()                 // INC H
            0x25 -> dec_h()                 // DEC H
            0x26 -> ld_h_n()                // LD H, n
            0x27 -> daa()                   // DAA
            0x28 -> jr_z_n()                // JR Z,N
            0x29 -> add_hl_hl()             // ADD HL,HL
            0x2A -> ldi_a_hl()              // LD A,[HL+]
            0x2B -> dec_hl()                // DEC HL
            0x2C -> inc_l()                 // INC L
            0x2D -> dec_l()                 // DEC L
            0x2E -> ld_l_n()                // LD L, n
            0x2F -> cpl()                   // CPL
            0x30 -> jr_nc_n()               // JR NC, n
            0x31 -> ld_sp_nn()              // LD SP, nn
            0x32 -> ldd_hl_a()              // LD [HL-], A
            0x33 -> inc_sp()                // INC SP
            0x34 -> inc_hl_v()              // INC [HL]
            0x35 -> dec_hl_v()              // DEC [HL]
            0x36 -> ld_hl_v_n()             // LD (HL), n
            0x37 -> scf()                   // SCF
            0x38 -> jr_c_n()                // JR C, n
            0x39 -> add_hl_sp()             // ADD HL, SP
            0x3A -> ldd_a_hl()              // LD A, [HL-]
            0x3B -> dec_sp()                // DEC SP
            0x3C -> inc_a()                 // INC A
            0x3D -> dec_a()                 // DEC A
            0x3E -> ld_a_n()                // LD A, n
            0x3F -> ccf()                   // CCF
            0x40 -> ld_b_b()                // LD B, B
            0x41 -> ld_b_c()                // LD B, C
            0x42 -> ld_b_d()                // LD B, D
            0x43 -> ld_b_e()                // LD B, E
            0x44 -> ld_b_h()                // LD B, H
            0x45 -> ld_b_l()                // LD B, H
            0x46 -> ld_b_hl()               // LD B, [HL]
            0x47 -> ld_b_a()                // LD B, A
            0x48 -> ld_c_b()                // LD C, B
            0x49 -> ld_c_c()                // LD C, C
            0x4A -> ld_c_d()                // LD C, D
            0x4B -> ld_c_e()                // LD C, E
            0x4C -> ld_c_h()                // LD C, H
            0x4D -> ld_c_l()                // LD C, L
            0x4E -> ld_c_hl()               // LD C, [HL]
            0x4F -> ld_c_a()                // LD C, A
            0x50 -> ld_d_b()                // LD D, B
            0x51 -> ld_d_c()                // LD D, C
            0x52 -> ld_d_d()                // LD D, D
            0x53 -> ld_d_e()                // LD D, E
            0x54 -> ld_d_h()                // LD D, H
            0x55 -> ld_d_l()                // LD D, L
            0x56 -> ld_d_hl()               // LD D, [HL]
            0x57 -> ld_d_a()                // LD D, A
            0x58 -> ld_e_b()                // LD E, B
            0x59 -> ld_e_c()                // LD E, C
            0x5A -> ld_e_d()                // LD E, D
            0x5B -> ld_e_e()                // LD E, E
            0x5C -> ld_e_h()                // LD E, H
            0x5D -> ld_e_l()                // LD E, L
            0x5E -> ld_e_hl()               // LD E, [HL]
            0x5F -> ld_e_a()                // LD E, A
            0x60 -> ld_h_b()                // LD H, B
            0x61 -> ld_h_c()                // LD H, C
            0x62 -> ld_h_d()                // LD H, D
            0x63 -> ld_h_e()                // LD H, E
            0x64 -> ld_h_h()                // LD H, H
            0x65 -> ld_h_l()                // LD H, L
            0x66 -> ld_h_hl()               // LD H, [HL]
            0x67 -> ld_h_a()                // LD H, A
            0x68 -> ld_l_b()                // LD L, B
            0x69 -> ld_l_c()                // LD L, C
            0x6A -> ld_l_d()                // LD L, D
            0x6B -> ld_l_e()                // LD L, E
            0x6C -> ld_l_h()                // LD L, H
            0x6D -> ld_l_l()                // LD L, L
            0x6E -> ld_l_hl()               // LD L, [HL]
            0x6F -> ld_l_a()                // LD L, A
            0x70 -> ld_hl_b()               // LD [HL], B
            0x71 -> ld_hl_c()               // LD [HL], C
            0x72 -> ld_hl_d()               // LD [HL], D
            0x73 -> ld_hl_e()               // LD [HL], E
            0x74 -> ld_hl_h()               // LD [HL], H
            0x75 -> ld_hl_l()               // LD [HL], L
            0x76 -> halt()                  // HALT
            0x77 -> ld_hl_a()               // LD [HL], A
            0x78 -> ld_a_b()                // LD A, B
            0x79 -> ld_a_c()                // LD A, C
            0x7A -> ld_a_d()                // LD A, D
            0x7B -> ld_a_e()                // LD A, E
            0x7C -> ld_a_h()                // LD A, H
            0x7D -> ld_a_l()                // LD A, L
            0x7E -> ld_a_hl()               // LD A, [HL]
            0x7F -> ld_a_a()                // LD A, A
            0x80 -> add_a_b()               // ADD A, B
            0x81 -> add_a_c()               // ADD A, C
            0x82 -> add_a_d()               // ADD A, D
            0x83 -> add_a_e()               // ADD A, E
            0x84 -> add_a_h()               // ADD A, H
            0x85 -> add_a_l()               // ADD A, L
            0x86 -> add_a_hl()              // ADD A, [HL]
            0x87 -> add_a_a()               // ADD A, A
            0x88 -> adc_a_b()               // ADC A, B
            0x89 -> adc_a_c()               // ADC A, C
            0x8A -> adc_a_d()               // ADC A, D
            0x8B -> adc_a_e()               // ADC A, E
            0x8C -> adc_a_h()               // ADC A, H
            0x8D -> adc_a_l()               // ADC A, L
            0x8E -> adc_a_hl()              // ADC A, [HL]
            0x8F -> adc_a_a()               // ADC A, A
            0x90 -> sub_b()                 // SUB B
            0x91 -> sub_c()                 // SUB C
            0x92 -> sub_d()                 // SUB D
            0x93 -> sub_e()                 // SUB E
            0x94 -> sub_h()                 // SUB H
            0x95 -> sub_l()                 // SUB L
            0x96 -> sub_hl()                // SUB [HL]
            0x97 -> sub_a()                 // SUB A
            0x98 -> sbc_a_b()               // SBC A, B
            0x99 -> sbc_a_c()               // SBC A, C
            0x9A -> sbc_a_d()               // SBC A, D
            0x9B -> sbc_a_e()               // SBC A, E
            0x9C -> sbc_a_h()               // SBC A, H
            0x9D -> sbc_a_l()               // SBC A, L
            0x9E -> sbc_a_hl()              // SBC A, [HL]
            0x9F -> sbc_a_a()               // SBC A, A
            0xA0 -> and_b()                 // AND B
            0xA1 -> and_c()                 // AND C
            0xA2 -> and_d()                 // AND D
            0xA3 -> and_e()                 // AND E
            0xA4 -> and_h()                 // AND H
            0xA5 -> and_l()                 // AND L
            0xA6 -> and_hl()                // AND [HL]
            0xA7 -> and_a()                 // AND A
            0xA8 -> xor_b()                 // XOR B
            0xA9 -> xor_c()                 // XOR C
            0xAA -> xor_d()                 // XOR D
            0xAB -> xor_e()                 // XOR E
            0xAC -> xor_h()                 // XOR H
            0xAD -> xor_l()                 // XOR L
            0xAE -> xor_hl()                // XOR [HL]
            0xAF -> xor_a()                 // XOR A
            0xB0 -> or_b()                  // OR B
            0xB1 -> or_c()                  // OR C
            0xB2 -> or_d()                  // OR D
            0xB3 -> or_e()                  // OR E
            0xB4 -> or_h()                  // OR H
            0xB5 -> or_l()                  // OR L
            0xB6 -> or_hl()                 // OR [HL]
            0xB7 -> or_a()                  // OR A
            0xB8 -> cp_b()                  // CP B
            0xB9 -> cp_c()                  // CP C
            0xBA -> cp_d()                  // CP D
            0xBB -> cp_e()                  // CP E
            0xBC -> cp_h()                  // CP H
            0xBD -> cp_l()                  // CP L
            0xBE -> cp_hl()                 // CP [HL]
            0xBF -> cp_a()                  // CP A
            0xC0 -> ret_nz()                // RET NZ
            0xC1 -> pop_bc()                // POP BC
            0xC2 -> jp_nz_nn()              // JP NZ, NN
            0xC3 -> jp_nn()                 // JP NN
            0xC4 -> call_nz_nn()            // CALL NZ, NN
            0xC5 -> push_bc()               // PUSH BC
            0xC6 -> add_a_n()               // ADD A, N
            0xC7 -> rst(0x0000)             // RST 00H
            0xC8 -> ret_z()                 // RET Z
            0xC9 -> ret()                   // RET
            0xCA -> jp_z_nn()               // JP Z, NN
            0xCB -> prefix_cb()             // PREFIX CB
            0xCC -> call_z_nn()             // CALL Z, NN
            0xCD -> call()                  // CALL
            0xCE -> adc_a_n()               // ADC A, N
            0xCF -> rst(0x0008)             // RST 08H
            0xD0 -> ret_nc()                // RET NC
            0xD1 -> pop_de()                // POP DE
            0xD2 -> jp_nc_nn()              // JP NC, NN
            0xD4 -> call_nc_nn()            // CALL NC, NN
            0xD5 -> push_de()               // PUSH DE
            0xD6 -> sub_n()                 // SUB N
            0xD7 -> rst(0x0010)             // RST 10H
            0xD8 -> ret_c()                 // RET C
            0xD9 -> reti()                  // RETI
            0xDA -> jp_c_nn()               // JP C, NN
            0xDC -> call_c_nn()             // CALL C, NN
            0xDE -> sbc_a_n()               // SBC A, N
            0xDF -> rst(0x0018)             // RST 18H
            0xE0 -> ldh_n_a()               // LDH [N], A
            0xE1 -> pop_hl()                // POP HL
            0xE2 -> ld_cn_a()               // LD [C], A
            0xE5 -> push_hl()               // PUSH HL
            0xE6 -> and_n()                 // AND N
            0xE7 -> rst(0x0020)             // RST 20H
            0xE8 -> add_sp_n()              // ADD SP, N
            0xE9 -> jp_hl()                 // JP [HL]
            0xEA -> ld_nn_a()               // LD NN, A
            0xEE -> xor_n()                 // XOR N
            0xEF -> rst(0x0028)             // RST 28H
            0xF0 -> ldh_a_n()               // LDH A, [N]
            0xF1 -> pop_af()                // POP AF
            0xF2 -> ld_a_cn()               // LD A, [C]
            0xF3 -> di()                    // DI
            0xF5 -> push_af()               // PUSH AF
            0xF6 -> or_n()                  // OR N
            0xF7 -> rst(0x0030)             // RST 30H
            0xF8 -> ld_hl_sp_n()            // LD HL, SP+N
            0xF9 -> ld_sp_hl()              // LD SP, HL
            0xFA -> ld_a_nn()               // LD A, [NN]
            0xFB -> ei()                    // EI
            0xFE -> cp_n()                  // CP N
            0xFF -> rst(0x0038)             // RST 38H
            else -> throw IllegalArgumentException("Instruction not supported: ${opcode.toInt() and 0xFF}")
        }
    }

    // -------------------------------- //
    //            UTILS
    // -------------------------------- //

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
        clearFlag(FLAG_N)                                             // Set to 0
        updateFlag(FLAG_H, (val1 and 0xF) + (val2 and 0xF) > 0xF)     // Set (1) if there was a carry from the low nibble (the first 4 bits) during the operation.
        updateFlag(FLAG_C, result > 0xFF)                             // Set (1) if there was a carry during the addition (greater than 0xFF)
    }

    fun updateSubOperationFlags(val1: Int, val2: Int, result: Int){
        updateFlag(FLAG_Z, result == 0)                               // Activated (1) if the result of the operation is 0.
        updateFlag(FLAG_N, true)                                      // Activated (1) because a subtraction was performed.
        updateFlag(FLAG_H, (val1 and 0xF) < (val2 and 0xF))           // Set (1) if there was a carry from the low nibble (the first 4 bits) during the subtraction.
        updateFlag(FLAG_C, (val1 and 0xFF) < (val2 and 0xFF))         // Set (1) if there was a carry during the subtraction from the most significant bit (bit 7).
    }

    fun executeAndOperation(register: Byte){
        A = ((A.toInt() and register.toInt()) and 0xFF).toByte()
        updateFlag(FLAG_Z, (A.toInt() and 0xFF) == 0)
        clearFlag(FLAG_N)
        setFlag(FLAG_H)
        clearFlag(FLAG_C)
    }

    fun executeXorOrOperation(register: Byte, orOp: Boolean){
        A = if(orOp)
            ((A.toInt() or register.toInt()) and 0xFF).toByte()
        else
            ((A.toInt() xor register.toInt()) and 0xFF).toByte()

        updateFlag(FLAG_Z, (A.toInt() and 0xFF) == 0)
        clearFlag(FLAG_N)
        clearFlag(FLAG_H)
        clearFlag(FLAG_C)
    }

    fun executeCpOperation(register: Byte){
        val intA = A.toInt()
        val intRegister = register.toInt()

        val result = (intA - intRegister) and 0xFF

        updateFlag(FLAG_Z, result == 0)
        setFlag(FLAG_N)
        updateFlag(FLAG_H, (intA and 0xF) < (intRegister and 0xF))
        updateFlag(FLAG_C, intA < intRegister)
    }

    fun updateBitOperationFlags(result: Boolean){
        updateFlag(FLAG_Z, result)
        clearFlag(FLAG_N)
        setFlag(FLAG_H)
    }

    fun executeRetOperation(){
        val low = memory[SP].toInt() and 0xFF
        SP = (SP + 1) and 0xFFFF
        val high = memory[SP].toInt() and 0xFF
        SP = (SP + 1) and 0xFFFF

        PC = (high shl 8) or low
    }

    fun executeRstOperation(address: Int){
        val returnAddress = PC
        SP = (SP - 1) and 0xFFFF
        memory[SP] = (returnAddress ushr 8).toByte()    // high
        SP = (SP - 1) and 0xFFFF
        memory[SP] = (returnAddress and 0xFF).toByte()  // low

        PC = address and 0xFFFF
    }

    fun executeCallOperation(address: Int){
        SP = (SP - 1) and 0xFFFF
        memory[SP] = (PC ushr 8).toByte()   // high
        SP = (SP - 1) and 0xFFFF
        memory[SP] = (PC and 0xFF).toByte() // low

        PC = address
    }

    fun uccu_flags(result: Byte, carry: Int){
        updateFlag(FLAG_Z, result == 0.toByte())
        clearFlag(FLAG_N)
        clearFlag(FLAG_H)
        updateFlag(FLAG_C, carry == 1)
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
        // TODO
        //cpu_halted = true
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
        cpu_halted = true
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
        val result = intA - intA        // Should be 0
        A = (result and 0xFF).toByte()  // A = 0x0

        updateSubOperationFlags(intA, intA, result)

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

    fun sbc_a_hl(): Int{
        val address = get_16bit_address(H, L)
        val intMem = memory[address].toInt()
        val carry = if (flagIsSet(FLAG_C)) 1 else 0
        val intA = A.toInt()
        val result = intA - (intMem + carry)
        A = (result and 0xFF).toByte()

        updateSubOperationFlags(intA, intMem + carry, result)

        return CYCLES_8
    }

    fun sbc_a_a(): Int{
        val carry = if (flagIsSet(FLAG_C)) 1 else 0
        val intA = A.toInt()
        val result = intA - (intA + carry)
        A = (result and 0xFF).toByte()

        updateSubOperationFlags(intA, intA + carry, result)

        return CYCLES_4
    }

    fun and_b(): Int{
        executeAndOperation(B)
        return CYCLES_4
    }

    fun and_c(): Int{
        executeAndOperation(C)
        return CYCLES_4
    }

    fun and_d(): Int{
        executeAndOperation(D)
        return CYCLES_4
    }

    fun and_e(): Int{
        executeAndOperation(E)
        return CYCLES_4
    }

    fun and_h(): Int{
        executeAndOperation(H)
        return CYCLES_4
    }

    fun and_l(): Int{
        executeAndOperation(L)
        return CYCLES_4
    }

    fun and_hl(): Int{
        val hl = get_16bit_address(H,L)
        executeAndOperation(memory[hl])
        return CYCLES_8
    }

    fun and_a(): Int{
        executeAndOperation(A)
        return CYCLES_4
    }

    fun xor_b(): Int{
        executeXorOrOperation(B, false)
        return CYCLES_4
    }

    fun xor_c(): Int{
        executeXorOrOperation(C, false)
        return CYCLES_4
    }

    fun xor_d(): Int{
        executeXorOrOperation(D, false)
        return CYCLES_4
    }

    fun xor_e(): Int{
        executeXorOrOperation(E, false)
        return CYCLES_4
    }

    fun xor_h(): Int{
        executeXorOrOperation(H, false)
        return CYCLES_4
    }

    fun xor_l(): Int{
        executeXorOrOperation(L, false)
        return CYCLES_4
    }

    fun xor_hl(): Int{
        val hl = get_16bit_address(H,L)
        executeXorOrOperation(memory[hl], false)
        return CYCLES_8
    }

    fun xor_a(): Int{
        executeXorOrOperation(A, false)
        return CYCLES_4
    }

    fun or_b(): Int{
        executeXorOrOperation(B, true)
        return CYCLES_4
    }

    fun or_c(): Int{
        executeXorOrOperation(C, true)
        return CYCLES_4
    }

    fun or_d(): Int{
        executeXorOrOperation(D, true)
        return CYCLES_4
    }

    fun or_e(): Int{
        executeXorOrOperation(E, true)
        return CYCLES_4
    }

    fun or_h(): Int{
        executeXorOrOperation(H, true)
        return CYCLES_4
    }

    fun or_l(): Int{
        executeXorOrOperation(L, true)
        return CYCLES_4
    }

    fun or_hl(): Int{
        val hl = get_16bit_address(H,L)
        executeXorOrOperation(memory[hl], true)
        return CYCLES_8
    }

    fun or_a(): Int{
        executeXorOrOperation(A, true)
        return CYCLES_4
    }

    fun cp_b(): Int{
        executeCpOperation(B)
        return CYCLES_4
    }

    fun cp_c(): Int{
        executeCpOperation(C)
        return CYCLES_4
    }

    fun cp_d(): Int{
        executeCpOperation(D)
        return CYCLES_4
    }

    fun cp_e(): Int{
        executeCpOperation(E)
        return CYCLES_4
    }

    fun cp_h(): Int{
        executeCpOperation(H)
        return CYCLES_4
    }

    fun cp_l(): Int{
        executeCpOperation(L)
        return CYCLES_4
    }

    fun cp_hl(): Int{
        val hl = get_16bit_address(H,L)
        executeCpOperation(memory[hl])
        return CYCLES_8
    }

    fun cp_a(): Int{
        executeCpOperation(A)
        return CYCLES_4
    }

    fun ret_nz(): Int{
        return if (!flagIsSet(FLAG_Z)) {
            executeRetOperation()
            CYCLES_20
        } else {
            CYCLES_8
        }
    }

    fun pop_bc(): Int{

        C = memory[SP]
        SP = (SP + 1) and 0xFFFF
        B = memory[SP]
        SP = (SP + 1) and 0xFFFF

        return CYCLES_12
    }

    fun jp_nz_nn(): Int{
        val address = fetch16()

        if (!flagIsSet(FLAG_Z)) {
            PC = address
            return CYCLES_16
        }

        return CYCLES_12
    }

    fun jp_nn(): Int{
        val address = fetch16()
        PC = address
        return CYCLES_16
    }

    fun call_nz_nn(): Int{
        val address = fetch16()

        if (!flagIsSet(FLAG_Z)) {
            executeCallOperation(address)
            return CYCLES_24
        }

        return CYCLES_12
    }

    fun push_bc(): Int{
        SP = (SP - 1) and 0xFFFF
        memory[SP] = B              // high
        SP = (SP - 1) and 0xFFFF
        memory[SP] = C              // low

        return CYCLES_16
    }

    fun add_a_n(): Int{
        val n8 = fetch()
        A = ((A.toInt() + n8.toInt()) and 0xFF).toByte()

        return CYCLES_8
    }

    fun rst(address: Int): Int{
        executeRstOperation(address and 0xFFFF)
        return CYCLES_16
    }

    fun ret_z(): Int{
        return if (flagIsSet(FLAG_Z)) {
            executeRetOperation()
            CYCLES_20
        } else {
            CYCLES_8
        }
    }

    fun ret(): Int{
        executeRetOperation()
        return CYCLES_16
    }

    fun jp_z_nn(): Int{
        val address = fetch16()

        if (flagIsSet(FLAG_Z)) {
            PC = address
            return CYCLES_16
        }

        return CYCLES_12
    }

    fun prefix_cb(): Int{

        val opcode = fetch()

        val cycles = when (opcode.toInt() and 0xFF) {
            0x00 -> rlc_b()                         // RLC B
            0x01 -> rlc_c()                         // RLC C
            0x02 -> rlc_d()                         // RLC D
            0x03 -> rlc_e()                         // RLC E
            0x04 -> rlc_h()                         // RLC H
            0x05 -> rlc_l()                         // RLC L
            0x06 -> rlc_hl()                        // RLC [HL]
            0x07 -> rlc_a()                         // RLC A
            0x08 -> rrc_b()                         // RRC B
            0x09 -> rrc_c()                         // RRC C
            0x0A -> rrc_d()                         // RRC D
            0x0B -> rrc_e()                         // RRC E
            0x0C -> rrc_h()                         // RRC H
            0x0D -> rrc_l()                         // RRC L
            0x0E -> rrc_hl()                        // RRC [HL]
            0x0F -> rrc_a()                         // RRC A
            0x10 -> rl_b()                          // RL B
            0x11 -> rl_c()                          // RL C
            0x12 -> rl_d()                          // RL D
            0x13 -> rl_e()                          // RL E
            0x14 -> rl_h()                          // RL H
            0x15 -> rl_l()                          // RL L
            0x16 -> rl_hl()                         // RL [HL]
            0x17 -> rl_a()                          // RL A
            0x18 -> rr_b()                          // RR B
            0x19 -> rr_c()                          // RR C
            0x1A -> rr_d()                          // RR D
            0x1B -> rr_e()                          // RR E
            0x1C -> rr_h()                          // RR H
            0x1D -> rr_l()                          // RR L
            0x1E -> rr_hl()                         // RR [HL]
            0x1F -> rr_a()                          // RR A
            0x20 -> sla_b()                         // SLA B
            0x21 -> sla_c()                         // SLA C
            0x22 -> sla_d()                         // SLA D
            0x23 -> sla_e()                         // SLA E
            0x24 -> sla_h()                         // SLA H
            0x25 -> sla_l()                         // SLA L
            0x26 -> sla_hl()                        // SLA [HL]
            0x27 -> sla_a()                         // SLA A
            0x28 -> sra_b()                         // SRA B
            0x29 -> sra_c()                         // SRA C
            0x2A -> sra_d()                         // SRA D
            0x2B -> sra_e()                         // SRA E
            0x2C -> sra_h()                         // SRA H
            0x2D -> sra_l()                         // SRA L
            0x2E -> sra_hl()                        // SRA [HL]
            0x2F -> sra_a()                         // SRA A
            0x30 -> swap_b()                        // SWAP B
            0x31 -> swap_c()                        // SWAP C
            0x32 -> swap_d()                        // SWAP D
            0x33 -> swap_e()                        // SWAP E
            0x34 -> swap_h()                        // SWAP H
            0x35 -> swap_l()                        // SWAP L
            0x36 -> swap_hl()                       // SWAP [HL]
            0x37 -> swap_a()                        // SWAP A
            0x38 -> srl_b()                         // SRL B
            0x39 -> srl_c()                         // SRL C
            0x3A -> srl_d()                         // SRL D
            0x3B -> srl_e()                         // SRL E
            0x3C -> srl_h()                         // SRL H
            0x3D -> srl_l()                         // SRL L
            0x3E -> srl_hl()                        // SRL [HL]
            0x3F -> srl_a()                         // SRL A
            0x40 -> bit_operation(B_RG_N, BIT_0)    // BIT 0, B
            0x41 -> bit_operation(C_RG_N, BIT_0)    // BIT 0, C
            0x42 -> bit_operation(D_RG_N, BIT_0)    // BIT 0, D
            0x43 -> bit_operation(E_RG_N, BIT_0)    // BIT 0, E
            0x44 -> bit_operation(H_RG_N, BIT_0)    // BIT 0, H
            0x45 -> bit_operation(L_RG_N, BIT_0)    // BIT 0, L
            0x46 -> bit_operation(HL_RG_N, BIT_0)   // BIT 0, [HL]
            0x47 -> bit_operation(A_RG_N, BIT_0)    // BIT 0, A
            0x48 -> bit_operation(B_RG_N, BIT_1)    // BIT 1, B
            0x49 -> bit_operation(C_RG_N, BIT_1)    // BIT 1, C
            0x4A -> bit_operation(D_RG_N, BIT_1)    // BIT 1, D
            0x4B -> bit_operation(E_RG_N, BIT_1)    // BIT 1, E
            0x4C -> bit_operation(H_RG_N, BIT_1)    // BIT 1, H
            0x4D -> bit_operation(L_RG_N, BIT_1)    // BIT 1, L
            0x4E -> bit_operation(HL_RG_N, BIT_1)   // BIT 1, [HL]
            0x4F -> bit_operation(A_RG_N, BIT_1)    // BIT 1, A
            0x50 -> bit_operation(B_RG_N, BIT_2)    // BIT 2, B
            0x51 -> bit_operation(C_RG_N, BIT_2)    // BIT 2, C
            0x52 -> bit_operation(D_RG_N, BIT_2)    // BIT 2, D
            0x53 -> bit_operation(E_RG_N, BIT_2)    // BIT 2, E
            0x54 -> bit_operation(H_RG_N, BIT_2)    // BIT 2, H
            0x55 -> bit_operation(L_RG_N, BIT_2)    // BIT 2, L
            0x56 -> bit_operation(HL_RG_N, BIT_2)   // BIT 2, [HL]
            0x57 -> bit_operation(A_RG_N, BIT_2)    // BIT 2, A
            0x58 -> bit_operation(B_RG_N, BIT_3)    // BIT 3, B
            0x59 -> bit_operation(C_RG_N, BIT_3)    // BIT 3, C
            0x5A -> bit_operation(D_RG_N, BIT_3)    // BIT 3, D
            0x5B -> bit_operation(E_RG_N, BIT_3)    // BIT 3, E
            0x5C -> bit_operation(H_RG_N, BIT_3)    // BIT 3, H
            0x5D -> bit_operation(L_RG_N, BIT_3)    // BIT 3, L
            0x5E -> bit_operation(HL_RG_N, BIT_3)   // BIT 3, [HL]
            0x5F -> bit_operation(A_RG_N, BIT_3)    // BIT 3, A
            0x60 -> bit_operation(B_RG_N, BIT_4)    // BIT 4, B
            0x61 -> bit_operation(C_RG_N, BIT_4)    // BIT 4, C
            0x62 -> bit_operation(D_RG_N, BIT_4)    // BIT 4, D
            0x63 -> bit_operation(E_RG_N, BIT_4)    // BIT 4, E
            0x64 -> bit_operation(H_RG_N, BIT_4)    // BIT 4, H
            0x65 -> bit_operation(L_RG_N, BIT_4)    // BIT 4, L
            0x66 -> bit_operation(HL_RG_N, BIT_4)   // BIT 4, [HL]
            0x67 -> bit_operation(A_RG_N, BIT_4)    // BIT 4, A
            0x68 -> bit_operation(B_RG_N, BIT_5)    // BIT 5, B
            0x69 -> bit_operation(C_RG_N, BIT_5)    // BIT 5, C
            0x6A -> bit_operation(D_RG_N, BIT_5)    // BIT 5, D
            0x6B -> bit_operation(E_RG_N, BIT_5)    // BIT 5, E
            0x6C -> bit_operation(H_RG_N, BIT_5)    // BIT 5, H
            0x6D -> bit_operation(L_RG_N, BIT_5)    // BIT 5, L
            0x6E -> bit_operation(HL_RG_N, BIT_5)   // BIT 5, [HL]
            0x6F -> bit_operation(A_RG_N, BIT_5)    // BIT 5, A
            0x70 -> bit_operation(B_RG_N, BIT_6)    // BIT 6, B
            0x71 -> bit_operation(C_RG_N, BIT_6)    // BIT 6, C
            0x72 -> bit_operation(D_RG_N, BIT_6)    // BIT 6, D
            0x73 -> bit_operation(E_RG_N, BIT_6)    // BIT 6, E
            0x74 -> bit_operation(H_RG_N, BIT_6)    // BIT 6, H
            0x75 -> bit_operation(L_RG_N, BIT_6)    // BIT 6, L
            0x76 -> bit_operation(HL_RG_N, BIT_6)   // BIT 6, [HL]
            0x77 -> bit_operation(A_RG_N, BIT_6)    // BIT 6, A
            0x78 -> bit_operation(B_RG_N, BIT_7)    // BIT 7, B
            0x79 -> bit_operation(C_RG_N, BIT_7)    // BIT 7, C
            0x7A -> bit_operation(D_RG_N, BIT_7)    // BIT 7, D
            0x7B -> bit_operation(E_RG_N, BIT_7)    // BIT 7, E
            0x7C -> bit_operation(H_RG_N, BIT_7)    // BIT 7, H
            0x7D -> bit_operation(L_RG_N, BIT_7)    // BIT 7, L
            0x7E -> bit_operation(HL_RG_N, BIT_7)   // BIT 7, [HL]
            0x7F -> bit_operation(A_RG_N, BIT_7)    // BIT 7, A
            0x80 -> res_operation(B_RG_N, BIT_0)    // RES 0, B
            0x81 -> res_operation(C_RG_N, BIT_0)    // RES 0, C
            0x82 -> res_operation(D_RG_N, BIT_0)    // RES 0, D
            0x83 -> res_operation(E_RG_N, BIT_0)    // RES 0, E
            0x84 -> res_operation(H_RG_N, BIT_0)    // RES 0, H
            0x85 -> res_operation(L_RG_N, BIT_0)    // RES 0, L
            0x86 -> res_operation(HL_RG_N, BIT_0)   // RES 0, [HL]
            0x87 -> res_operation(A_RG_N, BIT_0)    // RES 0, A
            0x88 -> res_operation(B_RG_N, BIT_1)    // RES 1, B
            0x89 -> res_operation(C_RG_N, BIT_1)    // RES 1, C
            0x8A -> res_operation(D_RG_N, BIT_1)    // RES 1, D
            0x8B -> res_operation(E_RG_N, BIT_1)    // RES 1, E
            0x8C -> res_operation(H_RG_N, BIT_1)    // RES 1, H
            0x8D -> res_operation(L_RG_N, BIT_1)    // RES 1, L
            0x8E -> res_operation(HL_RG_N, BIT_1)   // RES 1, [HL]
            0x8F -> res_operation(A_RG_N, BIT_1)    // RES 1, A
            0x90 -> res_operation(B_RG_N, BIT_2)    // RES 2, B
            0x91 -> res_operation(C_RG_N, BIT_2)    // RES 2, C
            0x92 -> res_operation(D_RG_N, BIT_2)    // RES 2, D
            0x93 -> res_operation(E_RG_N, BIT_2)    // RES 2, E
            0x94 -> res_operation(H_RG_N, BIT_2)    // RES 2, H
            0x95 -> res_operation(L_RG_N, BIT_2)    // RES 2, L
            0x96 -> res_operation(HL_RG_N, BIT_2)   // RES 2, [HL]
            0x97 -> res_operation(A_RG_N, BIT_2)    // RES 2, A
            0x98 -> res_operation(B_RG_N, BIT_3)    // RES 3, B
            0x99 -> res_operation(C_RG_N, BIT_3)    // RES 3, C
            0x9A -> res_operation(D_RG_N, BIT_3)    // RES 3, D
            0x9B -> res_operation(E_RG_N, BIT_3)    // RES 3, E
            0x9C -> res_operation(H_RG_N, BIT_3)    // RES 3, H
            0x9D -> res_operation(L_RG_N, BIT_3)    // RES 3, L
            0x9E -> res_operation(HL_RG_N, BIT_3)   // RES 3, [HL]
            0x9F -> res_operation(A_RG_N, BIT_3)    // RES 3, A
            0xA0 -> res_operation(B_RG_N, BIT_4)    // RES 4, B
            0xA1 -> res_operation(C_RG_N, BIT_4)    // RES 4, C
            0xA2 -> res_operation(D_RG_N, BIT_4)    // RES 4, D
            0xA3 -> res_operation(E_RG_N, BIT_4)    // RES 4, E
            0xA4 -> res_operation(H_RG_N, BIT_4)    // RES 4, H
            0xA5 -> res_operation(L_RG_N, BIT_4)    // RES 4, L
            0xA6 -> res_operation(HL_RG_N, BIT_4)   // RES 4, [HL]
            0xA7 -> res_operation(A_RG_N, BIT_4)    // RES 4, A
            0xA8 -> res_operation(B_RG_N, BIT_5)    // RES 5, B
            0xA9 -> res_operation(C_RG_N, BIT_5)    // RES 5, C
            0xAA -> res_operation(D_RG_N, BIT_5)    // RES 5, D
            0xAB -> res_operation(E_RG_N, BIT_5)    // RES 5, E
            0xAC -> res_operation(H_RG_N, BIT_5)    // RES 5, H
            0xAD -> res_operation(L_RG_N, BIT_5)    // RES 5, L
            0xAE -> res_operation(HL_RG_N, BIT_5)   // RES 5, [HL]
            0xAF -> res_operation(A_RG_N, BIT_5)    // RES 5, A
            0xB0 -> res_operation(B_RG_N, BIT_6)    // RES 6, B
            0xB1 -> res_operation(C_RG_N, BIT_6)    // RES 6, C
            0xB2 -> res_operation(D_RG_N, BIT_6)    // RES 6, D
            0xB3 -> res_operation(E_RG_N, BIT_6)    // RES 6, E
            0xB4 -> res_operation(H_RG_N, BIT_6)    // RES 6, H
            0xB5 -> res_operation(L_RG_N, BIT_6)    // RES 6, L
            0xB6 -> res_operation(HL_RG_N, BIT_6)   // RES 6, [HL]
            0xB7 -> res_operation(A_RG_N, BIT_6)    // RES 6, A
            0xB8 -> res_operation(B_RG_N, BIT_7)    // RES 7, B
            0xB9 -> res_operation(C_RG_N, BIT_7)    // RES 7, C
            0xBA -> res_operation(D_RG_N, BIT_7)    // RES 7, D
            0xBB -> res_operation(E_RG_N, BIT_7)    // RES 7, E
            0xBC -> res_operation(H_RG_N, BIT_7)    // RES 7, H
            0xBD -> res_operation(L_RG_N, BIT_7)    // RES 7, L
            0xBE -> res_operation(HL_RG_N, BIT_7)   // RES 7, [HL]
            0xBF -> res_operation(A_RG_N, BIT_7)    // RES 7, A
            0xC0 -> set_operation(B_RG_N, BIT_0)    // SET 0, B
            0xC1 -> set_operation(C_RG_N, BIT_0)    // SET 0, C
            0xC2 -> set_operation(D_RG_N, BIT_0)    // SET 0, D
            0xC3 -> set_operation(E_RG_N, BIT_0)    // SET 0, E
            0xC4 -> set_operation(H_RG_N, BIT_0)    // SET 0, H
            0xC5 -> set_operation(L_RG_N, BIT_0)    // SET 0, L
            0xC6 -> set_operation(HL_RG_N, BIT_0)   // SET 0, [HL]
            0xC7 -> set_operation(A_RG_N, BIT_0)    // SET 0, A
            0xC8 -> set_operation(B_RG_N, BIT_1)    // SET 1, B
            0xC9 -> set_operation(C_RG_N, BIT_1)    // SET 1, C
            0xCA -> set_operation(D_RG_N, BIT_1)    // SET 1, D
            0xCB -> set_operation(E_RG_N, BIT_1)    // SET 1, E
            0xCC -> set_operation(H_RG_N, BIT_1)    // SET 1, H
            0xCD -> set_operation(L_RG_N, BIT_1)    // SET 1, L
            0xCE -> set_operation(HL_RG_N, BIT_1)   // SET 1, [HL]
            0xCF -> set_operation(A_RG_N, BIT_1)    // SET 1, A
            0xD0 -> set_operation(B_RG_N, BIT_2)    // SET 2, B
            0xD1 -> set_operation(C_RG_N, BIT_2)    // SET 2, C
            0xD2 -> set_operation(D_RG_N, BIT_2)    // SET 2, D
            0xD3 -> set_operation(E_RG_N, BIT_2)    // SET 2, E
            0xD4 -> set_operation(H_RG_N, BIT_2)    // SET 2, H
            0xD5 -> set_operation(L_RG_N, BIT_2)    // SET 2, L
            0xD6 -> set_operation(HL_RG_N, BIT_2)   // SET 2, [HL]
            0xD7 -> set_operation(A_RG_N, BIT_2)    // SET 2, A
            0xD8 -> set_operation(B_RG_N, BIT_3)    // SET 3, B
            0xD9 -> set_operation(C_RG_N, BIT_3)    // SET 3, C
            0xDA -> set_operation(D_RG_N, BIT_3)    // SET 3, D
            0xDB -> set_operation(E_RG_N, BIT_3)    // SET 3, E
            0xDC -> set_operation(H_RG_N, BIT_3)    // SET 3, H
            0xDD -> set_operation(L_RG_N, BIT_3)    // SET 3, L
            0xDE -> set_operation(HL_RG_N, BIT_3)   // SET 3, [HL]
            0xDF -> set_operation(A_RG_N, BIT_3)    // SET 3, A
            0xE0 -> set_operation(B_RG_N, BIT_4)    // SET 4, B
            0xE1 -> set_operation(C_RG_N, BIT_4)    // SET 4, C
            0xE2 -> set_operation(D_RG_N, BIT_4)    // SET 4, D
            0xE3 -> set_operation(E_RG_N, BIT_4)    // SET 4, E
            0xE4 -> set_operation(H_RG_N, BIT_4)    // SET 4, H
            0xE5 -> set_operation(L_RG_N, BIT_4)    // SET 4, L
            0xE6 -> set_operation(HL_RG_N, BIT_4)   // SET 4, [HL]
            0xE7 -> set_operation(A_RG_N, BIT_4)    // SET 4, A
            0xE8 -> set_operation(B_RG_N, BIT_5)    // SET 5, B
            0xE9 -> set_operation(C_RG_N, BIT_5)    // SET 5, C
            0xEA -> set_operation(D_RG_N, BIT_5)    // SET 5, D
            0xEB -> set_operation(E_RG_N, BIT_5)    // SET 5, E
            0xEC -> set_operation(H_RG_N, BIT_5)    // SET 5, H
            0xED -> set_operation(L_RG_N, BIT_5)    // SET 5, L
            0xEE -> set_operation(HL_RG_N, BIT_5)   // SET 5, [HL]
            0xEF -> set_operation(A_RG_N, BIT_5)    // SET 5, A
            0xF0 -> set_operation(B_RG_N, BIT_6)    // SET 6, B
            0xF1 -> set_operation(C_RG_N, BIT_6)    // SET 6, C
            0xF2 -> set_operation(D_RG_N, BIT_6)    // SET 6, D
            0xF3 -> set_operation(E_RG_N, BIT_6)    // SET 6, E
            0xF4 -> set_operation(H_RG_N, BIT_6)    // SET 6, H
            0xF5 -> set_operation(L_RG_N, BIT_6)    // SET 6, L
            0xF6 -> set_operation(HL_RG_N, BIT_6)   // SET 6, [HL]
            0xF7 -> set_operation(A_RG_N, BIT_6)    // SET 6, A
            0xF8 -> set_operation(B_RG_N, BIT_7)    // SET 7, B
            0xF9 -> set_operation(C_RG_N, BIT_7)    // SET 7, C
            0xFA -> set_operation(D_RG_N, BIT_7)    // SET 7, D
            0xFB -> set_operation(E_RG_N, BIT_7)    // SET 7, E
            0xFC -> set_operation(H_RG_N, BIT_7)    // SET 7, H
            0xFD -> set_operation(L_RG_N, BIT_7)    // SET 7, L
            0xFE -> set_operation(HL_RG_N, BIT_7)   // SET 7, [HL]
            0xFF -> set_operation(A_RG_N, BIT_7)    // SET 7, A
            else -> throw IllegalStateException("Opcode CB $opcode not implemented")
        }

        return cycles
    }

    fun call_z_nn(): Int{
        val address = fetch16()

        if (flagIsSet(FLAG_Z)) {
            executeCallOperation(address)
            return CYCLES_24
        }

        return CYCLES_12
    }

    fun call(): Int{
        val address = fetch16()
        executeCallOperation(address)
        return CYCLES_24
    }

    fun adc_a_n(): Int{
        val carry = if (flagIsSet(FLAG_C)) 1 else 0
        val intA = A.toInt()
        val intN = fetch()
        val result = intA + (intN + carry)
        A = (result and 0xFF).toByte()

        updateAddOperationFlags(intA, intN + carry, result)

        return CYCLES_8
    }

    fun ret_nc(): Int{
        return if (!flagIsSet(FLAG_C)) {
            executeRetOperation()
            CYCLES_20
        } else {
            CYCLES_8
        }
    }

    fun pop_de(): Int{

        E = memory[SP]
        SP = (SP + 1) and 0xFFFF
        D = memory[SP]
        SP = (SP + 1) and 0xFFFF

        return CYCLES_12
    }

    fun jp_nc_nn(): Int{
        val address = fetch16()

        if (!flagIsSet(FLAG_C)) {
            PC = address
            return CYCLES_16
        }

        return CYCLES_12
    }

    fun call_nc_nn(): Int{
        val address = fetch16()

        if (!flagIsSet(FLAG_C)) {
            executeCallOperation(address)
            return CYCLES_24
        }

        return CYCLES_12
    }

    fun push_de(): Int{
        SP = (SP - 1) and 0xFFFF
        memory[SP] = D              // high
        SP = (SP - 1) and 0xFFFF
        memory[SP] = E              // low

        return CYCLES_16
    }

    fun sub_n(): Int{
        val byte = fetch().toInt()
        val intA = A.toInt()
        val result = intA - byte
        A = (result and 0xFF).toByte()

        updateSubOperationFlags(intA, byte, result)

        return CYCLES_8
    }

    fun ret_c(): Int{
        return if (flagIsSet(FLAG_C)) {
            executeRetOperation()
            CYCLES_20
        } else {
            CYCLES_8
        }
    }

    fun reti(): Int{
        executeRetOperation()
        Interrupt.enableInterrupts(true)
        return CYCLES_16
    }

    fun jp_c_nn(): Int{
        val address = fetch16()

        if (flagIsSet(FLAG_C)) {
            PC = address
            return CYCLES_16
        }

        return CYCLES_12
    }

    fun call_c_nn(): Int{
        val address = fetch16()

        if (flagIsSet(FLAG_C)) {
            executeCallOperation(address)
            return CYCLES_24
        }

        return CYCLES_12
    }

    fun sbc_a_n(): Int{
        val byte = fetch().toInt()
        val carry = if (flagIsSet(FLAG_C)) 1 else 0
        val intA = A.toInt()
        val result = intA - (byte + carry)
        A = (result and 0xFF).toByte()

        updateSubOperationFlags(intA, byte + carry, result)

        return CYCLES_4
    }

    fun ldh_n_a(): Int{

        val byte = fetch()
        val address = (0xFF00 + byte) and 0xFFFF

        memory[address] = A

        return CYCLES_12
    }

    fun pop_hl(): Int{

        L = memory[SP]
        SP = (SP + 1) and 0xFFFF
        H = memory[SP]
        SP = (SP + 1) and 0xFFFF

        return CYCLES_12
    }

    fun ld_cn_a(): Int{
        val address = (0xFF00 + C) and 0xFFFF
        memory[address] = A
        return CYCLES_8
    }

    fun push_hl(): Int{
        SP = (SP - 1) and 0xFFFF
        memory[SP] = H              // high
        SP = (SP - 1) and 0xFFFF
        memory[SP] = L              // low

        return CYCLES_16
    }

    fun and_n(): Int{
        val byte = fetch()
        executeAndOperation(byte)
        return CYCLES_8
    }

    fun add_sp_n(): Int{
        val byte = fetch().toInt()
        val oldSP = SP
        SP = (SP + byte) and 0xFFFF

        clearFlag(FLAG_Z)
        clearFlag(FLAG_N)
        updateFlag(FLAG_H, (oldSP and 0xF) + (byte and 0xF) > 0xF)
        updateFlag(FLAG_C, SP > 0xFF)

        return CYCLES_16
    }

    fun jp_hl(): Int{
        PC = get_16bit_address(H,L)
        return CYCLES_4
    }

    fun ld_nn_a(): Int{
        val address = fetch16()
        memory[address] = A
        return CYCLES_16
    }

    fun xor_n(): Int{
        executeXorOrOperation(fetch(), false)
        return CYCLES_8
    }

    fun ldh_a_n(): Int{

        val byte = fetch()
        val address = (0xFF00 + byte) and 0xFFFF

        A = memory[address]

        return CYCLES_12
    }

    fun pop_af(): Int{

        F = memory[SP]
        SP = (SP + 1) and 0xFFFF
        A = memory[SP]
        SP = (SP + 1) and 0xFFFF

        return CYCLES_12
    }

    fun ld_a_cn(): Int{
        val address = (0xFF00 + C) and 0xFFFF
        A = memory[address]
        return CYCLES_8
    }

    fun di(): Int{
        Interrupt.enableInterrupts(false)
        return CYCLES_4
    }

    fun push_af(): Int{

        SP = (SP - 1) and 0xFFFF
        memory[SP] = A              // high
        SP = (SP - 1) and 0xFFFF
        memory[SP] = F              // low

        return CYCLES_16
    }

    fun or_n(): Int{
        executeXorOrOperation(fetch(), true)
        return CYCLES_8
    }

    fun ld_hl_sp_n(): Int{

        val n = fetch().toInt()
        val sp = SP
        val result = sp + n

        H = ((result ushr 8) and 0xFF).toByte()
        L = (result and 0xFF).toByte()

        clearFlag(FLAG_Z)
        clearFlag(FLAG_N)
        updateFlag(FLAG_H, ((sp and 0xF) + (n and 0xF)) > 0xF)
        updateFlag(FLAG_C, ((sp and 0xFF) + (n and 0xFF)) > 0xFF)

        return CYCLES_12
    }

    fun ld_sp_hl(): Int{

        val intH = H.toInt()
        val intL = L.toInt()

        SP = (intH shl 8) or intL

        return CYCLES_8
    }

    fun ld_a_nn(): Int{
        val address = fetch16()
        A = memory[address]
        return CYCLES_16
    }

    fun ei(): Int{
        Interrupt.enableInterrupts(true)
        return CYCLES_4
    }

    fun cp_n(): Int{
        executeCpOperation(fetch())
        return CYCLES_8
    }

    // -------------------------------- //
    //        EXTENDED OPCODES
    // -------------------------------- //

    fun rlc_b(): Int{
        val carry = (B.toInt() ushr 7) and 0x1
        B = ((B.toInt() shl 1) or carry).toByte()

        uccu_flags(B, carry)

        return CYCLES_8
    }

    fun rlc_c(): Int{
        val carry = (C.toInt() ushr 7) and 0x1
        C = ((C.toInt() shl 1) or carry).toByte()

        uccu_flags(C, carry)

        return CYCLES_8
    }

    fun rlc_d(): Int{
        val carry = (D.toInt() ushr 7) and 0x1
        D = ((D.toInt() shl 1) or carry).toByte()

        uccu_flags(D, carry)

        return CYCLES_8
    }

    fun rlc_e(): Int{
        val carry = (E.toInt() ushr 7) and 0x1
        E = ((E.toInt() shl 1) or carry).toByte()

        uccu_flags(E, carry)

        return CYCLES_8
    }

    fun rlc_h(): Int{
        val carry = (H.toInt() ushr 7) and 0x1
        H = ((C.toInt() shl 1) or carry).toByte()

        uccu_flags(H, carry)

        return CYCLES_8
    }

    fun rlc_l(): Int{
        val carry = (L.toInt() ushr 7) and 0x1
        L = ((L.toInt() shl 1) or carry).toByte()

        uccu_flags(L, carry)

        return CYCLES_8
    }

    fun rlc_hl(): Int{
        val address = get_16bit_address(H, L)
        val carry = (memory[address].toInt() ushr 7) and 0x1
        memory[address] = ((memory[address].toInt() shl 1) or carry).toByte()

        uccu_flags(memory[address], carry)

        return CYCLES_16
    }

    fun rlc_a(): Int{
        val carry = (A.toInt() ushr 7) and 0x1
        A = ((A.toInt() shl 1) or carry).toByte()

        uccu_flags(A, carry)

        return CYCLES_8
    }

    fun rrc_b(): Int{

        val carry = B.toInt() and 0x1
        B = ((B.toInt() shr 1) or (carry shl 7)).toByte()

        uccu_flags(B, carry)

        return CYCLES_8
    }

    fun rrc_c(): Int{

        val carry = C.toInt() and 0x1
        C = ((C.toInt() shr 1) or (carry shl 7)).toByte()

        uccu_flags(C, carry)

        return CYCLES_8
    }

    fun rrc_d(): Int{

        val carry = D.toInt() and 0x1
        D = ((D.toInt() shr 1) or (carry shl 7)).toByte()

        uccu_flags(D, carry)

        return CYCLES_8
    }

    fun rrc_e(): Int{

        val carry = E.toInt() and 0x1
        E = ((E.toInt() shr 1) or (carry shl 7)).toByte()

        uccu_flags(E, carry)

        return CYCLES_8
    }

    fun rrc_h(): Int{

        val carry = H.toInt() and 0x1
        H = ((H.toInt() shr 1) or (carry shl 7)).toByte()

        uccu_flags(H, carry)

        return CYCLES_8
    }

    fun rrc_l(): Int{

        val carry = L.toInt() and 0x1
        L = ((L.toInt() shr 1) or (carry shl 7)).toByte()

        uccu_flags(L, carry)

        return CYCLES_8
    }

    fun rrc_hl(): Int{

        val address = get_16bit_address(H, L)
        val carry = memory[address].toInt() and 0x1
        memory[address] = ((memory[address].toInt() shr 1) or (carry shl 7)).toByte()

        uccu_flags(memory[address], carry)

        return CYCLES_16
    }

    fun rrc_a(): Int{

        val carry = A.toInt() and 0x1
        A = ((A.toInt() shr 1) or (carry shl 7)).toByte()

        uccu_flags(A, carry)

        return CYCLES_8
    }

    fun rl_b(): Int{

        val oldCarry = if (flagIsSet(FLAG_C)) 1 else 0
        val newCarry = (B.toInt() ushr 7) and 0x1

        B = ((B.toInt() shl 1) or oldCarry).toByte()

        uccu_flags(B, newCarry)

        return CYCLES_8
    }

    fun rl_c(): Int{

        val oldCarry = if (flagIsSet(FLAG_C)) 1 else 0
        val newCarry = (C.toInt() ushr 7) and 0x1

        C = ((C.toInt() shl 1) or oldCarry).toByte()

        uccu_flags(C, newCarry)

        return CYCLES_8
    }

    fun rl_d(): Int{

        val oldCarry = if (flagIsSet(FLAG_C)) 1 else 0
        val newCarry = (D.toInt() ushr 7) and 0x1

        D = ((D.toInt() shl 1) or oldCarry).toByte()

        uccu_flags(D, newCarry)

        return CYCLES_8
    }

    fun rl_e(): Int{

        val oldCarry = if (flagIsSet(FLAG_C)) 1 else 0
        val newCarry = (E.toInt() ushr 7) and 0x1

        E = ((E.toInt() shl 1) or oldCarry).toByte()

        uccu_flags(E, newCarry)

        return CYCLES_8
    }

    fun rl_h(): Int{

        val oldCarry = if (flagIsSet(FLAG_C)) 1 else 0
        val newCarry = (H.toInt() ushr 7) and 0x1

        H = ((H.toInt() shl 1) or oldCarry).toByte()

        uccu_flags(H, newCarry)

        return CYCLES_8
    }

    fun rl_l(): Int{

        val oldCarry = if (flagIsSet(FLAG_C)) 1 else 0
        val newCarry = (L.toInt() ushr 7) and 0x1

        L = ((L.toInt() shl 1) or oldCarry).toByte()

        uccu_flags(L, newCarry)

        return CYCLES_8
    }

    fun rl_hl(): Int{
        val address = get_16bit_address(H, L)
        val value = memory[address].toInt()
        val oldCarry = if (flagIsSet(FLAG_C)) 1 else 0
        val newCarry = (value ushr 7) and 0x1

        memory[address] = ((value shl 1) or oldCarry).toByte()

        uccu_flags(memory[address], newCarry)

        return CYCLES_16
    }

    fun rl_a(): Int{

        val oldCarry = if (flagIsSet(FLAG_C)) 1 else 0
        val newCarry = (A.toInt() ushr 7) and 0x1

        A = ((A.toInt() shl 1) or oldCarry).toByte()

        uccu_flags(A, newCarry)

        return CYCLES_8
    }

    fun rr_b(): Int{

        val oldCarry = if (flagIsSet(FLAG_C)) 1 else 0
        val newCarry = (B.toInt() ushr 7) and 0x1

        B = ((B.toInt() shr 1) or (oldCarry shl 7)).toByte()

        uccu_flags(B, newCarry)

        return CYCLES_8
    }

    fun rr_c(): Int{

        val oldCarry = if (flagIsSet(FLAG_C)) 1 else 0
        val newCarry = (C.toInt() ushr 7) and 0x1

        C = ((C.toInt() shr 1) or (oldCarry shl 7)).toByte()

        uccu_flags(C, newCarry)

        return CYCLES_8
    }

    fun rr_d(): Int{

        val oldCarry = if (flagIsSet(FLAG_C)) 1 else 0
        val newCarry = (D.toInt() ushr 7) and 0x1

        D = ((D.toInt() shr 1) or (oldCarry shl 7)).toByte()

        uccu_flags(D, newCarry)

        return CYCLES_8
    }

    fun rr_e(): Int{

        val oldCarry = if (flagIsSet(FLAG_C)) 1 else 0
        val newCarry = (E.toInt() ushr 7) and 0x1

        E = ((E.toInt() shr 1) or (oldCarry shl 7)).toByte()

        uccu_flags(E, newCarry)

        return CYCLES_8
    }

    fun rr_h(): Int{

        val oldCarry = if (flagIsSet(FLAG_C)) 1 else 0
        val newCarry = (H.toInt() ushr 7) and 0x1

        H = ((H.toInt() shr 1) or (oldCarry shl 7)).toByte()

        uccu_flags(H, newCarry)

        return CYCLES_8
    }

    fun rr_l(): Int{

        val oldCarry = if (flagIsSet(FLAG_C)) 1 else 0
        val newCarry = (L.toInt() ushr 7) and 0x1

        L = ((L.toInt() shr 1) or (oldCarry shl 7)).toByte()

        uccu_flags(L, newCarry)

        return CYCLES_8
    }

    fun rr_hl(): Int{
        val address = get_16bit_address(H, L)
        val value = memory[address].toInt()
        val oldCarry = if (flagIsSet(FLAG_C)) 1 else 0
        val newCarry = (value ushr 7) and 0x1

        memory[address] = ((value shr 1) or (oldCarry shl 7)).toByte()

        uccu_flags(memory[address], newCarry)

        return CYCLES_16
    }

    fun rr_a(): Int{

        val oldCarry = if (flagIsSet(FLAG_C)) 1 else 0
        val newCarry = (A.toInt() ushr 7) and 0x1

        A = ((A.toInt() shr 1) or (oldCarry shl 7)).toByte()

        uccu_flags(A, newCarry)

        return CYCLES_8
    }

    fun sla_b(): Int{

        val newCarry = (B.toInt() ushr 7) and 0x1
        B = ((B.toInt() shl 1) and 0xFE).toByte()

        uccu_flags(B, newCarry)
        
        return CYCLES_8
    }

    fun sla_c(): Int{

        val newCarry = (C.toInt() ushr 7) and 0x1
        C = ((C.toInt() shl 1) and 0xFE).toByte()

        uccu_flags(C, newCarry)

        return CYCLES_8
    }

    fun sla_d(): Int{

        val newCarry = (D.toInt() ushr 7) and 0x1
        D = ((D.toInt() shl 1) and 0xFE).toByte()

        uccu_flags(D, newCarry)

        return CYCLES_8
    }

    fun sla_e(): Int{

        val newCarry = (E.toInt() ushr 7) and 0x1
        E = ((E.toInt() shl 1) and 0xFE).toByte()

        uccu_flags(E, newCarry)

        return CYCLES_8
    }

    fun sla_h(): Int{

        val newCarry = (H.toInt() ushr 7) and 0x1
        H = ((H.toInt() shl 1) and 0xFE).toByte()

        uccu_flags(H, newCarry)

        return CYCLES_8
    }

    fun sla_l(): Int{

        val newCarry = (L.toInt() ushr 7) and 0x1
        L = ((L.toInt() shl 1) and 0xFE).toByte()

        uccu_flags(L, newCarry)

        return CYCLES_8
    }

    fun sla_hl(): Int{
        val address = get_16bit_address(H, L)
        val newCarry = (memory[address].toInt() ushr 7) and 0x1
        memory[address] = ((memory[address].toInt() shl 1) and 0xFE).toByte()

        uccu_flags(memory[address], newCarry)

        return CYCLES_16
    }

    fun sla_a(): Int{

        val newCarry = (A.toInt() ushr 7) and 0x1
        A = ((A.toInt() shl 1) and 0xFE).toByte()

        uccu_flags(A, newCarry)

        return CYCLES_8
    }

    fun sra_b(): Int{

        val oldBit7 = B.toInt() and 0x80
        val newCarry = B.toInt() and 0x1

        B = (((B.toInt() and 0xFF) shr 1) or oldBit7).toByte()

        uccu_flags(B, newCarry)

        return CYCLES_8
    }

    fun sra_c(): Int{

        val oldBit7 = C.toInt() and 0x80
        val newCarry = C.toInt() and 0x1

        C = (((C.toInt() and 0xFF) shr 1) or oldBit7).toByte()

        uccu_flags(C, newCarry)

        return CYCLES_8
    }

    fun sra_d(): Int{

        val oldBit7 = D.toInt() and 0x80
        val newCarry = D.toInt() and 0x1

        D = (((C.toInt() and 0xFF) shr 1) or oldBit7).toByte()

        uccu_flags(D, newCarry)

        return CYCLES_8
    }

    fun sra_e(): Int{

        val oldBit7 = E.toInt() and 0x80
        val newCarry = E.toInt() and 0x1

        E = (((E.toInt() and 0xFF) shr 1) or oldBit7).toByte()

        uccu_flags(E, newCarry)

        return CYCLES_8
    }

    fun sra_h(): Int{

        val oldBit7 = H.toInt() and 0x80
        val newCarry = H.toInt() and 0x1

        H = (((H.toInt() and 0xFF) shr 1) or oldBit7).toByte()

        uccu_flags(H, newCarry)

        return CYCLES_8
    }

    fun sra_l(): Int{

        val oldBit7 = L.toInt() and 0x80
        val newCarry = L.toInt() and 0x1

        L = (((L.toInt() and 0xFF) shr 1) or oldBit7).toByte()

        uccu_flags(L, newCarry)

        return CYCLES_8
    }

    fun sra_hl(): Int{
        val address = get_16bit_address(H,L)
        val oldBit7 = memory[address].toInt() and 0x80
        val newCarry = memory[address].toInt() and 0x1

        memory[address] = (((memory[address].toInt() and 0xFF) shr 1) or oldBit7).toByte()

        uccu_flags(memory[address], newCarry)

        return CYCLES_16
    }

    fun sra_a(): Int{

        val oldBit7 = A.toInt() and 0x80
        val newCarry = A.toInt() and 0x1

        A = (((A.toInt() and 0xFF) shr 1) or oldBit7).toByte()

        uccu_flags(A, newCarry)

        return CYCLES_8
    }

    fun swap_b(): Int{

        val low = (B.toInt() and 0x0F) shl 4
        val high = (B.toInt() and 0xF0) shr 4
        B = (low or high).toByte()

        uccu_flags(B, 0)

        return CYCLES_8
    }

    fun swap_c(): Int{

        val low = (C.toInt() and 0x0F) shl 4
        val high = (C.toInt() and 0xF0) shr 4
        C = (low or high).toByte()

        uccu_flags(C, 0)

        return CYCLES_8
    }

    fun swap_d(): Int{

        val low = (D.toInt() and 0x0F) shl 4
        val high = (D.toInt() and 0xF0) shr 4
        D = (low or high).toByte()

        uccu_flags(D, 0)

        return CYCLES_8
    }

    fun swap_e(): Int{

        val low = (E.toInt() and 0x0F) shl 4
        val high = (E.toInt() and 0xF0) shr 4
        E = (low or high).toByte()

        uccu_flags(E, 0)

        return CYCLES_8
    }

    fun swap_h(): Int{

        val low = (H.toInt() and 0x0F) shl 4
        val high = (H.toInt() and 0xF0) shr 4
        H = (low or high).toByte()

        uccu_flags(H, 0)

        return CYCLES_8
    }

    fun swap_l(): Int{

        val low = (L.toInt() and 0x0F) shl 4
        val high = (L.toInt() and 0xF0) shr 4
        L = (low or high).toByte()

        uccu_flags(L, 0)

        return CYCLES_8
    }

    fun swap_hl(): Int{
        val address = get_16bit_address(H, L)
        val low = (memory[address].toInt() and 0x0F) shl 4
        val high = (memory[address].toInt() and 0xF0) shr 4
        memory[address] = (low or high).toByte()

        uccu_flags(memory[address], 0)

        return CYCLES_16
    }

    fun swap_a(): Int{

        val low = (A.toInt() and 0x0F) shl 4
        val high = (A.toInt() and 0xF0) shr 4
        A = (low or high).toByte()

        uccu_flags(A, 0)

        return CYCLES_8
    }

    fun srl_b(): Int{

        val newCarry = B.toInt() and 0x1
        B = (((B.toInt() and 0xFF) shr 1) and 0x7F).toByte()

        uccu_flags(B, newCarry)

        return CYCLES_8
    }

    fun srl_c(): Int{

        val newCarry = C.toInt() and 0x1
        C = (((C.toInt() and 0xFF) shr 1) and 0x7F).toByte()

        uccu_flags(C, newCarry)

        return CYCLES_8
    }

    fun srl_d(): Int{

        val newCarry = D.toInt() and 0x1
        D = (((D.toInt() and 0xFF) shr 1) and 0x7F).toByte()

        uccu_flags(D, newCarry)

        return CYCLES_8
    }

    fun srl_e(): Int{

        val newCarry = E.toInt() and 0x1
        E = (((E.toInt() and 0xFF) shr 1) and 0x7F).toByte()

        uccu_flags(E, newCarry)

        return CYCLES_8
    }

    fun srl_h(): Int{

        val newCarry = H.toInt() and 0x1
        H = (((H.toInt() and 0xFF) shr 1) and 0x7F).toByte()

        uccu_flags(H, newCarry)

        return CYCLES_8
    }

    fun srl_l(): Int{

        val newCarry = L.toInt() and 0x1
        L = (((L.toInt() and 0xFF) shr 1) and 0x7F).toByte()

        uccu_flags(L, newCarry)

        return CYCLES_8
    }

    fun srl_hl(): Int{
        val address = get_16bit_address(H, L)
        val newCarry = memory[address].toInt() and 0x1
        memory[address] = (((memory[address].toInt() and 0xFF) shr 1) and 0x7F).toByte()

        uccu_flags(memory[address], newCarry)

        return CYCLES_16
    }

    fun srl_a(): Int{

        val newCarry = A.toInt() and 0x1
        A = (((A.toInt() and 0xFF) shr 1) and 0x7F).toByte()

        uccu_flags(A, newCarry)

        return CYCLES_8
    }

    fun bit_operation(register: Int, bitNumber: Int): Int{

        require(bitNumber in 0..7) { "Bit must be between 0 and 7" }
        require(register in 1..8) { "Register must be between 1 and 8" }

        var bitZero = false
        var cyclesToReturn = CYCLES_8
        val bit = 0x1 shl bitNumber

        when (register) {
            1 -> bitZero = (B.toInt() and bit) == 0
            2 -> bitZero = (C.toInt() and bit) == 0
            3 -> bitZero = (D.toInt() and bit) == 0
            4 -> bitZero = (E.toInt() and bit) == 0
            5 -> bitZero = (H.toInt() and bit) == 0
            6 -> bitZero = (L.toInt() and bit) == 0
            7 -> {
                val address = get_16bit_address(H, L)
                bitZero = (memory[address].toInt() and bit) == 0
                cyclesToReturn = CYCLES_16
            }
            8 -> bitZero = (A.toInt() and bit) == 0
        }

        updateBitOperationFlags(bitZero)
        return cyclesToReturn
    }

    fun res_operation(register: Int, bitNumber: Int): Int{

        require(bitNumber in 0..7) { "Bit must be between 0 and 7" }
        require(register in 1..8) { "Register must be between 1 and 8" }

        var cyclesToReturn = CYCLES_8
        val bit = (0x1 shl bitNumber).inv()

        when (register) {
            1 -> B = (B.toInt() and bit).toByte()
            2 -> C = (C.toInt() and bit).toByte()
            3 -> D = (D.toInt() and bit).toByte()
            4 -> E = (E.toInt() and bit).toByte()
            5 -> H = (H.toInt() and bit).toByte()
            6 -> L = (L.toInt() and bit).toByte()
            7 -> {
                val address = get_16bit_address(H, L)
                memory[address] = (memory[address].toInt() and bit).toByte()
                cyclesToReturn = CYCLES_16
            }
            8 -> A = (A.toInt() and bit).toByte()
        }

        return cyclesToReturn
    }

    fun set_operation(register: Int, bitNumber: Int): Int{

        require(bitNumber in 0..7) { "Bit must be between 0 and 7" }
        require(register in 1..8) { "Register must be between 1 and 8" }

        var cyclesToReturn = CYCLES_8
        val bit = 0x1 shl bitNumber

        when (register) {
            1 -> B = (B.toInt() or bit).toByte()
            2 -> C = (C.toInt() or bit).toByte()
            3 -> D = (D.toInt() or bit).toByte()
            4 -> E = (E.toInt() or bit).toByte()
            5 -> H = (H.toInt() or bit).toByte()
            6 -> L = (L.toInt() or bit).toByte()
            7 -> {
                val address = get_16bit_address(H, L)
                memory[address] = (memory[address].toInt() or bit).toByte()
                cyclesToReturn = CYCLES_16
            }
            8 -> A = (A.toInt() or bit).toByte()
        }

        return cyclesToReturn
    }

    // TODO: extended opcodes

}