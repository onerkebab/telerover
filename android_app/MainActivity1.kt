package com.example.teleroverapp

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import io.socket.client.IO
import kotlinx.coroutines.delay
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TeleRoverApp()
            }
        }
    }
}

enum class Screen {
    Welcome,
    Call
}

@Composable
fun TeleRoverApp() {
    val context = LocalContext.current

    // Runtime permission request for camera and mic
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            android.widget.Toast.makeText(
                context,
                "Camera and mic permissions are required",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    // Socket.io connection to Pi
    val socket = remember {
        try {
            IO.socket("https://100.125.106.122:3000")
        } catch (e: Exception) {
            Log.e("TeleRover", "Socket init failed", e)
            null
        }
    }

    LaunchedEffect(Unit) {
        socket?.connect()
    }

    // Clean up socket on dispose
    DisposableEffect(Unit) {
        onDispose {
            socket?.emit("stop")
            socket?.disconnect()
        }
    }

    var currentScreen by remember { mutableStateOf(Screen.Welcome) }
    var callSeconds by remember { mutableStateOf(0) }
    var timerRunning by remember { mutableStateOf(false) }
    var cameraOn by remember { mutableStateOf(true) }
    var micOn by remember { mutableStateOf(true) }
    var currentMotorCommand by remember { mutableStateOf("Idle") }
    var currentTiltCommand by remember { mutableStateOf("Center") }

    val motorBattery = 78
    val screenBattery = 45

    LaunchedEffect(timerRunning) {
        while (timerRunning) {
            delay(1000)
            callSeconds++
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F1117)
    ) {
        when (currentScreen) {
            Screen.Welcome -> {
                WelcomeScreen(
                    onStartCall = {
                        currentScreen = Screen.Call
                        callSeconds = 0
                        timerRunning = true
                        currentMotorCommand = "Idle"
                        currentTiltCommand = "Center"
                    }
                )
            }

            Screen.Call -> {
                CallScreen(
                    timerText = formatTime(callSeconds),
                    cameraOn = cameraOn,
                    micOn = micOn,
                    motorCommand = currentMotorCommand,
                    tiltCommand = currentTiltCommand,
                    motorBattery = motorBattery,
                    screenBattery = screenBattery,
                    onToggleCamera = { cameraOn = !cameraOn },
                    onToggleMic = { micOn = !micOn },

                    // Motor commands — lowercase to match motor.py expectations
                    onMotorPress = { command ->
                        currentMotorCommand = command
                        // Convert display name to motor.py command
                        val motorCmd = when (command) {
                            "Forward"  -> "forward"
                            "Backward" -> "back"     // motor.py uses "back" not "backward"
                            "Left"     -> "left"
                            "Right"    -> "right"
                            else       -> "stop"
                        }
                        val data = JSONObject().apply {
                            put("source", "controller")
                            put("command", motorCmd)
                            put("speed", 0.65)
                        }
                        socket?.emit("control", data)
                    },

                    onMotorRelease = {
                        currentMotorCommand = "Idle"
                        socket?.emit("stop")
                    },

                    onTiltPress = { currentTiltCommand = it },
                    onTiltRelease = { currentTiltCommand = "Center" },

                    onEndCall = {
                        timerRunning = false
                        currentScreen = Screen.Welcome
                        socket?.emit("stop")
                        cameraOn = true
                        micOn = true
                        currentMotorCommand = "Idle"
                        currentTiltCommand = "Center"
                    }
                )
            }
        }
    }
}

fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}

