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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

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

// Build a trust-all SSL socket that works with Pi's self-signed cert
fun buildTrustAllSocket(url: String): Socket? {
    return try {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        val okHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
        val options = IO.Options().apply {
            callFactory = okHttpClient
            webSocketFactory = okHttpClient
            secure = true
        }
        IO.socket(url, options)
    } catch (e: Exception) {
        Log.e("TeleRover", "Socket setup failed", e)
        null
    }
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

    // Socket.io with trust-all SSL for Pi's self-signed cert
    val socket = remember { buildTrustAllSocket("https://100.125.106.122:3000") }
    var connectionStatus by remember { mutableStateOf("Disconnected") }

    LaunchedEffect(Unit) {
        socket?.on(Socket.EVENT_CONNECT) {
            connectionStatus = "Connected"
            Log.d("TeleRover", "Socket connected")
        }
        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            connectionStatus = "Error: ${args[0]}"
            Log.e("TeleRover", "Socket error: ${args[0]}")
        }
        socket?.connect()
    }

    DisposableEffect(Unit) {
        onDispose {
            socket?.emit("control", JSONObject().apply {
                put("source", "controller")
                put("command", "stop")
                put("speed", 0)
            })
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
    var motorSpeed by remember { mutableStateOf(0.65f) }

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
                    connectionStatus = connectionStatus,
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
                    motorSpeed = motorSpeed,
                    socket = socket,
                    onToggleCamera = { cameraOn = !cameraOn },
                    onToggleMic = { micOn = !micOn },
                    onMotorCommandChange = { currentMotorCommand = it },
                    onTiltPress = { direction ->
                        currentTiltCommand = direction
                        socket?.emit("tilt", JSONObject().apply {
                            put("command", direction.lowercase())
                        })
                    },
                    onTiltRelease = { currentTiltCommand = "Center" },
                    onSpeedChange = { motorSpeed = it },
                    onEndCall = {
                        timerRunning = false
                        currentScreen = Screen.Welcome
                        socket?.emit("control", JSONObject().apply {
                            put("source", "controller")
                            put("command", "stop")
                            put("speed", 0)
                        })
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
fun WelcomeScreen(
    connectionStatus: String,
    onStartCall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1117))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "I am John, a telerover!",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "I can drive and video call :)",
            color = Color(0xFF6B7280),
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Socket connection status indicator
        Surface(
            color = if (connectionStatus == "Connected") Color(0x1A2D8CFF) else Color(0x1ADC2626),
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = "My Status: $connectionStatus",
                color = if (connectionStatus == "Connected") Color(0xFF2D8CFF) else Color(0xFFDC2626),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onStartCall,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D8CFF)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Start",
                color = Color(0xFF1F2937),
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
    motorSpeed: Float,
    socket: Socket?,
    onToggleCamera: () -> Unit,
    onToggleMic: () -> Unit,
    onMotorCommandChange: (String) -> Unit,
    onTiltPress: (String) -> Unit,
    onTiltRelease: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onEndCall: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        VideoPanel()

        TopHud(
            timerText = timerText,
            motorCommand = motorCommand,
            tiltCommand = tiltCommand,
            onEndCall = onEndCall
        )

        Box (
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            ControlsBar(
                motorCommand = motorCommand,
                tiltCommand = tiltCommand,
                motorSpeed = motorSpeed,
                socket = socket,
                onMotorCommandChange = onMotorCommandChange,
                onTiltPress = onTiltPress,
                onTiltRelease = onTiltRelease,
                onSpeedChange = onSpeedChange
            )
        }
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

                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                        view?.evaluateJavascript("""
                            setTimeout(function() {
                                document.getElementById('startBtn').click();
                                var remoteVideo = document.getElementById('remoteVideo');
                                if (remoteVideo) {
                                    remoteVideo.muted = false;
                                    remoteVideo.volume = 1.0;
                                    remoteVideo.play();
                                }
                            
                                //Hide webpage elements
                                var speedWrap = document.getElementById('speedWrap');
                                if (speedWrap) speedWrap.style.display = 'none';
                                var hud = document.getElementById('hud');
                                if (hud) hud.style.display = 'none';
                                var startBtn = document.getElementById('startBtn');
                                if (startBtn) startBtn.style.display = 'none';
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
            Text(text = "TeleRover", color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = timerText, color = Color(0x99FFFFFF))
            Button(
                onClick = onEndCall,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("End Call")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Motor: $motorCommand   |   Tilt: $tiltCommand",
            color = Color(0xFF2D8CFF),
            fontSize = 14.sp
        )
    }
}
@Composable
fun ControlsBar(
    motorCommand: String,
    tiltCommand: String,
    motorSpeed: Float,
    socket: Socket?,
    onMotorCommandChange: (String) -> Unit,
    onTiltPress: (String) -> Unit,
    onTiltRelease: () -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var heartbeatJob by remember { mutableStateOf<Job?>(null) }
    //for tilt Camer
    var tiltJob by remember { mutableStateOf<Job?>(null) }

    fun startMoving(cmd: String) {
        onMotorCommandChange(cmd.replaceFirstChar { it.uppercase() })
        heartbeatJob?.cancel()
        heartbeatJob = coroutineScope.launch {
            while (true) {
                socket?.emit("control", JSONObject().apply {
                    put("source", "controller")
                    put("command", cmd)
                    put("speed", motorSpeed.toDouble())
                })
                delay(200)
            }
        }
    }

    fun sendStop() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        onMotorCommandChange("Idle")
        socket?.emit("control", JSONObject().apply {
            put("source", "controller")
            put("command", "stop")
            put("speed", 0)
        })
    }
    ///////////////////////
    // New for Camera Tilt
    fun sendTilt(cmd: String) {
        onTiltPress(cmd)
        tiltJob?.cancel()
        tiltJob = coroutineScope.launch {
            while(true){
                socket?.emit("tilt", JSONObject().apply {
                    put("command", cmd.lowercase())
                })
                delay(100)
            }
        }
    }
    fun sendTiltStop() {
        tiltJob?.cancel()
        tiltJob = null
        onTiltRelease()
    }
    /////////////////////////
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Speed slider
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("SPEED", color = Color(0xFF6B7280), fontSize = 11.sp)
            Text(
                text = "${"%.0f".format(motorSpeed * 100)}%",
                color = Color(0xFF2D8CFF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = motorSpeed,
                onValueChange = onSpeedChange,
                valueRange = 0.2f..1.0f,
                modifier = Modifier.width(120.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2D8CFF),
                    activeTrackColor = Color(0xFF2D8CFF),
                    inactiveTrackColor = Color(0xFF374151)
                )
            )
        }

        VerticalDivider()

        // Motor D-pad
        Column (horizontalAlignment = Alignment.CenterHorizontally){
            Text("MOTOR CONTROL", color = Color(0xFF6B7280), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(6.dp))


            HoldButton("FWD",
                onDown = { startMoving("back") },
                onUp = { sendStop() }
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HoldButton("LEFT",
                    onDown = { startMoving("left") },
                    onUp = { sendStop() }
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (motorCommand == "Idle") Color(0xFF374151) else Color(0xFF2D8CFF),
                            CircleShape
                            ),
                        contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (motorCommand == "Idle") "•" else "GO",
                        color = if (motorCommand == "Idle") Color.White else Color(0xFF1F2937),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                        )
                }

                HoldButton("RIGHT",
                    onDown = { startMoving("right") },
                    onUp = { sendStop() }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HoldButton("BWD",
                onDown = { startMoving("forward") },
                onUp = { sendStop() }
            )

        }

        VerticalDivider()

        // Camera Tilt
        Column(horizontalAlignment = Alignment.CenterHorizontally){
            Text("CAMERA CONTROL", color = Color(0xFF6B7280), fontSize = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))

            HoldButton("U",
                onDown = { sendTilt("Up")},
                onUp = {sendTiltStop()}
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HoldButton("L",
                    onDown = { sendTilt("Left") },
                    onUp = { sendTiltStop() }
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (tiltCommand == "Center") Color(0xFF374151) else Color(0xFF2D8CFF),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CAM",
                        color = if (tiltCommand == "Center") Color.White else Color(0xFF1F2937),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                HoldButton("R",
                    onDown = { sendTilt("Right") },
                    onUp = { sendTiltStop() }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            HoldButton("D",
                onDown = { sendTilt("Down")},
                onUp = { sendTiltStop() }
            )
        }
        VerticalDivider()

        // Center reset button (separate column on the right)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Center", color = Color(0xFF6B7280), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(70.dp)
                    .background(Color(0xFF2D8CFF), RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                socket?.emit("tilt", JSONObject().apply {
                                    put("command", "center")
                                })
                                onTiltRelease()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CTR",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Press-and-hold button — fires onDown immediately, onUp when finger lifts
@Composable
fun HoldButton(label: String, onDown: () -> Unit, onUp: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(Color(0xFF2D8CFF), RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onDown()
                        tryAwaitRelease()
                        onUp()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
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