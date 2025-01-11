package de.gathok.pixcount.ui.customDialogs

import FilledPixIcon
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.CalendarLocale
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults.DecorationBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.gathok.pixcount.R
import de.gathok.pixcount.db.PixCategory
import de.gathok.pixcount.db.PixColor
import de.gathok.pixcount.ui.theme.PixCountTheme
import de.gathok.pixcount.util.Months
import io.realm.kotlin.internal.platform.currentTime
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// NewEntryDialog ----------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDialog(
    categories: List<PixCategory>,
    onDismiss: () -> Unit,
    onEdit: (Int, Months, PixCategory?) -> Unit,
    startDate: Long = currentTime().epochSeconds * 1000 + currentTime().nanosecondsOfSecond / 1_000_000,
    curCategory: PixCategory = categories.first(),
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        val year = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startDate), ZoneId.systemDefault()).year

        val showDatePickerDialog = remember { mutableStateOf(false) }
        val dateStatePicker by remember {
            mutableStateOf(DatePickerState(
                locale = CalendarLocale("de", "DE"),
                initialSelectedDateMillis = startDate,
                yearRange = year..year
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

        var selectedCategory by remember { mutableStateOf(curCategory) }

        CustomDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    stringResource(R.string.new_entry),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            leftIcon = {
                Row {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Edit Date",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable {
                            val selectedDate = formatTimestamp(dateStatePicker.selectedDateMillis!!)
                            val day = selectedDate.substring(0, 2).toInt()
                            val month = Months.getByIndex(selectedDate.substring(3, 5).toInt())
                            onEdit(day, month, null)
                        }
                    )
                }
            },
            rightIcon = {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Add",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        val selectedDate = formatTimestamp(dateStatePicker.selectedDateMillis!!)
                        val day = selectedDate.substring(0, 2).toInt()
                        val month = Months.getByIndex(selectedDate.substring(3, 5).toInt())
                        onEdit(day, month, selectedCategory)
                    }
                )
            },
            modifier = Modifier.padding(16.dp),
        ) {
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
                        label = {
                            Text(
                                stringResource(R.string.date),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
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
                    selectedOption = Pair(selectedCategory, selectedCategory.name),
                    optionIcon = { category ->
                        category as PixCategory
                        Icon(
                            imageVector = FilledPixIcon,
                            contentDescription = "Color",
                            tint = category.color!!.toColor(),
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedText(
    value: String,
    label: @Composable () -> Unit = {},
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    DecorationBox (
        value = "Value",
        innerTextField = {
            Row (
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = value,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (trailingIcon != null) {
                    trailingIcon()
                }
            }
        },
        enabled = true,
        singleLine = true,
        visualTransformation = VisualTransformation.None,
        interactionSource = remember { MutableInteractionSource() },
        label = label,
    )
//        Surface(
//            modifier = modifier
//                .border(
//                    1.dp,
//                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
//                    shape = MaterialTheme.shapes.extraSmall
//                )
//                .padding(top = 16.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
//        ) {
//            Row (
//                modifier = Modifier
//                    .fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text(
//                    text = value,
//                    modifier = Modifier.verticalScroll(scrollState)
//                )
//                if (trailingIcon != null) {
//                    trailingIcon()
//                }
//            }
//
//        }
//    }
}

fun formatTimestamp(timestamp: Long): String {
    val formatter = DateTimeFormatter
        .ofPattern("dd.MM.yyyy")
        .withZone(ZoneId.systemDefault()) // Set the time zone, e.g., your local time zone
    return formatter.format(Instant.ofEpochMilli(timestamp))
}

@Preview
@Composable
private fun EntryDialogPreview() {
    PixCountTheme (
        darkTheme = true
    ) {
        EntryDialog(
            categories = listOf(
                PixCategory("Category 1", PixColor("Color 1", 1f, 0f, 0f, 1f)),
                PixCategory("Category 2", PixColor("Color 2", 0f, 1f, 0f, 1f)),
                PixCategory("Category 3", PixColor("Color 3", 0f, 0f, 1f, 1f)),
            ),
            onDismiss = { },
            onEdit = { _, _, _ -> }
        )
    }
}