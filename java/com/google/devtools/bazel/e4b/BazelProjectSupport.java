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

package com.google.devtools.bazel.e4b;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.google.common.collect.ImmutableList;
import com.google.devtools.bazel.e4b.classpath.BazelClasspathContainer;
import com.google.devtools.bazel.e4b.command.BazelCommand;
import com.google.devtools.bazel.e4b.command.BazelCommand.BazelInstance;
import com.google.devtools.bazel.e4b.command.BazelNotFoundException;
import com.google.devtools.bazel.e4b.projectviews.ProjectView;

/**
 * A utility class to create e4b projects.
 */
public class BazelProjectSupport {

  private static final String STANDARD_VM_CONTAINER_PREFIX =
      "org.eclipse.jdt.launching.JRE_CONTAINER/"
          + "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.";

  /**
   * Create a e4b project. This method adds the natures to the project, saves the list of targets
   * and the workspace root to the project settings, make Bazel the default builder instead of ECJ
   * and create the classpath using ide build informations from Bazel.
   */
  public static IProject createProject(String projectName, URI location, String workspaceRoot,
      List<String> paths, List<String> targets, int javaLanguageVersion) {

    IProject project = createBaseProject(projectName, location);
    try {
      addNature(project, ProjectNature.NATURE_ID);
      addNature(project, JavaCore.NATURE_ID);
      addSettings(project, workspaceRoot, targets, ImmutableList.of());
      setBuilders(project);
      createClasspath(new Path(workspaceRoot), paths, JavaCore.create(project),
          javaLanguageVersion);
    } catch (CoreException e) {
      e.printStackTrace();
      project = null;
    } catch (BackingStoreException e) {
      e.printStackTrace();
      project = null;
    }

    return project;
  }

  private static void addSettings(IProject project, String workspaceRoot, List<String> targets,
      List<String> buildFlags) throws BackingStoreException {
    IScopeContext projectScope = new ProjectScope(project);
    Preferences projectNode = projectScope.getNode(Activator.PLUGIN_ID);
    int i = 0;
    for (String target : targets) {
      projectNode.put("target" + i, target);
      i++;
    }
    projectNode.put("workspaceRoot", workspaceRoot);
    i = 0;
    for (String flag : buildFlags) {
      projectNode.put("buildFlag" + i, flag);
      i++;
    }
    projectNode.flush();
  }

  private static void setBuilders(IProject project) throws CoreException {
    IProjectDescription description = project.getDescription();
    final ICommand buildCommand = description.newCommand();
    buildCommand.setBuilderName("com.google.devtools.bazel.e4b.builder");
    description.setBuildSpec(new ICommand[] {buildCommand});
    project.setDescription(description, null);
  }

  private static void createClasspath(IPath root, List<String> paths, IJavaProject javaProject,
      int javaLanguageLevel) throws CoreException {
    String name = root.lastSegment();
    IFolder base = javaProject.getProject().getFolder(name);
    if (!base.isLinked()) {
      base.createLink(root, IResource.NONE, null);
    }
    List<IClasspathEntry> list = new LinkedList<>();
    for (String path : paths) {
      IPath workspacePath = base.getFullPath().append(path);
      list.add(JavaCore.newSourceEntry(workspacePath));
    }
    list.add(JavaCore.newContainerEntry(new Path(BazelClasspathContainer.CONTAINER_NAME)));

    list.add(
        JavaCore.newContainerEntry(new Path(STANDARD_VM_CONTAINER_PREFIX + javaLanguageLevel)));
    IClasspathEntry[] newClasspath = list.toArray(new IClasspathEntry[0]);
    javaProject.setRawClasspath(newClasspath, null);
  }

  private static IProject createBaseProject(String projectName, URI location) {
    IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

    if (!newProject.exists()) {
      URI projectLocation = location;
      IProjectDescription desc =
          newProject.getWorkspace().newProjectDescription(newProject.getName());
      if (location != null
          && ResourcesPlugin.getWorkspace().getRoot().getLocationURI().equals(location)) {
        projectLocation = null;
      }

      desc.setLocationURI(projectLocation);
      try {
        newProject.create(desc, null);
        if (!newProject.isOpen()) {
          newProject.open(null);
        }
      } catch (CoreException e) {
        e.printStackTrace();
      }
    }

    return newProject;
  }

