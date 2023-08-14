import gym
from gym.spaces import Discrete, Box
import numpy as np
import os
from subprocess import run
from time import time
from ..util import getBlackboxes, getNumInstances


floorPlanGemmini = f'''
create_pblock pgem
resize_pblock pgem -add CLOCKREGION_X1Y3:CLOCKREGION_X1Y5
set_property CONTAIN_ROUTING 1 [get_pblocks pgem]
set_property EXCLUDE_PLACEMENT 1 [get_pblocks pgem]
set_property IS_SOFT 0 [get_pblocks pgem]
add_cells_to_pblock pgem [get_cells [list chiptop/system/tile_prci_domain/tile_reset_domain/tile/gemmini]]
'''
floorPlanMesh = f'''
create_pblock pmesh
resize_pblock pmesh -add CLOCKREGION_X0Y0:CLOCKREGION_X1Y2
set_property CONTAIN_ROUTING 1 [get_pblocks pmesh]
set_property EXCLUDE_PLACEMENT 1 [get_pblocks pmesh]
set_property IS_SOFT 0 [get_pblocks pmesh]
add_cells_to_pblock pmesh [get_cells [list chiptop/system/tile_prci_domain/tile_reset_domain/tile/gemmini/ex_controller/mesh/mesh]]
'''
beginStopwatch = 'set t1 [clock seconds]\n'
endStopwatch = '''
set t2 [clock seconds]
puts [format "Steptime: %d" [expr {$t2 - $t1}]]
'''
def synthFlowIP(targetDir, meshSize):
    return f'''
remove_files [get_files Mesh.v]
read_verilog {targetDir}/ipDir/{meshSize}.v
synth_design -top Gemmini -flatten_hierarchy none -mode out_of_context -constrset [create_fileset empty]
write_checkpoint -force [file join $wrkdir post_synth]
close_project
open_checkpoint {targetDir}/ipDir/static.dcp               
read_checkpoint -cell chiptop/system/tile_prci_domain/tile_reset_domain/tile/gemmini [file join $wrkdir post_synth.dcp]
read_checkpoint -cell [get_cells [list chiptop/system/tile_prci_domain/tile_reset_domain/tile/gemmini/ex_controller/mesh/mesh]] {targetDir}/ipDir/{meshSize}.dcp
'''


floorPlanIP = f'''
set_property CONTAIN_ROUTING 1 [get_pblocks *pmesh]
set_property EXCLUDE_PLACEMENT 1 [get_pblocks *pmesh]
set_property IS_SOFT 0 [get_pblocks *pmesh]
set_property HD.RECONFIGURABLE 0 [get_cells [list chiptop/system/tile_prci_domain/tile_reset_domain/tile/gemmini/ex_controller/mesh/mesh]]
'''

def synthFlowDiff(targetDir):
    return f'''
synth_design -top Gemmini -flatten_hierarchy none -mode out_of_context -constrset [create_fileset empty]
write_checkpoint -force [file join $wrkdir post_synth]
close_project

open_checkpoint {targetDir}/ipDir/static.dcp
read_checkpoint -cell chiptop/system/tile_prci_domain/tile_reset_domain/tile/gemmini [file join $wrkdir post_synth.dcp]
'''


def tailFlowIP(targetDir, meshSize):
    return f'''
set_property HD.RECONFIGURABLE 1 [get_cells [list chiptop/system/tile_prci_domain/tile_reset_domain/tile/gemmini/ex_controller/mesh/mesh]]
write_checkpoint -cell [get_cells [list chiptop/system/tile_prci_domain/tile_reset_domain/tile/gemmini/ex_controller/mesh/mesh]] {targetDir}/ipDir/{meshSize}.dcp
write_verilog -mode synth_stub -cell [get_cells chiptop/system/tile_prci_domain/tile_reset_domain/tile/gemmini/ex_controller/mesh/mesh] {targetDir}/ipDir/{meshSize}.v
'''


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

