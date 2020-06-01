package com.virtuslab.gitmachete.frontend.file;

import com.intellij.lang.Language;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.FileViewProviderFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SingleRootFileViewProvider;

public class MacheteFileViewProviderFactory implements FileViewProviderFactory {
  @Override
  public FileViewProvider createFileViewProvider(VirtualFile file,
      Language language,
      PsiManager manager,
      boolean eventSystemEnabled) {
    return new MacheteFileViewProvider(manager, file, eventSystemEnabled, language);
  }
}

class MacheteFileViewProvider extends SingleRootFileViewProvider {

  protected MacheteFileViewProvider(PsiManager manager,
      VirtualFile virtualFile,
      boolean eventSystemEnabled,
      Language language) {
    super(manager, virtualFile, eventSystemEnabled, language);
  }

  @Override
  protected boolean shouldCreatePsi() {
    return true;
  }
}
