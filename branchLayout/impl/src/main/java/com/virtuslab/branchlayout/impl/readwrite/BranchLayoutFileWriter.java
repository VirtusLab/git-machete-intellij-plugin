package com.virtuslab.branchlayout.impl.readwrite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.IBranchLayoutEntry;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;

@ExtensionMethod(BranchLayoutFileUtils.class)
@CustomLog
@RequiredArgsConstructor
public class BranchLayoutFileWriter implements IBranchLayoutWriter {

  // TODO (#692): wrap write operation in WriteAction
  @Override
  @SuppressWarnings("regexp") // to allow for `synchronized`
  public synchronized void write(Path path, IBranchLayout branchLayout, boolean backupOldFile) throws BranchLayoutException {
    LOG.debug(() -> "Entering: path = ${path}, branchLayout = ${branchLayout}, backupOldFile = ${backupOldFile}");
    val indentSpec = path.deriveIndentSpec();

    val lines = printEntriesOntoStringList(branchLayout.getRootEntries(), indentSpec, /* level */ 0);

    if (backupOldFile && Files.isRegularFile(path)) {
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

  private List<String> printEntriesOntoStringList(
      List<IBranchLayoutEntry> entries,
      IndentSpec indentSpec,
      @NonNegative int level) {

    List<String> stringList = List.empty();
    for (val entry : entries) {
      val sb = new StringBuilder();
      val count = level * indentSpec.getIndentWidth();
      for (int i = 0; i < count; i++) {
        sb.append(indentSpec.getIndentCharacter());
      }
      sb.append(entry.getName());
      String customAnnotation = entry.getCustomAnnotation();
      if (customAnnotation != null) {
        sb.append(" ").append(customAnnotation);
      }

      List<String> resultForChildren = printEntriesOntoStringList(entry.getChildren(), indentSpec, level + 1);
      stringList = stringList.append(sb.toString()).appendAll(resultForChildren);
    }
    return stringList;
  }
}
