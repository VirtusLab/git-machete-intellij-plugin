package com.virtuslab.gitmachete.frontend.file.grammar;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;

public class MacheteTokenType extends IElementType {
  public MacheteTokenType(@NonNls String debugName) {
    super(debugName, MacheteLanguage.INSTANCE);
  }

  @Override
  public String toString() {
    return MacheteTokenType.class.getSimpleName() + super.toString();
  }
}
