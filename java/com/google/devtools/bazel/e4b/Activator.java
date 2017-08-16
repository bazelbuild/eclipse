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

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.google.devtools.bazel.e4b.command.BazelCommand;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

  // The plug-in ID
  public static final String PLUGIN_ID = "com.google.devtools.bazel.e4b"; //$NON-NLS-1$

  // The shared instance
  private static Activator plugin;

  private BazelCommand command;

  /**
   * The constructor
   */
  public Activator() {}

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext context) throws Exception {
    plugin = this;
    super.start(context);
    this.command = new BazelCommand(new BazelAspectLocationImpl(), new CommandConsoleFactoryImpl());
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
  @Override
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
