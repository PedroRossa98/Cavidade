import serial
import pfeiffer_vacuum_protocol as pvp
from datetime import datetime


COM = 'COM3'

# Open the serial port with a 1 second timeout
s = serial.Serial(COM, timeout=1)


while (True):
    
# Read the pressure from address 1 and print it
    p = pvp.read_pressure(s, 1)
    
    now = datetime.now()
    
    current_time = now.strftime("%H:%M:%S")
    print(current_time,"    Pressure: {:.5f} bar".format(p))
