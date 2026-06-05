package com.virtuslab.gitmachete.frontend.actions.common;

import java.util.Collection;
import java.util.Collections;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.project.DumbAware;
import com.intellij.util.textCompletion.DefaultTextCompletionValueDescriptor;
import com.intellij.util.textCompletion.TextCompletionProviderBase;

public final class BranchNamesCompletion extends TextCompletionProviderBase<String> implements DumbAware {
  private final Collection<String> localDirectories;
  private final Collection<String> allSuggestions;

  public BranchNamesCompletion(Collection<String> localDirectories, Collection<String> allSuggestions) {
    // Empty separators: the whole field content is a single value (a branch name), so completion
    // matches against the entire text rather than tokenizing it.
    super(new DefaultTextCompletionValueDescriptor.StringValueDescriptor(), Collections.emptyList(), /* caseSensitive */ false);
    this.localDirectories = localDirectories;
    this.allSuggestions = allSuggestions;
  }

  @Override
  protected Collection<String> getValues(CompletionParameters parameters, String prefix, CompletionResultSet result) {
    return parameters.isAutoPopup() ? localDirectories : allSuggestions;
  }
}
