# Copyright 2019 SiFive, Inc #
# SPDX-License-Identifier: Apache-2.0 #

PROGRAM ?= gemmini

override CFLAGS  += -fno-builtin-printf -Xlinker --defsym=__stack_size=0x2000 -Xlinker --defsym=__heap_size=0x2000 -Xlinker --allow-multiple-definition -Xlinker --defsym=__imaddr=0xb0000000 -I$(GEMMINI_HEADER) 
override CFLAGS  += -I./ -DBAREMETAL -DFAST

SRCS := \
	mvin_mvout \
	mvin_mvout_zeros \
	mvin_mvout_stride \
	mvin_mvout_block_stride \
	mvin_mvout_acc \
	mvin_mvout_acc_zero_stride \
	mvin_mvout_acc_stride \
	mvin_mvout_acc_full \
	mvin_mvout_acc_full_stride \
	matmul_os \
	matmul_ws \
	matmul \
	raw_hazard \
	aligned \
	padded \
	mvin_scale \
	conv \
	conv_with_pool \
	conv_with_rot180 \
	conv_with_kernel_dilation \
	conv_with_input_dilation \
	conv_with_input_dilation_and_rot180 \
	conv_with_input_dilation_and_neg_padding \
	conv_trans_output_1203 \
	conv_trans_weight_1203 \
	conv_trans_weight_0132 \
	conv_trans_input_3120 \
	conv_trans_input_3120_with_kernel_dilation \
	conv_first_layer \
	conv_dw \
	tiled_matmul_os \
	tiled_matmul_ws \
	tiled_matmul_ws_At \
	tiled_matmul_ws_Bt \
	tiled_matmul_ws_full_C \
	tiled_matmul_ws_low_D \
	tiled_matmul_cpu \
	tiled_matmul_option \
	tiled_matmul_ws_perf \
	transpose \
	matrix_add \
	resadd \
	global_average \
	template \
	gemmini_counter

kernel_dir := /work/tayyeb/nas/gitlocal/fresh/chipyard/generators/gemmini/software/gemmini-rocc-tests

objects := $(SRCS:=.o)

%.o:
	$(CC) $(CFLAGS) -Dmain=$(patsubst %.o,%,$@)_main -c -o debug/$@ $(kernel_dir)/bareMetalC/$(patsubst %.o,%.c,$@)

resnet50.o: 
	$(CC) $(CFLAGS) -Dmain=resnet50_main -c -o debug/$@ $(kernel_dir)/imagenet/resnet50.c


mobilenet.o: 
	$(CC) $(CFLAGS) -Dmain=mobilenet_main -c -o debug/$@ $(kernel_dir)/imagenet/mobilenet.c

transformer.o: 
	$(CC) $(CFLAGS) -Dmain=transformer_main -c -o debug/$@ $(kernel_dir)/transformers/transformer.c


$(PROGRAM): $(objects) resnet50.o mobilenet.o transformer.o
	$(CC) $(CFLAGS) $(LDFLAGS) edk_main.c syscalls.c $(addprefix debug/,$^) debug/resnet50.o debug/mobilenet.o debug/transformer.o $(LOADLIBES) $(LDLIBS) -o $@

clean:
	rm -f $(PROGRAM) $(PROGRAM).hex
