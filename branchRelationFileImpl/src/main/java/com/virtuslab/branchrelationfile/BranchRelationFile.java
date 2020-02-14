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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;

@Getter
public class BranchRelationFile implements IBranchRelationFile {
  private Path path;
  private List<IBranchRelationFileEntry> rootBranches = new LinkedList<>();

  private Character indentType = null;
  private int levelWidth = 0;

  @Inject
  public BranchRelationFile(@Assisted Path path) throws BranchRelationFileException {
    this.path = path;

    List<String> lines;
    try {
      lines = Files.readAllLines(this.path);
    } catch (IOException e) {
      throw new BranchRelationFileException(
          MessageFormat.format(
              "Error while loading branch relation file ({0})",
              this.path.toAbsolutePath().toString()),
          e);
    }

    if (lines.size() < 1) return;

    boolean isFirstSignificantLine = true;
    int lineNumber = 0;
    int currentLevel = 0;
    List<IBranchRelationFileEntry> currentUpstreamList = new LinkedList<>();
    for (var line : lines) {
      lineNumber++;
      if (line.trim().isEmpty()) {
        continue;
      }

      if (isFirstSignificantLine && getIndentLevelWidth(lines.get(0)) > 0) {
        throw new BranchRelationFileException(
            MessageFormat.format(
                "The initial line of branch relation file ({0}) cannot be indented",
                this.path.toAbsolutePath().toString()),
            lineNumber);
      }

      isFirstSignificantLine = false;

      int level = getIndentLevel(getIndentLevelWidth(line), lineNumber);

      if (level - currentLevel > 1) {
        throw new BranchRelationFileException(
            MessageFormat.format(
                "One of branches in branch relation file ({0}) has incorrect level in relation to its parent branch",
                this.path.toAbsolutePath().toString()),
            lineNumber);
      }

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

      IBranchRelationFileEntry branch;

      if (level == 0) {
        branch = new BranchRelationFileEntry(branchName, /*upstream*/ null, customAnnotation);
        rootBranches.add(branch);
      } else {
        branch =
            new BranchRelationFileEntry(
                branchName, currentUpstreamList.get(level - 1), customAnnotation);
        currentUpstreamList.get(level - 1).addSubbranch(branch);
      }

      currentUpstreamList.add(level, branch);

      currentLevel = level;
    }
  }

  public BranchRelationFile(IBranchRelationFile branchRelationFile) {
    this.path = branchRelationFile.getPath();
    this.rootBranches = new LinkedList<>(branchRelationFile.getRootBranches());
    this.indentType = branchRelationFile.getIndentType();
    this.levelWidth = branchRelationFile.getLevelWidth();
  }

  private int getIndentLevelWidth(String l) {
    int result = 0;
    for (int i = 0; i < l.length(); i++) {
      if (indentType == null) {
        if (l.charAt(i) != ' ' && l.charAt(i) != '\t') {
          break;
        } else {
          result++;
          indentType = l.charAt(i);
        }
      } else {
        if (l.charAt(i) == indentType) result++;
        else break;
      }
    }

    return result;
  }

  private int getIndentLevel(int indent, int lineNumber) throws BranchRelationFileException {
    if (levelWidth == 0 && indent > 0) {
      levelWidth = indent;
      return 1;
    } else if (indent == 0) {
      return 0;
    }

    if (indent % levelWidth != 0) {
      throw new BranchRelationFileException(
          MessageFormat.format(
              "Levels of indentation are not matching in branch relation file ({0})",
              path.toAbsolutePath().toString()),
          lineNumber);
    }

    return indent / levelWidth;
  }

  @Override
  public void saveToFile(boolean backupOldFile) throws IOException {
    var lines = new LinkedList<String>();
    printBranchesOntoStringList(lines, getRootBranches(), 0);

    if (backupOldFile) {
      var pathToBackupFile = path.getParent().resolve(path.getFileName() + "~");
      Files.copy(path, pathToBackupFile, StandardCopyOption.REPLACE_EXISTING);
    }

    Files.write(path, lines);
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

  @Override
  public IBranchRelationFile withBranchSlidOut(String branchName)
      throws BranchRelationFileException, IOException {
    var branch = findBranchByName(branchName);
    if (branch.isEmpty()) {
      throw new BranchRelationFileException(
          MessageFormat.format("Branch \"{0}\" does not exist", branchName));
    }

    return withBranchSlidOut(branch.get());
  }

  @Override
  public IBranchRelationFile withBranchSlidOut(IBranchRelationFileEntry relationFileEntry)
      throws BranchRelationFileException, IOException {
    if (relationFileEntry.getUpstream().isEmpty()) {
      throw new BranchRelationFileException("Can not slide out root branch");
    }

    var upstream = relationFileEntry.getUpstream().get();
    int indexInUpstream = upstream.getSubbranches().indexOf(relationFileEntry);

    IBranchRelationFileEntry upstreamCopy = new BranchRelationFileEntry(upstream);

    for (var subbranch : relationFileEntry.getSubbranches()) {
      IBranchRelationFileEntry subbranchCopy = subbranch.withUpstream(upstreamCopy);
      upstreamCopy.getSubbranches().add(indexInUpstream, subbranchCopy);
      indexInUpstream++;
    }

    upstreamCopy.getSubbranches().remove(relationFileEntry);

    // Copy constructor
    BranchRelationFile newBranchRelationFile = new BranchRelationFile(/*branchRelationFile*/ this);

    traverseBranchesUpToRoot(newBranchRelationFile, upstreamCopy);

    newBranchRelationFile.saveToFile(/*backupOldFile*/ true);

    return newBranchRelationFile;
  }

  private void traverseBranchesUpToRoot(
      BranchRelationFile newBranchRelationFile, IBranchRelationFileEntry branchToTraverse) {
    var oldUpstreamBranch = branchToTraverse.getUpstream();
    IBranchRelationFileEntry oldBranchToTraverse;

    if (oldUpstreamBranch.isEmpty()) {
      oldBranchToTraverse =
          newBranchRelationFile
              .findBranchByNameInBranches(
                  branchToTraverse.getName(), newBranchRelationFile.getRootBranches())
              .get();
      Collections.replaceAll(
          newBranchRelationFile.getRootBranches(), oldBranchToTraverse, branchToTraverse);
    } else {
      oldBranchToTraverse =
          newBranchRelationFile
              .findBranchByNameInBranches(
                  branchToTraverse.getName(), oldUpstreamBranch.get().getSubbranches())
              .get();
      var newUpstreamBranch = new BranchRelationFileEntry(oldUpstreamBranch.get());
      Collections.replaceAll(
          newUpstreamBranch.getSubbranches(), oldBranchToTraverse, branchToTraverse);

      traverseBranchesUpToRoot(newBranchRelationFile, newUpstreamBranch);
    }
  }

  private Optional<IBranchRelationFileEntry> findBranchByNameInBranches(
      String branchName, List<IBranchRelationFileEntry> branches) {
    for (var branch : branches) {
      if (branch.getName().equals(branchName)) {
        return Optional.of(branch);
      }

      var ret = findBranchByNameInBranches(branchName, branch.getSubbranches());
      if (ret.isPresent()) {
        return ret;
      }
    }

    return Optional.empty();
  }
}
