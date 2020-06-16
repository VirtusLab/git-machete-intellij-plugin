package com.virtuslab.gitmachete.frontend.file;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

public class MacheteFile extends PsiFileBase {
    public MacheteFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, MacheteLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return MacheteFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "Machete File";
    }
}
