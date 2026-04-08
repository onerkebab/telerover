import React, { useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, SafeAreaView } from 'react-native';
import { RTCView } from 'react-native-webrtc'; 

export default function App() {
  // 1. App State
  const [status, setStatus] = useState('Disconnected'); 
  const [remoteStream, setRemoteStream] = useState(null); 
  const [isMuted, setIsMuted] = useState(false);

  // 2. Button Functions
  const handleConnect = () => {
    setStatus('Connecting...');
    setTimeout(() => {
      setStatus('Live');
    }, 2000);
  };

  const handleDisconnect = () => {
    setStatus('Disconnected');
    setRemoteStream(null);
  };

  // 3. The UI Layout
  return (
    <SafeAreaView style={styles.container}>
      
      {/* HEADER */}
      <View style={styles.header}>
        <Text style={styles.statusText}>Status: {status}</Text>
      </View>

      {/* MAIN BODY: Video Feed */}
      <View style={styles.videoContainer}>
        {status === 'Live' ? (
          <RTCView 
            streamURL={remoteStream ? remoteStream.toURL() : ''} 
            style={styles.videoStyle} 
            objectFit="cover" 
          />
        ) : (
          <View style={styles.placeholder}>
            <Text style={styles.placeholderText}>No Video Feed</Text>
          </View>
        )}
      </View>

      {/* FOOTER: Controls */}
      <View style={styles.controlsContainer}>
        {status !== 'Live' ? (
          <TouchableOpacity style={[styles.button, styles.connectBtn]} onPress={handleConnect}>
            <Text style={styles.buttonText}>Connect to Pi</Text>
          </TouchableOpacity>
        ) : (
          <>
            <TouchableOpacity 
              style={[styles.button, isMuted ? styles.mutedBtn : styles.activeBtn]} 
              onPress={() => setIsMuted(!isMuted)}>
              <Text style={styles.buttonText}>{isMuted ? "Unmute" : "Mute"}</Text>
            </TouchableOpacity>
            
            <TouchableOpacity style={[styles.button, styles.disconnectBtn]} onPress={handleDisconnect}>
              <Text style={styles.buttonText}>Disconnect</Text>
            </TouchableOpacity>
          </>
        )}
      </View>

    </SafeAreaView>
  );
}

// 4. The Styles
const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#1A1A1A', 
  },
  header: {
    paddingTop: 50,
    paddingBottom: 20,
    alignItems: 'center',
    backgroundColor: '#222',
  },
  statusText: {
    color: '#00FF00', 
    fontSize: 16,
    fontWeight: 'bold',
    letterSpacing: 1,
  },
  videoContainer: {
    flex: 1, 
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#000',
    margin: 20,
    borderRadius: 15,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: '#333',
  },
  videoStyle: {
    width: '100%',
    height: '100%',
  },
  placeholder: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  placeholderText: {
    color: '#666',
    fontSize: 18,
  },
  controlsContainer: {
    flexDirection: 'row', 
    justifyContent: 'space-evenly',
    padding: 30,
    paddingBottom: 50,
  },
  button: {
    paddingVertical: 15,
    paddingHorizontal: 30,
    borderRadius: 30,
    minWidth: 120,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 3,
    elevation: 5, 
  },
  connectBtn: {
    backgroundColor: '#007AFF', 
    width: '80%',
  },
  disconnectBtn: {
    backgroundColor: '#FF3B30', 
  },
  activeBtn: {
    backgroundColor: '#34C759', 
  },
  mutedBtn: {
    backgroundColor: '#FF9500', 
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  }
});