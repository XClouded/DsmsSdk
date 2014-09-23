LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := dsms
LOCAL_LDLIBS    := -llog
LOCAL_SRC_FILES := base64.cpp\
                   aes_core.cpp\
                   dsms.cpp
include $(BUILD_SHARED_LIBRARY)
