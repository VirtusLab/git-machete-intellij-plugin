package com.virtuslab.branchlayout.impl.manager;

import java.nio.file.Files;
import java.nio.file.Path;

import io.vavr.collection.List;
import io.vavr.control.Try;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

import com.virtuslab.branchlayout.api.BranchLayoutException;

public final class BranchLayoutFileUtils {
  private BranchLayoutFileUtils() {}

  public static final char DEFAULT_INDENT_CHARACTER = ' ';
  @Positive
  public static final int DEFAULT_INDENT_WIDTH = 2;

  @NonNegative
  public static int getIndentIndentWidth(String line, char indentCharacter) {
    return (int) line.chars().takeWhile(c -> c == indentCharacter).count();
  }

  public static List<String> getFileLines(Path path) throws BranchLayoutException {
    return Try.of(() -> List.ofAll(Files.readAllLines(path))).getOrElseThrow(
        e -> new BranchLayoutException("Error while loading branch layout file (${path.toAbsolutePath()})", e));
  }

}
