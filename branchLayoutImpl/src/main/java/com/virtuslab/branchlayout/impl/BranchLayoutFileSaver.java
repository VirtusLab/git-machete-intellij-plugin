package com.virtuslab.branchlayout.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.branchlayout.api.BaseBranchLayoutEntry;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutSaver;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

@RequiredArgsConstructor
public class BranchLayoutFileSaver implements IBranchLayoutSaver {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("branchLayout");

  private final Path path;
  private final Character indentCharacter = ' ';
  private final int levelWidth = 2;

  public void save(IBranchLayout branchLayout, boolean backupOldFile) throws BranchLayoutException {
    LOG.debug(
        () -> "Entering: branchLayout = ${branchLayout}, backupOldFile = ${backupOldFile}");
    var lines = printBranchesOntoStringList(branchLayout.getRootEntries(), 0);

    if (backupOldFile) {
      Path parentDir = path.getParent();
      assert parentDir != null : "Can't get parent directory of branch layout file";
      Path backupPath = parentDir.resolve(path.getFileName() + "~");
      Try.of(() -> Files.copy(path, backupPath, StandardCopyOption.REPLACE_EXISTING))
          .getOrElseThrow(
              e -> new BranchLayoutException("Unable to backup branch layout file from ${path} to ${backupPath}", e));
    }

    LOG.debug(() -> "Saving branch layout to ${path} with indent character ASCII code = ${(int)indentCharacter} " +
        "and level width = ${levelWidth}:");
    lines.forEach(LOG::debug);

    Try.of(() -> Files.write(path, lines))
        .getOrElseThrow(e -> new BranchLayoutException("Unable to save new branch layout file to ${path}", e));
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
          .appendAll(printBranchesOntoStringList(branch.getSubentries(), level + 1));
    }
    return stringList;
  }
}
