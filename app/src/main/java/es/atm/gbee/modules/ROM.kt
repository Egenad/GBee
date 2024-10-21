package es.atm.gbee.modules

import es.atm.gbee.etc.convertBytesToString
import es.atm.gbee.etc.extractByte
import es.atm.gbee.etc.extractByteArray
import es.atm.gbee.etc.memcmp
import es.atm.gbee.etc.printROM
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.min

// --- CARTRIDGE HEADER ---
// 0100-0103    — Entry point
// 0104-0133    — Nintendo logo
// 0134-0143    — Title
// 013F-0142    — Manufacturer code
// 0143         — CGB flag
// 0144–0145    — New licensee code
// 0146         — SGB flag
// 0147         — Cartridge type
// 0148         — ROM size
// 0149         — RAM size
// 014A         — Destination code
// 014B         — Old licensee code
// 014C         — Mask ROM version number
// 014D         — Header checksum
// 014E-014F    — Global checksum

const val ENTRY_POINT_START : Int       = 0x100
const val ENTRY_POINT_END : Int         = 0x103
const val N_LOGO_START : Int            = 0x104
const val N_LOGO_END : Int              = 0x133
const val TITLE_START : Int             = 0x134
const val TITLE_END : Int               = 0x143
const val MANF_CODE_START : Int         = 0x13F
const val MANF_CODE_END : Int           = 0x142
const val CBG_FLAG : Int                = 0x143
const val LCNS_CODE_START : Int         = 0x144
const val LCNS_CODE_END : Int           = 0x145
const val SGB_FLAG : Int                = 0x146
const val CART_TYPE : Int               = 0x147
const val ROM_SIZE : Int                = 0x148
const val RAM_SIZE : Int                = 0x149
const val DEST_CODE : Int               = 0x14A
const val OLD_LCNS_CODE : Int           = 0x14B
const val ROM_V_NUM : Int               = 0x14C
const val HEADER_CHECKSUM : Int         = 0x14D
const val GLOBAL_CHECKSUM_START : Int   = 0x14E
const val GLOBAL_CHECKSUM_END : Int     = 0x14F

const val NEW_LICENSE_CODE : Int        = 0x33

const val ENABLE_RAM_END : Int          = 0x1FFF
const val ROM_BANK_NUMBER_END : Int     = 0x3FFF
const val RAM_BANK_NUMBER_END : Int     = 0x5FFF
const val RAM_BANK_MODE_END : Int       = 0x7FFF


object ROM {

    enum class CONSOLE_TYPE(val value : Int){
        DMG(0),
        DMG_CGB(0x80),
        CGB(0xC0),
        UNKNOWN(-1);

        companion object {
            fun fromValue(value: Int): CONSOLE_TYPE {
                return entries.find { it.value == value } ?: UNKNOWN
            }
        }
    }

    private var bootSection = ByteArray(0xFF)

    private var cartTitle : String          = "Unknown"
    private var licenseCode : String        = "None"
    private var cartType : Int              = -1
    private var romSize : Int               = -1
    private var ramSize : Int               = -1
    private var romVersion : Int            = -1
    private var console: CONSOLE_TYPE       = CONSOLE_TYPE.UNKNOWN

    private var ramEnabled: Boolean         = false
    private var bankingMode: BankingMode    = BankingMode.MODE_0

    private var ramBanks                    = Array(16){ByteArray(8 * 1024)} // MBC1 = 4 Banks Max. -- MBC3 / MBC5 = 16 Banks Max.
    private var currentRamBank : Int        = -1
    private var currentRomBank : Int        = -1
    private var currentRomBank0 : Int       = 0

    private var saveNeeded: Boolean         = false

