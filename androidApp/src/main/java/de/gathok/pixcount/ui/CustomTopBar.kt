package de.gathok.pixcount.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import de.gathok.pixcount.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopBar(
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    openDrawer: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = title,
        actions = actions,
        navigationIcon = {
            IconButton(
                onClick = {
                    openDrawer()
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