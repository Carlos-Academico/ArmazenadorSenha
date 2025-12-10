package com.example.armazenadorsenha.data.crypto

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionHelper {

    // Simulação de algoritmo e modo
    private const val ALGORITHM = "AES"
    private const val MODE = "AES/CBC/PKCS5Padding"
    private const val KEY_SIZE = 16 // 128 bits para AES

    // Funções para criar uma chave a partir da Master Password
    private fun createKey(masterKey: String): SecretKeySpec {
        // Em um app real, o hash e o 'salt' são cruciais aqui.
        // Simplificado para demonstração:
        val keyBytes = masterKey.toByteArray(Charsets.UTF_8).copyOf(KEY_SIZE)
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    /**
     * Criptografa a senha em texto puro.
     * @return Pair<encryptedPasswordBase64, ivBase64>
     */
    fun encrypt(plainText: String, masterPassword: String): Pair<String, String> {
        val cipher = Cipher.getInstance(MODE)
        val key = createKey(masterPassword)

        cipher.init(Cipher.ENCRYPT_MODE, key)

        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val ivBytes = cipher.iv

        val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(ivBytes, Base64.NO_WRAP)

        return Pair(encryptedBase64, ivBase64)
    }

    /**
     * Descriptografa a senha.
     * @return Senha em texto puro.
     */
    fun decrypt(encryptedBase64: String, ivBase64: String, masterPassword: String): String {
        val cipher = Cipher.getInstance(MODE)
        val key = createKey(masterPassword)

        val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val ivBytes = Base64.decode(ivBase64, Base64.NO_WRAP)

        val ivSpec = IvParameterSpec(ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}