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

fun extractByteArray(original: ByteArray, start: Int, end: Int): ByteArray {
    if (start < 0 || end > original.size || start > end) {
        throw IllegalArgumentException("Out of bounds")
    }
    return original.copyOfRange(start, end)
}