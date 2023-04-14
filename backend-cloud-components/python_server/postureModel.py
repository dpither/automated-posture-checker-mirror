# https://www.ccohs.ca/oshanswers/ergonomics/sitting/sitting_position.html
# YES
# body-leg (hip angle) = 90-120
# thigh-lower leg (knee angle) = 90-130
# ground-lower leg (ankle angle) = 100-120
# body to vertical vector (trunk inclination) = 0-30
# knee joints at or below hips --> knee elevation <= hip elevation (given by x-coord)    x --> this is duplicate of hip angle?
# ankle in front of knee --> y or z coord is in front of knee                            x --> this is duplicate of knee angle?
# measure twist in body --> compare the RIGHT and LEFT hip angles 
# head aligned with spine --> angle between head and back somehow???                     
"https://upload.wikimedia.org/wikipedia/commons/f/f6/Female_Head_proportions.jpg"
"https://www.artyfactory.com/portraits/pencil-portraits/images/proportions_of_a_head_1.GIF"
# 5 units nose to center, 2.5 units or 3 units neck to nose --> atan(5/2.75) --> 61 deg --> 120 deg shoulder angle
# upper arm vs body angle (upper arm inclination) = 0-20
# upper arm vs lower arm (elbow angle) = 90-120 

'''
Decision tree model based on Canadian occupational health/safety data from above
'''

import angles as ang
import numpy as np

X_UNIT = np.array([-1,0,0])
Y_UNIT = np.array([0,1,0])
Z_UNIT = np.array([0,0,1])

X_INDEX = 0
Y_INDEX = 1
Z_INDEX = 2

# allow for +/- 10 degrees of tolerance
TOLERANCE = 10

class PostureModel:
    def __init__(self):
        self.ANGLE_DICT = {
            'hip':[[5,11,13],[90,120]],
            'knee':[[11,13,15],[90,130]],
            'ankle':[[13,15,"y_unit"],[100,120]],
            'trunk':[[5,11,"x_unit"],[0,30]],
            'upper-arm':[[11,5,7],[0,20]],
            'elbow':[[5,7,9],[90,120]],
            'neck':[[11,5,0], [110,130]]
        }

        self.POS_DICT = {
            'knee-hip':[[X_INDEX],[11,13]],
            'ankle-knee':[[Y_INDEX, Z_INDEX],[15,13]]
        }
        
    def checkPostureAnglesL(self, positions, output):

        for angle_name, angle_def in self.ANGLE_DICT.items():
            v1 = ang.getVec(positions[angle_def[0][0]], positions[angle_def[0][1]])
            if angle_def[0][2] == "y_unit":
                v2 = Y_UNIT
            elif angle_def[0][2] == "x_unit":
                v2 = X_UNIT
            else:                
                v2 = ang.getVec(positions[angle_def[0][2]], positions[angle_def[0][1]])

            angle = round(ang.findAngles(v1, v2))

            # if in good range, return TRUE
            # else reutrns FALSE
            output[angle_name+"L"] = [angle >= angle_def[1][0]-TOLERANCE and angle <= angle_def[1][1]+TOLERANCE, angle]

        return output
    
    def checkPostureAnglesR(self, positions, output):

        for angle_name, angle_def in self.ANGLE_DICT.items():
            v1 = ang.getVec(positions[angle_def[0][0]+1], positions[angle_def[0][1]+1])
            if angle_def[0][2] == "y_unit":
                v2 = Y_UNIT
            elif angle_def[0][2] == "x_unit":
                v2 = X_UNIT
            elif angle_name == "neck":
                v2 = ang.getVec(positions[angle_def[0][2]], positions[angle_def[0][1]+1])
            else:                
                v2 = ang.getVec(positions[angle_def[0][2]+1], positions[angle_def[0][1]+1])

            angle = round(ang.findAngles(v1, v2))

            # if in good range, return TRUE
            # else reutrns FALSE
            output[angle_name+"R"] = [angle >= angle_def[1][0]-TOLERANCE and angle <= angle_def[1][1]+TOLERANCE, angle]

        return output
    
    def checkTwist(self, output):
        difference = abs(output['hipL'][1] - output['hipR'][1])
        output["bodytwist"] = [difference <= 10+TOLERANCE, difference]

    def checkPositions(self, positions, output):
        for pos_name, pos_value in self.POS_DICT.items():
            isGood = False
            for i in pos_value[0]:
                if round(positions[pos_value[1][1]][i],1) - round(positions[pos_value[1][0]][i],1) < 0.3:
                    isGood = True
                    break

            output[pos_name+"L"] = [isGood, -1]

            isGood = False
            for i in pos_value[0]:
                if round(positions[pos_value[1][1]+1][i],1) - round(positions[pos_value[1][0]+1][i],1) < 0.3:
                    isGood = True
                    break
                    
            output[pos_name+"R"] = [isGood, -1]

        return output


    def checkPostureAngles(self, positions):
        result = {}
        self.checkPostureAnglesL(positions, result)
        self.checkPostureAnglesR(positions, result)
        self.checkTwist(result)
        self.checkPositions(positions, result)
        return result
    
    def generate_comments(self, results):
        comment = ""
        for name, value in results.items(): 
            if not value[0]:
                side_indicator = name[len(name)-1]
                if side_indicator == 'L':
                    comment += "LEFT "
                    comment += name[0:len(name)-1]
                elif side_indicator == 'R':
                    comment += "RIGHT "
                    comment += name[0:len(name)-1]
                else: 
                    comment += name[0:len(name)]
                
                comment += ', '

        # return comment with removed trailing comma and space
        return comment[0:len(comment)-2]