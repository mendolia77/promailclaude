package com.smartmail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartmail.ui.screens.category.CategoryDetailScreen
import com.smartmail.ui.screens.compose.ComposeScreen
import com.smartmail.ui.screens.dashboard.DashboardScreen
import com.smartmail.ui.screens.email.EmailDetailScreen
import com.smartmail.ui.screens.inbox.InboxScreenEnhanced
import com.smartmail.ui.screens.onboarding.OnboardingScreen
import com.smartmail.ui.screens.settings.SettingsScreen
import com.smartmail.ui.screens.splash.SplashScreen
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.smartmail.data.preferences.AppPreferences
import com.smartmail.data.preferences.ThemeMode
import com.smartmail.data.preferences.ThemePreferences
import com.smartmail.ui.theme.SmartMailTheme
import com.smartmail.workers.EmailSyncWorker
import com.smartmail.workers.EmailDeleteSyncWorker
import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Avvia sync periodica con intervallo dalle preferenze
            val prefs = AppPreferences(this)
            EmailSyncWorker.schedulePeriodic(this, prefs.syncIntervalMinutes)

            // Avvia worker per sincronizzare eliminazioni sul server
            EmailDeleteSyncWorker.schedulePeriodic(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val themePreferences = remember { ThemePreferences(this) }
            val themeMode = themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val systemInDarkTheme = isSystemInDarkTheme()

            val darkTheme = when (themeMode.value) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemInDarkTheme
            }

            SmartMailTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SmartMailApp()
                }
            }
        }
    }
}

@Composable
fun SmartMailApp() {
    val navController = rememberNavController()

    // Gestisci se è il primo avvio
    var isFirstLaunch by remember { mutableStateOf(true) }
    var showOnboarding by remember { mutableStateOf(true) }

    NavHost(
        navController = navController,
        startDestination = if (isFirstLaunch) "splash" else "inbox"
    ) {
        // Splash Screen
        composable("splash") {
            SplashScreen(
                onSplashComplete = {
                    if (showOnboarding) {
                        navController.navigate("onboarding") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("inbox") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }

        // Onboarding
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    showOnboarding = false
                    navController.navigate("inbox") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        // Main Inbox (Enhanced)
        composable("inbox") {
            InboxScreenEnhanced(
                onCompose = { navController.navigate("compose") },
                onSettings = { navController.navigate("settings") },
                onSearch = { navController.navigate("search") },
                onCategoryClick = { folderId, name, icon, color ->
                    // Converti colore in formato hex ARGB (8 caratteri)
                    val colorHex = color.value.toString(16).takeLast(8).padStart(8, 'F').uppercase()
                    // URL encode anche il nome per sicurezza
                    val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                    navController.navigate("category/$folderId/$encodedName/$colorHex")
                },
                onEmailClick = { emailId ->
                    // TODO: Implementare schermata dettaglio email
                    // Per ora navighiamo a una route placeholder
                    navController.navigate("email/$emailId")
                }
            )
        }

        // Search
        composable("search") {
            com.smartmail.ui.screens.search.SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onEmailClick = { emailId ->
                    navController.navigate("email/$emailId")
                }
            )
        }

        // Compose Email
        composable("compose") {
            ComposeScreen(
                onNavigateBack = { navController.popBackStack() },
                onEmailSent = { navController.popBackStack() }
            )
        }

        // Compose Reply
        composable(
            route = "compose/reply/{emailId}",
            arguments = listOf(navArgument("emailId") { type = NavType.LongType })
        ) { backStackEntry ->
            val emailId = backStackEntry.arguments?.getLong("emailId") ?: 0L
            ComposeScreen(
                emailId = emailId,
                isReply = true,
                onNavigateBack = { navController.popBackStack() },
                onEmailSent = { navController.popBackStack() }
            )
        }

        // Compose Forward
        composable(
            route = "compose/forward/{emailId}",
            arguments = listOf(navArgument("emailId") { type = NavType.LongType })
        ) { backStackEntry ->
            val emailId = backStackEntry.arguments?.getLong("emailId") ?: 0L
            ComposeScreen(
                emailId = emailId,
                isForward = true,
                onNavigateBack = { navController.popBackStack() },
                onEmailSent = { navController.popBackStack() }
            )
        }

        // Settings
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFilters = { navController.navigate("filters") },
                onNavigateToFolders = { navController.navigate("folders") }
            )
        }

        // Filtri Email
        composable("filters") {
            com.smartmail.ui.screens.filters.FiltersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Smart Folders
        composable("folders") {
            com.smartmail.ui.screens.folders.SmartFoldersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Dashboard Analytics
        composable("dashboard") {
            DashboardScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Category Detail
        composable(
            route = "category/{folderId}/{name}/{color}",
            arguments = listOf(
                navArgument("folderId") { type = NavType.LongType },
                navArgument("name") { type = NavType.StringType },
                navArgument("color") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getLong("folderId") ?: 0L
            val encodedName = backStackEntry.arguments?.getString("name") ?: ""
            val categoryName = URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString())
            val colorHex = backStackEntry.arguments?.getString("color") ?: "FF6B2FBF"
            val categoryColor = Color(colorHex.toLong(16))

            // Map category name to icon
            val categoryIcon = when (categoryName) {
                "Lavoro" -> "🏢"
                "Personale" -> "👥"
                "Shopping" -> "🛒"
                "Newsletter" -> "📰"
                "Allegati" -> "📎"
                "Importanti" -> "⭐"
                "Tutti" -> "📥"
                else -> "📧"
            }

            CategoryDetailScreen(
                folderId = folderId,
                categoryName = categoryName,
                categoryIcon = categoryIcon,
                categoryColor = categoryColor,
                onBack = { navController.popBackStack() },
                onEmailClick = { emailId ->
                    navController.navigate("email/$emailId")
                }
            )
        }

        // Email Detail
        composable(
            route = "email/{emailId}",
            arguments = listOf(
                navArgument("emailId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val emailId = backStackEntry.arguments?.getLong("emailId") ?: 0L

            EmailDetailScreen(
                emailId = emailId,
                onBack = { navController.popBackStack() },
                onReply = { id ->
                    navController.navigate("compose/reply/$id")
                },
                onForward = { id ->
                    navController.navigate("compose/forward/$id")
                }
            )
        }
    }
}
