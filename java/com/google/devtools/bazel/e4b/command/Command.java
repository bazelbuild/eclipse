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

package com.google.devtools.bazel.e4b.command;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.devtools.bazel.e4b.command.CommandConsole.CommandConsoleFactory;

/**
 * A utility class to spawn a command and parse its output. It allow to filter the output,
 * redirecting part of it to the console and getting the rest in a list of string.
 *
 * <p>
 * This class can only be initialized using a builder created with the {@link #builder()} method.
 */
final public class Command {

  private final File directory;
  private final ImmutableList<String> args;
  private final SelectOutputStream stdout;
  private final SelectOutputStream stderr;
  private boolean executed = false;

  private Command(CommandConsole console, File directory, ImmutableList<String> args,
      Function<String, String> stdoutSelector, Function<String, String> stderrSelector,
      OutputStream stdout, OutputStream stderr) throws IOException {
    this.directory = directory;
    this.args = args;
    if (console != null) {
      if (stdout == null) {
        stdout = console.createOutputStream();
      }
      if (stderr == null) {
        stderr = console.createErrorStream();
      }
    }
    this.stderr = new SelectOutputStream(stderr, stderrSelector);
    this.stdout = new SelectOutputStream(stdout, stdoutSelector);
  }

  /**
   * Executes the command represented by this instance, and return the exit code of the command.
   * This method should not be called twice on the same object.
   */
  public int run() throws IOException, InterruptedException {
    Preconditions.checkState(!executed);
    executed = true;
    ProcessBuilder builder = new ProcessBuilder(args);
    builder.directory(directory);
    builder.redirectOutput(ProcessBuilder.Redirect.PIPE);
    builder.redirectError(ProcessBuilder.Redirect.PIPE);
    Process process = builder.start();
    Thread err = copyStream(process.getErrorStream(), stderr);
    // seriously? That's stdout, why is it called getInputStream???
    Thread out = copyStream(process.getInputStream(), stdout);
    int r = process.waitFor();
    if (err != null) {
      err.join();
    }
    if (out != null) {
      out.join();
    }
    synchronized (stderr) {
      stderr.close();
    }
    synchronized (stdout) {
      stdout.close();
    }
    return r;
  }

  private static class CopyStreamRunnable implements Runnable {
    private InputStream inputStream;
    private OutputStream outputStream;

    CopyStreamRunnable(InputStream inputStream, OutputStream outputStream) {
      this.inputStream = inputStream;
      this.outputStream = outputStream;
    }

    @Override
    public void run() {
      byte[] buffer = new byte[4096];
      int read;
      try {
        while ((read = inputStream.read(buffer)) > 0) {
          synchronized (outputStream) {
            outputStream.write(buffer, 0, read);
          }
        }
      } catch (IOException ex) {
        // we simply terminate the thread on exceptions
      }
    }
  }

  // Launch a thread to copy all data from inputStream to outputStream
  private static Thread copyStream(InputStream inputStream, OutputStream outputStream) {
    if (outputStream != null) {
      Thread t = new Thread(new CopyStreamRunnable(inputStream, outputStream), "CopyStream");
      t.start();
      return t;
    }
    return null;
  }

  /**
   * Returns the list of lines selected from the standard error stream. Lines printed to the
   * standard error stream by the executed command can be filtered to be added to that list.
   *
   * @see {@link Builder#setStderrLineSelector(Function)}
   */
  public ImmutableList<String> getSelectedErrorLines() {
    return stderr.getLines();
  }

  /**
   * Returns the list of lines selected from the standard output stream. Lines printed to the
   * standard output stream by the executed command can be filtered to be added to that list.
   *
   * @see {@link Builder#setStdoutLineSelector(Function)}
   */
  public ImmutableList<String> getSelectedOutputLines() {
    return stdout.getLines();
  }

  /**
   * A builder class to generate a Command object.
   */
  public static class Builder {

    private String consoleName = null;
    private File directory;
    private ImmutableList.Builder<String> args = ImmutableList.builder();
    private OutputStream stdout = null;
    private OutputStream stderr = null;
    private Function<String, String> stdoutSelector;
    private Function<String, String> stderrSelector;
    private final CommandConsoleFactory consoleFactory;

