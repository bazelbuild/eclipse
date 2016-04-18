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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.google.common.collect.ImmutableList;
import com.google.devtools.bazel.e4b.command.BazelCommand;
import com.google.devtools.bazel.e4b.command.BazelCommand.BazelInstance;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

  // The plug-in ID
  public static final String PLUGIN_ID = "com.google.devtools.bazel.e4b"; //$NON-NLS-1$

  // The shared instance
  private static Activator plugin;

  private BazelCommand command;

  public static final String DEFAULT_BAZEL_PATH = "/usr/local/bin/bazel";

  /**
   * The constructor
   */
  public Activator() {}

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext context) throws Exception {
    plugin = this;
    super.start(context);
    this.command = new BazelCommand();
    // Get the bazel path from the settings
    this.command.setBazelPath(getPreferenceStore().getString("BAZEL_PATH"));
    getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (event.getProperty().equals("BAZEL_PATH")) {
          command.setBazelPath(event.getNewValue().toString());
        }
      }
    });
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    this.command = null;
    super.stop(context);
  }

  /**
   * Returns the shared instance
   *
   * @return the shared instance
   */
  public static Activator getDefault() {
    return plugin;
  }

  /**
   * Returns the unique instance of {@link BazelCommand}.
   */
  public BazelCommand getCommand() {
    return command;
  }


  /**
   * List targets configure for <code>project</code>. Each project configured for Bazel is
   * configured to track certain targets and this function fetch this list from the project
   * preferences.
   */
  public static ImmutableList<String> getTargets(IProject project) throws BackingStoreException {
    // Get the list of targets from the preferences
    IScopeContext projectScope = new ProjectScope(project);
    Preferences projectNode = projectScope.getNode(PLUGIN_ID);
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (String s : projectNode.keys()) {
      if (s.startsWith("target")) {
        builder.add(projectNode.get(s, ""));
      }
    }
    return builder.build();
  }

  /**
   * Return the {@link BazelInstance} corresponding to the given <code>project</code>. It looks for
   * the instance that runs for the workspace root configured for that project.
   */
  public static BazelCommand.BazelInstance getBazelCommandInstance(IProject project)
      throws BackingStoreException, IOException, InterruptedException {
    IScopeContext projectScope = new ProjectScope(project.getProject());
    Preferences projectNode = projectScope.getNode(Activator.PLUGIN_ID);
    File workspaceRoot =
        new File(projectNode.get("workspaceRoot", project.getLocation().toFile().toString()));
    return getDefault().getCommand().getInstance(workspaceRoot);
  }

  /**
   * Log an error to eclipse.
   */
  public static void error(String message) {
    plugin.getLog().log(new Status(Status.ERROR, PLUGIN_ID, message));
  }

  /**
   * Log an error to eclipse, with an attached exception.
   */
  public static void error(String message, Throwable exception) {
    plugin.getLog().log(new Status(Status.ERROR, PLUGIN_ID, message, exception));
  }
}
