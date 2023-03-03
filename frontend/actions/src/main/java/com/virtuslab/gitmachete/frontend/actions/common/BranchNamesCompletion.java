package com.virtuslab.gitmachete.frontend.actions.common;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.codeInsight.lookup.CharFilter;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.DumbAware;
import com.intellij.util.textCompletion.DefaultTextCompletionValueDescriptor;
import com.intellij.util.textCompletion.TextCompletionProvider;
import com.intellij.util.textCompletion.TextCompletionValueDescriptor;
import io.vavr.collection.List;
import lombok.val;

// TODO (#1604): remove this class and replace with com.intellij.util.textCompletion.TextCompletionProviderBase
public final class BranchNamesCompletion implements TextCompletionProvider, DumbAware {
  private final List<String> localDirectories;
  private final List<String> allSuggestions;
  private final TextCompletionValueDescriptor<String> myDescriptor = new DefaultTextCompletionValueDescriptor.StringValueDescriptor();
  private final InsertHandler<LookupElement> myInsertHandler = new CompletionCharInsertHandler();

  public BranchNamesCompletion(List<String> localDirectories, List<String> allSuggestions) {
    this.localDirectories = localDirectories;
    this.allSuggestions = allSuggestions;
  }

  @Override
  public String getAdvertisement() {
    return "";
  }

  @Override
  public String getPrefix(String text, int offset) {
    @SuppressWarnings("index") String substring = text.substring(0, offset);
    return substring;
  }

  @Override
  public CompletionResultSet applyPrefixMatcher(CompletionResultSet result, String prefix) {
    CompletionResultSet resultWithMatcher = result.withPrefixMatcher(new PlainPrefixMatcher(prefix));
    resultWithMatcher = resultWithMatcher.caseInsensitive();
    return resultWithMatcher;
  }

  @Override
  public CharFilter.Result acceptChar(char c) {
    return CharFilter.Result.ADD_TO_PREFIX;
  }

  @Override
  public void fillCompletionVariants(CompletionParameters parameters, String prefix, CompletionResultSet result) {
    val values = getValues(parameters).sorted(myDescriptor);

    for (String completionVariant : values) {
      result.addElement(installInsertHandler(myDescriptor.createLookupBuilder(completionVariant)));
    }
    result.stopHere();
  }

  private LookupElement installInsertHandler(LookupElementBuilder builder) {
    InsertHandler<LookupElement> handler = builder.getInsertHandler();
    if (handler == null) {
      return builder.withInsertHandler(myInsertHandler);
    }

    return builder.withInsertHandler(new InsertHandler<>() {
      @Override
      public void handleInsert(InsertionContext context, LookupElement item) {
        myInsertHandler.handleInsert(context, item);
        handler.handleInsert(context, item);
      }
    });
  }

  private List<String> getValues(CompletionParameters parameters) {
    if (parameters.isAutoPopup()) {
      return localDirectories;
    } else {
      return allSuggestions;
    }
  }

  public static class CompletionCharInsertHandler implements InsertHandler<LookupElement> {
    @Override
    public void handleInsert(InsertionContext context, LookupElement item) {
      context.setAddCompletionChar(false);
    }
  }
}
