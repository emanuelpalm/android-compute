![icon](/android/src/main/res/mipmap-xxhdpi/ic_launcher.png)

# Palm/compute Android Client

The Android client is complete enough to be useful, but lacks some conveniences
that would improve its utility. Currently, upon start, it presents a form for
entering the address of some compute service. Upon successful connection to
such a service, it displays basic statistics about any work progress.

The client currently runs on any platform on which [LuaJIT][ljit] can run,
which is most 32-bit Android platforms, and the 64-bit x86 platform.

[ljit]: http://luajit.org/

## Architecture

At the heart of the client is the [Computer][cmp] class, which actually is part
of the [Core][core] library, and not of the actual Android client. An instance
of this class keeps track a pool of threads, one for each available CPU core,
each with its own [AndroidComputeContext][acc]. When the Computer receives new
work via its [ComputeClientTcp][cclt], it puts that work on a queue, and the
first free ComputeContext in its thread pool will take it from the queue and
process it.

[lcm]: https://github.com/emanuelpalm/lua-compute
[cmp]: /core/src/main/java/se/ltu/emapal/compute/Computer.kt
[core]: /core
[acc]: src/main/java/se/ltu/emapal/compute/AndroidComputeContext.java
[cclt]: /core/src/main/java/se/ltu/emapal/compute/io/ComputeClientTcp.kt

![diagram](/design/docs/palm-compute-diagram.png)

Of special significance is the [AndroidComputeContext][acc] class, which binds
the [Lua/compute][lcm] C library to a concrete Java class via [JNI][jni]. Its C
counterpart is the [compute.c][comc] file.

[comc]: jni/compute.c
[jni]: http://docs.oracle.com/javase/6/docs/technotes/guides/jni/spec/jniTOC.html

## Significant Source Files

- **[ActivityMain.kt][actm]** - Android main activity.
- **[AndroidComputeContext.kt][acc]** - Context used to execute compute tasks.
- **[AsyncTaskCreateComputer.kt][atcc]** - Task for creating Computer object.
- **[compute.c][comc]** - Bridge between AndroidComputeContext and Lua/compute.
- **[Computer.kt][cmp]** - High-level management of compute tasks.
- **[ComputeChannel][cc]** - Transports byte arrays between client and service.
- **[ComputeClientTcp.kt][cct]** - Encapsulates a single client connection.
- **[ComputeMessage.kt][cmsg]** - Defines client/service messages.

[actm]: src/main/java/se/ltu/emapal/compute/client/android/ActivityMain.kt
[atcc]: src/main/java/se/ltu/emapal/compute/client/android/AsyncTaskCreateComputer.kt
[cc]: /core/src/main/java/se/ltu/emapal/compute/io/ComputeChannel.kt
[cct]: /core/src/main/java/se/ltu/emapal/compute/io/ComputeClientTcp.kt
[cmsg]: /core/src/main/java/se/ltu/emapal/compute/io/ComputeMessage.kt

## Future Improvements

Areas of future improvement could be to display log messages, better
statistics, allow it to look up compute services in some on-line registry, make
it build with a regular Lua library for platforms where [LuaJIT][ljit] cannot
be used, or maybe even be used as a compute service itself.

## Building and Running

Building and running the client requires both the Android SDK and NDK. The most
convenient way to acquire and use both of these is by downloading
[Android Studio][ands].

[ands]: https://developer.android.com/studio/index.html
