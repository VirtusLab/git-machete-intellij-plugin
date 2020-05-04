package com.virtuslab.branchlayout.impl.manager;

import java.nio.file.Files;
import java.nio.file.Path;

import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.Getter;

import com.virtuslab.branchlayout.api.manager.IBranchLayoutManager;
import com.virtuslab.branchlayout.api.manager.IBranchLayoutReader;
import com.virtuslab.branchlayout.api.manager.IBranchLayoutWriter;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

public class BranchLayoutManager implements IBranchLayoutManager {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("branchLayout");

  @Getter
  private final IBranchLayoutReader reader;
  @Getter
  private final IBranchLayoutWriter writer;

  public BranchLayoutManager(Path path) {
    boolean isBranchLayoutPresent = Files.isRegularFile(path);
    IndentSpec indentSpec = isBranchLayoutPresent ? deriveIndentSpec(path) : IndentSpec.DEFAULT_SPEC;
    this.reader = new BranchLayoutFileReader(path, indentSpec);
    this.writer = new BranchLayoutFileWriter(path, indentSpec);
  }

  private static IndentSpec deriveIndentSpec(Path path) {
    LOG.debug("Entering: branch layout file path: ${path}");

    List<String> lines = Try.of(() -> BranchLayoutFileUtils.readFileLines(path))
        .getOrElse(() -> {
          LOG.debug(() -> "Failed to read branch layout file from ${path}. Falling back to default indent definition.");
          return List.empty();
        });

    LOG.debug(() -> "${lines.length()} line(s) found");

    var firstLineWithBlankPrefixOption = lines.reject(String::isBlank)
        .find(line -> line.startsWith(" ") || line.startsWith("\t"));
    char indentCharacter = IndentSpec.DEFAULT_INDENT_CHARACTER;
    int indentWidth = IndentSpec.DEFAULT_INDENT_WIDTH;

    // Redundant non-emptiness check to satisfy IndexChecker
    if (firstLineWithBlankPrefixOption.isDefined() && !firstLineWithBlankPrefixOption.get().isEmpty()) {
      indentCharacter = firstLineWithBlankPrefixOption.get().charAt(0);
      indentWidth = BranchLayoutFileUtils.getIndentWidth(firstLineWithBlankPrefixOption.get(), indentCharacter);
      // we are processing a line satisfying `line.startsWith(" ") || line.startsWith("\t")`
      assert indentWidth > 0 : "indent width is ${indentWidth} <= 0";
    }
    IndentSpec indentSpec = new IndentSpec(indentCharacter, indentWidth);

    LOG.debug(() -> "Indent character is ${indentSpec.getIndentCharacter() == '\\t' ? \"TAB\" :" +
        " indentSpec.getIndentCharacter() == ' ' ? \"SPACE\" : \"'\" + indentSpec.getIndentCharacter() + \"'\"}");
    LOG.debug(() -> "Indent width is ${indentSpec.getIndentWidth()}");

    return indentSpec;
  }
}
