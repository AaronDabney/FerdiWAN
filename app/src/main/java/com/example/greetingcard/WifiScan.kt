package com.example.greetingcard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import android.net.wifi.ScanResult
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.net.wifi.rtt.WifiRttManager
import android.net.wifi.rtt.RangingRequest
import android.net.wifi.rtt.RangingResult
import android.net.wifi.rtt.ResponderLocation

data class WifiNetwork(val name: String, val BSSID: String, val signalStrength: Float, val freq: Int = 0, val timeStamp: Long = 0)

class WifiScanner(private val context: Context) {
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val TAG = "WifiScanner"

    private lateinit var wifiRttManager: WifiRttManager

    fun startRanging() {
        val builder = RangingRequest.Builder()
        builder.setRttBurstSize(2)
    }

    //WifiRttManager. startRanging(RangingRequest, java. util. concurrent. Executor, RangingResultCallback)

    fun scanWiFiNetworks(callback: (List<WifiNetwork>) -> Unit) {
        if (!permissionsValid()) {
            Log.w(TAG, "Permissions not valid, cannot scan")
            callback(emptyList())
            return
        }

        val wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    val success = intent.getBooleanExtra(
                        WifiManager.EXTRA_RESULTS_UPDATED, false)

                    if (success) {
                        Log.d(TAG, "Scan successful!")
                        scanSuccess()
                        val results = getScanResults()
                        callback(results)
                    } else {
                        Log.w(TAG, "Scan failed!")
                        scanFailure()
                        // Use old results anyway
                        val results = getScanResults()
                        callback(results)
                    }

                    try {
                        context.unregisterReceiver(this)
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "Error unregistering receiver: ${e.message}")
                    }
                }
            }
        }

        // Register for scan results
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(wifiScanReceiver, intentFilter)

        // Start the scan - with throttling disabled, this should work every time
        val scanStarted = wifiManager.startScan()

        if (!scanStarted) {
            Log.w(TAG, "startScan() returned false - scan did not start")
            // Get results anyway
            mainHandler.postDelayed({
                val results = getScanResults()
                callback(results)
                try {
                    context.unregisterReceiver(wifiScanReceiver)
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Error unregistering receiver: ${e.message}")
                }
            }, 500)
        } else {
            Log.d(TAG, "startScan() returned true - scan started successfully")
        }

        // Safety timeout in case we don't get a callback
        mainHandler.postDelayed({
            try {
                context.unregisterReceiver(wifiScanReceiver)
                Log.w(TAG, "Scan timeout - unregistered receiver")
            } catch (e: IllegalArgumentException) {
                // Receiver already unregistered, which means we got scan results
                Log.d(TAG, "Receiver already unregistered, scan completed normally")
            }
        }, 10000) // 10-second timeout
    }

    private fun getScanResults(): List<WifiNetwork> {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        val scanResults = wifiManager.scanResults
        Log.d(TAG, "Retrieved ${scanResults.size} scan results")

        return scanResults.map { scanResult ->
            WifiNetwork(
                name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    scanResult.wifiSsid.toString()
                } else {
                    @Suppress("DEPRECATION")
                    scanResult.SSID
                },
                BSSID = scanResult.BSSID,
                signalStrength = scanResult.level.toFloat(),
                freq = scanResult.frequency,
                timeStamp = scanResult.timestamp
            )
        }
    }

    fun permissionsValid(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun scanSuccess() {
        Log.d(TAG, "Scan success!")
    }

    private fun scanFailure() {
        Log.w(TAG, "Scan failure!")
    }
}