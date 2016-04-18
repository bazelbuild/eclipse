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

package com.google.devtools.bazel.e4b.wizard;

import java.io.IOException;

import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

import com.google.common.collect.ImmutableList;
import com.google.devtools.bazel.e4b.Activator;
import com.google.devtools.bazel.e4b.command.BazelCommand.BazelInstance;

/**
 * A {@link IContentProposalProvider} to provide completion for Bazel. Use the
 * {@link #setBazelInstance(BazelInstance)} method to provide with the {@link BazelInstance}
 * interface to Bazel.
 */
public class BazelTargetCompletionContentProposalProvider implements IContentProposalProvider {

  private BazelInstance bazel = null;

  @Override
  public IContentProposal[] getProposals(String contents, int position) {
    if (bazel == null) {
      return null;
    }
    try {
      ImmutableList<String> completions = bazel.complete(contents.substring(0, position));
      if (completions != null) {
        IContentProposal[] result = new IContentProposal[completions.size()];
        int i = 0;
        for (String s : completions) {
          result[i] = new ContentProposal(s);
          i++;
        }
        return result;
      }
    } catch (IOException e) {
      Activator.error("Failed to run Bazel to get completion information", e);
    } catch (InterruptedException e) {
      Activator.error("Bazel was interrupted", e);
    }
    return null;
  }

  /**
   * Set the {@link BazelInstance} to use to query for completion targets.
   */
  public void setBazelInstance(BazelInstance bazel) {
    this.bazel = bazel;
  }

}
