APP_ABI := armeabi-v7a arm64-v8a
APP_PLATFORM := android-16
APP_CPPFLAGS += -std=c++11

#APP_CPPFLAGS += -DNDEBUG
#APP_CPPFLAGS += -ffunction-sections -fdata-sections -fvisibility=hidden
#APP_LDFLAGS += -Wl,--gc-sections

APP_STL := c++_shared
#APP_STL := gnustl_static

OPENCV_LINK_TYPE := static
