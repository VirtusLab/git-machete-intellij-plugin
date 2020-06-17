// This is a generated file. Not intended for manual editing.
package com.virtuslab.gitmachete.frontend.file.grammar;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;

import com.virtuslab.gitmachete.frontend.file.grammar.impl.*;

public interface MacheteTypes {

  IElementType ENTRY = new MacheteElementType("ENTRY");

  IElementType COMMENT = new MacheteTokenType("COMMENT");
  IElementType CUSTOM_ANNOTATION = new MacheteTokenType("CUSTOM_ANNOTATION");
  IElementType EOL = new MacheteTokenType("EOL");
  IElementType INDENTATION = new MacheteTokenType("INDENTATION");
  IElementType NAME = new MacheteTokenType("NAME");
  IElementType PREFIX = new MacheteTokenType("PREFIX");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == ENTRY) {
        return new MacheteEntryImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
