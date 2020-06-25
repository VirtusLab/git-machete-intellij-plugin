package com.virtuslab.gitmachete.frontend.file.highlighting;

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import io.vavr.control.Option;
import lombok.Data;

import com.virtuslab.gitmachete.frontend.file.MacheteFileUtils;
import com.virtuslab.gitmachete.frontend.file.grammar.MacheteFile;
import com.virtuslab.gitmachete.frontend.file.grammar.MacheteGeneratedBranch;
import com.virtuslab.gitmachete.frontend.file.grammar.MacheteGeneratedElementTypes;
import com.virtuslab.gitmachete.frontend.file.grammar.MacheteGeneratedEntry;

public class MacheteAnnotator implements Annotator {
  @Override
  public void annotate(PsiElement element, AnnotationHolder holder) {
    if (element instanceof MacheteGeneratedEntry) {
      processMacheteGeneratedEntry((MacheteGeneratedEntry) element, holder);
    } else if (element.getNode().getElementType().equals(MacheteGeneratedElementTypes.INDENTATION)) {
      processIndentationElement(element, holder);
    }
  }

  private void processMacheteGeneratedEntry(MacheteGeneratedEntry macheteEntry, AnnotationHolder holder) {
    MacheteGeneratedBranch branch = macheteEntry.getBranch();

    PsiFile file = macheteEntry.getContainingFile();
    var branchNamesOption = MacheteFileUtils.getBranchNamesForFile(file);

    if (branchNamesOption.isEmpty()) {
      // TODO (#372): We can probably do something more useful here (some kind of message, etc.)
      return;
    }

    var branchNames = branchNamesOption.get();

    var processedBranchName = branch.getText();

    if (!branchNames.contains(processedBranchName)) {
      holder.newAnnotation(HighlightSeverity.ERROR, "Can't find local branch '${processedBranchName}' in git repository")
          .range(branch).create();
    }
  }

  private void processIndentationElement(PsiElement element, AnnotationHolder holder) {
    PsiElement parent = element.getParent();
    assert parent != null : "Element has not parent";

    if (parent instanceof MacheteFile) {
      return;
    }

    var prevMacheteGeneratedEntryOption = getPrevSiblingMacheteGeneratedEntry(parent);
    if (prevMacheteGeneratedEntryOption.isEmpty()) {
      holder.newAnnotation(HighlightSeverity.ERROR, "First entry cannot be indented").range(element).create();
      return;
    }

    int prevLevel;
    int thisLevel;
    boolean hasPrevLevelCorrectWidth;

    IndentationParameters indentationParameters = findIndentationParameters(element);

    var prevIndentationNodeOption = getIndentationNodeFromMacheteGeneratedEntry(prevMacheteGeneratedEntryOption.get());
    if (prevIndentationNodeOption.isEmpty()) {
      prevLevel = 0;
      hasPrevLevelCorrectWidth = true;
    } else {
      var prevIndentationText = prevIndentationNodeOption.get().getText();
      hasPrevLevelCorrectWidth = prevIndentationText.length() % indentationParameters.indentationWidth == 0;
      prevLevel = prevIndentationText.length() / indentationParameters.indentationWidth;
    }

    var thisIndentationText = element.getText();

    if (thisIndentationText.chars().filter(c -> c != indentationParameters.indentationCharacter).count() > 0) {
      holder.newAnnotation(HighlightSeverity.ERROR, "Indentation character does not match the first indented line")
          .range(element).create();
      return;
    }

    if (thisIndentationText.length() % indentationParameters.indentationWidth != 0) {
      holder.newAnnotation(HighlightSeverity.ERROR,
          "Indentation width is not multiple of ${indentationParameters.indentationWidth}" +
              " as first indented line suggests")
          .range(element).create();
    }

    thisLevel = thisIndentationText.length() / indentationParameters.indentationWidth;

    if (hasPrevLevelCorrectWidth && thisLevel > prevLevel + 1) {
      holder.newAnnotation(HighlightSeverity.ERROR, "Indentation level does not math")
          .range(element).create();
    }
  }

  private IndentationParameters findIndentationParameters(PsiElement currentElement) {
    var element = getFirstMacheteGeneratedEntry(currentElement);
    while (element.isDefined() && getIndentationNodeFromMacheteGeneratedEntry(element.get()).isEmpty()) {
      element = getNextSiblingMacheteGeneratedEntry(element.get());
    }

    if (element.isEmpty()) {
      // Default - theoretically this should never happen
      return new IndentationParameters(' ', 4);
    }

    var indentationNodeOption = getIndentationNodeFromMacheteGeneratedEntry(element.get());
    if (indentationNodeOption.isEmpty()) {
      // Default - this also theoretically should never happen
      return new IndentationParameters(' ', 4);
    }

    var indentationText = indentationNodeOption.get().getText();

    char indentationChar = indentationText.charAt(0);
    int indentationWidth = indentationText.length();

    return new IndentationParameters(indentationChar, indentationWidth);
  }

  private Option<ASTNode> getIndentationNodeFromMacheteGeneratedEntry(MacheteGeneratedEntry macheteEntry) {
    return Option.of(macheteEntry.getNode().findChildByType(MacheteGeneratedElementTypes.INDENTATION));
  }

  private Option<MacheteGeneratedEntry> getFirstMacheteGeneratedEntry(PsiElement currentElement) {
    PsiElement root = currentElement;
    while (root.getParent() != null) {
      root = root.getParent();
    }

    return getNextSiblingMacheteGeneratedEntry(root.getFirstChild());
  }

  private Option<MacheteGeneratedEntry> getNextSiblingMacheteGeneratedEntry(PsiElement currentElement) {
    PsiElement nextSiblingMacheteGeneratedEntry = currentElement.getNextSibling();
    while (nextSiblingMacheteGeneratedEntry != null && !(nextSiblingMacheteGeneratedEntry instanceof MacheteGeneratedEntry)) {
      nextSiblingMacheteGeneratedEntry = nextSiblingMacheteGeneratedEntry.getNextSibling();
    }

    if (nextSiblingMacheteGeneratedEntry == null || !(nextSiblingMacheteGeneratedEntry instanceof MacheteGeneratedEntry)) {
      return Option.none();
    }

    return Option.of((MacheteGeneratedEntry) nextSiblingMacheteGeneratedEntry);
  }

  private Option<MacheteGeneratedEntry> getPrevSiblingMacheteGeneratedEntry(PsiElement currentElement) {
    PsiElement prevSiblingMacheteGeneratedEntry = currentElement.getPrevSibling();
    while (prevSiblingMacheteGeneratedEntry != null && !(prevSiblingMacheteGeneratedEntry instanceof MacheteGeneratedEntry)) {
      prevSiblingMacheteGeneratedEntry = prevSiblingMacheteGeneratedEntry.getPrevSibling();
    }

    if (prevSiblingMacheteGeneratedEntry == null || !(prevSiblingMacheteGeneratedEntry instanceof MacheteGeneratedEntry)) {
      return Option.none();
    }

    return Option.of((MacheteGeneratedEntry) prevSiblingMacheteGeneratedEntry);
  }

  @Data
  private class IndentationParameters {
    private final char indentationCharacter;
    private final int indentationWidth;
  }
}
