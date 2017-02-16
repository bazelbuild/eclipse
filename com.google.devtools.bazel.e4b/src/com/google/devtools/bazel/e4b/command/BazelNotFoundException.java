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
