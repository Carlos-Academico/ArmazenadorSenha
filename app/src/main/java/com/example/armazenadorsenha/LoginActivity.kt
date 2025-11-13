package com.example.armazenadorsenha

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.armazenadorsenha.databinding.ActivityLoginBinding
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val FIXED_MASTER_PASSWORD = "123"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }
    }

    private fun attemptLogin() {
        val inputPassword = binding.editMasterPassword.text.toString()

        if (inputPassword == FIXED_MASTER_PASSWORD) {
            Toast.makeText(this, "Login bem-sucedido!", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, VaultActivity::class.java).apply {
                putExtra(VaultActivity.EXTRA_MASTER_PASSWORD, inputPassword)
            }
            startActivity(intent)
            finish()

        } else {
            Toast.makeText(this, "Senha incorreta. Tente novamente.", Toast.LENGTH_LONG).show()
            binding.editMasterPassword.error = "Senha incorreta"
        }
    }
}