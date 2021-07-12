from hashlib import new
from django.http import HttpResponseRedirect, HttpResponse, HttpResponseBadRequest
from django.shortcuts import render
from django.http import JsonResponse
from .forms import UploadFileForm
from django.views.decorators.csrf import csrf_exempt
import os
import json
#импорт сети
import shutil
import os
import cv2
import easyocr
from pyzbar.pyzbar import decode
import matplotlib.pyplot as plt
import zipfile
import datetime
import string
import glob
import math
import random
import tqdm
import matplotlib.pyplot as plt
import tensorflow as tf
import sklearn.model_selection
import keras_ocr
from IPython.display import clear_output 

@csrf_exempt 
def upload_file(request):
    #try:
        up_file = request.FILES['picture']
        path = os.path.dirname(os.path.dirname(os.path.abspath(__file__))) + '/content/' + up_file.name
        if os.path.exists(path):
            os.remove(path)
        destination = open(os.path.dirname(os.path.dirname(os.path.abspath(__file__))) + '/content/' + up_file.name, 'xb+')
        for chunk in up_file.chunks():
            destination.write(chunk)
        destination.close()
        #os.system("python3 /home/sergey/GPO/for_django_3-0/content/YOLO_EasyOCR.py")
        
        #result = {'success': True, 'description': 'test', 'price': 12}
        result = new_fun(path, up_file.name)
        return JsonResponse(result)
    #except Exception:
    #    return HttpResponseBadRequest()
    
def new_fun(path, filename):
    #file = open(path)
    reader = easyocr.Reader(['ru','en']) # need to run only once to load model into memory
    # демонстрация работы
    sdescriptions = []
    sbarcodes = []
    sprice11 = []
    sprice12 = []
    sprice21 = []
    sprice22 = []
    #os.system("cd /home/sergey/GPO/for_django_3-0/")
    os.system("/home/sergey/GPO/for_django_3-0/content/darknet/darknet detector test /home/sergey/GPO/for_django_3-0/content/data/obj.data /home/sergey/GPO/for_django_3-0/content/data/yolov4-tiny-3l.cfg /home/sergey/GPO/for_django_3-0/content/data/backup/yolov4-tiny-3l_fine_tuned.weights " + '"' + path + '"' + " -dont_show -ext_output | tee pred.txt")
    #print(path)
    #print(filename)
    #print("./content/darknet/darknet detector test ./content/data/obj.data ./content/data/yolov4-tiny-3l.cfg ./content/data/backup/yolov4-tiny-3l_fine_tuned.weights " + '"' + path + '"' + " -dont_show -ext_output | tee pred.txt")
    #filename = "photo(6).jpg"
    a_file = open("pred.txt", "r")
    lines = a_file.readlines()
    a_file.close()
    last_lines = [line for line in lines if ('width' in line and 'height' in line and 'left_x' in line)]
    #clear_output()
    img = cv2.imread("./content/"+filename)
    #tuple(img.shape[1::-1])
    res = []
    for line in last_lines:
        spl = line.split()
        cords = {"class":spl[0][:-1], "conf_value":spl[1][:-1], "left_x":int(spl[3]), "top_y":int(spl[5]), 'width':int(spl[7]), 'height':int(spl[9][:-1])}
        res.append(cords)
    for box in res:
        x = box['left_x']
        y = box['top_y']
        w = box['width']
        h = box['height']
        if box['class'] == 'description': 
            x = 0
            w = img.shape[1]
            #w = 511
        if box['class'] == 'barcode':
            x = x - 15
            w = w + 30
        if (box['class'] == 'price11'):
            x = x - 5
            w = w + 10
            h = h + 10
            y = y - 5
        if (box['class'] == 'price12'):
            x = x - 7
            w = w + 12
            h = h + 10
            y = y - 5
        if (box['class'] == 'price21'):
            x = x - 2
            w = w + 4
            h = h + 4
            y = y - 2
        if (box['class'] == 'price22'):
            x = x - 3
            w = w + 6
            h = h + 6
            y = y - 3
        if (x < 0): x=0
        if (y < 0): y=0
        if (w < 0): w=0
        if (h < 0): h=0
        if (x > img.shape[1]): x=img.shape[1]
        if (y > img.shape[0]): y=img.shape[0]
        if (w > img.shape[1]): w=img.shape[1]
        if (h > img.shape[0]): h=img.shape[0]
        y2 = y+h
        x2 = x+w
        if (y2 > img.shape[0]): y2 =img.shape[0]
        if (x2 > img.shape[1]): x2 =img.shape[1]
        if (y > y2): y = y2
        if (x > x2): x = x2
        crop_img = img[y:y+h, x:x+w]
        apps = {
                'description':sdescriptions.append,
                'barcode':sbarcodes.append,
                'price11':sprice11.append,
                'price12':sprice12.append,
                'price21':sprice21.append,
                'price22':sprice22.append
                }
        apps[box['class']](crop_img)
    os.remove("pred.txt")
    f = open(filename[:-4]+".txt", "w")
    for line in last_lines:
        f.write(line)
    f.close()
    os.remove("./predictions.jpg")
    os.remove("./"+filename[:-4]+".txt")
    
    restxt = open('./content/res.txt', 'w')
    text = ""
    for img in sdescriptions:
        result = reader.readtext(img)
        
        for box in result:
            text = text + " " + box[1]
        
        #restxt.write(text + '\n')
        #print(text)
    description_test = text
    text = ""
    for img in sprice11:
        #print(reader.readtext(img, allowlist='1234567890', detail = 0))
        #restxt.write(str(reader.readtext(img, allowlist='1234567890', detail = 0))+ '\n')
        result = reader.readtext(img, allowlist='1234567890')
        for box in result:
            text = text + " " + box[1]
        restxt.write(text + '\n')
    price11_test = text
    text = ""
    for img in sprice12:
        #print(reader.readtext(img, allowlist='1234567890', detail = 0))
        #restxt.write(reader.readtext(img, allowlist='1234567890', detail = 0)+ '\n')
        result = reader.readtext(img, allowlist='1234567890')
        for box in result:
            text = text + " " + box[1]
        restxt.write(text + '\n')
    price12_test = text
    text = ""
    for img in sprice22:
        #print(reader.readtext(img, allowlist='1234567890', detail = 0))
        #restxt.write(reader.readtext(img, allowlist='1234567890', detail = 0)+ '\n')
        result = reader.readtext(img, allowlist='1234567890')
        for box in result:
            text = text + " " + box[1]
        restxt.write(text + '\n')
    price21_test = text
    text = ""
    for img in sprice21:
        #print(reader.readtext(img, allowlist='1234567890', detail = 0))
        #restxt.write(reader.readtext(img, allowlist='1234567890', detail = 0)+ '\n')
        result = reader.readtext(img, allowlist='1234567890')
        for box in result:
            text = text + " " + box[1]
        restxt.write(text + '\n')
    price22_test = text
    data = ""
    for img in sbarcodes:
        decoded_objects = decode(img)
        for obj in decoded_objects:
            # draw the barcode
        #
            #restxt.write("Type:" + obj.type + '\n')
            #restxt.write("Data:" + obj.data + '\n')
            #restxt.write("test")
            data = str(obj.data)
            #type = obj.type
        #print("Type:", obj.type)
        #print("Data:", obj.data)
        
    restxt.close()
    result = {'success': True, 'description': description_test, 'price11': price11_test, 'price12': price12_test, 'price21': price21_test, 'price22': price22_test, 'barcode_data': data }
    os.system("rm " + path)
    print(result)
    return result