    private Builder(final CommandConsoleFactory consoleFactory) {
      // Default to the current working directory
      this.directory = new File(System.getProperty("user.dir"));
      this.consoleFactory = consoleFactory;
    }

    /**
     * Set the console name.
     *
     * <p>
     * The console name is used to print result of the program. Only lines not filtered by
     * {@link #setStderrLineSelector(Function)} and {@link #setStdoutLineSelector(Function)} are
     * printed to the console. If {@link #setStandardError(OutputStream)} or
     * {@link #setStandardOutput(OutputStream)} have been used with a non null value, then they
     * intercept all output from being printed to the console.
     *
     * <p>
     * If name is null, no output is written to any console.
     */
    public Builder setConsoleName(String name) {
      this.consoleName = name;
      return this;
    }

    /**
     * Set the working directory for the program, it is set to the current working directory of the
     * current java process by default.
     */
    public Builder setDirectory(File directory) {
      this.directory = directory;
      return this;
    }

    /**
     * Set an {@link OutputStream} to receive non selected lines from the standard output stream of
     * the program in lieu of the console. If a selector has been set with
     * {@link #setStdoutLineSelector(Function)}, only the lines not selected (for which the selector
     * returns null) will be printed to the {@link OutputStream}.
     */
    public Builder setStandardOutput(OutputStream stdout) {
      this.stdout = stdout;
      return this;
    }

    /**
     * Set an {@link OutputStream} to receive non selected lines from the standard error stream of
     * the program in lieu of the console. If a selector has been set with
     * {@link #setStderrLineSelector(Function)}, only the lines not selected (for which the selector
     * returns null) will be printed to the {@link OutputStream}.
     */
    public Builder setStandardError(OutputStream stderr) {
      this.stderr = stderr;
      return this;
    }

    /**
     * Add arguments to the command line. The first argument to be added to the builder is the
     * program name.
     */
    public Builder addArguments(String... args) {
      this.args.add(args);
      return this;
    }

    /**
     * Add a list of arguments to the command line. The first argument to be added to the builder is
     * the program name.
     */
    public Builder addArguments(Iterable<String> args) {
      this.args.addAll(args);
      return this;
    }

    /**
     * Set a selector to accumulate lines that are selected from the standard output stream.
     *
     * <p>
     * The selector is passed all lines that are printed to the standard output. It can either
     * returns null to say that the line should be passed to the console or to a non null value that
     * will be stored. All values that have been selected (for which the selector returns a non-null
     * value) will be stored in a list accessible through {@link Command#getSelectedOutputLines()}.
     * The selected lines will not be printed to the console.
     */
    public Builder setStdoutLineSelector(Function<String, String> selector) {
      this.stdoutSelector = selector;
      return this;
    }

    /**
     * Set a selector to accumulate lines that are selected from the standard error stream.
     *
     * <p>
     * The selector is passed all lines that are printed to the standard error. It can either
     * returns null to say that the line should be passed to the console or to a non null value that
     * will be stored. All values that have been selected (for which the selector returns a non-null
     * value) will be stored in a list accessible through {@link Command#getSelectedErrorLines()}.
     * The selected lines will not be printed to the console.
     */
    public Builder setStderrLineSelector(Function<String, String> selector) {
      this.stderrSelector = selector;
      return this;
    }

    /**
     * Build a Command object.
     */
    public Command build() throws IOException {
      Preconditions.checkNotNull(directory);
      ImmutableList<String> args = this.args.build();
      CommandConsole console = consoleName == null ? null
          : consoleFactory.get(consoleName,
              "Running " + String.join(" ", args) + " from " + directory.toString());
      return new Command(console, directory, args, stdoutSelector, stderrSelector,
          stdout, stderr);
    }
  }

  /**
   * Returns a {@link Builder} object to use to create a {@link Command} object.
   */
  public static Builder builder(CommandConsoleFactory consoleFactory) {
    return new Builder(consoleFactory);
  }
}
