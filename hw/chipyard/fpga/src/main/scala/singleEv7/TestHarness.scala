package chipyard.fpga.singleEv7

import chisel3._
import chisel3.util.{Counter}

import freechips.rocketchip.diplomacy.{LazyModule}
import freechips.rocketchip.config.{Parameters}
import freechips.rocketchip.util.{ElaborationArtefacts}

import sifive.fpgashells.shell.s2c._
import sifive.fpgashells.ip.xilinx._
import sifive.fpgashells.clocks._

import chipyard.{BuildTop, HasHarnessSignalReferences}
import chipyard.harness.{ApplyHarnessBinders}
import chipyard.iobinders.{HasIOBinders}

class SingleEv7FPGATestHarness(override implicit val p: Parameters) extends SingleEv7Shell with HasDDR3 with HasHarnessSignalReferences {
	
  //-----------------------------------------------------------------------
  // Clock Generator
  //-----------------------------------------------------------------------

  val clock_gen = Module(new Series7MMCM(PLLParameters("MASTER_CLOCK_GEN",
    PLLInClockParameters(200, 50),
    Seq(
      PLLOutClockParameters(p(FPGAFrequencyKey))))))
  
  clock_gen.io.clk_in1 := board_clock
  clock_gen.io.reset   := board_reset
  val clock_gen_locked = clock_gen.io.locked
  val Seq(busclk, _*) = clock_gen.getClocks

  //-----------------------------------------------------------------------
  // System clock and reset
  //-----------------------------------------------------------------------

  dut_clock := busclk
  
  val safe_reset = Module(new singleEv7reset)
  
  safe_reset.areset := !clock_gen_locked || mig_ui_reset || !mig_mmcm_locked || !mig_calib_comp || dbg_reset || ~SW3;
  safe_reset.clock1 := mig_ui_clock
  mig_aresetn          := ~safe_reset.reset1
  safe_reset.clock2 := dut_clock
  dut_reset            := safe_reset.reset2	

  val lazyDut = LazyModule(p(BuildTop)(p)).suggestName("chiptop")
  
  val buildtopClock = dut_clock
  val buildtopReset = dut_reset
  val success = false.B
  val dutReset = false.B

  val coreplex = withClockAndReset(buildtopClock, buildtopReset) 
  {
    Module(lazyDut.module)
  }
  
  //connectMIG(coreplex)

  // must be after HasHarnessSignalReferences assignments
  lazyDut match { case d: HasIOBinders =>
    ApplyHarnessBinders(this, d.lazySystem, d.portMap)
  }
  
  LED31 := clock_gen_locked

  ElaborationArtefacts.add(
    "1.clockdomains.synth.tcl",
    """
    create_clock -add -name sys_clk -period 5 [get_ports board_clk_p]
    create_clock -add -name j4 -period 20 [get_pins -of [get_cells -hierarchical -filter "JTAG_CHAIN == 4"] -filter "REF_PIN_NAME==TCK"]

    set_clock_groups -asynchronous -group [get_clocks j*] 
    set_clock_groups -asynchronous -group [get_clocks -of [get_pins clock_gen/clk_out1]]
    """)
}

