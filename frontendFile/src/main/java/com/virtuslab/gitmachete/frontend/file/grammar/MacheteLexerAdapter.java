package com.virtuslab.gitmachete.frontend.file.grammar;

import com.intellij.lexer.FlexAdapter;

public class MacheteLexerAdapter extends FlexAdapter {
  public MacheteLexerAdapter() {
    super(new MacheteLexer(null));
  }
}
