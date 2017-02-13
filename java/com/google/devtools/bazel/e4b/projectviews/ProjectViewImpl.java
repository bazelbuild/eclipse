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

package com.google.devtools.bazel.e4b.projectviews;

import com.google.common.collect.ImmutableList;
import java.util.List;

/** Implementation of the ProjectView object */
final class ProjectViewImpl implements ProjectView {

  private final ImmutableList<String> directories;
  private final ImmutableList<String> targets;
  private final int javaLanguageLevel;
  private final ImmutableList<String> buildFlags;

  public ProjectViewImpl(List<String> directories, List<String> targets, int javaLanguageLevel,
      List<String> buildFlags) {
    this.directories = ImmutableList.copyOf(directories);
    this.targets = ImmutableList.copyOf(targets);
    this.javaLanguageLevel = javaLanguageLevel;
    this.buildFlags = ImmutableList.copyOf(buildFlags);
  }

  @Override
  public List<String> getDirectories() {
    return directories;
  }

  @Override
  public List<String> getTargets() {
    return targets;
  }

  @Override
  public int getJavaLanguageLevel() {
    return javaLanguageLevel;
  }

  @Override
  public List<String> getBuildFlags() {
    return buildFlags;
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer();
    addList(result, directories, "directories");
    addList(result, targets, "targets");
    if (javaLanguageLevel > 0) {
      result.append("java_language_level: ").append(javaLanguageLevel).append("\n\n");
    }
    addList(result, buildFlags, "build_flags");
    return result.toString();
  }

  private static void addList(StringBuffer result, List<String> list, String header) {
    if (!list.isEmpty()) {
      result.append(header).append(":\n");
      for (String el : list) {
        result.append("  ").append(el).append("\n");
      }
      result.append("\n");
    }
  }
}
