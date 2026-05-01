#!/usr/bin/env python3
import sys
import json
import time
from adafruit_servokit import ServoKit

pan_angle  = 90  # servo 1 — left/right, pan center
tilt_angle = 180  # servo 0 — up/down, tilt center

# Initialize with retry
kit = None
while kit is None:
    try:
        kit = ServoKit(channels=16)
        kit.servo[0].angle = tilt_angle 
        kit.servo[1].angle = pan_angle
    except Exception as e:
        sys.stderr.write(f"ServoKit init failed, retrying: {e}\n")
        sys.stderr.flush()
        time.sleep(1)



STEP = 5

sys.stdout.write("ready\n")
sys.stdout.flush()

for line in sys.stdin:
    line = line.strip()
    if not line:
        continue
    try:
        msg = json.loads(line)
        cmd = msg.get("command", "")

        if cmd == "up":
            tilt_angle = min(180, tilt_angle + STEP)
            kit.servo[0].angle = tilt_angle
        elif cmd == "down":
            tilt_angle = max(0, tilt_angle - STEP)
            kit.servo[0].angle = tilt_angle
        elif cmd == "left":
            pan_angle = max(0, pan_angle - STEP)
            kit.servo[1].angle = pan_angle
        elif cmd == "right":
            pan_angle = min(180, pan_angle + STEP)
            kit.servo[1].angle = pan_angle
        elif cmd == "center":
            pan_angle  = 90
            tilt_angle = 180
            kit.servo[0].angle = tilt_angle
            kit.servo[1].angle = pan_angle

    except Exception as e:
        sys.stderr.write(f"servo error: {e}\n")
        sys.stderr.flush()
        # Don't crash — try to reinitialize
        try:
            kit = ServoKit(channels=16)
        except:
            pass