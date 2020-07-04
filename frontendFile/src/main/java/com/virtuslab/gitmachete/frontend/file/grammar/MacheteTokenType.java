package com.virtuslab.gitmachete.frontend.file.grammar;

import com.intellij.psi.tree.IElementType;
import lombok.ToString;
import org.jetbrains.annotations.NonNls;

@ToString
public class MacheteTokenType extends IElementType {
  public MacheteTokenType(@NonNls String debugName) {
    super(debugName, MacheteLanguage.INSTANCE);
  }
}
