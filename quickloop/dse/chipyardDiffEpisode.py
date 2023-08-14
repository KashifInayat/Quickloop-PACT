from fast.envs import ChipyardFIRRTLDSEEnv, FIRRTLVerilogEmitter, ChipyardVivadoDiffFlowEnv, ChipyardAnalyzeEnv
from time import time
from collections import namedtuple as tup
import os
import json
tmpDir = '/work/tayyeb/tmp'
a = f'''v{int(time() * 1000)}'''

Configuration = tup('Configuration', 'sbt chipyardDir package top config prefix targetDir firrtlPath')

c = Configuration('java -jar /work/tayyeb/nas/gitlocal/fresh/chipyard/generators/rocket-chip/sbt-launch.jar',
                  '/work/tayyeb/nas/gitlocal/fresh/chipyard',
                  'chipyard.fpga.singleEv7',
                  'SingleEv7FPGATestHarness',
                  'ModifyMeshAccSpadConfig',
                  'gen',
                  f'{tmpDir}/{a}',
                  '/work/tayyeb/nas/gitlocal/fresh/firrtl/utils/bin/firrtl')

AConfig = tup('AConfig', 'targetDir riscv_path vivado_path edkDir openocd serial prefix')
ac = AConfig(f'{tmpDir}/{a}',
            '/work/tayyeb/sifive/toolchain',
            '/usr/local/packages/Xilinx/Vivado/2021.2',
            '/work/tayyeb/nas/gitlocal/freedom-e-sdk',
            '/work/tayyeb/nas/openocd/bin/openocd',
            '/dev/ttyUSB1',
            'gen')

print(f'{tmpDir}/{a}')
envf = ChipyardFIRRTLDSEEnv(c)
ve = FIRRTLVerilogEmitter(c)
envflow = ChipyardVivadoDiffFlowEnv(c)
envf.reset()
envflow.reset()

result = {}
dselist = []
for meshRC in range(3):
    for acc in range(4):
        for sp in range(2):
            dselist.append([0, meshRC, acc, sp])
import random
random.shuffle(dselist)


for i, dse in enumerate(dselist):
    action = list(dse)
    _, _, done, _ = envf.step(action)
    if done: continue
    os.rename(f'{tmpDir}/{a}/current', f'{tmpDir}/{a}/{i}')
    ve.perform(f'{tmpDir}/{a}/{i}')
    _, _, _, info = envflow.step(i)
    conf = {'config': dse}
    result[i] = {**info, **conf}
    print(result)

f = open('/work/tayyeb/results/diffepisode.json', 'w')
json.dump(result, f, indent=6)
f.close()
