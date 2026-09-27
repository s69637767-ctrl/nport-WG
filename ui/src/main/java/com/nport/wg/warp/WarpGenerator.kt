package com.nport.wg.warp

import com.nport.wg.models.WarpAccount
import com.nport.wg.models.WarpConfig
import com.nport.wg.models.WarpLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WARP configuration generator
 * Generates WireGuard configurations from Cloudflare WARP
 */
class WarpGenerator {

    private val apiClient = WarpApiClient()
    private val keyGenerator = WarpKeyGenerator

    /**
     * Generate a new WARP configuration
     */
    suspend fun generate(): Result<WarpConfig> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Generate WireGuard keys
            val keyPair = keyGenerator.generateKeyPair()
            
            // Step 2: Register device with Cloudflare
            val accountResult = apiClient.registerDevice(keyPair)
            if (accountResult.isFailure) {
                return@withContext Result.failure(
                    accountResult.exceptionOrNull() ?: Exception("Failed to register device")
                )
            }
            
            val account = accountResult.getOrThrow()
            
            // Step 3: Get WARP configuration
            val configResult = apiClient.getConfig(account)
            if (configResult.isFailure) {
                return@withContext Result.failure(
                    configResult.exceptionOrNull() ?: Exception("Failed to get config")
                )
            }
            
            Result.success(configResult.getOrThrow())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate WARP configuration for specific location
     */
    suspend fun generateForLocation(location: WarpLocation): Result<WarpConfig> = withContext(Dispatchers.IO) {
        try {
            // Generate base config
            val baseConfig = generate().getOrThrow()
            
            // Update location (requires Warp+ subscription)
            val account = WarpAccount(
                id = "", // Would be stored from previous step
                token = "",
                publicKey = baseConfig.publicKey,
                privateKey = baseConfig.privateKey
            )
            
            apiClient.setLocation(account, location)
            
            Result.success(baseConfig.copy(location = location))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get available WARP locations
     */
    suspend fun getAvailableLocations(): Result<List<WarpLocation>> {
        return apiClient.getLocations()
    }

    /**
     * Validate WARP configuration
     */
    fun validateConfig(config: WarpConfig): Boolean {
        return try {
            // Basic validation
            config.privateKey.isNotBlank() &&
            config.publicKey.isNotBlank() &&
            config.endpoint.isNotBlank() &&
            config.clientIp.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }
}