    val newLicenseCodes: Map<String, String> = mapOf(
        "00" to "None",
        "01" to "Nintendo R&D1",
        "08" to "Capcom",
        "13" to "Electronic Arts",
        "18" to "Hudson Soft",
        "19" to "B-AI",
        "20" to "KSS",
        "22" to "Planning Office WADA",
        "24" to "PCM Complete",
        "25" to "San-X",
        "28" to "Kemco",
        "29" to "SETA Corporation",
        "30" to "Viacom",
        "31" to "Nintendo",
        "32" to "Bandai",
        "33" to "Ocean Software/Acclaim Entertainment",
        "34" to "Konami",
        "35" to "HectorSoft",
        "37" to "Taito",
        "38" to "Hudson Soft",
        "39" to "Banpresto",
        "41" to "Ubi Soft",
        "42" to "Atlus",
        "44" to "Malibu Interactive",
        "46" to "Angel",
        "47" to "Bullet-Proof Software",
        "49" to "Irem",
        "50" to "Absolute",
        "51" to "Acclaim Entertainment",
        "52" to "Activision",
        "53" to "Sammy USA Corporation",
        "54" to "Konami",
        "55" to "Hi Tech Expressions",
        "56" to "LJN",
        "57" to "Matchbox",
        "58" to "Mattel",
        "59" to "Milton Bradley Company",
        "60" to "Titus Interactive",
        "61" to "Virgin Games Ltd.",
        "64" to "Lucasfilm Games",
        "67" to "Ocean Software",
        "69" to "EA (Electronic Arts)",
        "70" to "Infogrames",
        "71" to "Interplay Entertainment",
        "72" to "Broderbund",
        "73" to "Sculptured Software",
        "75" to "The Sales Curve Limited",
        "78" to "THQ",
        "79" to "Accolade",
        "80" to "Misawa Entertainment",
        "83" to "lozc",
        "86" to "Tokuma Shoten",
        "87" to "Tsukuda Original",
        "91" to "Chunsoft Co.",
        "92" to "Video System",
        "93" to "Ocean Software/Acclaim Entertainment",
        "95" to "Varie",
        "96" to "Yonezawa/s’pal",
        "97" to "Kaneko",
        "99" to "Pack-In-Video",
        "A4" to "Konami (Yu-Gi-Oh!)",
    )

