load("//tools/build_defs:eclipse_plugin.bzl", "eclipse_plugin")

eclipse_plugin(
    name = "com.google.devtools.bazel.e4b",
    srcs = glob(["src/**/*.java"]),
    bundle_name = "Eclipse 4 Bazel",
    resources = glob(["resources/**"]),
    version = "0.0.3.qualifier",
    vendor = "The Bazel Authors",
    activator = "com.google.devtools.bazel.e4b.Activator",
    deps = [
        "//com.google.devtools.bazel.e4b/src/com/google/devtools/bazel/e4b/command",
        "@com_google_guava//jar",
    ],
)
