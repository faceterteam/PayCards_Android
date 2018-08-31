cmake_minimum_required(VERSION 3.4.1)

add_library(libyuv STATIC
		source/compare.cc
		source/compare_common.cc
		source/compare_gcc.cc
		source/compare_mmi.cc
		source/compare_msa.cc
		source/compare_neon64.cc
		source/convert.cc
		source/convert_argb.cc
		source/convert_from.cc
		source/convert_from_argb.cc
		source/convert_to_argb.cc
		source/convert_to_i420.cc
		source/cpu_id.cc
		source/planar_functions.cc
		source/rotate.cc
		source/rotate_any.cc
		source/rotate_argb.cc
		source/rotate_common.cc
		source/rotate_gcc.cc
		source/rotate_mmi.cc
		source/rotate_msa.cc
		source/rotate_neon64.cc
		source/row_any.cc
		source/row_common.cc
		source/row_gcc.cc
		source/row_mmi.cc
		source/row_msa.cc
		source/row_neon64.cc
		source/scale.cc
		source/scale_any.cc
		source/scale_argb.cc
		source/scale_common.cc
		source/scale_gcc.cc
		source/scale_mmi.cc
		source/scale_msa.cc
		source/scale_neon64.cc
		source/video_common.cc)

set(neon_SRCS source/compare_neon.cc
        source/rotate_neon.cc
        source/row_neon.cc
        source/scale_neon.cc)

target_include_directories(libyuv PUBLIC include)
target_compile_options(libyuv PRIVATE -fno-exceptions -fno-rtti -Wall -O3)
set_target_properties(libyuv PROPERTIES
    CXX_VISIBILITY_PRESET hidden
    VISIBILITY_INLINES_HIDDEN 1)

if (ANDROID_ABI STREQUAL "armeabi-v7a")
    set_property(SOURCE ${neon_SRCS} APPEND_STRING PROPERTY COMPILE_FLAGS " -mfpu=neon")
    target_sources(libyuv PRIVATE ${neon_SRCS})
    target_compile_definitions(libyuv PRIVATE LIBYUV_NEON)
endif ()
