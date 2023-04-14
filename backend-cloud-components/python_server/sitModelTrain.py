'''
THIS FILE IS FOR TRAINING/TEST PURPOSES - NOT USED IN FINAL PRODUCTION

TRAINING RESULTS:
Baseline model to beat was the simple decision tree model 
Other simple binary classifiers tried did not perform as well, even with hyperparameters tuned. 
It seems like a polynomial support vector machine did pretty well, but at the same time was pretty inconsistent with different random seeds...

For factors such as both reliability and performance on the server, we chose the simpler decision tree model. 

Limitations included not having much data, but I tried to overcome this with data augmentation.
'''


import poseDetection as pd
import numpy as np
from sklearn.linear_model import SGDClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.svm import SVC
from sklearn.svm import LinearSVC
from sklearn.ensemble import RandomForestClassifier
from sklearn.neighbors import KNeighborsClassifier
from sklearn.neural_network import MLPClassifier
from sklearn import metrics
from sklearn.utils import shuffle
import angles as ang

SITTING_PATH = "data\\data_sitting"
STANDING_PATH = "data\\data_standing"

def extract_posture_data_from_images():
    directory, filenames = pd.load_images_from_path(SITTING_PATH)
    _, sitting_keypoints = pd.detect_pose_on_images(directory, filenames)
    _, sitting_keypoints_flip = pd.detect_pose_on_images(directory, filenames, flip=True) #run second time with image flip data augmentation
    _, sitting_keypoints_rot = pd.detect_pose_on_images(directory, filenames, rotate=30) #run second time with image rotate data augmentation

    directory, filenames = pd.load_images_from_path(STANDING_PATH)
    _, standing_keypoints = pd.detect_pose_on_images(directory, filenames)
    _, standing_keypoints_flip = pd.detect_pose_on_images(directory, filenames, flip=True) #run second time with image flip data augmentation
    _, standing_keypoints_rot = pd.detect_pose_on_images(directory, filenames, rotate=30) #run second time with image rotate data augmentation

    return np.concatenate((sitting_keypoints, sitting_keypoints_flip, sitting_keypoints_rot)), np.concatenate((standing_keypoints, standing_keypoints_flip, standing_keypoints_rot))


def transform_data(sitting_data, standing_data):
    X_train = []
    y_train = []

    for sit_dp in sitting_data:
        X_train.append(sit_dp.flatten())
        y_train.append("sit")

    for stand_dp in standing_data:
        X_train.append(stand_dp.flatten())
        y_train.append("stand")

    return np.array(X_train), np.array(y_train)


def train():
    sitting_data, standing_data = extract_posture_data_from_images()
    X_data, y_data = transform_data(sitting_data, standing_data)

    #shuffle data
    X_data, y_data = shuffle(X_data, y_data, random_state=0)

    X_train = np.concatenate((np.array(X_data[0:40]), np.array(X_data[50:92])))
    y_train = np.concatenate((np.array(y_data[0:40]), np.array(y_data[50:92])))

    X_validate = np.concatenate((np.array(X_data[40:50]), np.array(X_data[92:102])))
    y_validate = np.concatenate((np.array(y_data[40:50]), np.array(y_data[92:102])))

    '''
    Want models that perform well with little data
    So, choosing simpler models are better
    Best performance found was SVC with polynomial kernel, but it was inconsistent.
    Better simpler model was manual decision tree
    '''

    # classifier = LogisticRegression(penalty='l1', solver='saga')
    #LogisticRegression --> 0.9 image(9) sitting--> train + validate = 82%
    #rand is validate: 0.7 train: 0.8
    #l1 liblinear (more resilient to outliers): validate 0.6, train 0.7
    #l1 saga (more resilient to outliers): validate 0.5, train 0.74

    # classifier = RandomForestClassifier()
    #RandomForest --> 0.9 image(9) sitting --> train + validate = 98,96%
    #rand is validate: 0.6 train: 0.92

    # classifier = KNeighborsClassifier()
    #KNN --> 0.9 image(8) sitting --> train + validate = 82%
    #rand is validate: 0.7 train: 0.86

    # classifier = SVC(kernel="poly")
    #validate: 0.7, train: 0.86
    #poly kernel validate: 0.9, train: 0.98
    #poly kernel validate: 0.8, train: 0.96, seed 100
    #poly kernel validate: 0.7, train: 0.94, seed 88

    # classifier = LinearSVC()
    #validate: 0.8, train: 0.9

    classifier = SGDClassifier(max_iter=10)
    #validate: 0.8, train: 0.76
    #validate: 0.7, train: 0.72, max iter = 10 (increased because train was lower than validate error...)

    classifier.fit(X_train, y_train)

    y_pred = classifier.predict(X_validate)
    print(metrics.accuracy_score(y_validate, y_pred))
    print(y_pred)
    print(y_validate)
    print("train")
    y_pred = classifier.predict(X_data)
    print(metrics.accuracy_score(y_data, y_pred))
    print(y_pred)
    print(y_data)

def train_angle():
    sitting_data, standing_data = extract_posture_data_from_images()
    count_stand = 0
    count_sit = 0
    for sit_dp in sitting_data:
        v1 = ang.getVec(sit_dp[5], sit_dp[11])
        v2 = ang.getVec(sit_dp[13], sit_dp[11])
        angle = ang.findAngles2D(v1, v2)
        if abs(angle) > 160:
            count_stand += 1
        else:
            count_sit += 1
    print(f"sit {count_sit}, stand {count_stand} of {count_sit + count_stand}")
    count_stand = 0
    count_sit = 0
    count = 0
    for stand_dp in standing_data:
        v1 = ang.getVec(stand_dp[5], stand_dp[11])
        v2 = ang.getVec(stand_dp[13], stand_dp[11])
        angle = ang.findAngles2D(v1, v2)
        if abs(angle) > 160:
            count_stand += 1
        else :
            count_sit += 1
            print(angle)
            print(count)
        count += 1
    print(f"sit {count_sit}, stand {count_stand} of {count_sit + count_stand}")
    #100% sitting
    #8sitting, 17 standing for standing error 8/25 (32%)
    #total 84% acc

def isSitting(datapoints):
    v1 = ang.getVec(datapoints[5], datapoints[11])
    v2 = ang.getVec(datapoints[13], datapoints[11])
    angle = ang.findAngles2D(v1, v2)
    if abs(angle) > 160:
        return False
    else :
        return True

train()