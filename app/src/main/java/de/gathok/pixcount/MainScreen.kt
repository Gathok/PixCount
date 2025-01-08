@file:OptIn(ExperimentalMaterial3Api::class)

package de.gathok.pixcount

import FilledBox
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material3.CalendarLocale
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.gathok.pixcount.dbObjects.PixCategory
import de.gathok.pixcount.dbObjects.PixColor
import de.gathok.pixcount.ui.theme.PixCountTheme
import de.gathok.pixcount.util.Months
import io.realm.kotlin.internal.platform.currentTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MainScreen(
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var showNewEntryDialog by remember { mutableStateOf(false) }

    var showNewCategoryDialog by remember { mutableStateOf(false) }

    if (showNewEntryDialog) {
        NewEntryDialog(
            categories = state.curCategories,
            onDismiss = { showNewEntryDialog = false },
            onAdd = { day, month, category ->
                viewModel.createPixEntry(day, month, category, state.curPixList!!.id)
                showNewEntryDialog = false
            }
        )
    }

    if (showNewCategoryDialog) {
        NewCategoryDialog(
            onDismiss = { showNewCategoryDialog = false },
            onAdd = { name, color ->
                viewModel.createPixCategory(name, color, state.curPixList!!.id)
                showNewCategoryDialog = false
            },
            colors = state.colorList,
            invalidNames = state.curCategories.map { it.name },
        )
    }

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
                                showNewEntryDialog = true
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
                        onClick = { }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                }
            )
        }
    ) { pad ->
        Box (
            modifier = Modifier
                .padding(pad)
                .padding(horizontal = 8.dp)
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
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (i == 0) {
                                        for (j in 0..31) {
                                            if (j == 0) {
                                                Text("")
                                            } else if (j < 10) {
                                                Text("0$j")
                                            } else {
                                                Text(j.toString())
                                            }
                                        }
                                    } else {
                                        val month = Months.getByIndex(i)
                                        for (day in 0..month.getDaysCount) {
                                            if (day == 0) {
                                                Text(stringResource(month.getShortStringId))
                                            } else {
                                                state.curPixList!!.entries?.getEntry(
                                                    day,
                                                    month
                                                ).let { pixCategory ->
                                                    if (pixCategory == null) { // TODO: This is just a placeholder
                                                        throw IllegalArgumentException("Entries might be null")
                                                    }
                                                    else if (!pixCategory.color!!.isPlaceholder) {
                                                        Icon(
                                                            imageVector = FilledBox,
                                                            contentDescription = "Pix",
                                                            tint = pixCategory.color!!.toColor(),
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckBoxOutlineBlank,
                                                            contentDescription = "Empty Pix",
                                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                        )
                                                    }
                                                }
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
                            .padding(start = 8.dp)
                            .fillMaxSize(),
                    ) {
                        items(state.curCategories) { curCategory ->
                            Row (
                                modifier = Modifier
                                    .clickable {
                                        viewModel.deleteCategory(curCategory, state.curPixList!!.id)
                                    }
                            ) {
                                val color = curCategory.color
                                Icon(
                                    imageVector = FilledBox,
                                    contentDescription = "Category Pix",
                                    tint = color!!.toColor(),
                                )
                                Text(curCategory.name)
                            }
                        }
                        item {
                            Row (
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                IconButton(
                                    onClick = {
                                        showNewCategoryDialog = true
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
                    Text("No Pix Lists")
                }
            }
        }
    }
}

// NewEntryDialog ----------------------------------------------------------------
@Composable
fun NewEntryDialog(
    categories: List<PixCategory>,
    onDismiss: () -> Unit,
    onAdd: (Int, Months, PixCategory) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {

        val showDatePickerDialog = remember { mutableStateOf(false) }
        val dateStatePicker by remember {
            mutableStateOf(DatePickerState(
                locale = CalendarLocale("de"),
                initialSelectedDateMillis =
                    currentTime().epochSeconds * 1000 + currentTime().nanosecondsOfSecond / 1_000_000,
            ))
        }
        var oldDateValue by remember { mutableLongStateOf(dateStatePicker.selectedDateMillis!!) }

        LaunchedEffect(showDatePickerDialog.value) {
            if (showDatePickerDialog.value) {
                oldDateValue = dateStatePicker.selectedDateMillis!!
            }
        }

        if (showDatePickerDialog.value) {
            DatePickerDialog(
                onDismissRequest = { showDatePickerDialog.value = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDatePickerDialog.value = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDatePickerDialog.value = false
                            dateStatePicker.selectedDateMillis = oldDateValue
                        }
                    ) {
                        Text("Cancel")
                    }
                },
            ) {
                DatePicker(
                    state = dateStatePicker
                )
            }
        }

        var selectedCategory by remember { mutableStateOf(categories.first()) }

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
                        stringResource(R.string.new_entry),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Add",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            val selectedDate = formatTimestamp(dateStatePicker.selectedDateMillis!!)
                            val day = selectedDate.substring(0, 2).toInt()
                            val month = Months.getByIndex(selectedDate.substring(3, 5).toInt())
                            onAdd(day, month, selectedCategory)
                        }
                    )
                }
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column (
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePickerDialog.value = true },
                        verticalArrangement = Arrangement.Center
                    ) {
                        OutlinedText(
                            value = formatTimestamp(dateStatePicker.selectedDateMillis!!),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = "Edit Date",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        )
                    }
                }
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Dropdown(
                        modifier = Modifier.fillMaxWidth(),
                        options = categories.associateBy({ it }, { it.name }),
                        label = stringResource(R.string.category),
                        onValueChanged = { selectedCategory = it as PixCategory },
                        selectedOption = Pair(selectedCategory, selectedCategory.name)
                    )
                }
            }
        }

    }
}

