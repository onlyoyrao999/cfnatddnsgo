package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data model for a Cloudflare IP scan result.
 */
data class ScannedIp(
    val ip: String,
    val dataCenter: String = "UNK", // e.g., HKG, SJC, LAX
    val region: String = "",
    val city: String = "",
    val latencyMs: Long = 0,
    val isValid: Boolean = true,
    val testedAt: Long = System.currentTimeMillis(),
    val ipVersion: String = "4", // "4" or "6"
    val isFavorite: Boolean = false
)

/**
 * Cloudflare Edge location dataset mapping (from locations.json)
 */
data class LocationInfo(
    val iata: String,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val cca2: String = "",
    val region: String = "",
    val city: String = ""
)

/**
 * Configuration options for the IP scanner engine
 */
data class ScanConfig(
    val ipType: String = "4", // "4" or "6"
    val port: Int = 443,
    val maxThreads: Int = 100,
    val delayMs: Int = 300,
    val coloFilter: String = "", // e.g. "HKG,SJC,LAX"
    val domain: String = "cloudflaremirrors.com/debian",
    val expectedCode: Int = 200,
    val random: Boolean = true,
    val ipCount: Int = 1000,
    val useTls: Boolean = true
)

/**
 * Status of the local TCP proxy relay server
 */
data class ProxyStatus(
    val isRunning: Boolean = false,
    val localPort: Int = 1234,
    val localAddr: String = "0.0.0.0:1234",
    val activeConnections: Int = 0,
    val activeTargetIp: String = "",
    val activeColo: String = "",
    val activeCity: String = "",
    val activeLatencyMs: Long = 0,
    val totalBytesTransferred: Long = 0L,
    val logMessages: List<String> = emptyList(),
    val targetPoolSize: Int = 0
)

/**
 * Entity for saving high-performance IPs in Room Database
 */
@Entity(tableName = "saved_ips")
data class ScannedIpEntity(
    @PrimaryKey val ip: String,
    val dataCenter: String,
    val region: String,
    val city: String,
    val latencyMs: Long,
    val testedAt: Long,
    val ipVersion: String,
    val isFavorite: Boolean = true,
    val port: Int = 443
)

/**
 * Entity for recording scan session history
 */
@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val ipType: String,
    val totalScanned: Int,
    val validFound: Int,
    val bestIp: String,
    val bestLatencyMs: Long,
    val bestColo: String
)
