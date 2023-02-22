package com.virtuslab.gitmachete.frontend.file;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiFile;
import com.intellij.ui.TextFieldWithAutoCompletionListProvider;
import lombok.experimental.ExtensionMethod;
import lombok.val;

import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod({MacheteFileUtils.class})
public class MacheteCompletionContributor extends CompletionContributor implements DumbAware {

  @Override
  @UIThreadUnsafe
  public void fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result) {
    if (javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      if (!stackTrace.contains("at com.virtuslab.gitmachete.frontend.actions.toolbar.DiscoverAction.actionPerformed")) {
        System.out.println("Expected non-EDT:");
        System.out.println(stackTrace);
        throw new RuntimeException("Expected EDT: " + stackTrace);
      }
    }
    PsiFile file = parameters.getOriginalFile();

    val branchNames = file.getBranchNamesForPsiMacheteFile();

    if (branchNames.isEmpty()) {
      return;
    }

    /*
     * {@link CompletionResultSet#stopHere} marks the result set as stopped. Completion service calls contributors as long as
     * everyone gets called or result set get marked as stopped. The following call allows to avoid other contributor
     * invocations (performance).
     *
     * See {@link com.intellij.codeInsight.completion.CompletionService#getVariantsFromContributors}
     */
    result.stopHere();

    String prefix = getCompletionPrefix(parameters);
    val matcher = new PlainPrefixMatcher(prefix, /* prefixMatchesOnly */ true);
    val completionResultSet = result.caseInsensitive().withPrefixMatcher(matcher);
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
    val max = Math.max(Math.max(lastSpaceIdx, lastTabIdx), lastNewLine);
    assert max <= offset : "File offset less than max indent/new line character index";
    assert offset <= text.length() : "File text length less than offset";
    return text.substring(max, offset);
  }
}