@Composable
fun OutlinedText(
    value: String,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val scrollState = rememberScrollState()
    Surface(
        modifier = modifier
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.extraSmall
            )
            .padding(16.dp)
    ) {
        Row (
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value,
                modifier = Modifier.verticalScroll(scrollState)
            )
            if (trailingIcon != null) {
                trailingIcon()
            }
        }

    }
}

fun formatTimestamp(timestamp: Long): String {
    val formatter = DateTimeFormatter
        .ofPattern("dd.MM.yyyy")
        .withZone(ZoneId.systemDefault()) // Set the time zone, e.g., your local time zone
    return formatter.format(Instant.ofEpochMilli(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dropdown(
    modifier: Modifier,
    options: Map<Any, String>,
    onValueChanged: (Any) -> Unit,
    label: String,
    selectedOption: Pair<Any, String>,
) {
    var expanded by remember { mutableStateOf(false) }
    var currentInput by remember { mutableStateOf(selectedOption.second) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
            if (expanded)
                currentInput = ""
        },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = !expanded,
            value = if (expanded) currentInput else selectedOption.second,
            onValueChange = {
                currentInput = it
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            label = { Text(label) },
            colors = OutlinedTextFieldDefaults.colors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            singleLine = true,
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.toList().forEach { pair ->
                val (option, text) = pair
                if (text.contains(currentInput, ignoreCase = true)) {
                    DropdownMenuItem(
                        text = { Text(text = text) },
                        onClick = {
                            expanded = false
                            currentInput = text
                            onValueChanged(option)
                        }
                    )
                }
            }
        }
    }

}

@Preview
@Composable
private fun NewEntryDialogPreview() {
    PixCountTheme (
        darkTheme = true
    ) {
        NewEntryDialog(
            categories = listOf(
                PixCategory("Category 1", PixColor("Color 1", 1f, 0f, 0f, 1f)),
                PixCategory("Category 2", PixColor("Color 2", 0f, 1f, 0f, 1f)),
                PixCategory("Category 3", PixColor("Color 3", 0f, 0f, 1f, 1f)),
            ),
            onDismiss = { },
            onAdd = { _, _, _ -> }
        )
    }
}

// NewCategoryDialog ----------------------------------------------------------------
@Composable
fun NewCategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (String, PixColor) -> Unit,
    colors: List<PixColor> = emptyList(),
    invalidNames: List<String> = emptyList()
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(PixColor()) }

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
                        stringResource(R.string.new_category),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Add",
                        tint = if (name.isNotEmpty() && !color.isPlaceholder && !invalidNames.contains(name)) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        },
                        modifier = Modifier
                            .clickable {
                                if (name.isNotEmpty() && !color.isPlaceholder && !invalidNames.contains(name)) {
                                    onAdd(name, color)
                                }
                            },
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (invalidNames.contains(name)) {
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
                        isError = invalidNames.contains(name),
                    )
                }
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column (
                        modifier = Modifier
                            .fillMaxWidth(),
                    ) {
                        Dropdown(
                            modifier = Modifier
                                .fillMaxWidth(),
                            options = colors.associateBy({ it }, { it.name }),
                            label = stringResource(R.string.color),
                            onValueChanged = { color = it as PixColor },
                            selectedOption = Pair(color, color.name)
                        )
                    }
//                    Column ( TODO: Implement custom color
//                        modifier = Modifier
//                            .fillMaxWidth(),
//                        horizontalAlignment = Alignment.End,
//                        verticalArrangement = Arrangement.Center,
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.AddCircle,
//                            contentDescription = "Add Color",
//                            tint = MaterialTheme.colorScheme.onSurface,
//                            modifier = Modifier
//                                .padding(top = 10.dp)
//                                .clickable {

//                                }
//                        )
//                    }
                }
            }
        }
    }
}

@Preview(apiLevel = 34)
@Composable
private fun NewCategoryDialogPreview() {
    PixCountTheme (
        darkTheme = true
    ) {
        NewCategoryDialog(
            onDismiss = { },
            onAdd = { _, _ -> }
        )
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