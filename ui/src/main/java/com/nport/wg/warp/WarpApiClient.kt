package com.nport.wg.warp

import com.nport.wg.models.WarpAccount
import com.nport.wg.models.WarpConfig
import com.nport.wg.models.WarpLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection

/**
 * Cloudflare WARP API client
 * Based on: https://github.com/ViRb3/wgcf and https://github.com/badafans/warp-reg
 */
class WarpApiClient {

    companion object {
        private const val API_BASE = "https://api.cloudflareclient.com"
        private const val API_VERSION = "v0a2322" // API version (changes over time)
        private const val USER_AGENT = "okhttp/3.12.1"
        
        // WARP endpoints
        private val WARP_ENDPOINTS = listOf(
            "engage.cloudflareclient.com:2408",
            "162.159.192.0:2408",
            "162.159.193.0:2408",
            "162.159.195.0:2408",
            "162.159.200.0:2408",
            "188.114.96.0:2408",
            "188.114.97.0:2408"
        )
    }

    /**
     * Register a new WARP device
     */
    suspend fun registerDevice(keyPair: WarpKeyGenerator.KeyPair): Result<WarpAccount> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$API_BASE/$API_VERSION/reg")
            val connection = url.openConnection() as HttpsURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonBody = JSONObject().apply {
                put("install_id", UUID.randomUUID().toString())
                put("tos", getCurrentTimestamp())
                put("key", keyPair.publicKey)
                put("type", "Android")
                put("model", "Android")
                put("locale", "en_US")
            }

            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
            }

            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)

            val account = WarpAccount(
                id = json.getString("id"),
                token = json.getString("token"),
                publicKey = keyPair.publicKey,
                privateKey = keyPair.privateKey
            )

            Result.success(account)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get WARP configuration
     */
    suspend fun getConfig(account: WarpAccount): Result<WarpConfig> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$API_BASE/$API_VERSION/reg/${account.id}")
            val connection = url.openConnection() as HttpsURLConnection
            
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Authorization", "Bearer ${account.token}")

            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)

            val config = WarpConfig(
                privateKey = account.privateKey,
                publicKey = json.getJSONObject("config").getJSONObject("peers")
                    .getJSONArray("peers").getJSONObject(0)
                    .getString("public_key"),
                endpoint = WARP_ENDPOINTS.random(),
                clientIp = json.getJSONObject("config").getJSONObject("interface")
                    .getString("addresses").let { 
                        JSONObject(it).getJSONArray("v4").getString(0) 
                    },
                reserved = extractReserved(json)
            )

            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get available WARP locations
     */
    suspend fun getLocations(): Result<List<WarpLocation>> = withContext(Dispatchers.IO) {
        try {
            // Simplified - in reality would need to fetch from Cloudflare's endpoint list
            val locations = listOf(
                WarpLocation("United States", "New York", "US", 40.7128, -74.0060),
                WarpLocation("United States", "Los Angeles", "US", 34.0522, -118.2437),
                WarpLocation("Germany", "Frankfurt", "DE", 50.1109, 8.6821),
                WarpLocation("Netherlands", "Amsterdam", "NL", 52.3676, 4.9041),
                WarpLocation("United Kingdom", "London", "GB", 51.5074, -0.1278),
                WarpLocation("Japan", "Tokyo", "JP", 35.6762, 139.6503),
                WarpLocation("Singapore", "Singapore", "SG", 1.3521, 103.8198),
                WarpLocation("Australia", "Sydney", "AU", -33.8688, 151.2093)
            )
            Result.success(locations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Set WARP location (requires Warp+ subscription)
     */
    suspend fun setLocation(account: WarpAccount, location: WarpLocation): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // This would require Warp+ subscription and proper API call
            // Placeholder for now
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getCurrentTimestamp(): String {
        return System.currentTimeMillis().toString()
    }

    private fun extractReserved(json: JSONObject): List<Int> {
        return try {
            val reservedJson = json.getJSONObject("config").getJSONObject("client_id")
            listOf(
                reservedJson.getInt("0"),
                reservedJson.getInt("1"),
                reservedJson.getInt("2")
            )
        } catch (e: Exception) {
            emptyList()
        }
    }
}
