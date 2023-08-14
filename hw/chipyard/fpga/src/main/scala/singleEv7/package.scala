// See LICENSE for license details.
package chipyard.fpga
import scala.language.implicitConversions
package object singleEv7 {
  type DefaultConfigType = gemmini.GemminiArrayConfig[chisel3.SInt,gemmini.Float,gemmini.Float]
  type ConfigCake = Function1[DefaultConfigType, DefaultConfigType]
  implicit def cake2config(cc: ConfigCake) = cc.apply(gemmini.GemminiConfigs.defaultConfig)
}
