package chipyard.fpga.singleEv7

import chisel3._

import freechips.rocketchip.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.system._
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._

import chipyard.{DigitalTop, DigitalTopModule}

import sifive.fpgashells.devices.xilinx.xilinxsingleEv7mig._

// ------------------------------------
// VCU118 DigitalTop
// ------------------------------------

case class TLScratchpadParams(
  base: BigInt,
  mask: BigInt)

case object TLScratchpadKey extends Field[Seq[TLScratchpadParams]](Nil)

trait CanHaveTLScratchpad { this: BaseSubsystem =>
  private val portName = "TL-Scratchpad"

  val tlspadOpt = p(TLScratchpadKey).map { param =>
    val spad = mbus { LazyModule(new TLRAM(address=AddressSet(param.base, param.mask), beatBytes=mbus.beatBytes, devName=Some("tl-scratchpad"))) }
    mbus.toVariableWidthSlave(Some(portName)) { spad.node }
    spad
  }
}

class CustomDigitalTop(implicit p: Parameters) extends DigitalTop
with HasMemoryXilinxSingleEv7MIG
with CanHaveTLScratchpad
{
  //val xpmROMs = p(XPMROMLocated(location)).map { XPMROM.attach(_, this, CBUS) }
  val hangROM  = p(HangROMLocated(location)).map { HangROM.attach(_, this, CBUS) }
  override lazy val module = new CustomDigitalTopModule(this)
}

trait HasResetVectorInputImp { this: HasTilesModuleImp =>  
  //val reset_vector_in = IO(Input(UInt(outer.p(XLen).W)))
  //outer.tileResetVectorIONodes.head.bundle := 0x010000.U
}

trait HasRTCTickOutputImp {self: HasRTCModuleImp =>
  val rt = IO(Output(Bool()))
  rt := outer.clintOpt.get.module.io.rtcTick
}

class CustomDigitalTopModule[+L <: CustomDigitalTop](l: L) extends DigitalTopModule(l) 
with HasResetVectorInputImp
with HasMemoryXilinxSingleEv7MIGModuleImp
with HasRTCTickOutputImp
