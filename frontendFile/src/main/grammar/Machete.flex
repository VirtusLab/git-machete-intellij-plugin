// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.virtuslab.gitmachete.frontend.file.grammar;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;

%%

%class MacheteGeneratedLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}


// End Of Line
EOL=[\r\n]
// For sake of distinguishablility WHITESPACE and INDENTATION are two separated entries
// even if they are the same for now
WHITESPACE=[\ \t]
INDENTATION=[\ \t]
// Probably all characters allowed in branch name
// (https://mirrors.edge.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html)
NAME_WITHOUT_SLASH=[\w\-.!@#$%&()+={}\];'\",.<>|`]
// In addition, names can contain / (slash)
NAME_WITH_SLASH=[\w\-.!@#$%&()+={}\];'\",.<>|`\/]
SEPARATOR="\/"
ANNOTATION=[^\r\n]

%state AFTER_PREFIX AFTER_NAME CUSTOM_ANNOTATION

%%

<YYINITIAL> {
    {INDENTATION}+                        { return MacheteGeneratedElementTypes.INDENTATION; }
    {NAME_WITHOUT_SLASH}+{SEPARATOR}      { yybegin(AFTER_PREFIX); return MacheteGeneratedElementTypes.PREFIX; }
    {NAME_WITHOUT_SLASH}+                 { yybegin(AFTER_NAME); return MacheteGeneratedElementTypes.NAME; }
    {EOL}+                                { return MacheteGeneratedElementTypes.EOL; }
}

<AFTER_PREFIX> {
    {NAME_WITH_SLASH}+                    { yybegin(AFTER_NAME); return MacheteGeneratedElementTypes.NAME; }
}

<AFTER_NAME> {
    {WHITESPACE}+                         { yybegin(CUSTOM_ANNOTATION); return TokenType.WHITE_SPACE; }
    {EOL}+                                { yybegin(YYINITIAL); return MacheteGeneratedElementTypes.EOL; }
}

<CUSTOM_ANNOTATION> {
    {ANNOTATION}+                         { return MacheteGeneratedElementTypes.CUSTOM_ANNOTATION; }
    {EOL}+                                { yybegin(YYINITIAL); return MacheteGeneratedElementTypes.EOL; }
}

[^]                                       { return TokenType.BAD_CHARACTER; }
