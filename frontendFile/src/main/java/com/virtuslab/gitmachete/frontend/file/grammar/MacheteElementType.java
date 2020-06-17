package com.virtuslab.gitmachete.frontend.file.grammar;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;

public class MacheteElementType extends IElementType {
  public MacheteElementType(@NonNls String debugName) {
    super(debugName, MacheteLanguage.INSTANCE);
  }
}
