/* The following code was generated by JFlex 1.7.0 tweaked for IntelliJ platform */
// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.virtuslab.gitmachete.frontend.file.grammar;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
/**
 * This class is a scanner generated by
 * <a href="http://www.jflex.de/">JFlex</a> 1.7.0
 * from the specification file <tt>Machete.flex</tt>
 */
class MacheteLexer implements FlexLexer {
  /** This character denotes the end of file */
  public static final int YYEOF = -1;
  /** initial size of the lookahead buffer */
  private static final int ZZ_BUFFERSIZE = 16384;
  /** lexical states */
  public static final int YYINITIAL = 0;
  public static final int AFTER_PREFIX = 2;
  public static final int AFTER_NAME = 4;
  public static final int CUSTOM_ANNOTATION = 6;
  /**
   * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
   * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
   *                  at the beginning of a line
   * l is of the form l = 2*k, k a non negative integer
   */
  private static final int ZZ_LEXSTATE[] = {
      0, 0, 1, 1, 2, 2, 3, 3
  };
  /**
   * Translates characters to character classes
   * Chosen bits are [11, 6, 4]
   * Total runtime size is 15104 bytes
   */
  public static int ZZ_CMAP(int ch) {
    return ZZ_CMAP_A[(ZZ_CMAP_Y[(ZZ_CMAP_Z[ch >> 10] << 6) | ((ch >> 4) & 0x3f)] << 4) | (ch & 0xf)];
  }
  /* The ZZ_CMAP_Z table has 1088 entries */
  static final char ZZ_CMAP_Z[] = zzUnpackCMap(
      "\1\0\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14\6\15\1\16\23\15" +
          "\1\17\1\15\1\20\1\21\12\15\1\22\10\12\1\23\1\24\1\25\1\26\1\27\1\30\1\31\1" +
          "\32\1\33\1\34\1\35\1\36\2\12\1\15\1\37\3\12\1\40\10\12\1\41\1\42\5\15\1\43" +
          "\1\44\11\12\1\45\2\12\1\46\4\12\1\47\1\50\1\51\1\12\1\52\1\12\1\53\1\54\1" +
          "\55\3\12\51\15\1\56\3\15\1\57\1\60\4\15\1\61\12\12\1\62\u02c1\12\1\63\277" +
          "\12");
  /* The ZZ_CMAP_Y table has 3328 entries */
  static final char ZZ_CMAP_Y[] = zzUnpackCMap(
      "\1\0\1\1\1\2\1\3\1\4\1\5\1\4\1\6\2\1\1\7\1\10\1\4\1\11\1\4\1\11\34\4\1\12" +
          "\1\13\1\14\1\1\7\4\1\15\1\16\1\4\1\17\4\4\1\20\10\4\1\17\12\4\1\21\1\4\1\22" +
          "\1\21\1\4\1\23\1\21\1\4\1\24\1\25\1\4\1\26\1\27\1\1\1\26\4\4\1\30\6\4\1\31" +
          "\1\32\1\33\1\1\3\4\1\34\6\4\1\13\3\4\1\35\2\4\1\6\1\1\1\4\1\36\4\1\1\4\1\37" +
          "\1\1\1\40\1\17\7\4\1\41\1\21\1\31\1\42\1\32\1\43\1\44\1\45\1\41\1\13\1\46" +
          "\1\42\1\32\1\47\1\50\1\51\1\52\1\53\1\54\1\17\1\32\1\55\1\56\1\57\1\41\1\60" +
          "\1\61\1\42\1\32\1\55\1\62\1\63\1\41\1\64\1\65\1\66\1\67\1\30\1\70\1\71\1\52" +
          "\1\1\1\72\1\73\1\32\1\74\1\75\1\76\1\41\1\1\1\72\1\73\1\32\1\77\1\75\1\100" +
          "\1\41\1\101\1\102\1\73\1\4\1\34\1\103\1\104\1\41\1\105\1\106\1\107\1\4\1\110" +
          "\1\111\1\112\1\52\1\113\1\21\2\4\1\26\1\114\1\115\2\1\1\116\1\117\1\120\1" +
          "\121\1\122\1\123\2\1\1\57\1\124\1\115\1\125\1\126\1\4\1\127\1\21\1\130\1\126" +
          "\1\4\1\127\1\131\3\1\4\4\1\115\4\4\1\6\2\4\1\132\2\4\1\133\24\4\1\134\1\135" +
          "\2\4\1\134\2\4\1\136\1\137\1\11\3\4\1\137\3\4\1\34\2\1\1\4\1\1\5\4\1\140\1" +
          "\21\45\4\1\33\1\4\1\21\1\26\4\4\1\141\1\142\1\143\1\144\1\4\1\144\1\4\1\145" +
          "\1\143\1\146\5\4\1\147\1\115\1\1\1\150\1\115\5\4\1\23\2\4\1\26\4\4\1\53\1" +
          "\4\1\114\2\36\1\52\1\4\1\6\1\144\2\4\1\36\1\4\2\115\2\1\1\4\1\36\3\4\1\114" +
          "\1\4\1\33\2\115\1\151\1\114\4\1\4\4\1\36\1\115\1\152\1\145\7\4\1\145\3\4\1" +
          "\23\1\74\2\4\1\6\1\142\4\1\1\153\1\4\1\154\17\4\1\155\21\4\1\140\2\4\1\140" +
          "\1\156\1\4\1\6\3\4\1\157\1\160\1\161\1\127\1\160\3\1\1\162\1\57\1\163\1\1" +
          "\1\164\1\1\1\127\3\1\2\4\1\57\1\165\1\166\1\167\1\123\1\170\1\1\2\4\1\142" +
          "\62\1\1\52\2\4\1\115\161\1\2\4\1\114\2\4\1\114\10\4\1\171\1\145\2\4\1\132" +
          "\3\4\1\172\1\162\1\4\1\173\4\174\2\4\2\1\1\162\35\1\1\175\1\1\1\21\1\176\1" +
          "\21\4\4\1\177\1\21\4\4\1\133\1\200\1\4\1\6\1\21\4\4\1\114\1\1\1\4\1\26\3\1" +
          "\1\4\40\1\133\4\1\53\4\1\135\4\1\53\2\1\10\4\1\127\4\1\2\4\1\6\20\4\1\127" +
          "\1\4\1\36\1\1\3\4\1\201\7\4\1\13\1\1\1\202\1\203\5\4\1\204\1\4\1\114\1\23" +
          "\3\1\1\202\2\4\1\23\1\1\3\4\1\145\4\4\1\53\1\115\1\4\1\205\2\4\1\6\2\4\1\145" +
          "\1\4\1\127\4\4\1\206\1\115\1\4\1\114\3\4\1\173\1\6\1\115\1\4\1\107\4\4\1\27" +
          "\1\150\1\4\1\207\1\210\1\211\1\174\2\4\1\133\1\53\7\4\1\212\1\115\72\4\1\145" +
          "\1\4\1\213\2\4\1\36\20\1\26\4\1\6\6\4\1\115\2\1\1\173\1\214\1\32\1\215\1\216" +
          "\6\4\1\13\1\1\1\217\25\4\1\6\1\1\4\4\1\203\2\4\1\23\2\1\1\36\1\4\1\1\1\4\1" +
          "\220\1\221\2\1\1\130\7\4\1\127\1\1\1\115\1\21\1\222\1\21\1\26\1\52\4\4\1\114" +
          "\1\223\1\224\2\1\1\225\1\4\1\11\1\226\2\6\2\1\7\4\1\26\4\1\3\4\1\144\7\1\1" +
          "\227\10\1\1\4\1\127\3\4\2\57\1\1\2\4\1\1\1\4\1\26\2\4\1\26\1\4\1\6\2\4\1\230" +
          "\1\231\2\1\11\4\1\6\1\115\2\4\1\230\1\4\1\36\2\4\1\23\3\4\1\145\11\1\23\4" +
          "\1\173\1\4\1\53\1\23\11\1\1\232\2\4\1\233\1\4\1\53\1\4\1\173\1\4\1\114\4\1" +
          "\1\4\1\234\1\4\1\53\1\4\1\115\4\1\3\4\1\235\4\1\1\236\1\237\1\4\1\240\2\1" +
          "\1\4\1\127\1\4\1\127\2\1\1\126\1\4\1\173\1\1\3\4\1\53\1\4\1\53\1\4\1\27\1" +
          "\4\1\13\6\1\4\4\1\142\3\1\3\4\1\27\3\4\1\27\60\1\4\4\1\173\1\1\1\52\1\162" +
          "\3\4\1\26\1\1\1\4\1\142\1\115\3\4\1\130\1\1\2\4\1\241\4\4\1\242\1\243\2\1" +
          "\1\4\1\17\1\4\1\244\4\1\1\245\1\24\1\142\3\4\1\26\1\115\1\31\1\42\1\32\1\55" +
          "\1\62\1\246\1\247\1\144\10\1\4\4\1\26\1\115\2\1\4\4\1\250\1\115\12\1\3\4\1" +
          "\251\1\57\1\252\2\1\4\4\1\253\1\115\2\1\3\4\1\23\1\115\3\1\1\4\1\74\1\36\1" +
          "\115\26\1\4\4\1\115\1\162\34\1\3\4\1\142\20\1\1\32\2\4\1\11\1\57\1\115\1\1" +
          "\1\203\1\4\1\203\1\126\1\173\64\1\71\4\1\115\6\1\6\4\1\114\1\1\14\4\1\145" +
          "\53\1\2\4\1\114\75\1\44\4\1\173\33\1\43\4\1\142\1\4\1\114\1\115\6\1\1\4\1" +
          "\6\1\144\3\4\1\173\1\145\1\115\1\217\1\254\1\4\67\1\4\4\1\144\2\4\1\114\1" +
          "\162\1\4\4\1\1\57\1\1\76\4\1\127\1\1\57\4\1\27\20\1\1\13\77\1\6\4\1\26\1\127" +
          "\1\142\1\255\114\1\1\256\1\257\1\260\1\1\1\261\11\1\1\262\33\1\5\4\1\130\3" +
          "\4\1\143\1\263\1\264\1\265\3\4\1\266\1\267\1\4\1\3\1\270\1\73\24\4\1\251\1" +
          "\4\1\73\1\133\1\4\1\133\1\4\1\130\1\4\1\130\1\114\1\4\1\114\1\4\1\32\1\4\1" +
          "\32\1\4\1\271\3\4\40\1\3\4\1\213\2\4\1\127\1\272\1\163\1\152\1\21\25\1\1\11" +
          "\1\204\1\273\75\1\14\4\1\144\1\173\2\1\4\4\1\26\1\115\112\1\1\265\1\4\1\274" +
          "\1\275\1\276\1\277\1\300\1\301\1\302\1\36\1\303\1\36\47\1\1\4\1\115\1\4\1" +
          "\115\1\4\1\115\47\1\55\4\1\173\2\1\103\4\1\144\15\4\1\6\150\4\1\13\25\1\41" +
          "\4\1\6\56\1\17\4\41\1");
  /* The ZZ_CMAP_A table has 3136 entries */
  static final char ZZ_CMAP_A[] = zzUnpackCMap(
      "\11\0\1\2\1\1\2\0\1\1\22\0\1\2\11\3\1\0\4\3\1\4\12\3\1\0\4\3\1\0\33\3\2\0" +
          "\1\3\1\0\17\3\14\0\1\3\12\0\1\3\4\0\1\3\5\0\7\3\1\0\12\3\4\0\14\3\16\0\5\3" +
          "\7\0\1\3\1\0\1\3\1\0\5\3\1\0\2\3\2\0\4\3\1\0\1\3\6\0\1\3\1\0\3\3\1\0\1\3\1" +
          "\0\4\3\1\0\23\3\1\0\11\3\1\0\26\3\2\0\1\3\6\0\10\3\10\0\16\3\1\0\1\3\1\0\2" +
          "\3\1\0\2\3\1\0\1\3\10\0\13\3\5\0\3\3\15\0\12\3\4\0\6\3\1\0\10\3\2\0\12\3\1" +
          "\0\23\3\2\0\14\3\2\0\11\3\4\0\1\3\5\0\14\3\4\0\5\3\1\0\10\3\6\0\20\3\2\0\13" +
          "\3\2\0\16\3\1\0\1\3\3\0\4\3\2\0\11\3\2\0\2\3\2\0\4\3\10\0\1\3\4\0\2\3\1\0" +
          "\1\3\1\0\3\3\1\0\6\3\4\0\2\3\1\0\2\3\1\0\2\3\1\0\2\3\2\0\1\3\1\0\5\3\4\0\2" +
          "\3\2\0\3\3\3\0\1\3\7\0\4\3\1\0\1\3\7\0\20\3\13\0\3\3\1\0\11\3\1\0\2\3\1\0" +
          "\2\3\1\0\5\3\2\0\12\3\1\0\3\3\1\0\3\3\2\0\1\3\30\0\1\3\7\0\3\3\1\0\10\3\2" +
          "\0\6\3\2\0\2\3\2\0\3\3\10\0\2\3\4\0\2\3\1\0\1\3\1\0\1\3\20\0\2\3\1\0\6\3\3" +
          "\0\3\3\1\0\4\3\3\0\2\3\1\0\1\3\1\0\2\3\3\0\2\3\3\0\3\3\3\0\5\3\3\0\3\3\1\0" +
          "\4\3\2\0\1\3\6\0\1\3\10\0\4\3\1\0\10\3\1\0\3\3\1\0\30\3\3\0\10\3\1\0\3\3\1" +
          "\0\4\3\7\0\2\3\1\0\3\3\5\0\4\3\1\0\5\3\2\0\4\3\5\0\2\3\7\0\1\3\2\0\2\3\16" +
          "\0\3\3\1\0\10\3\1\0\7\3\1\0\3\3\1\0\5\3\5\0\4\3\7\0\1\3\12\0\6\3\2\0\2\3\1" +
          "\0\22\3\3\0\10\3\1\0\11\3\1\0\1\3\2\0\7\3\3\0\1\3\4\0\6\3\1\0\1\3\1\0\10\3" +
          "\2\0\2\3\14\0\17\3\1\0\12\3\7\0\2\3\1\0\1\3\2\0\2\3\1\0\1\3\2\0\1\3\6\0\4" +
          "\3\1\0\7\3\1\0\3\3\1\0\1\3\1\0\1\3\2\0\2\3\1\0\15\3\1\0\3\3\2\0\5\3\1\0\1" +
          "\3\1\0\6\3\2\0\12\3\2\0\4\3\10\0\2\3\13\0\1\3\1\0\1\3\1\0\1\3\4\0\12\3\1\0" +
          "\24\3\3\0\5\3\1\0\12\3\6\0\1\3\11\0\6\3\1\0\1\3\5\0\1\3\2\0\13\3\1\0\15\3" +
          "\1\0\4\3\2\0\7\3\1\0\1\3\1\0\4\3\2\0\1\3\1\0\4\3\2\0\7\3\1\0\1\3\1\0\4\3\2" +
          "\0\16\3\2\0\6\3\2\0\13\3\3\0\13\3\7\0\15\3\1\0\7\3\13\0\4\3\14\0\1\3\1\0\2" +
          "\3\14\0\4\3\3\0\1\3\4\0\2\3\15\0\3\3\11\0\1\3\23\0\10\3\1\0\23\3\1\0\2\3\6" +
          "\0\6\3\5\0\15\3\1\0\1\3\1\0\1\3\1\0\1\3\1\0\6\3\1\0\7\3\1\0\1\3\3\0\3\3\1" +
          "\0\7\3\3\0\4\3\2\0\6\3\23\0\1\3\4\0\1\3\14\0\1\3\15\0\1\3\2\0\1\3\4\0\1\3" +
          "\2\0\12\3\1\0\1\3\3\0\5\3\6\0\1\3\1\0\1\3\1\0\1\3\1\0\4\3\1\0\1\3\5\0\5\3" +
          "\4\0\1\3\1\0\5\3\6\0\15\3\7\0\10\3\11\0\7\3\1\0\7\3\6\0\3\3\11\0\5\3\2\0\5" +
          "\3\3\0\7\3\2\0\2\3\2\0\3\3\5\0\16\3\1\0\12\3\1\0\1\3\7\0\11\3\2\0\27\3\2\0" +
          "\15\3\3\0\1\3\1\0\1\3\2\0\1\3\16\0\1\3\2\0\5\3\12\0\6\3\2\0\6\3\2\0\6\3\11" +
          "\0\13\3\1\0\2\3\2\0\7\3\4\0\5\3\3\0\5\3\5\0\12\3\1\0\5\3\1\0\1\3\1\0\2\3\1" +
          "\0\2\3\1\0\12\3\3\0\15\3\3\0\2\3\30\0\16\3\4\0\1\3\2\0\6\3\2\0\6\3\2\0\6\3" +
          "\2\0\3\3\3\0\14\3\1\0\16\3\1\0\2\3\1\0\1\3\15\0\1\3\2\0\4\3\4\0\10\3\1\0\5" +
          "\3\12\0\6\3\2\0\1\3\1\0\14\3\1\0\2\3\3\0\1\3\2\0\4\3\1\0\2\3\12\0\10\3\6\0" +
          "\6\3\1\0\2\3\5\0\10\3\1\0\3\3\1\0\13\3\4\0\3\3\4\0\5\3\2\0\1\3\11\0\5\3\5" +
          "\0\3\3\3\0\13\3\1\0\1\3\3\0\10\3\6\0\1\3\1\0\7\3\1\0\1\3\1\0\4\3\1\0\2\3\6" +
          "\0\1\3\5\0\7\3\2\0\7\3\3\0\6\3\1\0\1\3\10\0\6\3\2\0\10\3\10\0\6\3\2\0\1\3" +
          "\3\0\1\3\13\0\10\3\5\0\15\3\3\0\2\3\6\0\5\3\3\0\6\3\10\0\10\3\2\0\7\3\16\0" +
          "\4\3\4\0\3\3\15\0\1\3\2\0\2\3\2\0\4\3\1\0\14\3\1\0\1\3\1\0\7\3\1\0\21\3\1" +
          "\0\4\3\2\0\10\3\1\0\7\3\1\0\7\3\1\0\1\3\3\0\11\3\1\0\10\3\2\0\2\3\5\0\1\3" +
          "\12\0\2\3\1\0\2\3\1\0\5\3\6\0\2\3\1\0\1\3\2\0\1\3\1\0\12\3\1\0\4\3\1\0\1\3" +
          "\1\0\1\3\6\0\1\3\4\0\1\3\1\0\1\3\1\0\1\3\1\0\3\3\1\0\2\3\1\0\1\3\2\0\1\3\1" +
          "\0\1\3\1\0\1\3\1\0\1\3\1\0\1\3\1\0\2\3\1\0\1\3\2\0\4\3\1\0\7\3\1\0\4\3\1\0" +
          "\4\3\1\0\1\3\1\0\12\3\1\0\5\3\1\0\3\3\1\0\5\3\1\0\5\3");
  /**
   * Translates DFA states to action switch labels.
   */
  private static final int[] ZZ_ACTION = zzUnpackAction();
  private static final String ZZ_ACTION_PACKED_0 = "\4\0\1\1\1\2\1\3\2\4\1\5\1\6\1\7" +
      "\1\10";
  private static int[] zzUnpackAction() {
    int[] result = new int[13];
    int offset = 0;
    offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
    return result;
  }
  private static int zzUnpackAction(String packed, int offset, int[] result) {
    int i = 0; /* index in packed string */
    int j = offset; /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do
        result[j++] = value;
      while (--count > 0);
    }
    return j;
  }
  /**
   * Translates a state to a row index in the transition table
   */
  private static final int[] ZZ_ROWMAP = zzUnpackRowMap();
  private static final String ZZ_ROWMAP_PACKED_0 = "\0\0\0\5\0\12\0\17\0\24\0\31\0\36\0\43" +
      "\0\50\0\55\0\62\0\67\0\24";
  private static int[] zzUnpackRowMap() {
    int[] result = new int[13];
    int offset = 0;
    offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
    return result;
  }
  private static int zzUnpackRowMap(String packed, int offset, int[] result) {
    int i = 0; /* index in packed string */
    int j = offset; /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int high = packed.charAt(i++) << 16;
      result[j++] = high | packed.charAt(i++);
    }
    return j;
  }
  /**
   * The transition table of the DFA
   */
  private static final int[] ZZ_TRANS = zzUnpackTrans();
  private static final String ZZ_TRANS_PACKED_0 = "\1\5\1\6\1\7\1\10\4\5\2\11\1\5\1\12" +
      "\1\13\2\5\1\14\1\12\3\14\6\0\1\6\5\0" +
      "\1\7\5\0\1\10\1\15\3\0\2\11\1\0\1\12" +
      "\5\0\1\13\2\0\1\14\1\0\3\14";
  private static int[] zzUnpackTrans() {
    int[] result = new int[60];
    int offset = 0;
    offset = zzUnpackTrans(ZZ_TRANS_PACKED_0, offset, result);
    return result;
  }
  private static int zzUnpackTrans(String packed, int offset, int[] result) {
    int i = 0; /* index in packed string */
    int j = offset; /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      value--;
      do
        result[j++] = value;
      while (--count > 0);
    }
    return j;
  }
  /* error codes */
  private static final int ZZ_UNKNOWN_ERROR = 0;
  private static final int ZZ_NO_MATCH = 1;
  private static final int ZZ_PUSHBACK_2BIG = 2;
  /* error messages for the codes above */
  private static final String[] ZZ_ERROR_MSG = {
      "Unknown internal scanner error",
      "Error: could not match input",
      "Error: pushback value was too large"
  };
  /**
   * ZZ_ATTRIBUTE[aState] contains the attributes of state <code>aState</code>
   */
  private static final int[] ZZ_ATTRIBUTE = zzUnpackAttribute();
  private static final String ZZ_ATTRIBUTE_PACKED_0 = "\4\0\1\11\7\1\1\11";
  private static int[] zzUnpackAttribute() {
    int[] result = new int[13];
    int offset = 0;
    offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
    return result;
  }
  private static int zzUnpackAttribute(String packed, int offset, int[] result) {
    int i = 0; /* index in packed string */
    int j = offset; /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do
        result[j++] = value;
      while (--count > 0);
    }
    return j;
  }
  /** the input device */
  private java.io.Reader zzReader;
  /** the current state of the DFA */
  private int zzState;
  /** the current lexical state */
  private int zzLexicalState = YYINITIAL;
  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  private CharSequence zzBuffer = "";
  /** the textposition at the last accepting state */
  private int zzMarkedPos;
  /** the current text position in the buffer */
  private int zzCurrentPos;
  /** startRead marks the beginning of the yytext() string in the buffer */
  private int zzStartRead;
  /** endRead marks the last character in the buffer, that has been read
      from input */
  private int zzEndRead;
  /**
   * zzAtBOL == true <=> the scanner is currently at the beginning of a line
   */
  private boolean zzAtBOL = true;
  /** zzAtEOF == true <=> the scanner is at the EOF */
  private boolean zzAtEOF;
  /** denotes if the user-EOF-code has already been executed */
  private boolean zzEOFDone;
  /**
   * Creates a new scanner
   *
   * @param   in  the java.io.Reader to read input from.
   */
  MacheteLexer(java.io.Reader in) {
    this.zzReader = in;
  }
  /**
   * Unpacks the compressed character translation table.
   *
   * @param packed   the packed character translation table
   * @return         the unpacked character translation table
   */
  private static char[] zzUnpackCMap(String packed) {
    int size = 0;
    for (int i = 0, length = packed.length(); i < length; i += 2) {
      size += packed.charAt(i);
    }
    char[] map = new char[size];
    int i = 0; /* index in packed string */
    int j = 0; /* index in unpacked array */
    while (i < packed.length()) {
      int count = packed.charAt(i++);
      char value = packed.charAt(i++);
      do
        map[j++] = value;
      while (--count > 0);
    }
    return map;
  }
  public final int getTokenStart() {
    return zzStartRead;
  }
  public final int getTokenEnd() {
    return getTokenStart() + yylength();
  }
  public void reset(CharSequence buffer, int start, int end, int initialState) {
    zzBuffer = buffer;
    zzCurrentPos = zzMarkedPos = zzStartRead = start;
    zzAtEOF = false;
    zzAtBOL = true;
    zzEndRead = end;
    yybegin(initialState);
  }
  /**
   * Refills the input buffer.
   *
   * @return      {@code false}, iff there was new input.
   *
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  private boolean zzRefill() throws java.io.IOException {
    return true;
  }
  /**
   * Returns the current lexical state.
   */
  public final int yystate() {
    return zzLexicalState;
  }
  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  public final void yybegin(int newState) {
    zzLexicalState = newState;
  }
  /**
   * Returns the text matched by the current regular expression.
   */
  public final CharSequence yytext() {
    return zzBuffer.subSequence(zzStartRead, zzMarkedPos);
  }
  /**
   * Returns the character at position {@code pos} from the
   * matched text.
   *
   * It is equivalent to yytext().charAt(pos), but faster
   *
   * @param pos the position of the character to fetch.
   *            A value from 0 to yylength()-1.
   *
   * @return the character at position pos
   */
  public final char yycharat(int pos) {
    return zzBuffer.charAt(zzStartRead + pos);
  }
  /**
   * Returns the length of the matched text region.
   */
  public final int yylength() {
    return zzMarkedPos - zzStartRead;
  }
  /**
   * Reports an error that occurred while scanning.
   *
   * In a wellformed scanner (no or only correct usage of
   * yypushback(int) and a match-all fallback rule) this method
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
  private void zzScanError(int errorCode) {
    String message;
    try {
      message = ZZ_ERROR_MSG[errorCode];
    } catch (ArrayIndexOutOfBoundsException e) {
      message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
    }
    throw new Error(message);
  }
  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
  public void yypushback(int number) {
    if (number > yylength())
      zzScanError(ZZ_PUSHBACK_2BIG);
    zzMarkedPos -= number;
  }
  /**
   * Contains user EOF-code, which will be executed exactly once,
   * when the end of file is reached
   */
  private void zzDoEOF() {
    if (!zzEOFDone) {
      zzEOFDone = true;
    }
  }
  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  public IElementType advance() throws java.io.IOException {
    int zzInput;
    int zzAction;
    // cached fields:
    int zzCurrentPosL;
    int zzMarkedPosL;
    int zzEndReadL = zzEndRead;
    CharSequence zzBufferL = zzBuffer;
    int[] zzTransL = ZZ_TRANS;
    int[] zzRowMapL = ZZ_ROWMAP;
    int[] zzAttrL = ZZ_ATTRIBUTE;
    while (true) {
      zzMarkedPosL = zzMarkedPos;
      zzAction = -1;
      zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;
      zzState = ZZ_LEXSTATE[zzLexicalState];
      // set up zzAction for empty match case:
      int zzAttributes = zzAttrL[zzState];
      if ((zzAttributes & 1) == 1) {
        zzAction = zzState;
      }
      zzForAction : {
        while (true) {
          if (zzCurrentPosL < zzEndReadL) {
            zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL/* , zzEndReadL */);
            zzCurrentPosL += Character.charCount(zzInput);
          } else if (zzAtEOF) {
            zzInput = YYEOF;
            break zzForAction;
          } else {
            // store back cached positions
            zzCurrentPos = zzCurrentPosL;
            zzMarkedPos = zzMarkedPosL;
            boolean eof = zzRefill();
            // get translated positions and possibly new buffer
            zzCurrentPosL = zzCurrentPos;
            zzMarkedPosL = zzMarkedPos;
            zzBufferL = zzBuffer;
            zzEndReadL = zzEndRead;
            if (eof) {
              zzInput = YYEOF;
              break zzForAction;
            } else {
              zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL/* , zzEndReadL */);
              zzCurrentPosL += Character.charCount(zzInput);
            }
          }
          int zzNext = zzTransL[zzRowMapL[zzState] + ZZ_CMAP(zzInput)];
          if (zzNext == -1)
            break zzForAction;
          zzState = zzNext;
          zzAttributes = zzAttrL[zzState];
          if ((zzAttributes & 1) == 1) {
            zzAction = zzState;
            zzMarkedPosL = zzCurrentPosL;
            if ((zzAttributes & 8) == 8)
              break zzForAction;
          }
        }
      }
      // store back cached position
      zzMarkedPos = zzMarkedPosL;
      if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
        zzAtEOF = true;
        zzDoEOF();
        return null;
      } else {
        switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
          case 1 : {
            return TokenType.BAD_CHARACTER;
          }
          // fall through
          case 9 :
            break;
          case 2 : {
            return MacheteTypes.EOL;
          }
          // fall through
          case 10 :
            break;
          case 3 : {
            return MacheteTypes.INDENTATION;
          }
          // fall through
          case 11 :
            break;
          case 4 : {
            yybegin(AFTER_NAME);
            return MacheteTypes.NAME;
          }
          // fall through
          case 12 :
            break;
          case 5 : {
            yybegin(YYINITIAL);
            return MacheteTypes.EOL;
          }
          // fall through
          case 13 :
            break;
          case 6 : {
            yybegin(CUSTOM_ANNOTATION);
            return TokenType.WHITE_SPACE;
          }
          // fall through
          case 14 :
            break;
          case 7 : {
            return MacheteTypes.CUSTOM_ANNOTATION;
          }
          // fall through
          case 15 :
            break;
          case 8 : {
            yybegin(AFTER_PREFIX);
            return MacheteTypes.PREFIX;
          }
          // fall through
          case 16 :
            break;
          default :
            zzScanError(ZZ_NO_MATCH);
        }
      }
    }
  }
}
