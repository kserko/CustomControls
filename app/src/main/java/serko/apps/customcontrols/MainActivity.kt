package serko.apps.customcontrols

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import serko.apps.customcontrols.ui.theme.CustomControlsTheme
import kotlin.math.floor

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val temperature = remember { mutableStateOf(Temperature()) }
            CustomControlsTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize(),

                        ) {
                        RadialTemperatureDisplay(temperature = temperature.value)
                        TemperatureControls(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-100).dp), // move the controls closer to the display
                            temperature = temperature.value
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemperatureControls(
    modifier: Modifier,
    temperature: Temperature
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = {
            if (temperature.isNotAtMinimum) {
                temperature.targetTemperature -= 0.5f
            }
        }) {
            Icon(
                painterResource(R.drawable.ic_minus),
                contentDescription = null
            )
        }
        IconButton(onClick = {
            if (temperature.isNotAtMaximum) {
                println("target ${temperature.targetTemperature + 0.5}")
                temperature.targetTemperature += 0.5f
            }
        }) {
            Icon(
                painterResource(R.drawable.ic_plus),
                contentDescription = null
            )
        }
    }
}

@Composable
fun RadialTemperatureDisplay(
    temperature: Temperature
) {
    Canvas(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .height(500.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val center = Offset(x = canvasWidth / 2, y = canvasHeight / 2)
        val radius = (size.minDimension / 2.0f) //changing the divider here will change the size of the radial display

        val minTemperature = temperature.minTemperature
        val maxTemperature = temperature.maxTemperature
        val temperatureIncrements = temperature.increments
        val numItems = ((maxTemperature - minTemperature) / temperatureIncrements) + 1 // +1 to include an item for the max temperature
        val sweepAngle = 220 // the degrees that we want to draw
        val angleIncrements = sweepAngle / numItems

        val divider = 7f
        val lineLength = radius / divider

        //the angle at which our target temperature line is drawn
        val targetTemperatureAngle = floor(((temperature.targetTemperature - minTemperature) / temperatureIncrements) * angleIncrements)

        repeat(numItems.toInt()) {
            //the angle at which we are drawing the line for each temperature point
            val angle = floor(it / numItems * sweepAngle)

            //if the angle is lower or equal to the target temperature's angle then alpha should be 1
            val alpha = if (angle <= targetTemperatureAngle) 1.0f else 0.4f

            val yOffset = -140f //negative value will create a slightly curved radial display
            //rotate the Canvas and draw a line
            rotate(angle) {
                val start = center - Offset(radius, yOffset)
                val end = start + Offset(lineLength, yOffset / divider) //end yOffset is relative to the line length so we divide with the same divider
                drawLine(
                    color = Color.Magenta,
                    start = start,
                    end = end,
                    cap = StrokeCap.Round,
                    alpha = alpha,
                    strokeWidth = 15f
                )
            }
        }

    }
}

data class Temperature(
    var targetTemperature: Float = 19f,
    val minTemperature: Float = 12f,
    val maxTemperature: Float = 32f,
    val increments: Float = 0.5f
) {
    val isNotAtMinimum get() = targetTemperature != minTemperature
    val isNotAtMaximum get() = targetTemperature != maxTemperature

}