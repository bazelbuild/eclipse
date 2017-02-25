workspace(name = "build_bazel_e4b")

load("//tools/build_defs:eclipse.bzl", "load_eclipse_deps")

load_eclipse_deps()

new_http_archive(
    name = "com_google_python_gflags",
    url = "https://github.com/google/python-gflags/archive/python-gflags-2.0.zip",
    strip_prefix = "python-gflags-python-gflags-2.0",
    build_file_content = """
py_library(
    name = "gflags",
    srcs = [
        "gflags.py",
        "gflags_validators.py",
    ],
    visibility = ["//visibility:public"],
)
""",
    sha256 = "344990e63d49b9b7a829aec37d5981d558fea12879f673ee7d25d2a109eb30ce",
)

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
