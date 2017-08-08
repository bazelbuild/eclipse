# This is a build file for creating a corresponding package. This package will be added to the
# package path when using the aspect so we can load the e4b_aspect.bzl Skylark aspect extension
# when calling build from Eclipse. This package has an improbable name to avoid collision with
# packages in the main repository.

filegroup(
   name = "srcs",
   srcs = glob(["**"]),
   visibility = ["//visibility:public"],
)
