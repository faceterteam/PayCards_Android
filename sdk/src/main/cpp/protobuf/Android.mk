LOCAL_PATH := $(my-dir)
include $(CLEAR_VARS)

# find GoogleProtobuf -name "*.cc" | grep -Ev "tests|doc" | sort | awk '{ print "\t"$1" \\" }'
local_src_files := \
	GoogleProtobuf/google/protobuf/descriptor.cc \
	GoogleProtobuf/google/protobuf/descriptor_database.cc \
	GoogleProtobuf/google/protobuf/descriptor.pb.cc \
	GoogleProtobuf/google/protobuf/dynamic_message.cc \
	GoogleProtobuf/google/protobuf/extension_set.cc \
	GoogleProtobuf/google/protobuf/extension_set_heavy.cc \
	GoogleProtobuf/google/protobuf/generated_message_reflection.cc \
	GoogleProtobuf/google/protobuf/generated_message_util.cc \
	GoogleProtobuf/google/protobuf/io/coded_stream.cc \
	GoogleProtobuf/google/protobuf/io/gzip_stream.cc \
	GoogleProtobuf/google/protobuf/io/printer.cc \
	GoogleProtobuf/google/protobuf/io/tokenizer.cc \
	GoogleProtobuf/google/protobuf/io/zero_copy_stream.cc \
	GoogleProtobuf/google/protobuf/io/zero_copy_stream_impl.cc \
	GoogleProtobuf/google/protobuf/io/zero_copy_stream_impl_lite.cc \
	GoogleProtobuf/google/protobuf/message.cc \
	GoogleProtobuf/google/protobuf/message_lite.cc \
	GoogleProtobuf/google/protobuf/reflection_ops.cc \
	GoogleProtobuf/google/protobuf/repeated_field.cc \
	GoogleProtobuf/google/protobuf/service.cc \
	GoogleProtobuf/google/protobuf/stubs/common.cc \
	GoogleProtobuf/google/protobuf/stubs/once.cc \
	GoogleProtobuf/google/protobuf/stubs/structurally_valid.cc \
	GoogleProtobuf/google/protobuf/stubs/strutil.cc \
	GoogleProtobuf/google/protobuf/stubs/substitute.cc \
	GoogleProtobuf/google/protobuf/text_format.cc \
	GoogleProtobuf/google/protobuf/unknown_field_set.cc \
	GoogleProtobuf/google/protobuf/wire_format.cc \
	GoogleProtobuf/google/protobuf/wire_format_lite.cc

local_c_includes := \
	GoogleProtobuf


LOCAL_MODULE := protobuf_static
LOCAL_SRC_FILES  := $(local_src_files)
LOCAL_CFLAGS := -frtti -Wno-sign-compare -Wno-unused-parameter -Wno-sign-promo -Wno-error=return-type
LOCAL_CPPFLAGS   += -std=c++11
LOCAL_CPP_EXTENSION := .cc
LOCAL_C_INCLUDES := $(LOCAL_PATH) $(addprefix $(LOCAL_PATH)/, $(local_c_includes))
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH) $(LOCAL_PATH)/GoogleProtobuf
LOCAL_CPP_FEATURES := rtti

include $(BUILD_STATIC_LIBRARY)
