import gym
from gym.spaces import MultiDiscrete, Box
import numpy as np
from collections import namedtuple as tup
import os
from subprocess import run
from time import time
from anytree import Node, RenderTree
import os.path
from collections import defaultdict

class ChipyardVivadoDFFlowEnv1(gym.Env):

    def getsubmodules(self, moduleName):
        filename = f'{self.config.targetDir}/current/rtl/{moduleName}.v'
        if not os.path.isfile(filename):
            return []
        ilist = defaultdict(list)
        with open(filename) as f:
            for r in f.readlines():
                tok = [x.strip() for x in r.split()]
                if len(tok) > 3 and '(' == tok[2]:
                    ilist[tok[0]].append(tok[1])
        return [(k, len(ilist[k])) for k in ilist.keys()]

    def maketree(self, module):
        submodules = self.getsubmodules(module.name)
        for s, l in submodules:
            x = Node(s, parent=module, ins=l)
            self.maketree(x)
        return module


    def build_chipyard(self, action):
        targetDir = self.config.targetDir + '/current'
        root = Node('Gemmini')
        self.maketree(root)
        print(RenderTree(root))
        modlist = []
        for pre, fill, node in RenderTree(root):
            if not node.name in modlist:
                modlist.append(node.name)
        rtlDir = self.config.targetDir + '/current/rtl/'
        rtl_list = '\n'.join([rtlDir + i + '.v' for i in modlist])
        ip_list = ' '.join([targetDir + '/' + i for i in os.listdir(targetDir) if 'vivado.tcl' in i])
        with open(targetDir + '/vsrcs.txt', 'w') as f:
            f.write(rtl_list)
            f.write('\n')
            f.write(self.config.chipyardDir + '/generators/rocket-chip/src/main/resources/vsrc/plusarg_reader.v')
            f.close()

        buid_env = os.environ.copy()
        buid_env['XILINX_VIVADO'] ='/usr/local/packages/Xilinx/Vivado/2021.2'
        tclscript = self.config.chipyardDir + '/fpga/fpga-shells/xilinx/common/tcl/vivado_partial.tcl'

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

        super(ChipyardVivadoDFFlowEnv1, self).__init__()
        # Define action and observation space
        Parameter = tup('Parameter', 'name dim')

        self.parameters = [Parameter('meshRC', 4),
        Parameter('dataflow', 2) ,
        Parameter('sp_factor', 9),
        Parameter('acc_factor', 9),
        Parameter('has_training_convs', 2),
        Parameter('has_max_pool', 2),
        Parameter('has_nonlinear_activations', 2)]

        self.action_space = MultiDiscrete(np.array([par.dim for par in self.parameters]))
        self.observation_space = Box(low=0, high=1.0, shape=(1,), dtype=np.float64)

        self.config = config


    def step(self, action):
        print(action)
        done, elapsed_time = self.build_chipyard(action)
        print(done)
        observation = np.array([0.5])
        reward  = 0
        info = {"elapsed_time", elapsed_time}

        return observation, reward, done, info

    def reset(self):
        done, elapsed_time = self.build_chipyard([])
        print(done)
        observation = np.array([elapsed_time])
        return observation
