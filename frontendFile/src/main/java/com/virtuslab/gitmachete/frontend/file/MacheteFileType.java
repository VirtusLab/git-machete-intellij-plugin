package com.virtuslab.gitmachete.frontend.file;

import javax.swing.Icon;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import icons.MacheteIcons;

public final class MacheteFileType extends LanguageFileType {
  public static final MacheteFileType INSTANCE = new MacheteFileType();

  private MacheteFileType() {
    super(PlainTextLanguage.INSTANCE);
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public String getName() {
    return "MACHETE_FILE";
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
    return MacheteIcons.ICON;
  }
}
