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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import serko.apps.customcontrols.TemperatureData
import serko.apps.customcontrols.ui.Colors.HotWaterColourLight
import serko.apps.customcontrols.ui.Colors.HotWaterColourDark
import serko.apps.customcontrols.ui.CurrentTemperatureIndicator.Arc
import serko.apps.customcontrols.ui.CurrentTemperatureIndicator.Line
import serko.apps.customcontrols.ui.TemperatureColor.Cold
import serko.apps.customcontrols.ui.TemperatureColor.Hot
import serko.apps.customcontrols.ui.TemperatureColor.Warm
import java.lang.Math.PI
import kotlin.math.cos
import kotlin.math.sin

//Defines the rate at which consecutive user interactions are recorded on the UI when the user holds a finger down - lower is faster, higher is slow
//Also used as the tween animation speed at which the temperature indicator moves so it keeps up with the rate of temperature change
const val InteractionDelay = 100L

enum class CurrentTemperatureIndicator {
    Line,
    Arc,
}

enum class TemperatureColor(val color: Color) {
    Cold(color = Color(red = 100, green = 100, blue = 255)),
    Warm(color = Color(red = 250, green = 100, blue = 100)),
    Hot(color = Color(red = 255, green = 0, blue = 0)),
}

@Composable
fun ThermostatDisplay(
    temperatureDataState: MutableState<TemperatureData>,
) {
    val temperatureData = temperatureDataState.value
    val minTemperaturePoint = temperatureData.minTemperature
    val maxTemperaturePoint = temperatureData.maxTemperature
    val temperaturePointIncrements = temperatureData.increment
    val numItems = ((maxTemperaturePoint - minTemperaturePoint) / temperaturePointIncrements)
    val sweepAngle = 250f //the degrees we will be drawing our content on
    val temperatureAngleIncrements = (sweepAngle / numItems)

    // The offset needed to the starting position of the circle to the desired position. If you picture the circle as a clock face,
    // an offset of 0 degrees would start drawing at 3:00 when computing the x and y coordinates
    // along the path of the circle. An offset of 150 degrees moves the
    // starting position to about 7:00 so we match the designs
    val degreesOffset = 145f

    val currentTemperatureIndex =
        ((temperatureData.currentTemperature - minTemperaturePoint) / temperaturePointIncrements)
            .toInt()
    val currentTemperatureAngle = remember { Animatable(temperatureAngleIncrements * currentTemperatureIndex) }

    val targetTemperatureIndex =
        ((temperatureData.targetTemperature - minTemperaturePoint) / temperaturePointIncrements).toInt()
    val targetTemperatureAngle = remember { Animatable(temperatureAngleIncrements * targetTemperatureIndex) }


    val animatedVisibility = remember { Animatable(0.1f) }
    val firstLaunch = remember { mutableStateOf(true) }

    //every time the target Temperature changes do something
    LaunchedEffect(targetTemperatureIndex, currentTemperatureIndex) {
        launch {
            //Animate the visibility of the radial display
            animatedVisibility.animateTo(1f, animationSpec = tween(1000))
        }
        launch {
            //animate the indicator slowly on first launch but then speed it up for user interactions
            val tweenSpeed = InteractionDelay.toInt() //if (firstLaunch.value) 1400 else

            val targetAngle = ((temperatureAngleIncrements * targetTemperatureIndex) + degreesOffset)
            targetTemperatureAngle.animateTo(targetAngle, animationSpec = tween(tweenSpeed))

            val currentAngle = ((temperatureAngleIncrements * currentTemperatureIndex) + degreesOffset)
            currentTemperatureAngle.animateTo(currentAngle, animationSpec = tween(tweenSpeed))

            firstLaunch.value = false
        }
    }

    Canvas(
        modifier = Modifier
            .width(360.dp)
            .height(380.dp)
            .padding(horizontal = 14.dp)
            .padding(vertical = 24.dp)
    ) {
        val radius =
            (size.minDimension / 1.8f) //changing the divider here will change the size of the radial display

        val lineLength = (radius * 0.8f) // the length of each line drawn along the path

        //Use either the Arc drawn controls here or the line based controls below, not both
        drawArcBasedThermostatControl(
            startAngle = degreesOffset,
            sweepAngle = sweepAngle,
            targetTemperatureAngle = targetTemperatureAngle.value,
            currentTemperatureAngle = currentTemperatureAngle.value,
        )

//        drawLineBasedThermostatControl(
//            numItems,
//            temperatureAngleIncrements,
//            degreesOffset,
//            targetTemperatureAngle,
//            animatedVisibility,
//            sweepAngle,
//            radius,
//            lineLength,
//            currentTemperatureAngle
//        )
    }
}

