'''
Angle calculation based on points in space
'''

import math
import numpy as np

def vecLength(v):
    squaredSum = 0

    for x in v:
        squaredSum += square(x)

    return math.sqrt(squaredSum)

def square(n):
    return n**2

def findAngles(vec1, vec2):
    #TODO: figure out if i should actually be ignoring the z-component
    vec1 = vec1[0:2]
    vec2 = vec2[0:2]
    dp = vec1 @ vec2
    normalized = dp / (vecLength(vec1) * vecLength(vec2))

    return radToDeg(math.acos(normalized))

def findAngles2D(vec1, vec2):
    #TODO: figure out if i should actually be ignoring the z-component
    vec1 = vec1[0:2]
    vec2 = vec2[0:2]
    dp = vec1 @ vec2
    normalized = dp / (vecLength(vec1) * vecLength(vec2))

    return radToDeg(math.acos(normalized))

def radToDeg(rad):
    return rad * 180 / math.pi

def getVec(p1, p2):
    return p1-p2

KEYPOINT_DICT = {
    'nose': 0,
    'left_eye': 1,
    'right_eye': 2,
    'left_ear': 3,
    'right_ear': 4,
    'left_shoulder': 5,
    'right_shoulder': 6,
    'left_elbow': 7,
    'right_elbow': 8,
    'left_wrist': 9,
    'right_wrist': 10,
    'left_hip': 11,
    'right_hip': 12,
    'left_knee': 13,
    'right_knee': 14,
    'left_ankle': 15,
    'right_ankle': 16
}