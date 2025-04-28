package com.example.greetingcard

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.greetingcard.ui.theme.GreetingCardTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import kotlin.sequences.sequenceOf
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import java.util.Locale
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import android.Manifest.permission.ACCESS_FINE_LOCATION

val testList = listOf(
    WifiNetwork("BadRashNet", "1.a.2.3", 0.94f),
    WifiNetwork("RaccoonNet", "1.a.2.4", 0.68f),
    WifiNetwork("wanbanthankyouman", "4.a.2.7", 0.12f)
)



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val currentNetworks = remember { mutableStateOf(emptyList<WifiNetwork>()) }

            GreetingCardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Spacer(
                            modifier = Modifier.height(50.dp)
                        )
                        Button(onClick = {
                            if (!hasRequiredPermissions()) {
                                requestPermissions()
                            } else {
                                val wifiScanner = WifiScanner(this@MainActivity)
                                wifiScanner.scanWiFiNetworks { newNetworks ->
                                    currentNetworks.value = newNetworks
                                }
                            }
                        }) { Text("Scan") }
                        WiFiList(currentNetworks.value)
                    }
                }
            }
        }
    }

    // Add permission request logic
    private fun hasRequiredPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }

}


@Preview(showBackground = true)
@Composable
fun WifiListPreview() {
    WiFiList(listOf(
        WifiNetwork("TrashChuteNet", "1.a.2.3", 0.94f),
        WifiNetwork("RaccoonNet", "1.a.2.4", 0.68f),
        WifiNetwork("wanbanthankyouman", "4.a.2.7", 0.12f)
    ))
}

@Composable
fun WiFiList(wanList: List<WifiNetwork> = listOf()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header row
        item {
            GridRow(
                cells = listOf("Network Name", "BSSID", "Signal Strength"),
                isHeader = true
            )
        }

        // Data rows
        items(wanList) { entry ->
            GridRow(
                cells = listOf(
                    entry.name,
                    entry.BSSID,
                    String.format(Locale.US, "%.2f", entry.signalStrength)
                )
            )
        }
    }
}

@Composable
fun GridRow(cells: List<String>, isHeader: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isHeader) Color.LightGray else Color.White)
    ) {
        // Calculate weights for columns
        val weights = listOf(0.1f, 0.4f, 0.3f, 0.2f)

        cells.forEachIndexed { index, cell ->
            GridCell(
                text = cell,
                weight = weights[index],
                isHeader = isHeader
            )
        }
    }
}

@Composable
fun RowScope.GridCell(text: String, weight: Float, isHeader: Boolean = false) {
    Box(
        modifier = Modifier
            .weight(weight) // Now this will work because we're in RowScope
            .height(48.dp)
            .border(0.5.dp, Color.Gray)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            textAlign = if (isHeader) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}