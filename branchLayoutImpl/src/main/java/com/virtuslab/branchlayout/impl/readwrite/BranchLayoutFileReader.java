package com.virtuslab.branchlayout.impl.readwrite;

import java.nio.file.Files;
import java.nio.file.Path;

import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.List;
import lombok.CustomLog;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayoutEntry;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.branchlayout.impl.BranchLayoutEntry;

@CustomLog
public class BranchLayoutFileReader implements IBranchLayoutReader {

  @Override
  public BranchLayout read(Path path) throws BranchLayoutException {
    boolean isBranchLayoutPresent = Files.isRegularFile(path);
    IndentSpec indentSpec = isBranchLayoutPresent
        ? BranchLayoutFileUtils.deriveIndentSpec(path)
        : BranchLayoutFileUtils.getDefaultSpec();

    LOG.debug(() -> "Entering: Reading branch layout from ${path} with indent character ASCII " +
        "code = ${(int)indentSpec.getIndentCharacter()} and indent width = ${indentSpec.getIndentWidth()}");

    List<String> lines = BranchLayoutFileUtils.readFileLines(path);
    List<String> linesWithoutBlank = lines.reject(String::isBlank);

    LOG.debug(() -> "${lines.length()} line(s) found");

    List<IBranchLayoutEntry> roots = List.empty();
    if (!linesWithoutBlank.isEmpty()) {
      Array<Tuple2<Integer, Integer>> lineIndexToIndentLevelAndUpstreamLineIndex = parseToArrayRepresentation(path, indentSpec,
          lines);
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
  private List<IBranchLayoutEntry> buildEntriesStructure(
      List<String> lines,
      Array<Tuple2<Integer, Integer>> lineIndexToUpstreamLineIndex,
      @GTENegativeOne int upstreamLineIndex) {

    return lineIndexToUpstreamLineIndex
        .zipWithIndex()
        .filter(t -> t._1()._2() == upstreamLineIndex)
        .map(t -> createEntry(lines.get(t._2()),
            buildEntriesStructure(lines, lineIndexToUpstreamLineIndex, t._2())))
        .toList();
  }

  /**
   * Parses line to {@link BranchLayoutEntry#BranchLayoutEntry} arguments and creates an
   * entry with the specified {@code subentries}.
   */
  private IBranchLayoutEntry createEntry(String line, List<IBranchLayoutEntry> subentries) {
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

    var result = new BranchLayoutEntry(branchName, customAnnotation, subentries);
    LOG.debug(() -> "Created ${result}");
    return result;
  }

  /**
   * @return an array containing the indent level and upstream entry describing line index which indices correspond to
   *         provided {@code lines} indices. It may be understood as a helper metadata needed to build entries structure
   */
  private Array<Tuple2<Integer, Integer>> parseToArrayRepresentation(Path path, IndentSpec indentSpec, List<String> lines)
      throws BranchLayoutException {

    List<String> linesWithoutBlank = lines.reject(String::isBlank);

    if (linesWithoutBlank.nonEmpty() && BranchLayoutFileUtils.getIndentWidth(linesWithoutBlank.head(),
        indentSpec.getIndentCharacter()) > 0) {
      int firstNonEmptyLineIndex = lines.indexOf(linesWithoutBlank.head());
      assert firstNonEmptyLineIndex >= 0 : "Non-empty line not found";
      throw new BranchLayoutException(firstNonEmptyLineIndex + 1,
          "The initial line of branch layout file (${path.toAbsolutePath()}) must not be indented");
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
        LOG.debug("Line no ${realLineNumber + 1} is blank. Skipping");
        continue;
      }

      if (!BranchLayoutFileUtils.hasProperIndentationCharacter(line, indentSpec.getIndentCharacter())) {
        LOG.error("Line no ${realLineNumber + 1} has unexpected indentation character inconsistent with previous one");
        throw new BranchLayoutException(realLineNumber + 1,
            "Line no ${realLineNumber + 1} in branch layout file (${path.toAbsolutePath()}) has unexpected indentation "
                + "character inconsistent with previous one");
      }

      int lineIndentWidth = BranchLayoutFileUtils.getIndentWidth(line, indentSpec.getIndentCharacter());
      int level = getIndentLevel(path, indentSpec, lineIndentWidth, realLineNumber);

      if (level - previousLevel > 1) {
        throw new BranchLayoutException(realLineNumber + 1,
            "One of branches in branch layout file (${path.toAbsolutePath()}) has incorrect level in relation to its parent branch");
      }

      @SuppressWarnings("index:argument.type.incompatible") Integer upstreamLineIndex = level <= 0
          ? -1
          : levelToPresentUpstream.get(level - 1);
      Tuple2<Integer, Integer> levelAndUpstreamLineIndex = new Tuple2<>(level, upstreamLineIndex);

      lineIndexToIndentLevelAndUpstreamLineIndex = lineIndexToIndentLevelAndUpstreamLineIndex.update(lineIndex,
          levelAndUpstreamLineIndex);
      levelToPresentUpstream = levelToPresentUpstream.update(level, lineIndex);

      previousLevel = level;
      lineIndex++;
    }

    return lineIndexToIndentLevelAndUpstreamLineIndex;
  }

  private @NonNegative int getIndentLevel(Path path, IndentSpec indentSpec, @NonNegative int indent,
      @NonNegative int lineNumber)
      throws BranchLayoutException {
    if (indent == 0) {
      return 0;
    }

    if (indent % indentSpec.getIndentWidth() != 0) {
      throw new BranchLayoutException(lineNumber + 1,
          "Levels of indentation are not matching in branch layout file (${path.toAbsolutePath()})");
    }

    return indent / indentSpec.getIndentWidth();
  }
}
