package com.nport.wg.vless

/**
 * VLESS configuration model
 * Supports VLESS protocol with XTLS, Reality, and WebSocket transport
 */
data class VlessConfig(
    val uuid: String,
    val server: String,
    val port: Int,
    val encryption: String = "none",
    val flow: String = "", // xtls-rprx-vision, xtls-rprx-vision-udp443
    val security: String = "reality", // reality, tls, none
    val type: String = "tcp", // tcp, ws, grpc, http
    val headerType: String = "",
    val host: String = "",
    val path: String = "",
    val sni: String = "",
    val fingerprint: String = "chrome",
    // Reality specific
    val publicKey: String = "",
    val shortId: String = "",
    val spiderX: String = "",
    // Name and remarks
    val name: String = "",
    val remarks: String = ""
) {

    /**
     * Parse VLESS URI: vless://uuid@server:port?params#name
     */
    companion object {
        fun fromUri(uri: String): VlessConfig? {
            return try {
                if (!uri.startsWith("vless://")) return null
                
                val parts = uri.removePrefix("vless://").split("#")
                val mainPart = parts[0]
                val name = if (parts.size > 1) java.net.URLDecoder.decode(parts[1], "UTF-8") else ""
                
                // Parse uuid@server:port
                val atIndex = mainPart.indexOf('@')
                val uuid = mainPart.substring(0, atIndex)
                
                val serverPort = mainPart.substring(atIndex + 1)
                val colonIndex = serverPort.lastIndexOf(':')
                val server = serverPort.substring(0, colonIndex)
                val port = serverPort.substring(colonIndex + 1).toIntOrNull() ?: 443
                
                // Parse parameters
                val params = mutableMapOf<String, String>()
                val queryIndex = mainPart.indexOf('?')
                if (queryIndex != -1) {
                    val queryString = mainPart.substring(queryIndex + 1)
                    queryString.split("&").forEach { param ->
                        val keyValue = param.split("=")
                        if (keyValue.size == 2) {
                            params[keyValue[0]] = java.net.URLDecoder.decode(keyValue[1], "UTF-8")
                        }
                    }
                }
                
                VlessConfig(
                    uuid = uuid,
                    server = server,
                    port = port,
                    encryption = params["encryption"] ?: "none",
                    flow = params["flow"] ?: "",
                    security = params["security"] ?: "reality",
                    type = params["type"] ?: "tcp",
                    headerType = params["headerType"] ?: "",
                    host = params["host"] ?: "",
                    path = params["path"] ?: "",
                    sni = params["sni"] ?: "",
                    fingerprint = params["fp"] ?: "chrome",
                    publicKey = params["pbk"] ?: "",
                    shortId = params["sid"] ?: "",
                    spiderX = params["spx"] ?: "",
                    name = name
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Convert to VLESS URI
     */
    fun toUri(): String {
        val params = mutableListOf<String>()
        
        if (encryption != "none") params.add("encryption=$encryption")
        if (flow.isNotEmpty()) params.add("flow=$flow")
        params.add("security=$security")
        params.add("type=$type")
        if (headerType.isNotEmpty()) params.add("headerType=$headerType")
        if (host.isNotEmpty()) params.add("host=${java.net.URLEncoder.encode(host, "UTF-8")}")
        if (path.isNotEmpty()) params.add("path=${java.net.URLEncoder.encode(path, "UTF-8")}")
        if (sni.isNotEmpty()) params.add("sni=${java.net.URLEncoder.encode(sni, "UTF-8")}")
        params.add("fp=$fingerprint")
        
        // Reality specific
        if (security == "reality" && publicKey.isNotEmpty()) {
            params.add("pbk=${java.net.URLEncoder.encode(publicKey, "UTF-8")}")
            if (shortId.isNotEmpty()) params.add("sid=$shortId")
            if (spiderX.isNotEmpty()) params.add("spx=${java.net.URLEncoder.encode(spiderX, "UTF-8")}")
        }
        
        val paramString = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
        val namePart = if (name.isNotEmpty()) "#${java.net.URLEncoder.encode(name, "UTF-8")}" else ""
        
        return "vless://$uuid@$server:$port$paramString$namePart"
    }
}
