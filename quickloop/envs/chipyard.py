import gym
from gym.spaces import MultiDiscrete, Box
import numpy as np
from collections import namedtuple as tup
import os
from subprocess import PIPE, run
from re import findall

class ChipyardEnv(gym.Env):

    def build_chipyard(self, action):
        scala_conf = f"""
        package chipyard.fpga.singleEv7
        import gemmini._
        object Reconfig{{
            def apply() = {{
                gemmini.GemminiConfigs.defaultConfig.copy(
                    {','.join(['='.join([self.parameters[i].name, self.parameters[i].valFunct(v)]) for i, v in enumerate(list(action))])}
                )
            }}
        }}
        """
        self.fpga_dir = '/work/tayyeb/nas/gitlocal/fresh/chipyard/fpga'
        with open(self.fpga_dir + '/src/main/scala/singleEv7/reconfig/Reconfig.scala', 'w') as f:
            f.write(scala_conf)
            f.close()

        buid_env = os.environ.copy()
        buid_env['RISCV'] = '/work/tayyeb/sifive/toolchain'

        build_process = run(f'make -C {self.fpga_dir} verilog', shell = True, capture_output=True, text=True, env=buid_env)

        build_dir = findall(r'mkdir -p (.+)', build_process.stdout)
        done = 'make' in build_process.stderr

        return build_dir, done



    def __init__(self):
        super(ChipyardEnv, self).__init__()
        # Define action and observation space
        Parameter = tup('Parameter', 'name dim valFunct')

        self.parameters = [Parameter('meshRows', 4, lambda x: f'{2 << x}') ,
        Parameter('meshColumns', 4, lambda x: f'{2 << x}'),
        Parameter('dataflow', 2, lambda x: 'Dataflow.{}'.format(('WS', 'OS')[x])) ,
        Parameter('sp_capacity', 9, lambda x: f'CapacityInKilobytes({1 << x})'),
        Parameter('acc_capacity', 7, lambda x: f'CapacityInKilobytes({1 << x})'),
        Parameter('has_training_convs', 2, lambda x: ('false', 'true')[x]),
        Parameter('has_max_pool', 2, lambda x: ('false', 'true')[x]),
        Parameter('has_nonlinear_activations', 2, lambda x: ('false', 'true')[x])]

        self.action_space = MultiDiscrete(np.array([par.dim for par in self.parameters]))
        self.observation_space = Box(low=0, high=1.0, shape=(1,), dtype=np.float64)


    def step(self, action):
        print(action)
        build_dir, done = self.build_chipyard(action)
        print(build_dir, done)
        observation = np.array([0.5])
        reward  = 0

        info = {}

        return observation, reward, done, info

    def reset(self):
        build_dir, done = self.build_chipyard([])
        print(build_dir, done)
        observation = np.array([0.5])
        return observation