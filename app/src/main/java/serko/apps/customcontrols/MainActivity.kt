package serko.apps.customcontrols

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import serko.apps.customcontrols.TemperatureDirection.DOWN
import serko.apps.customcontrols.TemperatureDirection.UP
import serko.apps.customcontrols.ui.theme.CustomControlsTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

//Defines the rate at which consecutive user interactions are recorded on the UI when the user holds a finger down - lower is faster, higher is slow
//Also used as the tween animation speed at which the temperature indicator moves so it keeps up with the rate of temperature change
const val InteractionDelay = 130L

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
        val temperatureObserver = remember { mutableStateOf(Temperature()) }

        CustomControlsTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally

                ) {
                    RadialTemperatureDisplay(temperature = temperatureObserver.value)
                    TemperatureLabel(temperatureObserver)
                    TemperatureControls(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-150).dp), // move the controls closer to the display
                        temperatureObserver = temperatureObserver,
                    )
                }
            }
        }
    }

    @Composable
    private fun TemperatureLabel(temperatureObserver: MutableState<Temperature>) {
        Text(
            text = temperatureObserver.value.targetTemperature.toString(),
            style = TextStyle(color = Color.White, fontSize = 74.sp),
            modifier = Modifier.offset(y = ((-300).dp))
        )
    }
}

enum class TemperatureDirection {
    UP,
    DOWN,
}

@Composable
private fun TemperatureControls(
    modifier: Modifier,
    temperatureObserver: MutableState<Temperature>,
) {
    val temperature = temperatureObserver.value
    val newTemperature = remember { mutableStateOf(temperature.targetTemperature) }
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        var downPressed by remember { mutableStateOf(false) }
        Image(
            modifier = Modifier
                .padding(12.dp)
                .repeatedPressInterceptor(
                    interactionSource,
                    block = {
                        downPressed = true
                        if (newTemperature.value > temperature.minTemperature) {
                            changeTemperature(
                                temperatureDirection = DOWN,
                                newTemperature = newTemperature,
                                temperatureObserver = temperatureObserver
                            )
                        }
                    }),
            painter = painterResource(id = R.drawable.ic_minus),
            colorFilter = ColorFilter.tint(Color.White),
            contentDescription = null,
        )

        var upPressed by remember { mutableStateOf(false) }
        Image(
            modifier = Modifier
                .padding(12.dp)
                .repeatedPressInterceptor(
                    interactionSource,
                    block = {
                        upPressed = true
                        if (newTemperature.value < temperature.maxTemperature) {
                            changeTemperature(
                                temperatureDirection = UP,
                                newTemperature = newTemperature,
                                temperatureObserver = temperatureObserver
                            )
                        }
                    }),
            painter = painterResource(id = R.drawable.ic_plus),
            colorFilter = ColorFilter.tint(Color.White),
            contentDescription = null,
        )

        HapticFeedback(newTemperature.value)
    }
}

fun Modifier.repeatedPressInterceptor(interactionSource: InteractionSource, block: () -> Unit): Modifier =
    pointerInput(interactionSource) {
        forEachGesture {
            coroutineScope {
                awaitPointerEventScope {
                    val downAction = awaitFirstDown(requireUnconsumed = false)
                    val heldButtonJob = launch {
                        while (downAction.pressed) {
                            block()

                            //Need this to process consecutive gesture inputs. If removed completely the app will freeze, crash and die
                            delay(InteractionDelay)
                        }
                    }
                    waitForUpOrCancellation()
                    heldButtonJob.cancel()
                }
            }
        }
    }

private fun changeTemperature(
    temperatureDirection: TemperatureDirection,
    newTemperature: MutableState<Float>,
    temperatureObserver: MutableState<Temperature>
) {
    val temperature = temperatureObserver.value
    when (temperatureDirection) {
        DOWN -> newTemperature.value = newTemperature.value - 0.5f
        UP -> newTemperature.value = newTemperature.value + 0.5f
    }
    if (newTemperature.value <= temperature.maxTemperature || newTemperature.value >= temperature.minTemperature) {
        temperatureObserver.value = temperature.copy(targetTemperature = newTemperature.value)
    }
}

