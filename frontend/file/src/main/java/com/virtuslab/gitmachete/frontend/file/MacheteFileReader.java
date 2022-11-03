package com.virtuslab.gitmachete.frontend.file;

import java.io.IOException;
import java.nio.file.Path;

import com.intellij.openapi.vfs.VirtualFileManager;
import io.vavr.collection.List;
import lombok.CustomLog;
import lombok.val;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.qual.guieffect.IgnoreUIThreadUnsafeCalls;

@CustomLog
public final class MacheteFileReader {

  private MacheteFileReader() {}

  /**
   * Method for reading branch layout using IntelliJ's VFS API, should be used inside a ReadAction
   */
  @IgnoreUIThreadUnsafeCalls("java.io.InputStream.close()")
  public static BranchLayout readBranchLayout(Path path, IBranchLayoutReader branchLayoutReader) throws BranchLayoutException {
    LOG.debug(() -> "Reading branch layout from (${path}), branchLayoutReader = ${branchLayoutReader}");
    val macheteVFile = VirtualFileManager.getInstance().findFileByNioPath(path);

    BranchLayout resultBranchLayout = new BranchLayout(List.empty());

    if (macheteVFile != null) {
      try (val inputStream = macheteVFile.getInputStream()) {
        resultBranchLayout = branchLayoutReader.read(inputStream);
      } catch (IOException e) {
        throw new BranchLayoutException("Error while reading (${path})", e);
      }
    }
    return resultBranchLayout;
  }
}
