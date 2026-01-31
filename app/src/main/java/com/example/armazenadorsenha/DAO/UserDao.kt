package com.example.armazenadorsenha.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.armazenadorsenha.model.UserConfig

@Dao
interface UserDao {

    // Verifica se o registro principal de usuário existe
    @Query("SELECT COUNT(*) FROM user_config WHERE id = 1")
    suspend fun isUserRegistered(): Int

    // Retorna as configurações, incluindo Hash e Salt
    @Query("SELECT * FROM user_config WHERE id = 1")
    suspend fun getUserConfig(): UserConfig?

    // Insere ou substitui o único registro de configuração de usuário (Cadastro)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserConfig(config: UserConfig)

    // Atualiza apenas o status da biometria
    @Query("UPDATE user_config SET biometricEnabled = :enabled WHERE id = 1")
    suspend fun updateBiometricSetting(enabled: Boolean)
}