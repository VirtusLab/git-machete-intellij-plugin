package com.virtuslab.gitmachete.frontend.file.highlighting;

import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MacheteSyntaxHighlighterFactory extends SyntaxHighlighterFactory {
  @Override
  public SyntaxHighlighter getSyntaxHighlighter(@Nullable Project project, @Nullable VirtualFile virtualFile) {
    return new MacheteSyntaxHighlighter();
  }
}
