package com.example.armazenadorsenha.repository

import com.example.armazenadorsenha.DAO.UserDao
import com.example.armazenadorsenha.model.UserConfig

class UserRepository(private val userDao: UserDao) {

    suspend fun isUserRegistered(): Boolean {
        return userDao.isUserRegistered() > 0
    }

    suspend fun getUserConfig(): UserConfig? {
        return userDao.getUserConfig()
    }

    suspend fun registerUser(config: UserConfig) {
        userDao.insertUserConfig(config)
    }

    suspend fun updateBiometricSetting(enabled: Boolean) {
        userDao.updateBiometricSetting(enabled)
    }

    suspend fun getUserEmail(): String? {
        // Assume que getUserConfig() já retorna o objeto UserConfig com o campo 'email'
        return userDao.getUserConfig()?.email
    }
}