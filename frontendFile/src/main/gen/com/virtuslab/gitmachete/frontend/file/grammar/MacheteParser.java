// This is a generated file. Not intended for manual editing.
package com.virtuslab.gitmachete.frontend.file.grammar;

import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import static com.virtuslab.gitmachete.frontend.file.grammar.MacheteTypes.*;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LightPsiParser;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class MacheteParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return simpleFile(b, l + 1);
  }

  /* ********************************************************** */
  // INDENTATION? PREFIX? NAME CUSTOM_ANNOTATION?
  public static boolean entry(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "entry"))
      return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ENTRY, "<entry>");
    r = entry_0(b, l + 1);
    r = r && entry_1(b, l + 1);
    r = r && consumeToken(b, NAME);
    r = r && entry_3(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // INDENTATION?
  private static boolean entry_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "entry_0"))
      return false;
    consumeToken(b, INDENTATION);
    return true;
  }

  // PREFIX?
  private static boolean entry_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "entry_1"))
      return false;
    consumeToken(b, PREFIX);
    return true;
  }

  // CUSTOM_ANNOTATION?
  private static boolean entry_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "entry_3"))
      return false;
    consumeToken(b, CUSTOM_ANNOTATION);
    return true;
  }

  /* ********************************************************** */
  // entry|COMMENT|EOL
  static boolean item_(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "item_"))
      return false;
    boolean r;
    r = entry(b, l + 1);
    if (!r)
      r = consumeToken(b, COMMENT);
    if (!r)
      r = consumeToken(b, EOL);
    return r;
  }

  /* ********************************************************** */
  // item_*
  static boolean simpleFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simpleFile"))
      return false;
    while (true) {
      int c = current_position_(b);
      if (!item_(b, l + 1))
        break;
      if (!empty_element_parsed_guard_(b, "simpleFile", c))
        break;
    }
    return true;
  }

}
