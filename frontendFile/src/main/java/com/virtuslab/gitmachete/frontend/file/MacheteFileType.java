package com.virtuslab.gitmachete.frontend.file;

import javax.swing.Icon;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;

public class MacheteFileType extends LanguageFileType {
  public static final MacheteFileType INSTANCE = new MacheteFileType();

  protected MacheteFileType() {
    super(PlainTextLanguage.INSTANCE);
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public String getName() {
    return "Machete file";
  }

  @Override
  public String getDescription() {
    return "Configuration file for Git Machete";
  }

  @Override
  public String getDefaultExtension() {
    return "machete";
  }

  @Override
  public Icon getIcon() {
    return AllIcons.Actions.Menu_cut;
  }
}