/**
 * Draws consecutive equidistant lines along a circular path
 * Depending on startAngle and sweepAngle this can be a full circle or a semi-circle
 * Depends on computations happening in LaunchedEffect above to determine
 * the angle that corresponds to a given temperature value
 *
 * The final shape of this doesn't depend on the width/height and padding of its
 * container but it can be smaller/larger depending on those constraints
 */
private fun DrawScope.drawLineBasedThermostatControl(
    numItems: Float,
    temperatureAngleIncrements: Float,
    degreesOffset: Float,
    targetTemperatureAngle: Float,
    animatedVisibility: Animatable<Float, AnimationVector1D>,
    sweepAngle: Float,
    radius: Float,
    lineLength: Float,
    animatedIndicatorAngle: Animatable<Float, AnimationVector1D>
) {
    (0..numItems.toInt()).forEach { temperatureIndex ->
        // For every item compute the angle step along a circle
        val angleStep = ((temperatureAngleIncrements * temperatureIndex) + degreesOffset)
        val temperatureAngle = temperatureAngleIncrements * temperatureIndex

        //Dim the line for temperatures that are beyond the target temperature
        val alphaValue =
            if (temperatureAngle <= targetTemperatureAngle) animatedVisibility.value else animatedVisibility.value * 0.2f

        val color = temperatureAngle.getTemperatureColorForAngle(sweepAngle)
        drawThermostatLines(angleStep, radius, lineLength, alphaValue, color)
        drawLineIndicator(animatedIndicatorAngle, radius, lineLength, alphaValue)
    }
}

/**
 * Draws the circular control display with lines at specific angles
 *
 * Calculates the positions of each line from the circle's origin for a given angle and
 * draws the line at the correct angle and length
 *
 */
