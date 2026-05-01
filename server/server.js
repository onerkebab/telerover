const express = require('express');
const fs = require('fs');
const https = require('https');
const { spawn } = require('child_process');
const path = require('path');

const app = express();

const options = {
  key: fs.readFileSync('server.key'),
  cert: fs.readFileSync('server.cert')
};

const server = https.createServer(options, app);
const io = require('socket.io')(server);

app.use(express.static('public'));

// ----------------------------
// Motor control via motor.py subprocess
// Python/gpiozero/lgpio handles GPIO — proven to work on this Pi
// Node sends JSON commands over stdin
// ----------------------------
let motorProc = null;
let motorReady = false;

function startMotor() {
  const scriptPath = path.join(__dirname, 'motor.py');
  motorProc = spawn('python3', [scriptPath], {
    stdio: ['pipe', 'pipe', 'pipe']
  });

  motorProc.stdout.on('data', (data) => {
    const msg = data.toString().trim();
    if (msg === 'ready') {
      motorReady = true;
      console.log('motor.py ready');
    } else {
      console.log('motor.py:', msg);
    }
  });

  motorProc.stderr.on('data', (data) => {
    console.error('motor.py error:', data.toString().trim());
  });

  motorProc.on('exit', (code) => {
    motorReady = false;
    console.warn(`motor.py exited (code ${code}) — restarting in 2s`);
    setTimeout(startMotor, 2000);
  });
}

// ----------------------------
// Servo control via runServo.py subprocess
// ----------------------------
let servoProc = null;
let servoReady = false;

function startServo() {
  const scriptPath = path.join(__dirname, 'runServo.py');
  servoProc = spawn('python3', [scriptPath], {
    stdio: ['pipe', 'pipe', 'pipe']
  });

  servoProc.stdout.on('data', (data) => {
    const msg = data.toString().trim();
    if (msg === 'ready') {
      servoReady = true;
      console.log('runServo.py ready');
    }
  });

  servoProc.stderr.on('data', (data) => {
    console.error('servo.py error:', data.toString().trim());
  });

  servoProc.on('exit', (code) => {
    servoReady = false;
    console.warn(`runServo.py exited (code ${code}) — restarting in 2s`);
    setTimeout(startServo, 2000);
  });
}

function sendServo(command) {
  if (!servoReady || !servoProc) return;
  const msg = JSON.stringify({ command }) + '\n';
  servoProc.stdin.write(msg);
}

startServo();

function sendMotor(command, speed = 0) {
  if (!motorReady || !motorProc) return;
  const msg = JSON.stringify({ command, speed }) + '\n';
  motorProc.stdin.write(msg);
}

startMotor();

setTimeout(() => {
    console.log('motorReady:', motorReady);
    sendMotor('forward', 0.5);
    setTimeout(() => sendMotor('stop'), 1000);
}, 3000);
// ----------------------------
// Watchdog — stop if no command for 350ms
// (motor.py has its own watchdog too, double safety)
// ----------------------------
const WATCHDOG_MS = 350;
let lastCommandTime = Date.now();

setInterval(() => {
  if (Date.now() - lastCommandTime > WATCHDOG_MS) {
    sendMotor('stop');
  }
}, 50);

// ----------------------------
// Socket.IO
// ----------------------------
io.on('connection', (socket) => {
  console.log('Client connected:', socket.id);

  // WebRTC signaling relay 
  socket.on('message', (msg) => socket.broadcast.emit('message', msg));

  // Motor commands from controller
  socket.on('control', (data) => {
      console.log('control received:', JSON.stringify(data));
      lastCommandTime = Date.now();
      sendMotor(data.command || 'stop', parseFloat(data.speed) || 0.65);
  });

  socket.on('tilt', (data) => {
  console.log('tilt received:', JSON.stringify(data));
  sendServo(data.command || 'center');
  });

  socket.on('stop', () => {
    lastCommandTime = Date.now();
    sendMotor('stop');
  });

  socket.on('disconnect', () => {
    console.log('Client disconnected:', socket.id);
    sendMotor('stop');
  });
});

// Cleanup on exit
process.on('SIGINT',  () => { sendMotor('stop'); process.exit(); });
process.on('SIGTERM', () => { sendMotor('stop'); process.exit(); });

const PORT = 3000;
server.listen(PORT, '0.0.0.0', () => {
  console.log(`Server running on https://0.0.0.0:${PORT}`);
  console.log('  Controller: https://<pi-ip>:3000/?role=controller');
  console.log('  Robot:      https://<pi-ip>:3000/?role=robot');
});