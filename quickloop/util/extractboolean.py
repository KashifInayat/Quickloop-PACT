x = '''
sp_singleported
acc_singleported
mvin_scale_shared
acc_read_full_width
acc_read_small_width
use_dedicated_tl_port
use_tlb_register_filter
ex_read_from_spad
ex_read_from_acc
ex_write_to_spad
ex_write_to_acc
hardcode_d_to_garbage_addr
use_shared_tlb
use_tree_reduction_if_possible
has_training_convs
has_max_pool
has_nonlinear_activations
has_first_layer_optimizations
use_shared_ext_mem
clock_gate'''

y = [f'''    val {i} = sys.env.get("{i}").map(_ == "1").getOrElse(dp.{i})''' for i in x.split()]
z = [f'''    {i} = {i},''' for i in x.split()]
a = [f'''               |{i}: ${{config.{i}}}''' for i in x.split()]
b = [f'''    val {v} = if((c & (1 << {i})) > 0) !cin.{v} else cin.{v}''' for i, v in enumerate(x.split())]


r1 = '/work/tayyeb/tmp/dsebool/init/rtl'
r2 = '/work/tayyeb/tmp/dsebool/18/rtl'
from ..util import getBlackboxes
print(getBlackboxes('SingleEv7FPGATestHarness', r1, r2))