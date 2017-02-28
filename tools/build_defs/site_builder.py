# Copyright 2017 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""This tool build a zipped Eclipse p2 update site from features and plugins."""

import gflags
import os
import os.path
import shutil
import subprocess
import sys
import tempfile
import zipfile

from xml.etree import ElementTree
from xml.dom import minidom

gflags.DEFINE_string("output", None, "The output files, mandatory")
gflags.MarkFlagAsRequired("output")

gflags.DEFINE_multistring(
    "feature_id",
    [],
    "Feature id to include in the site, should come "
    "along with --feature and --feature_version.")
gflags.DEFINE_multistring(
    "feature",
    [],
    "Feature file to include in the site, should come "
    "along with --feature_id and --feature_version")
gflags.DEFINE_multistring(
    "feature_version",
    [],
    "Version of a feature to include in the site, should "
    "come along with --feature and --feature_id")

gflags.DEFINE_multistring(
    "bundle",
    [],
    "Bundle file to include in the sit")

gflags.DEFINE_string(
  "name",
  None,
  "The site name (i.e. short description), mandatory")
gflags.MarkFlagAsRequired("name")

gflags.DEFINE_string("url", None, "A URL for the site, mandatory")
gflags.MarkFlagAsRequired("url")

gflags.DEFINE_string(
  "description", None, "Description of the site, mandatory")
gflags.MarkFlagAsRequired("description")

gflags.DEFINE_string(
  "java",
  "java",
  "Path to java, optional")

gflags.DEFINE_string(
  "eclipse_launcher",
  None,
  "Path to the eclipse launcher, mandatory")
gflags.MarkFlagAsRequired("eclipse_launcher")

FLAGS=gflags.FLAGS

def _features(parent):
  if (len(FLAGS.feature) != len(FLAGS.feature_id)) or (
          len(FLAGS.feature) != len(FLAGS.feature_version)):
      raise Exception(
          "Should provide the same number of "
          "time --feature, --feature_id and "
          "--feature_version")
  for i in range(0, len(FLAGS.feature)):
    p = ElementTree.SubElement(parent, "feature")
    p.set("url", "feature/%s" % os.path.basename(FLAGS.feature[i]))
    p.set("id", FLAGS.feature_id[i])
    p.set("version", FLAGS.feature_version[i])


def create_site_xml(tmp_dir):
  site = ElementTree.Element("site")
  description = ElementTree.SubElement(site, "description")
  description.set("name", FLAGS.name)
  description.set("url", FLAGS.url)
  description.text = FLAGS.description
  _features(site)

  # Pretty print the resulting tree
  output = ElementTree.tostring(site, "utf-8")
  reparsed = minidom.parseString(output)
  with open(os.path.join(tmp_dir, "site.xml"), "w") as f:
    f.write(reparsed.toprettyxml(indent="  ", encoding="UTF-8"))


def copy_artifacts(tmp_dir):
  feature_dir = os.path.join(tmp_dir, "features")
  bundle_dir = os.path.join(tmp_dir, "plugins")
  os.mkdir(feature_dir)
  os.mkdir(bundle_dir)
  for f in FLAGS.feature:
    shutil.copyfile(f, os.path.join(feature_dir, os.path.basename(f)))
  for p in FLAGS.bundle:
    shutil.copyfile(p, os.path.join(bundle_dir, os.path.basename(p)))


def generate_metadata(tmp_dir):
  tmp_dir2 = tempfile.mkdtemp()

  args = [
    FLAGS.java,
    "-jar", FLAGS.eclipse_launcher,
    "-application", "org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher",
    "-metadataRepository", "file:/" + tmp_dir,
    "-artifactRepository", "file:/" + tmp_dir,
    "-configuration", tmp_dir2,
    "-source", tmp_dir,
    "-compress", "-publishArtifacts"]
  process = subprocess.Popen(args, stdout=subprocess.PIPE)
  stdout, _ = process.communicate()
  if process.returncode:
    sys.stdout.write(stdout)
    for root, dirs, files in os.walk(tmp_dir2):
      for f in files:
        if f.endswith(".log"):
          with open(os.path.join(root, f), "r") as fi:
            sys.stderr.write("Log %s: %s\n" % (f, fi.read()))
    shutil.rmtree(tmp_dir)
    sys.exit(process.returncode)
  shutil.rmtree(tmp_dir2)


def _zipinfo(filename):
  result = zipfile.ZipInfo(filename, (1980, 1, 1, 0, 0, 0))
  result.external_attr = 0o644 << 16L
  return result

def zip_all(tmp_dir):
  with zipfile.ZipFile(FLAGS.output, "w", zipfile.ZIP_DEFLATED) as zf:
    for root, dirs, files in os.walk(tmp_dir):
      reldir = os.path.relpath(root, tmp_dir)
      if reldir == ".":
        reldir = ""
      for f in files:
        with open(os.path.join(root, f), "r") as fi:
          zf.writestr(_zipinfo(os.path.join(reldir, f)), fi.read())


def main(unused_argv):
  tmp_dir = tempfile.mkdtemp()
  create_site_xml(tmp_dir)
  copy_artifacts(tmp_dir)
  generate_metadata(tmp_dir)
  zip_all(tmp_dir)
  shutil.rmtree(tmp_dir)


if __name__ == "__main__":
  main(FLAGS(sys.argv))
