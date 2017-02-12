// Copyright 2016 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.bazel.e4b.classpath;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.service.prefs.BackingStoreException;

import com.google.devtools.bazel.e4b.Activator;
import com.google.devtools.bazel.e4b.command.BazelNotFoundException;

public class BazelClasspathContainerInitilalizer extends ClasspathContainerInitializer {

  @Override
  public void initialize(IPath path, IJavaProject project) throws CoreException {
    try {
      BazelClasspathContainer container = new BazelClasspathContainer(path, project);
      if (container.isValid()) {
        JavaCore.setClasspathContainer(path, new IJavaProject[] {project},
            new IClasspathContainer[] {container}, null);
      } else {
        Activator.error("Unable to create classpath container (Not a Bazel workspace?)");
      }
    } catch (IOException | InterruptedException | BackingStoreException e) {
      Activator.error("Error while creating Bazel classpath container.", e);
    } catch (BazelNotFoundException e) {
      Activator.error("Bazel not found: " + e.getMessage());
    }
  }

}
