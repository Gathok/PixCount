package de.gathok.pixcount.ui.customDialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.gathok.pixcount.R

@Composable
fun RenamePixListDialog(
    curName: String,
    invalideNames: List<String>,
    onDismiss: () -> Unit,
    onFinish: (String) -> Unit,
) {
    var name by remember { mutableStateOf(curName) }

    var validToFinish by remember { mutableStateOf(false) }

    LaunchedEffect(name) {
        validToFinish = name.isNotBlank() && !invalideNames.contains(name)
    }

    CustomDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(
                text = stringResource(R.string.rename_pixlist),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        leftIcon = {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable { onDismiss() }
            )
        },
        rightIcon = {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Rename",
                tint = if (validToFinish) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.clickable {
                    if (validToFinish) {
                        onFinish(name)
                    }
                }
            )
        }
    ) {
        var isError by remember { mutableStateOf(invalideNames.contains(name) xor (name == curName)) }

        LaunchedEffect(name) {
            isError = invalideNames.contains(name) xor (name == curName)
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (isError) {
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
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.name)) },
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
    }
}