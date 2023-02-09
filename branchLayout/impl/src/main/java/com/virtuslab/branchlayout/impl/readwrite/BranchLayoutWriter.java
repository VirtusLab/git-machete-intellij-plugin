package com.virtuslab.branchlayout.impl.readwrite;

import java.io.IOException;
import java.io.OutputStream;

import io.vavr.collection.List;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutEntry;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.branchlayout.api.readwrite.IndentSpec;
import com.virtuslab.qual.guieffect.IgnoreUIThreadUnsafeCalls;

@CustomLog
public class BranchLayoutWriter implements IBranchLayoutWriter {
  @IgnoreUIThreadUnsafeCalls("java.io.OutputStream.write([B)")
  @Override
  public void write(OutputStream outputStream, BranchLayout branchLayout, IndentSpec indentSpec) throws IOException {

    val lines = entriesToStringList(branchLayout.getRootEntries(), indentSpec, /* level */ 0);

    LOG.debug(() -> "Writing branch layout with indent character ASCII " +
        "code = ${indentSpec.getIndentCharacter()} and indent width = ${indentSpec.getIndentWidth()}");
    lines.forEach(LOG::debug);

    outputStream.write(lines.mkString(System.lineSeparator()).getBytes());
  }

  private List<String> entriesToStringList(
      List<BranchLayoutEntry> entries,
      IndentSpec indentSpec,
      @NonNegative int level) {

    List<String> stringList = List.empty();
    for (val entry : entries) {
      val sb = new StringBuilder();
      val count = level * indentSpec.getIndentWidth();
      sb.append(String.valueOf(indentSpec.getIndentCharacter()).repeat(count));
      sb.append(entry.getName());
      String customAnnotation = entry.getCustomAnnotation();
      if (customAnnotation != null) {
        sb.append(" ").append(customAnnotation);
      }

      List<String> resultForChildren = entriesToStringList(entry.getChildren(), indentSpec, level + 1);
      stringList = stringList.append(sb.toString()).appendAll(resultForChildren);
    }
    return stringList;
  }

  public IndentSpec deriveIndentSpec(List<String> lines) {
    return BranchLayoutFileUtils.deriveIndentSpec(lines);
  }
}
