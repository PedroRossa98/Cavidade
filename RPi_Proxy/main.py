import requests
import json
from datetime import datetime
import importlib
import threading
import time
import configparser
import serial
import json
import re
# import pic_interface.interface as interface



lock = threading.Lock()

config = configparser.ConfigParser()
config.read('server_info.ini')

FORMAT = 'utf-8'

CONFIG_OF_EXP = []
next_execution = {}
status_config= {}
SAVE_DATA = []

test =False
test_end_point_print = True
Working = False
Waiting_for_config = True

interface = None

HEADERS = { 
  "Authentication": str(config['DEFAULT']['SECRET']), 
  "Content-Type": "application/json"
}

def send_exp_data(config_exp):
    global SAVE_DATA
    global Working
    global next_execution
    global lock
    while True:
        exp_data = interface.receive_data_from_exp(config,config_exp)
        if exp_data == True:
            Working = False
            next_execution = {}
            time.sleep(0.00001)
            return 


def Send_Config_to_Pic(myjson):
    global Working
    global Waiting_for_config
    print("Recebi mensagem de configurestart. A tentar configurar pic")
    # actual_config, config_feita_correcta = interface.do_config(myjson)
    print(myjson)
    if True :   #se config feita igual a pedida? (opcional?)
        data_thread = threading.Thread(target=send_exp_data,args=(myjson,),daemon=True)
        print("PIC configurado.\n")
        if interface.do_start():                            #tentar começar experiencia
            print("aqui")
            Working = True
            data_thread.start()
            time.sleep(0.000001)
            #O JSON dos config parameters está mal e crasha o server. ARRANJAR
            #send_mensage = '{"reply_id": "2","status":"Experiment Running","config_params":"'+str(myjson["config_params"])+'}'
            # Working = True
            send_mensage = {"reply_id": "2","status":"Experiment Running"}
        else :
            send_mensage = {"reply_id": "2", "error":"-1", "status":"Experiment could not start"}
    
    else:
        send_mensage = {"reply_id": "2", "error":"-2", "status":"Experiment could not be configured"}
    return send_mensage



# REST
def GetConfig():
    global CONFIG_OF_EXP
    api_url = "http://"+config['DEFAULT']['SERVER']+":"+config['DEFAULT']['PORT']+"/api/v1/apparatus/"+config['DEFAULT']['APPARATUS_ID']
    # msg = {"secret":SEGREDO}
    response =  requests.get(api_url, headers ={"Authentication":"Secret estou bem"})
    CONFIG_OF_EXP = response.json()["experiment"]
    if (test_end_point_print):
        print(json.dumps(CONFIG_OF_EXP,indent=4))
    return ''

def GetExecution():
    global next_execution
    api_url = "http://"+config['DEFAULT']['SERVER']+":"+config['DEFAULT']['PORT']+"/api/v1/apparatus/"+config['DEFAULT']['APPARATUS_ID']+"/nextexecution"
    response =  requests.get(api_url,headers = HEADERS)
    print(response.json())


    next_execution = response.json()
    if (test_end_point_print):
        print("REQUEST\n")
        print(json.dumps(next_execution,indent=4))
    return ''




# if __name__ == "__main__":
#     GetConfig()
#     GetExecution()
#     print(json.dumps(next_execution,indent=4))
#     SendPartialResult()

def main_cycle():
    global CONFIG_OF_EXP
    global next_execution
    global status_config
    global Working
    if CONFIG_OF_EXP != None:
        if test :
            print("Esta a passar pelo if none este\n")
        while True:
            if not Working:
                if test :
                    print("Esta a passar pelo if none\n")
                GetExecution()
                if test:
                    print("\n\nIsto_1 :")
                    print (next_execution)
            time.sleep(0.5)
            if ("config" in next_execution.keys()) and (not Working) and next_execution["config"]!=None:
                save_execution =next_execution.get("config",None)
                if save_execution != None:
                    print(json.dumps(save_execution))
                # if save_execution != None:                                 # Estava a passar em cima e não sei bem pq 
                    status_config=Send_Config_to_Pic(next_execution)
                if test:
                    print("O valor do Working é: "+str(Working))
            # pass
            # print("teste 12")
            # print(Working)

    return ''

if __name__ == "__main__":
    print("[Starting] Experiment Server Starting...")
    # global next_execution
    connected = None
    # importlib.import_module("pic_interface.PPT200")
    interface = importlib.import_module("pic_interface.interface")
    while True:
        try:
            GetConfig()
            print(CONFIG_OF_EXP["config"])
            if interface.do_init(CONFIG_OF_EXP["config"]) :
                main_cycle()
            else:
                print ("Experiment not found")
        except:
            #LOG ERROR
            print("Faill to connect to the Server. Trying again after 10 s")
            #So faz shutdown do socket se este chegou a estar connected
            time.sleep(10)



# TODO: o que fazer se a arinst estiver desligado




# global CONFIG_OF_EXP
# global interface
# interface = importlib.import_module("pic_interface.interface")
# print("Recebi mensagem de configuracao. A tentar inicializar a experiencia\n")
# CONFIG_OF_EXP = myjson['config_file']

# #LIGAR A DISPOSITIVO EXP - INIT
# #Talvez passar erros em forma de string JSON para incluir no reply em vez de OK e NOT OK
# if interface.do_init(CONFIG_OF_EXP) :
#     send('{"reply_id": "1", "status":"Experiment initialized OK"}')

# else :
#     send('{"reply_id": "1", "error":"-1", "status":"Experiment initialized NOT OK"}')
