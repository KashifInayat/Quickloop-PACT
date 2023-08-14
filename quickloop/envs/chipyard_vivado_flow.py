import gym
from gym.spaces import Discrete, Box
import numpy as np
import os
from subprocess import run
from time import time


class ChipyardVivadoFlowEnv(gym.Env):
    def build_chipyard(self, action):
        targetDir = self.config.targetDir + ('/current' if action == (1 << 31 -1) else f'/{action}')
        #os.makedirs(targetDir, exist_ok=True)
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
        scriptDir = self.config.chipyardDir + '/fpga/fpga-shells/xilinx/common/tcl/vivado.tcl'

        init_time = time()
        commandLine = ('/usr/local/packages/Xilinx/Vivado/2021.2/bin/vivado -nojournal -mode batch '
                             f'-source {scriptDir} -tclargs -top-module SingleEv7FPGATestHarness -board singleEv7 '
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
        super(ChipyardVivadoFlowEnv, self).__init__()
        # Define action and observation space
        self.action_space = Discrete(1 << 31 - 1)
        self.observation_space = Box(low=0, high=1.0, shape=(1,), dtype=np.float64)
        self.config = config

    def step(self, action):
        done, elapsed_time = self.build_chipyard(action)
        observation = np.array([0.5])
        reward = 0
        info = {"elapsed_time": elapsed_time}
        return observation, reward, done, info

    def reset(self):
        return np.array([0])
