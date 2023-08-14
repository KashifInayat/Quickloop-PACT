// See LICENSE for license details.
package chipyard.fpga.singleEv7

import freechips.rocketchip.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy.{DTSModel, DTSTimebase}
import freechips.rocketchip.system._
import freechips.rocketchip.tile._
import freechips.rocketchip.diplomacy._
import gemmini._

import sifive.blocks.devices.uart._
import sifive.blocks.devices.spi._
import sifive.blocks.devices.gpio._
import sifive.fpgashells.devices.xilinx.xilinxsingleEv7mig._

import testchipip.{SerialTLKey}

import chipyard.{BuildSystem}

case object FPGAFrequencyKey extends Field[Double](100.0)

class WithFPGAFrequency(MHz: Double) extends Config((site, here, up) => {
  case FPGAFrequencyKey => MHz
  case PeripheryBusKey => up(PeripheryBusKey).copy(dtsFrequency = Some(BigDecimal(1000000*MHz).setScale(0, BigDecimal.RoundingMode.HALF_UP).toBigInt))
  case MemoryBusKey => up(MemoryBusKey).copy(dtsFrequency = Some(BigDecimal(1000000*MHz).setScale(0, BigDecimal.RoundingMode.HALF_UP).toBigInt))
  case RocketTilesKey => up(RocketTilesKey) map { r =>
    r.copy(core = r.core.copy(bootFreqHz = BigDecimal(1000000*MHz).setScale(0, BigDecimal.RoundingMode.HALF_UP).toBigInt))
  }
})

class WithSystemModifications extends Config((site, here, up) => {
  case BuildSystem => (p: Parameters) => new CustomDigitalTop()(p)
  case DTSTimebase => BigInt((1000).toLong)
  case BootROMLocated(x) => None
  case HangROMLocated(x) => Some(HangROMParams(address = 0x10000))
  //case SubsystemExternalResetVectorKey => true
  //case MaskROMLocated(x) => List(MaskROMParams(address = 0x10000, name = "maskrom", depth = 8*1024))
  //case ExtMem => up(ExtMem, site).map(x => x.copy(master = x.master.copy(base = BigInt(0x800000L), size = 0x100))) // set extmem to DDR size
  case ExtMem => None
  case MemoryXilinxDDRKey => XilinxSingleEv7MIGParams((AddressSet.misaligned(0x80000000L,(1L << 33))))
  //case TLScratchpadKey => List(TLScratchpadParams(0x8000000L, 0x20000-1), TLScratchpadParams(0x80000000L, 0x10000-1))
})


class WithDefaultPeripherals extends Config((site, here, up) => {
  case PeripheryUARTKey => List(
    UARTParams(address = 0x10013000))
  //case PeripheryGPIOKey => List(
  //  GPIOParams(address = 0x10012000, width = 32, includeIOF = true))
  case PeripherySPIKey => List(
    SPIParams(rAddress = BigInt(0x10034000), txDepth = 16, rxDepth = 16))  
  case DTSTimebase => BigInt(1000)
  case JtagDTMKey => new JtagDTMConfig (
    idcodeVersion = 2,
    idcodePartNum = 0x000,
    idcodeManufId = 0x489,
    debugIdleCycles = 5)
  case SerialTLKey => None // remove serialized tl port
})
// DOC include start: AbstractSingleEv7 and Rocket
class WithSingleEv7Tweaks extends Config(
  new freechips.rocketchip.subsystem.WithoutTLMonitors ++
  new WithFPGAFrequency(20) ++
  //new chipyard.config.WithTLBackingMemory ++
  //new testchipip.WithBackingScratchpad(base = 0x8000000L, mask = 0x10000 - 1) ++
  new WithSystemModifications ++
  new WithRTCTick ++
  //new WithGPIOOff ++
  new WithSingleEv7JTAGHarnessBinder ++
  new WithDDRMIGHarnessBinder ++
  new WithSingleEv7SPIHarnessBinder ++
  new WithSingleEv7UARTHarnessBinder ++
  new WithSingleEv7ResetHarnessBinder ++
  new WithRTCTickPassthrough ++
  //new WithGPIOIOPassthrough ++
  new WithSPIIOPassthrough ++
  new WithDDRMIGPassthrough ++
  new WithDebugResetPassthrough ++
  new WithDefaultPeripherals ++
  new freechips.rocketchip.subsystem.WithNBreakpoints(2))
  
