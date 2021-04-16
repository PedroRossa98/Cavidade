# Module Imports
import sys

import RPi.GPIO as GPIO
from flask import Flask, request, jsonify
from flask_cors import CORS
from random import randrange

# print ('Syst 1 : ', sys.path)
sys.path.append('/home/pi/Cavidade/elab/webgpio/modules')
# print ('Syst 2 : ', sys.path)

import PPT200 as PPT200

import valvulas as val
app = Flask(__name__)
CORS(app)

val.int_valvulas()
# Set button and PIR sensor pins as an input
# GPIO.setup(button, GPIO.IN)   
# GPIO.setup(senPIR, GPIO.IN)

@app.route('/')
def hello_world():
    return 'Hello, Nice!'

@app.route('/test', methods=['GET', 'POST'])
def add_message():
    if request.method == 'GET':
        print ('GET :\n')
        print (request.args)
        print (request.args.get('ano'))
        print (request.args.get('dia'))
        print (request.args.get('mes'))
        return jsonify({"result": 'OK!'})
    if request.method == 'POST':
       print ('POST :\n')
       print (request.json)
       print (request.json.get('temperature'))
       print (request.json.get('humidity'))
       print (request.json.get('place'))
       return jsonify({"result": 'TODO!'})


@app.route('/gpio/switch', methods=['PUT'])
def gpio_switch():
    if request.method == 'PUT':
        origin = request.headers.get('Origin')	
        print ('PUT :\n')
        print (request.args)
        print (request.args.get('pin'))
        print (request.args.get('status'))
        print (request.args.get('time'))

        val.open_valvulas(17, request.args.get('time'))

        return jsonify({'pin' : request.args.get('pin'), 'result': 'OK!'})


@app.route('/pressure', methods=['GET'])
def ppt_pressure():
    if request.method == 'GET':
	
        serial = PPT200.int_com_PPT200('/dev/ttyUSB0')
        pressure = PPT200.get_pressure(serial)
		
        return jsonify({'pressure': pressure, 'result': 'OK!'})
        
    return ''

    
@app.route('/gpio/status', methods=['GET'])
def gpio_get_status():
    if request.method == 'GET':
        print ('GET :\n')
        print (request.args)
        print (request.args.get('pin'))
        if request.args.get('pin') != None:
            pin = int(request.args.get('pin'))
            print ('pin : ', pin)
            GPIO.setup(pin, GPIO.OUT)
            status = GPIO.input(pin)
            print ('status : ', status)

            return jsonify({"pin": pin, "result": status})

    return ''


@app.route('/gpio/status', methods=['PUT'])
def gpio_put_status():
    if request.method == 'PUT':
        print ('PUT :\n')
        print (request.args)
        print (request.args.get('pin'))
        print (request.args.get('status'))
        pin = int(request.args.get('pin'))
        GPIO.setup(pin, GPIO.OUT)
        status = GPIO.input(pin)
        print ('status : ', status)

        return jsonify({"pin": pin, "result": status})


if __name__ == '__main__':
    app.run('0.0.0.0', 8085)
