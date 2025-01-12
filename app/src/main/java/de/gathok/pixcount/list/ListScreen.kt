package de.gathok.pixcount.list

import FilledPixIcon
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.gathok.pixcount.R
import de.gathok.pixcount.db.PixCategory
import de.gathok.pixcount.ui.CustomTopBar
import de.gathok.pixcount.ui.customDialogs.CategoryDialog
import de.gathok.pixcount.ui.customDialogs.EntryDialog
import de.gathok.pixcount.ui.customDialogs.RenamePixListDialog
import de.gathok.pixcount.util.Months
import io.realm.kotlin.internal.platform.currentTime
import org.mongodb.kbson.BsonObjectId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Composable
fun ListScreen(
    viewModel: ListViewModel,
    openDrawer: () -> Unit,
    curPixListId: String?,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(curPixListId) {
        val objectId = curPixListId?.let { BsonObjectId(it) }
        println("Loading PixList with ID: $objectId of type ${objectId?.javaClass}")
        viewModel.setPixListId(objectId)
    }

    var showEntryDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<Long?>(null) }
    var curEntryCategory by remember { mutableStateOf<PixCategory?>(null) }

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
                viewModel.setPixEntry(day, month, category, state.curPixList!!)
                showEntryDialog = false
            },
            startDate = startDate
                ?: (currentTime().epochSeconds * 1000 + currentTime().nanosecondsOfSecond / 1_000_000),
            curCategory = curCategory ?: state.curCategories.first()
        )
    }

    var showCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<PixCategory?>(null) }

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
                        color
                    )
                } else {
                    viewModel.createPixCategory(name!!, color!!, state.curPixList!!)
                }
                showCategoryDialog = false
                categoryToEdit = null
            },
            onDelete = {
                viewModel.deletePixCategory(categoryToEdit!!, state.curPixList!!)
                showCategoryDialog = false
                categoryToEdit = null
            },
            colors = state.colorList,
            invalidNames = state.curCategories.map { it.name },
            isEdit = categoryToEdit != null,
            categoryToEdit = categoryToEdit,
        )
    }

    var showRenameDialog by remember { mutableStateOf(false) }

    if (showRenameDialog) {
        RenamePixListDialog(
            curName = state.curPixList?.name ?: "",
            invalideNames = viewModel.getInvalideNames(),
            onDismiss = { showRenameDialog = false },
            onFinish = { newName ->
                viewModel.updatePixListName(newName)
                showRenameDialog = false
            }
        )
    }

    Scaffold (
        topBar = {
            CustomTopBar(
                title = { Text(
                    text = if (state.curPixList != null) {
                        state.curPixList!!.name
                    } else {
                        stringResource(R.string.app_name)
                    },
                    modifier = Modifier
                        .clickable {
                            showRenameDialog = true
                        }
                ) },
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
                openDrawer = openDrawer
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
                                                            } else if (pixCategory.name != "") {
                                                                if (pixCategory.color != null && !pixCategory.color!!.isPlaceholder) {
                                                                    Icon(
                                                                        imageVector = FilledPixIcon,
                                                                        contentDescription = "Pix",
                                                                        tint = pixCategory.color!!.toColor(),
                                                                    )
                                                                } else {
                                                                    Icon(
                                                                        imageVector = Icons.Default.CheckBoxOutlineBlank,
                                                                        contentDescription = "Empty Pix",
                                                                        tint = MaterialTheme.colorScheme.error
                                                                    )
                                                                }
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
                                        if (color != null && !color.isPlaceholder) {
                                            Icon(
                                                imageVector = FilledPixIcon,
                                                contentDescription = "Category Pix",
                                                tint = color.toColor(),
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.CheckBoxOutlineBlank,
                                                contentDescription = "Empty Category Pix",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
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