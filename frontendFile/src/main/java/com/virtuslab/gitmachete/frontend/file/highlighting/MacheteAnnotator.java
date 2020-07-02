package com.virtuslab.gitmachete.frontend.file.highlighting;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.ui.GuiUtils;
import io.vavr.control.Option;
import lombok.Data;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.file.MacheteFileUtils;
import com.virtuslab.gitmachete.frontend.file.grammar.MacheteFile;
import com.virtuslab.gitmachete.frontend.file.grammar.MacheteGeneratedBranch;
import com.virtuslab.gitmachete.frontend.file.grammar.MacheteGeneratedElementTypes;
import com.virtuslab.gitmachete.frontend.file.grammar.MacheteGeneratedEntry;

public class MacheteAnnotator implements Annotator {
  private boolean cantGetBranchesMessageWasShown = false;

  @Override
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
    HintManager.getInstance().showErrorHint(currentEditor,
        "We can't get project branches, so we can't checking it names", HintManager.ABOVE);
    cantGetBranchesMessageWasShown = true;
  }

  private void processMacheteGeneratedEntry(MacheteGeneratedEntry macheteEntry, AnnotationHolder holder) {
    MacheteGeneratedBranch branch = macheteEntry.getBranch();

    PsiFile file = macheteEntry.getContainingFile();
    var branchNamesOption = MacheteFileUtils.getBranchNamesForFile(file);

    if (branchNamesOption.isEmpty() && !cantGetBranchesMessageWasShown) {
      GuiUtils.invokeLaterIfNeeded(() -> showCantGetBranchesMessage(file), NON_MODAL);
      return;
    }
    cantGetBranchesMessageWasShown = false;

    var branchNames = branchNamesOption.get();

    var processedBranchName = branch.getText();

    if (!branchNames.contains(processedBranchName)) {
      holder.newAnnotation(HighlightSeverity.ERROR, "Can't find local branch '${processedBranchName}' in git repository")
          .range(branch).create();
    }
  }

  private void processIndentationElement(PsiElement element, AnnotationHolder holder) {
    PsiElement parent = element.getParent();
    assert parent != null : "Element has no parent";

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

    // Potentially unrelated - it's responsible of changing default TAB key behavior
    // (insert real TAB or insert some number of spaces) depending on detected file indentation.
    // In case of space insertion indent width (size) is also set
    // This is not the best place for this action (because it is unnecessarily invoked on every
    // indentation element processing) but for now we can't find better place
    CodeStyleSettings codeStyleSettings = CodeStyle.getSettings(element.getContainingFile());
    CommonCodeStyleSettings.IndentOptions indentOptions = codeStyleSettings.getIndentOptions();
    indentOptions.USE_TAB_CHARACTER = indentationParameters.getIndentationCharacter() == '\t';
    if (indentOptions.USE_TAB_CHARACTER) {
      indentOptions.INDENT_SIZE = indentOptions.TAB_SIZE;
    } else {
      indentOptions.INDENT_SIZE = indentationParameters.getIndentationWidth();
    }

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

    var wrongIndentChar = thisIndentationText.chars().filter(c -> c != indentationParameters.indentationCharacter).findFirst();
    if (wrongIndentChar.isPresent()) {
      holder.newAnnotation(HighlightSeverity.ERROR,
          "Indentation character (${indentCharToName((char)wrongIndentChar.getAsInt())}) "
              + "does not match the indentation character in first indented line (${indentCharToName(indentationParameters.indentationCharacter)})")
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
      holder.newAnnotation(HighlightSeverity.ERROR, "Too much indent on this line")
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
