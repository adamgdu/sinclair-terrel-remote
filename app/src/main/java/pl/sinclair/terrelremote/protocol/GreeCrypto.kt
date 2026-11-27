package pl.sinclair.terrelremote.protocol

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedPack(
    val pack: String,
    val tag: String? = null
)

object GreeCrypto {
    const val GENERIC_KEY_ECB = "a3K8Bx%2r8Y7#xDh"
    const val GENERIC_KEY_GCM = "{yxAHAY_Lm6pbC/<"

    private val GCM_NONCE = byteArrayOf(
        0x54, 0x40, 0x78, 0x44,
        0x49, 0x67, 0x5a, 0x51,
        0x6c, 0x5e, 0x63, 0x13
    )
    private val GCM_AAD = "qualcomm-test".toByteArray(Charsets.UTF_8)

    fun encrypt(plainJson: String, key: String, useGcm: Boolean): EncryptedPack {
        return if (useGcm) {
            encryptGcm(plainJson, key)
        } else {
            EncryptedPack(pack = encryptEcb(plainJson, key), tag = null)
        }
    }

    fun decrypt(packBase64: String, key: String, useGcm: Boolean, tagBase64: String?): String {
        return if (useGcm && !tagBase64.isNullOrEmpty()) {
            decryptGcm(packBase64, tagBase64, key)
        } else {
            decryptEcb(packBase64, key)
        }
    }

    private fun encryptEcb(plainText: String, key: String): String {
        val keySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun decryptEcb(base64Pack: String, key: String): String {
        val keySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        val decoded = Base64.decode(base64Pack, Base64.DEFAULT)
        val decrypted = cipher.doFinal(decoded)
        return String(decrypted, Charsets.UTF_8)
    }

    private fun encryptGcm(plainText: String, key: String): EncryptedPack {
        val keySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, GCM_NONCE)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        cipher.updateAAD(GCM_AAD)
        val fullOutput = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val cipherBytes = fullOutput.copyOfRange(0, fullOutput.size - 16)
        val tagBytes = fullOutput.copyOfRange(fullOutput.size - 16, fullOutput.size)
        return EncryptedPack(
            pack = Base64.encodeToString(cipherBytes, Base64.NO_WRAP),
            tag = Base64.encodeToString(tagBytes, Base64.NO_WRAP)
        )
    }

    private fun decryptGcm(base64Pack: String, base64Tag: String, key: String): String {
        val keySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, GCM_NONCE)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        cipher.updateAAD(GCM_AAD)
        val cipherBytes = Base64.decode(base64Pack, Base64.DEFAULT)
        val tagBytes = Base64.decode(base64Tag, Base64.DEFAULT)
        val combined = ByteArray(cipherBytes.size + tagBytes.size)
        System.arraycopy(cipherBytes, 0, combined, 0, cipherBytes.size)
        System.arraycopy(tagBytes, 0, combined, cipherBytes.size, tagBytes.size)
        val decrypted = cipher.doFinal(combined)
        return String(decrypted, Charsets.UTF_8)
    }
}
