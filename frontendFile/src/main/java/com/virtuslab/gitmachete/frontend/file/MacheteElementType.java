package com.virtuslab.gitmachete.frontend.file;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class MacheteElementType extends IElementType {
    public MacheteElementType(@NotNull @NonNls String debugName) {
        super(debugName, MacheteLanguage.INSTANCE);
    }
}
