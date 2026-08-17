package org.dthv.linkforge.utils

private const val ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val BASE = 62L

fun Long.toBase62(): String {
    require(this >= 0) { "Cannot encode a negative number: $this" }
    if (this == 0L) return ALPHABET[0].toString()

    val sb = StringBuilder()
    var n = this
    while (n > 0) {
        val remainder = (n % BASE).toInt()
        sb.append(ALPHABET[remainder])
        n /= BASE
    }
    return sb.reverse().toString()
}

