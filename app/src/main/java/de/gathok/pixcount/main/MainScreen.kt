@file:OptIn(ExperimentalFoundationApi::class)

package de.gathok.pixcount.main

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.compose.rememberNavController
import de.gathok.pixcount.R
import de.gathok.pixcount.db.PixList
import de.gathok.pixcount.list.ListViewModel
import de.gathok.pixcount.main.util.NavGraph
import de.gathok.pixcount.manageColors.ManageColorsViewModel
import de.gathok.pixcount.ui.customDialogs.CustomDialog
import de.gathok.pixcount.ui.customDialogs.NewListDialog
import de.gathok.pixcount.ui.customIcons.FilledPixListIcon
import de.gathok.pixcount.ui.customIcons.OutlinedPixListIcon
import de.gathok.pixcount.ui.theme.PixCountTheme
import de.gathok.pixcount.util.LoadingScreen
import de.gathok.pixcount.util.NavListScreen
import de.gathok.pixcount.util.NavManageColorsScreen
import de.gathok.pixcount.util.Screen
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId

@Composable
fun MainScreen(
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)

    val selectedScreen = remember { mutableStateOf(Screen.LIST) }
    val selectedPixListId = remember { mutableStateOf<ObjectId?>(null) }

    var versionName: String?
    try {
        val pInfo: PackageInfo =
            context.packageManager.getPackageInfo(context.packageName, 0)
        versionName = pInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        versionName = null
    }

    var showNewListDialog by remember { mutableStateOf(false) }

    var showDeleteListDialog by remember { mutableStateOf(false) }
    var listToDelete by remember { mutableStateOf<PixList?>(null) }

    if (showNewListDialog) {
        NewListDialog(
            onDismiss = { showNewListDialog = false },
            onAdd = { name ->
                val newPixList = viewModel.createPixList(name.trim())
                showNewListDialog = false
                selectedScreen.value = Screen.LIST
                selectedPixListId.value = newPixList.id
                navController.navigate(LoadingScreen)
                scope.launch {
                    drawerState.close()
                    navController.navigate(NavListScreen(newPixList.id.toHexString()))
                }
            },
            invalidNames = state.allPixLists.map { it.name },
        )
    }

    if (showDeleteListDialog && listToDelete != null) {
        CustomDialog(
            onDismissRequest = { showDeleteListDialog = false },
            title = {
                Text(
                    text = "Delete PixList",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            leftIcon = {
                IconButton (
                    onClick = {
                        showDeleteListDialog = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            rightIcon = {
                TextButton(
                    onClick = {
                        viewModel.deletePixList(listToDelete!!)
                        showDeleteListDialog = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Yes",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            },
        ) {
            Text(
                text = stringResource(R.string.delete_list_desc, listToDelete!!.name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = 16.dp)
            )
        }
    }

    var showHiddenLists by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    val SHOW_HIDDEN_LISTS = stringResource(R.string.hidden_lists_shown)
                    val HIDE_HIDDEN_LISTS = stringResource(R.string.hidden_lists_hidden)
                    Spacer(modifier = Modifier.height(8.dp))
                    NavListHeader(onLongClick = {
                        showHiddenLists = !showHiddenLists
                        Toast.makeText(
                            context,
                            if (showHiddenLists) SHOW_HIDDEN_LISTS else HIDE_HIDDEN_LISTS,
                            Toast.LENGTH_SHORT
                        ).show()
                        // Vibrate
                        (getSystemService(context, Vibrator::class.java) as Vibrator)
                            .vibrate(
                                VibrationEffect
                                    .createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                            )
                    })
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn (
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        items(state.allPixLists) { curPixList ->
                            if (!(curPixList.name.matches(Regex("^\\(.*\\)$")) && !showHiddenLists)) {
                                NavigationDrawerItem(
                                    label = { Text(curPixList.name) },
                                    selected = curPixList.id == selectedPixListId.value,
                                    onClick = {
                                        selectedPixListId.value = curPixList.id
                                        selectedScreen.value = Screen.LIST
                                        scope.launch {
                                            navController.navigate(LoadingScreen)
                                            drawerState.close()
                                            navController.navigate(NavListScreen(curPixList.id.toHexString()))
                                        }
                                    },
                                    modifier = Modifier
                                        .padding(NavigationDrawerItemDefaults.ItemPadding),
                                    icon = {
                                        Icon(
                                            imageVector = if (curPixList.id == selectedPixListId.value) {
                                                FilledPixListIcon
                                            } else {
                                                OutlinedPixListIcon
                                            },
                                            contentDescription = "PixList"
                                        )
                                    },
                                    badge = {
                                        IconButton(
                                            onClick = {
                                                listToDelete = curPixList
                                                selectedPixListId.value = null
                                                selectedScreen.value = Screen.LIST
                                                showDeleteListDialog = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete PixList"
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        item {
                            NavigationDrawerItem(
                                label = { Text(stringResource(R.string.new_pixlist)) },
                                onClick = {
                                    showNewListDialog = true
                                    selectedPixListId.value = null
                                },
                                selected = false,
                                modifier = Modifier
                                    .padding(NavigationDrawerItemDefaults.ItemPadding),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.LibraryAdd,
                                        contentDescription = "Add PixList"
                                    )
                                }
                            )
                        }
                    }
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.manage_colors)) },
                        onClick = {
                            navController.navigate(NavManageColorsScreen)
                            selectedPixListId.value = null
                            selectedScreen.value = Screen.MANAGE_COLORS
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        selected = selectedScreen.value == Screen.MANAGE_COLORS,
                        modifier = Modifier
                            .padding(NavigationDrawerItemDefaults.ItemPadding),
                        icon = {
                            if (selectedScreen.value == Screen.MANAGE_COLORS) {
                                Icon(
                                    imageVector = Icons.Filled.ColorLens,
                                    contentDescription = "Manage Colors"
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.ColorLens,
                                    contentDescription = "Manage Colors"
                                )
                            }

                        }
                    )
                    if (versionName != null) {
                        Text(
                            text = stringResource(R.string.version_desc, versionName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        ) {
            NavGraph(
                navController = navController, { scope.launch { drawerState.open() } },
                listViewModel = ListViewModel(), manageColorsViewModel = ManageColorsViewModel()
            )
        }
    }
}

@Composable
fun NavListHeader(
    onLongClick: () -> Unit,
) {
    Row (
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .combinedClickable(
                onClick = { },
                onLongClick = onLongClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Surface(
                shape = RoundedCornerShape(50),
                color = de.gathok.pixcount.ui.theme.Surface,
                modifier = Modifier
                    .height(42.dp)
            ) {
                Image (
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "Menu",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .scale(1.5f),
                )
            }
        }
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(start = 16.dp)
            )
        }

    }
}

@Preview
@Composable
private fun Preview() {
    PixCountTheme (
        darkTheme = true
    ) {
        Surface (
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "Menu",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .scale(1.5f)
            )
        }
    }
}

@Preview
@Composable
fun MainScreenPreview() {
    PixCountTheme (
        darkTheme = true
    ) {
        
    }
}