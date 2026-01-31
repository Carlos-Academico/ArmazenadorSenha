package com.example.armazenadorsenha.DAO

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.armazenadorsenha.model.PasswordData
import com.example.armazenadorsenha.model.UserConfig

// Certifique-se de que a entidade PasswordData esteja no local correto
@Database(entities = [PasswordData::class, UserConfig::class], version = 2, exportSchema = false)
abstract class PasswordDatabase : RoomDatabase() {

    abstract fun passwordDao(): PasswordDao
    abstract fun userDao(): UserDao // NOVO DAO

    companion object {
        @Volatile
        private var INSTANCE: PasswordDatabase? = null

        fun getDatabase(context: Context): PasswordDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PasswordDatabase::class.java,
                    "password_vault_db"
                )
                    .fallbackToDestructiveMigration() // Apenas para desenvolvimento, remova em produção
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}