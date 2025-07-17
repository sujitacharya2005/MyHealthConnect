package com.sssk.myhealthconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sssk.myhealthconnect.ui.theme.MyHealthConnectTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var permissionLauncher: ActivityResultLauncher<Set<String>>
    private var onPermissionResult: ((Boolean) -> Unit)? = null
    private var healthConnectAvailable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val status = HealthConnectClient.getSdkStatus(this)
        healthConnectAvailable = status == HealthConnectClient.SDK_AVAILABLE
        if (!healthConnectAvailable) {
            setContent {
                MyHealthConnectTheme {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Health Connect is not available or not installed on this device.")
                    }
                }
            }
            return
        }
        healthConnectManager = HealthConnectManager(this)
        val stepRepository = StepRepository(this)
        val stepViewModel: StepViewModel by viewModels {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(StepViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return StepViewModel(stepRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
        // Register the launcher BEFORE setContent
        permissionLauncher = registerForActivityResult(
            healthConnectManager.requestPermissionsActivityContract()
        ) { granted ->
            val grantedAll = granted.containsAll(stepViewModel.stepPermissions)
            onPermissionResult?.invoke(grantedAll)
        }
        // Initial permission check
        stepViewModel.checkPermissions(healthConnectManager)
        enableEdgeToEdge()
        setContent {
            MyHealthConnectTheme {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                val message = stepViewModel.message.value
                val loading = stepViewModel.loading.value
                val permissionsGranted = stepViewModel.permissionsGranted.value
                val lastStepCount = stepViewModel.lastStepCount.value
                var launchPermission by remember { androidx.compose.runtime.mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                stepViewModel.checkPermissions(healthConnectManager)
                                if (!stepViewModel.permissionsGranted.value) {
                                    onPermissionResult = { granted ->
                                        if (granted) {
                                            stepViewModel.fetchAndPostSteps(healthConnectManager)
                                        } else {
                                            stepViewModel.message.value = "Permission denied."
                                        }
                                    }
                                    launchPermission = true
                                } else {
                                    stepViewModel.fetchAndPostSteps(healthConnectManager)
                                }
                            }
                        },
                        enabled = healthConnectAvailable && !loading
                    ) {
                        Text(if (loading) "Working..." else "Send Today's Steps to Server")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(message)
                    if (lastStepCount != null && message.startsWith("Success")) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Today's step count: $lastStepCount")
                    }
                }
                if (healthConnectAvailable) {
                    LaunchedEffect(launchPermission) {
                        if (launchPermission) {
                            permissionLauncher.launch(stepViewModel.stepPermissions)
                            launchPermission = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyHealthConnectTheme {
        Greeting("Android")
    }
}