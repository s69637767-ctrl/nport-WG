package com.nport.wg.vless

import android.net.Uri
import android.util.Log
import org.json.JSONObject
import java.net.URL

/**
 * VLESS configuration importer
 * Supports importing from:
 * - vless:// URI
 * - QR code
 * - Subscription URL
 * - JSON config
 */
class VlessImporter {

    /**
     * Import VLESS configuration from URI
     */
    fun fromUri(uri: String): Result<VlessConfig> {
        return try {
            val config = VlessConfig.fromUri(uri)
            if (config != null) {
                Result.success(config)
            } else {
                Result.failure(Exception("Invalid VLESS URI format"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import VLESS from subscription URL
     * Returns list of VLESS configurations
     */
    suspend fun fromSubscription(subscriptionUrl: String): Result<List<VlessConfig>> {
        return try {
            val url = URL(subscriptionUrl)
            val content = url.readText()
            
            // Try to parse as base64 encoded list
            val configs = try {
                val decoded = String(java.util.Base64.getDecoder().decode(content.trim()))
                decoded.lines()
                    .filter { it.startsWith("vless://") }
                    .mapNotNull { VlessConfig.fromUri(it) }
            } catch (e: Exception) {
                // Try to parse as plain text list
                content.lines()
                    .filter { it.startsWith("vless://") }
                    .mapNotNull { VlessConfig.fromUri(it) }
            }
            
            if (configs.isNotEmpty()) {
                Result.success(configs)
            } else {
                Result.failure(Exception("No valid VLESS configurations found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import VLESS from QR code content
     */
    fun fromQrCode(qrContent: String): Result<VlessConfig> {
        return fromUri(qrContent)
    }

    /**
     * Import VLESS from JSON
     */
    fun fromJson(json: String): Result<VlessConfig> {
        return try {
            val obj = JSONObject(json)
            
            val config = VlessConfig(
                uuid = obj.getString("uuid"),
                server = obj.getString("server"),
                port = obj.getInt("port"),
                encryption = obj.optString("encryption", "none"),
                flow = obj.optString("flow", ""),
                security = obj.optString("security", "reality"),
                type = obj.optString("type", "tcp"),
                headerType = obj.optString("headerType", ""),
                host = obj.optString("host", ""),
                path = obj.optString("path", ""),
                sni = obj.optString("sni", ""),
                fingerprint = obj.optString("fingerprint", "chrome"),
                publicKey = obj.optString("publicKey", ""),
                shortId = obj.optString("shortId", ""),
                spiderX = obj.optString("spiderX", ""),
                name = obj.optString("name", "")
            )
            
            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate VLESS configuration
     */
    fun validate(config: VlessConfig): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate UUID
        if (config.uuid.isEmpty()) {
            errors.add("UUID is required")
        } else if (!isValidUuid(config.uuid)) {
            errors.add("Invalid UUID format")
        }
        
        // Validate server
        if (config.server.isEmpty()) {
            errors.add("Server address is required")
        }
        
        // Validate port
        if (config.port < 1 || config.port > 65535) {
            errors.add("Port must be between 1 and 65535")
        }
        
        // Validate Reality parameters
        if (config.security == "reality") {
            if (config.publicKey.isEmpty()) {
                errors.add("Public key is required for Reality")
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    private fun isValidUuid(uuid: String): Boolean {
        val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)
        return uuidRegex.matches(uuid)
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)
