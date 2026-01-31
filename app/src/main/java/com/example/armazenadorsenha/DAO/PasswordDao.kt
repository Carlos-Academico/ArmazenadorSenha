package com.example.armazenadorsenha.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.armazenadorsenha.model.PasswordData
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {

    @Query("SELECT * FROM passwords ORDER BY serviceTitle ASC")
    fun getAllPasswords(): Flow<List<PasswordData>>

    @Query("SELECT * FROM passwords WHERE id = :itemId")
    suspend fun getPasswordById(itemId: Int): PasswordData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(password: PasswordData)

    @Update
    suspend fun update(password: PasswordData)

    @Query("DELETE FROM passwords WHERE id = :itemId")
    suspend fun deleteById(itemId: Int)
}