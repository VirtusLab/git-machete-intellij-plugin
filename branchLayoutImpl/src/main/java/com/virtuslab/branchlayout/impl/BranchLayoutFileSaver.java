package com.virtuslab.branchlayout.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import io.vavr.collection.List;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutEntry;

@RequiredArgsConstructor
@Getter
public class BranchLayoutFileSaver {
  private final Path path;
  private Character indentCharacter = ' ';
  private int levelWidth = 0;

  public void save(IBranchLayout branchLayout, boolean backupOldFile) throws IOException {
    var lines = printBranchesOntoStringList(branchLayout.getRootBranches(), 0);

    if (backupOldFile) {
      var pathToBackupFile = path.getParent().resolve(path.getFileName() + "~");
      Files.copy(path, pathToBackupFile, StandardCopyOption.REPLACE_EXISTING);
    }

    Files.write(path, lines);
  }

  private List<String> printBranchesOntoStringList(List<IBranchLayoutEntry> branches, int level) {
    List<String> stringList = List.empty();
    for (var branch : branches) {
      var sb = new StringBuilder();
      sb.append(String.valueOf(indentCharacter).repeat(level * levelWidth)).append(branch.getName());
      var customAnnotation = branch.getCustomAnnotation();
      if (customAnnotation.isPresent()) {
        sb.append(" ").append(customAnnotation.get());
      }

      stringList = stringList.append(sb.toString())
          .appendAll(printBranchesOntoStringList(branch.getSubbranches(), level + 1));
    }
    return stringList;
  }
}
