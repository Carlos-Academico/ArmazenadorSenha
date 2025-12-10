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
import com.example.armazenadorsenha.screen.EditScreen
import com.example.armazenadorsenha.screen.LoginScreen
import com.example.armazenadorsenha.screen.VaultScreen
import com.example.armazenadorsenha.ui.theme.ArmazenadorSenhaTheme

object Screen {
    const val LOGIN = "login"
    const val VAULT = "vault/{masterKey}"
    const val EDIT = "edit/{masterKey}/{itemId}"
}

class MainActivity : ComponentActivity() {

    // 1. Declara a instância do Repository
    private lateinit var repository: PasswordRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Inicializa o Database e o Repository
        val database = PasswordDatabase.getDatabase(applicationContext)
        repository = RoomPasswordRepository(database.passwordDao())

        setContent {
            ArmazenadorSenhaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 3. Passa o Repository para a navegação
                    AppNavigation(repository = repository)
                }
            }
        }
    }
}

@Composable
// ** 4. AppNavigation AGORA recebe o Repository **
fun AppNavigation(repository: PasswordRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.LOGIN) {

        // 1. Rota de Login (Sem Alteração)
        composable(Screen.LOGIN) {
            LoginScreen(
                onLoginSuccess = { masterKey ->
                    navController.navigate(Screen.VAULT.replace("{masterKey}", masterKey)) {
                        popUpTo(Screen.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // 2. Rota do Cofre (Vault)
        composable(Screen.VAULT) { backStackEntry ->
            val masterKey = backStackEntry.arguments?.getString("masterKey") ?: ""

            if (masterKey.isEmpty()) {
                navController.navigate(Screen.LOGIN)
            } else {
                VaultScreen(
                    masterKey = masterKey,
                    navController = navController,
                    repository = repository // ** 5. Passa o Repository para VaultScreen **
                )
            }
        }

        // 3. Rota de Edição (Edit)
        composable(Screen.EDIT) { backStackEntry ->
            val masterKey = backStackEntry.arguments?.getString("masterKey")
            val itemId = backStackEntry.arguments?.getString("itemId")?.toIntOrNull()

            if (itemId != null && masterKey != null) {
                EditScreen(
                    masterKey = masterKey,
                    itemId = itemId,
                    onBack = { navController.popBackStack() },
                    onUpdateSuccess = { navController.popBackStack() },
                    repository = repository // ** 6. Passa o Repository para EditScreen **
                )
            } else {
                navController.popBackStack()
            }
        }
    }
}