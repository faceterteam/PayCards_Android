cmake_minimum_required(VERSION 3.4.1)

add_library(protobuf STATIC
        GoogleProtobuf/google/protobuf/descriptor.cc
        GoogleProtobuf/google/protobuf/descriptor_database.cc
        GoogleProtobuf/google/protobuf/descriptor.pb.cc
        GoogleProtobuf/google/protobuf/dynamic_message.cc
        GoogleProtobuf/google/protobuf/extension_set.cc
        GoogleProtobuf/google/protobuf/extension_set_heavy.cc
        GoogleProtobuf/google/protobuf/generated_message_reflection.cc
        GoogleProtobuf/google/protobuf/generated_message_util.cc
        GoogleProtobuf/google/protobuf/io/coded_stream.cc
        GoogleProtobuf/google/protobuf/io/gzip_stream.cc
        GoogleProtobuf/google/protobuf/io/printer.cc
        GoogleProtobuf/google/protobuf/io/tokenizer.cc
        GoogleProtobuf/google/protobuf/io/zero_copy_stream.cc
        GoogleProtobuf/google/protobuf/io/zero_copy_stream_impl.cc
        GoogleProtobuf/google/protobuf/io/zero_copy_stream_impl_lite.cc
        GoogleProtobuf/google/protobuf/message.cc
        GoogleProtobuf/google/protobuf/message_lite.cc
        GoogleProtobuf/google/protobuf/reflection_ops.cc
        GoogleProtobuf/google/protobuf/repeated_field.cc
        GoogleProtobuf/google/protobuf/service.cc
        GoogleProtobuf/google/protobuf/stubs/common.cc
        GoogleProtobuf/google/protobuf/stubs/once.cc
        GoogleProtobuf/google/protobuf/stubs/structurally_valid.cc
        GoogleProtobuf/google/protobuf/stubs/strutil.cc
        GoogleProtobuf/google/protobuf/stubs/substitute.cc
        GoogleProtobuf/google/protobuf/text_format.cc
        GoogleProtobuf/google/protobuf/unknown_field_set.cc
        GoogleProtobuf/google/protobuf/wire_format.cc
        GoogleProtobuf/google/protobuf/wire_format_lite.cc)

set_target_properties(protobuf PROPERTIES
        CXX_EXTENSIONS OFF
        CXX_STANDARD 11
        CXX_VISIBILITY_PRESET hidden
        VISIBILITY_INLINES_HIDDEN 1)
target_compile_options(protobuf PRIVATE
        -frtti -fno-exceptions
        -Wno-enum-compare-switch
        -Wno-error=return-type
        -Wno-null-conversion
        -Wno-sign-compare
        -Wno-sign-promo
        -Wno-tautological-constant-compare
        -Wno-tautological-undefined-compare
        -Wno-unused-parameter)
target_include_directories(protobuf PUBLIC GoogleProtobuf)
