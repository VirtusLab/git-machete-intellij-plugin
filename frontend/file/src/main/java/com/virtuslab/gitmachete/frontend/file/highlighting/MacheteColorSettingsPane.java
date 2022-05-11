package com.virtuslab.gitmachete.frontend.file.highlighting;

import javax.swing.Icon;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import icons.MacheteIcons;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.file.MacheteFileUtils;

public class MacheteColorSettingsPane implements ColorSettingsPage {
  private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
      new AttributesDescriptor("Branch prefix", MacheteSyntaxHighlighter.PREFIX),
      new AttributesDescriptor("Branch name", MacheteSyntaxHighlighter.NAME),
      new AttributesDescriptor("Custom annotation", MacheteSyntaxHighlighter.CUSTOM_ANNOTATION),
      new AttributesDescriptor("Bad value", MacheteSyntaxHighlighter.BAD_CHARACTER)
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
    return MacheteFileUtils.getSampleMacheteFileContents();
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
  public String getDisplayName() {
    return "Git Machete";
  }
}
