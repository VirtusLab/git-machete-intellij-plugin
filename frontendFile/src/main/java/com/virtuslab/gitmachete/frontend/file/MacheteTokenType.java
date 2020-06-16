package com.virtuslab.gitmachete.frontend.file;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class MacheteTokenType extends IElementType {
    public MacheteTokenType(@NotNull @NonNls String debugName) {
        super(debugName, MacheteLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        return "MacheteTokenType." + super.toString();
    }
}
