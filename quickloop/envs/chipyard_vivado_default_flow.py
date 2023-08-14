import gym
from gym.spaces import Discrete, Box
import numpy as np
import os
from subprocess import run
from time import time


def wrapInStopwatch(text):
    return f'''
set t1 [clock seconds]
{text}
set t2 [clock seconds]
puts [format "Steptime: %d" [expr {{$t2 - $t1}}]]
'''

import re
def wrapStopwatch(text, commandList):
    outText = text + ''
    for c in commandList:
        if c in text:
            x = re.search(f'{c} .*', text).group()
            y= wrapInStopwatch(x)
            outText = outText.replace(x, y)
    return outText

def addSynthCheckpoint(text, targetDir, prev):
    chkpt = f'read_checkpoint -incremental {targetDir}/{prev}/obj/post_synth.dcp'
    x = re.search('synth_design .*', text).group()
    y = f'''
{chkpt}
{x}    
'''
    return text.replace(x, y)

def addImplCheckpoint(text, targetDir, prev):
    chkpt = f'read_checkpoint -incremental {targetDir}/{prev}/obj/post_route.dcp'
    x = re.search('place_design .*', text).group()
    y = f'''
{chkpt}
{x}    
'''
    return text.replace(x, y)


def addIncCheckpoint(text, targetDir, prev):
    src = text
    if prev > -1:
        src = addSynthCheckpoint(src, targetDir, prev)
        src = addImplCheckpoint(src, targetDir, prev)
    return src


class ChipyardVivadoDefaultFlowEnv(gym.Env):

    def vivadoFlow(self, targetDir, tclscript):
        rtlDir = f'{targetDir}/rtl/'
        rtl_list = '\n'.join([rtlDir + i for i in os.listdir(rtlDir)])
        ip_list = ' '.join([targetDir + '/' + i for i in os.listdir(targetDir) if 'vivado.tcl' in i])
        with open(targetDir + '/vsrcs.txt', 'w') as f:
            f.write(rtl_list)
            f.write('\n')
            f.write(self.config.chipyardDir + '/fpga/fpga-shells/xilinx/singleEv7/vsrc/singleEv7reset.v')
            f.write('\n')
            f.write(self.config.chipyardDir + '/generators/rocket-chip/src/main/resources/vsrc/EICG_wrapper.v')
            f.close()

        buid_env = os.environ.copy()
        buid_env['XILINX_VIVADO'] ='/usr/local/packages/Xilinx/Vivado/2021.2'

        init_time = time()
        commandLine = ('/usr/local/packages/Xilinx/Vivado/2021.2/bin/vivado -nojournal -mode batch '
                             f'-source {tclscript} -tclargs -top-module SingleEv7FPGATestHarness -board singleEv7 '
                             f'-F {targetDir}/vsrcs.txt -ip-vivado-tcls "{ip_list}"')

        build_process = run(commandLine,
                            shell = True, capture_output=True, text=True, cwd = targetDir, env = buid_env)
        elapsed_time = time() - init_time
        print('Finished Vivado ...')
        print(f'Time elapsed: {elapsed_time}')

        with open(f'{targetDir}/{self.config.prefix}.vivado.stdout.log', 'w') as f:
            f.write(build_process.stdout)
            f.close()

        with open(f'{targetDir}/{self.config.prefix}.vivado.stderr.log', 'w') as f:
            f.write(build_process.stderr)
            f.close()

        failed = '[error]' in build_process.stdout

        return failed, elapsed_time

    def __init__(self, config):
        super(ChipyardVivadoDefaultFlowEnv, self).__init__()
        # Define action and observation space
        self.action_space = Discrete(1 << 31 -1)
        self.observation_space = Box(low=0, high=1.0, shape=(1,), dtype=np.float64)

        self.config = config
        self.prev = -1



    def incFlow(self, action):
        targetDir = self.config.targetDir + f'/{action}'
        rtlDir = f'{targetDir}/rtl'


        vivadoScriptDir = f'{self.config.chipyardDir}/fpga/fpga-shells/xilinx/common/tcl'
        vivadoScript = 'set scriptdir [file dirname [info script]]\n'
        with open(f'{vivadoScriptDir}/prologue.tcl') as f:
            vivadoScript = vivadoScript + ''.join([l for l in f.readlines()])
            f.close()
        with open(f'{vivadoScriptDir}/util.tcl') as f:
            vivadoScript = vivadoScript + ''.join([l for l in f.readlines()])
            f.close()
        with open(f'{vivadoScriptDir}/init.tcl') as f:
            vivadoScript = vivadoScript + ''.join([l for l in f.readlines()])
            f.close()
        with open(f'{vivadoScriptDir}/synth.tcl') as f:
            vivadoScript = vivadoScript + ''.join([l for l in f.readlines()])
            f.close()
        with open(f'{vivadoScriptDir}/place.tcl') as f:
            vivadoScript = vivadoScript + ''.join([l for l in f.readlines()])
            f.close()
        with open(f'{vivadoScriptDir}/route.tcl') as f:
            vivadoScript = vivadoScript + ''.join([l for l in f.readlines()])
            f.close()
        with open(f'{vivadoScriptDir}/bitstream.tcl') as f:
            vivadoScript = vivadoScript + ''.join([l for l in f.readlines()])
            f.close()
        with open(f'{vivadoScriptDir}/report.tcl') as f:
            vivadoScript = vivadoScript + '\n'.join([l for l in f.readlines()])
            f.close()


        #add incremental checkpoints
        #vivadoScript = addIncCheckpoint(vivadoScript, self.config.targetDir, self.prev)

        self.prev = action

        #vivadoScript = wrapStopwatch(vivadoScript, ['synth_design', 'place_design', 'route_design'])

        with open(f'{targetDir}/vivadoScript.tcl', 'w') as f:
            f.write(vivadoScript)
            f.close()
        #return False, 0
        return self.vivadoFlow(targetDir, f'{targetDir}/vivadoScript.tcl')

    def step(self, action):
        done, elapsed_time = self.incFlow(action)
        observation = np.array([0.5])
        reward = 0
        info = {"elapsed_time": elapsed_time}

        return observation, reward, done, info

    def reset(self):
        self.prev = -1
        return np.array([0])
