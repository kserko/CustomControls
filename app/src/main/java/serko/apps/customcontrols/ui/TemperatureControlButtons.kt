package serko.apps.customcontrols.ui

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import serko.apps.customcontrols.R
import serko.apps.customcontrols.TemperatureData

enum class TemperatureDirection {
    UP,
    DOWN,
}

@Composable
fun TemperatureControlButtons(
    modifier: Modifier,
    temperatureDataState: MutableState<TemperatureData>,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ControlButton(Modifier, temperatureDataState, TemperatureDirection.DOWN)
        ControlButton(Modifier, temperatureDataState, TemperatureDirection.UP)
        HapticFeedback(temperatureDataState.value.targetTemperature)
    }
}

@Composable
private fun ControlButton(
    modifier: Modifier,
    temperatureDataState: MutableState<TemperatureData>,
    temperatureDirection: TemperatureDirection,
) {
    val interactionSource = remember { MutableInteractionSource() }

    IconButton(
        modifier = modifier
            .size(100.dp, 100.dp)
            .padding(12.dp)
            .repeatedPressInterceptor(
                interactionSource,
                block = {
                    //The block of code to run whe user interacts with the button
                    changeTemperature(
                        temperatureDirection = temperatureDirection,
                        temperatureDataState = temperatureDataState
                    )
                }),
        onClick = {} //Clicks are handled by the repeatedPressInterceptor modifier above
    )
    {
        val iconResource = if (temperatureDirection == TemperatureDirection.UP) R.drawable.ic_plus else R.drawable.ic_minus
        Icon(
            painterResource(iconResource),
            contentDescription = null
        )
    }
}

/**
 * Depending on the temperature direction (Up or Down) check new temperature boundaries
 * and update the state accordingly
 */
private fun changeTemperature(
    temperatureDirection: TemperatureDirection,
    temperatureDataState: MutableState<TemperatureData>
) {
    val temperatureData = temperatureDataState.value

    val newTemperature = when (temperatureDirection) {
        TemperatureDirection.DOWN -> temperatureData.targetTemperature - temperatureData.increment
        TemperatureDirection.UP -> temperatureData.targetTemperature + temperatureData.increment
    }

    //if going over the max, return the max
    if (newTemperature.isAboveMax(temperatureData)) {
        temperatureDataState.value = temperatureData.copy(targetTemperature = temperatureData.maxTemperature)
    }

    //if going below the min, return the min
    if (newTemperature.isBelowMin(temperatureData)) {
        temperatureDataState.value = temperatureData.copy(targetTemperature = temperatureData.minTemperature)
    }

    //if between min and max, return new value
    if (newTemperature.isBetweenMinAndMax(temperatureData)) {
        temperatureDataState.value = temperatureData.copy(targetTemperature = newTemperature)
    }
}

/**
 * Intercept long press gestures or single taps on and execute block function
 */
private fun Modifier.repeatedPressInterceptor(interactionSource: InteractionSource, block: () -> Unit): Modifier =
    pointerInput(interactionSource) {
        forEachGesture {
            coroutineScope {
                awaitPointerEventScope {
                    val pointerAction = awaitFirstDown(requireUnconsumed = false)
                    val buttonActionJob = launch {
                        while (pointerAction.pressed) {
                            block()
                            //Need this to process consecutive gesture inputs. If removed completely the app will freeze, crash and die
                            delay(InteractionDelay)
                        }
                    }
                    waitForUpOrCancellation()
                    buttonActionJob.cancel()
                }
            }
        }
    }

private fun Float.isAboveMax(temperatureData: TemperatureData) = this > temperatureData.maxTemperature
private fun Float.isBelowMin(temperatureData: TemperatureData) = this < temperatureData.minTemperature
private fun Float.isBetweenMinAndMax(temperatureData: TemperatureData) = !isBelowMin(temperatureData) && !isAboveMax(temperatureData)

@Composable
fun HapticFeedback(targetTemperature: Float) {
    LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)
    println(targetTemperature) //removing this stops the feedback from working (?)
}