object GemminiParameters{
  def applyDSE0() = {
	val dp = GemminiConfigs.defaultConfig
	val meshRC = sys.env.get("meshRC").map(x => 4 << x.toInt).getOrElse(dp.meshRows)
	val acc_capacity = sys.env.get("acc_factor").map(x => CapacityInMatrices(32 << x.toInt)).getOrElse(dp.acc_capacity)
	val sp_capacity = sys.env.get("sp_factor").map(x => CapacityInMatrices((2 << x.toInt) * sys.env.get("acc_factor").map(x => 32 << x.toInt).getOrElse(2))).getOrElse(dp.sp_capacity)
	val has_training_convs = sys.env.get("has_training_convs").map(_ == "1").getOrElse(dp.has_training_convs)
	val has_max_pool = sys.env.get("has_max_pool").map(_ == "1").getOrElse(dp.has_max_pool)
	val has_nonlinear_activations = sys.env.get("has_nonlinear_activations").map(_ == "1").getOrElse(dp.has_nonlinear_activations)
	val header = sys.env.get("gemmini_header").getOrElse(dp.headerFileName)  
	val config = GemminiConfigs.defaultConfig.copy(
    meshRows = meshRC,
    meshColumns = meshRC,
    sp_capacity = sp_capacity,
    acc_capacity = acc_capacity,
    has_training_convs = has_training_convs,
    has_max_pool = has_max_pool,
    has_nonlinear_activations = has_nonlinear_activations,
    headerFileName = header,
    )
    println(s"""+-------------------------+
               |meshRows: ${config.meshRows}
               |sp_capacity: ${config.sp_capacity}
               |acc_capacity: ${config.acc_capacity}
               |has_training_convs: ${config.has_training_convs}
               |has_max_pool: ${config.has_max_pool}
               |has_nonlinear_activations: ${config.has_nonlinear_activations}
               |gemmini_header $header:
               |+-------------------------+""".stripMargin)
    config
  }
  def applyDSE1() = {
	val dp = GemminiConfigs.defaultConfig
	val meshRC = sys.env.get("meshRC").map(x => 4 << x.toInt).getOrElse(dp.meshRows)
	val acc_capacity = sys.env.get("acc_factor").map(x => CapacityInMatrices(32 << x.toInt)).getOrElse(dp.acc_capacity)
	val sp_capacity = sys.env.get("sp_factor").map(x => CapacityInMatrices((2 << x.toInt) * sys.env.get("acc_factor").map(x => 32 << x.toInt).getOrElse(2))).getOrElse(dp.sp_capacity) 
	val config = GemminiConfigs.defaultConfig.copy(
    meshRows = meshRC,
    meshColumns = meshRC,
    sp_capacity = sp_capacity,
    acc_capacity = acc_capacity,
    )
    println(s"""+-------------------------+
               |meshRows: ${config.meshRows}
               |sp_capacity: ${config.sp_capacity}
               |acc_capacity: ${config.acc_capacity}
               |+-------------------------+""".stripMargin)
    config
  }
  def applyDSE2() = {
	val dp = GemminiConfigs.defaultConfig
	
	val sp_singleported = sys.env.get("sp_singleported").map(_ == "1").getOrElse(dp.sp_singleported)
	val acc_singleported = sys.env.get("acc_singleported").map(_ == "1").getOrElse(dp.acc_singleported)
	val mvin_scale_shared = sys.env.get("mvin_scale_shared").map(_ == "1").getOrElse(dp.mvin_scale_shared)
	val acc_read_full_width = sys.env.get("acc_read_full_width").map(_ == "1").getOrElse(dp.acc_read_full_width)
	val acc_read_small_width = sys.env.get("acc_read_small_width").map(_ == "1").getOrElse(dp.acc_read_small_width)
	val use_dedicated_tl_port = sys.env.get("use_dedicated_tl_port").map(_ == "1").getOrElse(dp.use_dedicated_tl_port)
	val use_tlb_register_filter = sys.env.get("use_tlb_register_filter").map(_ == "1").getOrElse(dp.use_tlb_register_filter)
	val ex_read_from_spad = sys.env.get("ex_read_from_spad").map(_ == "1").getOrElse(dp.ex_read_from_spad)
	val ex_read_from_acc = sys.env.get("ex_read_from_acc").map(_ == "1").getOrElse(dp.ex_read_from_acc)
	val ex_write_to_spad = sys.env.get("ex_write_to_spad").map(_ == "1").getOrElse(dp.ex_write_to_spad)
	val ex_write_to_acc = sys.env.get("ex_write_to_acc").map(_ == "1").getOrElse(dp.ex_write_to_acc)
	val hardcode_d_to_garbage_addr = sys.env.get("hardcode_d_to_garbage_addr").map(_ == "1").getOrElse(dp.hardcode_d_to_garbage_addr)
	val use_shared_tlb = sys.env.get("use_shared_tlb").map(_ == "1").getOrElse(dp.use_shared_tlb)
	val use_tree_reduction_if_possible = sys.env.get("use_tree_reduction_if_possible").map(_ == "1").getOrElse(dp.use_tree_reduction_if_possible)
	val has_training_convs = sys.env.get("has_training_convs").map(_ == "1").getOrElse(dp.has_training_convs)
	val has_max_pool = sys.env.get("has_max_pool").map(_ == "1").getOrElse(dp.has_max_pool)
	val has_nonlinear_activations = sys.env.get("has_nonlinear_activations").map(_ == "1").getOrElse(dp.has_nonlinear_activations)
	val has_first_layer_optimizations = sys.env.get("has_first_layer_optimizations").map(_ == "1").getOrElse(dp.has_first_layer_optimizations)
	val use_shared_ext_mem = sys.env.get("use_shared_ext_mem").map(_ == "1").getOrElse(dp.use_shared_ext_mem)
	val clock_gate = sys.env.get("clock_gate").map(_ == "1").getOrElse(dp.clock_gate)

	val config = GemminiConfigs.defaultConfig.copy(
    sp_singleported = sp_singleported,
    acc_singleported = acc_singleported,
    mvin_scale_shared = mvin_scale_shared,
    acc_read_full_width = acc_read_full_width,
    acc_read_small_width = acc_read_small_width,
    use_dedicated_tl_port = use_dedicated_tl_port,
    use_tlb_register_filter = use_tlb_register_filter,
    ex_read_from_spad = ex_read_from_spad,
    ex_read_from_acc = ex_read_from_acc,
    ex_write_to_spad = ex_write_to_spad,
    ex_write_to_acc = ex_write_to_acc,
    hardcode_d_to_garbage_addr = hardcode_d_to_garbage_addr,
    use_shared_tlb = use_shared_tlb,
    use_tree_reduction_if_possible = use_tree_reduction_if_possible,
    has_training_convs = has_training_convs,
    has_max_pool = has_max_pool,
    has_nonlinear_activations = has_nonlinear_activations,
    has_first_layer_optimizations = has_first_layer_optimizations,
    use_shared_ext_mem = use_shared_ext_mem,
    clock_gate = clock_gate,
    )
    println(s"""+-------------------------+
               |sp_singleported: ${config.sp_singleported}
               |acc_singleported: ${config.acc_singleported}
               |mvin_scale_shared: ${config.mvin_scale_shared}
               |acc_read_full_width: ${config.acc_read_full_width}
               |acc_read_small_width: ${config.acc_read_small_width}
               |use_dedicated_tl_port: ${config.use_dedicated_tl_port}
               |use_tlb_register_filter: ${config.use_tlb_register_filter}
               |ex_read_from_spad: ${config.ex_read_from_spad}
               |ex_read_from_acc: ${config.ex_read_from_acc}
               |ex_write_to_spad: ${config.ex_write_to_spad}
               |ex_write_to_acc: ${config.ex_write_to_acc}
               |hardcode_d_to_garbage_addr: ${config.hardcode_d_to_garbage_addr}
               |use_shared_tlb: ${config.use_shared_tlb}
               |use_tree_reduction_if_possible: ${config.use_tree_reduction_if_possible}
               |has_training_convs: ${config.has_training_convs}
               |has_max_pool: ${config.has_max_pool}
               |has_nonlinear_activations: ${config.has_nonlinear_activations}
               |has_first_layer_optimizations: ${config.has_first_layer_optimizations}
               |use_shared_ext_mem: ${config.use_shared_ext_mem}
               |clock_gate: ${config.clock_gate}
               |+-------------------------+""".stripMargin)
    //config.getClass.getDeclaredFields.map(_.getName).foreach(println(_))
    config
  }
  def applyDSE() = sys.env.get("chipyardDSE") match {
	  case Some("Booleans") => applyDSE2()
	  case Some("Parameters") => applyDSE1()
	  case _ => GemminiConfigs.defaultConfig
  }
  def apply() = sys.env.get("gemmini_header") match {
	  case Some(headerFileName) => applyDSE().copy(headerFileName = headerFileName)
	  case None => applyDSE()
  }
}

