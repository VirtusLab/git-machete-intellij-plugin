package com.virtuslab.branchlayout.impl.readwrite;

import static com.virtuslab.branchlayout.impl.IndentSpec.SPACE_INDENT_CHARACTER;
import static com.virtuslab.branchlayout.impl.IndentSpec.TAB_INDENT_CHARACTER;

import java.nio.file.Files;
import java.nio.file.Path;

import io.vavr.collection.List;
import io.vavr.control.Try;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.impl.IndentSpec;

public final class BranchLayoutFileUtils {
  private BranchLayoutFileUtils() {}

  public static final @Positive int DEFAULT_INDENT_WIDTH = 2;
  public static final char DEFAULT_INDENT_CHARACTER = SPACE_INDENT_CHARACTER;
  private static final IndentSpec DEFAULT_SPEC = new IndentSpec(DEFAULT_INDENT_CHARACTER, DEFAULT_INDENT_WIDTH);

  // Extracted to a method so that it can be mocked in the tests.
  public static IndentSpec getDefaultSpec() {
    return DEFAULT_SPEC;
  }

  public static @NonNegative int getIndentWidth(String line, char indentCharacter) {
    return (int) line.chars().takeWhile(c -> c == indentCharacter).count();
  }

  public static List<String> readFileLines(Path path) throws BranchLayoutException {
    return Try.of(() -> List.ofAll(Files.readAllLines(path))).getOrElseThrow(
        e -> new BranchLayoutException("Error while loading branch layout file (${path.toAbsolutePath()})", e));
  }

  public static boolean hasProperIndentationCharacter(String line, char expectedIndentationCharacter) {
    char unexpectedIndentationCharacter = expectedIndentationCharacter == SPACE_INDENT_CHARACTER
        ? TAB_INDENT_CHARACTER : SPACE_INDENT_CHARACTER;
    for (int i = 0; i < line.length(); i++) {
      char currentCharacter = line.charAt(i);
      if (currentCharacter != expectedIndentationCharacter) {
        return currentCharacter != unexpectedIndentationCharacter;
      }
    }
    return true;
  }

}
