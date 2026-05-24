package jekas.obps.fasmonelove

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,  // picks up Verdana/Playwrite
        color = MaterialTheme.colorScheme.onBackground  // picks up TextWhite
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FasmOneLoveTheme {
        Greeting("Android")
    }
}