/*  
object GemminiParameters{
  val meshRC = 4 << sys.env.get("meshRC").getOrElse("0").toInt
  //val dataflow = Dataflow(sys.env.get("dataflow").getOrElse("2").toInt)
  val sp_factor = 2 << sys.env.get("sp_factor").getOrElse("0").toInt
  val acc_factor = 32 << sys.env.get("acc_factor").getOrElse("0").toInt
  val has_training_convs = sys.env.get("has_training_convs").getOrElse("0").toInt == 1
  val has_max_pool = sys.env.get("has_max_pool").getOrElse("0").toInt == 1
  val has_nonlinear_activations = sys.env.get("has_nonlinear_activations").getOrElse("0").toInt == 1
  val sp_capacity = CapacityInMatrices(acc_factor * sp_factor)
  val acc_capacity = CapacityInMatrices(acc_factor)
  val header = sys.env.get("gemmini_header").getOrElse("gemmini_params.h")
  
  def apply() = {
	val config = GemminiConfigs.defaultConfig.copy(
    meshRows = meshRC,
    meshColumns = meshRC,
    sp_capacity = sp_capacity,
    acc_capacity = acc_capacity,
    has_training_convs = has_training_convs,
    has_max_pool = has_max_pool,
    has_nonlinear_activations = has_nonlinear_activations,
    headerFileName = header,
    )
    println("+---------------+")
    println(s"meshRows ${config.meshRows}")
    println(s"sp_capacity ${config.sp_capacity}")
    println(s"acc_capacity ${config.acc_capacity}")
    println(s"has_training_convs ${config.has_training_convs}")
    println(s"has_max_pool ${config.has_max_pool}")
    println(s"has_nonlinear_activations ${config.has_nonlinear_activations}")
    println(s"gemmini_header $header")
    println("+---------------+")
    config
  }
  
  def apply(n: Int) = {GemminiConfigs.defaultConfig.copy(
    meshRows = n,
    meshColumns = n,
    headerFileName = header,
    )
  }
  
} 
*/

//import chisel3._

//trait GemminiCake extends Function1[DefaultConfigType, DefaultConfigType]{
//	implicit def cake2config(gc: GemminiCake) = gc.apply(GemminiConfigs.defaultConfig)
//}

object ModifyHeader extends ConfigCake{
	def apply(cin: DefaultConfigType) = cin.copy(headerFileName = sys.env.get("gemmini_header").getOrElse(cin.headerFileName))
}

class SingleEv7Config extends Config(
  new WithSingleEv7Tweaks ++
  new gemmini.DefaultGemminiConfig(ModifyHeader) ++
  new chipyard.RocketConfig  
)
// DOC include end: AbstractSingleEv7 and Rocket
