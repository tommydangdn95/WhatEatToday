package com.family.homnayangi.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.family.homnayangi.R
import com.family.homnayangi.ui.history.HistoryScreen
import com.family.homnayangi.ui.recipe.RecipeScreen
import com.family.homnayangi.ui.recipe.RecipeViewModel
import com.family.homnayangi.ui.today.TodayScreen

/** 5 tab ở thanh điều hướng dưới cùng (SPEC mục 9). */
enum class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
    @param:DrawableRes val iconRes: Int? = null,
) {
    TODAY("today", "Hôm nay", icon = Icons.Filled.Home),
    PANTRY("pantry", "Tủ lạnh", iconRes = R.drawable.ic_fridge),
    SHOPPING("shopping", "Đi chợ", icon = Icons.Filled.ShoppingCart),
    DISHES("dishes", "Món nhà", icon = Icons.AutoMirrored.Filled.List),
    HISTORY("history", "Lịch sử", icon = Icons.Filled.DateRange),
}

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { nav.navigateToTab(tab) },
                        icon = {
                            val m = Modifier.size(28.dp)
                            when {
                                tab.icon != null -> Icon(tab.icon, contentDescription = null, modifier = m)
                                tab.iconRes != null -> Icon(painterResource(tab.iconRes), contentDescription = null, modifier = m)
                            }
                        },
                        // Nhãn chữ luôn hiện; TalkBack đọc nhãn này nên icon không cần contentDescription riêng.
                        label = { Text(tab.label, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Tab.TODAY.route, modifier = Modifier.padding(padding)) {
            composable(Tab.TODAY.route) {
                TodayScreen(
                    onOpenSettings = { nav.navigate(SETTINGS_ROUTE) },
                    onOpenRecipe = { nav.navigate(RecipeViewModel.route(it)) },
                )
            }
            composable(Tab.PANTRY.route) { ComingSoon("Tủ lạnh có gì") }
            composable(Tab.SHOPPING.route) { ComingSoon("Đi chợ") }
            composable(Tab.DISHES.route) { ComingSoon("Món của nhà") }
            composable(Tab.HISTORY.route) { HistoryScreen(onCookAgain = { nav.navigateToTab(Tab.TODAY) }) }
            composable(SETTINGS_ROUTE) { ComingSoon("Cài đặt") }
            composable(
                RecipeViewModel.ROUTE,
                arguments = listOf(navArgument(RecipeViewModel.ARG_DISH_ID) { type = NavType.StringType }),
            ) { RecipeScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

private const val SETTINGS_ROUTE = "settings"

private fun NavHostController.navigateToTab(tab: Tab) = navigate(tab.route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun ComingSoon(title: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text("$title\n(đang làm)", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    }
}