private fun DrawScope.drawThermostatLines(
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

/**
 * Draws a longer thicker line over the target temperature only
 * and acts as an indicator on the radial display
 */
private fun DrawScope.drawLineIndicator(
    animatedAngle: Animatable<Float, AnimationVector1D>,
    radius: Float,
    lineLength: Float,
    alphaValue: Float,
) {
    //the angleDiff in Radians
    val angleDiffRadians = (animatedAngle.value * (PI / 180f)).toFloat()

    //find the coordinates along a circle on which a certain angle is positioned
    val start = Offset(
        x = (radius) * cos(angleDiffRadians) + size.center.x,
        y = (radius) * sin(angleDiffRadians) + size.center.y
    )

    val end = Offset(
        x = (lineLength) * cos(angleDiffRadians) + size.center.x,
        y = (lineLength) * sin(angleDiffRadians) + size.center.y
    )

    //use the coordinates above to draw an indicator line
    drawLine(
        color = Color.Yellow,
        start = start,
        end = end,
        cap = StrokeCap.Round,
        alpha = alphaValue,
        strokeWidth = 38f
    )
}

/**
 * Draws an Arc with an optional gradient fill
 * The Arc isn't guaranteed to be a perfect circle because of width/height and padding
 * constraints applied to its container, therefore we treat it as an ellipse and use the
 * appropriate formulae to compute the correct position of the progress indicator
 */
private fun DrawScope.drawArcBasedThermostatControl(
    startAngle: Float,
    sweepAngle: Float,
    targetTemperatureAngle: Float,
    currentTemperatureAngle: Float,
    temperatureIndicator: CurrentTemperatureIndicator = Line,
) {
    val arcWidth = 86f

    //Draw the arc background. This doesn't change and acts as an unfilled version
    //of our control.
    drawArc(
        color = Colors.Background.color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        style = Stroke(width = 100f, cap = StrokeCap.Round)
    )

    //Draw current temperature's progress arc. Changes based on currentTemperatureAngle
    drawArcForTemperature(startAngle, currentTemperatureAngle, arcWidth)

    // Calculate the angles and coordinates for the placement of the target temperature indicator
    val horizontalOffset = size.width / 2
    val verticalOffset = size.height / 2

    //convert the angles we have set for a normal circle to
    //an ellipse equivalent considering that position 0 for the ellipse
    //is at the center bottom of the ellipse not at 3 o'clock as in a normal circle calculation

    //The angleOffset is added in order to move the indicator line a few degrees
    //to cover the arc's rounded edge. If you set it to 0f and set the current and target
    //temperatures to be equal and temperatureIndicator to Line you'll see what I mean
    val angleOffset = 3f
    val ellipseAngle = (targetTemperatureAngle - 90).toDouble() + angleOffset

    //Convert to Radians. Also convert to negative so it works clockwise
    val ellipseAngleRad = Math.toRadians(ellipseAngle).toFloat() * -1

    //Get the x and y coordinates of a given temperature's angle along the ellipse
    val x = size.width / 2 * sin(ellipseAngleRad)
    val y = size.height / 2 * cos(ellipseAngleRad)

    //The position in the arc where we can draw our temperature indicator
    val indicatorCoordinates = Offset(x + horizontalOffset, y + verticalOffset)

    //Draw the target temperature's progress indicator
    when (temperatureIndicator) {
        Line -> {
            drawLineIndicator(
                ellipseAngleRad,
                horizontalOffset,
                verticalOffset,
                indicatorCoordinates
            )
        }
        Arc -> {
            drawArcForTemperature(startAngle, targetTemperatureAngle, arcWidth)
        }
    }
}

/**
 * Draws an arc that where the sweepAngle (i.e how much of the arc is drawn) changes
 * based on currentTemperatureAngle essentially acting as a progress indicator
 */
private fun DrawScope.drawArcForTemperature(
    startAngle: Float,
    temperatureAngle: Float,
    arcWidth: Float
) {
    val brush = Brush.radialGradient(
        0.0f to HotWaterColourDark.color,
        0.5f to HotWaterColourLight.color,
        1.0f to HotWaterColourDark.color,
        radius = 96f, //relates to arcWidth
        center = size.center,
        tileMode = TileMode.Repeated
    )

    drawArc(
        brush = brush,
        startAngle = startAngle,
        sweepAngle = temperatureAngle - startAngle,
        useCenter = false,
        style = Stroke(width = arcWidth, cap = StrokeCap.Round)
    )
}

/**
 * Draws the line indicator. This is comprised of two half lines each starting from the
 * central position on the arc where we have computed the correct angle above and
 * going outward and inward respectively to create the illusion of a single line
 */
private fun DrawScope.drawLineIndicator(
    ellipseAngleRad: Float,
    horizontalOffset: Float,
    verticalOffset: Float,
    indicatorCoordinates: Offset
) {

    //the divider values (1.8f, 2.3f) on the width and height here determine the length of each
    // line. Can be tweaked to change according to requirements
    val line1EndPosition = Offset(
        x = size.width / 1.8f * sin(ellipseAngleRad),
        y = size.height / 1.8f * cos(ellipseAngleRad)
    )

    val line2EndPosition = Offset(
        x = size.width / 2.3f * sin(ellipseAngleRad),
        y = size.height / 2.3f * cos(ellipseAngleRad)
    )
    val line1Coordinates =
        Offset(line1EndPosition.x + horizontalOffset, line1EndPosition.y + verticalOffset)
    val line2Coordinates =
        Offset(line2EndPosition.x + horizontalOffset, line2EndPosition.y + verticalOffset)

    //draws first half of line from center of indicator position and outwards
    arcLineIndicator(indicatorCoordinates, line1Coordinates)
    //draws second half of line from center of indicator position and inwards
    arcLineIndicator(indicatorCoordinates, line2Coordinates)
}

private fun DrawScope.arcLineIndicator(
    indicatorCoordinates: Offset,
    coordinates: Offset
) {
    drawLine(
        color = Color.White,
        start = indicatorCoordinates,
        end = coordinates,
        cap = StrokeCap.Round,
        strokeWidth = 43f
    )
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
        this < segmentCold -> Cold
        isBetweenColdAndHotSegments -> Warm
        else -> Hot
    }
    return color
}


enum class Colors(val color: Color) {
    HotWaterColourLight(Color(red = 166, green = 167, blue = 255, alpha = 255)),
    HotWaterColourDark(Color(red = 102, green = 117, blue = 246, alpha = 255)),
    HeatingColourLight(Color(red = 240, green = 80, blue = 248, alpha = 255)),
    HeatingColourDark(Color(red = 187, green = 0, blue = 212, alpha = 255)),
    Background(Color(red = 24, green = 0, blue = 72, alpha = 255)),
}