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
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.bazel.e4b.command.CommandConsole.CommandConsoleFactory;

/**
 * Main utility to call bazel commands, wrapping its input and output to the message console.
 */
public class BazelCommand {

  private static Joiner NEW_LINE_JOINER = Joiner.on("\n");
  private static Pattern VERSION_PATTERN =
      Pattern.compile("^([0-9]+)\\.([0-9]+)\\.([0-9]+)([^0-9].*)?$");

  // Minimum bazel version needed to work with this plugin (currently 0.5.0)
  private static int[] MINIMUM_BAZEL_VERSION = {0, 5, 0};

  private static enum ConsoleType {
    NO_CONSOLE, SYSTEM, WORKSPACE
  }

  private final BazelAspectLocation aspectLocation;
  private final CommandConsoleFactory consoleFactory;

  private final List<String> buildOptions;
  private final List<String> aspectOptions;

  private final Map<File, BazelInstance> instances = new HashMap<>();
  private File bazel = null;

  /**
   * Create a {@link BazelCommand} object, providing the implementation for locating aspect and
   * getting console streams.
   */
  public BazelCommand(BazelAspectLocation aspectLocation, CommandConsoleFactory consoleFactory) {
    this.aspectLocation = aspectLocation;
    this.consoleFactory = consoleFactory;
    this.buildOptions = ImmutableList.of("--watchfs",
        "--override_repository=local_eclipse_aspect=" + aspectLocation.getWorkspaceDirectory(),
        "--aspects=@local_eclipse_aspect" + aspectLocation.getAspectLabel());
    this.aspectOptions = ImmutableList.<String>builder().addAll(buildOptions).add("-k",
        "--output_groups=ide-info-text,ide-resolve,-_,-defaults", "--experimental_show_artifacts")
        .build();
  }

  private String getBazelPath() throws BazelNotFoundException {
    if (bazel == null || !bazel.exists() || !bazel.canExecute()) {
      throw new BazelNotFoundException.BazelNotSetException();
    }

    return bazel.toString();
  }

  /**
   * Set the path to the Bazel binary.
   */
  public synchronized void setBazelPath(String bazel) {
    this.bazel = new File(bazel);
  }

  /**
   * Check the version of Bazel: throws an exception if the version is incorrect or the path does
   * not point to a Bazel binary.
   */
  public void checkVersion(String bazel) throws BazelNotFoundException {
    File path = new File(bazel);
    if (!path.exists() || !path.canExecute()) {
      throw new BazelNotFoundException.BazelNotExecutableException();
    }
    try {
      Command command = Command.builder(consoleFactory).setConsoleName(null)
          .setDirectory(aspectLocation.getWorkspaceDirectory()).addArguments(bazel, "version")
          .setStdoutLineSelector((s) -> s.startsWith("Build label:") ? s.substring(13) : null)
          .build();
      if (command.run() != 0) {
        throw new BazelNotFoundException.BazelNotExecutableException();
      }
      List<String> result = command.getSelectedOutputLines();
      if (result.size() != 1) {
        throw new BazelNotFoundException.BazelTooOldException("unknown");
      }
      String version = result.get(0);
      Matcher versionMatcher = VERSION_PATTERN.matcher(version);
      if (versionMatcher == null || !versionMatcher.matches()) {
        throw new BazelNotFoundException.BazelTooOldException(version);
      }
      int[] versionNumbers = {Integer.parseInt(versionMatcher.group(1)),
          Integer.parseInt(versionMatcher.group(2)), Integer.parseInt(versionMatcher.group(3))};
      if (compareVersion(versionNumbers, MINIMUM_BAZEL_VERSION) < 0) {
        throw new BazelNotFoundException.BazelTooOldException(version);
      }
    } catch (IOException | InterruptedException e) {
      throw new BazelNotFoundException.BazelNotExecutableException();
    }
  }

  private static int compareVersion(int[] version1, int[] version2) {
    for (int i = 0; i < Math.min(version1.length, version2.length); i++) {
      if (version1[i] < version2[i]) {
        return -1;
      } else if (version1[i] > version2[i]) {
        return 1;
      }
    }
    return Integer.compare(version1.length, version2.length);
  }

  /**
   * Returns a {@link BazelInstance} for the given directory. It looks for the enclosing workspace
   * and returns the instance that correspond to it. If not in a workspace, returns null.
   *
   * @throws BazelNotFoundException
   */
  public BazelInstance getInstance(File directory)
      throws IOException, InterruptedException, BazelNotFoundException {
    File workspaceRoot = getWorkspaceRoot(directory);
    if (workspaceRoot == null) {
      return null;
    }
    if (!instances.containsKey(workspaceRoot)) {
      instances.put(workspaceRoot, new BazelInstance(workspaceRoot));
    }
    return instances.get(workspaceRoot);
  }

  /**
   * An instance of the Bazel interface for a specific workspace. Provides means to query Bazel on
   * this workspace.
   */
  public class BazelInstance {
    private final File workspaceRoot;
    private final File execRoot;

