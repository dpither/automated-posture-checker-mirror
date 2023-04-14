import angles as ang

def isSitting(datapoints):
    v1 = ang.getVec(datapoints[5], datapoints[11])
    v2 = ang.getVec(datapoints[13], datapoints[11])
    angle = ang.findAngles2D(v1, v2)
    if abs(angle) > 160:
        return False
    else :
        return True