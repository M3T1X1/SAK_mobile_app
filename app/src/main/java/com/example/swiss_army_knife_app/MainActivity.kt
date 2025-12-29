package com.example.swiss_army_knife_app

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.swiss_army_knife_app.ui.theme.Swiss_Army_Knife_AppTheme
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

val httpClient = OkHttpClient()

data class WeatherData(
    val temperature: Float = 0f,
    val humidity: Float = 0f,
    val precipitation: Float = 0f,
    val windSpeed: Float = 0f,
    val windDirection: Float = 0f,
    val weatherCode: Int = 0
)

suspend fun fetchWeatherData(latitude: Float, longitude: Float): WeatherData {
    val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude&longitude=$longitude" +
            "&current=temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m" +
            "&timezone=Europe/Warsaw"

    return withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("WeatherAPI", "HTTP ${response.code}: ${response.message}")
                    return@withContext WeatherData()
                }

                val body = response.body?.string() ?: return@withContext WeatherData()
                Log.d("WeatherAPI", "Raw response: $body")

                val json = JSONObject(body)
                val current = json.optJSONObject("current") ?: run {
                    Log.e("WeatherAPI", "No 'current' object in response")
                    return@withContext WeatherData()
                }

                WeatherData(
                    temperature = current.optDouble("temperature_2m", 0.0).toFloat(),
                    humidity = current.optDouble("relative_humidity_2m", 0.0).toFloat(),
                    precipitation = current.optDouble("precipitation", 0.0).toFloat(),
                    windSpeed = current.optDouble("wind_speed_10m", 0.0).toFloat(),
                    windDirection = current.optDouble("wind_direction_10m", 0.0).toFloat(),
                    weatherCode = current.optInt("weather_code", 0)
                ).also { data ->
                    Log.d("WeatherAPI", "Parsed: temp=${data.temperature}, hum=${data.humidity}")
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherAPI", "Fetch error: ${e.message}", e)
            WeatherData(temperature = -999f)
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Swiss_Army_Knife_AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        context = this@MainActivity
                    )
                }
            }
        }
    }

    fun setScreenBrightness(level: Float) {
        val lp = window.attributes
        lp.screenBrightness = level.coerceIn(0f, 1f)
        window.attributes = lp
    }
}

@Composable
fun AmbientLightController(
    context: Context,
    enabled: Boolean,
    onLuxChange: (Float) -> Unit
) {
    var lux by remember { mutableStateOf(0f) }

    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val lightSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) }

    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                lux = event.values[0]
                val normalizedBrightness = (lux / 1000f).coerceIn(0f, 1f)
                onLuxChange(normalizedBrightness)
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
    }

    LaunchedEffect(enabled, lightSensor) {
        if (enabled && lightSensor != null) {
            sensorManager.registerListener(
                sensorListener,
                lightSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        } else if (lightSensor != null) {
            sensorManager.unregisterListener(sensorListener, lightSensor)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            sensorManager.unregisterListener(sensorListener, lightSensor)
        }
    }
}

@Composable
fun Tile(
    text: String,
    modifier: Modifier = Modifier,
    elevation: Dp = 8.dp,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .padding(8.dp)
            .let { if (onClick != null) it.clickable { onClick() } else it },
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(elevation),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2
            )
        }
    }
}

