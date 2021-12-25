import requests
import json


def SendPartialResult(config,msg):
    # print(next_execution)
    print(str(msg))
    HEADERS = { 
    "Authentication": str(config['DEFAULT']['SECRET']), 
    "Content-Type": "application/json"
    }
    api_url = "http://"+config['DEFAULT']['SERVER']+":"+config['DEFAULT']['PORT']+"/api/v1/result"
    print(api_url)
    # todo = {"value":{"ok":"ola","ponto":"oco"},"time":datetime.now().strftime('%Y-%m-%dT%H:%M:%SZ'),"result_type":"p"}
    print("Aqui:  " ,json.dumps(msg,indent=4))
    requests.post(api_url, headers = HEADERS, json=msg)
    # Result_id = response.json()
    # if (test_end_point_print):
    #     print(json.dumps(Result_id,indent=4))   
    return ''