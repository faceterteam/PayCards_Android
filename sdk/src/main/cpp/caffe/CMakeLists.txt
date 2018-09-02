cmake_minimum_required(VERSION 3.4.1)
add_library(caffe STATIC
        Caffe/blob.cpp
        Caffe/common.cpp
        Caffe/layer_factory.cpp
        Caffe/layers/base_conv_layer.cpp
        Caffe/layers/base_data_layer.cpp
        Caffe/layers/bnll_layer.cpp
        Caffe/layers/concat_layer.cpp
        Caffe/layers/conv_layer.cpp
        Caffe/layers/eltwise_layer.cpp
        Caffe/layers/flatten_layer.cpp
        Caffe/layers/im2col_layer.cpp
        Caffe/layers/inner_product_layer.cpp
        Caffe/layers/loss_layer.cpp
        Caffe/layers/lrn_layer.cpp
        Caffe/layers/memory_data_layer.cpp
        Caffe/layers/neuron_layer.cpp
        Caffe/layers/pooling_layer.cpp
        Caffe/layers/power_layer.cpp
        Caffe/layers/relu_layer.cpp
        Caffe/layers/sigmoid_layer.cpp
        Caffe/layers/softmax_layer.cpp
        Caffe/layers/softmax_loss_layer.cpp
        Caffe/layers/split_layer.cpp
        Caffe/layers/tanh_layer.cpp
        Caffe/net.cpp
        Caffe/proto/caffe.pb.cc
        Caffe/syncedmem.cpp
        Caffe/util/im2col.cpp
        Caffe/util/insert_splits.cpp
        Caffe/util/io.cpp
        Caffe/util/math_functions.cpp
        Caffe/util/upgrade_proto.cpp)

target_include_directories(caffe
        PRIVATE Caffe
        PUBLIC include)

target_compile_definitions(caffe PUBLIC CPU_ONLY USE_EIGEN)
target_compile_options(caffe PRIVATE
        -marm -Wno-unused-command-line-argument
        -frtti -fexceptions
        -O3)
set_target_properties(caffe PROPERTIES
        CXX_VISIBILITY_PRESET hidden
        VISIBILITY_INLINES_HIDDEN 1)

if (${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set_property(TARGET caffe APPEND_STRING PROPERTY COMPILE_FLAGS " -mfpu=neon")
endif()

target_link_libraries(caffe
        PUBLIC protobuf
        PRIVATE eigen)