    private final Map<String, ImmutableMap<String, IdeBuildInfo>> buildInfoCache = new HashMap<>();

    private BazelInstance(File workspaceRoot)
        throws IOException, InterruptedException, BazelNotFoundException {
      this.workspaceRoot = workspaceRoot;
      this.execRoot = new File(String.join("", runBazel("info", "execution_root")));
    }

    /**
     * Returns the list of targets present in the BUILD files for the given sub-directories.
     *
     * @throws BazelNotFoundException
     */
    public synchronized List<String> listTargets(File... directories)
        throws IOException, InterruptedException, BazelNotFoundException {
      StringBuilder builder = new StringBuilder();
      for (File f : directories) {
        builder.append(f.toURI().relativize(workspaceRoot.toURI()).getPath()).append("/... ");
      }
      return runBazel("query", builder.toString());
    }

    private synchronized List<String> runBazel(String... args)
        throws IOException, InterruptedException, BazelNotFoundException {
      return runBazel(ImmutableList.<String>builder().add(args).build());
    }

    private synchronized List<String> runBazel(List<String> args)
        throws IOException, InterruptedException, BazelNotFoundException {
      return BazelCommand.this.runBazelAndGetOuputLines(ConsoleType.WORKSPACE, workspaceRoot, args);
    }

    /**
     * Returns the IDE build information from running the aspect over the given list of targets. The
     * result is a list of of path to the output artifact created by the build.
     *
     * @throws BazelNotFoundException
     */
    private synchronized List<String> buildIdeInfo(Collection<String> targets)
        throws IOException, InterruptedException, BazelNotFoundException {
      return BazelCommand.this.runBazelAndGetErrorLines(ConsoleType.WORKSPACE, workspaceRoot,
          ImmutableList.<String>builder().add("build").addAll(aspectOptions).addAll(targets)
              .build(),
          // Strip out the artifact list, keeping the e4b-build.json files.
          t -> t.startsWith(">>>") ? (t.endsWith(".e4b-build.json") ? t.substring(3) : "") : null);
    }

    /**
     * Runs the analysis of the given list of targets using the IDE build information aspect and
     * returns a map of {@link IdeBuildInfo}-s (key is the label of the target) containing the
     * parsed form of the JSON file created by the aspect.
     *
     * <p>
     * This method cache it results and won't recompute a previously computed version unless
     * {@link #markAsDirty()} has been called in between.
     *
     * @throws BazelNotFoundException
     */
    public synchronized Map<String, IdeBuildInfo> getIdeInfo(Collection<String> targets)
        throws IOException, InterruptedException, BazelNotFoundException {
      String key = NEW_LINE_JOINER.join(targets);
      if (!buildInfoCache.containsKey(key)) {
        buildInfoCache.put(key, IdeBuildInfo.getInfo(buildIdeInfo(targets)));
      }
      return buildInfoCache.get(key);
    }

    /**
     * Clear the IDE build information cache. This cache is filled upon request and never emptied
     * unless we call that function.
     *
     * <p>
     * This function totally clear the cache and that might leads to useless rebuilds when several
     * eclipse project points to the same workspace but that is a rare case.
     */
    public synchronized void markAsDirty() {
      buildInfoCache.clear();
    }

    /**
     * Build a list of targets in the current workspace.
     *
     * @throws BazelNotFoundException
     */
    public synchronized int build(List<String> targets, String... extraArgs)
        throws IOException, InterruptedException, BazelNotFoundException {
      return BazelCommand.this.runBazel(workspaceRoot, ImmutableList.<String>builder().add("build")
          .addAll(buildOptions).add(extraArgs).add("--").addAll(targets).build());
    }

    /**
     * Build a list of targets in the current workspace.
     *
     * @throws BazelNotFoundException
     */
    public synchronized int build(List<String> targets, List<String> extraArgs)
        throws IOException, InterruptedException, BazelNotFoundException {
      return BazelCommand.this.runBazel(workspaceRoot, ImmutableList.<String>builder().add("build")
          .addAll(buildOptions).addAll(extraArgs).add("--").addAll(targets).build());
    }

    /**
     * Run test on a list of targets in the current workspace.
     *
     * @throws BazelNotFoundException
     */
    public synchronized int tests(List<String> targets, String... extraArgs)
        throws IOException, InterruptedException, BazelNotFoundException {
      return BazelCommand.this.runBazel(workspaceRoot, ImmutableList.<String>builder().add("test")
          .addAll(buildOptions).add(extraArgs).add("--").addAll(targets).build());
    }

    /**
     * Returns the workspace root corresponding to this object.
     */
    public File getWorkspaceRoot() {
      return workspaceRoot;
    }

    /**
     * Returns the execution root of the current workspace.
     */
    public File getExecRoot() {
      return execRoot;
    }

