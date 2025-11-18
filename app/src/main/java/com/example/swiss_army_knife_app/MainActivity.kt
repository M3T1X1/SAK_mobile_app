package com.example.swiss_army_knife_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
                    FourTilesScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun FourTilesScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
            Tile(text = "Kafelek 1", modifier = Modifier.weight(1f))
            Tile(text = "Kafelek 2", modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.weight(1f)) {
            Tile(text = "Kafelek 3", modifier = Modifier.weight(1f))
            Tile(text = "Kafelek 4", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun Tile(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FourTilesScreenPreview() {
    Swiss_Army_Knife_AppTheme {
        FourTilesScreen()
    }
}
