package com.virtuslab.branchlayout.impl.readwrite;

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
import com.virtuslab.branchlayout.api.manager.IBranchLayoutWriter;
import com.virtuslab.branchlayout.impl.BranchLayout;
import com.virtuslab.branchlayout.impl.IndentSpec;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

@RequiredArgsConstructor
public class BranchLayoutFileWriter implements IBranchLayoutWriter {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("branchLayout");

  @Override
  public void write(IBranchLayout branchLayout, boolean backupOldFile) throws BranchLayoutException {
    LOG.debug(() -> "Entering: branchLayout = ${branchLayout}, backupOldFile = ${backupOldFile}");

    var path = ((BranchLayout) branchLayout).getPath();
    var indentSpec = ((BranchLayout) branchLayout).getIndentSpec();

    var lines = printBranchesOntoStringList(branchLayout.getRootEntries(), indentSpec, /* level */ 0);

    if (backupOldFile) {
      Path parentDir = path.getParent();
      assert parentDir != null : "Can't get parent directory of branch layout file";
      Path backupPath = parentDir.resolve(path.getFileName() + "~");
      Try.of(() -> Files.copy(path, backupPath, StandardCopyOption.REPLACE_EXISTING))
          .getOrElseThrow(
              e -> new BranchLayoutException("Unable to backup branch layout file from ${path} to ${backupPath}", e));
    }

    LOG.debug(() -> "Writing branch layout to ${path} with indent character ASCII " +
        "code = ${indentSpec.getIndentCharacter()} and indent width = ${indentSpec.getIndentWidth()}");
    lines.forEach(LOG::debug);

    Try.of(() -> Files.write(path, lines))
        .getOrElseThrow(e -> new BranchLayoutException("Unable to write new branch layout file to ${path}", e));
  }

  private List<String> printBranchesOntoStringList(List<BaseBranchLayoutEntry> branches, IndentSpec indentSpec,
      @NonNegative int level) {
    List<String> stringList = List.empty();
    for (var branch : branches) {
      var sb = new StringBuilder();
      sb.append(String.valueOf(indentSpec.getIndentCharacter()).repeat(level * indentSpec.getIndentWidth()))
          .append(branch.getName());
      Option<String> customAnnotation = branch.getCustomAnnotation();
      if (customAnnotation.isDefined()) {
        sb.append(" ").append(customAnnotation.get());
      }

      stringList = stringList.append(sb.toString())
          .appendAll(printBranchesOntoStringList(branch.getSubentries(), indentSpec, level + 1));
    }
    return stringList;
  }
}
