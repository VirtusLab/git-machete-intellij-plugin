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

@RequiredArgsConstructor
@Getter
public class BranchLayoutFileParser {
  private final Path path;
  private Character indentCharacter = ' ';
  @NonNegative
  private int levelWidth = 0;

  public BranchLayout parse() throws BranchLayoutException {
    List<BaseBranchLayoutEntry> roots = List.empty();
    List<String> lines = getFileLines().reject(String::isBlank);

    deriveIndentCharacter(lines);

    if (!lines.isEmpty()) {
      Array<Tuple2<Integer, Integer>> lineIndexToIndentLevelAndUpstreamLineIndex = parseToArrayRepresentation(lines);
      roots = buildEntriesStructure(lines, lineIndexToIndentLevelAndUpstreamLineIndex, /* upstreamLineIndex */ -1);
    }

    return new BranchLayout(roots);
  }

  /**
   * Searches for first line starting with an indent character and based on that sets {@code indentCharacter} and
   * {@code levelWidth} fields.
   */
  private void deriveIndentCharacter(List<String> lines) {
    var firstLineWithBlankPrefixOptional = lines.find(line -> line.startsWith(" ") || line.startsWith("\t"))
        .toJavaOptional();
    // Redundant non-emptiness check to satisfy IndexChecker
    if (firstLineWithBlankPrefixOptional.isPresent() && !firstLineWithBlankPrefixOptional.get().isEmpty()) {
      indentCharacter = firstLineWithBlankPrefixOptional.get().charAt(0);
      levelWidth = getIndentLevelWidth(firstLineWithBlankPrefixOptional.get());
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
    return lineIndexToUpstreamLineIndex
        .zipWithIndex()
        .filter(t -> t._1()._2() == upstreamLineIndex)
        .map(t -> createEntry(lines.get(t._2()), buildEntriesStructure(lines, lineIndexToUpstreamLineIndex, t._2())))
        .collect(List.collector());
  }

  /**
   * Parses line to {@link com.virtuslab.branchlayout.impl.BranchLayoutEntry#BranchLayoutEntry} arguments and creates an
   * entry with the specified {@code subbranches}.
   */
  private BaseBranchLayoutEntry createEntry(String line, List<BaseBranchLayoutEntry> subbranches) {
    String trimmedLine = line.trim();
    String branchName = trimmedLine;
    String customAnnotation = null;
    int indexOfSpace = trimmedLine.indexOf(' ');
    if (indexOfSpace > -1) {
      branchName = trimmedLine.substring(0, indexOfSpace);
      customAnnotation = trimmedLine.substring(indexOfSpace + 1).trim();
    }
    return new BranchLayoutEntry(branchName, customAnnotation, subbranches);
  }

  /**
   * @return an array containing the indent level and upstream entry describing line index which indices correspond to
   *         provided {@code lines} indices. It may be understood as a helper metadata needed to build entries structure
   */
  private Array<Tuple2<Integer, Integer>> parseToArrayRepresentation(List<String> lines) throws BranchLayoutException {
    if (lines.size() > 0 && getIndentLevelWidth(lines.head()) > 0) {
      throw new BranchLayoutException(/* errorLine */ 1,
          "The initial line of branch layout file (${path.toAbsolutePath()}) may not be indented");
    }

    Array<Tuple2<Integer, Integer>> lineIndexToIndentLevelAndUpstreamLineIndex = Array.fill(lines.size(),
        new Tuple2<>(-1, -1));
    Array<Integer> levelToPresentUpstream = Array.fill(lines.size(), -1);

    int previousLevel = 0;
    for (int lineNumber = 0; lineNumber < lines.length(); ++lineNumber) {
      String line = lines.get(lineNumber);
      int lineIndentLevelWidth = getIndentLevelWidth(line);
      int level = getIndentLevel(lineIndentLevelWidth, lineNumber);

      if (level - previousLevel > 1) {
        throw new BranchLayoutException(lineNumber + 1,
            "One of branches in branch layout file (${path.toAbsolutePath()}) has incorrect level in relation to its parent branch");
      }

      @SuppressWarnings("index:argument.type.incompatible")
      Tuple2<Integer, Integer> levelAndUpstreamLineIndex = new Tuple2<>(level,
          level <= 0 ? -1 : levelToPresentUpstream.get(level - 1));
      lineIndexToIndentLevelAndUpstreamLineIndex = lineIndexToIndentLevelAndUpstreamLineIndex.update(lineNumber,
          levelAndUpstreamLineIndex);
      levelToPresentUpstream = levelToPresentUpstream.update(level, lineNumber);

      previousLevel = level;
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
