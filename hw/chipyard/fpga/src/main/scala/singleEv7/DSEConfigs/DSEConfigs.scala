// See LICENSE for license details.
package chipyard.fpga.singleEv7

import freechips.rocketchip.subsystem._

object FixBooleans extends ConfigCake{
	def apply(cin: DefaultConfigType) = {
	val config = cin.copy(
    acc_read_small_width = false,
    has_training_convs = false,
    has_max_pool = false,
    )     
    config
  }
}

object ModifyMesh extends ConfigCake{
	def apply(cin: DefaultConfigType) = {
	val meshRC = sys.env.get("meshRC")
	                    .map(x => 4 << x.toInt)
	                    .getOrElse(cin.meshRows)
	val tileRC = sys.env.get("tileRC")
	                    .map(x => 1 << x.toInt)
	                    .getOrElse(cin.tileRows)

	val config = cin.copy(
    meshRows = meshRC,
    meshColumns = meshRC,
    tileRows = tileRC,
    tileColumns = tileRC,
    )
    
    println(s"""+-------------------------+
               |meshRows: ${config.meshRows}
               |tileRows: ${config.tileRows}
               |+-------------------------+""".stripMargin)
       
    config
  }
}

object ModifyLat extends ConfigCake{
	def apply(cin: DefaultConfigType) = {
	val lat = sys.env.get("tilelat")
	                    .map(x => x.toInt)
	                    .getOrElse(cin.tile_latency)

	val config = cin.copy(
    tile_latency = lat
    )
    
    println(s"""+-------------------------+
               |tile_latency: ${config.tile_latency}
               |+-------------------------+""".stripMargin)
       
    config
  }
}

object ModifyDMA extends ConfigCake{
	def apply(cin: DefaultConfigType) = {
	val buswidth = sys.env.get("fetchwidth")
	                    .map(x => x.toInt)
	                    .getOrElse(cin.dma_buswidth)
	val maxbytes = sys.env.get("fetchbytes")
	                    .map(x => x.toInt)
	                    .getOrElse(cin.dma_maxbytes)

	val config = cin.copy(
    dma_buswidth = buswidth,
    dma_maxbytes = maxbytes
    )
    
    println(s"""+-------------------------+
               |dma_buswidth: ${config.dma_buswidth}
               |dma_maxbytes: ${config.dma_maxbytes}
               |+-------------------------+""".stripMargin)
       
    config
  }
}


object ModifyAccSpad extends ConfigCake{
	def apply(cin: DefaultConfigType) = {
	val acc_capacity_mat = cin.acc_capacity match {
      case gemmini.CapacityInKilobytes(kb) => kb * 1024 * 8 / (cin.meshRows * cin.meshColumns * cin.tileRows * cin.tileColumns * cin.accType.getWidth)
      case gemmini.CapacityInMatrices(ms) => ms
    }
    val sp_capacity_mat = cin.sp_capacity match {
      case gemmini.CapacityInKilobytes(kb) => kb * 1024 * 8 / (cin.meshRows * cin.tileRows * cin.sp_width)
      case gemmini.CapacityInMatrices(ms) => ms
    }
    val new_acc_capacity_mat = sys.env.get("accFactor")
	                         .map(x => 32 << x.toInt)
	                         .getOrElse(acc_capacity_mat)

	val new_sp_capacity_mat = sys.env.get("spadFactor")
	                         .map(x => (2 << x.toInt) * new_acc_capacity_mat)
	                         .getOrElse(sp_capacity_mat.max(2 * new_acc_capacity_mat))

	val config = cin.copy(
	acc_capacity = gemmini.CapacityInMatrices(new_acc_capacity_mat),
    sp_capacity = gemmini.CapacityInMatrices(new_sp_capacity_mat),
    )
    
    println(s"""+-------------------------+
               |acc_capacity: ${config.acc_capacity}
               |sp_capacity: ${config.sp_capacity}
               |+-------------------------+""".stripMargin)

    config
  }
}

object ModifyBooleans extends ConfigCake{
	def apply(cin: DefaultConfigType) = {
		val c: Int = sys.env.get("booleanConfig").map(_.toInt).getOrElse(0)
		
    val sp_singleported = if((c & (1 << 0)) > 0) !cin.sp_singleported else cin.sp_singleported
    val acc_singleported = if((c & (1 << 1)) > 0) !cin.acc_singleported else cin.acc_singleported
    val mvin_scale_shared = if((c & (1 << 2)) > 0) !cin.mvin_scale_shared else cin.mvin_scale_shared
    val acc_read_full_width = if((c & (1 << 3)) > 0) !cin.acc_read_full_width else cin.acc_read_full_width
    val acc_read_small_width = if((c & (1 << 4)) > 0) !cin.acc_read_small_width else cin.acc_read_small_width
    val use_dedicated_tl_port = if((c & (1 << 5)) > 0) !cin.use_dedicated_tl_port else cin.use_dedicated_tl_port
    val use_tlb_register_filter = if((c & (1 << 6)) > 0) !cin.use_tlb_register_filter else cin.use_tlb_register_filter
    val ex_read_from_spad = if((c & (1 << 7)) > 0) !cin.ex_read_from_spad else cin.ex_read_from_spad
    val ex_read_from_acc = if((c & (1 << 8)) > 0) !cin.ex_read_from_acc else cin.ex_read_from_acc
    val ex_write_to_spad = if((c & (1 << 9)) > 0) !cin.ex_write_to_spad else cin.ex_write_to_spad
    val ex_write_to_acc = if((c & (1 << 10)) > 0) !cin.ex_write_to_acc else cin.ex_write_to_acc
    val hardcode_d_to_garbage_addr = if((c & (1 << 11)) > 0) !cin.hardcode_d_to_garbage_addr else cin.hardcode_d_to_garbage_addr
    val use_shared_tlb = if((c & (1 << 12)) > 0) !cin.use_shared_tlb else cin.use_shared_tlb
    val use_tree_reduction_if_possible = if((c & (1 << 13)) > 0) !cin.use_tree_reduction_if_possible else cin.use_tree_reduction_if_possible
    val has_training_convs = if((c & (1 << 14)) > 0) !cin.has_training_convs else cin.has_training_convs
    val has_max_pool = if((c & (1 << 15)) > 0) !cin.has_max_pool else cin.has_max_pool
    val has_nonlinear_activations = if((c & (1 << 16)) > 0) !cin.has_nonlinear_activations else cin.has_nonlinear_activations
    val has_first_layer_optimizations = if((c & (1 << 17)) > 0) !cin.has_first_layer_optimizations else cin.has_first_layer_optimizations
    val use_shared_ext_mem = if((c & (1 << 18)) > 0) !cin.use_shared_ext_mem else cin.use_shared_ext_mem

		val config = cin.copy(
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
				   |+-------------------------+""".stripMargin)
	config
	}
}

class DSEConfig extends freechips.rocketchip.config.Config(
  new WithSingleEv7Tweaks ++
  new freechips.rocketchip.subsystem.WithoutFPU ++
  new gemmini.DefaultGemminiConfig(ModifyHeader andThen ModifyDMA andThen ModifyMesh andThen ModifyBooleans andThen ModifyAccSpad andThen ModifyLat) ++
  new chipyard.RocketConfig().alter((site, here, up) => {case SystemBusKey => up(SystemBusKey).copy(beatBytes = 16)})  
)



