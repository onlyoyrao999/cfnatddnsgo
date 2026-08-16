import re

with open('./app/src/main/java/com/example/data/network/CloudflareCidrs.kt', 'r') as f:
    content = f.read()

# Replace the fetchLocalIps implementation to read from ips_v6
new_logic = """
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
"""

content = re.sub(r'suspend fun fetchLocalIps.*?\}\n\}', new_logic.strip() + '\n}', content, flags=re.DOTALL | re.MULTILINE)

with open('./app/src/main/java/com/example/data/network/CloudflareCidrs.kt', 'w') as f:
    f.write(content)
