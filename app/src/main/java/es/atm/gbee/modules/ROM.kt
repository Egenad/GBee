package es.atm.gbee.modules

import es.atm.gbee.etc.extractByte
import es.atm.gbee.etc.extractByteArray
import es.atm.gbee.etc.memcmp
import java.io.File
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

object ROM {

    private var cartTitle : String      = "Unknown"
    private var licenseCode : String    = "None"
    private var cartType : String       = "ROM ONLY"
    private var romSize : Int           = -1
    private var ramSize : Int           = -1

    val licenseCodes: Map<String, String> = mapOf(
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

    fun getLicenseName(code: String): String {
        return licenseCodes[code] ?: "Unknown"
    }

    fun getRomType(index: Int): String {
       return cartTypes[index] ?: "Unknown"
    }

    fun load_rom(fileName: String): Boolean{

        try {
            val romFile = File(fileName)
            val romBytes = romFile.readBytes()

            if(romBytes.isNotEmpty()){
                val cartSize = min(romBytes.size, ROM_END - ROM_START + 1)

                for (i in 0 until cartSize) {
                    Memory.writeByteOnAddress(ROM_START + 1, romBytes[i])
                }

                return rom_init(romBytes)
            }
        }catch (_: Exception){}

        return false
    }

    fun convertBytesToString(bytes: ByteArray): String {
        val title = bytes.takeWhile { it != 0.toByte() && it.toInt() in 32..126 } // Filter only ASCII characters
        return String(title.toByteArray(), Charsets.US_ASCII)
    }

    fun rom_init(romBytes: ByteArray): Boolean{

        // Compare Cartridge Header with the Boot fixed one

        // Get header section from the romBytes
        val bootByteArray = Memory.getNintendoLogo()
        val cartByteArray = extractByteArray(romBytes, N_LOGO_START, N_LOGO_END, true) // Nintendo Logo on Cartridge goes from 0x104 to 0x133

        if(memcmp(Memory.getNintendoLogo(), cartByteArray, bootByteArray.size) != 0){
            return false
        }

        println("ROM Loaded Successfully!")

        cartTitle   = convertBytesToString(extractByteArray(romBytes, TITLE_START, TITLE_END, true))
        licenseCode = getLicenseName(convertBytesToString(extractByteArray(romBytes, LCNS_CODE_START, LCNS_CODE_END, true)))
        cartType    = getRomType(extractByte(romBytes, CART_TYPE).toInt() and 0xFF)
        romSize     = 32 * (1 shl extractByte(romBytes, ROM_SIZE).toInt() and 0xFF) // Value in KiB

        return true
    }
}