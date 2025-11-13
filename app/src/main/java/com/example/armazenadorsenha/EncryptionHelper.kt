package com.example.armazenadorsenha

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionHelper {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val KEY_SIZE_BYTES = 32

    /**
     * Chave AES-256 de 32 bytes usando SHA-256 da senha mestra.
     */
    private fun deriveKey(masterPassword: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(masterPassword.toByteArray(Charsets.UTF_8))

        // Trunca ou preenche para 32 bytes (256 bits)
        val key = keyBytes.copyOf(KEY_SIZE_BYTES)
        return SecretKeySpec(key, ALGORITHM)
    }

    // --- Criptografar ---
    /**
     * Criptografa o texto. Retorna a senha cifrada e o IV (ambos em Base64).
     */
    fun encrypt(plainText: String, masterPassword: String): Pair<String, String> {
        val key = deriveKey(masterPassword)
        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Conversão para Base64 para salvar como String
        val ivBase64 = Base64.encodeToString(cipher.iv, Base64.DEFAULT)
        val cipherTextBase64 = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)

        return Pair(cipherTextBase64, ivBase64)
    }

    // --- Decriptografar ---
    /**
     * Decriptografa a senha usando a chave derivada da Senha Mestra e o IV.
     * Lança SecurityException se a senha mestre for incorreta.
     */
    fun decrypt(cipherTextBase64: String, ivBase64: String, masterPassword: String): String {
        try {
            val key = deriveKey(masterPassword)
            val iv = Base64.decode(ivBase64, Base64.DEFAULT)
            val cipherText = Base64.decode(cipherTextBase64, Base64.DEFAULT)

            val cipher = Cipher.getInstance(TRANSFORMATION)

            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

            val decryptedBytes = cipher.doFinal(cipherText)
            return String(decryptedBytes, Charsets.UTF_8)

        } catch (e: Exception) {
            // Se a chave for inválida ou os dados estiverem corrompidos
            throw SecurityException("Falha na decriptografia. Senha mestre incorreta ou dados inválidos.")
        }
    }
}