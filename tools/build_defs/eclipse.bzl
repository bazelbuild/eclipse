# Copyright 2017 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# TODO(dmarting): mirror those jars.
# TODO(dmarting): Provide checksums for those files.
_EQUINOX_MIRROR_URL="http://download.eclipse.org/eclipse/updates/"
_ECLIPSE_VERSION="4.5/R-4.5.2-201602121500"
_DOWNLOAD_URL = _EQUINOX_MIRROR_URL + "/" + _ECLIPSE_VERSION + "/plugins/%s_%s.jar"

# TODO(dmarting): make this configurable?
_DECLARED_DEPS = [
    "org.eclipse.ui.console",
    "org.eclipse.ui",
    "org.eclipse.core.resources",
    "org.eclipse.ui.ide",
    "org.eclipse.jdt.core",
    "org.eclipse.core.runtime",
    "javax.inject",
]

_ECLIPSE_PLUGIN_DEPS = {
  # Declared deps.
  "org.eclipse.ui.console": "3.6.100.v20150822-1912",
  "javax.inject": "1.0.0.v20091030",
  "org.eclipse.core.runtime": "3.11.1.v20150903-1804",
  "org.eclipse.ui": "3.107.0.v20150507-1945",
  "org.eclipse.jdt.core": "3.11.2.v20160128-0629",
  "org.eclipse.core.resources": "3.10.1.v20150725-1910",
  "org.eclipse.ui.ide": "3.11.0.v20150825-2158",
  # implicit deps
  "org.eclipse.swt": "3.104.2.v20160212-1350",
  # TODO(dmarting): make it works cross platform. This is not a problem while
  # we are using the dependency to compile but this might become an issue if
  # we need to run the plugin (e.g. to test it).
  #
  # Available platforms: cocoa.macosx.x86_64 gtk.aix.ppc gtk.aix.ppc64
  # gtk.hpux.ia64 gtk.linux.ppc gtk.linux.ppc64 gtk.linux.ppc64le gtk.linux.s390
  # gtk.linux.s390x gtk.linux.x86 gtk.linux.x86_64 gtk.solaris.sparc
  # gtk.solaris.x86 win32.win32.x86 win32.win32.x86_64
  "org.eclipse.swt.gtk.linux.ppc": "3.104.2.v20160212-1350",
  "org.eclipse.jface": "3.11.1.v20160128-1644",
  "org.eclipse.core.commands": "3.7.0.v20150422-0725",
  "org.eclipse.ui.workbench": "3.107.1.v20160120-2131",
  "org.eclipse.e4.ui.workbench3": "0.13.0.v20150422-0725",
  "org.eclipse.jdt.compiler.apt": "1.2.0.v20150514-0146",
  "org.eclipse.jdt.compiler.tool": "1.1.0.v20150513-2007",
  "javax.annotation": "1.2.0.v201401042248",
  "org.eclipse.osgi": "3.10.102.v20160118-1700",
  "org.eclipse.osgi.compatibility.state": "1.0.100.v20150402-1551",
  "org.eclipse.equinox.common": "3.7.0.v20150402-1709",
  "org.eclipse.core.jobs": "3.7.0.v20150330-2103",
  "org.eclipse.core.runtime.compatibility.registry": "3.6.0.v20150318-1505",
  "org.eclipse.equinox.registry": "3.6.0.v20150318-1503",
  "org.eclipse.equinox.preferences": "3.5.300.v20150408-1437",
  "org.eclipse.core.contenttype": "3.5.0.v20150421-2214",
  "org.eclipse.equinox.app": "1.3.300.v20150423-1356",
  "org.eclipse.ui.views": "3.8.0.v20150422-0725",
}


def load_eclipse_deps():
  """Load dependencies of the Eclipse plugin."""
  for plugin in _ECLIPSE_PLUGIN_DEPS:
    native.http_file(
      name = plugin.replace(".", "_"),
      url = _DOWNLOAD_URL % (plugin, _ECLIPSE_PLUGIN_DEPS[plugin]),
    )


