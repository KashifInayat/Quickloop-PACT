// See LICENSE.SiFive for license details.

package chipyard.fpga.singleEv7

import Chisel._
import freechips.rocketchip.config.{Field, Parameters}
import freechips.rocketchip.subsystem.{BaseSubsystem, HierarchicalLocation, HasTiles, TLBusWrapperLocation}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.prci.{ClockSinkDomain}

import java.nio.ByteBuffer
import java.nio.file.{Files, Paths}

/** Size, location and contents of the hang rom. */
case class HangROMParams(
  address: BigInt = 0x10000)

class TLROM(val base: BigInt, val size: Int, contentsDelayed: => Seq[Byte], executable: Boolean = true, beatBytes: Int = 4,
  resources: Seq[Resource] = new SimpleDevice("rom", Seq("sifive,rom0")).reg("mem"))(implicit p: Parameters) extends LazyModule
{
  val node = TLManagerNode(Seq(TLSlavePortParameters.v1(
    Seq(TLSlaveParameters.v1(
      address     = List(AddressSet(base, size-1)),
      resources   = resources,
      regionType  = RegionType.UNCACHED,
      executable  = executable,
      supportsGet = TransferSizes(1, beatBytes),
      fifoId      = Some(0))),
    beatBytes = beatBytes)))

  lazy val module = new LazyModuleImp(this) {
    val contents = contentsDelayed
    val wrapSize = 1 << log2Ceil(contents.size)
    require (wrapSize <= size)

    val (in, edge) = node.in(0)

    val words = (contents ++ Seq.fill(wrapSize-contents.size)(0.toByte)).grouped(beatBytes).toSeq
    val bigs = words.map(_.foldRight(BigInt(0)){ case (x,y) => (x.toInt & 0xff) | y << 8})
    val rom = Vec(bigs.map(x => UInt(x, width = 8*beatBytes)))

    in.d.valid := in.a.valid
    in.a.ready := in.d.ready

    val index = in.a.bits.address(log2Ceil(wrapSize)-1,log2Ceil(beatBytes))
    val high = if (wrapSize == size) UInt(0) else in.a.bits.address(log2Ceil(size)-1, log2Ceil(wrapSize))
    in.d.bits := edge.AccessAck(in.a.bits, Mux(high.orR, UInt(0), rom(index)))

    // Tie off unused channels
    in.b.valid := Bool(false)
    in.c.ready := Bool(true)
    in.e.ready := Bool(true)
  }
}

case class HangROMLocated(loc: HierarchicalLocation) extends Field[Option[HangROMParams]](None)

object HangROM {
  /** HangROM.attach not only instantiates a TLROM and attaches it to the tilelink interconnect
    *    at a configurable location, but also drives the tiles' reset vectors to point
    *    at its 'hang' address parameter value.
    */
  def attach(params: HangROMParams, subsystem: BaseSubsystem with HasTiles, where: TLBusWrapperLocation)
            (implicit p: Parameters): TLROM = {
    val tlbus = subsystem.locateTLBusWrapper(where)
    val hangROMDomainWrapper = LazyModule(new ClockSinkDomain(take = None))
    hangROMDomainWrapper.clockNode := tlbus.fixedClockNode

    val hangROMResetVectorSourceNode = BundleBridgeSource[UInt]()
    lazy val contents = Seq(0x6f.toByte) ++ Seq.fill(63)(0.toByte)

    val hangrom = hangROMDomainWrapper {
      LazyModule(new TLROM(params.address, 64, contents, true, tlbus.beatBytes))
    }

    hangrom.node := tlbus.coupleTo("hangrom"){ TLFragmenter(tlbus) := _ }
    // Drive the `subsystem` reset vector to the `hang` address of this Hang ROM.
    subsystem.tileResetVectorNexusNode := hangROMResetVectorSourceNode
    InModuleBody {
      hangROMResetVectorSourceNode.bundle := params.address.U
    }
    hangrom
  }
}
