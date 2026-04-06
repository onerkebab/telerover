# test_motors.py
# Turn on motors running in the same direction for 5 seconds

import RPi.GPIO as GPIO
from time import sleep

def GPIO_out(pin):
    GPIO.setup(pin, GPIO.OUT)
GPIO.cleanup()
GPIO.setmode(GPIO.BCM)

m1pwm = 12; m2pwm = 13
m1en = 22; m2en = 23
m1dir = 24; m2dir = 25
GPIO_out(m1pwm)
GPIO_out(m2pwm)
GPIO_out(m1en)
GPIO_out(m2en)
GPIO_out(m1dir)
GPIO_out(m2dir)

GPIO.output(m1en, 1)
GPIO.output(m2en, 1)
GPIO.output(m1dir, 1)
GPIO.output(m2dir, 1)
GPIO.output(m1pwm, 1)
GPIO.output(m2pwm, 1)

sleep(5)

GPIO.output(m1pwm, 0)
GPIO.output(m2pwm, 0)

quit()
