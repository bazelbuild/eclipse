load("//tools/build_defs:eclipse.bzl", "eclipse_plugin")

eclipse_plugin(
    name = "com.google.devtools.bazel.e4b",
    srcs = glob(["src/**/*.java"]),
    activator = "com.google.devtools.bazel.e4b.Activator",
    bundle_name = "Eclipse 4 Bazel",
    resources = glob(["resources/**"]),
    vendor = "The Bazel Authors",
    version = "0.0.3.qualifier",
    visibility = ["//visibility:public"],
    deps = [
        "//com.google.devtools.bazel.e4b/src/com/google/devtools/bazel/e4b/command",
        "@com_google_guava//jar",
    ],
)
