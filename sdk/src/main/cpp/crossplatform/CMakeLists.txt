cmake_minimum_required(VERSION 3.4.1)

add_library(crossplatform STATIC
        CrossPlatform/CaffePredictor/CaffeDatum.cpp
        CrossPlatform/CaffePredictor/CaffeDatumList.cpp
        CrossPlatform/CaffePredictor/CaffeNeuralNetwork.cpp
        CrossPlatform/CaffePredictor/CaffeObjectFactory.cpp
        CrossPlatform/CaffePredictor/CaffeResult.cpp
        CrossPlatform/CaffePredictor/CaffeResultList.cpp
        CrossPlatform/CV/canny.cpp
        CrossPlatform/CV/hough.cpp
        CrossPlatform/CV/warp.cpp
        CrossPlatform/Recognizer/DateRecognizer.cpp
        CrossPlatform/Recognizer/EdgesDetector.cpp
        CrossPlatform/Recognizer/FrameStorage.cpp
        CrossPlatform/Recognizer/NameRecognizer.cpp
        CrossPlatform/Recognizer/NumberRecognizer.cpp
        CrossPlatform/Recognizer/RecognitionCore.cpp
        CrossPlatform/Recognizer/RecognitionResult.cpp
        CrossPlatform/Recognizer/Utils.cpp
        CrossPlatform/ServiceContainer.cpp
        CrossPlatform/Torch/TorchManager.cpp
        jniImpl/RecognitionCoreDelegate.cpp
        jniImpl/TorchDelegate.cpp
        )

find_package(OpenCV REQUIRED
        COMPONENTS opencv_objdetect opencv_imgcodecs opencv_imgproc opencv_ml opencv_core)

set_target_properties(crossplatform PROPERTIES
        CXX_EXTENSIONS OFF
        CXX_STANDARD 11
        CXX_VISIBILITY_PRESET hidden
        VISIBILITY_INLINES_HIDDEN 1)
target_compile_options(crossplatform PRIVATE
        -marm -Wno-unused-command-line-argument
        -frtti -fexceptions
        -O3)

target_include_directories(crossplatform
        PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
        CrossPlatform
        CrossPlatform/CaffePredictor
        CrossPlatform/CV
        CrossPlatform/Include
        CrossPlatform/Include/NeuralNetwork
        CrossPlatform/Include/Public
        CrossPlatform/Include/Recognizer
        CrossPlatform/Include/Torch
        CrossPlatform/Recognizer
        CrossPlatform/Torch
        jniImpl)

if (ANDROID_ABI STREQUAL "armeabi-v7a")
    set_property(TARGET crossplatform APPEND_STRING PROPERTY COMPILE_FLAGS " -mfpu=neon")
elseif(ANDROID_ABI MATCHES "x86(_64)?")
    target_compile_options(crossplatform PRIVATE
            $<$<STREQUAL:${ANDROID_ABI},x86>:-msse3>
            -Wno-unknown-attributes
            -Wno-deprecated-declarations)
    target_compile_definitions(crossplatform PRIVATE HAVE_NEON_X86=1)
    target_link_libraries(crossplatform PRIVATE NEON_2_SSE)
endif()

target_link_libraries(crossplatform
        PRIVATE
        eigen
        caffe
        libyuv
        PUBLIC
        ${OpenCV_LIBS}
)
