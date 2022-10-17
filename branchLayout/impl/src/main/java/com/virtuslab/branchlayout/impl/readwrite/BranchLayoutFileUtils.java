package com.virtuslab.branchlayout.impl.readwrite;

import static com.virtuslab.branchlayout.impl.readwrite.IndentSpec.SPACE;
import static com.virtuslab.branchlayout.impl.readwrite.IndentSpec.TAB;

import java.nio.file.Files;
import java.nio.file.Path;

import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Try;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
public final class BranchLayoutFileUtils {
  private BranchLayoutFileUtils() {}

  public static final @Positive int DEFAULT_INDENT_WIDTH = 2;
  public static final char DEFAULT_INDENT_CHARACTER = SPACE;
  private static final IndentSpec DEFAULT_SPEC = new IndentSpec(DEFAULT_INDENT_CHARACTER, DEFAULT_INDENT_WIDTH);

  // Extracted to a method so that it can be mocked in the tests.
  public static IndentSpec getDefaultSpec() {
    return DEFAULT_SPEC;
  }

  public static @NonNegative int getIndentWidth(String line, char indentCharacter) {
    return Stream.ofAll(line.chars().boxed()).takeWhile(c -> c == indentCharacter).size();
  }

  @UIThreadUnsafe
  public static IndentSpec deriveIndentSpec(Path path) {
    LOG.debug("Entering: branch layout file path: ${path}");
    List<String> lines = Try.of(() -> readFileLines(path))
        .getOrElse(() -> {
          LOG.debug(() -> "Failed to read branch layout file from ${path}. Falling back to default indent definition.");
          return List.empty();
        });
    return deriveIndentSpec(lines);
  }

  public static IndentSpec deriveIndentSpec(List<String> lines) {
    LOG.debug(() -> "${lines.length()} line(s) found");

    val firstLineWithBlankPrefixOption = lines.reject(String::isBlank)
        .find(line -> line.startsWith(String.valueOf(SPACE))
            || line.startsWith(String.valueOf(TAB)));
    char indentCharacter = BranchLayoutFileUtils.DEFAULT_INDENT_CHARACTER;
    int indentWidth = BranchLayoutFileUtils.DEFAULT_INDENT_WIDTH;

    // Redundant non-emptiness check to satisfy IndexChecker
    if (firstLineWithBlankPrefixOption.isDefined() && !firstLineWithBlankPrefixOption.get().isEmpty()) {
      indentCharacter = firstLineWithBlankPrefixOption.get().charAt(0);
      indentWidth = getIndentWidth(firstLineWithBlankPrefixOption.get(), indentCharacter);
      // we are processing a line satisfying `line.startsWith(" ") || line.startsWith("\t")`
      assert indentWidth > 0 : "indent width is ${indentWidth} <= 0";
    }
    IndentSpec indentSpec = new IndentSpec(indentCharacter, indentWidth);

    LOG.debug(() -> "Indent character is ${indentSpec.getIndentCharacter() == '\\t' ? \"TAB\" :" +
        " indentSpec.getIndentCharacter() == ' ' ? \"SPACE\" : \"'\" + indentSpec.getIndentCharacter() + \"'\"}");
    LOG.debug(() -> "Indent width is ${indentSpec.getIndentWidth()}");

    return indentSpec;
  }

  @UIThreadUnsafe
  public static List<String> readFileLines(Path path) throws BranchLayoutException {
    return Try.of(() -> List.ofAll(Files.readAllLines(path))).getOrElseThrow(
        e -> new BranchLayoutException("Error while loading branch layout file (${path.toAbsolutePath()})", e));
  }

  public static boolean hasProperIndentationCharacter(String line, char expectedIndentationCharacter) {
    char unexpectedIndentationCharacter = expectedIndentationCharacter == SPACE
        ? TAB
        : SPACE;
    return Stream.ofAll(line.toCharArray())
        .takeWhile(c -> c != expectedIndentationCharacter)
        .headOption()
        .map(c -> c != unexpectedIndentationCharacter)
        .getOrElse(true);
  }
}
