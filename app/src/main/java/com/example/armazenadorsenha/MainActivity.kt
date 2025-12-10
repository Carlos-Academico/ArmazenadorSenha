package com.example.armazenadorsenha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.armazenadorsenha.screen.LoginScreen
import com.example.armazenadorsenha.screen.VaultScreen
import com.example.armazenadorsenha.ui.theme.ArmazenadorSenhaTheme

// Definição das rotas
object Screen {
    const val LOGIN = "login"
    const val VAULT = "vault/{masterKey}" // Recebe a Master Key
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArmazenadorSenhaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.LOGIN) {

        // 1. Rota de Login
        composable(Screen.LOGIN) {
            LoginScreen(
                onLoginSuccess = { masterKey ->
                    // Navega para a tela do cofre, passando a chave
                    navController.navigate(Screen.VAULT.replace("{masterKey}", masterKey)) {
                        popUpTo(Screen.LOGIN) { inclusive = true } // Não permite voltar para o login
                    }
                }
            )
        }

        // 2. Rota do Cofre (Lista de Senhas)
        composable(Screen.VAULT) { backStackEntry ->
            val masterKey = backStackEntry.arguments?.getString("masterKey") ?: ""

            if (masterKey.isEmpty()) {
                // Se a chave não vier, retorna para o login
                navController.navigate(Screen.LOGIN)
            } else {
                VaultScreen(masterKey = masterKey)
            }
        }
    }
}