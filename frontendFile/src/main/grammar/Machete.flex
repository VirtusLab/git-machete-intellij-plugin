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

%{
    Character indentationChar = null;
    int singleIndentWidth = 0;
    int currentIndentLevel = 0;

    private boolean checkIndentation(char indentType, int processedIndentWidth) {
        if (indentationChar == null)
            indentationChar = indentType;
        else if (!indentationChar.equals(indentType))
            return false;

        if (singleIndentWidth == 0) {
            singleIndentWidth = processedIndentWidth;
            currentIndentLevel = 1;
        } else if (processedIndentWidth % singleIndentWidth != 0) {
            return false;
        } else {
            int level = processedIndentWidth / singleIndentWidth;
            if (level < currentIndentLevel - 1 || level > currentIndentLevel + 1)
                return false;
            currentIndentLevel = level;
        }

        return true;
    }
%}

// End Of Line
EOL=[\r\n]+
// For sake of distinguishablility WHITESPACE and INDENTATION are two separated entries
// even if they are the same for now
WHITESPACE=[\ \t]+
SPACE_INDENTATION=\ +
TAB_INDENTATION=\t+
// Probably all characters allowed in branch name
// (https://mirrors.edge.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html)
// \w is standard regex abbreviation of [a-zA-Z0-9_]
NAME_WITHOUT_SLASH=[\w\-.!@#$%&()+={}\];'\",.<>|`]+
// In addition, names can contain / (slash)
NAME_WITH_SLASH=[\w\-.!@#$%&()+={}\];'\",.<>|`\/]+
SLASH="\/"
ANNOTATION=[^\r\n]+

%state AFTER_INDENTATION AFTER_PREFIX AFTER_NAME CUSTOM_ANNOTATION

%%

<YYINITIAL> {
    {SPACE_INDENTATION}                  {
                                            yybegin(AFTER_INDENTATION);
                                            if (!checkIndentation(' ', yytext().length()))
                                                return TokenType.BAD_CHARACTER;
                                            return MacheteGeneratedElementTypes.INDENTATION;
                                         }
    {TAB_INDENTATION}                    {
                                            yybegin(AFTER_INDENTATION);
                                            if (!checkIndentation('\t', yytext().length()))
                                                return TokenType.BAD_CHARACTER;
                                            return MacheteGeneratedElementTypes.INDENTATION;
                                         }
    {NAME_WITHOUT_SLASH}{SLASH}          {
                                            yybegin(AFTER_PREFIX);
                                            currentIndentLevel = 0;
                                            return MacheteGeneratedElementTypes.PREFIX;
                                         }
    {NAME_WITHOUT_SLASH}                 {
                                            yybegin(AFTER_NAME);
                                            currentIndentLevel = 0;
                                            return MacheteGeneratedElementTypes.NAME;
                                         }
    {EOL}                                {
                                            return MacheteGeneratedElementTypes.EOL;
                                         }
}

<AFTER_INDENTATION> {
    {NAME_WITHOUT_SLASH}{SLASH}          { yybegin(AFTER_PREFIX); return MacheteGeneratedElementTypes.PREFIX; }
    {NAME_WITHOUT_SLASH}                 { yybegin(AFTER_NAME); return MacheteGeneratedElementTypes.NAME; }
    {EOL}                                { return MacheteGeneratedElementTypes.EOL; }
}

<AFTER_PREFIX> {
    {NAME_WITH_SLASH}                    { yybegin(AFTER_NAME); return MacheteGeneratedElementTypes.NAME; }
}

<AFTER_NAME> {
    {WHITESPACE}                         { yybegin(CUSTOM_ANNOTATION); return TokenType.WHITE_SPACE; }
    {EOL}                                { yybegin(YYINITIAL); return MacheteGeneratedElementTypes.EOL; }
}

<CUSTOM_ANNOTATION> {
    {ANNOTATION}                         { return MacheteGeneratedElementTypes.CUSTOM_ANNOTATION; }
    {EOL}                                { yybegin(YYINITIAL); return MacheteGeneratedElementTypes.EOL; }
}

[^]                                      { return TokenType.BAD_CHARACTER; }
