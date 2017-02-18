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

package com.google.devtools.bazel.e4b;

import com.google.devtools.bazel.e4b.command.BazelAspectLocation;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;

/** Implementation of {@link BazelAspectLocation} using Eclipse OSGi Bundle locations */
class BazelAspectLocationImpl implements BazelAspectLocation {

  // Returns the path of the resources file from this plugin.
  private static File getAspectWorkspace() {
    try {
      URL url = Platform.getBundle(Activator.PLUGIN_ID).getEntry("resources");
      URL resolved = FileLocator.resolve(url);
      return new File(resolved.getPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static File WORKSPACE_DIRECTORY = getAspectWorkspace();

  @Override
  public File getWorkspaceDirectory() {
    return WORKSPACE_DIRECTORY;
  }

  @Override
  public String getAspectLabel() {
    return "//tools/must/be/unique:e4b_aspect.bzl%e4b_aspect.bzl";
  }

}
