package com.virtuslab.gitmachete.frontend.file.grammar;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;

import com.virtuslab.gitmachete.frontend.defs.FileTypeIds;
import com.virtuslab.gitmachete.frontend.file.MacheteFileType;

public class MacheteFile extends PsiFileBase {
  public MacheteFile(FileViewProvider viewProvider) {
    super(viewProvider, MacheteLanguage.instance);
  }

  @Override
  public FileType getFileType() {
    return MacheteFileType.instance;
  }

  @Override
  public String toString() {
    return FileTypeIds.NAME;
  }
}
