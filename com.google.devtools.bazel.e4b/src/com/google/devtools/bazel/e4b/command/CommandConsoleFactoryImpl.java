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

import com.google.devtools.bazel.e4b.command.CommandConsole.CommandConsoleFactory;
import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/** Implementation of {@link CommandConsoleFactory} using Eclipse's console */
class CommandConsoleFactoryImpl implements CommandConsoleFactory {

  private static class CommandConsoleImpl implements CommandConsole {

    private MessageConsole console;

    CommandConsoleImpl(MessageConsole console) {
      this.console = console;
    }

    @Override
    public OutputStream createOutputStream() {
      return console.newMessageStream();
    }

    @Override
    public OutputStream createErrorStream() {
      // Get the error stream for the given console (a stream that print in red).
      final MessageConsoleStream errorStream = console.newMessageStream();
      Display display = Display.getCurrent();
      if (display == null) {
        display = Display.getDefault();
      }
      display.asyncExec(() -> errorStream.setColor(new Color(null, 255, 0, 0)));
      return errorStream;
    }
  }

  @Override
  public CommandConsole get(String name, String title) throws IOException {
    MessageConsole console = findConsole(name);
    MessageConsoleStream stream = console.newMessageStream();
    stream.setActivateOnWrite(true);
    stream.write("*** " + title + " ***\n");
    return new CommandConsoleImpl(console);
  }

  // Taken from the eclipse website, find a console
  private static MessageConsole findConsole(String name) {
    ConsolePlugin plugin = ConsolePlugin.getDefault();
    IConsoleManager conMan = plugin.getConsoleManager();
    IConsole[] existing = conMan.getConsoles();
    for (int i = 0; i < existing.length; i++) {
      if (name.equals(existing[i].getName())) {
        return (MessageConsole) existing[i];
      }
    }
    // no console found, so create a new one
    MessageConsole myConsole = new MessageConsole(name, null);
    conMan.addConsoles(new IConsole[] {myConsole});
    return myConsole;
  }
}
