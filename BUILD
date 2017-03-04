load(
    "//tools/build_defs:eclipse.bzl",
    "eclipse_feature",
    "eclipse_p2updatesite",
)

eclipse_feature(
    name = "com.google.devtools.bazel.e4b.feature",
    copyright = "Copyright 2016 The Bazel Authors",
    description = "Integrate Eclipse with the Bazel build system.",
    label = "Eclipse 4 Bazel",
    license = ":LICENSE.txt",
    license_url = "http://www.apache.org/licenses/LICENSE-2.0",
    plugins = ["//com.google.devtools.bazel.e4b"],
    provider = "The Bazel Authors",
    sites = {"Bazel": "https://bazel.build"},
    url = "https://github.com/bazelbuild/e4b",
    version = "0.0.3.qualifier",
    visibility = ["//visibility:public"],
)

eclipse_p2updatesite(
    name = "p2updatesite",
    description = "Eclipse plugin for Bazel",
    eclipse_features = [":com.google.devtools.bazel.e4b.feature"],
    label = "Eclipse 4 Bazel",
    url = "https://bazelbuild.github.io/e4b",
)
