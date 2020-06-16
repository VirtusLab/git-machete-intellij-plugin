// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.virtuslab.gitmachete.frontend.file;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import com.virtuslab.gitmachete.frontend.file.MacheteTypes;

%%

%class MacheteLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

CRLF=\R
INDENTATION=[\ \t]
NAME=[a-zA-Z0-9]
SEPARATOR="\/"
END_OF_LINE_COMMENT={INDENTATION}*"#"[^\r\n]*

/*KEY_CHARACTER=[^:=\ \n\t\f\\] | "\\ "
FIRST_VALUE_CHARACTER=[^ \n\f\\] | "\\"{CRLF} | "\\".
VALUE_CHARACTER=[^\n\f\\] | "\\"{CRLF} | "\\".*/

%state AFTER_INDENTATION AFTER_PREFIX

%%

{END_OF_LINE_COMMENT}                                       { yybegin(YYINITIAL); return MacheteTypes.COMMENT; }

<YYINITIAL> {INDENTATION}+                                  { yybegin(AFTER_INDENTATION); return MacheteTypes.INDENTATION; }

<YYINITIAL,AFTER_INDENTATION> ({NAME}{SEPARATOR})+          { yybegin(AFTER_PREFIX); return MacheteTypes.PREFIX; }

<AFTER_PREFIX> {NAME}                                       { yybegin(YYINITIAL); return MacheteTypes.NAME; }

[^]                                                         { return TokenType.BAD_CHARACTER; }
