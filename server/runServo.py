import pygame
from adafruit_servokit import ServoKit

kit = ServoKit(channels=16)
kit.servo[0].angle = 90
kit.servo[1].angle = 90

pygame.init()
pygame.display.set_mode((100, 100))

move_delay = 0.1
last_move_time = {'up': 0, 'down': 0, 'left': 0, 'right': 0}

def move_up():
    current_time = pygame.time.get_ticks() / 1000.0
    if current_time - last_move_time['up'] > move_delay:
        kit.servo[0].angle = min(180, kit.servo[0].angle + 5)
        last_move_time['up'] = current_time

def move_down():
    current_time = pygame.time.get_ticks() / 1000.0
    if current_time - last_move_time['down'] > move_delay:
        kit.servo[0].angle = max(0, kit.servo[0].angle - 5)
        last_move_time['down'] = current_time

def move_left():
    current_time = pygame.time.get_ticks() / 1000.0
    if current_time - last_move_time['left'] > move_delay:
        kit.servo[1].angle = max(0, kit.servo[1].angle - 5)
        last_move_time['left'] = current_time

def move_right():
    current_time = pygame.time.get_ticks() / 1000.0
    if current_time - last_move_time['right'] > move_delay:
        kit.servo[1].angle = min(180, kit.servo[1].angle + 5)
        last_move_time['right'] = current_time

def stop():
    pass

running = True
while running:
    for event in pygame.event.get():
        if event.type == pygame.QUIT:
            running = False
        elif event.type == pygame.KEYDOWN:
            if event.key != pygame.K_UP and event.key != pygame.K_DOWN and event.key != pygame.K_LEFT and event.key != pygame.K_RIGHT:
                running = False
   
    keys = pygame.key.get_pressed()
   
    if keys[pygame.K_UP]:
        move_up()
    elif keys[pygame.K_DOWN]:
        move_down()
    elif keys[pygame.K_LEFT]:
        move_left()
    elif keys[pygame.K_RIGHT]:
        move_right()
    else:
        stop()

pygame.quit()
