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

package com.google.devtools.bazel.e4b.command;

public class BazelNotFoundException extends Exception {
  private static final long serialVersionUID = 1L;

  private BazelNotFoundException(String msg) {
    super(msg);
  }

  public static final class BazelNotSetException extends BazelNotFoundException {
    private static final long serialVersionUID = 1L;

    public BazelNotSetException() {
      super("Path to Bazel binary is not set, please set it "
          + "(Preferences... > Bazel Plugins Preferences)");
    }
  }

  public static final class BazelNotExecutableException extends BazelNotFoundException {
    private static final long serialVersionUID = 1L;

    public BazelNotExecutableException() {
      super("Path to Bazel is wrong (does not point to a binary), please set it "
          + "(Preferences... > Bazel Plugins Preferences)");
    }
  }

  public static final class BazelTooOldException extends BazelNotFoundException {
    private static final long serialVersionUID = 1L;

    public BazelTooOldException(String version) {
      super("Bazel version (" + version + ") is unsupported (too old or development version), "
          + "please update your Bazel binary.");
    }
  }
}
