package chipyard.fpga.singleEv7

import chisel3._
import chisel3.experimental.{IO}

import freechips.rocketchip.util._
import freechips.rocketchip.devices.debug._
import sifive.blocks.devices.spi._
import sifive.blocks.devices.gpio._
import sifive.fpgashells.devices.xilinx.xilinxsingleEv7mig._

import chipyard.iobinders.{ComposeIOBinder, OverrideIOBinder}

class WithDebugResetPassthrough extends ComposeIOBinder({
  (system: HasPeripheryDebugModuleImp) => {
    // Debug module reset
    val io_ndreset: Bool = IO(Output(Bool())).suggestName("ndreset")
    io_ndreset := system.debug.get.ndreset

    // JTAG reset
    val sjtag = system.debug.get.systemjtag.get
    val io_sjtag_reset: Bool = IO(Input(Bool())).suggestName("sjtag_reset")
    sjtag.reset := io_sjtag_reset

    (Seq(io_ndreset, io_sjtag_reset), Nil)
  }
})

class WithRTCTickPassthrough extends ComposeIOBinder({
  (system: HasRTCTickOutputImp) => {
    // Debug module reset
    val io_tick: Bool = IO(Output(Bool())).suggestName("rtc_tick")
    io_tick := system.rt

    (Seq( io_tick ), Nil)
  }
})

class WithSPIIOPassthrough  extends ComposeIOBinder({
  (system: HasPeripherySPIModuleImp) => {
        val io_spi_pins_temp = system.spi.zipWithIndex.map { case (dio, i) => IO(dio.cloneType).suggestName(s"spi_$i") }
        (io_spi_pins_temp zip system.spi).map { case (io, sysio) =>
          io <> sysio
        }
        (io_spi_pins_temp, Nil)
      } 
})

class WithGPIOIOPassthrough extends OverrideIOBinder({
  (system: HasPeripheryGPIOModuleImp) => {
    val io_gpio_pins_temp = system.gpio.zipWithIndex.map { case (dio, i) => IO(dio.cloneType).suggestName(s"gpio_$i") }
    (io_gpio_pins_temp zip system.gpio).map { case (io, sysio) =>
      io <> sysio
    }
    (io_gpio_pins_temp, Nil)
  }
})

class WithDDRMIGPassthrough extends ComposeIOBinder({
  (system: HasMemoryXilinxSingleEv7MIGModuleImp) => {
    val ddrIO: XilinxSingleEv7MIGIO = IO(new XilinxSingleEv7MIGIO(system.depth))suggestName("ddr_io")
    ddrIO <> system.xilinxsingleEv7mig
    
    (Seq(ddrIO), Nil)
  }
})