@Composable
fun RadialTemperatureDisplay(
    temperature: Temperature,
) {
    val minTemperaturePoint = temperature.minTemperature
    val maxTemperaturePoint = temperature.maxTemperature
    val temperaturePointIncrements = temperature.increments
    val numItems = ((maxTemperaturePoint - minTemperaturePoint) / temperaturePointIncrements) + 1 // +1 to include an item for the max temperature
    val sweepAngle = 250f
    val temperatureAngleIncrements = sweepAngle / numItems

    // The offset needed to the starting position of the circle to the desired position. If you picture the circle as a clock face,
    // an offset of 0 degrees would start drawing at 3:00. An offset of 150 degrees moves the starting position to about 7:00 so we match the designs
    val degreesOffset = 150f
    val animatedIndicatorAngle = remember { Animatable(degreesOffset) }

    val targetTemperatureIndex = ((temperature.targetTemperature - minTemperaturePoint) / temperaturePointIncrements).toInt()
    val targetTemperatureAngle = temperatureAngleIncrements * targetTemperatureIndex
    val animatedVisibility = remember { Animatable(0.1f) }

    val firstLaunch = remember { mutableStateOf(true) }

    //every time the target Temperature changes do something
    LaunchedEffect(targetTemperatureIndex) {
        launch {
            //Animate the visibility of the radial display
            animatedVisibility.animateTo(1f, animationSpec = tween(1000))
        }
        launch {
            //animate the indicator slowly on first launch but then speed it up for user interactions
            val tweenSpeed = if (firstLaunch.value) 1400 else InteractionDelay.toInt()
            val targetAngle = ((temperatureAngleIncrements * targetTemperatureIndex) + degreesOffset)
            animatedIndicatorAngle.animateTo(targetAngle, animationSpec = tween(tweenSpeed))
            firstLaunch.value = false
        }
    }

    Canvas(
        modifier = Modifier
            .padding(24.dp)
            .width(360.dp)
            .height(360.dp)
    ) {
        val radius = (size.minDimension / 1.8f) //changing the divider here will change the size of the radial display
        val lineLength = (radius * 0.8f) // the length of each line drawn along the path

        (0 until numItems.toInt()).forEach { temperatureIndex ->
            val angleDiff = ((temperatureAngleIncrements * temperatureIndex) + degreesOffset)
            val temperatureAngle = temperatureAngleIncrements * temperatureIndex

            //Dim the line for temperatures that are beyond the target temperature
            val alphaValue =
                if (temperatureAngle <= targetTemperatureAngle) animatedVisibility.value else animatedVisibility.value * 0.2f

            val quarter = sweepAngle / 3

            //TODO: refactor this
            val color = if (temperatureAngle < (quarter)) {
                TemperatureColor.Cold
            } else if (temperatureAngle > quarter && temperatureAngle < quarter * 2) {
                TemperatureColor.Warm
            } else TemperatureColor.Hot

            drawRadialControl(angleDiff, radius, lineLength, alphaValue, color)
            drawRadialControlIndicator(animatedIndicatorAngle, radius, lineLength, alphaValue)
        }
    }
}

/** Draws a longer thicker line over the target temperature only
 *  and acts as an indicator on the radial display
 */
private fun DrawScope.drawRadialControlIndicator(
    animatedAngle: Animatable<Float, AnimationVector1D>,
    radius: Float,
    lineLength: Float,
    alphaValue: Float
) {
    //the angleDiff in Radians
    val angleDiffRadians = (animatedAngle.value * (PI / 180f)).toFloat()

    val extraLength = 20f

    val start = Offset(
        x = (radius + extraLength) * cos(angleDiffRadians) + size.center.x,
        y = (radius + extraLength) * sin(angleDiffRadians) + size.center.y
    )
    val end = Offset(
        x = (lineLength - extraLength) * cos(angleDiffRadians) + size.center.x,
        y = (lineLength - extraLength) * sin(angleDiffRadians) + size.center.y
    )

    drawLine(
        color = Color.White,
        start = start,
        end = end,
        cap = StrokeCap.Round,
        alpha = alphaValue,
        strokeWidth = 18f
    )
}

enum class TemperatureColor(val color: Color) {
    Cold(color = Color(red = 100, green = 100, blue = 255)),
    Warm(color = Color(red = 200, green = 100, blue = 100)),
    Hot(color = Color(red = 255, green = 50, blue = 50)),
}

/**
 * Calculate the angle from the circle's origin for a given angle.
 * Then draw the line with the specified coordinates and line length
 */
private fun DrawScope.drawRadialControl(
    angleDiff: Float,
    radius: Float,
    lineLength: Float,
    alphaValue: Float,
    color: TemperatureColor
) {
    //the angleDiff in Radians
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
        color = color.color,
        start = start,
        end = end,
        cap = StrokeCap.Round,
        alpha = alphaValue,
        strokeWidth = 13f
    )
}

data class Temperature(
    val targetTemperature: Float = 22f,
    val minTemperature: Float = 10f,
    val maxTemperature: Float = 30f,
    val increments: Float = 0.5f
)

@Composable
fun HapticFeedback(newTemperature: Float) {
    LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)
    print(newTemperature)
}
