package com.virtuslab.branchlayout.impl.manager;

import java.nio.file.Path;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Try;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.initialization.qual.UnderInitialization;

import com.virtuslab.branchlayout.api.manager.IBranchLayoutManager;
import com.virtuslab.branchlayout.api.manager.IBranchLayoutReader;
import com.virtuslab.branchlayout.api.manager.IBranchLayoutWriter;
import com.virtuslab.branchlayout.api.manager.IIndentDefining;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

public class BranchLayoutManager implements IBranchLayoutManager {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("branchLayout");

  private final IBranchLayoutReader reader;
  private final IBranchLayoutWriter writer;

  public BranchLayoutManager(Path path, boolean isBranchLayoutPresent) {
    char indentCharacter = BranchLayoutFileUtils.DEFAULT_INDENT_CHARACTER;
    int indentWidth = BranchLayoutFileUtils.DEFAULT_INDENT_WIDTH;
    if (isBranchLayoutPresent) {
      IndentDerivator indentDerivator = new IndentDerivator(path);
      indentCharacter = indentDerivator.getIndentCharacter();
      indentWidth = indentDerivator.getIndentWidth();
    }
    this.reader = new BranchLayoutFileReader(path, indentCharacter, indentWidth);
    this.writer = new BranchLayoutFileWriter(path, indentCharacter, indentWidth);
  }

  @Override
  public IBranchLayoutReader getReader() {
    return reader;
  }

  @Override
  public IBranchLayoutWriter getWriter() {
    return writer;
  }

  private static class IndentDerivator implements IIndentDefining {

    private final char indentCharacter;
    @Positive
    private final int indentWidth;

    IndentDerivator(Path path) {
      LOG.debug("Entering");
      LOG.debug(() -> "Branch layout file path: ${path}");

      List<String> lines = Try.of(() -> BranchLayoutFileUtils.getFileLines(path))
          .getOrElse(() -> {
            LOG.debug(() -> "Failed to read branch layout file from ${path}. Falling to default indent definition.");
            return List.empty();
          });

      LOG.debug(() -> "${lines.length()} line(s) found");

      Tuple2<Character, @Positive Integer> charAndWidth = deriveIndentCharacter(lines);
      indentCharacter = charAndWidth._1();
      indentWidth = charAndWidth._2();

      LOG.debug(() -> "Indent character is ${indentCharacter == '\\t' ? \"TAB\" : indentCharacter == ' ' " +
          "? \"SPACE\" : \"'\" + indentCharacter + \"'\"}");
      LOG.debug(() -> "Indent width is ${indentWidth}");
    }

    /**
     * Searches for first line starting with an indent character
     *
     * @return a tuple of {@code indentCharacter} and {@code indentWidth}
     */
    private Tuple2<Character, @Positive Integer> deriveIndentCharacter(@UnderInitialization IndentDerivator this,
        List<String> lines) {
      var firstLineWithBlankPrefixOption = lines.reject(String::isBlank)
          .find(line -> line.startsWith(" ") || line.startsWith("\t"));
      // Redundant non-emptiness check to satisfy IndexChecker
      char indentCharacterTmp = BranchLayoutFileUtils.DEFAULT_INDENT_CHARACTER;
      int indentWidthTmp = BranchLayoutFileUtils.DEFAULT_INDENT_WIDTH;
      if (firstLineWithBlankPrefixOption.isDefined() && !firstLineWithBlankPrefixOption.get().isEmpty()) {
        indentCharacterTmp = firstLineWithBlankPrefixOption.get().charAt(0);
        indentWidthTmp = BranchLayoutFileUtils.getIndentIndentWidth(firstLineWithBlankPrefixOption.get(), indentCharacterTmp);
        // we are processing a line satisfying `line.startsWith(" ") || line.startsWith("\t")`
        assert indentWidthTmp > 0 : "indent width is 0";
      }
      return Tuple.<Character, @Positive Integer>of(indentCharacterTmp, indentWidthTmp);
    }

    @Override
    public char getIndentCharacter() {
      return indentCharacter;
    }

    @Override
    public @Positive int getIndentWidth() {
      return indentWidth;
    }
  }
}
