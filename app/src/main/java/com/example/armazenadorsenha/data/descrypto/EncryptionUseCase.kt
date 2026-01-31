package com.example.armazenadorsenha.data.descrypto

import com.example.armazenadorsenha.data.crypto.EncryptionHelper

/**
 * Use Case para isolar a lógica de criptografia.
 */
class EncryptionUseCase {

    fun encrypt(plainText: String, masterPassword: String): Pair<String, String> {
        return EncryptionHelper.encrypt(plainText, masterPassword)
    }

    fun decrypt(encryptedBase64: String, ivBase64: String, masterPassword: String): String {
        return EncryptionHelper.decrypt(encryptedBase64, ivBase64, masterPassword)
    }
}