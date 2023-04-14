'''
Simple test script to run all models at once
'''

# 1. run pose detection
# 2. run posture model
# 3. run sitting model 

import postureModel as pm
import numpy as np
import poseDetection as pd
import sitModel as sm
import json

def isGoodPosture(postureMap):
    for item in postureMap:
        if postureMap[item][0]:
            continue
        else:
            return False
    return True

# im = pd.open_image('data\\data_standing\\images (12).jpg')
im = pd.open_image('data\\data_sitting\\download (3).jpg')
datapoints, encoded_image = pd.detect_pose_on_image(im)
# print(datapoints)
# print(encoded_image)

postureM = pm.PostureModel()
result = postureM.checkPostureAngles(datapoints)
comments = postureM.generate_comments(result)

isSitting = sm.isSitting(datapoints)
print(f"isSitting {isSitting}")


# result = postureM.checkPostureAngles(np.array([[0.35705766, 0.44761577, 0.52933115],
#    [0.33233482, 0.42783743, 0.5284759 ],
#    [0.33952576, 0.47159505, 0.5872201 ],
#    [0.35448307, 0.39105788, 0.5178075 ],
#    [0.3675284,  0.49778795, 0.7116443 ],
#    [0.454352,   0.32292667, 0.87250257],
#    [0.45600364, 0.51089793, 0.8936839 ],
#    [0.60244024, 0.32653075, 0.14651638],
#    [0.64254993, 0.57177764, 0.6858432 ],
#    [0.64466596, 0.46013296, 0.22642016],
#    [0.611384,   0.6558212,  0.50936824],
#    [0.7877892,  0.32278714, 0.48766384],
#    [0.7967643,  0.4876842,  0.68871784],
#    [0.5792592,  0.3230783,  0.5871292 ],
#    [0.66361815, 0.6859678,  0.3076136 ],
#    [0.71828,    0.4602053,  0.33975196],
#    [0.7358789,  0.512898,   0.23153126]]))

# result = postureM.checkPostureAngles(np.array([[0.16395657, 0.28606546, 0.82149804],
#     [0.14621207, 0.26040754, 0.8061753],
#     [0.14630474, 0.260463, 0.8589711],
#     [0.1734157, 0.2045058, 0.79887956],
#     [0.17482494, 0.2012673, 0.90541816],
#     [0.2857987, 0.2617854, 0.8854254],
#     [0.30770537, 0.21791404, 0.81031203],
#     [0.43743077, 0.41986546, 0.26449943],
#     [0.4583728, 0.4102358, 0.7611318],
#     [0.41908935, 0.43514636, 0.44507742],
#     [0.4205939, 0.44219393, 0.44885823],
#     [0.5271225, 0.43423957, 0.71017987],
#     [0.5569942, 0.42430973, 0.8145001],
#     [0.5377766, 0.7318596, 0.75493634],
#     [0.56606257, 0.75119686, 0.91232026],
#     [0.79885286, 0.67743355, 0.82671404],
#     [0.85447615, 0.7050154, 0.8829053]]))

print(result)

retVal = {
        "content":str(encoded_image.decode("utf-8")),
        "goodPosture":isGoodPosture(result),
        "comments":comments,
        "isSitting":isSitting
}

print(json.dumps(retVal))
