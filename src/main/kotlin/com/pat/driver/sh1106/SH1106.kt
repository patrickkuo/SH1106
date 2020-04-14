package com.pat.driver.sh1106

import com.pi4j.io.i2c.I2CBus
import com.pi4j.io.i2c.I2CDevice
import com.pi4j.io.i2c.I2CFactory
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.IOException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class SH1106(busNumber: Int = DEFAULT_I2C_BUS, displayAddress: Int = DEFAULT_DISPLAY_ADDRESS) {
    private val bus: I2CBus = I2CFactory.getInstance(busNumber)
    private val device: I2CDevice
    private val imageBuffer = IntArray(width * height / 8)

    @Synchronized
    fun clear() {
        Arrays.fill(imageBuffer, 0x00)
    }

    private fun writeCommand(command: Int) {
        device.write(0x00, command.toByte())
    }

    private fun init() {
        writeCommand(SH1106_DISPLAYOFF) // 0xAE
        writeCommand(SH1106_SETDISPLAYCLOCKDIV) // 0xD5
        writeCommand(0x80) // the suggested ratio 0x80
        writeCommand(SH1106_SETMULTIPLEX) // 0xA8
        writeCommand(0x3F)
        writeCommand(SH1106_SETDISPLAYOFFSET) // 0xD3
        writeCommand(0x0) // no offset
        writeCommand(SH1106_SETSTARTLINE or 0x0) // line #0
        writeCommand(SH1106_CHARGEPUMP) // 0x8D
        writeCommand(0x14)
        writeCommand(SH1106_MEMORYMODE) // 0x20
        writeCommand(0x00) // 0x0 act like ks0108
        writeCommand(SH1106_SEGREMAP or 0x1)
        writeCommand(SH1106_COMSCANDEC)
        writeCommand(SH1106_SETCOMPINS) // 0xDA
        writeCommand(0x12)
        writeCommand(SH1106_SETCONTRAST) // 0x81
        writeCommand(0xCF)
        writeCommand(SH1106_SETPRECHARGE) // 0xd9
        writeCommand(0xF1)
        writeCommand(SH1106_SETVCOMDETECT) // 0xDB
        writeCommand(0x40)
        writeCommand(SH1106_DISPLAYALLON_RESUME) // 0xA4
        writeCommand(SH1106_NORMALDISPLAY)
        writeCommand(SH1106_DISPLAYON) //--turn on oled panel
    }

    @Synchronized
    fun setPixel(x: Int, y: Int, on: Boolean) {
        val pos: Int = x + (y / 8) * width
        //if (pos in 0 until MAX_INDEX) {
        if (on) {
            imageBuffer[pos] = imageBuffer[pos] or (1 shl (y and 0x07))
        } else {
            imageBuffer[pos] = imageBuffer[pos] and (1 shl (y and 0x07)).inv()
        }
        // }
    }

    @Synchronized
    fun drawChar(c: Char, font: Font, x: Int, y: Int, on: Boolean) {
        font.drawChar(this, c, x, y, on)
    }

    @Synchronized
    fun drawString(string: String, font: Font, x: Int, y: Int, on: Boolean) {
        var posX = x
        var posY = y
        for (c in string.toCharArray()) {
            if (c == '\n') {
                posY += font.outerHeight
                posX = x
            } else {
                if (posX >= 0 && posX + font.width < width && posY >= 0 && posY + font.height < height
                ) {
                    drawChar(c, font, posX, posY, on)
                }
                posX += font.outerWidth
            }
        }
    }

    @Synchronized
    fun drawStringCentered(string: String, font: Font, y: Int, on: Boolean) {
        val strSizeX: Int = string.length * font.outerWidth
        val x: Int = (width - strSizeX) / 2
        drawString(string, font, x, y, on)
    }

    @Synchronized
    fun clearRect(x: Int, y: Int, width: Int, height: Int, on: Boolean) {
        for (posX in x until x + width) {
            for (posY in y until y + height) {
                setPixel(posX, posY, on)
            }
        }
    }

    @Synchronized
    fun drawImage(image: BufferedImage?, x: Int, y: Int) {
        val tmpImage = BufferedImage(
            width,
            height, BufferedImage.TYPE_BYTE_BINARY
        )
        tmpImage.graphics.drawImage(image, x, y, null)
        var index = 0
        var pixelval: Int
        val pixels = (tmpImage.raster.dataBuffer as DataBufferByte).data
        for (posY in 0 until height) {
            for (posX in 0 until width / 8) {
                for (bit in 0..7) {
                    pixelval = (pixels[index / 8].toInt() shr 7 - bit and 0x01)
                    setPixel(posX * 8 + bit, posY, pixelval > 0)
                    index++
                }
            }
        }
    }

    @Synchronized
    fun update() {
        writeCommand(SH1106_SETLOWCOLUMN or 0x0)
        writeCommand(SH1106_SETHIGHCOLUMN or 0x0)
        writeCommand(SH1106_SETSTARTLINE or 0x0)

        val width = 132 shr 3
        val height = 64 shr 3
        val row = 0
        val col = 2

        val buffer = imageBuffer.map { it.toByte() }.toByteArray()
        for (i in 0 until height) {
            writeCommand(0xB0 + i + row)
            writeCommand(col and 0xF)
            writeCommand(0x10 or (col shr 4))
            val offset = i * width * 8
            for (j in 0 until 8) {
                device.write(0x40, buffer, offset + j * width, width)
            }
        }
    }

    @Synchronized
    private fun shutdown() {
        try {
            //before we shut down we clear the display
            clear()
            update()

            //now we close the bus
            bus.close()
        } catch (ex: IOException) {
            LOGGER.log(Level.FINE, "Closing i2c bus")
        }
    }

    companion object {
        private val LOGGER = Logger.getLogger(SH1106::class.java.canonicalName)
        private const val DEFAULT_I2C_BUS: Int = I2CBus.BUS_1
        private const val DEFAULT_DISPLAY_ADDRESS = 0x3C
        private const val width = 128
        private const val height = 64
        private const val MAX_INDEX = height / 8 * width
        private const val SH1106_SETCONTRAST = 0x81
        private const val SH1106_DISPLAYALLON_RESUME = 0xA4
        private const val SH1106_DISPLAYALLON = 0xA5
        private const val SH1106_NORMALDISPLAY = 0xA6
        private const val SH1106_INVERTDISPLAY = 0xA7
        private const val SH1106_DISPLAYOFF = 0xAE
        private const val SH1106_DISPLAYON = 0xAF
        private const val SH1106_SETDISPLAYOFFSET = 0xD3
        private const val SH1106_SETCOMPINS = 0xDA
        private const val SH1106_SETVCOMDETECT = 0xDB
        private const val SH1106_SETDISPLAYCLOCKDIV = 0xD5
        private const val SH1106_SETPRECHARGE = 0xD9
        private const val SH1106_SETMULTIPLEX = 0xA8
        private const val SH1106_SETLOWCOLUMN = 0x00
        private const val SH1106_SETHIGHCOLUMN = 0x10
        private const val SH1106_SETSTARTLINE = 0x40
        private const val SH1106_MEMORYMODE = 0x20
        private const val SH1106_COLUMNADDR = 0x21
        private const val SH1106_PAGEADDR = 0x22
        private const val SH1106_COMSCANINC = 0xC0
        private const val SH1106_COMSCANDEC = 0xC8
        private const val SH1106_SEGREMAP = 0xA0
        private const val SH1106_CHARGEPUMP = 0x8D
        private const val SH1106_EXTERNALVCC = 0x1
        private const val SH1106_SWITCHCAPVCC = 0x2
    }

    init {
        device = bus.getDevice(displayAddress)
        LOGGER.log(Level.FINE, "Opened i2c bus")
        clear()

        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                shutdown()
            }
        })
        init()
    }
}