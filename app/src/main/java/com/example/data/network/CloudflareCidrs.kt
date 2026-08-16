package com.example.data.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import com.example.R

object CloudflareCidrs {
    val DEFAULT_IPV6_CIDRS = listOf(
        "2400:cb00:2048::/48",
        "2400:cb00:2049::/48",
        "2400:cb00:445::/48",
        "2400:cb00:497::/48",
        "2400:cb00:618::/48",
        "2400:cb00:bbd0::/48",
        "2400:cb00:bbd1::/48",
        "2400:cb00:bbd2::/48",
        "2400:cb00:bbd3::/48",
        "2400:cb00:bbd4::/48",
        "2400:cb00:bbd5::/48",
        "2400:cb00:bbd6::/48",
        "2400:cb00:bbd7::/48",
        "2400:cb00:bbd8::/48",
        "2400:cb00:bbd9::/48",
        "2400:cb00:bbda::/48",
        "2400:cb00:bbdb::/48",
        "2400:cb00:bbdc::/48",
        "2400:cb00:bbdd::/48",
        "2400:cb00:bbde::/48",
        "2400:cb00:bbdf::/48",
        "2400:cb00:f00e::/48",
        "2606:4700:0::/48",
        "2606:4700:1::/48",
        "2606:4700:10::/48",
        "2606:4700:100::/48",
        "2606:4700:101::/48",
        "2606:4700:10f::/48",
        "2606:4700:11::/48",
        "2606:4700:12::/48",
        "2606:4700:13::/48",
        "2606:4700:130::/48",
        "2606:4700:131::/48",
        "2606:4700:132::/48",
        "2606:4700:133::/48",
        "2606:4700:134::/48",
        "2606:4700:135::/48",
        "2606:4700:136::/48",
        "2606:4700:137::/48",
        "2606:4700:138::/48",
        "2606:4700:139::/48",
        "2606:4700:13a::/48",
        "2606:4700:13b::/48",
        "2606:4700:13c::/48",
        "2606:4700:13d::/48",
        "2606:4700:13e::/48",
        "2606:4700:13f::/48",
        "2606:4700:14::/48",
        "2606:4700:15::/48",
        "2606:4700:20::/48",
        "2606:4700:3000::/48",
        "2606:4700:4400::/48",
        "2606:4700:4700::/48",
        "2606:4700:8390::/48",
        "2606:4700:85c0::/48",
        "2606:4700:8ca0::/48",
        "2606:4700:8d70::/48",
        "2606:4700:90c0::/48",
        "2606:4700:9640::/48",
        "2606:4700:9760::/48",
        "2606:4700:99e0::/48",
        "2803:f800:50::/48",
        "2a06:98c1:3100::/48",
        "2a06:98c1:50::/48"
    )

    suspend fun fetchLocalIps(context: Context, isIpv6: Boolean): List<String> = withContext(Dispatchers.IO) {
        val ipList = mutableListOf<String>()
        val resourceId = if (isIpv6) R.raw.ips_v6 else R.raw.ips_v4
        try {
            val inputStream = context.resources.openRawResource(resourceId)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.trim()?.let { trimmedLine ->
                    if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                        // Ensure it has a subnet mask
                        if (trimmedLine.contains("/")) {
                           ipList.add(trimmedLine)
                        } else {
                           // default to /24 for IPv4 or /48 for IPv6
                           val suffix = if (isIpv6) "/48" else "/24"
                           ipList.add("$trimmedLine$suffix") 
                        }
                    }
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        if (ipList.isEmpty()) {
            return@withContext if (isIpv6) DEFAULT_IPV6_CIDRS else listOf("1.0.0.0/24")
        }
        
        return@withContext ipList
    }
}
