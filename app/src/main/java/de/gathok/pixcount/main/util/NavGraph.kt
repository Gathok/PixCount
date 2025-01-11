package de.gathok.pixcount.main.util

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import de.gathok.pixcount.list.ListScreen
import de.gathok.pixcount.list.ListViewModel
import de.gathok.pixcount.manageColors.ManageColorsScreen
import de.gathok.pixcount.manageColors.ManageColorsViewModel
import de.gathok.pixcount.util.NavListScreen
import de.gathok.pixcount.util.NavManageColorsScreen

@Composable
fun NavGraph(
    navController: NavHostController, openDrawer: () -> Unit,
    listViewModel: ListViewModel, manageColorsViewModel: ManageColorsViewModel,
) {
    NavHost(
        navController = navController,
        startDestination = NavListScreen(),
    ) {
        // List Screen
        composable<NavListScreen> {
            val args = it.toRoute<NavListScreen>()
            ListScreen(
                viewModel = listViewModel,
                openDrawer = openDrawer,
                curPixListId = args.curPixListId,
            )
        }
        // Manage Colors Screen
        composable<NavManageColorsScreen> {
            ManageColorsScreen(
                viewModel = manageColorsViewModel,
                openDrawer = openDrawer,
            )
        }
    }
}