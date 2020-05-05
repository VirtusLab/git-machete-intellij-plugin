package com.virtuslab.branchlayout.impl.readwrite;

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

  @Positive
  public static final int DEFAULT_INDENT_WIDTH = 2;
  public static final char DEFAULT_INDENT_CHARACTER = ' ';
  private static final IndentSpec DEFAULT_SPEC = new IndentSpec(DEFAULT_INDENT_CHARACTER, DEFAULT_INDENT_WIDTH);

  // Extracted to a method so that it can be mocked in the tests.
  public static IndentSpec getDefaultSpec() {
    return DEFAULT_SPEC;
  }

  @NonNegative
  public static int getIndentWidth(String line, char indentCharacter) {
    return (int) line.chars().takeWhile(c -> c == indentCharacter).count();
  }

  public static List<String> readFileLines(Path path) throws BranchLayoutException {
    return Try.of(() -> List.ofAll(Files.readAllLines(path))).getOrElseThrow(
        e -> new BranchLayoutException("Error while loading branch layout file (${path.toAbsolutePath()})", e));
  }

}
