# Palm/compute JNI

This folder most significantly contains the `compute.c` file, which implements the native methods
of the `AndroidComputeContext` class. This file is re-compiled, if necessary, using the Android
NDK, for each supported Android platform. Please consult the Android NDK documentation for further
details regarding how this is done, if necessary.