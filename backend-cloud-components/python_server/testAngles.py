import angles as ang
import numpy as np

# sampleData = np.array([[0.16395657, 0.28606546, 0.82149804],
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
#     [0.85447615, 0.7050154, 0.8829053]])
sampleData = np.array([[0.35705766, 0.44761577, 0.52933115],
   [0.33233482, 0.42783743, 0.5284759 ],
   [0.33952576, 0.47159505, 0.5872201 ],
   [0.35448307, 0.39105788, 0.5178075 ],
   [0.3675284,  0.49778795, 0.7116443 ],
   [0.454352,   0.32292667, 0.87250257],
   [0.45600364, 0.51089793, 0.8936839 ],
   [0.60244024, 0.32653075, 0.14651638],
   [0.64254993, 0.57177764, 0.6858432 ],
   [0.64466596, 0.46013296, 0.22642016],
   [0.611384,   0.6558212,  0.50936824],
   [0.7877892,  0.32278714, 0.48766384],
   [0.7967643,  0.4876842,  0.68871784],
   [0.5792592,  0.3230783,  0.5871292 ],
   [0.66361815, 0.6859678,  0.3076136 ],
   [0.71828,    0.4602053,  0.33975196],
   [0.7358789,  0.512898,   0.23153126]])

vec1 = ang.getVec(sampleData[6], sampleData[12])
vec2 = ang.getVec(sampleData[14], sampleData[12])

print(f"RIGHT side body-leg angle: {ang.findAngles(vec1, vec2)}")

vec3 = ang.getVec(sampleData[5], sampleData[11])
vec4 = ang.getVec(sampleData[13], sampleData[11])

print(f"LEFT side body-leg angle: {ang.findAngles(vec3, vec4)}")

# Test of removing z dimension - gives perspective of image using TA1 (z points out of screen)
# vec5 = ang.getVec(np.array([0.31279173, 0.33806083]), np.array([0.63192385, 0.32733732]))
# vec6 = ang.getVec(np.array([0.5122879,  0.34647343]), np.array([0.63192385, 0.32733732]))

# print(f"body-leg angle: {findAngles(vec5, vec6)}")

# seems like removing x in TA2 gives angle that means x is verical axis?? 
# makes sense because x increases as we go 0->16 so 0 must be head, and 1 must be foot or smt
# vec5 = ang.getVec(np.array([0.51089793, 0.8936839 ]), np.array([0.4876842,  0.68871784]))
# vec6 = ang.getVec(np.array([0.6859678,  0.3076136 ]), np.array([0.4876842,  0.68871784]))

# print(f"body-leg angle: {findAngles(vec5, vec6)}")

vec5 = ang.getVec(np.array([0.30770537, 0.21791404]), np.array([0.5569942, 0.42430973]))
vec6 = ang.getVec(np.array([0.56606257, 0.75119686]), np.array([0.5569942, 0.42430973]))

print(f"body-leg angle: {ang.findAngles(vec5, vec6)}")