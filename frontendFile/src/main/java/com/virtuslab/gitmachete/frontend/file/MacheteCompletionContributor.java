package com.virtuslab.gitmachete.frontend.file;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.TextFieldWithAutoCompletionListProvider;

public class MacheteCompletionContributor extends CompletionContributor {

  @Override
  public void fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result) {
    PsiFile file = parameters.getOriginalFile();

    var branchNamesOption = MacheteFileUtils.getBranchNamesForFile(file);

    if (branchNamesOption.isEmpty()) {
      return;
    }

    var branchNames = branchNamesOption.get();

    /**
     * {@link CompletionResultSet#stopHere} marks the result set as stopped.
     * Completion service calls contributors as long as everyone gets called or result set get marked as stopped.
     * The following call allows to avoid other contributor invocations (performance).
     *
     * See {@link com.intellij.codeInsight.completion.CompletionService#getVariantsFromContributors}
     */
    result.stopHere();

    String prefix = getCompletionPrefix(parameters);
    var matcher = new PlainPrefixMatcher(prefix, /* prefixMatchesOnly */ true);
    var completionResultSet = result.caseInsensitive().withPrefixMatcher(matcher);
    for (String branchName : branchNames) {
      ProgressManager.checkCanceled();
      completionResultSet.addElement(LookupElementBuilder.create(branchName));
    }
  }

  public static String getCompletionPrefix(CompletionParameters parameters) {
    String text = parameters.getOriginalFile().getText();
    int offset = parameters.getOffset();
    return getCompletionPrefix(text, offset);
  }

  /**
   * Sadly the original method {@link TextFieldWithAutoCompletionListProvider#getCompletionPrefix}
   * cannot be used as it does not take '\t' into account.
   */
  private static String getCompletionPrefix(String text, int offset) {
    int lastSpaceIdx = text.lastIndexOf(' ', offset - 1) + 1;
    int lastTabIdx = text.lastIndexOf('\t', offset - 1) + 1;
    int lastNewLine = text.lastIndexOf(System.lineSeparator(), offset - 1) + 1;
    var max = Math.max(Math.max(lastSpaceIdx, lastTabIdx), lastNewLine);
    assert max <= offset : "File offset less than max indent/new line character index";
    assert offset <= text.length() : "File text length less than offset";
    return text.substring(max, offset);
  }
}
