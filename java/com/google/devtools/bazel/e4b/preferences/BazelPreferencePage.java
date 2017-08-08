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

import com.google.devtools.bazel.e4b.Activator;
import com.google.devtools.bazel.e4b.command.BazelNotFoundException;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Page to configure the e4b plugin. The only configuration parameter is the path to the Bazel
 * binary so this page provide a file field to specify it.
 */
public class BazelPreferencePage extends FieldEditorPreferencePage
    implements
      IWorkbenchPreferencePage {

  private static class BazelBinaryFieldEditor extends FileFieldEditor {
    BazelBinaryFieldEditor(Composite parent) {
      super("BAZEL_PATH", "Path to the &Bazel binary:", true, parent);
      setValidateStrategy(VALIDATE_ON_KEY_STROKE);
    }

    @Override
    protected boolean doCheckState() {
      try {
        Activator.getDefault().getCommand().checkVersion(getTextControl().getText());
        return true;
      } catch (BazelNotFoundException e) {
        setErrorMessage(e.getMessage());
        return false;
      }
    }
  }

  public BazelPreferencePage() {
    super(GRID);
  }

  public void createFieldEditors() {
    addField(new BazelBinaryFieldEditor(getFieldEditorParent()));
  }

  @Override
  public void init(IWorkbench workbench) {
    setPreferenceStore(Activator.getDefault().getPreferenceStore());
    setDescription("Bazel plugin settings");
  }
}
