package com.virtuslab.gitmachete.frontend.file.highlighting;

import javax.swing.Icon;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import icons.MacheteIcons;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.Nls;

public class MacheteColorSettingsPane implements ColorSettingsPage {
  private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
      new AttributesDescriptor("Branch Prefix", MacheteSyntaxHighlighter.PREFIX),
      new AttributesDescriptor("Branch Name", MacheteSyntaxHighlighter.NAME),
      new AttributesDescriptor("Custom Annotation", MacheteSyntaxHighlighter.CUSTOM_ANNOTATION),
      new AttributesDescriptor("Bad Value", MacheteSyntaxHighlighter.BAD_CHAR)
  };

  @Override
  public Icon getIcon() {
    return MacheteIcons.MACHETE_FILE;
  }

  @Override
  public SyntaxHighlighter getHighlighter() {
    return new MacheteSyntaxHighlighter();
  }

  @Override
  public String getDemoText() {
    return "develop" + System.lineSeparator() +
        "    allow-ownership-link PR #123" + System.lineSeparator() +
        "        build-chain PR #124" + System.lineSeparator() +
        "    call-ws" + System.lineSeparator() +
        "master" + System.lineSeparator() +
        "    hotfix/add-trigger  PR #127";
  }

  @Override
  public java.util.@Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return null;
  }

  @Override
  public AttributesDescriptor[] getAttributeDescriptors() {
    return DESCRIPTORS;
  }

  @Override
  public ColorDescriptor[] getColorDescriptors() {
    return ColorDescriptor.EMPTY_ARRAY;
  }

  @Override
  public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
    return "Machete";
  }
}
