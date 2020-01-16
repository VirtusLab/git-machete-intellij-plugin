package com.virtuslab.branchrelationfile;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.virtuslab.branchrelationfile.api.BranchRelationFileException;
import com.virtuslab.branchrelationfile.api.IBranchRelationFile;
import com.virtuslab.branchrelationfile.api.IBranchRelationFileEntry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.*;
import lombok.Getter;

public class BranchRelationFile implements IBranchRelationFile {
  private Path pathToBranchRelationFile;
  @Getter private List<IBranchRelationFileEntry> rootBranches = new LinkedList<>();

  private Character indentType = null;
  private int levelWidth = 0;

  @Inject
  public BranchRelationFile(@Assisted Path path) throws BranchRelationFileException {
    pathToBranchRelationFile = path;

    List<String> lines;
    try {
      lines = Files.readAllLines(pathToBranchRelationFile);
    } catch (IOException e) {
      throw new BranchRelationFileException(
          MessageFormat.format(
              "Error while loading branch relation file ({0})",
              pathToBranchRelationFile.toAbsolutePath().toString()),
          e);
    }

    if (lines.size() < 1) return;

    boolean isFirstSignificantLine = true;
    int lineNumber = 1;
    int currentLevel = 0;
    List<IBranchRelationFileEntry> currentUpstreamList = new LinkedList<>();
    for (var line : lines) {
      if (isEmptyLine(line)) continue;

      if (isFirstSignificantLine && getIndent(lines.get(0)) > 0)
        throw new BranchRelationFileException(
            MessageFormat.format(
                "The initial line of branch relation file ({0}) cannot be indented",
                pathToBranchRelationFile.toAbsolutePath().toString()),
            lineNumber);

      isFirstSignificantLine = false;

      int level = getLevel(getIndent(line), lineNumber);

      if (level - currentLevel > 1)
        throw new BranchRelationFileException(
            MessageFormat.format(
                "One of branches in branch relation file ({0}) has incorrect level in relation to its parent branch",
                pathToBranchRelationFile.toAbsolutePath().toString()),
            lineNumber);

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

      IBranchRelationFileEntry branch;

      if (level == 0) {
        branch = new BranchRelationFileEntry(branchName, Optional.empty(), customAnnotation);
        rootBranches.add(branch);
      } else {
        branch =
            new BranchRelationFileEntry(
                branchName, Optional.of(currentUpstreamList.get(level - 1)), customAnnotation);
        currentUpstreamList.get(level - 1).addSubbranch(branch);
      }

      currentUpstreamList.add(level, branch);

      currentLevel = level;
    }
  }

  private boolean isEmptyLine(String l) {
    return l.trim().length() == 0;
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

  private int getLevel(int indent, int lineNumber) throws BranchRelationFileException {
    if (levelWidth == 0 && indent > 0) {
      levelWidth = indent;
      return 1;
    } else if (indent == 0) {
      return 0;
    }

    if (indent % levelWidth != 0)
      throw new BranchRelationFileException(
          MessageFormat.format(
              "Levels of indentations are not matching in branch relation file ({0})",
              pathToBranchRelationFile.toAbsolutePath().toString()),
          lineNumber);

    return indent / levelWidth;
  }

  @Override
  public void saveToFile(boolean backupOldFile) throws IOException {
    var lines = new LinkedList<String>();
    printBranchesOntoStringList(lines, getRootBranches(), 0);

    if (backupOldFile) {
      var pathToBackupFile =
          pathToBranchRelationFile
              .getParent()
              .resolve(pathToBranchRelationFile.getFileName() + "~");
      Files.copy(pathToBranchRelationFile, pathToBackupFile, StandardCopyOption.REPLACE_EXISTING);
    }

    Files.write(pathToBranchRelationFile, lines);
  }

  private void printBranchesOntoStringList(
      List<String> sl, List<IBranchRelationFileEntry> branches, int level) {
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

  public Optional<IBranchRelationFileEntry> findBranchByName(String branchName) {
    return findBranchByNameInBranches(branchName, getRootBranches());
  }

  private Optional<IBranchRelationFileEntry> findBranchByNameInBranches(
      String branchName, List<IBranchRelationFileEntry> branches) {
    for (var branch : branches) {
      if (branch.getName().equals(branchName)) return Optional.of(branch);

      var ret = findBranchByNameInBranches(branchName, branch.getSubbranches());
      if (ret.isPresent()) return ret;
    }

    return Optional.empty();
  }
}
