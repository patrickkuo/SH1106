package com.pat.driver.sh1106

import kotlin.streams.toList

enum class Font(
    private val minChar: Int,
    private val maxChar: Int,
    val width: Int,
    val height: Int,
    val outerWidth: Int,
    val outerHeight: Int,
    private val data: ByteArray
) {
    FONT_5X8(0, 255, 5, 8, 6, 9, readFromFile("FONT_5X8")),
    FONT_4X5(32, 95, 4, 5, 4, 7, readFromFile("FONT_4X5"));

    fun getData(offset: Int): Byte {
        return this.data[offset]
    }

    internal fun drawChar(display: SH1106, _c: Char, x: Int, y: Int, on: Boolean) {
        var c = _c
        if (c.toInt() > maxChar || c.toInt() < minChar) {
            c = '?'
        }
        c -= minChar
        for (i in 0 until width) {
            var line = data[(c.toInt() * width) + i].toInt()
            for (j in 0 until height) {
                if ((line and 0x01) > 0) {
                    display.setPixel(x + i, y + j, on)
                }
                line = line shr 1
            }
        }
    }
}

private fun readFromFile(filename: String) = ClassLoader.getSystemResourceAsStream(filename)!!
    .bufferedReader().lines().toList()
    .map { it.drop(2).toInt(16).toByte() }
    .toByteArray()