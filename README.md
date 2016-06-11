![icon](/android/src/main/res/mipmap-xxhdpi/ic_launcher.png)

# Palm/compute

An application for spreading out compute tasks, written as [Lua][lua] programs,
in a cluster of compute clients. Currently includes an [Android client][droid],
primarily implemented in [Kotlin][kotlin], and a prototype [Server][server]
implemented in the same programming language.

[lua]: http://www.lua.org/
[droid]: /android
[kotlin]: https://kotlinlang.org/
[server]: /server

The application wraps the [Lua/compute][lcm] library, which is a C library that
makes Lua useful as a context for registering compute functions, referred to as
*lambdas*, and submitting work, in the form of byte arrays, to those functions.

[lcm]: https://github.com/emanuelpalm/lua-compute

*The application is to be regarded as a prototype, and is currently only*
*finished enough to demonstrate that the Android client is working. The server*
*only sends bogus work to connected clients.*

## Manual

At this point, the application isn't finished enough to be useful for anyone
not interested in contributing code to this project, or using the code as basis
for another one. It has a working Android client, and a prototype server
implementation. Enough documentation for someone somewhat familiar with Android
and Java development is given via the links below.

- [Android Client](/android)
- [Server](/server)

## Design & Concepts

### Clients & Services

There are two primary categories of agents in a Palm/compute application,
namely *clients*, and *services*. A client is any agent able to accept incoming
*lambdas* and *batches*, and a service is any agent able to submit such to
known clients. The diagram below illustrates a service sending a batch to some client and receiving the result.

![diagram](/design/docs/palm-compute-diagram.png)

### Lambdas & Batches

A *lambda* is a Lua program that calls the function `lcm:register()` once with
a function as argument. This function accepts a batch as its single parameter,
and returns another processed batch whenever finished.

A *batch* is nothing more or less than a Lua string. Each lambda program needs
to register a function that accepts such a string as its single parameter, and
return another such string containing any processing results. Lua strings can
contain any sequence of bytes, and could potentially contain JSON, CBOR, or any
other kind of structured data.

The below Lua program shows what a lambda could look like that accepts and
uppercases regular text batches. It also shows the use of `lcm:log()`, which
can be used to send log data to the service that submitted the current batch
while its lambda program is running.

```lua
function hello()
    lcm:log("Hello?")
end

lcm:register(function (batch)
    hello()
    lcm:log("I just received \"" .. batch .. "\", and now I will uppercase it!")
    return string.upper(batch)
end)
```

## Significant Technologies

This project brings together quite a few technologies and concepts, and some
familiarity with these might be required, depending on which parts of the
application are of interest, if wanting to further develop it, or at least
understand the existing source code.

- **Functional Programming** - If not at all familiar with concepts common in
  this domain, there might be some confusion regarding design choices manifest
  in the source code. Below follows a list of principles applied in the
  project. Information about these topics ought to be readily available via any
  of the popular Internet search engines.
  - *Immutability over mutability* - `let` is preferred over `var`.
  - *Functional purity* - Side effects, especially exceptions, are avoided.
  - *Higher-order functions* - Lambda functions are used extensively.
- **Reactive Programming** - For the uninitiated, RP could be explained as a
  more feature-rich alternative to the *Observer Pattern*. This particular
  project uses the [RxJava][rxjv] library.
- **Kotlin** - A JVM language that offers 100% interoperability with Java. Is a
  strongly typed hybrid functional and object oriented programming language,
  that brings many concepts popularized via languages such as Python, Ruby,
  etc. to the JVM.
- **Lua** - An embeddable scripting language that has been used extensively in
  the computer games industry for the last decade. Has the benefits of having a
  small runtime, a limited standard library, a [JIT][ljit] implementation of
  significant efficiency, and being quite straightforward to learn.
- **JNI** - An interface between C or C++ to/from Java. Diving into this topic
  is only required if wanting to work on the bindings to the Lua/compute C
  library.
- **Android** - As the application includes and Android client, improving on
  this client would require using Android APIs.

[rxjv]: https://github.com/ReactiveX/RxJava
[ljit]: http://luajit.org/

## Building and Running

Instructions are available via the below links.

- [Android Client](/android)
- [Server](/server)

## Contributing

Bug fixes and minor enhancements are most appreciated. If wanting to add some
more significant functionality, please create a question issue and discuss it
first.

All contributions are expected to adhere to the code style and conventions
adhered to by the project.

The code currently in the repository was written in Android Studio and
IntelliJ. An IDE with good support for Kotlin is recommended.
