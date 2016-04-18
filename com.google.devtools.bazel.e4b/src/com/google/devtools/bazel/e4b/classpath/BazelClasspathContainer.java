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

package com.google.devtools.bazel.e4b.classpath;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.service.prefs.BackingStoreException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.bazel.e4b.Activator;
import com.google.devtools.bazel.e4b.command.BazelCommand.BazelInstance;
import com.google.devtools.bazel.e4b.command.IdeBuildInfo;
import com.google.devtools.bazel.e4b.command.IdeBuildInfo.Jars;

public class BazelClasspathContainer implements IClasspathContainer {
  public static final String CONTAINER_NAME = "com.google.devtools.bazel.e4b.BAZEL_CONTAINER";

  private final IPath path;
  private final IJavaProject project;
  private final BazelInstance instance;

  public BazelClasspathContainer(IPath path, IJavaProject project)
      throws IOException, InterruptedException, BackingStoreException, JavaModelException {
    this.path = path;
    this.project = project;
    this.instance = Activator.getBazelCommandInstance(project.getProject());
  }

  private boolean isSourcePath(String path) throws JavaModelException, BackingStoreException {
    Path pp = new File(instance.getWorkspaceRoot().toString() + File.separator + path).toPath();
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    for (IClasspathEntry entry : project.getRawClasspath()) {
      if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
        IResource res = root.findMember(entry.getPath());
        if (res != null) {
          String file = res.getLocation().toOSString();
          if (!file.isEmpty() && pp.startsWith(file)) {
            IPath[] inclusionPatterns = entry.getInclusionPatterns();
            if (!matchPatterns(pp, entry.getExclusionPatterns()) && (inclusionPatterns == null
                || inclusionPatterns.length == 0 || matchPatterns(pp, inclusionPatterns))) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private boolean matchPatterns(Path path, IPath[] patterns) {
    if (patterns != null) {
      for (IPath p : patterns) {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + p.toOSString());
        if (matcher.matches(path)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isSourceInPaths(List<String> sources)
      throws JavaModelException, BackingStoreException {
    for (String s : sources) {
      if (isSourcePath(s)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public IClasspathEntry[] getClasspathEntries() {
    try {
      ImmutableList<String> targets = Activator.getTargets(project.getProject());
      ImmutableMap<String, IdeBuildInfo> infos = instance.getIdeInfo(targets);
      Set<Jars> jars = new HashSet<>();
      for (IdeBuildInfo s : infos.values()) {
        jars.addAll(s.getGeneratedJars());
        if (!isSourceInPaths(s.getSources())) {
          jars.addAll(s.getJars());
        }
      }
      return jarsToClasspathEntries(jars);
    } catch (JavaModelException | BackingStoreException | IOException | InterruptedException e) {
      Activator.error("Unable to compute classpath containers entries.", e);
      return new IClasspathEntry[] {};
    }
  }

  private IClasspathEntry[] jarsToClasspathEntries(Set<Jars> jars) {
    IClasspathEntry[] entries = new IClasspathEntry[jars.size()];
    int i = 0;
    File execRoot = instance.getExecRoot();
    for (Jars j : jars) {
      entries[i] = JavaCore.newLibraryEntry(getJarIPath(execRoot, j.getJar()),
          getJarIPath(execRoot, j.getSrcJar()), null);
      i++;
    }
    return entries;
  }

  private static IPath getJarIPath(File execRoot, String file) {
    if (file == null) {
      return null;
    }
    File path = new File(execRoot, file);
    return org.eclipse.core.runtime.Path.fromOSString(path.toString());
  }

  @Override
  public String getDescription() {
    return "Bazel Classpath Container";
  }

  @Override
  public int getKind() {
    return K_APPLICATION;
  }

  @Override
  public IPath getPath() {
    return path;
  }


  public boolean isValid() {
    return instance != null;
  }
}
