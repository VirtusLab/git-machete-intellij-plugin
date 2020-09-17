package com.virtuslab.gitmachete.frontend.ui.impl;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.awt.Component;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.ui.GuiUtils;
import com.intellij.util.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

public class GitMacheteErrorHandler extends ErrorReportSubmitter {
  @Override
  public String getReportActionText() {
    return getString("string.GitMachete.error-handler.report-action-text");
  }

  @Override
  public boolean submit(
      IdeaLoggingEvent[] events,
      @Nullable String additionalInfo,
      Component parentComponent,
      Consumer<SubmittedReportInfo> consumer) {
    GuiUtils.invokeLaterIfNeeded(() -> BrowserUtil.browse(
        "https://github.com/VirtusLab/git-machete-intellij-plugin/issues/new?assignees=&labels=bug&template=bug_report.md"),
        ModalityState.NON_MODAL);
    return true;
  }
}
