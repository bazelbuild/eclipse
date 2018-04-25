// Copyright 2017 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.bazel.e4b.integration;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import build.bazel.tests.integration.Command;
import build.bazel.tests.integration.WorkspaceDriver;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.bazel.e4b.command.IdeBuildInfo;

/** Integration test for the aspect used by the plugin. */
public final class AspectIntegrationTest {

  private WorkspaceDriver driver = new WorkspaceDriver();
  private Path aspectWorkspace;

  @BeforeClass
  public static void setUpClass() throws IOException {
    WorkspaceDriver.setUpClass();
  }

  @Before
  public void setUp() throws Exception {
    driver.setUp();
    aspectWorkspace = driver.currentWorkspace();
    driver.copyFromRunfiles("build_bazel_eclipse/resources/e4b_aspect.bzl", "e4b_aspect.bzl");
    driver.scratchFile("BUILD");
    driver.newWorkspace();
    createJavaProgram();
  }

  private void createJavaProgram() throws Exception {
    driver.scratchFile("java/my/pkg/Main.java", // force-new-line
        "package my.pkg;", // force-new-line
        "import my.other.pkg.Annex;", // force-new-line
        "public class Main {", // force-new-line
        "  public static void main(String[] args) {", // force-new-line
        "    System.out.println(new Annex().helloWorld());", // force-new-line
        "  }", // force-new-line
        "}");
    driver.scratchFile("java/my/pkg/BUILD", // force-new-line
        "java_binary(name='pkg',", // force-new-line
        "            srcs=['Main.java'],", // force-new-line
        "            deps=['//java/my/other/pkg:Annex'])");
    driver.scratchFile("java/my/other/pkg/Annex.java", // force-new-line
        "package my.other.pkg;", // force-new-line


        "public class Annex {", // force-new-line
        "  public Annex() {}", // force-new-line

        "  public String helloWorld() {", // force-new-line
        "    return \"Hello, World!\";", // force-new-line
        "  }", // force-new-line
        "}");
    driver.scratchFile("java/my/other/pkg/BUILD", // force-new-line
        "java_library(name='Annex',", // force-new-line
        "             srcs=['Annex.java'],", // force-new-line
        "             visibility = ['//visibility:public'])");
    driver.scratchFile("javatests/my/other/pkg/AnnexTest.java", // force-new-line
        "package my.other.pkg;", // force-new-line

        "import static org.junit.Assert.assertEquals;", // force-new-line
        "import org.junit.Test;", // force-new-line


        "public class AnnexTest {", // force-new-line
        "  @Test", // force-new-line
        "  public void testAnnex() {", // force-new-line
        "    assertEquals(\"Hello, World!\", new Annex().helloWorld());", // force-new-line
        "  }", // force-new-line
        "}");
    driver.scratchFile("javatests/my/other/pkg/BUILD", // force-new-line
        "java_test(name='AnnexTest',", // force-new-line
        "          srcs=['AnnexTest.java'],", // force-new-line
        "          deps=['//java/my/other/pkg:Annex'])");
  }

  @Test
  public void testAspectGenerateJson() throws Exception {
    Command cmd = driver.bazelCommand("build", "--override_repository=local_eclipse_aspect=" + aspectWorkspace,
        "--aspects=@local_eclipse_aspect//:e4b_aspect.bzl%e4b_aspect", "-k",
        "--output_groups=ide-info-text,ide-resolve,-_,-defaults", "--experimental_show_artifacts",
        "//...").build();
    int retCode = cmd.run();
    assertEquals("Bazel failed to build, stderr: " + Joiner.on("\n").join(cmd.getErrorLines()),
        0, retCode);
    String[] jsonFiles = cmd.getErrorLines().stream().filter((s) -> {
      return s.startsWith(">>>") && s.endsWith(".json");
    }).map((s) -> {
      return s.substring(3);
    }).toArray(String[]::new);
    assertThat(jsonFiles).hasLength(3);

    ImmutableMap<String, IdeBuildInfo> infos =
        IdeBuildInfo.getInfo(ImmutableList.<String>copyOf(jsonFiles));

    assertThat(infos).hasSize(3);

    assertThat(infos.get("//java/my/pkg:pkg").getLabel()).isEqualTo("//java/my/pkg:pkg");
    assertThat(infos.get("//java/my/pkg:pkg").getLocation()).isEqualTo("java/my/pkg/BUILD");
    assertThat(infos.get("//java/my/pkg:pkg").getKind()).isEqualTo("java_binary");
    assertThat(infos.get("//java/my/pkg:pkg").getGeneratedJars()).isEmpty();
    assertThat(infos.get("//java/my/pkg:pkg").getDeps())
        .containsExactly("//java/my/other/pkg:Annex");
    assertThat(infos.get("//java/my/pkg:pkg").getSources())
        .containsExactly("java/my/pkg/Main.java");

    assertThat(infos.get("//java/my/other/pkg:Annex").getLabel())
        .isEqualTo("//java/my/other/pkg:Annex");
    assertThat(infos.get("//java/my/other/pkg:Annex").getLocation())
        .isEqualTo("java/my/other/pkg/BUILD");
    assertThat(infos.get("//java/my/other/pkg:Annex").getKind()).isEqualTo("java_library");
    assertThat(infos.get("//java/my/other/pkg:Annex").getGeneratedJars()).isEmpty();
    assertThat(infos.get("//java/my/other/pkg:Annex").getDeps()).isEmpty();
    assertThat(infos.get("//java/my/other/pkg:Annex").getSources())
        .containsExactly("java/my/other/pkg/Annex.java");

    assertThat(infos.get("//javatests/my/other/pkg:AnnexTest").getLabel())
        .isEqualTo("//javatests/my/other/pkg:AnnexTest");
    assertThat(infos.get("//javatests/my/other/pkg:AnnexTest").getLocation())
        .isEqualTo("javatests/my/other/pkg/BUILD");
    assertThat(infos.get("//javatests/my/other/pkg:AnnexTest").getKind()).isEqualTo("java_test");
    assertThat(infos.get("//javatests/my/other/pkg:AnnexTest").getGeneratedJars()).isEmpty();
    assertThat(infos.get("//javatests/my/other/pkg:AnnexTest").getDeps())
        .containsExactly("//java/my/other/pkg:Annex");
    assertThat(infos.get("//javatests/my/other/pkg:AnnexTest").getSources())
        .containsExactly("javatests/my/other/pkg/AnnexTest.java");
  }
}
