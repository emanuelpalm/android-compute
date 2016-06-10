# Palm/compute Server

The server currently consists of a [single file][main], apart from classes
shared with the Android client via the [Core][core] module, that simply awaits
incoming client connections, registers a lambda for uppercasing batches, and
then proceeds to send an eternal sequence of "hello" messages for the client to
uppercase.

[main]: src/main/java/se/ltu/emapal/compute/main.kt
[core]: /core

## Significant Source Files

Understanding the [Android client][android], at least superficially, before
diving into the server source code is recommended. If wanting to improve upon
the existing code used for TCP communication, some grasp of the Java NIO
[ServerSocketChannel][ssch], [SocketChannel][sch], and [Selector][sel] is
required, unless this ambition is to replace the NIO TCP code with such that
uses some other library.

- **[ComputeChannel][cc]** - Transports byte arrays between client and service.
- **[ComputeServiceTcp.kt][cst]** - Encapsulates a single client connection.
- **[ComputeServiceTcpListener.kt][cstl]** - Listens for incoming connections.
- **[main.kt][main]** - High-level management of incoming client connections.

[android]: /android
[sch]: https://docs.oracle.com/javase/8/docs/api/java/nio/channels/SocketChannel.html
[ssch]: https://docs.oracle.com/javase/8/docs/api/java/nio/channels/ServerSocketChannel.html
[sel]: https://docs.oracle.com/javase/8/docs/api/java/nio/channels/Selector.html
[cc]: /core/src/main/java/se/ltu/emapal/compute/io/ComputeChannel.kt
[cst]: /core/src/main/java/se/ltu/emapal/compute/io/ComputeServiceTcp.kt
[cstl]: /core/src/main/java/se/ltu/emapal/compute/io/ComputeServiceTcpListener.kt

## Building and Running

Building the server requires Java JRE 6 or later, [Gradle][grad], which is
already included in the repository via a [Gradle build wrapper][grwr]. The
most convenient way to build and run the server is to use an IDE with Gradle
support.

[grad]: http://gradle.org/
[grwr]: /gradle/wrapper

If wanting to use the command line, some relevant commands are presented below.
They are all expected to be executed with the repository root folder as current
working directory.

**List available server Gradle tasks**
```sh
$ ./gradlew :server:tasks
```

**Run server on port 62000**
```sh
$ ./gradlew :server:run
```

**Assemble JAR file**
```sh
$ ./gradlew :server:jar
```
