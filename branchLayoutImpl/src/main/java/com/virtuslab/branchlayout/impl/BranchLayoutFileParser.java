package com.virtuslab.branchlayout.impl;

import java.nio.file.Files;
import java.nio.file.Path;

import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.branchlayout.api.BaseBranchLayoutEntry;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayoutParser;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

@RequiredArgsConstructor
@Getter
public class BranchLayoutFileParser implements IBranchLayoutParser {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("branchLayout");

  private final Path path;
  private Character indentCharacter = ' ';
  @NonNegative
  private int levelWidth = 0;

  public BranchLayout parse() throws BranchLayoutException {
    LOG.debug("Entering");
    LOG.debug(() -> "Branch layout file path: ${path}");

    List<BaseBranchLayoutEntry> roots = List.empty();
    List<String> lines = getFileLines();
    List<String> linesWithoutBlank = lines.reject(String::isBlank);

    LOG.debug(() -> "${lines.length()} line(s) fund");

    deriveIndentCharacter(lines);

    LOG.debug(() -> "Indent character is ${indentCharacter.equals('\\t') ? \"TAB\" : indentCharacter.equals(' ') " +
        "? \"SPACE\" : \"'\" + indentCharacter + \"'\"}");
    LOG.debug(() -> "Indent level width is ${levelWidth}");

    if (!linesWithoutBlank.isEmpty()) {
      Array<Tuple2<Integer, Integer>> lineIndexToIndentLevelAndUpstreamLineIndex = parseToArrayRepresentation(lines);
      LOG.debug(() -> "lineIndexToIndentLevelAndUpstreamLineIndex = ${lineIndexToIndentLevelAndUpstreamLineIndex}");

      roots = buildEntriesStructure(linesWithoutBlank,
          lineIndexToIndentLevelAndUpstreamLineIndex, /* upstreamLineIndex */ -1);
    } else {
      LOG.debug("Branch layout file is empty");
    }

    return new BranchLayout(roots);
  }

  /**
   * Searches for first line starting with an indent character and based on that sets {@code indentCharacter} and
   * {@code levelWidth} fields.
   */
  private void deriveIndentCharacter(List<String> lines) {
    var firstLineWithBlankPrefixOption = lines.reject(String::isBlank)
        .find(line -> line.startsWith(" ") || line.startsWith("\t"));
    // Redundant non-emptiness check to satisfy IndexChecker
    if (firstLineWithBlankPrefixOption.isDefined() && !firstLineWithBlankPrefixOption.get().isEmpty()) {
      indentCharacter = firstLineWithBlankPrefixOption.get().charAt(0);
      levelWidth = getIndentLevelWidth(firstLineWithBlankPrefixOption.get());
    }
  }

  /**
   * @param lines
   *          list of lines read from branch layout file
   * @param lineIndexToUpstreamLineIndex
   *          as it says ({@code lines} metadata containing structure, see {@link #parseToArrayRepresentation})
   * @param upstreamLineIndex
   *          index of line whose subentries are to be built
   *
   * @return list of entries with recursively built lists of subentries
   */
  @SuppressWarnings("index:argument.type.incompatible")
  private List<BaseBranchLayoutEntry> buildEntriesStructure(List<String> lines,
      Array<Tuple2<Integer, Integer>> lineIndexToUpstreamLineIndex,
      @GTENegativeOne int upstreamLineIndex) {
    LOG.debug(() -> "Entering: lines = ${lines}, lineIndexToUpstreamLineIndex = ${lineIndexToUpstreamLineIndex}, " +
        "upstreamLineIndex = ${upstreamLineIndex}");
    return lineIndexToUpstreamLineIndex
        .zipWithIndex()
        .filter(t -> t._1()._2() == upstreamLineIndex)
        .map(t -> createEntry(lines.get(t._2()),
            buildEntriesStructure(lines, lineIndexToUpstreamLineIndex, t._2())))
        .collect(List.collector());
  }

