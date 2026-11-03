package com.example.financialflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.financialflow.model.TransactionType
import com.example.financialflow.ui.AddTransactionScreen
import com.example.financialflow.ui.CategoriesScreen
import com.example.financialflow.ui.DashboardScreen
import com.example.financialflow.ui.FinancialFlowTheme
import com.example.financialflow.ui.formatMoney
import java.math.BigDecimal
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )

        val preferences = getSharedPreferences("appearance", MODE_PRIVATE)
        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            var darkTheme by rememberSaveable {
                mutableStateOf(preferences.getBoolean("dark_theme", systemDarkTheme))
            }
            val view = LocalView.current

            SideEffect {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            FinancialFlowTheme(darkTheme = darkTheme) {
                FinancialFlowApp(
                    viewModel = viewModel,
                    darkTheme = darkTheme,
                    onToggleTheme = {
                        darkTheme = !darkTheme
                        preferences.edit().putBoolean("dark_theme", darkTheme).apply()
                    },
                )
            }
        }
    }
}

private enum class AppDestination {
    DASHBOARD,
    CATEGORIES,
    ADD_TRANSACTION,
}

@Composable
private fun FinancialFlowApp(
    viewModel: MainViewModel,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var destination by rememberSaveable { mutableStateOf(AppDestination.DASHBOARD) }
    var initialTransactionType by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var editingTransactionId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }
    LaunchedEffect(uiState.noticeMessage) {
        uiState.noticeMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearNotice()
        }
    }

    val balance = remember(uiState.transactions) {
        uiState.transactions.fold(BigDecimal.ZERO) { total, transaction ->
            if (transaction.type == TransactionType.INCOME) {
                total + transaction.amount
            } else {
                total - transaction.amount
            }
        }
    }

    fun openMenu() {
        coroutineScope.launch { drawerState.open() }
    }

    fun navigateTo(target: AppDestination) {
        destination = target
        coroutineScope.launch { drawerState.close() }
    }

    fun showComingSoon(title: String) {
        coroutineScope.launch {
            drawerState.close()
            snackbarHostState.showSnackbar("Розділ «$title» буде додано в наступній версії")
        }
    }

    BackHandler(enabled = destination == AppDestination.ADD_TRANSACTION && !uiState.isSaving) {
        editingTransactionId = null
        destination = AppDestination.DASHBOARD
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = destination != AppDestination.ADD_TRANSACTION,
        drawerContent = {
            AppDrawer(
                balance = balance,
                destination = destination,
                darkTheme = darkTheme,
                onNavigate = ::navigateTo,
                onToggleTheme = onToggleTheme,
                onComingSoon = ::showComingSoon,
            )
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when (destination) {
                    AppDestination.DASHBOARD -> DashboardScreen(
                        uiState = uiState,
                        onOpenMenu = ::openMenu,
                        onAddTransaction = { type ->
                            editingTransactionId = null
                            initialTransactionType = type
                            destination = AppDestination.ADD_TRANSACTION
                        },
                        onEditTransaction = { transaction ->
                            editingTransactionId = transaction.id
                            initialTransactionType = transaction.type
                            destination = AppDestination.ADD_TRANSACTION
                        },
                        onDeleteTransaction = viewModel::deleteTransaction,
                    )

                    AppDestination.CATEGORIES -> CategoriesScreen(
                        uiState = uiState,
                        onOpenMenu = ::openMenu,
                        onCreate = viewModel::addCategory,
                        onUpdate = viewModel::updateCategory,
                        onDelete = viewModel::deleteCategory,
                    )

                    AppDestination.ADD_TRANSACTION -> AddTransactionScreen(
                        initialType = initialTransactionType,
                        transaction = uiState.transactions.firstOrNull { it.id == editingTransactionId },
                        categories = uiState.categories,
                        isSaving = uiState.isSaving,
                        onBack = {
                            editingTransactionId = null
                            destination = AppDestination.DASHBOARD
                        },
                        onSave = { draft ->
                            val transactionId = editingTransactionId
                            val onSaved = {
                                editingTransactionId = null
                                destination = AppDestination.DASHBOARD
                            }
                            if (transactionId == null) {
                                viewModel.addTransaction(draft, onSaved)
                            } else {
                                viewModel.updateTransaction(transactionId, draft, onSaved)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppDrawer(
    balance: BigDecimal,
    destination: AppDestination,
    darkTheme: Boolean,
    onNavigate: (AppDestination) -> Unit,
    onToggleTheme: () -> Unit,
    onComingSoon: (String) -> Unit,
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.width(320.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 20.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "₴",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text("Мої фінанси", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatMoney(balance),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.size(12.dp))

            DrawerDestinationItem(
                icon = "⌂",
                label = "Огляд",
                selected = destination == AppDestination.DASHBOARD,
                onClick = { onNavigate(AppDestination.DASHBOARD) },
            )
            DrawerDestinationItem(
                icon = "▦",
                label = "Категорії",
                selected = destination == AppDestination.CATEGORIES,
                onClick = { onNavigate(AppDestination.CATEGORIES) },
            )

            Spacer(Modifier.size(8.dp))
            Text(
                text = "МАЙБУТНІ ФУНКЦІЇ",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp),
            )
            listOf(
                "▤" to "Бюджети",
                "↻" to "Регулярні платежі",
                "◷" to "Нагадування",
                "⚙" to "Налаштування",
            ).forEach { (icon, title) ->
                DrawerDestinationItem(
                    icon = icon,
                    label = title,
                    selected = false,
                    onClick = { onComingSoon(title) },
                    muted = true,
                )
            }

            Spacer(Modifier.weight(1f))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            DrawerDestinationItem(
                icon = if (darkTheme) "☀" else "◐",
                label = if (darkTheme) "Світла тема" else "Темна тема",
                selected = false,
                onClick = onToggleTheme,
            )
            Text(
                text = "Financial Flow · v1.2",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun DrawerDestinationItem(
    icon: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    muted: Boolean = false,
) {
    NavigationDrawerItem(
        icon = { Text(icon, fontSize = 22.sp) },
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedContainerColor = Color.Transparent,
            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (muted) 0.62f else 0.9f),
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (muted) 0.55f else 0.82f),
        ),
    )
}
