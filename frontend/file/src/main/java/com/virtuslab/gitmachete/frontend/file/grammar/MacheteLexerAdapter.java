package com.virtuslab.gitmachete.frontend.file.grammar;

import com.intellij.lexer.FlexAdapter;

public class MacheteLexerAdapter extends FlexAdapter {
  @SuppressWarnings("nullness:argument")
  public MacheteLexerAdapter() {
    super(new MacheteGeneratedLexer(/* in */ null));
  }
}
