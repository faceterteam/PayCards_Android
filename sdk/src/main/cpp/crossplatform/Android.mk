LOCAL_PATH := $(my-dir)
include $(CLEAR_VARS)

local_cross_platform_path := CrossPlatform

# find CrossPlatform jniImpl -name "*.cpp" | grep -Ev "tests|doc" | sort | awk '{ print "\t"$1" \\" }'
local_src_files := \
	CrossPlatform/CaffePredictor/CaffeDatum.cpp \
	CrossPlatform/CaffePredictor/CaffeDatumList.cpp \
	CrossPlatform/CaffePredictor/CaffeNeuralNetwork.cpp \
	CrossPlatform/CaffePredictor/CaffeObjectFactory.cpp \
	CrossPlatform/CaffePredictor/CaffeResult.cpp \
	CrossPlatform/CaffePredictor/CaffeResultList.cpp \
	CrossPlatform/CV/canny.cpp \
	CrossPlatform/CV/hough.cpp \
	CrossPlatform/CV/warp.cpp \
	CrossPlatform/Recognizer/DateRecognizer.cpp \
	CrossPlatform/Recognizer/EdgesDetector.cpp \
	CrossPlatform/Recognizer/FrameStorage.cpp \
	CrossPlatform/Recognizer/NameRecognizer.cpp \
	CrossPlatform/Recognizer/NumberRecognizer.cpp \
	CrossPlatform/Recognizer/RecognitionCore.cpp \
	CrossPlatform/Recognizer/RecognitionResult.cpp \
	CrossPlatform/Recognizer/Utils.cpp \
	CrossPlatform/ServiceContainer.cpp \
	CrossPlatform/Torch/TorchManager.cpp \
	jniImpl/RecognitionCoreDelegate.cpp \
	jniImpl/TorchDelegate.cpp

#find CrossPlatform -type d | sort | awk '{ print "\t"$1" \\" }'
local_c_includes := \
	CrossPlatform \
	CrossPlatform/CaffePredictor \
	CrossPlatform/CV \
	CrossPlatform/Include \
	CrossPlatform/Include/NeuralNetwork \
	CrossPlatform/Include/Public \
	CrossPlatform/Include/Recognizer \
	CrossPlatform/Include/Torch \
	CrossPlatform/Recognizer \
	CrossPlatform/Torch \
	jniImpl

LOCAL_MODULE := crossplatform
LOCAL_SRC_FILES  := $(local_src_files)
LOCAL_CPPFLAGS   += -std=c++11
#APP_CPPFLAGS += -O3
LOCAL_CPP_FEATURES := rtti exceptions
LOCAL_ARM_MODE := arm
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../eigen \
    	$(LOCAL_PATH)/../eigen/Eigen \
	$(LOCAL_PATH) \
	$(addprefix $(LOCAL_PATH)/, $(local_c_includes))
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH) $(LOCAL_PATH)/CrossPlatform/Include $(addprefix $(LOCAL_PATH)/, $(local_c_includes))

LOCAL_STATIC_LIBRARIES := caffe_static libyuv_static

ifeq ($(OPENCV_LINK_TYPE), static)
    LOCAL_STATIC_LIBRARIES += opencv2_static
else
    LOCAL_SHARED_LIBRARIES := opencv2_shared
endif

ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI), armeabi-v7a x86))
    LOCAL_ARM_NEON  := true
endif # TARGET_ARCH_ABI == armeabi-v7a || x86

include $(BUILD_STATIC_LIBRARY)

