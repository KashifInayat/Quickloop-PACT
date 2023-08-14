// See LICENSE for license details.
package sifive.fpgashells.shell.s2c

import Chisel._
import chisel3.{Input, Output, RawModule, withClockAndReset}
import chisel3.experimental.{attach, Analog, IntParam, ExtModule}

import freechips.rocketchip.config._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.util.{ElaborationArtefacts}
import freechips.rocketchip.jtag.{JTAGIO}

import sifive.fpgashells.ip.xilinx._
import sifive.fpgashells.clocks._

import sifive.fpgashells.devices.xilinx.xilinxsingleEv7mig._
import sifive.fpgashells.ip.xilinx.singleEv7mig._

class singleEv7reset() extends ExtModule
{
    val areset = IO(Input(Bool()))
    val clock1 = IO(Input(Clock()))
    val reset1 = IO(Output(Bool()))
    val clock2 = IO(Input(Clock()))
    val reset2 = IO(Output(Bool()))
}

trait HasDDR3 { this: SingleEv7Shell =>
  
  require(!p.lift(MemoryXilinxDDRKey).isEmpty)
  val ddr = IO(new XilinxSingleEv7MIGPads(p(MemoryXilinxDDRKey)))
  
  def connectMIG(dut: HasMemoryXilinxSingleEv7MIGModuleImp): Unit = {
	  
	mig_ui_reset                 := dut.xilinxsingleEv7mig.ui_clk_sync_rst
    mig_ui_clock                 := dut.xilinxsingleEv7mig.ui_clk  
    mig_mmcm_locked              := dut.xilinxsingleEv7mig.mmcm_locked
    mig_calib_comp               := dut.xilinxsingleEv7mig.init_calib_complete

    dut.xilinxsingleEv7mig.aresetn   := mig_aresetn
    
    LED33 := dut.xilinxsingleEv7mig.init_calib_complete
    
    dut.xilinxsingleEv7mig.sys_clk_i := board_clock.asBool
    dut.xilinxsingleEv7mig.sys_rst   := ~board_reset

    ddr <> dut.xilinxsingleEv7mig
  }
}



//-------------------------------------------------------------------------
// SingleEv7Shell
//-------------------------------------------------------------------------

abstract class SingleEv7Shell(implicit val p: Parameters) extends RawModule {

  //-----------------------------------------------------------------------
  // Interface
  //-----------------------------------------------------------------------

  // Clock & Reset
  
  val RESET_SW  = IO(Input(Bool()))
  val SW2   	= IO(Input(Bool()))
  val SW3       = IO(Input(Bool()))
  
  val board_clk_p   = IO(Input(Clock()))
  val board_clk_n   = IO(Input(Clock()))

  val LED31    = IO(Output(Bool()))
  val LED32    = IO(Output(Bool()))
  val LED33    = IO(Output(Bool()))

  // UART
  val UART	   = IO(new Bundle {
									val TX		= Output(Bool())
									val RX		= Input(Bool())})

  //Microsd
  val SDIO        = IO(new Bundle {
                  val CLK  = Analog(1.W)
                  val CMD  = Analog(1.W)
                  val DAT0     = Analog(1.W)
                  val DAT1    = Analog(1.W)
                  val DAT2     = Analog(1.W)
                  val DAT3    = Analog(1.W)
                })

  val dut_clock       = Wire(Clock())
  val dut_reset       = Wire(Bool())
  
  val board_clock       = Wire(Clock())
  val board_reset       = Wire(Bool())
  val dbg_reset         = Wire(Bool())
  
  val mig_ui_reset   = Wire(Bool())
  val mig_ui_clock   = Wire(Clock())
  val mig_aresetn    = Wire(Bool())
  val mig_mmcm_locked = Wire(Bool())
  val mig_calib_comp = Wire(Bool())
  
  val ibufds = Module(new IBUFDS)
  ibufds.io.I  := board_clk_p
  ibufds.io.IB := board_clk_n
  board_clock  := ibufds.io.O
  board_reset  := ~RESET_SW | ~SW2
  
  ElaborationArtefacts.add(
    "0.pinmap.synth.tcl",
    """
    set_property -dict { PACKAGE_PIN N4   IOSTANDARD DIFF_SSTL15 } [get_ports { board_clk_p }]; 
    set_property -dict { PACKAGE_PIN N3   IOSTANDARD DIFF_SSTL15 } [get_ports { board_clk_n }];
    set_property -dict { PACKAGE_PIN AH42   IOSTANDARD LVCMOS18 } [get_ports { RESET_SW }];
    set_property -dict { PACKAGE_PIN T24    IOSTANDARD LVCMOS18 } [get_ports { SW2 }]; 
    set_property -dict { PACKAGE_PIN M21    IOSTANDARD LVCMOS18 } [get_ports { SW3 }]; 
    set_property -dict { PACKAGE_PIN T19    IOSTANDARD LVCMOS18 } [get_ports { UART_RX }]; 
    set_property -dict { PACKAGE_PIN R20    IOSTANDARD LVCMOS18 } [get_ports { UART_TX }]; 
    set_property -dict { PACKAGE_PIN F34    IOSTANDARD LVCMOS18 } [get_ports { LED31 }];
    set_property -dict { PACKAGE_PIN F37    IOSTANDARD LVCMOS18 } [get_ports { LED32 }];
    set_property -dict { PACKAGE_PIN N34    IOSTANDARD LVCMOS18 } [get_ports { LED33 }];
    set_property -dict { PACKAGE_PIN F18    IOSTANDARD LVCMOS18 } [get_ports { SDIO_CLK }]; 
    set_property -dict { PACKAGE_PIN E18    IOSTANDARD LVCMOS18 } [get_ports { SDIO_CMD }]; 
    set_property -dict { PACKAGE_PIN A19    IOSTANDARD LVCMOS18 } [get_ports { SDIO_DAT0 }]; 
    set_property -dict { PACKAGE_PIN C18    IOSTANDARD LVCMOS18 } [get_ports { SDIO_DAT1 }];
    set_property -dict { PACKAGE_PIN D18    IOSTANDARD LVCMOS18 } [get_ports { SDIO_DAT2 }];
    set_property -dict { PACKAGE_PIN A18    IOSTANDARD LVCMOS18 } [get_ports { SDIO_DAT3 }];
    """)
}
