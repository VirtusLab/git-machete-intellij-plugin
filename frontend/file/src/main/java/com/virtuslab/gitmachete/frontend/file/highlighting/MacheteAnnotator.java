package com.virtuslab.gitmachete.frontend.file.highlighting;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.diagnostic.PluginException;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ModalityUiUtil;
import lombok.Data;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

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
        ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> showCantGetBranchesMessage(file));
      }
      return;
    }
    cantGetBranchesMessageWasShown = false;

    val processedBranchName = branch.getText();

    if (!branchNames.contains(processedBranchName)) {
      holder
          .newAnnotation(HighlightSeverity.ERROR,
              getNonHtmlString("string.GitMachete.MacheteAnnotator.cannot-find-local-branch-in-repo")
                  .format(processedBranchName))
          .range(branch).create();
    }

    // update the state of the .git/machete VirtualFile so that new entry is available in the VirtualFile
    ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> saveDocumentBeforeCheck(file));
    // check for duplicate entries in the machete file
    try {
      val branchNamesFromFile = VfsUtil.loadText(file.getVirtualFile());
      if (isBranchNameRepeated(branchNamesFromFile, processedBranchName)) {
        holder.newAnnotation(HighlightSeverity.ERROR,
            getNonHtmlString("string.GitMachete.MacheteAnnotator.branch-entry-already-defined")
                .format(processedBranchName))
            .range(branch).create();
      }
    } catch (PluginException | IllegalStateException ignored) { // ignore dubious IDE checks against annotation range
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @UIEffect
  private void saveDocumentBeforeCheck(PsiFile file) {
    val fileDocManager = FileDocumentManager.getInstance();
    fileDocManager.saveDocument(Objects.requireNonNull(fileDocManager.getDocument(file.getVirtualFile())));
  }

  private boolean isBranchNameRepeated(String fileContent, String branchName) {
    boolean result = false;
    List<String> lines = Arrays.stream(fileContent.split("\n")).map(String::trim).collect(Collectors.toList());
    if (lines.stream().filter(line -> line.equals(branchName)).count() > 1) {
      result = true;
    }
    return result;
  }

  private void processIndentationElement(PsiElement element, AnnotationHolder holder) {
    PsiElement parent = element.getParent();
    assert parent != null : "Element has no parent";

    if (parent instanceof MacheteFile) {
      return;
    }

    val prevMacheteGeneratedEntryOption = getPrevSiblingMacheteGeneratedEntry(parent);
    if (prevMacheteGeneratedEntryOption == null) {
      holder
          .newAnnotation(HighlightSeverity.ERROR,
              getNonHtmlString("string.GitMachete.MacheteAnnotator.cannot-indent-first-entry"))
          .range(element).create();
      return;
    }

    int prevLevel;
    int thisLevel;
    boolean hasPrevLevelCorrectWidth;

    IndentationParameters indentationParameters = findIndentationParameters(element);

    val prevIndentationNodeOption = getIndentationNodeFromMacheteGeneratedEntry(prevMacheteGeneratedEntryOption);
    if (prevIndentationNodeOption == null) {
      prevLevel = 0;
      hasPrevLevelCorrectWidth = true;
    } else {
      val prevIndentationText = prevIndentationNodeOption.getText();
      hasPrevLevelCorrectWidth = prevIndentationText.length() % indentationParameters.indentationWidth == 0;
      prevLevel = prevIndentationText.length() / indentationParameters.indentationWidth;
    }

    val thisIndentationText = element.getText();

    OptionalInt wrongIndentChar = thisIndentationText.chars().filter(c -> c != indentationParameters.indentationCharacter)
        .findFirst();
    if (wrongIndentChar.isPresent()) {
      holder.newAnnotation(HighlightSeverity.ERROR,
          getNonHtmlString("string.GitMachete.MacheteAnnotator.indent-char-not-match")
              .format(indentCharToName((char) wrongIndentChar.getAsInt()),
                  indentCharToName(indentationParameters.indentationCharacter)))
          .range(element).create();
      return;
    }

    if (thisIndentationText.length() % indentationParameters.indentationWidth != 0) {
      holder
          .newAnnotation(HighlightSeverity.ERROR,
              getNonHtmlString("string.GitMachete.MacheteAnnotator.indent-width-not-match")
                  .format(String.valueOf(indentationParameters.indentationWidth)))
          .range(element).create();
    }

    thisLevel = thisIndentationText.length() / indentationParameters.indentationWidth;

    if (hasPrevLevelCorrectWidth && thisLevel > prevLevel + 1) {
      holder.newAnnotation(HighlightSeverity.ERROR, getNonHtmlString("string.GitMachete.MacheteAnnotator.too-much-indent"))
          .range(element).create();
    }
  }

  private IndentationParameters findIndentationParameters(PsiElement currentElement) {
    MacheteGeneratedEntry element = getFirstMacheteGeneratedEntry(currentElement);
    while (element != null && getIndentationNodeFromMacheteGeneratedEntry(element) == null) {
      element = getNextSiblingMacheteGeneratedEntry(element);
    }

    if (element == null) {
      // Default - theoretically this should never happen
      return new IndentationParameters(' ', 4);
    }

    val indentationNodeOption = getIndentationNodeFromMacheteGeneratedEntry(element);
    if (indentationNodeOption == null) {
      // Default - this also theoretically should never happen
      return new IndentationParameters(' ', 4);
    }

    val indentationText = indentationNodeOption.getText();

    @SuppressWarnings("index")
    // Indentation text is never empty (otherwise element does not exist)
    char indentationChar = indentationText.charAt(0);
    int indentationWidth = indentationText.length();

    return new IndentationParameters(indentationChar, indentationWidth);
  }

  private @Nullable ASTNode getIndentationNodeFromMacheteGeneratedEntry(MacheteGeneratedEntry macheteEntry) {
    return macheteEntry.getNode().findChildByType(MacheteGeneratedElementTypes.INDENTATION);
  }

  private @Nullable MacheteGeneratedEntry getFirstMacheteGeneratedEntry(PsiElement currentElement) {
    PsiElement root = currentElement;
    while (root.getParent() != null) {
      root = root.getParent();
    }

    return getNextSiblingMacheteGeneratedEntry(root.getFirstChild());
  }

  private @Nullable MacheteGeneratedEntry getNextSiblingMacheteGeneratedEntry(PsiElement currentElement) {
    PsiElement nextSiblingMacheteGeneratedEntry = currentElement.getNextSibling();
    while (nextSiblingMacheteGeneratedEntry != null && !(nextSiblingMacheteGeneratedEntry instanceof MacheteGeneratedEntry)) {
      nextSiblingMacheteGeneratedEntry = nextSiblingMacheteGeneratedEntry.getNextSibling();
    }

    if (nextSiblingMacheteGeneratedEntry == null || !(nextSiblingMacheteGeneratedEntry instanceof MacheteGeneratedEntry)) {
      return null;
    }

    return (MacheteGeneratedEntry) nextSiblingMacheteGeneratedEntry;
  }

  private @Nullable MacheteGeneratedEntry getPrevSiblingMacheteGeneratedEntry(PsiElement currentElement) {
    PsiElement prevSiblingMacheteGeneratedEntry = currentElement.getPrevSibling();
    while (prevSiblingMacheteGeneratedEntry != null && !(prevSiblingMacheteGeneratedEntry instanceof MacheteGeneratedEntry)) {
      prevSiblingMacheteGeneratedEntry = prevSiblingMacheteGeneratedEntry.getPrevSibling();
    }

    if (prevSiblingMacheteGeneratedEntry == null || !(prevSiblingMacheteGeneratedEntry instanceof MacheteGeneratedEntry)) {
      return null;
    }

    return (MacheteGeneratedEntry) prevSiblingMacheteGeneratedEntry;
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
