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
import com.example.armazenadorsenha.DAO.PasswordDatabase
import com.example.armazenadorsenha.repository.PasswordRepository
import com.example.armazenadorsenha.repository.RoomPasswordRepository
import com.example.armazenadorsenha.repository.UserRepository
import com.example.armazenadorsenha.screen.EditScreen
import com.example.armazenadorsenha.screen.LoginScreen
import com.example.armazenadorsenha.screen.VaultScreen
import com.example.armazenadorsenha.ui.theme.ArmazenadorSenhaTheme
import androidx.appcompat.app.AppCompatActivity
var currentMasterKey: String? = null

object Screen {
    const val LOGIN = "login"
    const val VAULT = "vault/{masterKey}"
    const val EDIT = "edit/{masterKey}/{itemId}"
}

class MainActivity : AppCompatActivity() {

    private lateinit var passwordRepository: PasswordRepository
    private lateinit var userRepository: UserRepository // NOVO REPOSITÓRIO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa Database e Repositórios
        val database = PasswordDatabase.getDatabase(applicationContext)
        passwordRepository = RoomPasswordRepository(database.passwordDao())
        userRepository = UserRepository(database.userDao()) // Inicializa User Repository

        setContent {
            ArmazenadorSenhaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        passwordRepository = passwordRepository,
                        userRepository = userRepository // Passa o novo Repositório
                    )
                }
            }
        }
    }

    // --- Lógica de Sessão: Limpa a chave ao sair da aplicação ---
    override fun onPause() {
        super.onPause()
        // Invalida a chave, forçando o re-login
        currentMasterKey = null
    }
}

@Composable
fun AppNavigation(passwordRepository: PasswordRepository, userRepository: UserRepository) {
    val navController = rememberNavController()

    // Controla o ponto de partida. Se a chave estiver em memória, vai para o cofre.
    val startDestination = if (currentMasterKey != null) {
        Screen.VAULT.replace("{masterKey}", currentMasterKey!!)
    } else {
        Screen.LOGIN
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // 1. Rota de Login (Agora recebe o UserRepository)
        composable(Screen.LOGIN) {
            LoginScreen(
                repository = userRepository, // Passa o UserRepository
                onLoginSuccess = { masterKey ->
                    currentMasterKey = masterKey // Armazena a chave em memória
                    navController.navigate(Screen.VAULT.replace("{masterKey}", masterKey)) {
                        // Limpa a pilha de navegação para que o usuário não possa voltar para a tela de login
                        popUpTo(Screen.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // 2. Rota do Cofre (Vault)
        composable(Screen.VAULT) { backStackEntry ->
            val masterKey = currentMasterKey ?: backStackEntry.arguments?.getString("masterKey") ?: ""

            // Verifica a validade da sessão (caso o usuário tenha vindo direto do NavHost inicial)
            if (masterKey.isEmpty()) {
                navController.navigate(Screen.LOGIN) {
                    popUpTo(Screen.LOGIN) { inclusive = true }
                }
            } else {
                VaultScreen(
                    masterKey = masterKey,
                    navController = navController,
                    repository = passwordRepository
                )
            }
        }

        // 3. Rota de Edição (Edit)
        composable(Screen.EDIT) { backStackEntry ->
            val masterKey = currentMasterKey ?: backStackEntry.arguments?.getString("masterKey")
            val itemId = backStackEntry.arguments?.getString("itemId")?.toIntOrNull()

            if (itemId != null && masterKey != null) {
                EditScreen(
                    masterKey = masterKey,
                    itemId = itemId,
                    onBack = { navController.popBackStack() },
                    onUpdateSuccess = { navController.popBackStack() },
                    repository = passwordRepository
                )
            } else {
                navController.navigate(Screen.LOGIN) // Redireciona se a sessão cair
            }
        }
    }
}