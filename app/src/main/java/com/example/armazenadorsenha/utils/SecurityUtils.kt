package com.example.armazenadorsenha.utils

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityUtils {

    // --- CONFIGURAÇÃO DE SEGURANÇA ---
    private const val ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val ITERATIONS = 64000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    /**
     * Gera um salt aleatório (16 bytes) e retorna em Base64.
     */
    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    /**
     * Cria o hash da senha usando PBKDF2 com o salt fornecido. Retorna o hash em Base64.
     */
    fun hashPassword(password: String, saltBase64: String): String {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)

        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            ITERATIONS,
            KEY_LENGTH
        )

        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hashBytes = factory.generateSecret(spec).encoded

        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }

    /**
     * Verifica se a senha corresponde ao hash e salt armazenados.
     */
    fun verifyPassword(password: String, storedHash: String, storedSalt: String): Boolean {
        // Gera o hash da senha de entrada usando o salt armazenado
        val hashOfInput = hashPassword(password, storedSalt)

        // Compara o hash gerado com o hash armazenado
        return hashOfInput == storedHash
    }
}