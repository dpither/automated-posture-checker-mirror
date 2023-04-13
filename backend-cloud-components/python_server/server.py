from flask import Flask, jsonify, request
import postureModel as pm
import numpy as np
import poseDetection as pd
import sitModelTrain as sm
import json
# import base64
import sys

app = Flask(__name__)

@app.route('/')
def test():
    return "Test"

@app.post('/model')
def server():
    
    encoded_string = request.json['content']
    print(encoded_string)


# with open("C:\\Users\\jdara\\Documents\\UBC\\Year3\\Term2\\CPEN391\\M3\\M3-Integration\\391-M3\\data\\data_sitting\\download (4).jpg", "rb") as image:
#         encoded_string = base64.urlsafe_b64encode(image.read())

# with open("C:\\Users\\jdara\\Documents\\UBC\\Year3\\Term2\\CPEN391\\M3\\M3-Integration\\391-M3\\data\\data_sitting\\download (4).jpg", "rb") as image:
#         encoded_string = base64.b64encode(image.read())

    #encoded_string = str(encoded_string.decode("utf-8"))
    encoded_string = encoded_string.replace('+', '-')
    encoded_string = encoded_string.replace('/', '_')

    im = pd.decode_image_from_base64(encoded_string)

    # im = pd.open_image('data\\data_sitting\\download (4).jpg')
    datapoints, encoded_image = pd.detect_pose_on_image(im)
    # print(datapoints)
    # print(encoded_image)

    postureM = pm.PostureModel()
    result = postureM.checkPostureAngles(datapoints)
    comments = postureM.generate_comments(result)

    isSitting = sm.isSitting(datapoints)
    # print(f"isSitting {isSitting}")

    # print(result)

    retVal = {
            "content":str(encoded_image.decode("utf-8")),
            "goodPosture":isGoodPosture(result),
            "isSitting":isSitting,
            "comments":comments
    }

    return json.dumps(retVal)


def isGoodPosture(postureMap):
    for item in postureMap:
        if postureMap[item][0]:
            continue
        else:
            return False
    return True