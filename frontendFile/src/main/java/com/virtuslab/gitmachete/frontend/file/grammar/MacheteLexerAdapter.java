package com.virtuslab.gitmachete.frontend.file.grammar;

import com.intellij.lexer.FlexAdapter;

public class MacheteLexerAdapter extends FlexAdapter {
  @SuppressWarnings("nullness:argument.type.incompatible")
  public MacheteLexerAdapter() {
    super(new MacheteGeneratedLexer(/* in */ null));
  }
}
