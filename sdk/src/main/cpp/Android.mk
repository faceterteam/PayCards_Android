LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := cardrecognizer
LOCAL_SRC_FILES := CardRecognizer-jni.cpp
LOCAL_CPPFLAGS   += -std=c++11
LOCAL_C_INCLUDES := $(LOCAL_PATH)/eigen
LOCAL_LDFLAGS := -llog -lz

LOCAL_STATIC_LIBRARIES := crossplatform caffe_static

ifeq ($(OPENCV_LINK_TYPE), static)
    LOCAL_STATIC_LIBRARIES += opencv2_static
else
    LOCAL_SHARED_LIBRARIES := opencv2_shared
endif

include $(BUILD_SHARED_LIBRARY)

$(call import-add-path,$(LOCAL_PATH))
$(call import-module,opencv2)
$(call import-module,protobuf)
$(call import-module,caffe)
$(call import-module,libyuv)
$(call import-module,crossplatform)
