package com.nport.wg.amnezia

import com.nport.wg.warp.WarpApiClient
import com.nport.wg.warp.WarpKeyGenerator
import com.nport.wg.models.WarpAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Amnezia WARP configuration generator
 * Generates WireGuard configs with obfuscation for bypassing DPI
 */
class AmneziaWarpGenerator {

    private val apiClient = WarpApiClient()
    private val keyGenerator = WarpKeyGenerator

    /**
     * Generate Amnezia WARP configuration with obfuscation
     */
    suspend fun generate(
        obfuscationPreset: ObfuscationPreset = AmneziaObfuscationPresets.MEDIUM
    ): Result<AmneziaWarpConfig> = withContext(Dispatchers.IO) {
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
            
            val baseConfig = configResult.getOrThrow()
            
            // Step 4: Create Amnezia WARP config with obfuscation
            val amneziaConfig = AmneziaWarpConfig(
                privateKey = baseConfig.privateKey,
                publicKey = baseConfig.publicKey,
                endpoint = baseConfig.endpoint,
                clientIp = baseConfig.clientIp,
                reserved = baseConfig.reserved,
                Jc = obfuscationPreset.Jc,
                Jmin = obfuscationPreset.Jmin,
                Jmax = obfuscationPreset.Jmax,
                S1 = obfuscationPreset.S1,
                S2 = obfuscationPreset.S2,
                H1 = obfuscationPreset.H1,
                H2 = obfuscationPreset.H2,
                H3 = obfuscationPreset.H3,
                H4 = obfuscationPreset.H4
            )
            
            Result.success(amneziaConfig)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate Amnezia WARP for specific location
     */
    suspend fun generateForLocation(
        location: AmneziaWarpLocation,
        obfuscationPreset: ObfuscationPreset = AmneziaObfuscationPresets.MEDIUM
    ): Result<AmneziaWarpConfig> = withContext(Dispatchers.IO) {
        try {
            val baseConfig = generate(obfuscationPreset).getOrThrow()
            
            Result.success(baseConfig.copy(
                endpoint = location.endpoint,
                location = location
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get available Amnezia WARP locations
     */
    suspend fun getAvailableLocations(): Result<List<AmneziaWarpLocation>> {
        // Use different endpoints for Amnezia WARP
        val locations = listOf(
            AmneziaWarpLocation("Netherlands", "Amsterdam", "NL", "162.159.192.0:2408"),
            AmneziaWarpLocation("Germany", "Frankfurt", "DE", "162.159.193.0:2408"),
            AmneziaWarpLocation("Finland", "Helsinki", "FI", "162.159.195.0:2408"),
            AmneziaWarpLocation("Russia", "Moscow", "RU", "162.159.200.0:2408"),
            AmneziaWarpLocation("Switzerland", "Zurich", "CH", "188.114.96.0:2408"),
            AmneziaWarpLocation("Ukraine", "Kyiv", "UA", "188.114.97.0:2408")
        )
        return Result.success(locations)
    }

    /**
     * Test if Amnezia WARP can bypass DPI
     */
    fun testObfuscation(config: AmneziaWarpConfig): ObfuscationTestResult {
        // Simple heuristic for DPI bypass capability
        val score = when {
            config.Jc >= 5 && config.Jmax >= 2000 -> ObfuscationLevel.HIGH
            config.Jc >= 3 && config.Jmax >= 1000 -> ObfuscationLevel.MEDIUM
            config.Jc >= 1 -> ObfuscationLevel.LOW
            else -> ObfuscationLevel.NONE
        }
        
        return ObfuscationTestResult(
            level = score,
            canBypassDPI = score != ObfuscationLevel.NONE,
            recommendation = when (score) {
                ObfuscationLevel.HIGH -> "Excellent obfuscation. Should bypass most DPI systems."
                ObfuscationLevel.MEDIUM -> "Good obfuscation. Should work in most regions."
                ObfuscationLevel.LOW -> "Basic obfuscation. May work in some regions."
                ObfuscationLevel.NONE -> "No obfuscation. Not recommended for restricted regions."
            }
        )
    }
}

enum class ObfuscationLevel {
    NONE, LOW, MEDIUM, HIGH
}

data class ObfuscationTestResult(
    val level: ObfuscationLevel,
    val canBypassDPI: Boolean,
    val recommendation: String
)
