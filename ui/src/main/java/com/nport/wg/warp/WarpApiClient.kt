package com.nport.wg.warp

import android.util.Log
import com.nport.wg.models.WarpAccount
import com.nport.wg.models.WarpConfig
import com.nport.wg.models.WarpLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection
import java.io.IOException

/**
 * Cloudflare WARP API client
 * Based on: https://github.com/ViRb3/wgcf and https://github.com/badafans/warp-reg
 */
class WarpApiClient {

    companion object {
        private const val TAG = "WarpApiClient"
        private const val API_BASE = "https://api.cloudflareclient.com"
        private const val API_VERSION = "v0a2322" // API version (changes over time)
        private const val USER_AGENT = "okhttp/3.12.1"
        private const val CONNECT_TIMEOUT = 15000 // 15 seconds
        private const val READ_TIMEOUT = 15000 // 15 seconds

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
        var connection: HttpsURLConnection? = null
        try {
            Log.d(TAG, "Registering device with public key: ${keyPair.publicKey.take(10)}...")

            val url = URL("$API_BASE/$API_VERSION/reg")
            connection = url.openConnection() as HttpsURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.doOutput = true

            val jsonBody = JSONObject().apply {
                put("install_id", UUID.randomUUID().toString())
                put("tos", getCurrentTimestamp())
                put("key", keyPair.publicKey)
                put("type", "Android")
                put("model", "Android")
                put("locale", "en_US")
            }

            Log.d(TAG, "Sending registration request...")
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")

            if (responseCode != 200 && responseCode != 201) {
                val errorStream = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "Registration failed: $errorStream")
                return@withContext Result.failure(Exception("HTTP $responseCode: $errorStream"))
            }

            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            Log.d(TAG, "Registration successful, account ID: ${json.getString("id")}")

            val account = WarpAccount(
                id = json.getString("id"),
                token = json.getString("token"),
                publicKey = keyPair.publicKey,
                privateKey = keyPair.privateKey
            )

            Result.success(account)
        } catch (e: Exception) {
            Log.e(TAG, "Registration error", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Get WARP configuration
     */
    suspend fun getConfig(account: WarpAccount): Result<WarpConfig> = withContext(Dispatchers.IO) {
        var connection: HttpsURLConnection? = null
        try {
            Log.d(TAG, "Getting config for account: ${account.id}")

            val url = URL("$API_BASE/$API_VERSION/reg/${account.id}")
            connection = url.openConnection() as HttpsURLConnection

            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Authorization", "Bearer ${account.token}")
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT

            Log.d(TAG, "Fetching config...")
            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")

            if (responseCode != 200) {
                val errorStream = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "Get config failed: $errorStream")
                return@withContext Result.failure(Exception("HTTP $responseCode: $errorStream"))
            }

            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            Log.d(TAG, "Config received successfully")

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
            Log.e(TAG, "Get config error", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
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
