package com.virtuslab.gitmachete.frontend.file;

import javax.swing.Icon;

import com.intellij.openapi.fileTypes.LanguageFileType;

import com.virtuslab.gitmachete.frontend.defs.FileTypeIds;
import com.virtuslab.gitmachete.frontend.file.grammar.MacheteLanguage;
import com.virtuslab.gitmachete.frontend.icons.MacheteIcons;

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
    return FileTypeIds.NAME;
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
