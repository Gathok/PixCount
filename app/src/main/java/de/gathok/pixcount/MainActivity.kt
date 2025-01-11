package de.gathok.pixcount

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.gathok.pixcount.main.MainScreen
import de.gathok.pixcount.main.MainViewModel
import de.gathok.pixcount.ui.theme.PixCountTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PixCountTheme {
                MainScreen(
                    viewModel = MainViewModel()
                )
            }
        }
    }
}