    val oldLicenseCodes : Map<Int, String> = mapOf(
        0x00 to "None",
        0x01 to "Nintendo",
        0x08 to "Capcom",
        0x09 to "HOT-B",
        0x0B to "Coconuts Japan",
        0x0C to "Elite Systems",
        0x13 to "EA (Electronic Arts)",
        0x18 to "Hudson Soft",
        0x19 to "ITC Entertainment",
        0x1A to "Yanoman",
        0x1D to "Japan Clary",
        0x1F to "Virgin Games Ltd.",
        0x24 to "PCM Complete",
        0x25 to "San-X",
        0x28 to "Kemco",
        0x29 to "SETA Corporation",
        0x30 to "Infogrames",
        0x31 to "Nintendo",
        0x32 to "Bandai",
        0x34 to "Konami",
        0x35 to "HectorSoft",
        0x38 to "Capcom",
        0x39 to "Banpresto",
        0x3C to ".Entertainment i",
        0x3E to "Gremlin",
        0x41 to "Ubi Soft",
        0x42 to "Atlus",
        0x44 to "Malibu Interactive",
        0x46 to "Angel",
        0x47 to "Spectrum Holoby",
        0x49 to "Irem",
        0x4A to "Virgin Games Ltd.",
        0x4D to "Malibu Interactive",
        0x4F to "U.S. Gold",
        0x50 to "Absolute",
        0x51 to "Acclaim Entertainment",
        0x52 to "Activision",
        0x53 to "Sammy USA Corporation",
        0x54 to "GameTek",
        0x55 to "Park Place",
        0x56 to "LJN",
        0x57 to "Matchbox",
        0x59 to "Milton Bradley Company",
        0x5A to "Mindscape",
        0x5B to "Romstar",
        0x5C to "Naxat Soft",
        0x5D to "Tradewest",
        0x60 to "Titus Interactive",
        0x61 to "Virgin Games Ltd.",
        0x67 to "Ocean Software",
        0x69 to "EA (Electronic Arts)",
        0x6E to "Elite Systems",
        0x6F to "Electro Brain",
        0x70 to "Infogrames5",
        0x71 to "Interplay Entertainment",
        0x72 to "Broderbund",
        0x73 to "Sculptured Software",
        0x75 to "The Sales Curve Limited",
        0x78 to "THQ",
        0x79 to "Accolade",
        0x7A to "Triffix Entertainment",
        0x7C to "Microprose",
        0x7F to "Kemco",
        0x80 to "Misawa Entertainment",
        0x83 to "Lozc",
        0x86 to "Tokuma Shoten",
        0x8B to "Bullet-Proof Software",
        0x8C to "Vic Tokai",
        0x8E to "Ape",
        0x8F to "I’Max",
        0x91 to "Chunsoft Co.",
        0x92 to "Video System",
        0x93 to "Tsubaraya Productions",
        0x95 to "Varie",
        0x96 to "Yonezawa/S’Pal",
        0x97 to "Kemco",
        0x99 to "Arc",
        0x9A to "Nihon Bussan",
        0x9B to "Tecmo",
        0x9C to "Imagineer",
        0x9D to "Banpresto",
        0x9F to "Nova",
        0xA1 to "Hori Electric",
        0xA2 to "Bandai",
        0xA4 to "Konami",
        0xA6 to "Kawada",
        0xA7 to "Takara",
        0xA9 to "Technos Japan",
        0xAA to "Broderbund",
        0xAC to "Toei Animation",
        0xAD to "Toho",
        0xAF to "Namco",
        0xB0 to "Acclaim Entertainment",
        0xB1 to "ASCII Corporation or Nexsoft",
        0xB2 to "Bandai",
        0xB4 to "Square Enix",
        0xB6 to "HAL Laboratory",
        0xB7 to "SNK",
        0xB9 to "Pony Canyon",
        0xBA to "Culture Brain",
        0xBB to "Sunsoft",
        0xBD to "Sony Imagesoft",
        0xBF to "Sammy Corporation",
        0xC0 to "Taito",
        0xC2 to "Kemco",
        0xC3 to "Square",
        0xC4 to "Tokuma Shoten",
        0xC5 to "Data East",
        0xC6 to "Tonkinhouse",
        0xC8 to "Koei",
        0xC9 to "UFL",
        0xCA to "Ultra",
        0xCB to "Vap",
        0xCC to "Use Corporation",
        0xCD to "Meldac",
        0xCE to "Pony Canyon",
        0xCF to "Angel",
        0xD0 to "Taito",
        0xD1 to "Sofel",
        0xD2 to "Quest",
        0xD3 to "Sigma Enterprises",
        0xD4 to "ASK Kodansha Co.",
        0xD6 to "Naxat Soft13",
        0xD7 to "Copya System",
        0xD9 to "Banpresto",
        0xDA to "Tomy",
        0xDB to "LJN",
        0xDD to "NCS",
        0xDE to "Human",
        0xDF to "Altron",
        0xE0 to "Jaleco",
        0xE1 to "Towa Chiki",
        0xE2 to "Yutaka",
        0xE3 to "Varie",
        0xE5 to "Epcoh",
        0xE7 to "Athena",
        0xE8 to "Asmik Ace Entertainment",
        0xE9 to "Natsume",
        0xEA to "King Records",
        0xEB to "Atlus",
        0xEC to "Epic/Sony Records",
        0xEE to "IGS",
        0xF0 to "A Wave",
        0xF3 to "Extreme Entertainment",
        0xFF to "LJN"
    )

    // MBC = Memory Bank Controller

    val cartTypes : Map<Int, String> = mapOf(
        0x00 to "ROM ONLY",
        0x01 to "MBC1",
        0x02 to "MBC1+RAM",
        0x03 to "MBC1+RAM+BATTERY",
        0x05 to "MBC2",
        0x06 to "MBC2+BATTERY",
        0x08 to "ROM+RAM",
        0x09 to "ROM+RAM+BATTERY",
        0x0B to "MMM01",
        0x0C to "MMM01+RAM",
        0x0D to "MMM01+RAM+BATTERY",
        0x0F to "MBC3+TIMER+BATTERY",
        0x10 to "MBC3+TIMER+RAM+BATTERY",
        0x11 to "MBC3",
        0x12 to "MBC3+RAM",
        0x13 to "MBC3+RAM+BATTERY",
        0x19 to "MBC5",
        0x1A to "MBC5+RAM",
        0x1B to "MBC5+RAM+BATTERY",
        0x1C to "MBC5+RUMBLE",
        0x1D to "MBC5+RUMBLE+RAM",
        0x1E to "MBC5+RUMBLE+RAM+BATTERY",
        0x20 to "MBC6",
        0x22 to "MBC7+SENSOR+RUMBLE+RAM+BATTERY",
        0xFC to "POCKET CAMERA",
        0xFD to "BANDAI TAMA5",
        0xFE to "HuC3",
        0xFF to "HuC1+RAM+BATTERY"
    )

