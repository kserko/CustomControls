package serko.apps.customcontrols

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import serko.apps.customcontrols.ui.RadialTemperatureDisplay
import serko.apps.customcontrols.ui.TemperatureControlButtons
import serko.apps.customcontrols.ui.theme.CustomControlsTheme

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Content()
        }
    }

    @Composable
    private fun Content() {
        val temperatureDataObserver = remember { mutableStateOf(TemperatureData()) }

        CustomControlsTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally

                ) {
                    RadialTemperatureDisplay(temperatureData = temperatureDataObserver.value)
                    TemperatureLabel(temperatureDataObserver)
                    TemperatureControlButtons(
                        modifier = Modifier.offset(y = (-170).dp), // move the controls closer to the display
                        temperatureDataObserver = temperatureDataObserver,
                    )
                }
            }
        }
    }

    @Composable
    private fun TemperatureLabel(temperatureDataObserver: MutableState<TemperatureData>) {
        Text(
            text = String.format("%.1f", temperatureDataObserver.value.targetTemperature),
            style = TextStyle(color = Color.White, fontSize = 74.sp),
            modifier = Modifier.offset(y = ((-260).dp))
        )
    }
}


