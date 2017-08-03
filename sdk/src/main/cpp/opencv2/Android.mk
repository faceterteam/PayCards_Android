LOCAL_PATH := $(call my-dir)

#opencv_static
include $(CLEAR_VARS)

OPENCV_LIB_TYPE:=STATIC

include $(LOCAL_PATH)/sdk/jni/OpenCV.mk

LOCAL_MODULE    := opencv2_static
LOCAL_EXPORT_C_INCLUDES := $(OPENCV_LOCAL_C_INCLUDES)
LOCAL_EXPORT_CFLAGS := $(OPENCV_LOCAL_CFLAGS)

LOCAL_STATIC_LIBRARIES := \
    opencv_objdetect \
    opencv_imgcodecs \
    opencv_imgproc \
    opencv_ml \
    opencv_core \
    libpng \
    tbb

include $(BUILD_STATIC_LIBRARY)

#Opencv shared
include $(CLEAR_VARS)
LOCAL_MODULE := opencv2_shared
LOCAL_SRC_FILES = sdk/libs/$(TARGET_ARCH_ABI)/libopencv_java3.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/sdk/jni/include
include $(PREBUILT_SHARED_LIBRARY)
