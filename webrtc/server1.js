const express = require('express');
const fs = require('fs');
const https = require('https');

const app = express();

// Load certificates
const options = {
  key: fs.readFileSync('server.key'),
  cert: fs.readFileSync('server.cert')
};

// Create HTTPS server
const server = https.createServer(options, app);
const io = require('socket.io')(server);

app.use(express.static('public'));

// Socket.io events

io.on('connection', (socket) => {
    console.log('A user connected:', socket.id);
    
    socket.on('message', (message) => {
        socket.broadcast.emit('message', message);
    });

    // Named signal channel used by the combined HTML
    socket.on('signal', (data) => {
    socket.broadcast.emit('signal', data);
    });

    socket.on('disconnect', () => {
        console.log('User disconnected:', socket.id);
    });
});

const PORT = 3000;
server.listen(PORT, '0.0.0.0', () => {
    console.log(`Secure HTTPS Signaling server running on port ${PORT}`);
});