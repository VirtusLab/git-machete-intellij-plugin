package com.virtuslab.branchrelationfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.*;
import lombok.Getter;

public class BranchRelationFile {
  private Path pathToMacheteFile;
  @Getter private List<BranchRelationFileBranchEntry> rootBranches = new LinkedList<>();

  private Character indentType = null;
  private int levelWidth = 0;

  public BranchRelationFile(Path path) throws BranchRelationFileException {
    pathToMacheteFile = path;

    List<String> lines;
    try {
      lines = Files.readAllLines(pathToMacheteFile);
    } catch (IOException e) {
      throw new BranchRelationFileException(
          MessageFormat.format(
              "Error while loading machete file ({0})",
              pathToMacheteFile.toAbsolutePath().toString()),
          e);
    }

    lines.removeIf(this::isEmptyLine);

    if (lines.size() < 1) return;

    if (getIndent(lines.get(0)) > 0)
      throw new BranchRelationFileException(
          MessageFormat.format(
              "The initial line of machete file ({0}) cannot be indented",
              pathToMacheteFile.toAbsolutePath().toString()));

    int currentLevel = 0;
    Map<Integer, BranchRelationFileBranchEntry> macheteBranchesLevelsMap = new HashMap<>();
    for (var line : lines) {
      int level = getLevel(getIndent(line));

      if (level - currentLevel > 1)
        throw new BranchRelationFileException(
            MessageFormat.format(
                "One of branches in machete file ({0}) has incorrect level in relation to its parent branch",
                pathToMacheteFile.toAbsolutePath().toString()));

      String trimmedLine = line.trim();

      String branchName;
      Optional<String> customAnnotation;
      int indexOfSpace = trimmedLine.indexOf(' ');
      if (indexOfSpace > -1) {
        branchName = trimmedLine.substring(0, indexOfSpace);
        customAnnotation = Optional.of(trimmedLine.substring(indexOfSpace + 1).trim());
      } else {
        branchName = trimmedLine;
        customAnnotation = Optional.empty();
      }

      BranchRelationFileBranchEntry branch;

      if (level == 0) {
        branch = new BranchRelationFileBranchEntry(branchName, Optional.empty(), customAnnotation);
        rootBranches.add(branch);
      } else {
        branch =
            new BranchRelationFileBranchEntry(
                branchName, Optional.of(macheteBranchesLevelsMap.get(level - 1)), customAnnotation);
        macheteBranchesLevelsMap.get(level - 1).addSubbranch(branch);
      }

      macheteBranchesLevelsMap.put(level, branch);

      currentLevel = level;
    }
  }

  private boolean isEmptyLine(String l) {
    return l.trim().length() < 1;
  }

  private int getIndent(String l) {
    int indent = 0;
    for (int i = 0; i < l.length(); i++) {
      if (indentType == null) {
        if (l.charAt(i) != ' ' && l.charAt(i) != '\t') {
          break;
        } else {
          indent++;
          indentType = l.charAt(i);
        }
      } else {
        if (l.charAt(i) == indentType) indent++;
        else break;
      }
    }

    return indent;
  }

  private int getLevel(int indent) throws BranchRelationFileException {
    if (levelWidth == 0 && indent > 0) {
      levelWidth = indent;
      return 1;
    } else if (indent == 0) {
      return 0;
    }

    if (indent % levelWidth != 0)
      throw new BranchRelationFileException(
          MessageFormat.format(
              "Levels of indentations are not matching in machete file ({0})",
              pathToMacheteFile.toAbsolutePath().toString()));

    return indent / levelWidth;
  }

  public void saveToFile() throws IOException {
    saveToFile(true);
  }

  public void saveToFile(boolean backupOldFile) throws IOException {
    var lines = new LinkedList<String>();
    printBranchesOntoStringList(lines, rootBranches, 0);

    if (backupOldFile) {
      var pathToBackupFile = pathToMacheteFile.getParent().resolve("machete~");
      Files.copy(pathToMacheteFile, pathToBackupFile, StandardCopyOption.REPLACE_EXISTING);
    }

    Files.write(pathToMacheteFile, lines);
  }

  private void printBranchesOntoStringList(
      List<String> sl, List<BranchRelationFileBranchEntry> branches, int level) {
    for (var branch : branches) {
      var sb = new StringBuilder();
      sb.append(String.valueOf(indentType).repeat(level * levelWidth));
      sb.append(branch.getName());

      var customAnnotation = branch.getCustomAnnotation();
      if (customAnnotation.isPresent()) {
        sb.append(" ");
        sb.append(customAnnotation.get());
      }

      sl.add(sb.toString());

      printBranchesOntoStringList(sl, branch.getSubbranches(), level + 1);
    }
  }
}
