package com.virtuslab.gitmachete.frontend.ui.impl;

import static org.mockito.ArgumentMatchers.*;

import java.util.HashMap;

import com.intellij.diagnostic.AbstractMessage;
import com.intellij.diagnostic.IdeaReportingEvent;
import com.intellij.diagnostic.LogMessage;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GitMacheteErrorReportSubmitter.class})
public class GitMacheteErrorReportSubmitterTest {
  @Test
  public void testGitHubIssueUriConstruction() throws Exception {
    IdeaLoggingEvent[] events = new IdeaLoggingEvent[1];

    AbstractMessage messageObject = Whitebox.newInstance(LogMessage.class);

    events[0] = new IdeaReportingEvent(messageObject, /* message */ null, /* stacktrace */"", /* plugin */null);

    GitMacheteErrorReportSubmitter reportSubmitter = PowerMockito.spy(new GitMacheteErrorReportSubmitter());

    PowerMockito
        .stub(PowerMockito.method(GitMacheteErrorReportSubmitter.class, "getTemplateVariables", IdeaLoggingEvent[].class,
            String.class))
        .toReturn(new HashMap<String, String>());

    String reportBody = Whitebox.invokeMethod(reportSubmitter, "getReportBody", events, /* additionalInfo */ null);

    Whitebox.invokeMethod(reportSubmitter, "constructNewGitHubIssueUri", events, reportBody);

    Whitebox.invokeMethod(reportSubmitter, "constructNewGitHubIssueUri", events, null);
  }
}
