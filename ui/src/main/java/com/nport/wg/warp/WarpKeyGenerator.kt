package com.nport.wg.warp

import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.SecureRandom
import java.util.Base64

/**
 * Generates WireGuard keys (Curve25519/X25519)
 * Uses Bouncy Castle for cryptographic operations
 */
object WarpKeyGenerator {

    private val secureRandom = SecureRandom()

    /**
     * Generate a new WireGuard key pair using X25519
     */
    fun generateKeyPair(): KeyPair {
        val generator = X25519KeyPairGenerator()
        generator.init(X25519KeyGenerationParameters(secureRandom))
        
        val keyPair: AsymmetricCipherKeyPair = generator.generateKeyPair()
        
        val privateKey = keyPair.private as X25519PrivateKeyParameters
        val publicKey = keyPair.public as X25519PublicKeyParameters
        
        return KeyPair(
            privateKey = Base64.getEncoder().withoutPadding().encodeToString(privateKey.encoded),
            publicKey = Base64.getEncoder().withoutPadding().encodeToString(publicKey.encoded)
        )
    }

    data class KeyPair(
        val privateKey: String,
        val publicKey: String
    )
}
