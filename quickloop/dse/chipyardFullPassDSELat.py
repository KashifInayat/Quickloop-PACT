from fast.envs import ChipyardFIRRTLDSELatEnv, FIRRTLVerilogEmitter, ChipyardVivadoDefaultFlowEnv, ChipyardAnalyzeEnv
from time import time
from collections import namedtuple as tup
import os
import json
tmpDir = '/work/tayyeb/tmp'
a = f'''v{int(time() * 1000)}'''

Configuration = tup('Configuration', 'sbt chipyardDir package top config prefix targetDir firrtlPath parameters ranges')

c = Configuration('java -jar /work/tayyeb/nas/gitlocal/fresh/chipyard/generators/rocket-chip/sbt-launch.jar',
                  '/work/tayyeb/nas/gitlocal/fresh/chipyard',
                  'chipyard.fpga.singleEv7',
                  'SingleEv7FPGATestHarness',
                  'DSEConfig',
                  'gen',
                  f'{tmpDir}/{a}',
                  '/work/tayyeb/nas/gitlocal/fresh/firrtl/utils/bin/firrtl',
                  ['meshRC', 'tileRC'],
                  [3, 2])

AConfig = tup('AConfig', 'targetDir riscv_path vivado_path edkDir openocd serial prefix')
ac = AConfig(f'{tmpDir}/{a}',
            '/work/tayyeb/sifive/toolchain',
            '/usr/local/packages/Xilinx/Vivado/2021.2',
            '/work/tayyeb/nas/gitlocal/freedom-e-sdk',
            '/work/tayyeb/nas/openocd/bin/openocd',
            '/dev/ttyUSB1',
            'gen')

envf = ChipyardFIRRTLDSELatEnv(c)
ve = FIRRTLVerilogEmitter(c)
envflow = ChipyardVivadoDefaultFlowEnv(c)
envanalyze = ChipyardAnalyzeEnv(ac)
envf.reset()
envflow.reset()
envanalyze.reset()

result = {}

#dselist = [(m, ac, spad, l) for m in [0, 1, 2] for ac in range(4) for spad in range(3) for l in range(3)]

#dselist = [[2, 8 << i] for i in range(6)]
dselist = [[1, 1]]

for i, dse in enumerate(dselist):
    action = list(dse)
    observation, reward, done, info = envf.step(action)
    if done: continue
    os.rename(f'{tmpDir}/{a}/current', f'{tmpDir}/{a}/{i}')
    ve.perform(f'{tmpDir}/{a}/{i}')
    envflow.step(i)

    #observation, reward, done, info = envanalyze.step(i)
    #conf = {'config': dse}
    #result[i] = {**info, **conf}
    #print(result)

#f = open('/work/tayyeb/results/latpass.json', 'w')
#json.dump(result, f, indent=6)
#f.close()

