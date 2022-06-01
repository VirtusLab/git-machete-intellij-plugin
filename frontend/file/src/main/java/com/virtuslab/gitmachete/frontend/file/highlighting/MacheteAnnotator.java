package com.virtuslab.gitmachete.frontend.file.highlighting;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.util.OptionalInt;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import io.vavr.control.Option;
import lombok.Data;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.compat.UiThreadExecutionCompat;
import com.virtuslab.gitmachete.frontend.file.MacheteFileUtils;
import com.virtuslab.gitmachete.frontend.file.grammar.MacheteFile;
import com.virtuslab.gitmachete.frontend.file.grammar.MacheteGeneratedBranch;
import com.virtuslab.gitmachete.frontend.file.grammar.MacheteGeneratedElementTypes;
import com.virtuslab.gitmachete.frontend.file.grammar.MacheteGeneratedEntry;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod({GitMacheteBundle.class, MacheteFileUtils.class})
public class MacheteAnnotator implements Annotator, DumbAware {
  private boolean cantGetBranchesMessageWasShown = false;

  @Override
  @UIThreadUnsafe
  public void annotate(PsiElement element, AnnotationHolder holder) {
    if (element instanceof MacheteGeneratedEntry) {
      processMacheteGeneratedEntry((MacheteGeneratedEntry) element, holder);
    } else if (element.getNode().getElementType().equals(MacheteGeneratedElementTypes.INDENTATION)) {
      processIndentationElement(element, holder);
    }
  }

  @UIEffect
  private void showCantGetBranchesMessage(PsiFile file) {
    Editor currentEditor = FileEditorManager.getInstance(file.getProject()).getSelectedTextEditor();
    if (currentEditor == null) {
      return;
    }
    HintManager.getInstance().showInformationHint(currentEditor,
        getString("string.GitMachete.MacheteAnnotator.could-not-retrieve-local-branches"), HintManager.ABOVE);
    cantGetBranchesMessageWasShown = true;
  }

  @UIThreadUnsafe
  private void processMacheteGeneratedEntry(MacheteGeneratedEntry macheteEntry, AnnotationHolder holder) {
    MacheteGeneratedBranch branch = macheteEntry.getBranch();

    PsiFile file = macheteEntry.getContainingFile();
    val branchNames = file.getBranchNamesForPsiMacheteFile();

    if (branchNames.isEmpty()) {
      if (!cantGetBranchesMessageWasShown) {
        UiThreadExecutionCompat.invokeLaterIfNeeded(NON_MODAL, () -> showCantGetBranchesMessage(file));
      }
      return;
    }
    cantGetBranchesMessageWasShown = false;

    val processedBranchName = branch.getText();

    if (!branchNames.contains(processedBranchName)) {
      holder
          .newAnnotation(HighlightSeverity.ERROR,
              getString("string.GitMachete.MacheteAnnotator.cannot-find-local-branch-in-repo").format(processedBranchName))
          .range(branch).create();
    }
  }

  private void processIndentationElement(PsiElement element, AnnotationHolder holder) {
    PsiElement parent = element.getParent();
    assert parent != null : "Element has no parent";

    if (parent instanceof MacheteFile) {
      return;
    }

    val prevMacheteGeneratedEntryOption = getPrevSiblingMacheteGeneratedEntry(parent);
    if (prevMacheteGeneratedEntryOption.isEmpty()) {
      holder.newAnnotation(HighlightSeverity.ERROR, getString("string.GitMachete.MacheteAnnotator.cannot-indent-first-entry"))
          .range(element).create();
      return;
    }

    int prevLevel;
    int thisLevel;
    boolean hasPrevLevelCorrectWidth;

    IndentationParameters indentationParameters = findIndentationParameters(element);

    val prevIndentationNodeOption = getIndentationNodeFromMacheteGeneratedEntry(prevMacheteGeneratedEntryOption.get());
    if (prevIndentationNodeOption.isEmpty()) {
      prevLevel = 0;
      hasPrevLevelCorrectWidth = true;
    } else {
      val prevIndentationText = prevIndentationNodeOption.get().getText();
      hasPrevLevelCorrectWidth = prevIndentationText.length() % indentationParameters.indentationWidth == 0;
      prevLevel = prevIndentationText.length() / indentationParameters.indentationWidth;
    }

    val thisIndentationText = element.getText();

    OptionalInt wrongIndentChar = thisIndentationText.chars().filter(c -> c != indentationParameters.indentationCharacter)
        .findFirst();
    if (wrongIndentChar.isPresent()) {
      holder.newAnnotation(HighlightSeverity.ERROR,
          getString("string.GitMachete.MacheteAnnotator.indent-char-not-match")
              .format(indentCharToName((char) wrongIndentChar.getAsInt()),
                  indentCharToName(indentationParameters.indentationCharacter)))
          .range(element).create();
      return;
    }

    if (thisIndentationText.length() % indentationParameters.indentationWidth != 0) {
      holder
          .newAnnotation(HighlightSeverity.ERROR,
              getString("string.GitMachete.MacheteAnnotator.indent-width-not-match")
                  .format(String.valueOf(indentationParameters.indentationWidth)))
          .range(element).create();
    }

    thisLevel = thisIndentationText.length() / indentationParameters.indentationWidth;

    if (hasPrevLevelCorrectWidth && thisLevel > prevLevel + 1) {
      holder.newAnnotation(HighlightSeverity.ERROR, getString("string.GitMachete.MacheteAnnotator.too-much-indent"))
          .range(element).create();
    }
  }

  private IndentationParameters findIndentationParameters(PsiElement currentElement) {
    Option<MacheteGeneratedEntry> element = getFirstMacheteGeneratedEntry(currentElement);
    while (element.isDefined() && getIndentationNodeFromMacheteGeneratedEntry(element.get()).isEmpty()) {
      element = getNextSiblingMacheteGeneratedEntry(element.get());
    }

    if (element.isEmpty()) {
      // Default - theoretically this should never happen
      return new IndentationParameters(' ', 4);
    }

    val indentationNodeOption = getIndentationNodeFromMacheteGeneratedEntry(element.get());
    if (indentationNodeOption.isEmpty()) {
      // Default - this also theoretically should never happen
      return new IndentationParameters(' ', 4);
    }

    val indentationText = indentationNodeOption.get().getText();

    @SuppressWarnings("index")
    // Indentation text is never empty (otherwise element does not exist)
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

  private String indentCharToName(char indentChar) {
    if (indentChar == ' ') {
      return "SPACE";
    } else if (indentChar == '\t') {
      return "TAB";
    } else {
      return "ASCII " + (int) indentChar;
    }
  }

  @Data
  private static class IndentationParameters {
    private final char indentationCharacter;
    private final int indentationWidth;
  }
}
