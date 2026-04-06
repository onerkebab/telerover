const socket = io();
const localVideo = document.getElementById('localVideo');
const remoteVideo = document.getElementById('remoteVideo');
const callButton = document.getElementById('callButton');
const toggleBtn = document.getElementById('toggleBtn');

let isVisible;
let localStream;
let peerConnection;

// Assuming Tailscale is used
const rtcConfig = { iceServers: [] }; 

async function startMedia() {
    try {
        // Request a 720p@30 feed
        const constraints = {
            audio: true,
            video: {
                width: { ideal: 1280, max: 1920 },
                height: { ideal: 720, max: 1080 },
                frameRate: { ideal: 24, max: 30 }
            }
        };
        
        localStream = await navigator.mediaDevices.getUserMedia(constraints);
        localVideo.srcObject = localStream;
        callButton.disabled = false; 
        
    } catch (error) {
        console.error("Error accessing media devices.", error);
    }
}

function createPeerConnection() {
    if (!localStream) {
        console.error("Cannot create connection: No local stream available.");
        return; 
    }

    peerConnection = new RTCPeerConnection(rtcConfig);
    
    localStream.getTracks().forEach(track => {
        peerConnection.addTrack(track, localStream);
    });

    // Find the transceiver handling the video track, forcing H.264
    const videoTransceiver = peerConnection.getTransceivers().find(t => 
        t.sender.track && t.sender.track.kind === 'video'
    );
    
    // Failsafe check to ensure the browser supports the API
    if (videoTransceiver && typeof videoTransceiver.setCodecPreferences === 'function' && RTCRtpReceiver.getCapabilities) {
        const capabilities = RTCRtpReceiver.getCapabilities('video');
        // Look for H.264 codecs
        const h264Codecs = capabilities.codecs.filter(c => c.mimeType.toLowerCase() === 'video/h264');
        
        if (h264Codecs.length > 0) {
            videoTransceiver.setCodecPreferences(h264Codecs);
            console.log("Successfully forced H.264 Hardware Encoding via Transceiver!");
        } else {
            console.log("H.264 not found in capabilities, falling back to default codec.");
        }
    }

    // Handle incoming remote tracks
    peerConnection.ontrack = event => {
        remoteVideo.srcObject = event.streams[0];
    };

    // 4. Send ICE candidates to the other peer
    peerConnection.onicecandidate = event => {
        if (event.candidate) {
            socket.emit('message', { type: 'candidate', candidate: event.candidate });
        }
    };
    
    // [DEBUG] Log connection status
    peerConnection.oniceconnectionstatechange = () => {
        console.log("Connection Status:", peerConnection.iceConnectionState);
    };
}

callButton.onclick = async () => {
    createPeerConnection();
    const offer = await peerConnection.createOffer();
    await peerConnection.setLocalDescription(offer);
    socket.emit('message', { type: 'offer', offer: offer });
};


socket.on('message', async (message) => {
    if (message.type === 'offer') {
        createPeerConnection();
        await peerConnection.setRemoteDescription(new RTCSessionDescription(message.offer));
        const answer = await peerConnection.createAnswer();
        await peerConnection.setLocalDescription(answer);
        socket.emit('message', { type: 'answer', answer: answer });
    } else if (message.type === 'answer') {
        await peerConnection.setRemoteDescription(new RTCSessionDescription(message.answer));
    } else if (message.type === 'candidate') {
        await peerConnection.addIceCandidate(new RTCIceCandidate(message.candidate));
    }
});

toggleBtn.onclick = () => {
    isVisible = !isVisible;
    localVideo.style.display = isVisible ? 'block' : 'none';
    toggleBtn.textContent = isVisible ? 'Hide My Camera' : 'Show My Camera';
};

startMedia();