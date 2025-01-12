package de.gathok.pixcount.ui.customDialogs

import FilledPixIcon
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.gathok.pixcount.R
import de.gathok.pixcount.db.PixCategory
import de.gathok.pixcount.db.PixColor

// NewCategoryDialog ----------------------------------------------------------------
@Composable
fun CategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (String?, PixColor?, Boolean) -> Unit,
    onDelete: () -> Unit,
    colors: List<PixColor> = emptyList(),
    invalidNames: List<String> = emptyList(),
    isEdit: Boolean = false,
    categoryToEdit: PixCategory? = null,
) {
    var name by remember { mutableStateOf(categoryToEdit?.name ?: "") }
    var color by remember { mutableStateOf(categoryToEdit?.color ?: colors.firstOrNull() ?: PixColor()) }

    CustomDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) {
                    stringResource(R.string.edit_category)
                } else {
                    stringResource(R.string.add_category)
                },
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
                if (isEdit) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable { onDelete() }
                    )
                }
            }
        },
        rightIcon = {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Add",
                tint = if (name.isNotBlank() && !color.isPlaceholder &&
                    (!invalidNames.contains(name.trim()) xor (name.trim() == (categoryToEdit?.name ?: ""))) &&
                    (name.trim() != (categoryToEdit?.name ?: "") || color != categoryToEdit?.color)
                ) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                },
                modifier = Modifier
                    .clickable {
                        if (name.isNotBlank() && !color.isPlaceholder &&
                            (!invalidNames.contains(name.trim()) xor (name.trim() == (categoryToEdit?.name ?: ""))) &&
                            (name.trim() != (categoryToEdit?.name ?: "") || color != categoryToEdit?.color)
                        ) {
                            onAdd(
                                if (name.trim() == (categoryToEdit?.name ?: "")) null else name,
                                if (color == categoryToEdit?.color) null else color,
                                isEdit
                            )
                        }
                    },
            )
        }
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        if (invalidNames.contains(name.trim()) && name.trim() != (categoryToEdit?.name ?: "")) {
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                modifier = Modifier
                    .fillMaxWidth(),
                isError = invalidNames.contains(name.trim()) && name.trim() != (categoryToEdit?.name ?: ""),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Dropdown(
                    modifier = Modifier
                        .fillMaxWidth(),
                    options = colors.filter { !it.isPlaceholder }.associateBy({ it }, { it.name }),
                    label = stringResource(R.string.color),
                    onValueChanged = { color = it as PixColor },
                    selectedOption = Pair(color, color.name),
                    optionIcon = { color ->
                        color as PixColor
                        Icon(
                            imageVector = FilledPixIcon,
                            contentDescription = "Color",
                            tint = color.toColor(),
                        )
                    }
                )
            }
//            Column ( TODO: Implement custom color
//                modifier = Modifier
//                    .fillMaxWidth(),
//                horizontalAlignment = Alignment.End,
//                verticalArrangement = Arrangement.Center,
//            ) {
//                Icon(
//                    imageVector = Icons.Default.AddCircle,
//                    contentDescription = "Add Color",
//                    tint = MaterialTheme.colorScheme.onSurface,
//                    modifier = Modifier
//                        .padding(top = 10.dp)
//                        .clickable {
//
//                        }
//                )
//            }
        }
    }
}

//@Preview(apiLevel = 34)
//@Composable
//private fun CategoryDialogPreview() {
//    PixCountTheme(
//        darkTheme = true
//    ) {
//        CategoryDialog(
//            onDismiss = { },
//            onAdd = { _, _, _ -> },
//            isEdit = true,
//            categoryToEdit = PixCategory(name = "Geilo", color = PixColor(name = "Farbe", color = Color.Red)),
//        )
//    }
//}