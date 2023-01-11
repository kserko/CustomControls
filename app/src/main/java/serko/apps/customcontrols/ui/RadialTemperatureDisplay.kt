package serko.apps.customcontrols.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import serko.apps.customcontrols.TemperatureData
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

//Defines the rate at which consecutive user interactions are recorded on the UI when the user holds a finger down - lower is faster, higher is slow
//Also used as the tween animation speed at which the temperature indicator moves so it keeps up with the rate of temperature change
const val InteractionDelay = 100L

enum class TemperatureColor(val color: Color) {
    Cold(color = Color(red = 100, green = 100, blue = 255)),
    Warm(color = Color(red = 250, green = 100, blue = 100)),
    Hot(color = Color(red = 255, green = 0, blue = 0)),
}
@Composable
fun RadialTemperatureDisplay(
    temperatureData: TemperatureData,
) {
    val minTemperaturePoint = temperatureData.minTemperature
    val maxTemperaturePoint = temperatureData.maxTemperature
    val temperaturePointIncrements = temperatureData.increments
    val numItems = ((maxTemperaturePoint - minTemperaturePoint) / temperaturePointIncrements) + 1 // +1 to include an item for the max temperature
    val sweepAngle = 250f
    val temperatureAngleIncrements = sweepAngle / numItems

    // The offset needed to the starting position of the circle to the desired position. If you picture the circle as a clock face,
    // an offset of 0 degrees would start drawing at 3:00. An offset of 150 degrees moves the starting position to about 7:00 so we match the designs
    val degreesOffset = 150f
    val animatedIndicatorAngle = remember { Animatable(degreesOffset) }

    val targetTemperatureIndex = ((temperatureData.targetTemperature - minTemperaturePoint) / temperaturePointIncrements).toInt()
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
            val alphaValue = if (temperatureAngle <= targetTemperatureAngle) animatedVisibility.value else animatedVisibility.value * 0.2f

            val color = temperatureAngle.getTemperatureColorForAngle(sweepAngle)
            drawRadialTemperatureDisplay(angleDiff, radius, lineLength, alphaValue, color)
            drawRadialControlIndicator(animatedIndicatorAngle, radius, lineLength, alphaValue)
        }
    }
}

/**
 * breaks the total number of angles into equal segments and returns a different color for each
 */
private fun Float.getTemperatureColorForAngle(sweepAngle: Float): TemperatureColor {
    //sweepAngle is the angles we're drawing in total. Splitting in 3 we get the max number of angles each segment represents
    val segmentMaxAngle = sweepAngle / 3

    val segmentCold = segmentMaxAngle
    val segmentWarm = segmentMaxAngle * 2

    val isBetweenColdAndHotSegments = this > segmentCold && this < segmentWarm

    val color = when {
        this < segmentCold -> TemperatureColor.Cold
        isBetweenColdAndHotSegments -> TemperatureColor.Warm
        else -> TemperatureColor.Hot
    }
    return color
}

/**
 * Draws a longer thicker line over the target temperature only
 * and acts as an indicator on the radial display
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

/**
 * Draws the circular control display with lines at specific angles
 *
 * Calculates the positions of each line from the circle's origin for a given angle and
 * draws the line at the correct angle and length
 *
 */
private fun DrawScope.drawRadialTemperatureDisplay(
    angleDiff: Float,
    radius: Float,
    lineLength: Float,
    alphaValue: Float,
    color: TemperatureColor,
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