@Composable
fun DataTileSurface(
    text: String,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.padding(8.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (content != null) {
                content()
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun LightSensorTile(
    modifier: Modifier = Modifier,
    currentLux: Float
) {
    DataTileSurface(modifier = modifier, text = "") {
        Text(
            text = "Światło:\n${currentLux.toInt()} lx",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun StepCounterTile(modifier: Modifier = Modifier, context: Context) {
    var steps by remember { mutableStateOf(0) }
    var initialValue by remember { mutableStateOf<Float?>(null) }

    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val stepSensor = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }

    val listener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val total = event.values[0]
                if (initialValue == null) {
                    initialValue = total
                }
                steps = (total - (initialValue ?: total)).toInt()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    DisposableEffect(Unit) {
        if (stepSensor != null) {
            sensorManager.registerListener(
                listener,
                stepSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    DataTileSurface(modifier = modifier, text = "") {
        Text(
            text = "Kroki:\n$steps",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun FlashlightTile(modifier: Modifier = Modifier, context: Context) {
    val cameraManager = remember {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    val cameraId = remember {
        try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val hasFlash = cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)
                    ?: false
                hasFlash
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    var isFlashOn by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun toggleFlashlight() {
        if (!hasCameraPermission) {
            Toast.makeText(context, "Brak uprawnień do kamery", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            isFlashOn = !isFlashOn
            cameraManager.setTorchMode(cameraId, isFlashOn)
        } catch (e: CameraAccessException) {
            Toast.makeText(context, "Błąd włączenia latarki", Toast.LENGTH_SHORT).show()
        }
    }

    Tile(
        text = if (isFlashOn) "Latarka ON" else "Latarka OFF",
        modifier = modifier,
        onClick = { toggleFlashlight() }
    )
}

fun vibratePhone(context: Context, millis: Long = 4000L) {
    val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (vibrator?.hasVibrator() == true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(
                millis,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(millis)
        }
    }
}

@Composable
fun VibrationTile(
    modifier: Modifier = Modifier,
    context: Context
) {
    Tile(
        text = "Usuwanie Wody",
        modifier = modifier,
        onClick = { vibratePhone(context) }
    )
}

@Composable
fun SpiritLevelTile(
    modifier: Modifier = Modifier,
    context: Context
) {
    var angleX by remember { mutableStateOf(0f) }
    var angleY by remember { mutableStateOf(0f) }

    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val accelSensor = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    val listener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]

                angleX = Math.toDegrees(kotlin.math.atan2(ax.toDouble(), az.toDouble())).toFloat()
                angleY = Math.toDegrees(kotlin.math.atan2(ay.toDouble(), az.toDouble())).toFloat()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    DisposableEffect(Unit) {
        if (accelSensor != null) {
            sensorManager.registerListener(
                listener,
                accelSensor,
                1000000000
            )
        }
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    DataTileSurface(modifier = modifier, text = "") {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Poziomica",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "X: ${angleX.toInt()}°\nY: ${angleY.toInt()}°",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun BackButton(text: String = "Powrót", onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun CityTile(
    modifier: Modifier = Modifier,
    latitude: Float,
    longitude: Float,
    onCoordsChange: (Float, Float) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var latText by remember { mutableStateOf(latitude.toString()) }
    var lonText by remember { mutableStateOf(longitude.toString()) }

    DataTileSurface(modifier = modifier, text = "") {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Współrzędne",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(4.dp))

            Text(
                text = "Lat: ${String.format("%.4f", latitude)}\nLon: ${String.format("%.4f", longitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { showDialog = true }) {
                Text("Edytuj")
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Edytuj współrzędne") },
            text = {
                Column {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Szerokość (lat) -90..90") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = { lonText = it },
                        label = { Text("Długość (lon) -180..180") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newLat = latText.toFloatOrNull()
                        val newLon = lonText.toFloatOrNull()

                        if (newLat != null && newLon != null &&
                            newLat in -90f..90f && newLon in -180f..180f) {
                            onCoordsChange(newLat, newLon)
                            showDialog = false
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
fun WeatherTile(
    modifier: Modifier = Modifier,
    weather: WeatherData,
    isLoading: Boolean = false
) {
    DataTileSurface(modifier = modifier, text = "") {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Pogoda",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(4.dp))

            if (isLoading) {
                Text(
                    text = "Ładowanie...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else if (weather.temperature < -100f) {
                Text(
                    text = "Błąd API",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "${weather.temperature}°C",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Wilg: ${weather.humidity.toInt()}%\nWiatr: ${weather.windSpeed.toInt()} km/h\nOpady: ${weather.precipitation.toInt()} mm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    context: Context
) {
    var selectedLatitude by remember { mutableStateOf(50.0413f) }
    var selectedLongitude by remember { mutableStateOf(21.999f) }
    var weatherData by remember { mutableStateOf(WeatherData()) }
    var isLoadingWeather by remember { mutableStateOf(false) }
    var currentLux by remember { mutableStateOf(0f) }
    var brightnessLevel by remember { mutableStateOf(0.5f) }
    var autoBrightnessEnabled by remember { mutableStateOf(true) }

    AmbientLightController(
        context = context,
        enabled = autoBrightnessEnabled,
        onLuxChange = { level ->
            brightnessLevel = level
            currentLux = level * 1000f
        }
    )

    val activity = context as MainActivity
    LaunchedEffect(brightnessLevel, autoBrightnessEnabled) {
        if (autoBrightnessEnabled) {
            activity.setScreenBrightness(brightnessLevel)
        } else {
            activity.setScreenBrightness(0.5f)
        }
    }

    LaunchedEffect(selectedLatitude, selectedLongitude) {
        while (true) {
            isLoadingWeather = true
            try {
                weatherData = fetchWeatherData(selectedLatitude, selectedLongitude)
                Log.d("Weather", "Updated: ${weatherData.temperature}°C")
            } catch (e: Exception) {
                Log.e("Weather", "Fetch failed: ${e.message}")
                weatherData = WeatherData(temperature = -999f)
            } finally {
                isLoadingWeather = false
            }
            delay(30000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.fav),
                    contentDescription = "Logo",
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Swiss Army Knife",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = if (autoBrightnessEnabled) "Auto" else "50%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = autoBrightnessEnabled,
                    onCheckedChange = { autoBrightnessEnabled = it }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LightSensorTile(modifier = Modifier.weight(1f), currentLux = currentLux)
            StepCounterTile(modifier = Modifier.weight(1f), context = context)
            SpiritLevelTile(modifier = Modifier.weight(1f), context = context)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeatherTile(
                modifier = Modifier.weight(1f),
                weather = weatherData,
                isLoading = isLoadingWeather
            )
            CityTile(
                modifier = Modifier.weight(1f),
                latitude = selectedLatitude,
                longitude = selectedLongitude,
                onCoordsChange = { lat, lon ->
                    selectedLatitude = lat
                    selectedLongitude = lon
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlashlightTile(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                context = context
            )
            VibrationTile(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                context = context
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    Swiss_Army_Knife_AppTheme {
        MainScreen(
            context = LocalContext.current
        )
    }
}
