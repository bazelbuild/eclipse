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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * A wrapper output stream to output part of the result to a given output and extracting the other
 * part with a selector function. The other part is return as a list of string.
 */
public class SelectOutputStream extends OutputStream {

  private OutputStream output;
  private Function<String, String> selector;
  private boolean closed = false;
  private List<String> lines = new LinkedList<>();
  private ByteArrayOutputStream stream = new ByteArrayOutputStream();

  /**
   * Create a SelectOutputStream. <code>output<code> is the output stream where non-selected lines
   * will be printed. <code>selector<code> is a function that will be called on each line. If
   * <code>selector</code> returns a non null value, then the resulting value will be stored in a
   * lines buffer that can be consumed with the {@link #getLines()} method. If <code>selector</code>
   * returns a null value, the corresponding line will be send to <code>output</code>.
   *
   * <p>
   * Both <code>output</code> and <code>selector</code> can be null. If <code>output</code> is null,
   * unselected lines will be discarded. If <code>selector</code> is null, all lines will be
   * considered as unselected.
   */
  public SelectOutputStream(OutputStream output, Function<String, String> selector) {
    super();
    this.output = output;
    this.selector = selector;
  }

  @Override
  public void write(int b) throws IOException {
    byte b0 = (byte) b;
    if (b0 == '\n') {
      select(true);
    } else {
      stream.write(b);
    }
  }

  private void select(boolean appendNewLine) throws UnsupportedEncodingException, IOException {
    String line = null;
    if (selector != null) {
      line = selector.apply(stream.toString(StandardCharsets.UTF_8.name()));
    }

    if (line != null) {
      lines.add(line);
    } else if (output != null) {
      if (appendNewLine) {
        stream.write('\n');
      }
      output.write(stream.toByteArray());
    }
    stream.reset();
  }

  @Override
  public void close() throws IOException {
    Preconditions.checkState(!closed);
    super.close();
    select(false);
    closed = true;
  }

  /**
   * Returns the list of selected lines.
   */
  public ImmutableList<String> getLines() {
    return ImmutableList.copyOf(lines);
  }
}