    val ramSizes : Map<Int, Int> = mapOf(
        0x00 to 0, // KiB
        0x01 to 2,
        0x02 to 8,
        0x03 to 32,
        0x04 to 128,
        0x05 to 64
    )

    enum class BankingMode(val mode: Int) {
        MODE_0(0),
        MODE_1(1)
    }

    fun getNewLicenseNameFromIndex(code: String): String {
        return newLicenseCodes[code] ?: "Unknown"
    }

    fun getOldLicenseNameFromIndex(code: Int): String {
        return oldLicenseCodes[code] ?: "Unknown"
    }

    fun getRomTypeFromIndex(index: Int): String {
       return cartTypes[index] ?: "Unknown"
    }

    fun cartTypeIsMBC(): Boolean{
        return getRomTypeFromIndex(cartType).contains("MBC")
    }

    fun getCartTypeIndex(): Int{
        val regex = """\d+""".toRegex()
        val matchResult = regex.find(getRomTypeFromIndex(cartType))
        return matchResult?.value?.toInt() ?: 0
    }

    fun getRamSizeFromIndex(index: Int): Int {
        return ramSizes[index] ?: 0
    }

    fun load_rom_from_path(path: String){
        try {
            val file = File(path)
            val bytes = file.readBytes()
            load_rom(bytes)
        }catch (ex: Exception){
            println("Error loading ROM: $ex")
        }
    }

    fun load_rom(romBytes: ByteArray): Boolean{

        try {
            if(romBytes.isNotEmpty()){
                val cartSize = min(romBytes.size, ROM_END - ROM_START + 1)

                for (i in 0 until cartSize) {
                    Memory.write(ROM_START + i, romBytes[i])
                }

                return rom_init(romBytes)
            }
        }catch (ex: Exception){
            println("Error loading ROM: $ex")
        }

        return false
    }

    fun rom_init(romBytes: ByteArray): Boolean{

        // Compare Cartridge Header with the Boot fixed one

        // Get header section from the romBytes
        val bootByteArray = Memory.getNintendoLogo()
        val cartByteArray = extractByteArray(romBytes, N_LOGO_START, N_LOGO_END, true) // Nintendo Logo on Cartridge goes from 0x104 to 0x133

        if(memcmp(Memory.getNintendoLogo(), cartByteArray, bootByteArray.size) != 0){
            return false
        }

        cartTitle   = convertBytesToString(extractByteArray(romBytes, TITLE_START, TITLE_END, true))

        licenseCode = if((extractByte(romBytes, OLD_LCNS_CODE).toInt() and 0xFF) == NEW_LICENSE_CODE){
            getNewLicenseNameFromIndex(convertBytesToString(extractByteArray(romBytes, LCNS_CODE_START, LCNS_CODE_END, true)))
        }else{
            getOldLicenseNameFromIndex(extractByte(romBytes, OLD_LCNS_CODE).toInt() and 0xFF)
        }

        cartType    = extractByte(romBytes, CART_TYPE).toInt() and 0xFF
        romSize     = 32 * (1 shl extractByte(romBytes, ROM_SIZE).toInt() and 0xFF) // Value in KiB
        ramSize     = extractByte(romBytes, RAM_SIZE).toInt() and 0xFF
        romVersion  = extractByte(romBytes, ROM_V_NUM).toInt() and 0xFF
        console     = CONSOLE_TYPE.fromValue(extractByte(romBytes, TITLE_END).toInt() and 0xFF)

        println("ROM Loaded Successfully!")
        printROM()

        bootSection = extractByteArray(romBytes, 0x00, 0xFF, true) // Save portion of code where the boot is going to load

        return true
    }

    fun reloadBootPortion(){
        for (address in 0x00..0xFF){
            Memory.write(address, bootSection[address])
        }
    }

