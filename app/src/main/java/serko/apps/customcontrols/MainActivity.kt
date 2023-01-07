package serko.apps.customcontrols

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import serko.apps.customcontrols.ui.theme.CustomControlsTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val temperature = remember { mutableStateOf(Temperature()) }
            CustomControlsTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally

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
    val minTemperaturePoint = temperature.minTemperature
    val maxTemperaturePoint = temperature.maxTemperature
    val temperaturePointIncrements = temperature.increments
    val numItems = ((maxTemperaturePoint - minTemperaturePoint) / temperaturePointIncrements) + 1 // +1 to include an item for the max temperature
    val sweepAngle = 250
    val temperatureAngleIncrements = sweepAngle / numItems

    Canvas(
        modifier = Modifier
            .padding(24.dp)
            .width(360.dp)
            .height(360.dp)
    ) {
        val radius = (size.minDimension / 1.8f) //changing the divider here will change the size of the radial display
        val lineLength = radius * 0.8f // the length of each line drawn along the path

        val targetTemperatureIndex = floor(((temperature.targetTemperature - minTemperaturePoint) / temperaturePointIncrements))
        val targetTemperatureAngle = temperatureAngleIncrements * targetTemperatureIndex

        //Iterate over each temperature and calculate the angle from the circle's origin. Then draw the
        //line with the specified coordinates and line length
        (0 until numItems.toInt()).forEach {temperatureIndex ->
            //the offset added here is affect by the sweepAngle above and vice versa - need to find a formula of correlation
            val angleDiff = ((temperatureAngleIncrements * temperatureIndex) + 150)
            val temperaturePoint = minTemperaturePoint + (temperatureIndex * temperaturePointIncrements)
            val temperatureAngle = temperatureAngleIncrements * temperatureIndex
            println("temperature: $temperaturePoint - angle: $temperatureAngle")

            //Dim the line for temperatures that are beyond the target temperature
            val alpha = if (temperatureAngle <= targetTemperatureAngle) 1.0f else 0.4f

            //the angleDiff in  Radians
            val angleDiffRadians = (angleDiff * (PI / 180f)).toFloat()

            val start = Offset(
                x = radius * cos(angleDiffRadians) + size.center.x,
                y = radius * sin(angleDiffRadians) + size.center.y
            )
            val end = Offset(
                x = lineLength * cos(angleDiffRadians) + size.center.x,
                y = lineLength * sin(angleDiffRadians) + size.center.y
            )

            drawLine(
                color = Color.Magenta,
                start = start,
                end = end,
                cap = StrokeCap.Round,
                alpha = alpha,
                strokeWidth = 13f
            )
        }
    }
}

data class Temperature(
    var targetTemperature: Float = 15f,
    val minTemperature: Float = 10f,
    val maxTemperature: Float = 30f,
    val increments: Float = 0.5f
) {
    val isNotAtMinimum get() = targetTemperature != minTemperature
    val isNotAtMaximum get() = targetTemperature != maxTemperature

}