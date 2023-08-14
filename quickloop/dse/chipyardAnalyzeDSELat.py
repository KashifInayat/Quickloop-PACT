from fast.envs import ChipyardFIRRTLDSE2Env, FIRRTLVerilogEmitter, ChipyardVivadoDefaultFlowEnv, ChipyardAnalyzeEnv
from time import time
from collections import namedtuple as tup
import os
import json
tmpDir = '/work/tayyeb/tmp'
a = f'''v{int(time() * 1000)}'''
a = 'v1679376933898'
Configuration = tup('Configuration', 'sbt chipyardDir package top config prefix targetDir firrtlPath')

c = Configuration('java -jar /work/tayyeb/nas/gitlocal/fresh/chipyard/generators/rocket-chip/sbt-launch.jar',
                  '/work/tayyeb/nas/gitlocal/fresh/chipyard',
                  'chipyard.fpga.singleEv7',
                  'SingleEv7FPGATestHarness',
                  'DSEConfig',
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


envanalyze = ChipyardAnalyzeEnv(ac)
envanalyze.reset()

result = {}

for i in range(1):
    dse = [i]
    action = list(dse)
    observation, reward, done, info = envanalyze.step(i)
    conf = {'config': dse}
    result[i] = {**info, **conf}
    print(result)

f = open('/work/tayyeb/results/new82.json', 'w')
json.dump(result, f, indent=6)
f.close()

