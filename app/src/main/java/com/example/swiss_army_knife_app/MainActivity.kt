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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.swiss_army_knife_app.ui.theme.Swiss_Army_Knife_AppTheme

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
fun Tile(text: String, modifier: Modifier = Modifier, elevation: Dp = 8.dp, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier
            .padding(8.dp)
            .fillMaxSize()
            .let {
                if (onClick != null) it.clickable { onClick() } else it
            },
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(elevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(16.dp),
                maxLines = 2
            )
        }
    }
}

@Composable
fun LightSensorTile(modifier: Modifier = Modifier, context: Context) {
    var lux by remember { mutableStateOf(0f) }

    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
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
        sensorManager.registerListener(sensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sensorManager.unregisterListener(sensorListener) }
    }

    Card(
        modifier = modifier.padding(8.dp).fillMaxSize(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Światło: ${lux} lx",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(16.dp)
            )
        }
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
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
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
fun StepCounterTile(modifier: Modifier = Modifier, context: Context) {
    var steps by remember { mutableStateOf(0) }
    var initialValue by remember { mutableStateOf<Float?>(null) }

    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val stepSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }

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
            sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    Card(
        modifier = modifier.padding(8.dp).fillMaxSize(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Kroki: $steps",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
@Composable
fun MainScreen(modifier: Modifier = Modifier, context: Context) {
    var currentScreen by remember { mutableStateOf("main") }

    when (currentScreen) {
        "main" -> {
            Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
                    Tile(
                        text = "Dane",
                        modifier = Modifier.weight(1f).height(150.dp),
                        onClick = { currentScreen = "data" }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Tile(
                        text = "Klikalne",
                        modifier = Modifier.weight(1f).height(150.dp),
                        onClick = { currentScreen = "clickables" }
                    )
                }
            }
        }
        "data" -> DataScreen(modifier = modifier, onBack = { currentScreen = "main" }, context = context)
        "clickables" -> ClickableScreen(modifier = modifier, onBack = { currentScreen = "main" }, context = context)
    }
}

@Composable
fun DataScreen(modifier: Modifier, onBack: () -> Unit, context: Context) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Dane", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))
        Row(modifier = Modifier.weight(1f)) {
            LightSensorTile(modifier = Modifier.weight(1f), context = context)
            Spacer(modifier = Modifier.width(16.dp))
            StepCounterTile(modifier = Modifier.weight(1f), context = context)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Tile(text = "Pogoda", modifier = Modifier.fillMaxWidth().height(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        BackButton(onClick = onBack)    }
}

@Composable
fun ClickableScreen(modifier: Modifier, onBack: () -> Unit, context: Context) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Klikalne", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))
        FlashlightTile(modifier = Modifier.fillMaxWidth().height(150.dp), context = context)
        Spacer(modifier = Modifier.height(24.dp))
        BackButton(onClick = onBack)    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    Swiss_Army_Knife_AppTheme {
        MainScreen(context = androidx.compose.ui.platform.LocalContext.current)
    }
}
@Composable
fun BackButton(text: String = "Powrót", onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 8.dp),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}