    fun readFromROM(address: Int): Byte{

        // Check if fixed ROM
        if(!cartTypeIsMBC() || address < ROM_SW_START){
            return Memory.read(address)
        }

        if(address in EXTERNAL_RAM_START..< WRAM_START){
            if (!ramEnabled || bankingMode == BankingMode.MODE_0 || currentRamBank < 0 || currentRamBank >= ramBanks.size){
                return 0xFF.toByte()
            }

            return ramBanks[currentRamBank][address - EXTERNAL_RAM_START]
        }

        return Memory.read(address)
    }

    fun writeToROM(address: Int, value: Byte){

        if(!cartTypeIsMBC()){
            return
        }

        when(getCartTypeIndex()){
            1 -> writeMBC1(address, value)
        }

    }

    private fun writeMBC1(address: Int, value: Byte){
        val valueInt = value.toInt() and 0xFF

        when {
            address <= ENABLE_RAM_END -> {  // ENABLE RAM
                ramEnabled = valueInt and 0xF == 0xA
            }
            address in (ENABLE_RAM_END + 1)..ROM_BANK_NUMBER_END -> {// ROM BANK SELECTION
                currentRomBank = valueInt and 0b11111

                if (bankingMode == BankingMode.MODE_0) { // MODE 0 -> Use RAM Bank 2 bit register as upper 5-6 bits of the ROM Bank
                    currentRomBank += (currentRamBank shl 5)

                    if (currentRomBank == 0 || currentRomBank == 0x20 || currentRomBank == 0x40 || currentRomBank == 0x60) { // In MODE 0 isn't possible to access these banks
                        currentRomBank++
                    }
                }else{ // MODE 1
                    /*currentRomBank0 = when {
                        currentRomBank in 0x21..0x3F -> 0x20
                        currentRomBank in 0x41..0x5F -> 0x40
                        currentRomBank in 0x61..0x7F -> 0x60
                        else -> 0
                    }*/
                }

                // TODO: Switch bank in Memory module
            }
            address in (ROM_BANK_NUMBER_END + 1)..RAM_BANK_NUMBER_END -> {  // RAM BANK SELECTION
                if (bankingMode == BankingMode.MODE_1 && saveNeeded) {
                    saveExRAMToFile()
                }
                currentRamBank = valueInt and 0b11
            }
            address in (RAM_BANK_NUMBER_END + 1)..RAM_BANK_MODE_END -> {  // BANKING MODE
                bankingMode = if (valueInt and 0b1 != 0) BankingMode.MODE_1 else BankingMode.MODE_0

                if(bankingMode == BankingMode.MODE_1 && saveNeeded){
                    saveExRAMToFile()
                }
            }
            address in EXTERNAL_RAM_START..< WRAM_START -> { // WRITE TO EXTERNAL RAM
                if(ramEnabled && bankingMode == BankingMode.MODE_1){
                    ramBanks[currentRamBank][address - EXTERNAL_RAM_START] = value

                    if(cartHasBattery()) saveNeeded = true
                }
            }
        }
    }

    private fun loadExRAMFromFile(){

    }

    private fun saveExRAMToFile(){
        if(currentRamBank >= 0){
            val batteryFilename = "$cartTitle.sav"
            try {
                val file = File(batteryFilename)
                FileOutputStream(file).use { fos ->
                    fos.write(ramBanks[currentRamBank])
                }
            } catch (e: IOException) {
                System.err.println("FAILED TO OPEN OR WRITE: $batteryFilename")
            }
        }
    }

    private fun cartHasBattery() : Boolean{
        return getRomTypeFromIndex(cartType).contains("BATTERY")
    }

    fun getCartTitle() : String{
        return cartTitle
    }

    fun getLicenseCode(): String{
        return licenseCode
    }

    fun getCartType(): Int{
        return cartType
    }

    fun getRomSize(): Int{
        return romSize
    }

    fun getRamSize(): Int{
        return ramSize
    }

    fun getRomVersion(): Int{
        return romVersion
    }

    fun getConsole(): CONSOLE_TYPE{
        return console
    }

    fun isCGB(): Boolean{
        return console == CONSOLE_TYPE.CGB
    }
}