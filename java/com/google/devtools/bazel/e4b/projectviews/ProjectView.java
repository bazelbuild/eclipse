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

import java.util.List;

/**
 * This is an interface to defined a project view from the IntelliJ plugin for Bazel
 * (https://ij.bazel.build) so that project view can be shared between IntelliJ and Eclipse users.
 *
 * <p>
 * See http://ij.bazel.build/docs/project-views.html for the specification of the project view. This
 * project view support only a subset relevant for the Eclipse plugin.
 */
public interface ProjectView {
  /**
   * List of directories defined in the {@code directories} section of the project view. These are
   * the directories to include as source directories.
   */
  public List<String> getDirectories();

  /**
   * List of targets to build defined in the {@code targets} section of the project view.
   */
  public List<String> getTargets();

  /**
   * Return a number (e.g. 7) giving the java version that the IDE should support (section
   * {@code java_language_level} of the project view).
   */
  public int getJavaLanguageLevel();

  /**
   * List of build flags to pass to Bazel defined in the {@code build_flags} section of the project
   * view.
   */
  public List<String> getBuildFlags();

  /** Returns a builder to construct an project view object. */
  public static Builder builder() {
    return new Builder();
  }
}
