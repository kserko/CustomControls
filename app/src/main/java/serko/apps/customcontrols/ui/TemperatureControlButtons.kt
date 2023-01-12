package serko.apps.customcontrols.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
    temperatureDataObserver: MutableState<TemperatureData>,
) {
    val temperature = temperatureDataObserver.value
    val newTemperature = remember { mutableStateOf(temperature.targetTemperature) }
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        //Temperature Down button
        Image(
            modifier = Modifier
                .padding(12.dp)
                .repeatedPressInterceptor(
                    interactionSource,
                    block = {
                        if (newTemperature.value > temperature.minTemperature) {
                            changeTemperature(
                                temperatureDirection = TemperatureDirection.DOWN,
                                newTemperature = newTemperature,
                                temperatureDataObserver = temperatureDataObserver
                            )
                        }
                    }),
            painter = painterResource(id = R.drawable.ic_minus),
            colorFilter = ColorFilter.tint(Color.White),
            contentDescription = null,
        )

        //Temperature Up button
        Image(
            modifier = Modifier
                .padding(12.dp)
                .repeatedPressInterceptor(
                    interactionSource,
                    block = {
                        if (newTemperature.value < temperature.maxTemperature) {
                            changeTemperature(
                                temperatureDirection = TemperatureDirection.UP,
                                newTemperature = newTemperature,
                                temperatureDataObserver = temperatureDataObserver
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

private fun changeTemperature(
    temperatureDirection: TemperatureDirection,
    newTemperature: MutableState<Float>,
    temperatureDataObserver: MutableState<TemperatureData>
) {
    val temperatureData = temperatureDataObserver.value

    when (temperatureDirection) {
        TemperatureDirection.DOWN -> newTemperature.value = newTemperature.value - temperatureData.increment
        TemperatureDirection.UP -> newTemperature.value = newTemperature.value + temperatureData.increment
    }

    //if going over the max, return the max
    if (newTemperature.value.isAboveMax(temperatureData)) {
        temperatureDataObserver.value = temperatureData.copy(targetTemperature = temperatureData.maxTemperature)
    }

    //if going below the min, return the min
    if (newTemperature.value.isBelowMin(temperatureData)) {
        temperatureDataObserver.value = temperatureData.copy(targetTemperature = temperatureData.minTemperature)
    }

    //if between min and max, return new value
    if (newTemperature.value.isBetweenMinAndMax(temperatureData)) {
        temperatureDataObserver.value = temperatureData.copy(targetTemperature = newTemperature.value)
    }
}

private fun Modifier.repeatedPressInterceptor(interactionSource: InteractionSource, block: () -> Unit): Modifier =
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

private fun Float.isAboveMax(temperatureData: TemperatureData) = this > temperatureData.maxTemperature
private fun Float.isBelowMin(temperatureData: TemperatureData) = this < temperatureData.minTemperature
private fun Float.isBetweenMinAndMax(temperatureData: TemperatureData) = !isBelowMin(temperatureData) && !isAboveMax(temperatureData)


@Composable
fun HapticFeedback(newTemperature: Float) {
    LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)
    println(newTemperature) //removing this stops the feedback from working (?)
}