  private static void addNature(IProject project, String nature) throws CoreException {
    if (!project.hasNature(nature)) {
      IProjectDescription description = project.getDescription();
      String[] prevNatures = description.getNatureIds();
      String[] newNatures = new String[prevNatures.length + 1];
      System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
      newNatures[prevNatures.length] = nature;
      description.setNatureIds(newNatures);

      project.setDescription(description, null);
    }
  }

  /**
   * List targets configure for <code>project</code>. Each project configured for Bazel is
   * configured to track certain targets and this function fetch this list from the project
   * preferences.
   */
  public static List<String> getTargets(IProject project) throws BackingStoreException {
    // Get the list of targets from the preferences
    IScopeContext projectScope = new ProjectScope(project);
    Preferences projectNode = projectScope.getNode(Activator.PLUGIN_ID);
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (String s : projectNode.keys()) {
      if (s.startsWith("target")) {
        builder.add(projectNode.get(s, ""));
      }
    }
    return builder.build();
  }

  /**
   * List of build flags for <code>project</code>, taken from the project configuration
   */
  public static List<String> getBuildFlags(IProject project) throws BackingStoreException {
    // Get the list of targets from the preferences
    IScopeContext projectScope = new ProjectScope(project);
    Preferences projectNode = projectScope.getNode(Activator.PLUGIN_ID);
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (String s : projectNode.keys()) {
      if (s.startsWith("buildArgs")) {
        builder.add(projectNode.get(s, ""));
      }
    }
    return builder.build();
  }

  /**
   * Return the {@link BazelInstance} corresponding to the given <code>project</code>. It looks for
   * the instance that runs for the workspace root configured for that project.
   *
   * @throws BazelNotFoundException
   */
  public static BazelCommand.BazelInstance getBazelCommandInstance(IProject project)
      throws BackingStoreException, IOException, InterruptedException, BazelNotFoundException {
    IScopeContext projectScope = new ProjectScope(project.getProject());
    Preferences projectNode = projectScope.getNode(Activator.PLUGIN_ID);
    File workspaceRoot =
        new File(projectNode.get("workspaceRoot", project.getLocation().toFile().toString()));
    return Activator.getDefault().getCommand().getInstance(workspaceRoot);
  }

  /**
   * Convert an Eclipse JDT project into an IntelliJ project view
   */
  public static ProjectView getProjectView(IProject project)
      throws BackingStoreException, JavaModelException {
    com.google.devtools.bazel.e4b.projectviews.Builder builder = ProjectView.builder();
    IScopeContext projectScope = new ProjectScope(project);
    Preferences projectNode = projectScope.getNode(Activator.PLUGIN_ID);
    for (String s : projectNode.keys()) {
      if (s.startsWith("buildArgs")) {
        builder.addBuildFlag(projectNode.get(s, ""));
      } else if (s.startsWith("target")) {
        builder.addTarget(projectNode.get(s, ""));
      }
    }

    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    for (IClasspathEntry entry : ((IJavaProject) project).getRawClasspath()) {
      switch (entry.getEntryKind()) {
        case IClasspathEntry.CPE_SOURCE:
          IResource res = root.findMember(entry.getPath());
          if (res != null) {
            builder.addDirectory(res.getProjectRelativePath().removeFirstSegments(1).toOSString());
          }
          break;
        case IClasspathEntry.CPE_CONTAINER:
          String path = entry.getPath().toOSString();
          if (path.startsWith(STANDARD_VM_CONTAINER_PREFIX)) {
            builder.setJavaLanguageLevel(
                Integer.parseInt(path.substring(STANDARD_VM_CONTAINER_PREFIX.length())));
          }
          break;
      }
    }
    return builder.build();
  }
}
