[![Build status](https://badge.buildkite.com/7d78219bdc587a47f48cc96314f36f0f25ed9d0145f56b12ab.svg)](https://buildkite.com/bazel/eclipse-plugin-postsubmit)

# Eclipse plugin for [Bazel](http://bazel.io) (**Highly experimental**)

__This project is unmaintained__

e4b is an Eclipse plugin for Bazel. It is really rudimentary.
It simply supports creating the classpath from a project and using
Bazel for incremental builds.

You can create a new Bazel project with New project > Import Bazel project
Then you can select the list of targets you want to build and the list
of directories you want to track.

It is highly experimental and support is minimal. However, we are happy
to accept contribution to make this support grows to a better shape.

## Installation

This plugin was tested with Eclipse Mars (4.5) but should also work with Eclipse Neon (4.6).

   * Start Eclipse
   * Select "Help" > "Install Software"
   * Add the update
site
[https://eclipse.bazel.build/updatesite](https://eclipse.bazel.build/updatesite).
   * Select "Uncategorized category" > "Eclipse 4 Bazel"

## Using the plugin

To import an existing Bazel project or start a new one, go to "New" > "Project..." > "Others".

## Contributing

See our [contributing
page](https://github.com/bazelbuild/eclipse/blob/master/CONTRIBUTING.md) for how
to contribute.
