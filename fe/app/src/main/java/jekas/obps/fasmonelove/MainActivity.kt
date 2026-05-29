package jekas.obps.fasmonelove

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import jekas.obps.fasmonelove.ui.theme.FasmOneLoveTheme
import jekas.obps.fasmonelove.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FasmOneLoveTheme {
                MainScreen()
            }
        }
    }
}