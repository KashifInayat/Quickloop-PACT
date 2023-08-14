from fast.envs import ChipyardFIRRTLDSEEnv, FIRRTLVerilogEmitter, ChipyardVivadoIncFlowEnv, ChipyardAnalyzeEnv
from time import time
from collections import namedtuple as tup
import os

tmpDir = '/work/tayyeb/tmp'
Configuration = tup('Configuration', 'chipyardDir package top prefix targetDir firrtlPath')

AConfig = tup('AConfig', 'targetDir riscv_path vivado_path edkDir openocd serial prefix')

a = f'v{int(time() * 1000)}'
expDir = f'{tmpDir}/{a}'

ac = AConfig(expDir,
            '/work/tayyeb/sifive/toolchain',
            '/usr/local/packages/Xilinx/Vivado/2021.2',
            '/work/tayyeb/nas/gitlocal/freedom-e-sdk',
            '/work/tayyeb/nas/openocd/bin/openocd',
            '/dev/ttyUSB1',
            'gen')


c = Configuration('/work/tayyeb/nas/gitlocal/fresh/chipyard',
                  'chipyard.fpga.singleEv7',
                  'SingleEv7FPGATestHarness',
                  'gen',
                  expDir,
                  '/work/tayyeb/nas/gitlocal/fresh/firrtl/utils/bin/firrtl')

envf = ChipyardFIRRTLDSEEnv(c)
ve = FIRRTLVerilogEmitter(c)
envflow = ChipyardVivadoIncFlowEnv(c)
envanalyze = ChipyardAnalyzeEnv(ac)
envf.reset()
envflow.reset()
envanalyze.reset()

dselist = [(0, 1, 2, 0), (0, 1, 0, 0), (0, 1, 4, 0), (0, 0, 2, 0), (0, 2, 2, 0)]

for i, dse in enumerate(dselist):
    action = list(dse)
    _, _, done, _ = envf.step(action)
    if done: continue
    os.rename(f'{expDir}/current', f'{expDir}/{i}')
    ve.perform(f'{expDir}/{i}')
    envflow.step(i)
    _, _, _, info = envanalyze.step(i)
    print(info)

