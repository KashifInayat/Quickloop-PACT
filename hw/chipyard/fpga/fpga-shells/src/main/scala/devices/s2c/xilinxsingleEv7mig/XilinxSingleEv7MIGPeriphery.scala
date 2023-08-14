// See LICENSE for license details.
package sifive.fpgashells.devices.xilinx.xilinxsingleEv7mig

import Chisel._
import freechips.rocketchip.config._
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp, AddressRange}

case object MemoryXilinxDDRKey extends Field[XilinxSingleEv7MIGParams]

trait HasMemoryXilinxSingleEv7MIG { this: BaseSubsystem =>
  val module: HasMemoryXilinxSingleEv7MIGModuleImp

  val xilinxsingleEv7mig = LazyModule(new XilinxSingleEv7MIG(p(MemoryXilinxDDRKey)))

  xilinxsingleEv7mig.node := mbus.toDRAMController(Some("xilinxsingleEv7mig"))()
}

trait HasMemoryXilinxSingleEv7MIGBundle {
  val xilinxsingleEv7mig: XilinxSingleEv7MIGIO
  def connectXilinxSingleEv7MIGToPads(pads: XilinxSingleEv7MIGPads) {
    pads <> xilinxsingleEv7mig
  }
}

trait HasMemoryXilinxSingleEv7MIGModuleImp extends LazyModuleImp
    with HasMemoryXilinxSingleEv7MIGBundle {
  val outer: HasMemoryXilinxSingleEv7MIG
  val ranges = AddressRange.fromSets(p(MemoryXilinxDDRKey).address)
  require (ranges.size == 1, "DDR range must be contiguous")
  val depth = ranges.head.size
  val xilinxsingleEv7mig = IO(new XilinxSingleEv7MIGIO(depth))

  xilinxsingleEv7mig <> outer.xilinxsingleEv7mig.module.io.port
}
