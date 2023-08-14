import os
from subprocess import run
from time import time


class FIRRTLVerilogEmitter:
    def build_verilog(self, targetDir):
        buid_env = os.environ.copy()
        init_time = time()
        build_process = run(f"""{self.config.firrtlPath} --allow-unrecognized-annotations --input-file  {targetDir}/{self.config.prefix}.fir --annotation-file  {targetDir}/{self.config.prefix}.anno.json --output-annotation-file  {targetDir}/{self.config.prefix}.out.anno.json --log-level error --target:fpga --emission-options disableMemRandomization --emission-options disableRegisterRandomization --target-dir {targetDir}/rtl -e verilog""",
                            shell = True, capture_output=True, text=True, cwd = self.config.chipyardDir, env = buid_env)
        elapsed_time = time() - init_time
        print('Finished Verilog ...')
        print(f'Time elapsed: {elapsed_time}')

        with open(f'{targetDir}/{self.config.prefix}.verilog.stdout.log', 'w') as f:
            f.write(build_process.stdout)
            f.close()

        with open(f'{targetDir}/{self.config.prefix}.verilog.stderr.log', 'w') as f:
            f.write(build_process.stderr)
            f.close()

        failed = '[error]' in build_process.stdout or 'Exception' in build_process.stderr

        return failed, elapsed_time

    def __init__(self, config):
        self.config = config

    def perform(self, action):
        done, elapsed_time = self.build_verilog(action)
        info = {'elapsed_time': elapsed_time, 'done': done}
        return info
