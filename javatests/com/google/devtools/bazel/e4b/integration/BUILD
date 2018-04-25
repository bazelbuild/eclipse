load("@build_bazel_integration_testing//tools:bazel_java_integration_test.bzl", "bazel_java_integration_test")

bazel_java_integration_test(
    name = "AspectIntegrationTest",
    srcs = ["AspectIntegrationTest.java"],
    data = ["//resources:srcs"],
    deps = [
        "//java/com/google/devtools/bazel/e4b/command",
        "@org_junit//jar",
        "@org_hamcrest_core//jar",
        "@com_google_guava//jar",
        "@com_google_truth//jar",
    ],
)
