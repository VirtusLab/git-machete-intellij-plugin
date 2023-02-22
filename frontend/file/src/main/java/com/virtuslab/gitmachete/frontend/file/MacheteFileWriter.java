package com.virtuslab.gitmachete.frontend.file;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFileManager;
import io.vavr.collection.List;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.qual.guieffect.IgnoreUIThreadUnsafeCalls;

@CustomLog
public final class MacheteFileWriter {

  private MacheteFileWriter() {}

  /**
   * The method for writing the branch layout using IntelliJ's VFS API, should be executed on the UI thread and wrapped in a WriteAction.
   * @param path a path to file where branch layout should be written
   * @param branchLayout a layout to be written
   * @param backupOldFile a flag stating if the old layout file should be backed-up
   * @param requestor an object requesting the write execution (can be used for debugging when listening for the file changes)
   */
  @IgnoreUIThreadUnsafeCalls("java.io.BufferedOutputStream.close()")
  @UIEffect
  public static void writeBranchLayout(
      Path path,
      IBranchLayoutWriter branchLayoutWriter,
      BranchLayout branchLayout,
      boolean backupOldFile,
      @Nullable Object requestor) throws IOException {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    LOG.debug(() -> "Writing branch layout to (${path}), branchLayout = ${branchLayout}, backupOldFile = ${backupOldFile}");

    val parentPath = path.getParent();
    assert parentPath != null : "Can't get parent directory of branch layout file";
    val parentDirVFile = VirtualFileManager.getInstance().findFileByNioPath(parentPath);
    assert parentDirVFile != null : "Can't get parent directory of branch layout file";

    val macheteFileName = path.getFileName();
    assert macheteFileName != null : "Invalid path to machete file";
    var macheteVFile = parentDirVFile.findChild(macheteFileName.toString());

    if (macheteVFile != null) {
      if (backupOldFile) {
        val backupFileName = macheteVFile.getName() + "~";
        val backupVFile = parentDirVFile.findChild(backupFileName);
        if (backupVFile != null) {
          backupVFile.delete(requestor);
        }

        VfsUtilCore.copyFile(requestor, macheteVFile, parentDirVFile, backupFileName);
      }
    } else {
      macheteVFile = parentDirVFile.createChildData(requestor, macheteFileName.toString());
    }

    try (val outputStream = new BufferedOutputStream(macheteVFile.getOutputStream(requestor))) {
      val fileLines = List.ofAll(VfsUtilCore.loadText(macheteVFile).lines());
      val indentSpec = branchLayoutWriter.deriveIndentSpec(fileLines);

      branchLayoutWriter.write(outputStream, branchLayout, indentSpec);
    }
  }
}
