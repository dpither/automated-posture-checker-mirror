import postureModel as pm
import numpy as np
import poseDetection as pd

posm = pm.PostureModel()

result = {'hipL': [True, 116], 'kneeL': [True, 107], 'ankleL': [True, 105], 'trunkL': [True, 24], 'upper-armL': [True, 0], 'elbowL': [True, 96], 'neckL': [True, 110], 'hipR': [True, 110], 'kneeR': [True, 100], 'ankleR': [True, 101], 'trunkR': [True, 22], 'upper-armR': [True, 0], 'elbowR': [True, 87], 'neckR': [True, 118], 'bodytwist': [True, 6], 'knee-hip-L': [True, -1], 'knee-hip-R': [True, -1], 'ankle-knee-L': [True, -1], 'ankle-knee-R': [True, -1]}

print(pd.retreive_angles(posm, result))
