package com.example.data.network

import java.net.InetAddress
import java.util.Random

object IpGenerator {

    private val random = Random()

    /**
     * Parse CIDRs or individual IPs from text lines
     */
    fun parseIpList(content: String): List<String> {
        return content.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
    }

    /**
     * Generate random IPv4 addresses based on CIDR subnet definitions
     */
    fun getRandomIPv4s(cidrs: List<String>, count: Int = 500): List<String> {
        val ips = mutableSetOf<String>()
        val parsedRanges = cidrs.mapNotNull { parseIpv4Cidr(it) }
        if (parsedRanges.isEmpty()) return emptyList()

        var attempts = 0
        val maxAttempts = count * 10
        while (ips.size < count && attempts < maxAttempts) {
            attempts++
            val range = parsedRanges[random.nextInt(parsedRanges.size)]
            val ipLong = range.startLong + (random.nextLong() and Long.MAX_VALUE) % (range.count)
            ips.add(longToIpv4(ipLong))
        }
        return ips.toList()
    }

    /**
     * Generate random IPv6 addresses based on CIDR subnet definitions
     */
    fun getRandomIPv6s(cidrs: List<String>, count: Int = 300): List<String> {
        val ips = mutableSetOf<String>()
        val validCidrs = cidrs.filter { it.contains(":") }
        if (validCidrs.isEmpty()) return emptyList()

        var attempts = 0
        val maxAttempts = count * 10
        while (ips.size < count && attempts < maxAttempts) {
            attempts++
            val cidr = validCidrs[random.nextInt(validCidrs.size)]
            val prefix = cidr.substringBefore("/")
            val parts = prefix.split(":")
            val p0 = if (parts.isNotEmpty()) parts[0] else "2606"
            val p1 = if (parts.size > 1) parts[1] else "4700"
            val r2 = String.format("%x", random.nextInt(0xFFFF))
            val r3 = String.format("%x", random.nextInt(0xFFFF))
            val r4 = String.format("%x", random.nextInt(0xFFFF))
            val r5 = String.format("%x", random.nextInt(0xFFFF))
            val r6 = String.format("%x", random.nextInt(0xFFFF))
            val r7 = String.format("%x", random.nextInt(0xFFFF))
            val ip6 = "$p0:$p1:$r2:$r3:$r4:$r5:$r6:$r7"
            ips.add(ip6)
        }
        return ips.toList()
    }

    data class Ipv4CidrRange(val startLong: Long, val count: Long)

    private fun parseIpv4Cidr(cidr: String): Ipv4CidrRange? {
        return try {
            val parts = cidr.split("/")
            val ipStr = parts[0].trim()
            val mask = if (parts.size > 1) parts[1].toInt() else 32
            val ipLong = ipv4ToLong(ipStr)
            val hostBits = 32 - mask
            val totalHosts = if (hostBits >= 31) 1L else (1L shl hostBits)
            Ipv4CidrRange(ipLong, totalHosts)
        } catch (e: Exception) {
            null
        }
    }

    private fun ipv4ToLong(ip: String): Long {
        val bytes = InetAddress.getByName(ip).address
        var result = 0L
        for (b in bytes) {
            result = (result shl 8) or (b.toInt() and 0xFF).toLong()
        }
        return result
    }

    private fun longToIpv4(ipLong: Long): String {
        return "${(ipLong shr 24) and 0xFF}.${(ipLong shr 16) and 0xFF}.${(ipLong shr 8) and 0xFF}.${ipLong and 0xFF}"
    }
}
