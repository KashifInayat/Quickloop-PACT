import re
from dateutil.parser import parse


def getTimingDataFromVivadoLog(filename):
    with open(filename, 'r') as f:
        text = f.readlines()
        f.close()

    for l in text:
        if 'Start of session at:' in l:
            starttime = re.search(r': (.*)', l).group(1)
        if 'synth_design: Time' in l:
            synth_time = re.search(r'elapsed = (\d\d:\d\d:\d\d)', l).group(1)
        if 'place_design: Time' in l:
            place_time = re.search(r'elapsed = (\d\d:\d\d:\d\d)', l).group(1)
        if 'route_design: Time' in l:
            route_time = re.search(r'elapsed = (\d\d:\d\d:\d\d)', l).group(1)
        if 'Exiting Vivado at' in l:
            endtime = re.search(r'at (.*)\.\.\.', l).group(1)

    total_time = (parse(endtime) - parse(starttime)).seconds
    data = [d.split(':') for d in [synth_time, place_time, route_time]]
    x = []
    for d in data:
        x.append(sum([int(i) * m for i, m in zip(d, [3600, 60, 1])]))
    other = total_time - sum(x)
    return x + [other]

def getTimingDataFromVivadoLogs(topDir, expList):
    result = []
    for e in expList:
        filename = f'{topDir}/{e}/vivado.log'
        result.append(getTimingDataFromVivadoLog(filename))
    return result

