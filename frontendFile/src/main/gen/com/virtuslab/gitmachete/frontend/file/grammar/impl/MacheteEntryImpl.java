// This is a generated file. Not intended for manual editing.
package com.virtuslab.gitmachete.frontend.file.grammar.impl;

import static com.virtuslab.gitmachete.frontend.file.grammar.MacheteTypes.*;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.*;

import com.virtuslab.gitmachete.frontend.file.grammar.*;

public class MacheteEntryImpl extends ASTWrapperPsiElement implements MacheteEntry {

  public MacheteEntryImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MacheteVisitor visitor) {
    visitor.visitEntry(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof MacheteVisitor)
      accept((MacheteVisitor) visitor);
    else
      super.accept(visitor);
  }

}