def eclipse_plugin(name, version, bundle_name, activator=None,
                   vendor=None, **kwargs):
  """A macro to generate an eclipse plugin (see java_binary)."""
  jars = ["@%s//file" % plugin.replace(".", "_")
          for plugin in _ECLIPSE_PLUGIN_DEPS]
  native.java_import(
    name = name + "-deps",
    neverlink = 1,
    jars = jars,
  )
  deps = [name + "-deps"]
  if "deps" in kwargs:
    deps = deps + kwargs["deps"]
  args = {k: kwargs[k]
          for k in kwargs
          if k not in [
              "deps",
              "classpath_resources",
              "deploy_manifest_lines",
              "visibility",
              "main_class"]}
  visibility = kwargs["visibility"] if "visibility" in kwargs else None
  # Generate the .api_description to put in the final jar
  native.genrule(
    name = name + ".api_description",
    srcs = [],
    outs = [name + "/.api_description"],
    cmd = """
cat <<EOF >$@
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<component name="%s_%s" version="1.2">
    <plugin id="%s_%s"/>
</component>
EOF
""" % (name, version, name, version))
  # Generate the final jar (a deploy jar)
  native.java_binary(
    name = name + "-bin",
    main_class = "does.not.exist",
    classpath_resources = [
        ":%s/.api_description" % name,
        # TODO(dmarting): this add the plugin.xml dependency here, maybe we
        # should move that to the BUILD file to avoid surprise? 
        "plugin.xml",
    ] + (kwargs["classpath_resources"]
         if "classpath_resources" in kwargs else []),
    deploy_manifest_lines = [
      "Bundle-ManifestVersion: 2",
      "Bundle-Name: " + bundle_name,
      # TODO(dmarting): We mark always as singleton, make it configurable?
      "Bundle-SymbolicName: %s;singleton:=true" % name,
      "Bundle-Version: " + version,
      "Require-Bundle: " + ", ".join(_DECLARED_DEPS),
      # TODO(dmarting): Take the java version from java_toolchain.
      "Bundle-RequiredExecutionEnvironment: JavaSE-1.8",
      "Bundle-ActivationPolicy: lazy",
      "Bundle-ClassPath: .",
    ] + (
      ["Bundle-Activator: " + activator] if activator else []
    ) + (
      ["Bundle-Vendor: " + vendor] if vendor else []
    ) + (kwargs["deploy_manifest_lines"]
        if "deploy_manifest_lines" in kwargs else []),
    deps = deps,
    **args)
  # Rename the output to the correct name
  native.genrule(
    name = name,
    srcs = [":%s-bin_deploy.jar" % name],
    outs = ["%s_%s.jar" % (name, version)],
    cmd = "cp $< $@",
    output_to_bindir = 1,
    visibility = visibility,
  )


def _eclipse_feature_impl(ctx):
  feature_xml = ctx.new_file(ctx.outputs.out, ctx.label.name + ".xml")
  ctx.action(
    outputs = [feature_xml],
    inputs = [ctx.file.license],
    executable = ctx.executable._builder,
    arguments = [
      "--output=" + feature_xml.path,
      "--id=" + ctx.label.name,
      "--label=" + ctx.attr.label,
      "--version=" + ctx.attr.version,
      "--provider=" + ctx.attr.provider,
      "--url=" + ctx.attr.url,
      "--description=" + ctx.attr.description,
      "--copyright=" + ctx.attr.copyright,
      "--license_url=" + ctx.attr.license_url,
      "--license=" + ctx.file.license.path] + [
        "--site=%s=%s" % (site, ctx.attr.sites[site])
        for site in ctx.attr.sites] + [
          "--plugin=" + p.basename for p in ctx.files.plugins])
  ctx.action(
      outputs = [ctx.outputs.out],
      inputs = [feature_xml],
      executable = ctx.executable._zipper,
      arguments = ["c",
                   ctx.outputs.out.path,
                   "feature.xml=" + feature_xml.path],
  )


eclipse_feature = rule(
   implementation=_eclipse_feature_impl,
   attrs = {
       "label": attr.string(mandatory=True),
       "version": attr.string(mandatory=True),
       "provider": attr.string(mandatory=True),
       "description": attr.string(mandatory=True),
       "url": attr.string(mandatory=True),
       "copyright": attr.string(mandatory=True),
       "license_url": attr.string(mandatory=True),
       "license": attr.label(mandatory=True, allow_single_file=True),
       "sites": attr.string_dict(),
       # TODO(dmarting): restrict what can be passed to the plugins attribute.
       "plugins": attr.label_list(),
       "_zipper": attr.label(default=Label("@bazel_tools//tools/zip:zipper"),
                             executable=True,
                             cfg="host"),
       "_builder": attr.label(default=Label("//tools/build_defs:feature_builder"),
                              executable=True,
                              cfg="host"),
    },
    outputs = {"out": "%{name}_%{version}.jar"})
"""Create an eclipse feature jar."""


# TODO(dmarting): implement eclipse_p2updatesite.
# An p2 site is a site which has the following layout:
# /site.xml (see p2updatesite/site.xml)
# /artifacts.jar
#   jar that contains only one XML file called artifacts.xml.
#   This file contains the list of artifacts available on that
#   update site, and the mapping between the name of the artifact,
#   and the file position.
# /content.jar
#   jar that contains only one XML file called content.xml.
#   This XML file describe which feature are available in that
#   update site and their description (so a client may read only
#   that file to list the content of the repository to the user).
# /plugins
#   plugin1_v1.jar -> OSGi Bundle for eclipse
# /features
#   feature1_v1.jar -> feature jar
