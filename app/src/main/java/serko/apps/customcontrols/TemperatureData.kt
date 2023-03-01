package serko.apps.customcontrols

data class TemperatureData(
    val minTemperature: Float = 10f,
    val maxTemperature: Float = 30f,
    val targetTemperature: Float = 20f, //should be between min and max
    val currentTemperature: Float = 19f,
    val increment: Float = 0.5f
)