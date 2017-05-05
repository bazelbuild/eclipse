// Copyright 2016 The Bazel Authors. All rights reserved.
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

package com.google.devtools.bazel.e4b.preferences;

import java.io.File;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.google.common.collect.ImmutableList;
import com.google.devtools.bazel.e4b.Activator;

/**
 * Initialize the preferences of Bazel. The only preferences stored for now is the path to the Bazel
 * binary, which is expected to be in /usr/local/bin/bazel by default.
 */
public class BazelPreferenceInitializer extends AbstractPreferenceInitializer {

  private static final ImmutableList<String> BAZEL_PATH_CANDIDATES =
      ImmutableList.of("/usr/local/bin/bazel", "/usr/bin/bazel");

  @Override
  public void initializeDefaultPreferences() {
    IPreferenceStore store = Activator.getDefault().getPreferenceStore();
    store.setDefault("BAZEL_PATH", findBazelPath());
  }

  private static String findBazelPath() {
    for (String path : BAZEL_PATH_CANDIDATES) {
      if (new File(path).isFile()) {
        return path;
      }
    }

    System.err.println("No candidate path for blaze found. Using default.");

    return Activator.DEFAULT_BAZEL_PATH;
  }
}