def tailFlowInit(targetDir, meshSize):
    return f'''
set_property HD.RECONFIGURABLE 1 [get_cells [list chiptop/system/tile_prci_domain/tile_reset_domain/tile/gemmini/ex_controller/mesh/mesh]]
write_checkpoint -cell [get_cells [list chiptop/system/tile_prci_domain/tile_reset_domain/tile/gemmini/ex_controller/mesh/mesh]] {targetDir}/ipDir/{meshSize}.dcp
write_verilog -mode synth_stub -cell [get_cells chiptop/system/tile_prci_domain/tile_reset_domain/tile/gemmini/ex_controller/mesh/mesh] {targetDir}/ipDir/{meshSize}.v
set_property HD.RECONFIGURABLE 0 [get_cells [list chiptop/system/tile_prci_domain/tile_reset_domain/tile/gemmini/ex_controller/mesh/mesh]]
update_design -cells [get_cells chiptop/system/tile_prci_domain/tile_reset_domain/tile/gemmini] -black_box
write_checkpoint {targetDir}/ipDir/static.dcp
'''


placeFlowAlt = '''
place_design -no_timing_driven
write_checkpoint -force [file join $wrkdir post_place]
'''


class ChipyardVivadoDiffFlowEnv(gym.Env):

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
        super(ChipyardVivadoDiffFlowEnv, self).__init__()
        # Define action and observation space
        self.action_space = Discrete(1 << 31 -1)
        self.observation_space = Box(low=0, high=1.0, shape=(1,), dtype=np.float64)

        self.config = config
        self.prev = ''
        self.ips = []

    def getBlackBoxes(self, rtlDir1, rtlDir2):
        return getBlackboxes('SingleEv7FPGATestHarness', rtlDir1, rtlDir2)

    def getMeshSize(self, rtlDir):
        sqr = {256: 16, 64: 8, 16: 4}
        n = getNumInstances(rtlDir, 'Mesh', 'Tile')
        return sqr[n]

    def diffFlow(self, action):
        targetDir = self.config.targetDir + f'/{action}'
        rtlDir = f'{targetDir}/rtl'
        meshSize = self.getMeshSize(rtlDir)

        synthFlow = ''
        placeFlow = ''
        floorPlan = floorPlanGemmini
        tailFlow = ''

        if len(self.prev) > 0:
            #Can use previous steps
            bbs = self.getBlackBoxes(self.prev, rtlDir)
            if len(bbs) == 1 and 'Gemmini' in bbs:
                # Use previous static
                if f'{meshSize}' in self.ips:
                    #Reuse mesh ip
                    synthFlow = synthFlowIP(self.config.targetDir, meshSize)
                    floorPlan = floorPlan + floorPlanIP
                    placeFlow = placeFlowAlt
                else:
                    #Emit mesh ip
                    emitIPFlow = '1'
                    self.ips.append(f'{meshSize}')
                    synthFlow = synthFlowDiff(self.config.targetDir)
                    floorPlan = floorPlan + floorPlanMesh
                    tailFlow = tailFlowIP(self.config.targetDir, meshSize)
        else:
            #First step
            floorPlan = floorPlan + floorPlanMesh
            tailFlow = tailFlowInit(self.config.targetDir, meshSize)
            self.prev = rtlDir
            self.ips = ['static', f'{meshSize}']


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

        if len(synthFlow) > 0:
            vivadoScript = vivadoScript + synthFlow
        else:
            with open(f'{vivadoScriptDir}/synth.tcl') as f:
                vivadoScript = vivadoScript + ''.join([l for l in f.readlines()])
                f.close()

        vivadoScript = vivadoScript + floorPlan

        if len(placeFlow) > 0:
            vivadoScript = vivadoScript + placeFlow
        else:
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

        vivadoScript = vivadoScript + tailFlow
        vivadoScript = wrapStopwatch(vivadoScript, ['synth_design', 'place_design', 'route_design'])

        with open(f'{targetDir}/vivadoScript.tcl', 'w') as f:
            f.write(vivadoScript)
            f.close()
        #return False, 0
        return self.vivadoFlow(targetDir, f'{targetDir}/vivadoScript.tcl')

    def step(self, action):
        done, elapsed_time = self.diffFlow(action)
        observation = np.array([0.5])
        reward = 0
        info = {"elapsed_time": elapsed_time}
        return observation, reward, done, info

    def reset(self):
        self.prev = ''
        self.ips = []
        os.makedirs(self.config.targetDir + '/ipDir', exist_ok=True)
        return np.array([0])
