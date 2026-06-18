# Telerover: App-Controlled Telepresence Rover

![GPLv3](https://img.shields.io/badge/License-GPLv3-%23BD0000) ![Python](https://img.shields.io/badge/Python-3776AB?logo=python&logoColor=fff) ![Node.js](https://img.shields.io/badge/Node.js-6DA55F?logo=node.js&logoColor=white) ![React Native](https://img.shields.io/badge/React_Native-%2320232a.svg?logo=react&logoColor=%2361DAFB) ![HTML](https://img.shields.io/badge/HTML-%23E34F26.svg?logo=html5&logoColor=white)

## About Telerover

<p align="center">
  <img src="assets/main_view.jpg" alt="Telerover" width="40%">
</p>

Telerover is a proof-of-concept (PoC) telepresence rover designed to demonstrate the integration of low-level hardware control, real-time two-way audio/video streaming, and full-stack software architecture. Inspiration was largely taken from [enabot's ROLA Mini FamilyBot](https://store.enabot.com/products/rola-mini-familybot), although the crude Telerover features way more zipties, tape and standoffs than a polished consumer product.

The rover consists of a 4-wheel differential drive, 2-tier chassis powered by a Raspberry Pi 4B, which handles local hardware control, peripheral interfacing and WebRTC media routing. A Node.js signaling server brokers peer-to-peer connections between the rover and remote clients (web interface/mobile app), allowing for ultra-low-latency video feeds and instantaneous motor/camera control. 

To complement the rover, an Android mobile app was also developed using React Native to act as a client that connects to the Pi-hosted Node.js server and provides an interactive UI for telepresence and rover control.

### Demonstrated Functions:

<p align="center">
  <img src="assets/demo.gif" alt="Telerover" width="50%">
</p>

* **Real-time communication:** Low-latency peer-to-peer A/V streaming using WebRTC.
* **Hardware control:** PWM motor control and I2C servo manipulation via Python.
* **Inter-process communication:** Node.js spawning, managing, and exchanging data with low-level Python scripts.
* **User interface:** Web and React Native mobile app clients for remote operation.

## System Architecture

<p align="center">
  <img src="assets/Diagram.svg" alt="Telerover" width="100%">
</p>

### Hardware Stack
The physical rover is built on a 4WD two-tier compact chassis:

<p align="center">
  <img src="assets/frontal_view.jpg" alt="Telerover" width="50%">
</p>

* **Compute:** Raspberry Pi 4B.
* **Motor control:** Pololu Dual MC33926 Motor Driver Shield.
* **Drivetrain:** 4-wheel differential drive.
    * 4x 25mm 12V 100RPM high-torque DC gearmotors.
    * Pololu 80x10mm rubber-rimmed plastic wheels.
    * Motor shafts are extended with a shaft coupler and 2cm-long stainless steel shaft to ensure wheel-chassis clearance.
* **Power:** Dual isolated power sources to prevent brownouts.
    * *RPi:* 10000mAh 3.7V Li-ion battery (via UPS module).
    * *Motors:* 2200mAh 11.1V LiPo battery (via Motor Shield with low-voltage alarm module).
* **Peripherals:** 
    * *Monitor:* 7" TFT LCD display (HDMI+USB) mounted on a gooseneck tablet clamp.
    * *Audio:* Jabra 510 Speakerphone (Bluetooth/USB) for 2-way audio with built-in noise cancellation.
    * *Camera:* Raspberry Pi Camera Module 3 (CSI) mounted on an I2C pan-tilt servo mechanism.


### Software Stack

* **Server/Signaling (Node.js):** An Express & Socket.io server handles WebRTC signaling (SDP offers/answers and ICE candidates) and relays JSON control commands. 
* **Hardware Control (Python):** 
    * `motor.py`: Listens to `stdin` for JSON commands to send PWM signals to the MC33926 motor drivers. 
      * Includes a software watchdog to kill motors if the connection drops.
    * `runServo.py`: Manages the pan-tilt I2C servos for camera articulation.
* **Web Client (HTML/JS):** A browser-based interface running locally on the Pi (for the LCD) and remotely for the controller, utilizing the WebRTC API for media.
* **Mobile Client (React Native):** An Android application utilizing `react-native-webrtc` to view the remote and local camera feeds picture-in-picture (PiP) and control the rover.

<p align="center">
  <img src="assets/app_ui.jpg" alt="Telerover" width="90%">
</p>

## Repo Structure

* `server/` - Core Node.js application, Python scripts, and Socket.io setup.
* `public/` - HTML/JS web client for browser-based control and local rover UI.
* `android/` - React Native application for mobile control.
* `server/legacy/` - Previous iterations of motor scripts and local gamepad (evdev) testing.
* `assets/` - Media of the Telerover project.

## Room for Improvement

While this PoC successfully validates the core software/hardware integration, there are many potential improvements before the project can transition from a modular prototype to an ultra-compact, consumer-styled companion robot like the ROLA Mini Family Bot. 

* Hardware Miniaturization: Design a custom 3D-printed chassis and integrated PCBs to replace the two-tier plate system and off-the-shelf breakout boards.
* Unify dual-battery architecture into single power source with voltage regulators, integrated charging and decoupling logic/actuation power.
* Improve UX and QoL:
    * Implement an on-screen joystick control scheme in the React Native app.
    * Consolidate the charging system into a unified USB Type-C charging interface.
    * Real-time battery level monitoring through the app.
* Configure the Pi telepresence client to start automatically on startup, with a QR code splash screen to streamline rover-app pairing.

## Credit

Check out our LinkedIn and GitHub pages here!

<div align="center">

|[![LinkedIn](https://custom-icon-badges.demolab.com/badge/Sufi_Hossain-0A66C2?logo=linkedin-white&logoColor=fff)](https://www.linkedin.com/in/sufihossain/) |[![LinkedIn](https://custom-icon-badges.demolab.com/badge/Quang_Nguyen-0A66C2?logo=linkedin-white&logoColor=fff)](https://www.linkedin.com/in/qnguyen13/) | [![LinkedIn](https://custom-icon-badges.demolab.com/badge/Khoi_Phan-0A66C2?logo=linkedin-white&logoColor=fff)](https://www.linkedin.com/in/phantruonganhkhoi/)|[![LinkedIn](https://custom-icon-badges.demolab.com/badge/Saumili_Chakravarty-0A66C2?logo=linkedin-white&logoColor=fff)](https://www.linkedin.com/in/saumili-chakravarty/) |[![LinkedIn](https://custom-icon-badges.demolab.com/badge/Nyasha_Gwaza-0A66C2?logo=linkedin-white&logoColor=fff)](https://www.linkedin.com/in/nyasha-gwaza-9a8098181/) |
|:-:|:-:|:-:|:-:|:-:|
| [![GHSufi](https://img.shields.io/badge/sufihossain-black?style=flat&logo=github)](https://github.com/sufihossain)  | [![GHQuang](https://img.shields.io/badge/quang--n--h-black?logo=github)](https://github.com/quang-n-h) |  [![GHKhoi](https://img.shields.io/badge/-onerkebab-black?logo=github)](https://github.com/onerkebab) | [![GHSaumili](https://img.shields.io/badge/schakr13-black?logo=github)](https://github.com/schakr13)  | [![GHNyasha](https://img.shields.io/badge/Nyasha20-black?logo=github)](https://github.com/Nyasha20)  |
|  <img src="https://github.com/sufihossain.png?size=125" width=125> | <img src="https://github.com/quang-n-h.png?size=125" width=125> |  <img src="https://github.com/onerkebab.png?size=125" width=125> |  <img src="https://github.com/schakr13.png?size=125" width=125> | <img src="https://github.com/Nyasha20.png?size=125" width=125>  |




<p align="center">
  <img src="assets/WE.jpg" alt="Telerover" width="60%">
</p>

*The Telerover team at the 2026 University of Rochester Design Day.*

</div>
