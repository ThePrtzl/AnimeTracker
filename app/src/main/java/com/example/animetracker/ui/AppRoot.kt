package com.example.animetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.animetracker.ui.browse.BrowseScreen
import com.example.animetracker.ui.detail.DetailScreen
import com.example.animetracker.ui.library.LibraryScreen
import com.example.animetracker.ui.theme.Ink

private const val ROUTE_BROWSE = "browse"
private const val ROUTE_LIBRARY = "library"
private const val ROUTE_DETAIL = "detail"

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = route == ROUTE_BROWSE || route == ROUTE_LIBRARY

    val openDetail: (Int) -> Unit = { id -> nav.navigate("$ROUTE_DETAIL/$id") }

    Scaffold(
        containerColor = Ink.Background,
        // Screens handle the status bar themselves; the bottom bar handles the navigation bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBar) {
                BottomBar(
                    current = route,
                    onSelect = { target ->
                        nav.navigate(target) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = ROUTE_BROWSE,
            modifier = Modifier.padding(inner),
        ) {
            composable(ROUTE_BROWSE) { BrowseScreen(onOpen = openDetail) }
            composable(ROUTE_LIBRARY) { LibraryScreen(onOpen = openDetail) }
            composable(
                route = "$ROUTE_DETAIL/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                val id = entry.arguments?.getInt("id") ?: return@composable
                DetailScreen(id = id, onBack = { nav.popBackStack() }, onOpen = openDetail)
            }
        }
    }
}

@Composable
private fun BottomBar(current: String?, onSelect: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Ink.NavBar)
            .border(width = 1.dp, color = Ink.GlassBorderSoft, shape = androidx.compose.ui.graphics.RectangleShape)
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarItem("Browse", Icons.Rounded.GridView, selected = current == ROUTE_BROWSE) { onSelect(ROUTE_BROWSE) }
            BarItem("Library", Icons.Rounded.Bookmark, selected = current == ROUTE_LIBRARY) { onSelect(ROUTE_LIBRARY) }
        }
    }
}

@Composable
private fun BarItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) Ink.Accent else Ink.TextMuted
    Column(
        Modifier
            .width(110.dp)
            .height(56.dp)
            .clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(
            label,
            color = tint,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
