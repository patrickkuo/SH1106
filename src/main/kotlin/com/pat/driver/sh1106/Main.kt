package com.pat.driver.sh1106

import java.net.NetworkInterface
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

fun main() {
    val oled = SH1106()
    val ipRegex =
        Regex("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\$")

    val ip = NetworkInterface.getNetworkInterfaces().asSequence()
        .flatMap { it.inetAddresses.asSequence() }
        .map { it.hostAddress }
        .filter { it.matches(ipRegex) }
        .find { !it.startsWith("127.") } ?: "Unknown IP"

    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(Locale.UK)
        .withZone(ZoneId.systemDefault())

    while (true) {
        Thread.sleep(200)
        oled.clear()
        oled.drawStringCentered(
            formatter.format(Instant.now()),
            Font.FONT_5X8, 10, true
        )
        oled.drawStringCentered(ip, Font.FONT_5X8, 40, true)
        oled.update()
    }
}