  /**
   * Parses line to {@link com.virtuslab.branchlayout.impl.BranchLayoutEntry#BranchLayoutEntry} arguments and creates an
   * entry with the specified {@code subentries}.
   */
  private BaseBranchLayoutEntry createEntry(String line, List<BaseBranchLayoutEntry> subentries) {
    LOG.debug(() -> "Entering: line = '${line}', subentries = ${subentries}");

    String trimmedLine = line.trim();
    String branchName;
    String customAnnotation;
    int indexOfSpace = trimmedLine.indexOf(' ');
    if (indexOfSpace > -1) {
      branchName = trimmedLine.substring(0, indexOfSpace);
      customAnnotation = trimmedLine.substring(indexOfSpace + 1).trim();
    } else {
      branchName = trimmedLine;
      customAnnotation = null;
    }

    LOG.debug(() -> "Creating BranchLayoutEntry(branchName = '${branchName}', " +
        "customAnnotation = ${customAnnotation != null ? \"'\" + customAnnotation + \"'\" : null}, " +
        "subentries.length() = ${subentries.length()})");

    return new BranchLayoutEntry(branchName, customAnnotation, subentries);
  }

  /**
   * @return an array containing the indent level and upstream entry describing line index which indices correspond to
   *         provided {@code lines} indices. It may be understood as a helper metadata needed to build entries structure
   */
  private Array<Tuple2<Integer, Integer>> parseToArrayRepresentation(List<String> lines) throws BranchLayoutException {
    LOG.debug("Entering");

    List<String> linesWithoutBlank = lines.reject(String::isBlank);

    if (linesWithoutBlank.size() > 0 && getIndentLevelWidth(linesWithoutBlank.head()) > 0) {
      throw new BranchLayoutException(
          "The initial line of branch layout file (${path.toAbsolutePath()}) cannot be indented");
    }

    Array<Tuple2<Integer, Integer>> lineIndexToIndentLevelAndUpstreamLineIndex = Array.fill(linesWithoutBlank.size(),
        new Tuple2<>(-1, -1));
    Array<Integer> levelToPresentUpstream = Array.fill(linesWithoutBlank.size(), -1);

    int previousLevel = 0;
    int lineIndex = 0;
    for (int realLineNumber = 0; realLineNumber < lines.length(); ++realLineNumber) {
      String line = lines.get(realLineNumber);
      if (line.isBlank()) {
        // Can't use lambda because `realLineNumber` is not effectively final
        LOG.debug("Line no ${realLineNumber} is blank. Skipping");
        continue;
      }

      int lineIndentLevelWidth = getIndentLevelWidth(line);
      int level = getIndentLevel(lineIndentLevelWidth, realLineNumber);

      if (level - previousLevel > 1) {
        throw new BranchLayoutException(realLineNumber + 1,
            "One of branches in branch layout file (${path.toAbsolutePath()}) has incorrect level in relation to its parent branch");
      }

      @SuppressWarnings("index:argument.type.incompatible")
      Integer upstreamLineIndex = level <= 0 ? -1 : levelToPresentUpstream.get(level - 1);
      Tuple2<Integer, Integer> levelAndUpstreamLineIndex = new Tuple2<>(level, upstreamLineIndex);

      // Can't use lambda because `realLineNumber` and `lineIndex` are not effectively final
      LOG.debug("For line ${realLineNumber}: lineIndex = ${lineIndex}, level = ${level}, " +
          "upstreamLineIndex = ${upstreamLineIndex}");

      lineIndexToIndentLevelAndUpstreamLineIndex = lineIndexToIndentLevelAndUpstreamLineIndex.update(lineIndex,
          levelAndUpstreamLineIndex);
      levelToPresentUpstream = levelToPresentUpstream.update(level, lineIndex);

      previousLevel = level;
      lineIndex++;
    }

    return lineIndexToIndentLevelAndUpstreamLineIndex;
  }

  private List<String> getFileLines() throws BranchLayoutException {
    return Try.of(() -> List.ofAll(Files.readAllLines(path)))
        .getOrElseThrow(
            e -> new BranchLayoutException("Error while loading branch layout file (${path.toAbsolutePath()})", e));
  }

  @NonNegative
  private int getIndentLevelWidth(String line) {
    return (int) line.chars().takeWhile(c -> c == indentCharacter).count();
  }

  @NonNegative
  private int getIndentLevel(@NonNegative int indent, @NonNegative int lineNumber) throws BranchLayoutException {
    if (indent == 0) {
      return 0;
    }

    if (indent % levelWidth != 0) {
      throw new BranchLayoutException(lineNumber + 1,
          "Levels of indentation are not matching in branch layout file (${path.toAbsolutePath()})");
    }

    return indent / levelWidth;
  }
}
