package com.virtuslab.gitmachete.frontend.file;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.ui.TextFieldWithAutoCompletionListProvider;
import git4idea.repo.GitRepositoryManager;
import io.vavr.collection.List;

public class MacheteCompletionContributor extends CompletionContributor {

  @Override
  public void fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result) {

    PsiFile file = parameters.getOriginalFile();
    Project project = file.getProject();

    var gitRepository = List.ofAll(GitRepositoryManager.getInstance(project).getRepositories())
        .find(r -> file.getVirtualFile().getPath().startsWith(r.getRoot().getPath()));

    if (gitRepository.isEmpty()) {
      return;
    }

    var lists = List.ofAll(gitRepository.get().getInfo().getLocalBranchesWithHashes().keySet())
        .map(localBranch -> localBranch.getName());

    result.stopHere();
    int count = parameters.getInvocationCount();

    String prefix = TextFieldWithAutoCompletionListProvider.getCompletionPrefix(parameters);
    if (count == 0 && prefix.length() < 1) {
      return;
    }

    CompletionResultSet resultSet = result.caseInsensitive().withPrefixMatcher(
        count == 0 ? new PlainPrefixMatcher(prefix, true) : new CamelHumpMatcher(prefix));
    for (String list : lists) {
      ProgressManager.checkCanceled();
      resultSet.addElement(LookupElementBuilder.create(list));
    }
  }
}
