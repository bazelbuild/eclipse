# Eclipse plugin for [Bazel](http://bazel.io) (**Highly experimental**)

e4b is an Eclipse plugin for Bazel. It is really rudimentary.
It simply supports creating the classpath from a project and using
Bazel for incremental builds..

You can create a new Bazel project with New project > Import Bazel project
Then you can select the list of targets you want to build and the list
of directories you want to track.

It is highly experimental and support is minimal. However, we are happy
to accept contribution to make this support grows to a better shape.

## Missing features

Interesting feature to add is first to be able to launch test from
Eclipse, correctly adding the eclipse debugger to the JVM.

Second, better support for BUILD file would be interesting. Maybe
using Xtext.

Finally, the code is completely missing tests and improving the test
coverage would be great.
