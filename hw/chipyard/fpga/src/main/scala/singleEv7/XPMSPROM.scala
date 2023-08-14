package chipyard.fpga.singleEv7

import chisel3._
import chisel3.experimental._

// xpm_memory_sprom: Single Port ROM
// Xilinx Parameterized Macro, version 2022.1
class xpm_memory_sprom(addr_width: Int = 6, data_width: Int  = 32) extends ExtModule(Map(
        "ADDR_WIDTH_A" -> addr_width, 
        "AUTO_SLEEP_TIME" -> 0, 
        "CASCADE_HEIGHT" -> 0, 
        "ECC_MODE" -> "no_ecc", 
        "MEMORY_INIT_FILE" -> "bootrom.mem", 
        "MEMORY_INIT_PARAM" -> "0", 
        "MEMORY_OPTIMIZATION" -> "false", 
        "MEMORY_PRIMITIVE" -> "block", 
        "MEMORY_SIZE" -> ((1 << addr_width) * data_width), 
        "MESSAGE_CONTROL" -> 0, 
        "READ_DATA_WIDTH_A" -> data_width, 
        "READ_LATENCY_A" -> 1, 
        "READ_RESET_VALUE_A" -> "0", 
        "RST_MODE_A" -> "SYNC", 
        "SIM_ASSERT_CHK" -> 0, 
        "USE_MEM_INIT" -> 0, 
        "USE_MEM_INIT_MMI" -> 0, 
        "WAKEUP_TIME" -> "disable_sleep", 
)){
        //val dbiterra = IO(Output(UInt(1.W)))    // 1-bit output: Leave open.
        val douta = IO(Output(UInt(READ_DATA_WIDTH_A.W)))       // READ_DATA_WIDTH_A-bit output: Data output for port A read operations.
        //val sbiterra = IO(Output(UInt(1.W)))    // 1-bit output: Leave open.
        val addra = IO(Input(UInt(ADDR_WIDTH_A.W)))     // ADDR_WIDTH_A-bit input: Address for port A read operations.
        val clka = IO(Input(UInt(1.W)))         // 1-bit input: Clock signal for port A.
        val ena = IO(Input(UInt(1.W)))  // 1-bit input: Memory enable signal for port A. Must be high on clock
                                    // cycles when read operations are initiated. Pipelined internally.
        //val injectdbiterra = IO(Input(UInt(1.W)))       // 1-bit input: Do not change from the provided value.
        //val injectsbiterra = IO(Input(UInt(1.W)))       // 1-bit input: Do not change from the provided value.
        //val regcea = IO(Input(UInt(1.W)))       // 1-bit input: Do not change from the provided value.
        //val rsta = IO(Input(UInt(1.W)))         // 1-bit input: Reset signal for the final port A output register stage.
                                    // Synchronously resets output port douta to the value specified by
                                    // parameter READ_RESET_VALUE_A.
        //val sleep = IO(Input(UInt(1.W)))        // 1-bit input: sleep signal to enable the dynamic power saving feature.

        def READ_DATA_WIDTH_A = params("READ_DATA_WIDTH_A").asInstanceOf[IntParam].value.toInt
        def ADDR_WIDTH_A = params("ADDR_WIDTH_A").asInstanceOf[IntParam].value.toInt
}
