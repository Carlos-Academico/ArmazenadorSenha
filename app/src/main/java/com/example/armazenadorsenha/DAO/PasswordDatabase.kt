package com.example.armazenadorsenha.DAO

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.armazenadorsenha.model.PasswordData

// Certifique-se de que a entidade PasswordData esteja no local correto
@Database(entities = [PasswordData::class], version = 1, exportSchema = false)
abstract class PasswordDatabase : RoomDatabase() {

    abstract fun passwordDao(): PasswordDao

    companion object {
        @Volatile
        private var INSTANCE: PasswordDatabase? = null

        fun getDatabase(context: Context): PasswordDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PasswordDatabase::class.java,
                    "password_vault_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}