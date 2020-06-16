package com.virtuslab.gitmachete.frontend.file;

import com.intellij.lexer.FlexAdapter;
import java.io.Reader;

public class MacheteLexerAdapter extends FlexAdapter {
    public MacheteLexerAdapter() {
        super(new MacheteLexer((Reader) null));
    }
}
