package com.virtuslab.gitmachete.frontend.file;

import java.util.Arrays;
import java.util.List;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.TextFieldWithAutoCompletionListProvider;
import one.util.streamex.StreamEx;

public class MacheteCompletionContributor extends CompletionContributor {

  @Override
  public void fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result) {

    PsiFile file = parameters.getOriginalFile();
    Project project = file.getProject();
    Document document = PsiDocumentManager.getInstance(project).getDocument(file);
    if (document == null) {
      return;
    }

    List<String> lists = Arrays.asList("haha", "DDD-: sineb");

    result.stopHere();
    int count = parameters.getInvocationCount();

    String prefix = TextFieldWithAutoCompletionListProvider.getCompletionPrefix(parameters);
    if (count == 0 && prefix.length() < 2) {
      return;
    }

    CompletionResultSet resultSet = result.caseInsensitive().withPrefixMatcher(
        count == 0 ? new PlainPrefixMatcher(prefix, true) : new CamelHumpMatcher(prefix));
    for (String list : lists) {
      ProgressManager.checkCanceled();
      resultSet.addElement(LookupElementBuilder.create(list));

      if (count > 0) {
        result.caseInsensitive()
            .withPrefixMatcher(new PlainPrefixMatcher(prefix))
            .addAllElements(
                StreamEx.of(VcsConfiguration.getInstance(project).getRecentMessages())
                    .reverseSorted()
                    .map(lookupString -> PrioritizedLookupElement.withPriority(LookupElementBuilder.create(lookupString),
                        Integer.MIN_VALUE)));
      }
    }
  }
}
