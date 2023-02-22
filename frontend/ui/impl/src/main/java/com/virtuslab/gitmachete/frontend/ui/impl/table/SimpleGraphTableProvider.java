package com.virtuslab.gitmachete.frontend.ui.impl.table;

import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseGraphTable;
import com.virtuslab.gitmachete.frontend.ui.api.table.ISimpleGraphTableProvider;

public class SimpleGraphTableProvider implements ISimpleGraphTableProvider {
  @Override
  @UIEffect
  public BaseGraphTable deriveInstance(IGitMacheteRepositorySnapshot macheteRepositorySnapshot,
      boolean isListingCommitsEnabled, boolean shouldDisplayActionToolTips) {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    // The reinstantiation is needed every time because without it
    // the table keeps the first IDE theme despite the theme changes.
    return SimpleGraphTable.deriveInstance(macheteRepositorySnapshot, isListingCommitsEnabled, shouldDisplayActionToolTips);
  }

  @Override
  @UIEffect
  public BaseGraphTable deriveDemoInstance() {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    return deriveInstance(new DemoGitMacheteRepositorySnapshot(), /* isListingCommitsEnabled */ true,
        /* shouldDisplayActionToolTips */ false);
  }
}
