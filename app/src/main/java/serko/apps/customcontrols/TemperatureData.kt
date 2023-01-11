package serko.apps.customcontrols

data class TemperatureData(
    val targetTemperature: Float = 22f,
    val minTemperature: Float = 10f,
    val maxTemperature: Float = 30f,
    val increments: Float = 0.5f
)