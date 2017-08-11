load("//tools/build_defs:bazel_integration_test.bzl", "BAZEL_VERSIONS")

java_library(
    name = "base",
    srcs = ["BazelBaseTestCase.java"],
    data = [
        "@build_bazel_bazel_%s//:bazel_binary" % v.replace(".", "_")
        for v in BAZEL_VERSIONS
    ],
    deps = [
        "//java/com/google/devtools/bazel/e4b/command",
        "@com_google_guava//jar",
        "@org_junit//jar",
    ],
)

[java_test(
    name = "base_integration_test_with_bazel_" + version,
    srcs = ["BazelBaseTestCaseTest.java"],
    test_class = "com.google.devtools.bazel.e4b.integration.BazelBaseTestCaseTest",
    jvm_flags = ["-Dbazel.version=" + version],
    deps = [
        ":base",
        "//java/com/google/devtools/bazel/e4b/command",
        "@com_google_truth//jar",
        "@org_hamcrest_core//jar",
        "@org_junit//jar",
    ],
)for version in BAZEL_VERSIONS]

[java_test(
    name = "aspect_integration_test_with_bazel_" + version,
    srcs = ["AspectIntegrationTest.java"],
    test_class = "com.google.devtools.bazel.e4b.integration.AspectIntegrationTest",
    data = ["//resources:srcs"],
    jvm_flags = ["-Dbazel.version=" + version],
    deps = [
        ":base",
        "//java/com/google/devtools/bazel/e4b/command",
        "@com_google_guava//jar",
        "@com_google_truth//jar",
        "@org_hamcrest_core//jar",
        "@org_junit//jar",
    ],
) for version in BAZEL_VERSIONS]
