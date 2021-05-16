import sys
import os
import configparser
import RPi.GPIO as GPIO

from flask import Flask, request, jsonify, session, Response
from flask_session import Session
from flask_csv import send_csv
from flask_cors import CORS
from random import randrange

# print ('Syst 1 : ', sys.path)
sys.path.append('/home/pi/Cavidade/elab/webgpio/modules')
# print ('Syst 2 : ', sys.path)

import PPT200 as PPT200
import Analisador_v2 as arinst
import valvulas as val

app = Flask(__name__)
app.secret_key = '*}6Ttt)G7X_T}3VF:ygc'


app.config['SESSION_TYPE'] = 'filesystem'
# app.config['SESSION_TYPE'] = 'memcached'
#sess = Session(app)
    

app.debug = False

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
        serial.close()
        return jsonify({'pressure': pressure, 'result': 'OK!'})
        
    return ''


@app.route('/pressure/start', methods=['GET'])
def start_pressure():
    if request.method == 'GET':
        PPT200.reset_data()
        
    return ''
    
    
@app.route('/pressure/download', methods=['GET'])
def download_pressure():
    if request.method == 'GET':
        PPT200.store_pressure(randrange(20, 70))
        pressure_data = PPT200.get_data()
        return send_csv(pressure_data, 'pressure.csv', ['time', 'pressure'])

    return ''

@app.route('/arinst', methods=['GET'])
def arinst_data():
    if request.method == 'GET':
        
        print (request.args)
        print ('Start '+ request.args.get('start'))
        print ('Stop '+ request.args.get('stop'))
        print ('Step '+ request.args.get('step'))
        print ('Itera '+ request.args.get('n_itera'))
        
        data = arinst.arnist('/dev/ttyACM0',int(request.args.get('start')), int(request.args.get('stop')), int(request.args.get('step')), int(request.args.get('n_itera')))
        #print (data)
        return jsonify(data)
        
    return ''
    

def upload_path():
    path = '';

    try:
        config = configparser.RawConfigParser()
        config.read('config.cfg')
        upload_dict = dict(config.items('upload'))
        path = upload_dict['path']
    except:
        print('Check config.cfg !!!')
        pass

    return path


@app.route('/arinst/list', methods=['GET'])
def arinst_data_list():
    filelist = []
    if request.method == 'GET':
        path = upload_path()
        files = os.listdir(path)

        for file in files:
            filelist.append(file)

            print('file -{}-'.format(file))
 
    return jsonify(filelist)


@app.route('/arinst/csv/<filename>', methods=['GET', 'DELETE'])
def arinst_data_csv(filename):
    if request.method == 'GET':
        filename== request.view_args['filename']
        print('filename -{}-'.format(filename))
        path = upload_path()
        fullFilename = path + filename
        print('path -{}-'.format(fullFilename))

        try:
            with open(fullFilename) as fp:
                csv = fp.read()
                return Response(csv, mimetype="text/csv", headers={"Content-disposition": "attachment; filename="+ filename })
        except:
            pass
 
    if request.method == 'DELETE':
        filename== request.view_args['filename']
        print('filename -{}-'.format(filename))
        path = upload_path()
        fullFilename = path + filename
        print('path -{}-'.format(fullFilename))

        try:
            if os.path.isfile(fullFilename):
                os.remove(fullFilename)
                return 'File \'' + filename + '\' : Deleted'
        except:
            pass


    return 'File \'' + filename + '\' : Not found'




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
