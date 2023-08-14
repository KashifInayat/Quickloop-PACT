// See LICENSE for license details.
package sifive.fpgashells.shell.xilinx

import chisel3._
import chisel3.experimental.{attach, Analog, IO}
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.SyncResetSynchronizerShiftReg
import sifive.fpgashells.clocks._
import sifive.fpgashells.shell._
import sifive.fpgashells.ip.xilinx._
import sifive.blocks.devices.chiplink._
import sifive.fpgashells.devices.xilinx.xilinxsingleEv7mig._
import sifive.fpgashells.devices.xilinx.xdma._
import sifive.fpgashells.ip.xilinx.xxv_ethernet._

class SysClockSingleEv7PlacedOverlay(val shell: SingleEv7ShellBasicOverlays, name: String, val designInput: ClockInputDesignInput, val shellInput: ClockInputShellInput)
  extends LVDSClockInputXilinxPlacedOverlay(name, designInput, shellInput)
{
  val node = shell { ClockSourceNode(freqMHz = 200, jitterPS = 50)(ValName(name)) }

  shell { InModuleBody {
    shell.xdc.addPackagePin(io.p, "N4")
    shell.xdc.addPackagePin(io.n, "N3")
    shell.xdc.addIOStandard(io.p, "DIFF_SSTL15")
    shell.xdc.addIOStandard(io.n, "DIFF_SSTL15")
  } }
}
class SysClockSingleEv7ShellPlacer(shell: SingleEv7ShellBasicOverlays, val shellInput: ClockInputShellInput)(implicit val valName: ValName)
  extends ClockInputShellPlacer[SingleEv7ShellBasicOverlays]
{
    def place(designInput: ClockInputDesignInput) = new SysClockSingleEv7PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class RefClockSingleEv7PlacedOverlay(val shell: SingleEv7ShellBasicOverlays, name: String, val designInput: ClockInputDesignInput, val shellInput: ClockInputShellInput)
  extends LVDSClockInputXilinxPlacedOverlay(name, designInput, shellInput) {
  val node = shell { ClockSourceNode(freqMHz = 125, jitterPS = 50)(ValName(name)) }

  shell { InModuleBody {
    shell.xdc.addPackagePin(io.p, "AY24")
    shell.xdc.addPackagePin(io.n, "AY23")
    shell.xdc.addIOStandard(io.p, "LVDS")
    shell.xdc.addIOStandard(io.n, "LVDS")
  } }
}
class RefClockSingleEv7ShellPlacer(shell: SingleEv7ShellBasicOverlays, val shellInput: ClockInputShellInput)(implicit val valName: ValName)
  extends ClockInputShellPlacer[SingleEv7ShellBasicOverlays] {
  def place(designInput: ClockInputDesignInput) = new RefClockSingleEv7PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class SDIOSingleEv7PlacedOverlay(val shell: SingleEv7ShellBasicOverlays, name: String, val designInput: SPIDesignInput, val shellInput: SPIShellInput)
  extends SDIOXilinxPlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    val packagePinsWithPackageIOs = Seq(("F18", IOPin(io.spi_clk)),
                                        ("E18", IOPin(io.spi_cs)),
                                        ("A19", IOPin(io.spi_dat(0))),
                                        ("C18", IOPin(io.spi_dat(1))),
                                        ("D18", IOPin(io.spi_dat(2))),
                                        ("A18", IOPin(io.spi_dat(3))))
    
    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.xdc.addPackagePin(io, pin)
      shell.xdc.addIOStandard(io, "LVCMOS18")
    } }
    packagePinsWithPackageIOs drop 1 foreach { case (pin, io) => {
      shell.xdc.addPullup(io)
      shell.xdc.addIOB(io)
    } }
  } }
}
class SDIOSingleEv7ShellPlacer(shell: SingleEv7ShellBasicOverlays, val shellInput: SPIShellInput)(implicit val valName: ValName)
  extends SPIShellPlacer[SingleEv7ShellBasicOverlays] {
  def place(designInput: SPIDesignInput) = new SDIOSingleEv7PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class SPIFlashSingleEv7PlacedOverlay(val shell: SingleEv7ShellBasicOverlays, name: String, val designInput: SPIFlashDesignInput, val shellInput: SPIFlashShellInput)
  extends SPIFlashXilinxPlacedOverlay(name, designInput, shellInput)
{

  shell { InModuleBody {
    /*val packagePinsWithPackageIOs = Seq(("AF13", IOPin(io.qspi_sck)),
      ("AJ11", IOPin(io.qspi_cs)),
      ("AP11", IOPin(io.qspi_dq(0))),
      ("AN11", IOPin(io.qspi_dq(1))),
      ("AM11", IOPin(io.qspi_dq(2))),
      ("AL11", IOPin(io.qspi_dq(3))))

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.xdc.addPackagePin(io, pin)
      shell.xdc.addIOStandard(io, "LVCMOS18")
      shell.xdc.addIOB(io)
    } }
    packagePinsWithPackageIOs drop 1 foreach { case (pin, io) => {
      shell.xdc.addPullup(io)
    } }
*/
  } }
}
class SPIFlashSingleEv7ShellPlacer(shell: SingleEv7ShellBasicOverlays, val shellInput: SPIFlashShellInput)(implicit val valName: ValName)
  extends SPIFlashShellPlacer[SingleEv7ShellBasicOverlays] {
  def place(designInput: SPIFlashDesignInput) = new SPIFlashSingleEv7PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class UARTSingleEv7PlacedOverlay(val shell: SingleEv7ShellBasicOverlays, name: String, val designInput: UARTDesignInput, val shellInput: UARTShellInput)
  extends UARTXilinxPlacedOverlay(name, designInput, shellInput, false)
{
  shell { InModuleBody {
    val packagePinsWithPackageIOs = Seq(("R37", IOPin(io.rxd)),
                                        ("R36", IOPin(io.txd)))

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.xdc.addPackagePin(io, pin)
      shell.xdc.addIOStandard(io, "LVCMOS18")
      shell.xdc.addIOB(io)
    } }
  } }
}
class UARTSingleEv7ShellPlacer(shell: SingleEv7ShellBasicOverlays, val shellInput: UARTShellInput)(implicit val valName: ValName)
  extends UARTShellPlacer[SingleEv7ShellBasicOverlays] {
  def place(designInput: UARTDesignInput) = new UARTSingleEv7PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class QSFP1SingleEv7PlacedOverlay(val shell: SingleEv7ShellBasicOverlays, name: String, val designInput: EthernetDesignInput, val shellInput: EthernetShellInput)
  extends EthernetUltraScalePlacedOverlay(name, designInput, shellInput, XXVEthernetParams(name = name, speed   = 10, dclkMHz = 125))
{
  val dclkSource = shell { BundleBridgeSource(() => Clock()) }
  val dclkSink = dclkSource.makeSink()
  InModuleBody {
    dclk := dclkSink.bundle
  }
  shell { InModuleBody {
    dclkSource.bundle := shell.ref_clock.get.get.overlayOutput.node.out(0)._1.clock
    shell.xdc.addPackagePin(io.tx_p, "V7")
    shell.xdc.addPackagePin(io.tx_n, "V6")
    shell.xdc.addPackagePin(io.rx_p, "Y2")
    shell.xdc.addPackagePin(io.rx_n, "Y1")
    shell.xdc.addPackagePin(io.refclk_p, "W9")
    shell.xdc.addPackagePin(io.refclk_n, "W8")
  } }
}
class QSFP1SingleEv7ShellPlacer(shell: SingleEv7ShellBasicOverlays, val shellInput: EthernetShellInput)(implicit val valName: ValName)
  extends EthernetShellPlacer[SingleEv7ShellBasicOverlays] {
  def place(designInput: EthernetDesignInput) = new QSFP1SingleEv7PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class QSFP2SingleEv7PlacedOverlay(val shell: SingleEv7ShellBasicOverlays, name: String, val designInput: EthernetDesignInput, val shellInput: EthernetShellInput)
  extends EthernetUltraScalePlacedOverlay(name, designInput, shellInput, XXVEthernetParams(name = name, speed   = 10, dclkMHz = 125))
{
  val dclkSource = shell { BundleBridgeSource(() => Clock()) }
  val dclkSink = dclkSource.makeSink()
  InModuleBody {
    dclk := dclkSink.bundle
  }
  shell { InModuleBody {
    dclkSource.bundle := shell.ref_clock.get.get.overlayOutput.node.out(0)._1.clock
    shell.xdc.addPackagePin(io.tx_p, "L5")
    shell.xdc.addPackagePin(io.tx_n, "L4")
    shell.xdc.addPackagePin(io.rx_p, "T2")
    shell.xdc.addPackagePin(io.rx_n, "T1")
    shell.xdc.addPackagePin(io.refclk_p, "R9")
    shell.xdc.addPackagePin(io.refclk_n, "R8")
  } }
}
class QSFP2SingleEv7ShellPlacer(shell: SingleEv7ShellBasicOverlays, val shellInput: EthernetShellInput)(implicit val valName: ValName)
  extends EthernetShellPlacer[SingleEv7ShellBasicOverlays] {
  def place(designInput: EthernetDesignInput) = new QSFP2SingleEv7PlacedOverlay(shell, valName.name, designInput, shellInput)
}

object LEDSingleEv7PinConstraints {
  val pins = Seq("AT32", "AV34", "AY30", "BB32", "BF32", "AU37", "AV36", "BA37")
}
class LEDSingleEv7PlacedOverlay(val shell: SingleEv7ShellBasicOverlays, name: String, val designInput: LEDDesignInput, val shellInput: LEDShellInput)
  extends LEDXilinxPlacedOverlay(name, designInput, shellInput, packagePin = Some(LEDSingleEv7PinConstraints.pins(shellInput.number)), ioStandard = "LVCMOS12")
class LEDSingleEv7ShellPlacer(shell: SingleEv7ShellBasicOverlays, val shellInput: LEDShellInput)(implicit val valName: ValName)
  extends LEDShellPlacer[SingleEv7ShellBasicOverlays] {
  def place(designInput: LEDDesignInput) = new LEDSingleEv7PlacedOverlay(shell, valName.name, designInput, shellInput)
}

object ButtonSingleEv7PinConstraints {
  val pins = Seq("BB24", "BE23", "BF22", "BE22", "BD23")
}
class ButtonSingleEv7PlacedOverlay(val shell: SingleEv7ShellBasicOverlays, name: String, val designInput: ButtonDesignInput, val shellInput: ButtonShellInput)
  extends ButtonXilinxPlacedOverlay(name, designInput, shellInput, packagePin = Some(ButtonSingleEv7PinConstraints.pins(shellInput.number)), ioStandard = "LVCMOS18")
class ButtonSingleEv7ShellPlacer(shell: SingleEv7ShellBasicOverlays, val shellInput: ButtonShellInput)(implicit val valName: ValName)
  extends ButtonShellPlacer[SingleEv7ShellBasicOverlays] {
  def place(designInput: ButtonDesignInput) = new ButtonSingleEv7PlacedOverlay(shell, valName.name, designInput, shellInput)
}

object SwitchSingleEv7PinConstraints {
  val pins = Seq("B17", "G16", "J16", "D21")
}
class SwitchSingleEv7PlacedOverlay(val shell: SingleEv7ShellBasicOverlays, name: String, val designInput: SwitchDesignInput, val shellInput: SwitchShellInput)
  extends SwitchXilinxPlacedOverlay(name, designInput, shellInput, packagePin = Some(SwitchSingleEv7PinConstraints.pins(shellInput.number)), ioStandard = "LVCMOS12")
class SwitchSingleEv7ShellPlacer(shell: SingleEv7ShellBasicOverlays, val shellInput: SwitchShellInput)(implicit val valName: ValName)
  extends SwitchShellPlacer[SingleEv7ShellBasicOverlays] {
  def place(designInput: SwitchDesignInput) = new SwitchSingleEv7PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class ChipLinkSingleEv7PlacedOverlay(val shell: SingleEv7ShellBasicOverlays, name: String, val designInput: ChipLinkDesignInput, val shellInput: ChipLinkShellInput)
  extends ChipLinkXilinxPlacedOverlay(name, designInput, shellInput, rxPhase= -120, txPhase= -90, rxMargin=0.6, txMargin=0.5)
{
  val ereset_n = shell { InModuleBody {
    val ereset_n = IO(Analog(1.W))
    ereset_n.suggestName("ereset_n")
    val pin = IOPin(ereset_n, 0)
    shell.xdc.addPackagePin(pin, "BC8")
    shell.xdc.addIOStandard(pin, "LVCMOS18")
    shell.xdc.addTermination(pin, "NONE")
    shell.xdc.addPullup(pin)

    val iobuf = Module(new IOBUF)
    iobuf.suggestName("chiplink_ereset_iobuf")
    attach(ereset_n, iobuf.io.IO)
    iobuf.io.T := true.B // !oe
    iobuf.io.I := false.B

    iobuf.io.O
  } }

  shell { InModuleBody {
    val dir1 = Seq("BC9", "AV8", "AV9", /* clk, rst, send */
                   "AY9",  "BA9",  "BF10", "BF9",  "BC11", "BD11", "BD12", "BE12",
                   "BF12", "BF11", "BE14", "BF14", "BD13", "BE13", "BC15", "BD15",
                   "BE15", "BF15", "BA14", "BB14", "BB13", "BB12", "BA16", "BA15",
                   "BC14", "BC13", "AY8",  "AY7",  "AW8",  "AW7",  "BB16", "BC16")
    val dir2 = Seq("AV14", "AK13", "AK14", /* clk, rst, send */
                   "AR14", "AT14", "AP12", "AR12", "AW12", "AY12", "AW11", "AY10",
                   "AU11", "AV11", "AW13", "AY13", "AN16", "AP16", "AP13", "AR13",
                   "AT12", "AU12", "AK15", "AL15", "AL14", "AM14", "AV10", "AW10",
                   "AN15", "AP15", "AK12", "AL12", "AM13", "AM12", "AJ13", "AJ12")
    (IOPin.of(io.b2c) zip dir1) foreach { case (io, pin) => shell.xdc.addPackagePin(io, pin) }
    (IOPin.of(io.c2b) zip dir2) foreach { case (io, pin) => shell.xdc.addPackagePin(io, pin) }
  } }
}
class ChipLinkSingleEv7ShellPlacer(shell: SingleEv7ShellBasicOverlays, val shellInput: ChipLinkShellInput)(implicit val valName: ValName)
  extends ChipLinkShellPlacer[SingleEv7ShellBasicOverlays] {
  def place(designInput: ChipLinkDesignInput) = new ChipLinkSingleEv7PlacedOverlay(shell, valName.name, designInput, shellInput)
}

// TODO: JTAG is untested
class JTAGDebugSingleEv7PlacedOverlay(val shell: SingleEv7ShellBasicOverlays, name: String, val designInput: JTAGDebugDesignInput, val shellInput: JTAGDebugShellInput)
  extends JTAGDebugXilinxPlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    val pin_locations = Map(
      "PMOD_J52" -> Seq("AW15",      "AU16",      "AV16",      "AY14",      "AY15"),
      "PMOD_J53" -> Seq( "N30",       "L31",       "P29",       "N28",       "M30"),
      "FMC_J2"   -> Seq("AL12",      "AN15",      "AP15",      "AM12",      "AK12"))
    val pins      = Seq(io.jtag_TCK, io.jtag_TMS, io.jtag_TDI, io.jtag_TDO, io.srst_n)

    shell.sdc.addClock("JTCK", IOPin(io.jtag_TCK), 10)
    shell.sdc.addGroup(clocks = Seq("JTCK"))
    shell.xdc.clockDedicatedRouteFalse(IOPin(io.jtag_TCK))

    val pin_voltage:String = if(shellInput.location.get == "PMOD_J53") "LVCMOS12" else "LVCMOS18"

    (pin_locations(shellInput.location.get) zip pins) foreach { case (pin_location, ioport) =>
      val io = IOPin(ioport)
      shell.xdc.addPackagePin(io, pin_location)
      shell.xdc.addIOStandard(io, pin_voltage)
      shell.xdc.addPullup(io)
      shell.xdc.addIOB(io)
    }
  } }
}
class JTAGDebugSingleEv7ShellPlacer(shell: SingleEv7ShellBasicOverlays, val shellInput: JTAGDebugShellInput)(implicit val valName: ValName)
  extends JTAGDebugShellPlacer[SingleEv7ShellBasicOverlays] {
  def place(designInput: JTAGDebugDesignInput) = new JTAGDebugSingleEv7PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class cJTAGDebugSingleEv7PlacedOverlay(val shell: SingleEv7ShellBasicOverlays, name: String, val designInput: cJTAGDebugDesignInput, val shellInput: cJTAGDebugShellInput)
  extends cJTAGDebugXilinxPlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    shell.sdc.addClock("JTCKC", IOPin(io.cjtag_TCKC), 10)
    shell.sdc.addGroup(clocks = Seq("JTCKC"))
    shell.xdc.clockDedicatedRouteFalse(IOPin(io.cjtag_TCKC))
    val packagePinsWithPackageIOs = Seq(("AW11", IOPin(io.cjtag_TCKC)),
                                        ("AP13", IOPin(io.cjtag_TMSC)),
                                        ("AY10", IOPin(io.srst_n)))

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.xdc.addPackagePin(io, pin)
      shell.xdc.addIOStandard(io, "LVCMOS18")
    } }
      shell.xdc.addPullup(IOPin(io.cjtag_TCKC))
      shell.xdc.addPullup(IOPin(io.srst_n))
  } }
}
class cJTAGDebugSingleEv7ShellPlacer(shell: SingleEv7ShellBasicOverlays, val shellInput: cJTAGDebugShellInput)(implicit val valName: ValName)
  extends cJTAGDebugShellPlacer[SingleEv7ShellBasicOverlays] {
  def place(designInput: cJTAGDebugDesignInput) = new cJTAGDebugSingleEv7PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class JTAGDebugBScanSingleEv7PlacedOverlay(val shell: SingleEv7ShellBasicOverlays, name: String, val designInput: JTAGDebugBScanDesignInput, val shellInput: JTAGDebugBScanShellInput)
  extends JTAGDebugBScanXilinxPlacedOverlay(name, designInput, shellInput)
class JTAGDebugBScanSingleEv7ShellPlacer(val shell: SingleEv7ShellBasicOverlays, val shellInput: JTAGDebugBScanShellInput)(implicit val valName: ValName)
  extends JTAGDebugBScanShellPlacer[SingleEv7ShellBasicOverlays] {
  def place(designInput: JTAGDebugBScanDesignInput) = new JTAGDebugBScanSingleEv7PlacedOverlay(shell, valName.name, designInput, shellInput)
}

case object SingleEv7DDRSize extends Field[BigInt](0x40000000L * 8) // 2GB
class DDRSingleEv7PlacedOverlay(val shell: SingleEv7ShellBasicOverlays, name: String, val designInput: DDRDesignInput, val shellInput: DDRShellInput)
  extends DDRPlacedOverlay[XilinxSingleEv7MIGPads](name, designInput, shellInput)
{
  val size = p(SingleEv7DDRSize)

  val migParams = XilinxSingleEv7MIGParams(address = AddressSet.misaligned(di.baseAddress, size))
  val mig = LazyModule(new XilinxSingleEv7MIG(migParams))
  val ioNode = BundleBridgeSource(() => mig.module.io.cloneType)
  val topIONode = shell { ioNode.makeSink() }
  val ddrUI     = shell { ClockSourceNode(freqMHz = 200) }
  val areset    = shell { ClockSinkNode(Seq(ClockSinkParameters())) }
  areset := designInput.wrangler := ddrUI

  def overlayOutput = DDROverlayOutput(ddr = mig.node)
  def ioFactory = new XilinxSingleEv7MIGPads(size)

  InModuleBody { ioNode.bundle <> mig.module.io }

  shell { InModuleBody {
    require (shell.sys_clock.get.isDefined, "Use of DDRSingleEv7Overlay depends on SysClockSingleEv7Overlay")
    val (sys, _) = shell.sys_clock.get.get.overlayOutput.node.out(0)
    val (ui, _) = ddrUI.out(0)
    val (ar, _) = areset.in(0)
    val port = topIONode.bundle.port
    io <> port
    ui.clock := port.ui_clk
    ui.reset := /*!port.mmcm_locked ||*/ port.ui_clk_sync_rst
    port.sys_clk_i := sys.clock.asUInt
    port.sys_rst := sys.reset // pllReset
    port.aresetn := !ar.reset
  } }

  shell.sdc.addGroup(pins = Seq(mig.island.module.blackbox.io.ui_clk))
}
class DDRSingleEv7ShellPlacer(shell: SingleEv7ShellBasicOverlays, val shellInput: DDRShellInput)(implicit val valName: ValName)
  extends DDRShellPlacer[SingleEv7ShellBasicOverlays] {
  def place(designInput: DDRDesignInput) = new DDRSingleEv7PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class PCIeSingleEv7FMCPlacedOverlay(val shell: SingleEv7ShellBasicOverlays, name: String, val designInput: PCIeDesignInput, val shellInput: PCIeShellInput)
  extends PCIeUltraScalePlacedOverlay(name, designInput, shellInput, XDMAParams(
    name     = "fmc_xdma",
    location = "X0Y3",
    bars     = designInput.bars,
    control  = designInput.ecam,
    bases    = designInput.bases,
    lanes    = 4))
{
  shell { InModuleBody {
    // Work-around incorrectly pre-assigned pins
    IOPin.of(io).foreach { shell.xdc.addPackagePin(_, "") }

    // We need some way to connect both of these to reach x8
    val ref126 = Seq("V38",  "V39")  /* [pn] GBT0 Bank 126 */
    val ref121 = Seq("AK38", "AK39") /* [pn] GBT0 Bank 121 */
    val ref = ref126

    // Bank 126 (DP5, DP6, DP4, DP7), Bank 121 (DP3, DP2, DP1, DP0)
    val rxp = Seq("U45", "R45", "W45", "N45", "AJ45", "AL45", "AN45", "AR45") /* [0-7] */
    val rxn = Seq("U46", "R46", "W46", "N46", "AJ46", "AL46", "AN46", "AR46") /* [0-7] */
    val txp = Seq("P42", "M42", "T42", "K42", "AL40", "AM42", "AP42", "AT42") /* [0-7] */
    val txn = Seq("P43", "M43", "T43", "K43", "AL41", "AM43", "AP43", "AT43") /* [0-7] */

    def bind(io: Seq[IOPin], pad: Seq[String]) {
      (io zip pad) foreach { case (io, pad) => shell.xdc.addPackagePin(io, pad) }
    }

    bind(IOPin.of(io.refclk), ref)
    // We do these individually so that zip falls off the end of the lanes:
    bind(IOPin.of(io.lanes.pci_exp_txp), txp)
    bind(IOPin.of(io.lanes.pci_exp_txn), txn)
    bind(IOPin.of(io.lanes.pci_exp_rxp), rxp)
    bind(IOPin.of(io.lanes.pci_exp_rxn), rxn)
  } }
}
class PCIeSingleEv7FMCShellPlacer(shell: SingleEv7ShellBasicOverlays, val shellInput: PCIeShellInput)(implicit val valName: ValName)
  extends PCIeShellPlacer[SingleEv7ShellBasicOverlays] {
  def place(designInput: PCIeDesignInput) = new PCIeSingleEv7FMCPlacedOverlay(shell, valName.name, designInput, shellInput)
}

class PCIeSingleEv7EdgePlacedOverlay(val shell: SingleEv7ShellBasicOverlays, name: String, val designInput: PCIeDesignInput, val shellInput: PCIeShellInput)
  extends PCIeUltraScalePlacedOverlay(name, designInput, shellInput, XDMAParams(
    name     = "edge_xdma",
    location = "X1Y2",
    bars     = designInput.bars,
    control  = designInput.ecam,
    bases    = designInput.bases,
    lanes    = 8))
{
  shell { InModuleBody {
    // Work-around incorrectly pre-assigned pins
    IOPin.of(io).foreach { shell.xdc.addPackagePin(_, "") }

    // PCIe Edge connector U2
    //   Lanes 00-03 Bank 227
    //   Lanes 04-07 Bank 226
    //   Lanes 08-11 Bank 225
    //   Lanes 12-15 Bank 224

    // FMC+ J22
    val ref227 = Seq("AC9", "AC8")  /* [pn]  Bank 227 PCIE_CLK2_*/
    val ref = ref227

    // PCIe Edge connector U2 : Bank 227, 226
    val rxp = Seq("AA4", "AB2", "AC4", "AD2", "AE4", "AF2", "AG4", "AH2") // [0-7]
    val rxn = Seq("AA3", "AB1", "AC3", "AD1", "AE3", "AF1", "AG3", "AH1") // [0-7]
    val txp = Seq("Y7", "AB7", "AD7", "AF7", "AH7", "AK7", "AM7", "AN5") // [0-7]
    val txn = Seq("Y6", "AB6", "AD6", "AF6", "AH6", "AK6", "AM6", "AN4") // [0-7]

    def bind(io: Seq[IOPin], pad: Seq[String]) {
      (io zip pad) foreach { case (io, pad) => shell.xdc.addPackagePin(io, pad) }
    }

    bind(IOPin.of(io.refclk), ref)
    // We do these individually so that zip falls off the end of the lanes:
    bind(IOPin.of(io.lanes.pci_exp_txp), txp)
    bind(IOPin.of(io.lanes.pci_exp_txn), txn)
    bind(IOPin.of(io.lanes.pci_exp_rxp), rxp)
    bind(IOPin.of(io.lanes.pci_exp_rxn), rxn)
  } }
}
class PCIeSingleEv7EdgeShellPlacer(shell: SingleEv7ShellBasicOverlays, val shellInput: PCIeShellInput)(implicit val valName: ValName)
  extends PCIeShellPlacer[SingleEv7ShellBasicOverlays] {
  def place(designInput: PCIeDesignInput) = new PCIeSingleEv7EdgePlacedOverlay(shell, valName.name, designInput, shellInput)
}

abstract class SingleEv7ShellBasicOverlays()(implicit p: Parameters) extends UltraScaleShell{
  // PLL reset causes
  val pllReset = InModuleBody { Wire(Bool()) }

  val sys_clock = Overlay(ClockInputOverlayKey, new SysClockSingleEv7ShellPlacer(this, ClockInputShellInput()))
  val ref_clock = Overlay(ClockInputOverlayKey, new RefClockSingleEv7ShellPlacer(this, ClockInputShellInput()))
  val led       = Seq.tabulate(8)(i => Overlay(LEDOverlayKey, new LEDSingleEv7ShellPlacer(this, LEDShellInput(color = "red", number = i))(valName = ValName(s"led_$i"))))
  val switch    = Seq.tabulate(4)(i => Overlay(SwitchOverlayKey, new SwitchSingleEv7ShellPlacer(this, SwitchShellInput(number = i))(valName = ValName(s"switch_$i"))))
  val button    = Seq.tabulate(5)(i => Overlay(ButtonOverlayKey, new ButtonSingleEv7ShellPlacer(this, ButtonShellInput(number = i))(valName = ValName(s"button_$i"))))
  val ddr       = Overlay(DDROverlayKey, new DDRSingleEv7ShellPlacer(this, DDRShellInput()))
  val qsfp1     = Overlay(EthernetOverlayKey, new QSFP1SingleEv7ShellPlacer(this, EthernetShellInput()))
  val qsfp2     = Overlay(EthernetOverlayKey, new QSFP2SingleEv7ShellPlacer(this, EthernetShellInput()))
  val chiplink  = Overlay(ChipLinkOverlayKey, new ChipLinkSingleEv7ShellPlacer(this, ChipLinkShellInput()))
  //val spi_flash = Overlay(SPIFlashOverlayKey, new SPIFlashSingleEv7ShellPlacer(this, SPIFlashShellInput()))
  //SPI Flash not functional
}

case object SingleEv7ShellPMOD extends Field[String]("JTAG")
case object SingleEv7ShellPMOD2 extends Field[String]("JTAG")

class WithSingleEv7ShellPMOD(device: String) extends Config((site, here, up) => {
  case SingleEv7ShellPMOD => device
})

// Change JTAG pinouts to SingleEv7 J53
// Due to the level shifter is from 1.2V to 3.3V, the frequency of JTAG should be slow down to 1Mhz
class WithSingleEv7ShellPMOD2(device: String) extends Config((site, here, up) => {
  case SingleEv7ShellPMOD2 => device
})

class WithSingleEv7ShellPMODJTAG extends WithSingleEv7ShellPMOD("JTAG")
class WithSingleEv7ShellPMODSDIO extends WithSingleEv7ShellPMOD("SDIO")

// Reassign JTAG pinouts location to PMOD J53
class WithSingleEv7ShellPMOD2JTAG extends WithSingleEv7ShellPMOD2("PMODJ53_JTAG")

class SingleEv7Shell()(implicit p: Parameters) extends SingleEv7ShellBasicOverlays
{
  val pmod_is_sdio  = p(SingleEv7ShellPMOD) == "SDIO"
  val pmod_j53_is_jtag = p(SingleEv7ShellPMOD2) == "PMODJ53_JTAG"
  val jtag_location = Some(if (pmod_is_sdio) (if (pmod_j53_is_jtag) "PMOD_J53" else "FMC_J2") else "PMOD_J52")

  // Order matters; ddr depends on sys_clock
  val uart      = Overlay(UARTOverlayKey, new UARTSingleEv7ShellPlacer(this, UARTShellInput()))
  val sdio      = if (pmod_is_sdio) Some(Overlay(SPIOverlayKey, new SDIOSingleEv7ShellPlacer(this, SPIShellInput()))) else None
  val jtag      = Overlay(JTAGDebugOverlayKey, new JTAGDebugSingleEv7ShellPlacer(this, JTAGDebugShellInput(location = jtag_location)))
  val cjtag     = Overlay(cJTAGDebugOverlayKey, new cJTAGDebugSingleEv7ShellPlacer(this, cJTAGDebugShellInput()))
  val jtagBScan = Overlay(JTAGDebugBScanOverlayKey, new JTAGDebugBScanSingleEv7ShellPlacer(this, JTAGDebugBScanShellInput()))
  val fmc       = Overlay(PCIeOverlayKey, new PCIeSingleEv7FMCShellPlacer(this, PCIeShellInput()))
  val edge      = Overlay(PCIeOverlayKey, new PCIeSingleEv7EdgeShellPlacer(this, PCIeShellInput()))

  val topDesign = LazyModule(p(DesignKey)(designParameters))

  // Place the sys_clock at the Shell if the user didn't ask for it
  designParameters(ClockInputOverlayKey).foreach { unused =>
    val source = unused.place(ClockInputDesignInput()).overlayOutput.node
    val sink = ClockSinkNode(Seq(ClockSinkParameters()))
    sink := source
  }

  override lazy val module = new LazyRawModuleImp(this) {
    val reset = IO(Input(Bool()))
    xdc.addPackagePin(reset, "AH42")
    xdc.addIOStandard(reset, "LVCMOS18")

    val reset_ibuf = Module(new IBUF)
    reset_ibuf.io.I := reset

    val sysclk: Clock = sys_clock.get() match {
      case Some(x: SysClockSingleEv7PlacedOverlay) => x.clock
    }

    val powerOnReset: Bool = PowerOnResetFPGAOnly(sysclk)
    sdc.addAsyncPath(Seq(powerOnReset))

    val ereset: Bool = chiplink.get() match {
      case Some(x: ChipLinkSingleEv7PlacedOverlay) => !x.ereset_n
      case _ => false.B
    }

    pllReset := (reset_ibuf.io.O || powerOnReset || ereset)
  }
}
