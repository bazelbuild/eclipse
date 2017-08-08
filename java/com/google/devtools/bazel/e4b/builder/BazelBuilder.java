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

package com.google.devtools.bazel.e4b.builder;

import java.io.IOException;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.osgi.service.prefs.BackingStoreException;

import com.google.devtools.bazel.e4b.Activator;
import com.google.devtools.bazel.e4b.command.BazelCommand.BazelInstance;
import com.google.devtools.bazel.e4b.command.BazelNotFoundException;

public class BazelBuilder extends IncrementalProjectBuilder {

  @Override
  protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor)
      throws CoreException {
    IProject project = getProject();
    try {
      BazelInstance instance = Activator.getBazelCommandInstance(project);
      if (kind == INCREMENTAL_BUILD || kind == AUTO_BUILD) {
        IResourceDelta delta = getDelta(getProject());
        if (delta == null || delta.getAffectedChildren().length == 0) {
          // null build, skip calling Bazel.
          return null;
        }
      }
      instance.markAsDirty();
      instance.build(Activator.getTargets(project));
    } catch (BackingStoreException | IOException | InterruptedException e) {
      Activator.error("Failed to build " + project.getName(), e);
    } catch (BazelNotFoundException e) {
      Activator.error("Bazel not found: " + e.getMessage());
    }
    return null;
  }
}
