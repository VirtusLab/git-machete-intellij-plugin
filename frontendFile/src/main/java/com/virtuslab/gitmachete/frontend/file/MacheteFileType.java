package com.virtuslab.gitmachete.frontend.file;

import javax.swing.Icon;

import com.intellij.openapi.fileTypes.LanguageFileType;
import icons.MacheteIcons;

import com.virtuslab.gitmachete.frontend.file.grammar.MacheteLanguage;

public final class MacheteFileType extends LanguageFileType {
  public static final MacheteFileType instance = new MacheteFileType();

  private MacheteFileType() {
    super(MacheteLanguage.instance);
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public String getName() {
    return "Machete File";
  }

  @Override
  public String getDescription() {
    return "Branch layout file for Git Machete";
  }

  @Override
  public String getDefaultExtension() {
    return "machete";
  }

  @Override
  public Icon getIcon() {
    return MacheteIcons.MACHETE_FILE;
  }
}
