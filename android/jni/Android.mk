LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE     := libluacompute
LOCAL_SRC_FILES  := libs/$(TARGET_ARCH_ABI)/libluacompute.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE     := libluajit-5.1
LOCAL_SRC_FILES  := libs/$(TARGET_ARCH_ABI)/libluajit-5.1.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE     := compute
LOCAL_C_INCLUDES += jni/include
LOCAL_SRC_FILES  := compute.c
LOCAL_STATIC_LIBRARIES := luacompute luajit-5.1
include $(BUILD_SHARED_LIBRARY)