package com.virtuslab.gitmachete.frontend.file.highlighting;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

import com.virtuslab.gitmachete.frontend.file.grammar.MacheteGeneratedElementTypes;
import com.virtuslab.gitmachete.frontend.file.grammar.MacheteLexerAdapter;

public class MacheteSyntaxHighlighter extends SyntaxHighlighterBase {
  public static final TextAttributesKey BAD_CHARACTER = createTextAttributesKey("MACHETE_BAD_CHARACTER",
      HighlighterColors.BAD_CHARACTER);
  public static final TextAttributesKey PREFIX = createTextAttributesKey("MACHETE_PREFIX",
      DefaultLanguageHighlighterColors.KEYWORD);
  public static final TextAttributesKey NAME = createTextAttributesKey("MACHETE_NAME",
      DefaultLanguageHighlighterColors.CLASS_NAME);
  public static final TextAttributesKey CUSTOM_ANNOTATION = createTextAttributesKey("MACHETE_CUSTOM_ANNOTATION",
      DefaultLanguageHighlighterColors.LINE_COMMENT);

  private static final TextAttributesKey[] BAD_CHAR_KEYS = new TextAttributesKey[]{BAD_CHARACTER};
  private static final TextAttributesKey[] PREFIX_KEYS = new TextAttributesKey[]{PREFIX};
  private static final TextAttributesKey[] NAME_KEYS = new TextAttributesKey[]{NAME};
  private static final TextAttributesKey[] CUSTOM_ANNOTATION_KEYS = new TextAttributesKey[]{CUSTOM_ANNOTATION};
  private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

  @Override
  public Lexer getHighlightingLexer() {
    return new MacheteLexerAdapter();
  }

  @Override
  public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
    return Match(tokenType).of(
        Case($(MacheteGeneratedElementTypes.PREFIX), PREFIX_KEYS),
        Case($(MacheteGeneratedElementTypes.NAME), NAME_KEYS),
        Case($(MacheteGeneratedElementTypes.CUSTOM_ANNOTATION), CUSTOM_ANNOTATION_KEYS),
        Case($(TokenType.BAD_CHARACTER), BAD_CHAR_KEYS),
        Case($(), EMPTY_KEYS));
  }
}
