from fast.envs import ChipyardFIRRTLDSE2Env, FIRRTLVerilogEmitter, ChipyardVivadoDiffFlowEnv, ChipyardAnalyzeEnv
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

envf = ChipyardFIRRTLDSE2Env(c)
ve = FIRRTLVerilogEmitter(c)
envflow = ChipyardVivadoDiffFlowEnv(c)
envanalyze = ChipyardAnalyzeEnv(ac)
envf.reset()
envflow.reset()
envanalyze.reset()

result = {}

dselist = [(0, 0, 0), (0, 1, 2), (2, 1, 2), (2, 2, 4), (1, 2, 4)]


for i, dse in enumerate(dselist):
    action = list(dse)
    observation, reward, done, info = envf.step(action)
    if done: continue
    os.rename(f'{tmpDir}/{a}/current', f'{tmpDir}/{a}/{i}')
    ve.perform(f'{tmpDir}/{a}/{i}')
    envflow.step(i)
    observation, reward, done, info = envanalyze.step(i)
    print(info)
    result[i] = info