@Composable
fun WelcomeScreen(onStartCall: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1117))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = Color(0x1A34C778),
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = "Wi-Fi Controlled Telepresence Robot",
                color = Color(0xFF34C778),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Control your rover,\nfrom anywhere.",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "TeleRover lets you drive, navigate, and video-call through your robot with intuitive live controls.",
            color = Color(0xFF9CA3AF),
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStartCall,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C778)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Start a Call",
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun CallScreen(
    timerText: String,
    cameraOn: Boolean,
    micOn: Boolean,
    motorCommand: String,
    tiltCommand: String,
    motorBattery: Int,
    screenBattery: Int,
    onToggleCamera: () -> Unit,
    onToggleMic: () -> Unit,
    onMotorPress: (String) -> Unit,
    onMotorRelease: () -> Unit,
    onTiltPress: (String) -> Unit,
    onTiltRelease: () -> Unit,
    onEndCall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1117))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.72f)
                .background(Color.Black)
        ) {
            VideoPanel()

            TopHud(
                timerText = timerText,
                motorCommand = motorCommand,
                tiltCommand = tiltCommand,
                onEndCall = onEndCall
            )

            PipPanel(
                cameraOn = cameraOn,
                micOn = micOn,
                onToggleCamera = onToggleCamera,
                onToggleMic = onToggleMic
            )
        }

        ControlsBar(
            motorCommand = motorCommand,
            tiltCommand = tiltCommand,
            motorBattery = motorBattery,
            screenBattery = screenBattery,
            onMotorPress = onMotorPress,
            onMotorRelease = onMotorRelease,
            onTiltPress = onTiltPress,
            onTiltRelease = onTiltRelease
        )
    }
}
@Composable
fun VideoPanel() {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { context ->
            android.webkit.WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.allowContentAccess = true
                settings.allowFileAccess = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true

                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest) {
                        request.grant(request.resources)
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onReceivedSslError(
                        view: android.webkit.WebView?,
                        handler: SslErrorHandler?,
                        error: android.net.http.SslError?
                    ) {
                        handler?.proceed()
                    }

                    override fun onPageFinished(
                        view: android.webkit.WebView?,
                        url: String?
                    ) {
                        view?.evaluateJavascript("""
                            setTimeout(function() {
                                document.getElementById('startBtn').click();
                                var remoteVideo = document.getElementById('remoteVideo');
                                if (remoteVideo) {
                                    remoteVideo.muted = false;
                                    remoteVideo.volume = 1.0;
                                    remoteVideo.play();
                                }
                            }, 800);
                        """.trimIndent(), null)
                    }
                }

                loadUrl("https://100.125.106.122:3000/?role=controller")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
@Composable
fun TopHud(
    timerText: String,
    motorCommand: String,
    tiltCommand: String,
    onEndCall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TeleRover",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = timerText,
                color = Color(0x99FFFFFF)
            )

            Button(
                onClick = onEndCall,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xCCDC2626)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("End Call")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Motor: $motorCommand   |   Tilt: $tiltCommand",
            color = Color(0xFF34C778),
            fontSize = 14.sp
        )
    }
}

@Composable
fun PipPanel(
    cameraOn: Boolean,
    micOn: Boolean,
    onToggleCamera: () -> Unit,
    onToggleMic: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .width(160.dp)
                .background(Color(0xFF1E293B), RoundedCornerShape(14.dp))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        Color(0xFF000000),
                        RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "You", color = Color(0xAAFFFFFF))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x99000000))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SmallToggleButton(
                    text = if (cameraOn) "Cam" else "Cam Off",
                    isDanger = !cameraOn,
                    onClick = onToggleCamera
                )
                SmallToggleButton(
                    text = if (micOn) "Mic" else "Mic Off",
                    isDanger = !micOn,
                    onClick = onToggleMic
                )
            }
        }
    }
}

@Composable
fun SmallToggleButton(text: String, isDanger: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDanger) Color(0xCCDC2626) else Color(0x22FFFFFF)
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ControlsBar(
    motorCommand: String,
    tiltCommand: String,
    motorBattery: Int,
    screenBattery: Int,
    onMotorPress: (String) -> Unit,
    onMotorRelease: () -> Unit,
    onTiltPress: (String) -> Unit,
    onTiltRelease: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Color(0xFF181D27))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Motor Control D-pad
        Column {
            Text("MOTOR CONTROL", color = Color(0xFF6B7280), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ControlButton("Fwd") { onMotorPress("Forward") }
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ControlButton("Left") { onMotorPress("Left") }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (motorCommand == "Idle") Color(0xFF374151) else Color(0xFF34C778),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (motorCommand == "Idle") "•" else "Go",
                            color = if (motorCommand == "Idle") Color.White else Color.Black,
                            fontSize = 12.sp
                        )
                    }

                    ControlButton("Right") { onMotorPress("Right") }
                }

                Spacer(modifier = Modifier.height(8.dp))
                ControlButton("Back") { onMotorPress("Backward") }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onMotorRelease) {
                    Text("Stop", color = Color.White)
                }
            }
        }

        VerticalDivider()

        // Camera Tilt
        Column {
            Text("CAMERA TILT", color = Color(0xFF6B7280), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlButton("Left") { onTiltPress("Left") }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (tiltCommand == "Center") Color(0xFF374151) else Color(0xFF34C778),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cam",
                        color = if (tiltCommand == "Center") Color.White else Color.Black,
                        fontSize = 10.sp
                    )
                }

                ControlButton("Right") { onTiltPress("Right") }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onTiltRelease) {
                Text("Center", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        VerticalDivider()

        // Battery indicators
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BatteryItem("Motor Battery", motorBattery)
            BatteryItem("Screen Battery", screenBattery)
        }
    }
}

@Composable
fun ControlButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0x2234C778)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(label, color = Color.White)
    }
}

@Composable
fun BatteryItem(label: String, percent: Int) {
    val color = when {
        percent > 60 -> Color(0xFF4ADE80)
        percent > 25 -> Color(0xFFFACC15)
        else -> Color(0xFFEF4444)
    }

    Column {
        Text(label.uppercase(), color = Color(0xFF9CA3AF), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .width(120.dp)
                .height(8.dp)
                .background(Color(0xFF374151), RoundedCornerShape(8.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percent / 100f)
                    .background(color, RoundedCornerShape(8.dp))
            )
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text("$percent%", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(80.dp)
            .background(Color(0x22FFFFFF))
    )
}