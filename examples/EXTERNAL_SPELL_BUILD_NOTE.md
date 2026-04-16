# Building an external spell jar now

There is no separate API module in this version.

A separate spell project should compile against:

- the Paper API
- the built `mageswand-core` jar from this repository

In other words, build the core plugin first, then point your external spell project at that jar as a `compileOnly` dependency.
