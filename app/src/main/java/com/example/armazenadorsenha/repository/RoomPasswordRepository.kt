package com.example.armazenadorsenha.repository

import com.example.armazenadorsenha.DAO.PasswordDao
import com.example.armazenadorsenha.model.PasswordData
import kotlinx.coroutines.flow.Flow

interface PasswordRepository {

    fun getAllPasswords(): Flow<List<PasswordData>>
    suspend fun getPasswordById(itemId: Int): PasswordData?
    suspend fun addPassword(password: PasswordData)
    suspend fun updatePassword(password: PasswordData)
    suspend fun deletePassword(itemId: Int)
}

class RoomPasswordRepository(private val passwordDao: PasswordDao) : PasswordRepository {

    override fun getAllPasswords(): Flow<List<PasswordData>> {
        return passwordDao.getAllPasswords()
    }

    override suspend fun getPasswordById(itemId: Int): PasswordData? {
        return passwordDao.getPasswordById(itemId)
    }

    override suspend fun addPassword(password: PasswordData) {
        passwordDao.insert(password)
    }

    override suspend fun updatePassword(password: PasswordData) {
        passwordDao.update(password)
    }

    override suspend fun deletePassword(itemId: Int) {
        passwordDao.deleteById(itemId)
    }
}