package de.gathok.pixcount.manageColors

import FilledPixIcon
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.gathok.pixcount.R
import de.gathok.pixcount.db.PixColor
import de.gathok.pixcount.ui.CustomTopBar
import de.gathok.pixcount.ui.customDialogs.ColorDialog
import de.gathok.pixcount.ui.customDialogs.CustomDialog

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ManageColorsScreen(
    viewModel: ManageColorsViewModel,
    openDrawer: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    var showColorDialog by remember { mutableStateOf(false) }
    var colorToEdit by remember { mutableStateOf<PixColor?>(null) }

    if (showColorDialog) {
        ColorDialog(
            onDismiss = {
                showColorDialog = false
                colorToEdit = null
            },
            onFinish = { newName, newRgb, isEdit ->
                if (isEdit) {
                    viewModel.updateColor(colorToEdit!!, newName, newRgb)
                } else {
                    viewModel.addColor(PixColor(
                        name = newName!!,
                        red = newRgb!![0],
                        green = newRgb[1],
                        blue = newRgb[2]
                    ))
                }
                showColorDialog = false
                colorToEdit = null
            },
            onDelete = {
                colorToEdit?.let {
                    viewModel.deleteColor(it)
                }
                showColorDialog = false
                colorToEdit = null
            },
            invalidNames = state.colorList.map { it.name },
            isEdit = colorToEdit != null,
            colorToEdit = colorToEdit
        )
    }

    var showDeleteUnusedDialog by remember { mutableStateOf(false) }

    if (showDeleteUnusedDialog) {
        CustomDialog(
            onDismissRequest = {
                showDeleteUnusedDialog = false
            },
            title = {
                Text("Delete Unused Colors")
            },
            leftIcon = {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable {
                        showDeleteUnusedDialog = false
                    }
                )
            },
            rightIcon = {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        viewModel.deleteUnusedColors()
                        showDeleteUnusedDialog = false
                    }
                )
            }
        ) {
            Text("Are you sure you want to delete all colors that are not used in any Category?")
        }
    }

    Scaffold (
        topBar = {
            CustomTopBar(
                title = { Text(
                    text = stringResource(R.string.manage_colors),
                    modifier = Modifier
                        .combinedClickable (
                            onClick = {
                                showDeleteUnusedDialog = true
                            },
                            onLongClick = {
                                viewModel.loadDefaultColors()
                            }
                        )
                ) },
                actions = {
                    IconButton(
                        onClick = {
                            showColorDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Add Color",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                openDrawer = openDrawer
            )
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            // TODO: Search bar

            LazyColumn (
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(state.colorList) { color ->
                    if (!color.isPlaceholder) {
                        ColorItem(
                            color = color,
                            count = state.colorUses[color.id] ?: 0,
                            modifier = Modifier
                                .padding(16.dp)
                                .clickable {
                                    colorToEdit = color
                                    showColorDialog = true
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColorItem(
    color: PixColor,
    modifier: Modifier = Modifier,
    count: Int = 0,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column {
            Icon(
                imageVector = FilledPixIcon,
                contentDescription = "Preview Pix",
                tint = color.toColor()
            )
        }
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        ) {
            Text(
                text = color.name,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = color.toHex(),
                style = MaterialTheme.typography.bodySmall
            )
        }
//        if (uses > 0) {
            Column {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.bodySmall
                )
            }
//        }
    }
}