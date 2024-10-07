package es.atm.gbee.etc

fun memcmp(b1: ByteArray, b2: ByteArray, sz: Int): Int {
    for (i in 0 until sz) {
        if (b1[i] != b2[i]) {
            if ((b1[i] >= 0 && b2[i] >= 0) || (b1[i] < 0 && b2[i] < 0)) return b1[i] - b2[i]
            if (b1[i] < 0 && b2[i] >= 0) return 1
            if (b2[i] < 0 && b1[i] >= 0) return -1
        }
    }
    return 0
}

fun extractByteArray(original: ByteArray, start: Int, end: Int, inclusive: Boolean): ByteArray {
    if (start < 0 || end > original.size || start > end) {
        throw IllegalArgumentException("Out of bounds")
    }
    return original.copyOfRange(start, if (inclusive) end + 1 else end)
}

fun extractByte(original: ByteArray, byteAddres: Int): Byte {
    if (byteAddres < 0 || byteAddres > original.size) {
        throw IllegalArgumentException("Out of bounds")
    }
    return original[byteAddres]
}

fun convertBytesToString(bytes: ByteArray): String {
    val title = bytes.takeWhile { it != 0.toByte() && it.toInt() in 32..126 } // Filter only ASCII characters
    return String(title.toByteArray(), Charsets.US_ASCII)
}