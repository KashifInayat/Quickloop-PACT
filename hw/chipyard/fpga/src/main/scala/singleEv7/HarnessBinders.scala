package chipyard.fpga.singleEv7

import chisel3._
import chisel3.util.{Counter}

import freechips.rocketchip.devices.debug._
import freechips.rocketchip.jtag.{JTAGIO}
import freechips.rocketchip.subsystem._

import sifive.blocks.devices.uart._
import sifive.blocks.devices.spi._
import sifive.blocks.devices.gpio._
import sifive.blocks.devices.jtag._
import sifive.blocks.devices.pinctrl._

import sifive.fpgashells.ip.xilinx.{IBUFG, IOBUF, PULLUP, PowerOnResetFPGAOnly}
import sifive.fpgashells.ip.xilinx.bscan2._
import sifive.fpgashells.devices.xilinx.xilinxsingleEv7mig._

import chipyard.harness.{ComposeHarnessBinder, OverrideHarnessBinder}
import chipyard.iobinders.JTAGChipIO

class WithSingleEv7ResetHarnessBinder extends ComposeHarnessBinder({
  (system: HasPeripheryDebugModuleImp, th: SingleEv7FPGATestHarness, ports: Seq[Bool]) => {
    require(ports.size == 2)
      // Debug module reset
      th.dbg_reset := ports(0)

      // JTAG reset
      ports(1) := th.dut_reset
  }
})

class WithSingleEv7JTAGHarnessBinder extends OverrideHarnessBinder({
  (system: HasPeripheryDebug, th: SingleEv7FPGATestHarness, ports: Seq[Data]) => {
    ports.map {
      case j: JTAGChipIO =>
      
          val btunnel = withClockAndReset(false.B.asClock, false.B) {Module(new JTAGTUNNEL)}
          
          j.TCK := btunnel.jtag_tck
          j.TMS := btunnel.jtag_tms
          j.TDI := btunnel.jtag_tdi
			
          btunnel.jtag_tdo := j.TDO
          btunnel.jtag_tdo_en := true.B	
    }
  }
})

class WithSingleEv7UARTHarnessBinder extends OverrideHarnessBinder({
  (system: HasPeripheryUARTModuleImp, th: SingleEv7FPGATestHarness, ports: Seq[UARTPortIO]) => {
      th.UART.TX :=  ~ports.head.txd
      ports.head.rxd := ~th.UART.RX
  }
})

class WithSingleEv7SPIHarnessBinder extends OverrideHarnessBinder({
  (system: HasPeripherySPIModuleImp, th: SingleEv7FPGATestHarness, ports: Seq[SPIPortIO]) => {
	  val ioport = ports.head
	  ioport.dq.foreach {pin => pin.i := false.B}
      IOBUF(th.SDIO.CLK, ioport.sck)
      IOBUF(th.SDIO.DAT3, ioport.cs(0))
      ioport.dq(1).i := IOBUF(th.SDIO.DAT0)
      IOBUF(th.SDIO.CMD, ioport.dq(0).o)
  }
})

class WithGPIOOff extends OverrideHarnessBinder({
  (system: HasPeripheryGPIOModuleImp, th: SingleEv7FPGATestHarness, ports: Seq[GPIOPortIO]) => {
	  ports.foreach{ g => g.pins.foreach { p =>
      p.i.ival := false.B
      p.i.po.get := false.B
      } }
  }
})

class WithRTCTick extends OverrideHarnessBinder({
  (system: HasRTCTickOutputImp, th: SingleEv7FPGATestHarness, ports: Seq[Bool]) => {
	  val ioport = ports.head
	  withClockAndReset(th.dut_clock, th.dut_reset) 
      {
		  val (v, i) = Counter(ioport, 512) 
          th.LED32 := v.head(1)
      }
  }
})

class WithDDRMIGHarnessBinder extends OverrideHarnessBinder({
  (system: HasMemoryXilinxSingleEv7MIGModuleImp, th: SingleEv7FPGATestHarness, ports: Seq[XilinxSingleEv7MIGIO]) => {
	  
	  val ioport         = ports.head
      th.mig_ui_reset    := ioport.ui_clk_sync_rst
      th.mig_ui_clock    := ioport.ui_clk  
      th.mig_mmcm_locked := ioport.mmcm_locked
      th.mig_calib_comp  := ioport.init_calib_complete

      ioport.aresetn     := th.mig_aresetn
    
      th.LED33           := ioport.init_calib_complete
    
      ioport.sys_clk_i   := th.board_clock.asBool
      ioport.sys_rst     := ~th.board_reset
      
      th.ddr             <> ioport
  }
})
