// See LICENSE.SiFive for license details.
package chipyard.fpga.singleEv7

import Chisel._
import chisel3.DontCare
import freechips.rocketchip.config.{Field, Parameters}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.subsystem.{Attachable, HierarchicalLocation, TLBusWrapperLocation}
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._
import freechips.rocketchip.devices.tilelink._

case class XPMROMParams(address: BigInt, name: String, depth: Int = 2048, width: Int = 32)

class TLXPMROM(c: XPMROMParams)(implicit p: Parameters) extends LazyModule {
  val beatBytes = c.width/8
  val node = TLManagerNode(Seq(TLSlavePortParameters.v1(
    Seq(TLSlaveParameters.v1(
      address            = AddressSet.misaligned(c.address, c.depth*beatBytes),
      resources          = new SimpleDevice("rom", Seq("xilinx,XPMrom0")).reg("mem"),
      regionType         = RegionType.UNCACHED,
      executable         = true,
      supportsGet        = TransferSizes(1, beatBytes),
      fifoId             = Some(0))), // requests are handled in order
    beatBytes = beatBytes)))

  lazy val module = new LazyModuleImp(this) {
    val (in, edge)= node.in(0)
    
    val rom = Module(new xpm_memory_sprom(log2Ceil(c.depth), c.width)) suggestName(c.name)
    
    /*
    val injectdbiterra = Wire(Bool())
    val injectsbiterra = Wire(Bool())
    val regcea = Wire(Bool())
    val rsta = Wire(Bool())
    val sleep = Wire(Bool())
    
    injectdbiterra := DontCare
    injectsbiterra := DontCare
    regcea := DontCare
    rsta := DontCare
    sleep := DontCare*/
    
    
    
    //val dbiterra = rom.dbiterra // 1-bit output: Leave open.
    val douta = rom.douta // READ_DATA_WIDTH_A-bit output: Data output for port A read operations.
    //val sbiterra = rom.sbiterra // 1-bit output: Leave open.
    rom.addra := edge.addr_hi(in.a.bits.address - UInt(c.address))(log2Ceil(c.depth)-1, 0) // ADDR_WIDTH_A-bit input: Address for port A read operations.
    rom.clka := clock.asUInt() // 1-bit input: Clock signal for port A.
    rom.ena := in.a.fire() // 1-bit input: Memory enable signal for port A. Must be high on clock
    // cycles when read operations are initiated. Pipelined internally.
    //rom.injectdbiterra := DontCare // 1-bit input: Do not change from the provided value.
    //rom.injectsbiterra := DontCare // 1-bit input: Do not change from the provided value.
    //rom.regcea := DontCare // 1-bit input: Do not change from the provided value.
    //rom.rsta := DontCare // 1-bit input: Reset signal for the final port A output register stage.
    // Synchronously resets output port douta to the value specified by
       // parameter READ_RESET_VALUE_A.
    //rom.sleep := DontCare // 1-bit input: sleep signal to enable the dynamic power saving feature.

    val d_full = RegInit(Bool(false))
    val d_size = Reg(UInt())
    val d_source = Reg(UInt())
    val d_data = douta holdUnless RegNext(in.a.fire())

    // Flow control
    when (in.d.fire()) { d_full := Bool(false) }
    when (in.a.fire()) { d_full := Bool(true)  }
    in.d.valid := d_full
    in.a.ready := in.d.ready || !d_full

    when (in.a.fire()) {
      d_size   := in.a.bits.size
      d_source := in.a.bits.source
    }

    in.d.bits := edge.AccessAck(d_source, d_size, d_data)

    // Tie off unused channels
    in.b.valid := Bool(false)
    in.c.ready := Bool(true)
    in.e.ready := Bool(true)
  }
}

case class XPMROMLocated(loc: HierarchicalLocation) extends Field[Seq[XPMROMParams]](Nil)

object XPMROM {
  def attach(params: XPMROMParams, subsystem: Attachable, where: TLBusWrapperLocation)
            (implicit p: Parameters): TLXPMROM = {
    val bus = subsystem.locateTLBusWrapper(where)
    val XPMROM = LazyModule(new TLXPMROM(params))
    XPMROM.node := bus.coupleTo("XPMROM") {
      TLFragmenter(XPMROM.beatBytes, bus.blockBytes) :*= TLWidthWidget(bus) := _
    }
    InModuleBody { XPMROM.module.clock := bus.module.clock }
    InModuleBody { XPMROM.module.reset := bus.module.reset }
    XPMROM
  }
}
