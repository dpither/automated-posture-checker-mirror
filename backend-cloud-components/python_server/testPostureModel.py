import postureModel as pm

test_input = {'hipL': [True, 116], 'kneeL': [True, 107], 'ankleL': [True, 105], 'trunkL': [True, 24], 'upper-armL': [True, 0], 'elbowL': [True, 96], 'neckL': [True, 110], 'hipR': [True, 110], 'kneeR': [True, 100], 'ankleR': [True, 101], 'trunkR': [True, 22], 'upper-armR': [True, 0], 'elbowR': [True, 87], 'neckR': [True, 118], 'bodytwist': [True, 6], 'knee-hipL': [True, -1], 'knee-hipR': [True, -1], 'ankle-kneeL': [True, -1], 'ankle-kneeR': [True, -1]}

def test_generate_comments():
    postureM = pm.PostureModel()
    result = postureM.generate_comments(test_input)
    assert result == ""

    test_input['hipL'] = [False, 80]
    result = postureM.generate_comments(test_input)
    assert result == "LEFT hip"
    

    test_input['bodytwist'] = [False, 20]
    result = postureM.generate_comments(test_input)
    assert result == "LEFT hip, bodytwist"

    test_input['ankle-kneeR'] = [False, -1]
    result = postureM.generate_comments(test_input)
    assert result == "LEFT hip, bodytwist, RIGHT ankle-knee"

    print("success")

test_generate_comments()
