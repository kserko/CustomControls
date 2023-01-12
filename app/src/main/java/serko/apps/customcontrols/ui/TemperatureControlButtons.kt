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
    val temperature = temperatureDataObserver.value
    when (temperatureDirection) {
        TemperatureDirection.DOWN -> newTemperature.value = newTemperature.value - temperature.increment
        TemperatureDirection.UP -> newTemperature.value = newTemperature.value + temperature.increment
    }
    if (newTemperature.value <= temperature.maxTemperature || newTemperature.value >= temperature.minTemperature) {
        temperatureDataObserver.value = temperature.copy(targetTemperature = newTemperature.value)
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

@Composable
fun HapticFeedback(newTemperature: Float) {
    LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)
    print(newTemperature)
}
