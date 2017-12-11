load(
    "//tools/build_defs:eclipse.bzl",
    "eclipse_plugin",
    "eclipse_feature",
    "eclipse_p2updatesite",
)
load(":version.bzl", "VERSION")

eclipse_plugin(
    name = "com.google.devtools.bazel.e4b",
    srcs = glob(["java/**/*.java"]),
    activator = "com.google.devtools.bazel.e4b.Activator",
    bundle_name = "Eclipse 4 Bazel",
    resources = ["//resources:srcs"],
    vendor = "The Bazel Authors",
    version = VERSION,
    visibility = ["//visibility:public"],
    deps = [
        "//java/com/google/devtools/bazel/e4b/command",
        "//java/com/google/devtools/bazel/e4b/projectviews",
        "@com_google_guava//jar",
    ],
)

eclipse_feature(
    name = "com.google.devtools.bazel.e4b.feature",
    copyright = "Copyright 2016 The Bazel Authors",
    description = "Integrate Eclipse with the Bazel build system.",
    label = "Eclipse 4 Bazel",
    license = ":LICENSE.txt",
    license_url = "http://www.apache.org/licenses/LICENSE-2.0",
    plugins = ["//:com.google.devtools.bazel.e4b"],
    provider = "The Bazel Authors",
    sites = {"Bazel": "https://bazel.build"},
    url = "https://github.com/bazelbuild/e4b",
    version = VERSION,
    visibility = ["//visibility:public"],
)

eclipse_p2updatesite(
    name = "p2updatesite",
    description = "Eclipse plugin for Bazel",
    eclipse_features = [":com.google.devtools.bazel.e4b.feature"],
    label = "Eclipse 4 Bazel",
    url = "https://eclipse.bazel.build",
    visibility = ["//tools/release:__pkg__"],
)
