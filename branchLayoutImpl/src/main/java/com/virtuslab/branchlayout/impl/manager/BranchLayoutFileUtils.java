package com.virtuslab.branchlayout.impl.manager;

import java.nio.file.Files;
import java.nio.file.Path;

import io.vavr.collection.List;
import io.vavr.control.Try;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.branchlayout.api.BranchLayoutException;

public final class BranchLayoutFileUtils {
  private BranchLayoutFileUtils() {}

  @NonNegative
  public static int getIndentWidth(String line, char indentCharacter) {
    return (int) line.chars().takeWhile(c -> c == indentCharacter).count();
  }

  public static List<String> readFileLines(Path path) throws BranchLayoutException {
    return Try.of(() -> List.ofAll(Files.readAllLines(path))).getOrElseThrow(
        e -> new BranchLayoutException("Error while loading branch layout file (${path.toAbsolutePath()})", e));
  }

}
