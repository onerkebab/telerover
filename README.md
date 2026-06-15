# Telerover: App-Controlled Telepresence Rover (PoC)

**Telerover** is a proof-of-concept (PoC) telepresence robot designed to demonstrate the integration of low-level hardware control, real-time WebRTC audio/video streaming, and full-stack software architecture. 

This project was built to explore the intersection of mechanical assembly, embedded scripting, and modern web protocols, serving as a functional foundation for a remote-controlled robotic avatar.

## Project Overview

The system consists of a 4WD robotic chassis powered by a Raspberry Pi 4B, which handles both local hardware orchestration and WebRTC media routing. A Node.js signaling server brokers peer-to-peer connections between the rover and remote clients (a web interface or a mobile app), allowing for ultra-low-latency video feeds and instantaneous motor/camera control.

### Key Capabilities Demonstrated:
* **Real-time Communication:** Low-latency peer-to-peer A/V streaming using WebRTC.
* **Hardware Interfacing:** PWM motor control and I2C servo manipulation via Python (`gpiozero`, `lgpio`, `adafruit_servokit`).
* **Process Management:** Node.js spawning, managing, and exchanging data with Python subprocesses via `stdin`/`stdout`.
* **Cross-Platform UI:** Web and React Native mobile clients for remote operation.

---

## System Architecture

### Hardware Stack
The physical rover is built on a 4WD two-tier compact chassis:
* **Compute:** Raspberry Pi 4B with an Uninterruptible Power Supply (UPS) module.
* **Motor Control:** Pololu Dual MC33926 Motor Driver Shield.
* **Drive System:** 4x 12V 100RPM high-torque DC gearmotors, coupled to Pololu 80x10mm rubber-rimmed plastic wheels.
* **Power Management:** Dual isolated power loops to prevent brownouts.
    * *Logic:* 10000mAh 3.7V Li-ion battery for the Pi/UPS.
    * *Motors:* 3S1P 25C 2200mAh 11.1V LiPo battery (with low-voltage alarm module).
* **Telepresence Peripherals:** 
    * 7" TFT LCD display (HDMI+USB) mounted on a gooseneck tablet clamp.
    * Jabra 510 Speakerphone (Bluetooth/USB) for 2-way audio.
    * Raspberry Pi Camera Module 3 (CSI) mounted on an I2C pan-tilt mechanism.

### Software Stack
* **Server/Signaling (Node.js):** An Express & Socket.io server handles WebRTC signaling (SDP offers/answers and ICE candidates) and relays JSON control commands. 
* **Hardware Daemons (Python):** * `motor.py`: Listens to `stdin` for JSON commands to drive the Pololu shield via PWM. Includes a software watchdog to kill motors if the connection drops.
    * `runServo.py`: Manages the pan-tilt I2C servos for camera articulation.
* **Web Client (HTML/JS):** A browser-based interface running locally on the Pi (for the LCD) and remotely for the controller, utilizing the WebRTC API for media.
* **Mobile Client (React Native):** A custom Android application (`App.tsx`) utilizing `react-native-webrtc` to view the feed and control the robot.


---

## Repo Structure

* `server/` - Core Node.js application, Python hardware daemons, and Socket.io setup.
* `public/` - HTML/JS web client for browser-based control and local robot UI.
* `android/` - React Native application for mobile control.
* `server/legacy/` - Previous iterations of motor scripts and local gamepad (evdev) testing.

---

## Roadmap & Future Improvements

While this PoC successfully validates the core software/hardware integration, the next iteration aims to transition from a modular prototype to an ultra-compact, consumer-styled device akin to modern home companion bots. 

**Planned Enhancements:**
* Hardware Miniaturization: Design a custom 3D-printed chassis and integrated PCBs to replace the two-tier plate system and off-the-shelf breakout boards.
* Unify dual-battery architecture into single power source with voltage regulators, integrated charging and decoupling logic/actuation power.
* Improve UX and QoL:
    * Implement an on-screen joystick control scheme in the React Native app.
    * Consolidate the charging system into a unified USB Type-C charging interface.
    * Real-time battery level monitoring through the app.
* Configure the Pi telepresence client to start automatically on startup, with a QR code splash screen to streamline rover-app pairing.