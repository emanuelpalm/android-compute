LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := compute
LOCAL_SRC_FILES := compute.c

include $(BUILD_SHARED_LIBRARY)