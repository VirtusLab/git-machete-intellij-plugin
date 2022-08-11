package com.virtuslab.gitmachete.frontend.ui.impl;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import com.intellij.diagnostic.IdeaReportingEvent;
import com.intellij.diagnostic.LogMessage;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GitMacheteErrorReportSubmitter.class})
public class GitMacheteErrorReportSubmitterTest {

  private String defaultErrorReport;
  private String defaultErrorReportWithTemplate;

  @Before
  public void setUp() {
    {
      try {
        defaultErrorReport = IOUtils.resourceToString("/error_report_uri.txt", StandardCharsets.UTF_8).strip();
        defaultErrorReportWithTemplate = IOUtils.resourceToString("/error_report_uri_with_template.txt",
            StandardCharsets.UTF_8).strip();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Test
  public void testGitHubIssueUriConstruction() throws Exception {
    val events = new IdeaLoggingEvent[1];

    val messageObject = Whitebox.newInstance(LogMessage.class);

    events[0] = new IdeaReportingEvent(messageObject, /* message */ null, /* stacktrace */"", /* plugin */null);

    val reportSubmitter = PowerMockito.spy(new GitMacheteErrorReportSubmitter());

    PowerMockito
        .stub(PowerMockito.method(GitMacheteErrorReportSubmitter.class, "getTemplateVariables", IdeaLoggingEvent[].class,
            String.class))
        .toReturn(new HashMap<String, String>());

    val logger = PowerMockito.mock(LambdaLogger.class);
    Whitebox.setInternalState(reportSubmitter.getClass(), logger);

    String reportBody = Whitebox.invokeMethod(reportSubmitter, "getReportBody", events, /* additionalInfo */ null);

    Mockito.verify(logger, never().description("Reading the resource file with the bug template should NOT throw"))
        .error(anyString(), (Throwable) any());

    URI uriWithReport = Whitebox.invokeMethod(reportSubmitter, "constructNewGitHubIssueUri", events, reportBody);
    Assert.assertEquals("URI should match default with report body", defaultErrorReportWithTemplate, uriWithReport.toString());

    URI uriNoReport = Whitebox.invokeMethod(reportSubmitter, "constructNewGitHubIssueUri", events, /* report Body */ null);
    Assert.assertEquals("URI should match default with NO report body", defaultErrorReport, uriNoReport.toString());
  }
}
