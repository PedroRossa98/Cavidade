import serial
import pfeiffer_vacuum_protocol as pvp

def int_com_PPT200(COM):
    # Open the serial port with a 1 second timeout
    s = serial.Serial(COM, timeout=1)
    return s

def get_pressure(s):

    # Read the pressure from address 1 and print it
    p = pvp.read_pressure(s, 1)
    # print("Pressure: {:.3f} bar".format(p))
    return p