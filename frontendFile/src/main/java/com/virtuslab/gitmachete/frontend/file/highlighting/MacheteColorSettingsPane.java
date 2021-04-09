package com.virtuslab.gitmachete.frontend.file.highlighting;

import javax.swing.Icon;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import icons.MacheteIcons;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MacheteColorSettingsPane implements ColorSettingsPage {
  private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
      new AttributesDescriptor("Branch prefix", MacheteSyntaxHighlighter.PREFIX),
      new AttributesDescriptor("Branch name", MacheteSyntaxHighlighter.NAME),
      new AttributesDescriptor("Custom annotation", MacheteSyntaxHighlighter.CUSTOM_ANNOTATION),
      new AttributesDescriptor("Bad value", MacheteSyntaxHighlighter.BAD_CHARACTER)
  };

  // We're deliberately using \n rather than `System.lineSeparator()` here
  // since it turned out that even on Windows (which generally uses \r\n) IntelliJ expects \n in this context for some reason.
  @SuppressWarnings("regexp")
  public static final String NL = "\n";

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
    return "develop" + NL +
        "    allow-ownership-link PR #123" + NL +
        "        build-chain" + NL +
        "    call-ws PR #124" + NL +
        "master" + NL +
        "    hotfix/add-trigger PR #127";
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
    return "Machete";
  }
}
