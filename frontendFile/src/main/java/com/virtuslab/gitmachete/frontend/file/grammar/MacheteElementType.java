package com.virtuslab.gitmachete.frontend.file.grammar;

import com.intellij.psi.tree.IElementType;

public class MacheteElementType extends IElementType {
  public MacheteElementType(String debugName) {
    super(debugName, MacheteLanguage.INSTANCE);
  }
}
