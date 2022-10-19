package com.virtuslab.gitmachete.frontend.file.grammar;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;

public class MacheteParserDefinition implements ParserDefinition {
  public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);
  public static final TokenSet COMMENTS = TokenSet.create(MacheteGeneratedElementTypes.COMMENT);

  public static final IFileElementType FILE = new IFileElementType(MacheteLanguage.instance);

  @Override
  public Lexer createLexer(Project project) {
    return new MacheteLexerAdapter();
  }

  @Override
  public TokenSet getWhitespaceTokens() {
    return WHITE_SPACES;
  }

  // Even if we don't support comments, this element is part of the ParserDefinition interface.
  // So - we need to provide something here.
  @Override
  public TokenSet getCommentTokens() {
    return COMMENTS;
  }

  @Override
  public TokenSet getStringLiteralElements() {
    return TokenSet.EMPTY;
  }

  @Override
  public PsiParser createParser(final Project project) {
    return new MacheteGeneratedParser();
  }

  @Override
  public IFileElementType getFileNodeType() {
    return FILE;
  }

  @Override
  public PsiFile createFile(FileViewProvider viewProvider) {
    return new MacheteFile(viewProvider);
  }

  @Override
  public SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
    return SpaceRequirements.MAY;
  }

  @Override
  public PsiElement createElement(ASTNode node) {
    return MacheteGeneratedElementTypes.Factory.createElement(node);
  }
}
