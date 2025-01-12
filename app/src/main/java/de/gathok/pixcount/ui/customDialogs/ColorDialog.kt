package de.gathok.pixcount.ui.customDialogs

import FilledPixIcon
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.gathok.pixcount.R
import de.gathok.pixcount.db.PixColor

@Composable
fun ColorDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onFinish: (newName: String?, newRgb: List<Float>?, Boolean) -> Unit,
    onDelete: () -> Unit,
    invalidNames: List<String> = emptyList(),
    isEdit: Boolean = false,
    colorToEdit: PixColor? = null,
) {
    var name by remember { mutableStateOf(colorToEdit?.name ?: "") }
    var selectedHexValue by remember { mutableStateOf(colorToEdit?.toHex() ?: "#") }
    var selectedRgbValues by remember { mutableStateOf(colorToEdit?.getRgbValues() ?: listOf(0f, 0f, 0f)) }
    var mode by remember { mutableStateOf(Mode.HEX) }

    var validToSave by remember { mutableStateOf(false) }

    LaunchedEffect(name, selectedHexValue, selectedRgbValues, mode) {
        validToSave = name.isNotBlank() &&
            (!invalidNames.contains(name.trim()) xor (name.trim() == (colorToEdit?.name ?: "")) &&
            (mode == Mode.HEX && isValidHexColor(selectedHexValue) &&
                (!isEdit || selectedHexValue != (colorToEdit?.toHex() ?: "") || name.trim() != (colorToEdit?.name ?: "")) ||
            mode == Mode.RGB &&
                (!isEdit || selectedRgbValues != (colorToEdit?.getRgbValues() ?: listOf<Float>()) || name.trim() != (colorToEdit?.name ?: ""))))
    }

    CustomDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) {
                    stringResource(R.string.edit_color)
                } else {
                    stringResource(R.string.add_color)
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
                imageVector = Icons.Default.Done,
                contentDescription = "Done",
                tint = if (validToSave) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                },
                modifier = Modifier.clickable {
                    onFinish(
                        if (name.trim() == (colorToEdit?.name ?: "")) null else name,
                        if (mode == Mode.HEX) {
                            if (selectedHexValue == (colorToEdit?.toHex() ?: "")) null else hexToRgb(selectedHexValue)
                        }
                        else if (mode == Mode.RGB) {
                            if (selectedRgbValues == (colorToEdit?.getRgbValues() ?: listOf<Float>())) null else selectedRgbValues
                        }
                        else null,
                        isEdit,
                    )
                }
            )
        },
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        if (invalidNames.contains(name.trim()) && name.trim() != (colorToEdit?.name ?: "")) {
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
                isError = invalidNames.contains(name.trim()) && name.trim() != (colorToEdit?.name ?: ""),
            )
        }
        // TODO: Add mode switcher
        when (mode) {
            Mode.HEX -> {
                OutlinedTextField(
                    value = selectedHexValue,
                    onValueChange = {
                        if (it.length in 1..7) {
                            selectedHexValue = it
                        }
                    },
                    label = { Text(stringResource(R.string.color)) },
                    modifier = Modifier
                        .fillMaxWidth(),
                    isError = !isValidHexColor(selectedHexValue),
                    trailingIcon = {
                        if (isValidHexColor(selectedHexValue)) {
                            val tempRbgValues = hexToRgb(selectedHexValue)
                            Icon(
                                imageVector = FilledPixIcon,
                                contentDescription = "Preview",
                                tint = Color(red = tempRbgValues[0], green = tempRbgValues[1], blue = tempRbgValues[2]),
                            )
                        }
                    }
                )
            }
            Mode.RGB -> {
                // TODO: Add RGB input fields
            }
        }
    }
}

fun isValidHexColor(hex: String): Boolean {
    val regex = Regex("^#?([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")
    return regex.matches(hex)
}

fun hexToRgb(hexString: String): List<Float> {
    var red = 0f
    var green = 0f
    var blue = 0f
    when (hexString.length) {
        7 -> {
            red = hexString.substring(1, 3).toInt(16) / 255f
            green = hexString.substring(3, 5).toInt(16) / 255f
            blue = hexString.substring(5, 7).toInt(16) / 255f
        }
        4 -> {
            red = hexString.substring(1, 2).toInt(16) / 15f
            green = hexString.substring(2, 3).toInt(16) / 15f
            blue = hexString.substring(3, 4).toInt(16) / 15f
        }
    }
    return listOf(red, green, blue)
}

enum class Mode {
    HEX, RGB
}

