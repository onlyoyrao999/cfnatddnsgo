package com.example.data.db

import com.example.data.model.CfDnsRuleEntity
import com.example.data.model.ScanHistoryEntity
import com.example.data.model.ScannedIpEntity
import kotlinx.coroutines.flow.Flow

class IpRepository(
    private val scannedIpDao: ScannedIpDao,
    private val scanHistoryDao: ScanHistoryDao,
    private val cfDnsRuleDao: CfDnsRuleDao
) {
    val savedIps: Flow<List<ScannedIpEntity>> = scannedIpDao.getAllSavedIps()
    val favoriteIps: Flow<List<ScannedIpEntity>> = scannedIpDao.getFavoriteIps()
    val scanHistory: Flow<List<ScanHistoryEntity>> = scanHistoryDao.getScanHistory()
    val dnsRules: Flow<List<CfDnsRuleEntity>> = cfDnsRuleDao.getAllRules()

    suspend fun saveIp(entity: ScannedIpEntity) {
        scannedIpDao.insertIp(entity)
        scannedIpDao.trimToTop100Newest()
    }

    suspend fun saveIps(entities: List<ScannedIpEntity>) {
        scannedIpDao.insertIps(entities)
        scannedIpDao.trimToTop100Newest()
    }

    suspend fun toggleFavorite(ip: String, isFavorite: Boolean) {
        scannedIpDao.updateFavorite(ip, isFavorite)
    }

    suspend fun deleteIp(ip: String) {
        scannedIpDao.deleteIpByAddress(ip)
    }

    suspend fun clearSavedIps() {
        scannedIpDao.clearAll()
    }

    suspend fun addHistory(history: ScanHistoryEntity) {
        scanHistoryDao.insertHistory(history)
    }

    suspend fun clearHistory() {
        scanHistoryDao.clearHistory()
    }

    // DNS Sync Rules
    suspend fun getEnabledDnsRules(): List<CfDnsRuleEntity> {
        return cfDnsRuleDao.getEnabledRules()
    }

    suspend fun saveDnsRule(rule: CfDnsRuleEntity): Long {
        return cfDnsRuleDao.insertRule(rule)
    }

    suspend fun updateDnsRule(rule: CfDnsRuleEntity) {
        cfDnsRuleDao.updateRule(rule)
    }

    suspend fun deleteDnsRule(id: Long) {
        cfDnsRuleDao.deleteRuleById(id)
    }

    suspend fun updateDnsRuleSyncResult(id: Long, status: String, ip: String, time: Long) {
        cfDnsRuleDao.updateSyncResult(id, status, ip, time)
    }
}
