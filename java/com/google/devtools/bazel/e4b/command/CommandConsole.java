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

import java.io.IOException;
import java.io.OutputStream;

/**
 * An interface to describe an output console to stream output from a command. A command console
 * should be able to provide output stream for both normal ouput and error output.
 */
public interface CommandConsole {

  /** Create an {@link OuputStream} suitable to print standard output of a command. */
  OutputStream createOutputStream();

  /** Create an {@link OuputStream} suitable to print standard error output of a command. */
  OutputStream createErrorStream();

  /** A factory that returns a command console by name */
  interface CommandConsoleFactory {
    /**
     * Returns a {@link CommandConsole} that has the name {@code name}. {@code title} will be
     * written at the beginning of the console.
     */
    CommandConsole get(String name, String title) throws IOException;
  }
}
