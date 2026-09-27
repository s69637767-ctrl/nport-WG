package com.nport.wg.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data model for Cloudflare WARP configuration
 */
@Parcelize
data class WarpConfig(
    val privateKey: String,
    val publicKey: String,
    val endpoint: String,
    val clientIp: String,
    val reserved: List<Int>,
    val mtu: Int = 1280,
    val location: WarpLocation? = null
) : Parcelable {

    /**
     * Convert WARP config to WireGuard format
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
 * WARP location data
 */
@Parcelize
data class WarpLocation(
    val country: String,
    val city: String,
    val code: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Parcelable

/**
 * WARP account registration data
 */
data class WarpAccount(
    val id: String,
    val token: String,
    val publicKey: String,
    val privateKey: String,
    val createdAt: Long = System.currentTimeMillis()
)
