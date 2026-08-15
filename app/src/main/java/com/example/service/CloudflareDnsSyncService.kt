package com.example.service

import com.example.data.model.CfDnsRuleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class CloudflareDnsSyncService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun getIpType(ip: String): String? {
        return try {
            val address = InetAddress.getByName(ip)
            if (address.address.size == 4) "A" else "AAAA"
        } catch (e: Exception) {
            null
        }
    }

    suspend fun syncDnsRecord(rule: CfDnsRuleEntity, targetIp: String): SyncResult {
        return syncDnsRecords(rule, listOf(targetIp))
    }

    suspend fun syncDnsRecords(rule: CfDnsRuleEntity, targetIps: List<String>): SyncResult = withContext(Dispatchers.IO) {
        val desiredIps = targetIps.distinct().take(rule.maxIpCount.coerceAtLeast(1))
        if (desiredIps.isEmpty()) {
            return@withContext SyncResult.Error("No target IP addresses provided for sync")
        }

        if (rule.cfZoneId.isBlank() || rule.cfRecordName.isBlank() || rule.cfApiKey.isBlank()) {
            return@withContext SyncResult.Error("Missing Zone ID, Record Name, or API Key")
        }

        val baseUrl = "https://api.cloudflare.com/client/v4/zones/${rule.cfZoneId.trim()}/dns_records"

        fun applyAuth(builder: Request.Builder) {
            if (rule.cfEmail.isNotBlank()) {
                builder.addHeader("X-Auth-Email", rule.cfEmail.trim())
                builder.addHeader("X-Auth-Key", rule.cfApiKey.trim())
            } else {
                builder.addHeader("Authorization", "Bearer ${rule.cfApiKey.trim()}")
            }
        }

        try {
            // 1. Fetch existing DNS records for this record name
            val getUrl = "$baseUrl?name=${rule.cfRecordName.trim()}&per_page=100"
            val getRequest = Request.Builder().url(getUrl).get().apply { applyAuth(this) }.build()
            var isGetSuccessful = false
            var getStatusCode = 0
            var getResponseBody = ""

            httpClient.newCall(getRequest).execute().use { getResponse ->
                isGetSuccessful = getResponse.isSuccessful
                getStatusCode = getResponse.code
                getResponseBody = getResponse.body?.string() ?: ""
            }

            if (!isGetSuccessful || getResponseBody.isBlank()) {
                return@withContext SyncResult.Error("Failed to fetch DNS records (HTTP $getStatusCode)")
            }

            val getJson = JSONObject(getResponseBody)
            if (!getJson.optBoolean("success", false)) {
                val errors = getJson.optJSONArray("errors")?.toString() ?: "Unknown error"
                return@withContext SyncResult.Error("Cloudflare API error: $errors")
            }

            val recordsArray = getJson.optJSONArray("result")
            val desiredSet = desiredIps.toSet()
            val existingMatchingIps = mutableSetOf<String>()
            val recordsToDelete = mutableListOf<String>()

            if (recordsArray != null) {
                for (i in 0 until recordsArray.length()) {
                    val record = recordsArray.getJSONObject(i)
                    val rType = record.optString("type")
                    val rContent = record.optString("content")
                    val rId = record.optString("id")

                    // Match A or AAAA records
                    if (rType.equals("A", ignoreCase = true) || rType.equals("AAAA", ignoreCase = true)) {
                        if (desiredSet.contains(rContent) && !existingMatchingIps.contains(rContent)) {
                            // Valid matching record!
                            existingMatchingIps.add(rContent)
                        } else {
                            // Extra record or old IP not in desired set -> mark for deletion
                            recordsToDelete.add(rId)
                        }
                    }
                }
            }

            // 2. Delete extra / outdated records
            var deletedCount = 0
            val deleteErrors = mutableListOf<String>()
            for (rId in recordsToDelete) {
                val delUrl = "$baseUrl/$rId"
                val delRequest = Request.Builder()
                    .url(delUrl)
                    .delete()
                    .header("Content-Type", "application/json")
                    .apply { applyAuth(this) }
                    .build()
                
                try {
                    httpClient.newCall(delRequest).execute().use { delResp ->
                        val respBody = delResp.body?.string() ?: ""
                        if (delResp.isSuccessful) {
                            val json = JSONObject(respBody)
                            if (json.optBoolean("success", false)) {
                                deletedCount++
                            } else {
                                val err = json.optJSONArray("errors")?.toString() ?: "Unknown API Error"
                                deleteErrors.add("CF Error: $err")
                            }
                        } else {
                            deleteErrors.add("HTTP ${delResp.code}")
                        }
                    }
                } catch (e: Exception) {
                    deleteErrors.add(e.message?.take(30) ?: "Exception")
                }
            }

            // 3. Create missing desired IPs
            val ipsToCreate = desiredIps.filter { !existingMatchingIps.contains(it) }
            var createdCount = 0
            val createErrors = mutableListOf<String>()

            for (ip in ipsToCreate) {
                val recordType = getIpType(ip) ?: "A"
                val postData = JSONObject().apply {
                    put("type", recordType)
                    put("name", rule.cfRecordName.trim())
                    put("content", ip.trim())
                    put("ttl", 1)
                    put("proxied", false)
                }

                val postRequest = Request.Builder()
                    .url(baseUrl)
                    .header("Content-Type", "application/json")
                    .apply { applyAuth(this) }
                    .post(postData.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                try {
                    httpClient.newCall(postRequest).execute().use { postResponse ->
                        val postResponseBody = postResponse.body?.string() ?: ""
                        if (postResponse.isSuccessful) {
                            val postJson = JSONObject(postResponseBody)
                            if (postJson.optBoolean("success", false)) {
                                createdCount++
                            } else {
                                val err = postJson.optJSONArray("errors")?.toString() ?: "Unknown API Error"
                                createErrors.add("CF Error: $err")
                            }
                        } else {
                            createErrors.add("HTTP ${postResponse.code}")
                        }
                    }
                } catch (e: Exception) {
                    createErrors.add(e.message?.take(30) ?: "Exception")
                }
            }

            val actions = mutableListOf<String>()
            if (createdCount > 0) actions.add("Added $createdCount")
            if (deletedCount > 0) actions.add("Removed $deletedCount extra")
            if (createErrors.isNotEmpty()) actions.add("Add Err: ${createErrors.joinToString("|")}")
            if (deleteErrors.isNotEmpty()) actions.add("Del Err: ${deleteErrors.joinToString("|")}")
            if (existingMatchingIps.isNotEmpty()) actions.add("${existingMatchingIps.size} matched")

            val actionSummary = if (actions.isNotEmpty()) " (${actions.joinToString(", ")})" else ""
            val ipListStr = desiredIps.joinToString(", ")

            return@withContext SyncResult.Success("Synced ${desiredIps.size} IP(s) [$ipListStr] -> ${rule.cfRecordName}$actionSummary")

        } catch (e: Exception) {
            return@withContext SyncResult.Error("Sync error: ${e.localizedMessage ?: "Network exception"}")
        }
    }
}

sealed class SyncResult {
    data class Success(val message: String) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
