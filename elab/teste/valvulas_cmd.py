import RPi.GPIO as GPIO
import time

GPIO.setmode(GPIO.BCM)
GPIO.setwarnings(False)

check= 0
sleep_time = 0
n_of_brust= 0
sleep_time_1= 0

GPIO.setup(17, GPIO.OUT, initial=GPIO.HIGH)





while True:
    if (check == 0):
        sleep_time = input("Time led off (ms): ")
        sleep_time_1 = input("Time led on (ms): ")
        n_of_brust = input("Numero de brust: ")
        check=1
    else:
        for x in range(0, int(n_of_brust)):
            GPIO.output(17, GPIO.LOW)
            time.sleep(float(sleep_time)*0.001)
            GPIO.output(17, GPIO.HIGH)
            time.sleep(float(sleep_time_1)*0.001)
        check =0
    
    time.sleep(1)