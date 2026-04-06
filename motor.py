#!/usr/bin/env python3
"""
Motor controller for Pololu MC33926.
Reads JSON commands from stdin, one per line:
  {"command": "forward", "speed": 0.65}
  {"command": "stop"}
"""
import sys
import json
import time
import threading

from gpiozero import Device, PWMOutputDevice, DigitalOutputDevice
from gpiozero.pins.lgpio import LGPIOFactory
Device.pin_factory = LGPIOFactory()

RIGHT_PWM = PWMOutputDevice(12, frequency=1000, initial_value=0)
LEFT_PWM  = PWMOutputDevice(13, frequency=1000, initial_value=0)
RIGHT_EN  = DigitalOutputDevice(22, initial_value=False)
LEFT_EN   = DigitalOutputDevice(23, initial_value=False)
RIGHT_DIR = DigitalOutputDevice(24, initial_value=False)
LEFT_DIR  = DigitalOutputDevice(25, initial_value=False)

RIGHT_FORWARD_DIR = 1
LEFT_FORWARD_DIR  = 0

WATCHDOG_TIMEOUT = 0.35
last_command_time = time.monotonic()

def set_side(side, speed, forward):
    speed = max(0.0, min(1.0, float(speed)))
    if side == "right":
        if speed == 0:
            RIGHT_PWM.value = 0; RIGHT_EN.off(); return
        RIGHT_EN.on()
        RIGHT_DIR.value = RIGHT_FORWARD_DIR if forward else (1 - RIGHT_FORWARD_DIR)
        RIGHT_PWM.value = speed
    elif side == "left":
        if speed == 0:
            LEFT_PWM.value = 0; LEFT_EN.off(); return
        LEFT_EN.on()
        LEFT_DIR.value = LEFT_FORWARD_DIR if forward else (1 - LEFT_FORWARD_DIR)
        LEFT_PWM.value = speed

def stop_all():
    RIGHT_PWM.value = 0; LEFT_PWM.value = 0
    RIGHT_EN.off(); LEFT_EN.off()

def drive(command, speed=0):
    global last_command_time
    last_command_time = time.monotonic()
    if command == "forward":
        set_side("right", speed, True);  set_side("left", speed, True)
    elif command == "back":
        set_side("right", speed, False); set_side("left", speed, False)
    elif command == "left":
        set_side("right", speed, False); set_side("left", speed, True)
    elif command == "right":
        set_side("right", speed, True);  set_side("left", speed, False)
    else:
        stop_all()

def watchdog():
    while True:
        if time.monotonic() - last_command_time > WATCHDOG_TIMEOUT:
            stop_all()
        time.sleep(0.05)

threading.Thread(target=watchdog, daemon=True).start()

stop_all()
sys.stdout.write("ready\n")
sys.stdout.flush()

for line in sys.stdin:
    line = line.strip()
    if not line:
        continue
    try:
        msg = json.loads(line)
        drive(msg.get("command", "stop"), msg.get("speed", 0))
    except Exception as e:
        sys.stderr.write(f"motor.py error: {e}\n")
        sys.stderr.flush()