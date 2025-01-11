@file:OptIn(ExperimentalMaterial3Api::class)

package de.gathok.pixcount

import FilledBoxIcon
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.gathok.pixcount.db.PixCategory
import de.gathok.pixcount.db.PixList
import de.gathok.pixcount.ui.customDialogs.CategoryDialog
import de.gathok.pixcount.ui.customDialogs.CustomDialog
import de.gathok.pixcount.ui.customDialogs.EntryDialog
import de.gathok.pixcount.ui.customIcons.FilledPixListIcon
import de.gathok.pixcount.ui.customIcons.OutlinedPixListIcon
import de.gathok.pixcount.ui.theme.PixCountTheme
import de.gathok.pixcount.util.Months
import io.realm.kotlin.internal.platform.currentTime
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Composable
fun MainScreen(
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var versionName: String?
    try {
        val pInfo: PackageInfo =
            context.packageManager.getPackageInfo(context.packageName, 0)
        versionName = pInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        versionName = null
    }

    var showEntryDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<Long?>(null) }
    var curEntryCategory by remember { mutableStateOf<PixCategory?>(null) }

    var showCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<PixCategory?>(null) }

    var showNewListDialog by remember { mutableStateOf(false) }

    var showDeleteListDialog by remember { mutableStateOf(false) }
    var listToDelete by remember { mutableStateOf<PixList?>(null) }

    if (showEntryDialog) {
        val startDate: Long? = entryToEdit
        val curCategory: PixCategory? = curEntryCategory
        if (startDate != null) {
            entryToEdit = null
        }
        if (curCategory != null) {
            curEntryCategory = null
        }
        EntryDialog(
            categories = state.curCategories,
            onDismiss = { showEntryDialog = false },
            onEdit = { day, month, category ->
                viewModel.createPixEntry(day, month, category, state.curPixList!!.id)
                showEntryDialog = false
            },
            startDate = startDate
                ?: (currentTime().epochSeconds * 1000 + currentTime().nanosecondsOfSecond / 1_000_000),
            curCategory = curCategory ?: state.curCategories.first()
        )
    }

    if (showCategoryDialog) {
        CategoryDialog(
            onDismiss = {
                showCategoryDialog = false
                categoryToEdit = null
            },
            onAdd = { name, color, isEdit ->
                if (isEdit) {
                    viewModel.updatePixCategory(
                        categoryToEdit!!,
                        name,
                        color,
                        state.curPixList!!.id
                    )
                } else {
                    viewModel.createPixCategory(name!!, color!!, state.curPixList!!.id)
                }
                showCategoryDialog = false
                categoryToEdit = null
            },
            onDelete = {
                viewModel.deleteCategory(
                    categoryToEdit!!,
                    state.curPixList!!.id
                )
                showCategoryDialog = false
                categoryToEdit = null
            },
            colors = state.colorList,
            invalidNames = state.curCategories.map { it.name },
            isEdit = categoryToEdit != null,
            categoryToEdit = categoryToEdit,
        )
    }

    if (showNewListDialog) {
        NewListDialog(
            onDismiss = { showNewListDialog = false },
            onAdd = { name ->
                val newPixList = viewModel.createPixList(name.trim())
                showNewListDialog = false
                viewModel.setCurPixList(newPixList)
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

    LaunchedEffect(state.curPixList) {
        if (state.curPixList == null) {
            scope.launch {
                 drawerState.open()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(modifier = Modifier.height(16.dp))
                    state.allPixLists.forEach { curPixList ->
                        NavigationDrawerItem(
                            label = { Text(curPixList.name) },
                            selected = curPixList == state.curPixList,
                            onClick = {
                                viewModel.setCurPixList(curPixList)
                                scope.launch {
                                    drawerState.close()
                                }
                            },
                            modifier = Modifier
                                .padding(NavigationDrawerItemDefaults.ItemPadding),
                            icon = {
                                Icon(
                                    imageVector = if (curPixList == state.curPixList) {
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
                    Spacer(modifier = Modifier.weight(1f))
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.new_pixlist)) },
                        onClick = {
                            showNewListDialog = true
                            scope.launch {
                                drawerState.close()
                            }
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
            Scaffold (
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            if (state.curPixList != null) {
                                Text(state.curPixList!!.name)
                            } else {
                                Text(stringResource(R.string.app_name))
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    if (state.curPixList != null  && state.curCategories.isNotEmpty()) {
                                        showEntryDialog = true
                                    } else {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.create_pixlist_and_category_first_desc),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = "Add Entry",
                                    tint = if (state.curPixList != null && state.curCategories.isNotEmpty()) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    }
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    scope.launch { 
                                        drawerState.open()
                                    }
                                }
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ) {
                                    Image (
                                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                                        contentDescription = "Menu",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .scale(1.5f)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors().copy(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    )
                }
            ) { pad ->
                Box (
                    modifier = Modifier
                        .padding(pad)
                        .padding(start = 8.dp, end = 4.dp, bottom = 16.dp)
                ) {
                    if (state.curPixList != null) {
                        Row {
                            Column (
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Row (
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                ) {
                                    for (i in 0..12) {
                                        Column (
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .weight(1f / 13f)
                                        ) {
                                            if (i == 0) {
                                                for (j in 0..31) {
                                                    Row (
                                                        modifier = Modifier
                                                            .weight(1f / 32f),
                                                        horizontalArrangement = Arrangement.End,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        val text = if (j == 0) {
                                                            null
                                                        } else if (j < 10) {
                                                            "0$j"
                                                        } else {
                                                            "$j"
                                                        }
                                                        if (text != null) {
                                                            Text (
                                                                text = text,
                                                                style = MaterialTheme.typography.labelMedium,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                textAlign = TextAlign.Center,
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                val month = Months.getByIndex(i)
                                                for (day in 0..month.getDaysCount) {
                                                    Row (
                                                        modifier = Modifier
                                                            .weight(1f / 32f),
                                                        verticalAlignment = Alignment.Bottom,
                                                    ) {
                                                        if (day == 0) {
                                                            Text(
                                                                text = stringResource(month.getShortStringId),
                                                                style = MaterialTheme.typography.labelMedium,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                            )
                                                        } else {
                                                            state.curPixList!!.entries?.getEntry(
                                                                day,
                                                                month
                                                            ).let { pixCategory ->
                                                                IconButton(
                                                                    onClick = {
                                                                        if (state.curCategories.isNotEmpty()) {
                                                                            entryToEdit = getDateAsLong(day, month.getIndex)
                                                                            curEntryCategory = pixCategory
                                                                            showEntryDialog = true
                                                                        } else {
                                                                            Toast.makeText(
                                                                                context,
                                                                                context.getString(R.string.create_category_first_desc),
                                                                                Toast.LENGTH_SHORT
                                                                            ).show()
                                                                        }
                                                                    }
                                                                ) {
                                                                    if (pixCategory == null) { // TODO: This is just a placeholder
                                                                        throw IllegalArgumentException(
                                                                            "Entries might be null"
                                                                        )
                                                                    } else if (!pixCategory.color!!.isPlaceholder) {
                                                                        Icon(
                                                                            imageVector = FilledBoxIcon,
                                                                            contentDescription = "Pix",
                                                                            tint = pixCategory.color!!.toColor(),
                                                                        )
                                                                    } else {
                                                                        Icon(
                                                                            imageVector = Icons.Default.CheckBoxOutlineBlank,
                                                                            contentDescription = "Empty Pix",
                                                                            tint = MaterialTheme.colorScheme.onSurface.copy(
                                                                                alpha = 0.5f
                                                                            ),
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                for (r in 0 until 31 - month.getDaysCount) {
                                                    Row (
                                                        modifier = Modifier
                                                            .weight(1f / 32f),
                                                    ) {

                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            }
                            LazyColumn (
                                modifier = Modifier
                                    .weight(0.2f)
                                    .padding(start = 4.dp, top = 8.dp)
                                    .fillMaxSize(),
                            ) {
                                items(state.curCategories) { curCategory ->
                                    Row (
                                        modifier = Modifier
                                            .clickable {
                                                categoryToEdit = curCategory
                                                showCategoryDialog = true
                                            }
                                            .fillMaxWidth(),
                                    ) {
                                        val color = curCategory.color
                                        Column (
                                            modifier = Modifier
                                                .padding(bottom = 4.dp)
                                                .fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row {
                                                Icon(
                                                    imageVector = FilledBoxIcon,
                                                    contentDescription = "Category Pix",
                                                    tint = color!!.toColor(),
                                                )
                                            }
                                            Row {
                                                Text(
                                                    text = curCategory.name,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Center,
                                                )
                                            }
                                        }
                                    }
                                }
                                item {
                                    Row (
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        IconButton(
                                            onClick = {
                                                showCategoryDialog = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AddBox,
                                                contentDescription = "Add Category"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column (
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(stringResource(R.string.no_pixlist_selected))
                        }
                    }
                }
            }
        }
    }
}

fun getDateAsLong(
    day: Int,
    month: Int,
    year: Int = ZonedDateTime.ofInstant(
        Instant.ofEpochMilli(currentTime().epochSeconds * 1000 + currentTime().nanosecondsOfSecond / 1_000_000),
        ZoneId.systemDefault()
    ).year
): Long {
    return LocalDate.of(year, month, day)
        .atStartOfDay(ZoneOffset.UTC) // Start of day in the system's default timezone
        .toInstant()
        .toEpochMilli()
}


// NewListDialog ----------------------------------------------------------------
@Composable
fun NewListDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    invalidNames: List<String> = emptyList()
) {
    var name by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box (
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column (
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Row (
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                    Text(
                        stringResource(R.string.new_pixlist),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Add",
                        tint = if (name.isNotBlank() && !invalidNames.contains(name.trim())) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        },
                        modifier = Modifier
                            .clickable {
                                if (name.isNotBlank() && !invalidNames.contains(name.trim())) {
                                    onAdd(name)
                                }
                            },
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (invalidNames.contains(name.trim())) {
                    Row {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .height(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.name_already_in_use),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier
                            .fillMaxWidth(),
                        isError = invalidNames.contains(name.trim()),
                    )
                }
            }
        }
    }
}

@Preview(apiLevel = 34)
@Composable
private fun NewListDialogPreview() {
    PixCountTheme (
        darkTheme = true
    ) {
        NewListDialog(
            onDismiss = { },
            onAdd = { _ -> },
        )
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

//@Preview
//@Composable
//private fun MainScreenPreview() {
//    PixCountTheme (
//        darkTheme = true
//    ) {
//        MainScreen(
//            viewModel = MainViewModel()
//        )
//    }
//}