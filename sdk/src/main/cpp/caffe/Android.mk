LOCAL_PATH := $(my-dir)
include $(CLEAR_VARS)

# find Caffe -name '*.cpp' -o -name '*.cc' | grep -Ev "tests|doc" | sort | awk '{ print "\t"$1" \\" }'
local_src_files := Caffe/blob.cpp \
	Caffe/common.cpp \
	Caffe/layer_factory.cpp \
	Caffe/layers/base_conv_layer.cpp \
	Caffe/layers/base_data_layer.cpp \
	Caffe/layers/bnll_layer.cpp \
	Caffe/layers/concat_layer.cpp \
	Caffe/layers/conv_layer.cpp \
	Caffe/layers/eltwise_layer.cpp \
	Caffe/layers/flatten_layer.cpp \
	Caffe/layers/im2col_layer.cpp \
	Caffe/layers/inner_product_layer.cpp \
	Caffe/layers/loss_layer.cpp \
	Caffe/layers/lrn_layer.cpp \
	Caffe/layers/memory_data_layer.cpp \
	Caffe/layers/neuron_layer.cpp \
	Caffe/layers/pooling_layer.cpp \
	Caffe/layers/power_layer.cpp \
	Caffe/layers/relu_layer.cpp \
	Caffe/layers/sigmoid_layer.cpp \
	Caffe/layers/softmax_layer.cpp \
	Caffe/layers/softmax_loss_layer.cpp \
	Caffe/layers/split_layer.cpp \
	Caffe/layers/tanh_layer.cpp \
	Caffe/net.cpp \
	Caffe/proto/caffe.pb.cc \
	Caffe/syncedmem.cpp \
	Caffe/util/im2col.cpp \
	Caffe/util/insert_splits.cpp \
	Caffe/util/io.cpp \
	Caffe/util/math_functions.cpp \
	Caffe/util/upgrade_proto.cpp

local_c_includes := \
    Caffe \
    include


LOCAL_MODULE := caffe_static
LOCAL_SRC_FILES  := $(local_src_files)
LOCAL_CFLAGS += -DCPU_ONLY -DUSE_EIGEN
LOCAL_CPP_EXTENSION := .cc .cpp
LOCAL_CPPFLAGS   += -std=c++11
APP_CPPFLAGS += -O3
LOCAL_CPP_FEATURES := rtti exceptions
LOCAL_ARM_MODE := arm
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../eigen $(addprefix $(LOCAL_PATH)/, $(local_c_includes))
LOCAL_EXPORT_CFLAGS := -DCPU_ONLY -DUSE_EIGEN
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include

LOCAL_STATIC_LIBRARIES := protobuf_static

ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI), armeabi-v7a x86))
    LOCAL_ARM_NEON  := true
endif # TARGET_ARCH_ABI == armeabi-v7a || x86

include $(BUILD_STATIC_LIBRARY)
