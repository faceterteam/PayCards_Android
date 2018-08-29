cmake_minimum_required(VERSION 3.4.1)

set(OpenCV_DIR "${CMAKE_CURRENT_SOURCE_DIR}/opencv2/sdk/native/jni")

add_subdirectory(armneon2x86sse)
add_subdirectory(eigen)
add_subdirectory(protobuf)
add_subdirectory(caffe)
add_subdirectory(libyuv)
add_subdirectory(crossplatform)

add_library(cardrecognizer SHARED CardRecognizer-jni.cpp)

target_link_libraries(cardrecognizer
        crossplatform
        libyuv
        log
        z)

