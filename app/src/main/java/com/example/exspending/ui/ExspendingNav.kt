package com.example.exspending.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.exspending.data.ExspendingRepository
import com.example.exspending.domain.CategoryType
import com.example.exspending.ui.overview.OverviewScreen
import com.example.exspending.ui.overview.OverviewViewModel
import com.example.exspending.ui.settings.CategoriesScreen
import com.example.exspending.ui.settings.CategoriesViewModel
import com.example.exspending.ui.settings.CategoryCreateScreen
import com.example.exspending.ui.settings.SettingsScreen
import com.example.exspending.ui.settings.SettingsViewModel
import com.example.exspending.ui.settings.TemplateDetailScreen
import com.example.exspending.ui.settings.TemplateDetailViewModel
import com.example.exspending.ui.settings.TemplatesScreen
import com.example.exspending.ui.settings.TemplatesViewModel
import com.example.exspending.ui.transactions.TransactionEditScreen
import com.example.exspending.ui.transactions.TransactionEditViewModel
import com.example.exspending.ui.transactions.TransactionsScreen
import com.example.exspending.ui.transactions.TransactionsViewModel

private object Routes {
    const val TRANSACTIONS = "transactions"
    const val OVERVIEW = "overview"
    const val SETTINGS = "settings"
    const val CATEGORIES = "categories"
    const val CATEGORY_CREATE = "category_create"
    const val TEMPLATES = "templates"
    const val TEMPLATE_DETAIL = "template/{id}/{startEdit}"
    const val TX_EDIT = "tx_edit?id={id}&type={type}"

    fun templateDetail(id: Long, startEdit: Boolean = false) = "template/$id/$startEdit"
    fun txEdit(id: Long? = null, type: String? = null): String {
        val idPart = id?.toString() ?: "-1"
        val typePart = type ?: ""
        return "tx_edit?id=$idPart&type=$typePart"
    }
}

@Composable
fun ExspendingNav(repository: ExspendingRepository) {
    val navController = rememberNavController()
    val factory = AppViewModelFactory(repository)
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route.orEmpty()
    val showBottomBar = currentRoute == Routes.TRANSACTIONS ||
        currentRoute == Routes.OVERVIEW ||
        currentRoute == Routes.SETTINGS

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.TRANSACTIONS,
                        onClick = {
                            navController.navigate(Routes.TRANSACTIONS) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Filled.Payments,
                                contentDescription = null,
                                tint = if (currentRoute == Routes.TRANSACTIONS) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        },
                        label = { Text("Transactions") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.OVERVIEW,
                        onClick = {
                            navController.navigate(Routes.OVERVIEW) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = null,
                                tint = if (currentRoute == Routes.OVERVIEW) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        },
                        label = { Text("Overview") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETTINGS,
                        onClick = {
                            navController.navigate(Routes.SETTINGS) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = if (currentRoute == Routes.SETTINGS) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TRANSACTIONS,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.TRANSACTIONS) {
                val vm: TransactionsViewModel = viewModel(factory = factory)
                TransactionsScreen(
                    viewModel = vm,
                    onAddIncome = {
                        navController.navigate(Routes.txEdit(type = CategoryType.INCOME.storageValue))
                    },
                    onAddExpense = {
                        navController.navigate(Routes.txEdit(type = CategoryType.EXPENSE.storageValue))
                    },
                    onOpenTransaction = { id ->
                        navController.navigate(Routes.txEdit(id = id))
                    }
                )
            }
            composable(Routes.OVERVIEW) {
                val vm: OverviewViewModel = viewModel(factory = factory)
                OverviewScreen(viewModel = vm)
            }
            composable(
                route = Routes.TX_EDIT,
                arguments = listOf(
                    navArgument("id") {
                        type = NavType.StringType
                        defaultValue = "-1"
                    },
                    navArgument("type") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { entry ->
                val vm: TransactionEditViewModel = viewModel(factory = factory)
                val idArg = entry.arguments?.getString("id")?.toLongOrNull()
                val typeArg = entry.arguments?.getString("type").orEmpty()
                val createType = typeArg.takeIf { it.isNotBlank() }?.let { CategoryType.fromStorage(it) }
                TransactionEditScreen(
                    viewModel = vm,
                    transactionId = idArg?.takeIf { it > 0 },
                    createType = createType,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(
                    viewModel = vm,
                    onCategories = { navController.navigate(Routes.CATEGORIES) },
                    onTemplates = { navController.navigate(Routes.TEMPLATES) }
                )
            }
            composable(Routes.CATEGORIES) {
                val vm: CategoriesViewModel = viewModel(factory = factory)
                CategoriesScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onCreate = { navController.navigate(Routes.CATEGORY_CREATE) }
                )
            }
            composable(Routes.CATEGORY_CREATE) {
                val vm: CategoriesViewModel = viewModel(factory = factory)
                CategoryCreateScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.TEMPLATES) {
                val vm: TemplatesViewModel = viewModel(factory = factory)
                TemplatesScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onOpenTemplate = { id ->
                        navController.navigate(Routes.templateDetail(id, startEdit = false))
                    },
                    onCreate = {
                        navController.navigate(Routes.templateDetail(0L, startEdit = true))
                    }
                )
            }
            composable(
                route = Routes.TEMPLATE_DETAIL,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("startEdit") { type = NavType.BoolType }
                )
            ) { entry ->
                val vm: TemplateDetailViewModel = viewModel(factory = factory)
                val id = entry.arguments?.getLong("id") ?: return@composable
                val startEdit = entry.arguments?.getBoolean("startEdit") ?: false
                TemplateDetailScreen(
                    viewModel = vm,
                    templateId = id,
                    startInEditMode = startEdit,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
