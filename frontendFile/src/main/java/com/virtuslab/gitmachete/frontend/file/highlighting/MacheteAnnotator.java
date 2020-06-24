package com.virtuslab.gitmachete.frontend.file.highlighting;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import com.virtuslab.gitmachete.frontend.file.MacheteFileUtils;
import com.virtuslab.gitmachete.frontend.file.grammar.MacheteGeneratedBranch;
import com.virtuslab.gitmachete.frontend.file.grammar.MacheteGeneratedEntry;

public class MacheteAnnotator implements Annotator {
  @Override
  public void annotate(PsiElement element, AnnotationHolder holder) {
    if (!(element instanceof MacheteGeneratedEntry)) {
      return;
    }

    MacheteGeneratedEntry macheteEntry = (MacheteGeneratedEntry) element;
    MacheteGeneratedBranch branch = macheteEntry.getBranch();

    PsiFile file = macheteEntry.getContainingFile();
    var branchNamesOption = MacheteFileUtils.getBranchNamesForFile(file);

    if (branchNamesOption.isEmpty()) {
      // We can probably do something more useful here (some kind of message, etc.)
      return;
    }

    var branchNames = branchNamesOption.get();

    var processedBranchName = branch.getText();

    if (!branchNames.contains(processedBranchName)) {
      holder.newAnnotation(HighlightSeverity.ERROR, "Can't find '${processedBranchName}' branch in git repository")
          .range(branch).create();
    }
  }
}
