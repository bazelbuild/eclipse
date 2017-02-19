workspace(name = "build_bazel_e4b")

load("//tools/build_defs:eclipse_plugin.bzl", "load_eclipse_deps")

load_eclipse_deps()

# TODO(dmarting): Use http_file and relies on a mirror instead of maven_jar
maven_jar(
    name = "com_google_guava",
    artifact = "com.google.guava:guava:jar:21.0",
)

maven_jar(
    name = "org_json",
    artifact = "org.json:json:jar:20160212",
)

maven_jar(
    name = "org_hamcrest_core",
    artifact = "org.hamcrest:hamcrest-core:jar:1.3",
)

maven_jar(
    name = "org_junit",
    artifact = "junit:junit:jar:4.11",
)

maven_jar(
    name = "com_google_truth",
    artifact = "com.google.truth:truth:jar:0.31",
)
