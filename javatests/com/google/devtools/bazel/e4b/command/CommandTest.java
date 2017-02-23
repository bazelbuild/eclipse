package com.google.devtools.bazel.e4b.command;

import static com.google.common.truth.Truth.assertThat;

import com.google.devtools.bazel.e4b.command.CommandConsole.CommandConsoleFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** @{link Command}Test */
public class CommandTest {

  private static Function<String, String> NON_EMPTY_LINES_SELECTOR =
      (x) -> x.trim().isEmpty() ? null : x;

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private class MockCommandConsole implements CommandConsole {
    final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    final String name;
    final String title;

    public MockCommandConsole(String name, String title) {
      this.title = title;
      this.name = name;
    }

    @Override
    public OutputStream createOutputStream() {
      return stdout;
    }

    @Override
    public OutputStream createErrorStream() {
      return stderr;
    }
  }

  private class MockConsoleFactory implements CommandConsoleFactory {
    final List<MockCommandConsole> consoles = new LinkedList<>();

    @Override
    public CommandConsole get(String name, String title) throws IOException {
      MockCommandConsole console = new MockCommandConsole(name, title);
      consoles.add(console);
      return console;
    }
  }

  public MockConsoleFactory mockConsoleFactory;

  @Before
  public void setup() {
    mockConsoleFactory = new MockConsoleFactory();
  }

  @Test
  public void testCommandWithStream() throws IOException, InterruptedException {
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    Function<String, String> stdoutSelector =
        (x) -> (x.trim().isEmpty() || x.equals("a")) ? null : x;
    Function<String, String> stderrSelector =
        (x) -> (x.trim().isEmpty() || x.equals("b")) ? null : x;

    Command.Builder builder = Command.builder(mockConsoleFactory)
        .setConsoleName("test")
        .setDirectory(folder.getRoot())
        .setStandardError(stderr)
        .setStandardOutput(stdout)
        .setStderrLineSelector(stderrSelector)
        .setStdoutLineSelector(stdoutSelector);
    builder.addArguments("bash", "-c", "echo a; echo b; echo a >&2; echo b >&2");
    Command cmd = builder.build();
    assertThat(cmd.run()).isEqualTo(0);
    String stdoutStr = new String(stdout.toByteArray(), StandardCharsets.UTF_8).trim();
    String stderrStr = new String(stderr.toByteArray(), StandardCharsets.UTF_8).trim();

    assertThat(stdoutStr).isEqualTo("a");
    assertThat(stderrStr).isEqualTo("b");
    assertThat(cmd.getSelectedErrorLines()).containsExactly("a");
    assertThat(cmd.getSelectedOutputLines()).containsExactly("b");
    assertThat(mockConsoleFactory.consoles).hasSize(1);
    MockCommandConsole console = mockConsoleFactory.consoles.get(0);
    assertThat(console.name).isEqualTo("test");
    assertThat(console.title).isEqualTo(
        "Running bash -c echo a; echo b; echo a >&2; echo b >&2 " + "from " + folder.getRoot());
    stdoutStr = new String(console.stdout.toByteArray(), StandardCharsets.UTF_8).trim();
    stderrStr = new String(console.stderr.toByteArray(), StandardCharsets.UTF_8).trim();
    assertThat(stdoutStr).isEmpty();
    assertThat(stderrStr).isEmpty();
  }

  @Test
  public void testCommandNoStream() throws IOException, InterruptedException {
    Command.Builder builder =
        Command.builder(mockConsoleFactory).setConsoleName(null).setDirectory(folder.getRoot());
    builder.addArguments("bash", "-c", "echo a; echo b; echo a >&2; echo b >&2");
    builder.setStderrLineSelector(NON_EMPTY_LINES_SELECTOR)
        .setStdoutLineSelector(NON_EMPTY_LINES_SELECTOR);
    Command cmd = builder.build();
    assertThat(cmd.run()).isEqualTo(0);
    assertThat(cmd.getSelectedErrorLines()).containsExactly("a", "b");
    assertThat(cmd.getSelectedOutputLines()).containsExactly("a", "b");
  }

  @Test
  public void testCommandStreamAllToConsole() throws IOException, InterruptedException {
    Command.Builder builder =
        Command.builder(mockConsoleFactory).setConsoleName("test").setDirectory(folder.getRoot());
    builder.addArguments("bash", "-c", "echo a; echo b; echo a >&2; echo b >&2");
    Command cmd = builder.build();
    assertThat(cmd.run()).isEqualTo(0);
    MockCommandConsole console = mockConsoleFactory.consoles.get(0);
    assertThat(console.name).isEqualTo("test");
    assertThat(console.title).isEqualTo(
        "Running bash -c echo a; echo b; echo a >&2; echo b >&2 " + "from " + folder.getRoot());
    String stdoutStr = new String(console.stdout.toByteArray(), StandardCharsets.UTF_8).trim();
    String stderrStr = new String(console.stderr.toByteArray(), StandardCharsets.UTF_8).trim();
    assertThat(stdoutStr).isEqualTo("a\nb");
    assertThat(stderrStr).isEqualTo("a\nb");
  }

  @Test
  public void testCommandWorkDir() throws IOException, InterruptedException {
    Command.Builder builder =
        Command.builder(mockConsoleFactory).setConsoleName(null).setDirectory(folder.getRoot());
    builder.setStderrLineSelector(NON_EMPTY_LINES_SELECTOR)
        .setStdoutLineSelector(NON_EMPTY_LINES_SELECTOR);
    builder.addArguments("pwd");
    Command cmd = builder.build();
    assertThat(cmd.run()).isEqualTo(0);
    assertThat(cmd.getSelectedErrorLines()).isEmpty();
    assertThat(cmd.getSelectedOutputLines()).containsExactly(folder.getRoot().getCanonicalPath());
  }
}
