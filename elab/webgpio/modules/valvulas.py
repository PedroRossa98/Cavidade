import RPi.GPIO as GPIO
import time

GPIO.setmode(GPIO.BCM)
GPIO.setwarnings(False)



def int_valvulas():
    GPIO.setup(17, GPIO.OUT, initial=GPIO.HIGH)
    # GPIO.setup(17, GPIO.OUT, initial=GPIO.HIGH)
    # GPIO.setup(17, GPIO.OUT, initial=GPIO.HIGH)
    return


def open_valvulas(valvulas, sleep_time):
    GPIO.output(int(valvulas), GPIO.LOW)
    time.sleep(float(sleep_time)*0.001)
    GPIO.output(int(valvulas), GPIO.HIGH)
    return
    

