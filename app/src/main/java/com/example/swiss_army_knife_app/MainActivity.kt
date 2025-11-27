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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.swiss_army_knife_app.ui.theme.Swiss_Army_Knife_AppTheme
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Swiss_Army_Knife_AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding), context = this)
                }
            }
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
fun LightSensorTile(modifier: Modifier = Modifier, context: Context) {
    var lux by remember { mutableStateOf(0f) }

    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val lightSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) }

    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                lux = event.values[0]
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
    }

    DisposableEffect(Unit) {
        sensorManager.registerListener(
            sensorListener,
            lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        onDispose { sensorManager.unregisterListener(sensorListener) }
    }

    DataTileSurface(modifier = modifier, text = "") {
        Text(
            text = "Światło:\n${lux.toInt()} lx",
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

@Composable
fun MainScreen(modifier: Modifier = Modifier, context: Context) {
    var currentScreen by remember { mutableStateOf("main") }

    when (currentScreen) {
        "main" -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // OBRAZEK PNG NAD KAFELKAMI
                Image(
                    painter = painterResource(id = R.drawable.fav),
                    contentDescription = "Logo aplikacji",
                    modifier = Modifier
                        .size(96.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Tile(
                        text = "Dane",
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp),
                        onClick = { currentScreen = "data" }
                    )
                    Tile(
                        text = "Klikalne",
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp),
                        onClick = { currentScreen = "clickables" }
                    )
                }
            }
        }
        "data" -> DataScreen(
            modifier = modifier,
            onBack = { currentScreen = "main" },
            context = context
        )

        "clickables" -> ClickableScreen(
            modifier = modifier,
            onBack = { currentScreen = "main" },
            context = context
        )
    }
}

@Composable
fun DataScreen(modifier: Modifier, onBack: () -> Unit, context: Context) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Dane",
            style = MaterialTheme.typography.headlineMedium
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LightSensorTile(
                modifier = Modifier.weight(1f),
                context = context
            )
            StepCounterTile(
                modifier = Modifier.weight(1f),
                context = context
            )
        }

        DataTileSurface(
            text = "Pogoda",
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        BackButton(onClick = onBack)
    }
}

@Composable
fun ClickableScreen(modifier: Modifier, onBack: () -> Unit, context: Context) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Klikalne",
            style = MaterialTheme.typography.headlineMedium
        )

        FlashlightTile(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            context = context
        )

        Spacer(modifier = Modifier.weight(1f))

        BackButton(onClick = onBack)
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

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    Swiss_Army_Knife_AppTheme {
        MainScreen(context = androidx.compose.ui.platform.LocalContext.current)
    }
}
