import RPi.GPIO as GPIO
from evdev import InputDevice, ecodes
import sys

GPIO.cleanup()

# Hardware Pin Configuration
m1pwm = 12; m1en = 22; m1dir = 24 # Motor 1 (Left)
m2pwm = 13; m2en = 23; m2dir = 25 # Motor 2 (Right)

# GPIO Setup
GPIO.setwarnings(False)
GPIO.setmode(GPIO.BCM)

pins = [m1pwm, m2pwm, m1en, m2en, m1dir, m2dir]
for pin in pins:
    GPIO.setup(pin, GPIO.OUT)

GPIO.output(m1en, GPIO.HIGH)
GPIO.output(m2en, GPIO.HIGH)

# Initialize PWM at 100Hz
pwm_left = GPIO.PWM(m2pwm, 100)
pwm_right = GPIO.PWM(m1pwm, 100)
pwm_left.start(0)
pwm_right.start(0)

# Controller
DEVICE_PATH = '/dev/input/by-id/usb-Logitech_Wireless_Gamepad_F710_2C159CE8-event-joystick'

try:
    gamepad = InputDevice(DEVICE_PATH)
    print(f"Connected to: {gamepad.name} (Arcade Drive Mode)")
except FileNotFoundError:
    print("Controller not found. Check the USB dongle, X/D switch, and DEVICE_PATH.")
    GPIO.cleanup()
    sys.exit()

# Motor Control Logic
MAX_JOY_VAL = 32768.0
DEADZONE = 4000 # Slightly larger deadzone helps with single-stick drift

# State trackers for the joystick axes
stick_x = 0.0
stick_y = 0.0

TURN_SENSITIVITY = 0.65 # Adjust this between 0.5 (wide turns) and 1.0 (spin in place)

def update_motors():
    """
    Calculates Arcade Drive mixing with input shaping for smoother steering.
    """
    global stick_x, stick_y

    # Normalize values from -1.0 to 1.0
    x_norm = stick_x / MAX_JOY_VAL
    y_norm = -stick_y / MAX_JOY_VAL # Invert Y

    # Apply deadzone
    if abs(stick_x) < DEADZONE: x_norm = 0.0
    if abs(stick_y) < DEADZONE: y_norm = 0.0

    # Input shaping (cubed)
    x_norm = x_norm ** 3
    y_norm = y_norm ** 3

    # Prevents the steering axis from completely overpowering the forward drive
    x_norm = x_norm * TURN_SENSITIVITY

    # Arcade drive
    left_val = y_norm + x_norm
    right_val = y_norm - x_norm

    # Scale values
    max_val = max(abs(left_val), abs(right_val), 1.0)
    left_val /= max_val
    right_val /= max_val

    GPIO.output(m2dir, GPIO.HIGH if left_val >= 0 else GPIO.LOW)
    pwm_left.ChangeDutyCycle(abs(left_val) * 100.0)

    GPIO.output(m1dir, GPIO.HIGH if right_val >= 0 else GPIO.LOW)
    pwm_right.ChangeDutyCycle(abs(right_val) * 100.0)

print("Ready to drive! Use the LEFT stick. Press Ctrl+C to quit.")

try:
    for event in gamepad.read_loop():
        if event.type == ecodes.EV_ABS:
            
            if event.code == ecodes.ABS_X:
                stick_x = event.value
                update_motors()

            elif event.code == ecodes.ABS_Y:
                stick_y = event.value
                update_motors()

except KeyboardInterrupt:
    print("\nShutting down rover...")
except Exception as e:
    print(f"\nAn error occurred: {e}")
finally:
    pwm_left.stop()
    pwm_right.stop()
    GPIO.cleanup()
    print("GPIO cleaned up safely.")
