package com.nport.wg.amnezia

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * AmneziaWG configuration model
 * AmneziaWG is a modified WireGuard with obfuscation for bypassing DPI
 * 
 * Key differences from standard WireGuard:
 * - Packet size randomization
 * - Header obfuscation
 * - Magic number customization
 * - Improved resistance to DPI detection
 */
@Parcelize
data class AmneziaWarpConfig(
    // Standard WireGuard fields
    val privateKey: String,
    val publicKey: String,
    val endpoint: String,
    val clientIp: String,
    
    // AmneziaWG specific obfuscation parameters
    val Jc: Int = 3,              // Junk packet count
    val Jmin: Int = 50,           // Minimum junk packet size
    val Jmax: Int = 1000,         // Maximum junk packet size
    val S1: Int = 15,             // Initial packet size
    val S2: Int = 16,             // Subsequent packet size
    val H1: Long = 1123615445,    // Magic number 1
    val H2: Long = 1455708525,    // Magic number 2
    val H3: Long = 1923390209,    // Magic number 3
    val H4: Long = 324057139,     // Magic number 4
    
    // WARP specific
    val reserved: List<Int> = emptyList(),
    val mtu: Int = 1280,
    val location: AmneziaWarpLocation? = null
) : Parcelable {

    /**
     * Convert to AmneziaWG config format
     */
    fun toAmneziaConfig(): String {
        return """
            [Interface]
            PrivateKey = $privateKey
            Address = $clientIp/32
            DNS = 1.1.1.1
            MTU = $mtu
            Jc = $Jc
            Jmin = $Jmin
            Jmax = $Jmax
            S1 = $S1
            S2 = $S2
            H1 = $H1
            H2 = $H2
            H3 = $H3
            H4 = $H4
            
            [Peer]
            PublicKey = $publicKey
            Endpoint = $endpoint
            AllowedIPs = 0.0.0.0/0, ::/0
            ${if (reserved.isNotEmpty()) "Reserved = ${reserved.joinToString(", ")}" else ""}
        """.trimIndent()
    }

    /**
     * Convert to standard WireGuard format (for compatibility)
     */
    fun toWireGuardConfig(): String {
        return """
            [Interface]
            PrivateKey = $privateKey
            Address = $clientIp/32
            DNS = 1.1.1.1
            MTU = $mtu
            
            [Peer]
            PublicKey = $publicKey
            Endpoint = $endpoint
            AllowedIPs = 0.0.0.0/0, ::/0
            ${if (reserved.isNotEmpty()) "Reserved = ${reserved.joinToString(", ")}" else ""}
        """.trimIndent()
    }
}

/**
 * Amnezia WARP location data
 */
@Parcelize
data class AmneziaWarpLocation(
    val country: String,
    val city: String,
    val code: String,
    val endpoint: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Parcelable

/**
 * AmneziaWG obfuscation presets
 */
object AmneziaObfuscationPresets {
    
    /**
     * Low obfuscation - better performance, easier to detect
     */
    val LOW = ObfuscationPreset(
        Jc = 1,
        Jmin = 10,
        Jmax = 100,
        S1 = 10,
        S2 = 12,
        H1 = 1123615445,
        H2 = 1455708525,
        H3 = 1923390209,
        H4 = 324057139
    )
    
    /**
     * Medium obfuscation - balanced
     */
    val MEDIUM = ObfuscationPreset(
        Jc = 3,
        Jmin = 50,
        Jmax = 1000,
        S1 = 15,
        S2 = 16,
        H1 = 1123615445,
        H2 = 1455708525,
        H3 = 1923390209,
        H4 = 324057139
    )
    
    /**
     * High obfuscation - maximum stealth, lower performance
     */
    val HIGH = ObfuscationPreset(
        Jc = 5,
        Jmin = 100,
        Jmax = 2000,
        S1 = 20,
        S2 = 25,
        H1 = 843375324,
        H2 = 1953257864,
        H3 = 2538456892,
        H4 = 342175629
    )
    
    /**
     * Custom random magic numbers for better obfuscation
     */
    fun random(): ObfuscationPreset {
        val random = java.security.SecureRandom()
        return ObfuscationPreset(
            Jc = random.nextInt(5) + 1,
            Jmin = random.nextInt(100) + 10,
            Jmax = random.nextInt(2000) + 500,
            S1 = random.nextInt(20) + 10,
            S2 = random.nextInt(30) + 10,
            H1 = random.nextLong(),
            H2 = random.nextLong(),
            H3 = random.nextLong(),
            H4 = random.nextLong()
        )
    }
}

data class ObfuscationPreset(
    val Jc: Int,
    val Jmin: Int,
    val Jmax: Int,
    val S1: Int,
    val S2: Int,
    val H1: Long,
    val H2: Long,
    val H3: Long,
    val H4: Long
)
