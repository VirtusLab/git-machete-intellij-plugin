package com.virtuslab.gitmachete.frontend.ui.impl;

import java.awt.Component;

import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.util.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

public class GitMacheteErrorHandler extends ErrorReportSubmitter {
  @Override
  public String getReportActionText() {
    return "Report to VirtusLab";
  }

  @Override
  public boolean submit(
      IdeaLoggingEvent[] events,
      @Nullable String additionalInfo,
      Component parentComponent,
      Consumer<SubmittedReportInfo> consumer) {
    // implement here
    return super.submit(events, additionalInfo, parentComponent, consumer);
  }
}
