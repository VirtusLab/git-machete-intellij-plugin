package com.virtuslab.branchlayout.impl;

import java.nio.file.Files;
import java.nio.file.Path;

import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.virtuslab.branchlayout.api.BaseBranchLayoutEntry;
import com.virtuslab.branchlayout.api.BranchLayoutException;

@RequiredArgsConstructor
@Getter
public class BranchLayoutFileParser {
  private final Path path;
  private Character indentCharacter = ' ';
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
   * Searches for first line preceded some indent character and based on that sets {@code indentCharacter} and
   * {@code levelWidth} fields.
   */
  @SuppressWarnings("argument.type.incompatible") // for IndexChecker only
  private void deriveIndentCharacter(List<String> lines) {
    var firstLineWithBlankPrefixOption = lines.find(line -> line.startsWith(" ") || line.startsWith("\t"));
    if (firstLineWithBlankPrefixOption.isDefined()) {
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
   *          index of line which subentry structures are to be built
   *
   * @return list of entries with recursively built lists of subentries
   */
  private List<BaseBranchLayoutEntry> buildEntriesStructure(List<String> lines,
      Array<Tuple2<Integer, Integer>> lineIndexToUpstreamLineIndex, int upstreamLineIndex) {
    return lineIndexToUpstreamLineIndex
        .zipWithIndex()
        .filter(t -> t._1()._2() == upstreamLineIndex)
        .map(t -> createEntry(lines.get(t._2()), buildEntriesStructure(lines, lineIndexToUpstreamLineIndex, t._2())))
        .collect(List.collector());
  }

  /**
   * Parses line to {@link com.virtuslab.branchlayout.impl.BranchLayoutEntry#BranchLayoutEntry} arguments and creates an
   * entry with specified {@code subbranches}.
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
      throw new BranchLayoutException(0, String.format(
          "The initial line of branch layout file (%s) cannot be indented", path.toAbsolutePath().toString()));
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
        throw new BranchLayoutException(lineNumber, String.format(
            "One of branches in branch layout file (%s) has incorrect level in relation to its parent branch",
            path.toAbsolutePath().toString()));
      }
      Tuple2<Integer, Integer> levelAndUpstreamLineIndex = new Tuple2<>(level,
          level == 0 ? -1 : levelToPresentUpstream.get(level - 1));
      lineIndexToIndentLevelAndUpstreamLineIndex = lineIndexToIndentLevelAndUpstreamLineIndex.update(lineNumber,
          levelAndUpstreamLineIndex);
      levelToPresentUpstream = levelToPresentUpstream.update(level, lineNumber);

      previousLevel = level;
    }

    return lineIndexToIndentLevelAndUpstreamLineIndex;
  }

  private List<String> getFileLines() throws BranchLayoutException {
    return Try.of(() -> List.ofAll(Files.readAllLines(path)))
        .getOrElseThrow(e -> new BranchLayoutException(String.format(
            "Error while loading branch layout file (%s)", path.toAbsolutePath().toString()), e));
  }

  private int getIndentLevelWidth(String line) {
    return (int) line.chars().takeWhile(c -> c == indentCharacter).count();
  }

  private int getIndentLevel(int indent, int lineNumber) throws BranchLayoutException {
    if (indent == 0) {
      return 0;
    }

    if (indent % levelWidth != 0) {
      throw new BranchLayoutException(lineNumber, String.format(
          "Levels of indentation are not matching in branch layout file (%s)", path.toAbsolutePath().toString()));
    }

    return indent / levelWidth;
  }
}
