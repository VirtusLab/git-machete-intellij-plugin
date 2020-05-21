package com.virtuslab.gitmachete.frontend.vfs.utils;

import javax.swing.Icon;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MacheteFileType extends LanguageFileType {
  public static final MacheteFileType INSTANCE = new MacheteFileType();

  public static final String FILE_NAME = "machete";

  protected MacheteFileType() {
    super(PlainTextLanguage.INSTANCE);
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public String getName() {
    return "Machete";
  }

  @Override
  public String getDescription() {
    return "Configuration file for Git Machete";
  }

  @Override
  public String getDefaultExtension() {
    return "";
  }

  @Override
  public @Nullable Icon getIcon() {
    return null;
  }
}