    /**
     * Gives a list of target completions for the given beginning string. The result is the list of
     * possible completion for a target pattern starting with string.
     *
     * @throws BazelNotFoundException
     */
    public List<String> complete(String string)
        throws IOException, InterruptedException, BazelNotFoundException {
      if (string.equals("/") || string.isEmpty()) {
        return ImmutableList.of("//");
      } else if (string.contains(":")) {
        // complete targets using `bazel query`
        int idx = string.indexOf(':');
        final String packageName = string.substring(0, idx);
        final String targetPrefix = string.substring(idx + 1);
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        builder.addAll(
            BazelCommand.this.runBazelAndGetOuputLines(ConsoleType.NO_CONSOLE, workspaceRoot,
                ImmutableList.<String>builder().add("query", packageName + ":*").build(), line -> {
                  int i = line.indexOf(':');
                  String s = line.substring(i + 1);
                  return !s.isEmpty() && s.startsWith(targetPrefix) ? (packageName + ":" + s)
                      : null;
                }));
        if ("all".startsWith(targetPrefix)) {
          builder.add(packageName + ":all");
        }
        if ("*".startsWith(targetPrefix)) {
          builder.add(packageName + ":*");
        }
        return builder.build();
      } else {
        // complete packages
        int lastSlash = string.lastIndexOf('/');
        final String prefix = lastSlash > 0 ? string.substring(0, lastSlash + 1) : "";
        final String suffix = lastSlash > 0 ? string.substring(lastSlash + 1) : string;
        final String directory = (prefix.isEmpty() || prefix.equals("//")) ? ""
            : prefix.substring(string.startsWith("//") ? 2 : 0, prefix.length() - 1);
        File file = directory.isEmpty() ? workspaceRoot : new File(workspaceRoot, directory);
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        File[] files = file.listFiles((f) -> {
          // Only give directories whose name starts with suffix...
          return f.getName().startsWith(suffix) && f.isDirectory()
          // ...that does not start with '.'...
              && !f.getName().startsWith(".")
          // ...and is not a Bazel convenience link
              && (!file.equals(workspaceRoot) || !f.getName().startsWith("bazel-"));
        });
        if (files != null) {
          for (File d : files) {
            builder.add(prefix + d.getName() + "/");
            if (new File(d, "BUILD").exists()) {
              builder.add(prefix + d.getName() + ":");
            }
          }
        }
        if ("...".startsWith(suffix)) {
          builder.add(prefix + "...");
        }
        return builder.build();
      }
    }
  }

  private File getWorkspaceRoot(File directory)
      throws IOException, InterruptedException, BazelNotFoundException {
    List<String> result = runBazelAndGetOuputLines(ConsoleType.SYSTEM, directory,
        ImmutableList.of("info", "workspace"));
    if (result.size() > 0) {
      return new File(result.get(0));
    }
    return null;
  }

  private List<String> runBazelAndGetOuputLines(ConsoleType type, File directory, List<String> args)
      throws IOException, InterruptedException, BazelNotFoundException {
    return runBazelAndGetOuputLines(type, directory, args, (t) -> t);
  }

  private synchronized List<String> runBazelAndGetOuputLines(ConsoleType type, File directory,
      List<String> args, Function<String, String> selector)
      throws IOException, InterruptedException, BazelNotFoundException {
    Command command = Command.builder(consoleFactory)
        .setConsoleName(getConsoleName(type, directory)).setDirectory(directory)
        .addArguments(getBazelPath()).addArguments(args).setStdoutLineSelector(selector).build();
    if (command.run() == 0) {
      return command.getSelectedOutputLines();
    }
    return ImmutableList.of();
  }

  private synchronized List<String> runBazelAndGetErrorLines(ConsoleType type, File directory,
      List<String> args, Function<String, String> selector)
      throws IOException, InterruptedException, BazelNotFoundException {
    Command command = Command.builder(consoleFactory)
        .setConsoleName(getConsoleName(type, directory)).setDirectory(directory)
        .addArguments(getBazelPath()).addArguments(args).setStderrLineSelector(selector).build();
    if (command.run() == 0) {
      return command.getSelectedErrorLines();
    }
    return ImmutableList.of();
  }

  private synchronized int runBazel(ConsoleType type, File directory, List<String> args,
      OutputStream stdout, OutputStream stderr)
      throws IOException, InterruptedException, BazelNotFoundException {
    return Command.builder(consoleFactory).setConsoleName(getConsoleName(type, directory))
        .setDirectory(directory).addArguments(getBazelPath()).addArguments(args)
        .setStandardOutput(stdout).setStandardError(stderr).build().run();
  }

  private int runBazel(File directory, List<String> args)
      throws IOException, InterruptedException, BazelNotFoundException {
    return runBazel(ConsoleType.WORKSPACE, directory, args, null, null);
  }

  private String getConsoleName(ConsoleType type, File directory) {
    switch (type) {
      case SYSTEM:
        return "Bazel [system]";
      case WORKSPACE:
        return "Bazel [" + directory.toString() + "]";
      case NO_CONSOLE:
      default:
        return null;
    }
  }

}
