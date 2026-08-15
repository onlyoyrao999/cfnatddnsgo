package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cf_dns_rules")
data class CfDnsRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleName: String,         // e.g. "Airport HK Node", "Main Domain"
    val coloFilter: String = "",  // e.g. "HKG", "SJC", or "" (all)
    val cfEmail: String = "",     // Cloudflare account email
    val cfApiKey: String = "",    // Cloudflare Global API Key or Token
    val cfZoneId: String = "",    // Cloudflare Zone ID
    val cfRecordName: String = "",// e.g. hk.example.com
    val maxIpCount: Int = 1,      // Max number of IPs to sync to this domain record
    val isEnabled: Boolean = true,
    val lastSyncStatus: String = "Not synced",
    val lastSyncedIp: String = "",
    val lastSyncTime: Long = 0
)
