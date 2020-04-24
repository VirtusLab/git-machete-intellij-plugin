package com.virtuslab.branchlayout.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.branchlayout.api.BaseBranchLayoutEntry;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutSaver;

@RequiredArgsConstructor
public class BranchLayoutFileSaver implements IBranchLayoutSaver {
  private final Path path;
  private final Character indentCharacter = ' ';
  private final int levelWidth = 2;

  @Setter
  private boolean backupOldFile = true;

  public void save(IBranchLayout branchLayout) throws BranchLayoutException {
    var lines = printBranchesOntoStringList(branchLayout.getRootBranches(), 0);

    if (backupOldFile) {
      Path parentDir = path.getParent();
      assert parentDir != null : "Can't get parent directory of branch relation file";
      Path backupFilePath = parentDir.resolve(path.getFileName() + "~");
      Try.of(() -> Files.copy(path, backupFilePath, StandardCopyOption.REPLACE_EXISTING))
          .getOrElseThrow(e -> new BranchLayoutException("Unable to backup machete file", e));
    }

    Try.of(() -> Files.write(path, lines))
        .getOrElseThrow(e -> new BranchLayoutException("Unable to save new machete file", e));
  }

  private List<String> printBranchesOntoStringList(List<BaseBranchLayoutEntry> branches, @NonNegative int level) {
    List<String> stringList = List.empty();
    for (var branch : branches) {
      var sb = new StringBuilder();
      sb.append(String.valueOf(indentCharacter).repeat(level * levelWidth)).append(branch.getName());
      Option<String> customAnnotation = branch.getCustomAnnotation();
      if (customAnnotation.isDefined()) {
        sb.append(" ").append(customAnnotation.get());
      }

      stringList = stringList.append(sb.toString())
          .appendAll(printBranchesOntoStringList(branch.getSubbranches(), level + 1));
    }
    return stringList;
  }
}
