import os
from subprocess import run
from time import time
import serial
import json
import shutil

import gym
from gym.spaces import Discrete, Box
import numpy as np

class ChipyardPartialAnalyzeEnv(gym.Env):

    def analyze_chipyard(self, action):
        buildDir = f'{self.config.buildDir}/{action}/current'
        targetDir = buildDir + '/dump'
        os.makedirs(targetDir, exist_ok=True)

        buid_env = os.environ.copy()
        buid_env['RISCV_PATH'] = self.config.riscv_path
        buid_env['XILINX_VIVADO'] = self.config.vivado_path
        buid_env['GEMMINI_HEADER'] = buildDir

        init_time = time()
        configScript = """
        open_hw_manager
        connect_hw_server -allow_non_jtag
        open_hw_target
        current_hw_device [get_hw_devices xc7v2000t_0]
        refresh_hw_device -update_hw_probes false [lindex [get_hw_devices xc7v2000t_0] 0]
        set_property PROBES.FILE {} [get_hw_devices xc7v2000t_0]
        set_property FULL_PROBES.FILE {} [get_hw_devices xc7v2000t_0]
        set_property PROGRAM.FILE {%s/obj/SingleEv7FPGATestHarness.bit} [get_hw_devices xc7v2000t_0]
        set_property PARAM.FREQUENCY 15000000 [current_hw_target]
        program_hw_devices [get_hw_devices xc7v2000t_0]
        refresh_hw_device [lindex [get_hw_devices xc7v2000t_0] 0]
        close_hw_manager
        """
        with open(targetDir + '/config.tcl', 'w') as f:
            f.writelines(configScript % buildDir)
        build_process = run(f'{self.config.vivado_path}/bin/vivado -mode batch -source config.tcl',
                            shell=True, capture_output=True, text=True, cwd=targetDir, env=buid_env)
        print('Finished config FPGA...')
        elapsed_time = time() - init_time
        print(f'Time elapsed: {elapsed_time}')
        with open(f'{targetDir}/{self.config.prefix}.config.stdout.log', 'w') as f:
            f.write(build_process.stdout)
            f.close()

        with open(f'{targetDir}/{self.config.prefix}.config.stderr.log', 'w') as f:
            f.write(build_process.stderr)
            f.close()

        init_time = time()
        build_process = run('make PROGRAM=gemmini TARGET=freedom-e310-arty LINK_TARGET=scratchpad clean',
                            shell=True, capture_output=True, text=True, cwd=self.config.edkDir, env=buid_env)
        print('Cleaned gemmini build...')
        build_process = run('make PROGRAM=gemmini TARGET=freedom-e310-arty LINK_TARGET=scratchpad software',
                            shell=True, capture_output=True, text=True, cwd=self.config.edkDir, env=buid_env)
        shutil.copyfile(f'{self.config.edkDir}/software/gemmini/debug/gemmini.elf', f'{targetDir}/gemmini.elf')
        print('Finished gemmini build...')

        with open(f'{targetDir}/{self.config.prefix}.edk.stdout.log', 'w') as f:
            f.write(build_process.stdout)
            f.close()

        with open(f'{targetDir}/{self.config.prefix}.edkl.stderr.log', 'w') as f:
            f.write(build_process.stderr)
            f.close()
        elapsed_time = time() - init_time
        print(f'Time elapsed: {elapsed_time}')

        init_time = time()
        edkScript = """
        adapter speed 10000
        adapter driver ftdi
        ftdi vid_pid 0x0403 0x6014
        ftdi channel 0
        ftdi layout_init 0x00e8 0x60eb
        transport select jtag
        reset_config none
        set chain_length 24
        set _CHIPNAME singeEv7
        jtag newtap $_CHIPNAME tap -irlen $chain_length -ignore-version -expected-id 0x036B3093
        foreach t [jtag names] {
            puts [format "TAP: %s\n" $t]
        }
        target create him riscv -chain-position $_CHIPNAME.tap
        foreach t [target names] {
        puts [format "Target: %s\n" $t]
        }
        riscv set_bscan_tunnel_ir 0x8E4924
        riscv use_bscan_tunnel 5
        him configure -work-area-phys 0x80000000 -work-area-size 0x1000
        echo "Ready for Remote Connections"
        init
        halt
        set workload gemmini
        load_image gemmini.elf 0 elf 
        puts [read_memory 0x10000 32 2]
        set_reg {a0 0 a1 0x80000000 pc 0x80010000}
        resume
        exit"""
        with open(targetDir + '/run.tcl', 'w') as f:
            f.writelines(edkScript)

        build_process = run(f'{self.config.openocd} -f run.tcl',
                            shell=True, capture_output=True, text=True, cwd=targetDir, env=buid_env)
        print('Finished gemmini load...')

        with open(f'{targetDir}/{self.config.prefix}.load.stdout.log', 'w') as f:
            f.write(build_process.stdout)
            f.close()

        with open(f'{targetDir}/{self.config.prefix}.load.stderr.log', 'w') as f:
            f.write(build_process.stderr)
            f.close()
        elapsed_time = time() - init_time
        print(f'Time elapsed: {elapsed_time}')

        init_time = time()
        resultLines = []
        with serial.Serial(f'{self.config.serial}', 115200, timeout=600) as ser:
            while True:
                resultline = ser.readline()
                try:
                    resultline = resultline.decode().splitlines()[0]
                    resultLines.append(resultline)
                    if 'resnet' in resultline:
                        break
                except:
                    break
            ser.close()

        with open(f'{targetDir}/readback.txt', 'w') as f:
            f.write('\n'.join(resultLines))
            f.close()
        print('Finished gemmini readback...')
        elapsed_time = time() - init_time
        print(f'Time elapsed: {elapsed_time}')

        state = 0
        cycles = 0

        result = {}

        for line in resultLines:
            tok = line.split()
            if state == 0:
                if len(tok) == 5 and 'Gemmini' in tok[0] and 'cycles' in tok[-1]:
                    cycles = tok[3]
                    state = 1
                elif len(tok) == 3 and 'Cycles' in tok[0]:
                    cycles = tok[2]
                    state = 1
                elif len(tok) == 4 and 'Total' in tok[0]:
                    cycles = tok[2]
                    state = 1
            else:
                if 'exited' in line:
                    bench = tok[0].split('_main')[0]
                    state = 0
                    result[bench] = eval(cycles)

        util_file = buildDir + '/obj/report/utilization.txt'
        names = []
        indices = [5, 9, 10, 11, 12]
        with open(util_file) as f:
            for r in f.readlines():
                tok = [x.strip() for x in r.split('|')]
                if len(tok) > 2 and 'Instance' == tok[1]:
                    names = [tok[i] for i in indices]
                if len(tok) > 2 and 'gemmini' == tok[1]:
                    values = [tok[i] for i in indices]
        for n, v in zip(names, values):
            result['_'.join(n.split())] = eval(v)

        util_file = buildDir + '/obj/report/power.txt'
        names = []
        indices = [2, 4, 8, 10, 12, 14]
        with open(util_file) as f:
            for r in f.readlines():
                tok = [x.strip() for x in r.split('|')]
                if len(tok) > 2 and 'Name' == tok[1]:
                    names = [tok[i] for i in indices]
                if len(tok) > 2 and 'gemmini' == tok[1]:
                    values = [tok[i] for i in indices]

        for n, v in zip(names, values):
            try:
                result[n.split()[0]] = eval(v)
            except:
                result[n.split()[0]] = 0.0

        with open(f'{targetDir}/result.txt', 'w') as f:
            f.write(json.dumps(result))
            f.close()

        return False, result

    def __init__(self, config):

        super(ChipyardPartialAnalyzeEnv, self).__init__()
        # Define action and observation space

        self.action_space = Discrete(1 << 31 - 1)
        self.observation_space = Box(low=0, high=1.0, shape=(1,), dtype=np.float64)

        self.config = config

    def step(self, action):
        print(action)
        done, result = self.analyze_chipyard(action)
        print(done)
        observation = np.array([0.5])
        reward  = 0
        info = result

        return observation, reward, done, info

    def reset(self):
        done, elapsed_time = False, 0.0
        print(done)
        observation = np.array([elapsed_time])
        return observation