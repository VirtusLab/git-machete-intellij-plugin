package com.virtuslab.branchlayout.impl.manager;

import java.nio.file.Path;

import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

import com.virtuslab.branchlayout.api.BaseBranchLayoutEntry;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.manager.IBranchLayoutReader;
import com.virtuslab.branchlayout.impl.BranchLayout;
import com.virtuslab.branchlayout.impl.BranchLayoutEntry;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

@RequiredArgsConstructor
@Getter
public class BranchLayoutFileReader implements IBranchLayoutReader {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("branchLayout");

  private final Path path;

  private final char indentCharacter;
  @Positive
  private final int indentWidth;

  public BranchLayout read() throws BranchLayoutException {
    LOG.debug("Entering");
    LOG.debug(() -> "Reading branch layout from ${path} with indent character ASCII code = ${(int)indentCharacter} " +
        "and indent width = ${indentWidth}:");

    List<String> lines = BranchLayoutFileUtils.readFileLines(path);
    List<String> linesWithoutBlank = lines.reject(String::isBlank);

    LOG.debug(() -> "${lines.length()} line(s) found");

    List<BaseBranchLayoutEntry> roots = List.empty();
    if (!linesWithoutBlank.isEmpty()) {
      Array<Tuple2<Integer, Integer>> lineIndexToIndentLevelAndUpstreamLineIndex = parseToArrayRepresentation(lines);
      LOG.debug(() -> "lineIndexToIndentLevelAndUpstreamLineIndex = ${lineIndexToIndentLevelAndUpstreamLineIndex}");

      roots = buildEntriesStructure(linesWithoutBlank, lineIndexToIndentLevelAndUpstreamLineIndex, /* upstreamLineIndex */ -1);
    } else {
      LOG.debug("Branch layout file is empty");
    }

    return new BranchLayout(roots);
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
   * Parses line to {@link BranchLayoutEntry#BranchLayoutEntry} arguments and creates an
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

    if (linesWithoutBlank.size() > 0 && BranchLayoutFileUtils.getIndentWidth(linesWithoutBlank.head(), indentCharacter) > 0) {
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

      int lineIndentWidth = BranchLayoutFileUtils.getIndentIndentWidth(line, indentCharacter);
      int level = getIndentLevel(lineIndentWidth, realLineNumber);

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

  @NonNegative
  private int getIndentLevel(@NonNegative int indent, @NonNegative int lineNumber) throws BranchLayoutException {
    if (indent == 0) {
      return 0;
    }

    if (indent % indentWidth != 0) {
      throw new BranchLayoutException(lineNumber + 1,
          "Levels of indentation are not matching in branch layout file (${path.toAbsolutePath()})");
    }

    return indent / indentWidth;
  }
}
